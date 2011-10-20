package controllers.dos

import play.mvc.Util
import org.bson.types.ObjectId
import com.mongodb.casbah.gridfs.{GridFS, GridFSDBFile}

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

trait Thumbnail {

  @Util protected def createThumbnails(image: GridFSDBFile, store: GridFS, globalParams: Map[String, String] = Map.empty[String, String]): Map[Int, Option[ObjectId]] = {
    thumbnailSizes.map {
      size => createThumbnail(image, size._2, store, globalParams)
    }
  }

  /**
   * Creates thumbnail and stores a pointer to the original image
   */
  @Util protected def createThumbnail(image: GridFSDBFile, width: Int, store: GridFS, params: Map[String, String] = Map.empty[String, String]) = {
    val thumbnailStream = ImageCacheService.createThumbnail(image.inputStream, width)
    val thumbnail = store.createFile(thumbnailStream)
    thumbnail.filename = image.filename
    thumbnail.contentType = "image/jpeg"
    thumbnail.put(FILE_POINTER_FIELD, image._id)
    thumbnail.put (FILE_FILENAME_FIELD, image.filename)
    thumbnail.put (THUMBNAIL_WIDTH_FIELD, width.asInstanceOf[AnyRef])
    params foreach { p => thumbnail.put(p._1, p._2)}
    thumbnail.save
    (width, thumbnail._id)
  }

}