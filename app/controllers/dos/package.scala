package controllers {

import java.io.File
import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.gridfs.GridFS
import play.Play

package object dos {

  // ~~ connection to mongo
  val fileStoreConnnection = MongoConnection().getDB(Play.configuration.getProperty("db.fileStore.name", "fileStore"))
  val fileStore = GridFS(fileStoreConnnection)

  val imageCacheStoreConnection = MongoConnection().getDB("imageCache")
  val imageCacheStore: GridFS = GridFS(imageCacheStoreConnection)

  val emptyThumbnail = "/public/images/dummy-object.png"
  val emptyThumbnailFile = new File(Play.modules.get("dos").getRealFile.getAbsolutePath + emptyThumbnail)

  val DEFAULT_THUMBNAIL_WIDTH = 220
  val thumbnailSizes = Map("tiny" -> 80, "small" -> 220)

  val THUMBNAIL_WIDTH_FIELD = "thumbnail_width"

  // ~~ images uploaded directly via culturehub
  val UPLOAD_UID_FIELD = "uid" // temporary UID given to files that are not yet attached to an object after upload
  val ITEM_POINTER_FIELD = "object_id" // pointer to the owning item, for cleanup
  val FILE_POINTER_FIELD = "original_file" // pointer from a thumbnail to its parent file
  val FILE_FILENAME_FIELD = "original_file_name" // in a thumbnail, name of the parent file
  val IMAGE_ITEM_POINTER_FIELD = "image_object_id" // pointer from an chosen image to its item, useful to lookup an image by item ID
  val THUMBNAIL_ITEM_POINTER_FIELD = "thumbnail_object_id" // pointer from a chosen thumbnail to its item, useful to lookup a thumbnail by item ID

  // ~~ images stored locally (file system)
  val IMAGE_ID_FIELD = "file_id"
  val ORIGIN_PATH_FIELD = "origin_path"


}

}