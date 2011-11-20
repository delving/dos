package controllers.dos

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.Implicits._
import play.mvc.{Util, Controller}
import org.bson.types.ObjectId
import com.mongodb.gridfs.GridFSDBFile
import play.mvc.results.{RenderBinary, Result}
import com.mongodb.casbah.gridfs.GridFS
import play.Logger

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object ImageDisplay extends Controller with RespondWithDefaultImage {

  // ~~ public HTTP API

  /**
   * Display a thumbnail given an ID and a width
   */
  def displayThumbnail(id: String, orgId: String, collectionId: String, width: String = "", browse: Boolean = false, fileId:Boolean = false): Result = renderImage(id = id, thumbnail = true, orgId = orgId, collectionId = collectionId, thumbnailWidth = thumbnailWidth(width), browse = browse, isFileId = fileId)

  /**
   * Display an image given an ID
   */
  def displayImage(id: String, fileId: Boolean = false): Result = renderImage(id = id, thumbnail = false, isFileId = fileId)


  // ~~ public Scala API

  @Util def imageExists(objectId: ObjectId) = fileStore.find(MongoDBObject(IMAGE_ITEM_POINTER_FIELD -> objectId)).nonEmpty


  // ~~ PRIVATE

  @Util private[dos] def renderImage(id: String, orgId: String = "", collectionId: String = "", thumbnail: Boolean, thumbnailWidth: Int = DEFAULT_THUMBNAIL_WIDTH, store: GridFS = fileStore, browse: Boolean = false, isFileId: Boolean = false): Result = {

    val baseQuery: MongoDBObject = if (ObjectId.isValid(id)) {
      val f: String = if (isFileId && thumbnail) {
        FILE_POINTER_FIELD
      } else if(isFileId && !thumbnail) {
        "_id"
      } else if(thumbnail && !isFileId) {
        THUMBNAIL_ITEM_POINTER_FIELD
      } else {
        IMAGE_ITEM_POINTER_FIELD
      }
      MongoDBObject(f -> new ObjectId(id))
    } else {
      // we have a string identifier - from ingested images
      // in order to resolve these we want:
      // - the organization the image belongs to
      // - the collection identifier (spec) the image belongs to
      // - the image identifier (file name minus file extension)

      val idIsUrl = id.startsWith("http://")
      var incomplete = false
      if(!idIsUrl && !browse && (orgId == null || orgId.isEmpty)) {
        Logger.warn("Attempting to display image '%s' with string identifier without orgId".format(id))
        incomplete = true
      }
      if(!idIsUrl && !browse && (collectionId == null || collectionId.isEmpty)) {
        Logger.warn("Attempting to display image '%s' with string identifier without collectionId".format(id))
        incomplete = true
      }
      if(browse) {
        MongoDBObject(ORIGIN_PATH_FIELD -> id)
      }
      else if(idIsUrl || incomplete) {
        MongoDBObject(IMAGE_ID_FIELD -> id)
      }
      else {
        MongoDBObject(IMAGE_ID_FIELD -> id, ORGANIZATION_IDENTIFIER_FIELD -> orgId, COLLECTION_IDENTIFIER_FIELD -> collectionId)
      }
    }

    val query: MongoDBObject = if (thumbnail) (baseQuery ++ MongoDBObject(THUMBNAIL_WIDTH_FIELD -> thumbnailWidth)) else baseQuery

    val image: Option[GridFSDBFile] = store.findOne(query) match {
      case Some(file) => {
        ImageCacheService.setImageCacheControlHeaders(file, response, 60 * 15)
        Some(file.underlying)
      }
      case None if (thumbnail) => {
        // try to find the next fitting size
        store.find(baseQuery).sortWith((a, b) => a.get(THUMBNAIL_WIDTH_FIELD).asInstanceOf[Int] > b.get(THUMBNAIL_WIDTH_FIELD).asInstanceOf[Int]).headOption match {
          case Some(t) => Some(t)
          case None => None//return new RenderBinary(emptyThumbnailFile, emptyThumbnailFile.getName, true)
        }
      }
      case None => None //if (thumbnail) return new RenderBinary(emptyThumbnailFile, emptyThumbnailFile.getName, true) else None

    }
    image match {
      case None =>
          withDefaultFromRequest(request, new play.mvc.results.NotFound(request.querystring), thumbnail, thumbnailWidth.toString, false)
      case Some(t) => new RenderBinary(t.inputStream, t.filename, t.length, t.contentType, true)
    }
  }

  @Util private[dos] def thumbnailWidth(width: String): Int = {
    width match {
      case null => DEFAULT_THUMBNAIL_WIDTH
      case "" => DEFAULT_THUMBNAIL_WIDTH
      case w if thumbnailSizes.contains(width) => thumbnailSizes(w)
      case w =>
        try {
          Integer.parseInt(width)
        } catch {
          case _ => DEFAULT_THUMBNAIL_WIDTH
        }
    }
  }
}