package controllers.dos

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.Implicits._
import play.mvc.{Util, Controller}
import org.bson.types.ObjectId
import com.mongodb.gridfs.GridFSDBFile
import play.mvc.results.{RenderBinary, Result}

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object ImageDisplay extends Controller {

  // ~~ public HTTP API

  /**
   * Display a thumbnail given an ID and a width
   */
  def displayThumbnail(id: String, width: String = ""): Result = {
    val thumbnailWidth = if (thumbnailSizes.contains(width)) {
      thumbnailSizes(width)
    } else {
      try {
        Integer.parseInt(width)
      } catch {
        case _ => DEFAULT_THUMBNAIL_WIDTH
      }
    }
    renderImage(id, true, thumbnailWidth)
  }

  /**
   * Display an image given an ID
   */
  def displayImage(id: String): Result = renderImage(id, false)


  // ~~ public Scala API

  @Util def imageExists(objectId: ObjectId) = fileStore.find(MongoDBObject(IMAGE_ITEM_POINTER_FIELD -> objectId)).nonEmpty


  // ~~ PRIVATE

  @Util private def renderImage(id: String, thumbnail: Boolean, thumbnailWidth: Int = DEFAULT_THUMBNAIL_WIDTH): Result = {

    val (field, oid) = if (ObjectId.isValid(id)) {
      (if (thumbnail) THUMBNAIL_ITEM_POINTER_FIELD else IMAGE_ITEM_POINTER_FIELD, new ObjectId(id))
    } else {
      // string identifier - for e.g. ingested images
      (IMAGE_ID_FIELD, id)
    }

    val query = if (thumbnail) MongoDBObject(field -> oid, THUMBNAIL_WIDTH_FIELD -> thumbnailWidth) else MongoDBObject(field -> oid)

    val image: Option[GridFSDBFile] = fileStore.findOne(query) match {
      case Some(file) => {
        ImageCacheService.setImageCacheControlHeaders(file, response, 60 * 15)
        Some(file.underlying)
      }
      case None if (thumbnail) => {
        // try to find the next fitting size
        fileStore.find(MongoDBObject(field -> oid)).sortWith((a, b) => a.get(THUMBNAIL_WIDTH_FIELD).asInstanceOf[Int] > b.get(THUMBNAIL_WIDTH_FIELD).asInstanceOf[Int]).headOption match {
          case Some(t) => Some(t)
          case None => return new RenderBinary(emptyThumbnailFile, emptyThumbnailFile.getName, true)
        }
      }
      case None => if (thumbnail) return new RenderBinary(emptyThumbnailFile, emptyThumbnailFile.getName, true) else None

    }
    image match {
      case None => NotFound
      case Some(t) => new RenderBinary(t.inputStream, t.filename, t.length, t.contentType, true)
    }
  }
}