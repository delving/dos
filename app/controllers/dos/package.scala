package controllers {

import java.io.File
import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.gridfs.GridFS

package object dos {

  // ~~ connection to mongo
  val fileStore = MongoConnection().getDB(play.Play.configuration.getProperty("db.fileStore.name"))
  val fs = GridFS(fileStore)


  val emptyThumbnail = "/public/images/dummy-object.png"
  val emptyThumbnailFile = new File(play.Play.applicationPath + emptyThumbnail)

  val DEFAULT_THUMBNAIL_WIDTH = 220
  val thumbnailSizes = Map("tiny" -> 80, "small" -> 220)

  val THUMBNAIL_WIDTH_FIELD = "thumbnail_width"

  // ~~ images uploaded directly via culturehub
  val UPLOAD_UID_FIELD = "uid" // temporary UID given to files that are not yet attached to an object after upload
  val OBJECT_POINTER_FIELD = "object_id" // pointer to the owning object, for cleanup
  val FILE_POINTER_FIELD = "original_file" // pointer from a thumbnail to its parent file
  val IMAGE_OBJECT_POINTER_FIELD = "image_object_id" // pointer from an chosen image to its object, useful to lookup an image by object ID
  val THUMBNAIL_OBJECT_POINTER_FIELD = "thumbnail_object_id" // pointer from a chosen thumbnail to its object, useful to lookup a thumbnail by object ID

  // ~~ images stored locally (file system)
  val IMAGE_ID_FIELD = "file_id"
  val ORIGIN_PATH_FIELD = "origin_path"


}

}