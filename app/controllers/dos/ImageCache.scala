package controllers.dos

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.Imports._
import com.mongodb.casbah.Implicits._
import org.apache.log4j.Logger
import java.util.Date
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import play.mvc.Http.Response
import play.mvc.results.{NotFound, RenderBinary, Result}
import play.utils.Utils
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.Header
import com.thebuzzmedia.imgscalr.Scalr
import play.mvc.Controller

/**
 * TODO retrieve thumbnails via ImageDisplay (consolidate)
 *
 * @author Sjoerd Siebinga <sjoerd.siebinga@gmail.com>
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 * @since 1/2/11 10:09 PM
 */
object ImageCache extends Controller {

  val imageCacheService = new ImageCacheService

  def image(id: String, size: String): Result = {
    imageCacheService.retrieveImageFromCache(id, size, response)
  }

}

class ImageCacheService extends HTTPClient with Thumbnail {

  // General Settings
  val thumbnailSizeString = "BRIEF_DOC"
  private val log: Logger = Logger.getLogger("ImageCacheService")

  def retrieveImageFromCache(url: String, sizeString: String, response: Response): Result = {
    // catch try block to harden the application and always give back a 404 for the application
    try {
      require(url != null)
      require(url != "noImageFound")
      require(!url.isEmpty)
      findOrInsert(sanitizeUrl(url), isThumbnail(Option(sizeString)), response)
    } catch {
      case ia: IllegalArgumentException =>
        log.error("problem with processing this url: \"" + url + "\"")
        respondWithNotFound(url)
      case ex: Exception =>
        log.error("unable to find image: \"" + url + "\"\n" + ex.getStackTraceString)
        respondWithNotFound(url)
    }
  }

  def findOrInsert(url: String, thumbnail: Boolean, response: Response): Result = {
    val image: Option[GridFSDBFile] = findImageInCache(url, thumbnail)
    if (!image.isDefined) {
      log info ("image not found, attempting to store it in the cache based on URL: " + url)
      val item = storeImage(url)
      if (item.available) {
        val storedImage = findImageInCache(url, thumbnail)
        ImageCacheService.setImageCacheControlHeaders(storedImage.get, response)
        respondWithImageStream(storedImage.get)
      }
      else {
        log info ("unable to store " + url)
        respondWithNotFound(url)
      }
    }
    else {
      ImageCacheService.setImageCacheControlHeaders(image.get, response)
      respondWithImageStream(image.get)
    }
  }

  def findImageInCache(url: String, thumbnail: Boolean = false): Option[GridFSDBFile] = {
    log info ("attempting to retrieve %s: " format (if (thumbnail) "thumbnail for image" else "image") + " " + url)

    val image: Option[GridFSDBFile] = if (thumbnail) {
      imageCacheStore.findOne(MongoDBObject(FILE_FILENAME_FIELD -> url)).headOption
    } else {
      imageCacheStore.findOne(url)
    }

    if (image.isDefined) {
      image.get.put("lastViewed", new Date)
      val viewed = image.get("viewed")
      if (viewed != null) image.get.put("viewed", viewed.asInstanceOf[Int] + 1) else image.get.put("viewed", 1)
      image.get.save
    }
    image
  }

  private def sanitizeUrl(url: String): String = {
    val sanitizeUrl: String = url.replaceAll("""\\""", "%5C").replaceAll("\\[", "%5B").replaceAll("\\]", "%5D")
    sanitizeUrl
  }

  private def storeImage(url: String): CachedItem = {
    val image = retrieveImageFromUrl(url)
    if (image.storable) {
      val inputFile = imageCacheStore.createFile(image.dataAsStream, image.url)
      inputFile.contentType = image.contentType
      inputFile put("viewed", 0)
      inputFile put("lastViewed", new Date)
      inputFile.save

      val cachedImage = imageCacheStore.findOne(image.url).getOrElse(return CachedItem(false, null))
      createThumbnails(cachedImage, imageCacheStore)
      CachedItem(true, inputFile)
    } else {
      CachedItem(false, null)
    }
  }

  def retrieveImageFromUrl(url: String): WebResource = {
    val method = new GetMethod(url)
    getHttpClient executeMethod (method)
    method.getResponseHeaders.foreach(header => log debug (header))
    val storable = isStorable(method)
    WebResource(url, method.getResponseBodyAsStream, storable._1, storable._2)
  }

  def isStorable(method: GetMethod) = {
    val contentType: Header = method.getResponseHeader("Content-Type")
    val contentLength: Header = method.getResponseHeader("Content-Length")
    val mimeTypes = List("image/png", "image/jpeg", "image/jpg", "image/gif", "image/tiff", "image/pjpeg")
    //todo build a size check in later
    (mimeTypes.contains(contentType.getValue.toLowerCase), contentType.getValue)
  }

  private def respondWithNotFound(url: String): Result = {
    new NotFound(format(
      """<?xml encoding="utf-8"?>
     <error>
       <message>Unable to retrieve your image (%s) through the CacheProxy</message>
     </error> """, url))
  }

  private def respondWithImageStream(image: GridFSDBFile): Result = {
    new RenderBinary(image.inputStream, "cachedImage", image.contentType, true)
  }

  private def isThumbnail(thumbnail: Option[String]): Boolean = {
    thumbnail.getOrElse(false) == thumbnailSizeString
  }
}

object ImageCacheService {

  val cacheDuration = 60 * 60 * 24

  def createThumbnail(sourceStream: InputStream, thumbnailWidth: Int, boundingBox: Boolean = false): InputStream = {
    val thumbnail: BufferedImage = resizeImage(sourceStream, thumbnailWidth, boundingBox)
    val os: ByteArrayOutputStream = new ByteArrayOutputStream()
    ImageIO.write(thumbnail, "jpg", os)
    new ByteArrayInputStream(os.toByteArray)
  }

  def setImageCacheControlHeaders(image: GridFSDBFile, response: Response, duration: Int = cacheDuration) {
    response.setContentTypeIfNotSet(image.contentType)
    val now = System.currentTimeMillis();
    //    response.cacheFor(image.underlying.getMD5, duration.toString + "s", now)
    // overwrite the Cache-Control header and add the must-revalidate directive by hand
    response.setHeader("Cache-Control", "max-age=%s, must-revalidate".format(duration))
    response.setHeader("Expires", Utils.getHttpDateFormatter.format(new Date(now + duration * 1000)))
  }


  private def resizeImage(imageStream: InputStream, width: Int, boundingBox: Boolean): BufferedImage = {
    val bufferedImage: BufferedImage = ImageIO.read(imageStream)
    if (boundingBox) {
      Scalr.resize(bufferedImage, width, width)
    } else {
      Scalr.resize(bufferedImage, Scalr.Mode.FIT_TO_WIDTH, width)
    }
  }


}

case class WebResource(url: String, dataAsStream: InputStream, storable: Boolean, contentType: String)

case class CachedItem(available: Boolean, item: GridFSInputFile)

