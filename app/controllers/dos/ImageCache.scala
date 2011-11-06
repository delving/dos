package controllers.dos

import com.mongodb.casbah.gridfs.Imports._
import com.mongodb.casbah.Implicits._
import org.apache.log4j.Logger
import java.util.Date
import java.io.InputStream
import play.mvc.Http.Response
import play.mvc.results.{NotFound, Result}
import play.utils.Utils
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.Header
import play.mvc.Controller

/**
 * @author Sjoerd Siebinga <sjoerd.siebinga@gmail.com>
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 * @since 1/2/11 10:09 PM
 */
object ImageCache extends Controller with RespondWithDefaultImage {
  val imageCacheService = new ImageCacheService

  def image(id: String, withDefaultFromUrl: Boolean = true): Result = {
  val result = imageCacheService.retrieveImageFromCache(id, false, "", response)
  if (withDefaultFromUrl) withDefaultFromRequest(request, result, false, "") else result
  }
  def thumbnail(id: String, width: String, withDefaultFromUrl: Boolean = true): Result = {
    val result = imageCacheService.retrieveImageFromCache(id, true, width, response)
    if (withDefaultFromUrl) withDefaultFromRequest(request, result, true, width) else result
  }
}

class ImageCacheService extends HTTPClient with Thumbnail {

  private val log: Logger = Logger.getLogger("ImageCacheService")

  def retrieveImageFromCache(url: String, thumbnail: Boolean, thumbnailWidth: String = "", response: Response): Result = {
    // catch try block to harden the application and always give back a 404 for the application
    try {
      require(url != null)
      require(url != "noImageFound")
      require(!url.isEmpty)

      val isAvailable = checkOrInsert(sanitizeUrl(url), response)
      isAvailable match {
        case false => new NotFound(url)
        case true => ImageDisplay.renderImage(id = url, thumbnail = thumbnail, thumbnailWidth = ImageDisplay.thumbnailWidth(thumbnailWidth), store = imageCacheStore)
      }

    } catch {
      case ia: IllegalArgumentException =>
        log.error("problem with processing this url: \"" + url + "\"")
        new NotFound(url)
      case ex: Exception =>
        log.error("unable to find image: \"" + url + "\"\n" + ex.getStackTraceString)
        new NotFound(url)
    }
  }

  def checkOrInsert(url: String, response: Response): Boolean = {
    if(isImageCached(url)) true else {
      log info ("image not found, attempting to store it in the cache based on URL: " + url)
      val stored = storeImage(url)
      if(stored) {
        log info ("successfully cached image for URL: " + url)
        true
      } else {
        log info ("unable to store " + url)
        false
      }
    }
  }

  private def isImageCached(url: String): Boolean = {
    log info ("attempting to retrieve image for URL " + url)
    imageCacheStore.findOne(url) != None
  }

  private def sanitizeUrl(url: String): String = {
    val sanitizeUrl: String = url.replaceAll("""\\""", "%5C").replaceAll("\\[", "%5B").replaceAll("\\]", "%5D")
    sanitizeUrl
  }

  private def storeImage(url: String): Boolean = {
    val image = retrieveImageFromUrl(url)
    if (image.storable) {
      val inputFile = imageCacheStore.createFile(image.dataAsStream, image.url)
      inputFile.contentType = image.contentType
      inputFile put (IMAGE_ID_FIELD, image.url)
      inputFile put("viewed", 0)
      inputFile put("lastViewed", new Date)
      inputFile.save

      val cachedImage = imageCacheStore.findOne(image.url).getOrElse(return false)
      createThumbnails(cachedImage, imageCacheStore, Map(IMAGE_ID_FIELD -> image.url))
      true
    } else {
      false
    }
  }

  private def retrieveImageFromUrl(url: String): WebResource = {
    val method = new GetMethod(url)
    getHttpClient executeMethod (method)
    method.getResponseHeaders.foreach(header => log debug (header))
    val storable = isStorable(method)
    WebResource(url, method.getResponseBodyAsStream, storable._1, storable._2)
  }

  private def isStorable(method: GetMethod) = {
    val contentType: Header = method.getResponseHeader("Content-Type")
    val contentLength: Header = method.getResponseHeader("Content-Length")
    val mimeTypes = List("image/png", "image/jpeg", "image/jpg", "image/gif", "image/tiff", "image/pjpeg")
    //todo build a size check in later
    (mimeTypes.contains(contentType.getValue.toLowerCase), contentType.getValue)
  }

}

object ImageCacheService {

  val cacheDuration = 60 * 60 * 24

  def setImageCacheControlHeaders(image: GridFSDBFile, response: Response, duration: Int = cacheDuration) {
    response.setContentTypeIfNotSet(image.contentType)
    val now = System.currentTimeMillis();
    //    response.cacheFor(image.underlying.getMD5, duration.toString + "s", now)
    // overwrite the Cache-Control header and add the must-revalidate directive by hand
    response.setHeader("Cache-Control", "max-age=%s, must-revalidate".format(duration))
    response.setHeader("Expires", Utils.getHttpDateFormatter.format(new Date(now + duration * 1000)))
  }



}

case class WebResource(url: String, dataAsStream: InputStream, storable: Boolean, contentType: String)

trait RespondWithDefaultImage {

  import play.mvc.Http.Request
  val IMAGE_NOT_FOUND = classOf[NotFound]

  def withDefaultFromRequest(request: Request, result: Result, thumbnail: Boolean = true, width: String, notFoundResponse: Boolean = true): Result = {
    import play.mvc.results.RenderBinary

    def getDefaultImage: Result = {
      if (request.params._contains("default")) {
        val defaultImageUrl = request.params.get("default")

        val defaultImage = if (thumbnail)
          ImageCache.thumbnail(defaultImageUrl, width, false)
        else
          ImageCache.image(defaultImageUrl, false)

        defaultImage.getClass match {
          case `IMAGE_NOT_FOUND` => if (notFoundResponse) result else new RenderBinary(emptyThumbnailFile, emptyThumbnailFile.getName, true)
          case _ => defaultImage
        }
      }
      else
        if (notFoundResponse) result else new RenderBinary(emptyThumbnailFile, emptyThumbnailFile.getName, true)
    }

    result.getClass match {
        case `IMAGE_NOT_FOUND` => getDefaultImage
        case _ => result
    }
  }
}
