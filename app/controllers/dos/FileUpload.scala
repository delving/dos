package controllers.dos

import com.mongodb.casbah.Imports._
import play.mvc.results.Result
import collection.JavaConversions._
import com.mongodb.gridfs.GridFSFile
import java.util.List
import play.data.Upload
import com.mongodb.casbah.gridfs.GridFSDBFile
import play.mvc.{Controller, Util}
import org.bson.types.ObjectId
import extensions.dos.Extensions

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object FileUpload extends Controller with Extensions with Thumbnail {

  // ~~ public HTTP API

  /**
   * POST handler for uploading a file, given an UID that will be attached to it.
   * If the uploaded file is an image, thumbnails are created for it.
   * The response contains a JSON-Encoded array of objects representing the uploaded file.
   */
  def uploadFile(uid: String): Result = {
    val uploads: List[Upload] = request.args.get("__UPLOADS").asInstanceOf[java.util.List[play.data.Upload]]
    uploadFileInternal(uid, asScalaIterable(uploads))
  }

  /**
   * DELETE handler for removing a file given an ID
   */
  def deleteFile(id: String): Result = {
    val oid = if (ObjectId.isValid(id)) new ObjectId(id) else (return Error("Invalid file ID " + id))
    fileStore.find(oid) foreach {
      toDelete =>
      // remove thumbnails
        fileStore.find(MongoDBObject(FILE_POINTER_FIELD -> oid)) foreach {
          t =>
            fileStore.remove(t.getId.asInstanceOf[ObjectId])
        }
        // remove the file itself
        fileStore.remove(oid)
    }
    Ok
  }


  // ~~ public Scala API

  @Util def getFilesForUID(uid: String): Seq[StoredFile] = fileStore.find(MongoDBObject("uid" -> uid)) map {
    f => {
      val id = f.getId.asInstanceOf[ObjectId]
      val thumbnail = if (isImage(f)) {
        fileStore.findOne(MongoDBObject(FILE_POINTER_FIELD -> id)) match {
          case Some(t) => Some(t.id.asInstanceOf[ObjectId])
          case None => None
        }
      } else {
        None
      }
      StoredFile(id, f.getFilename, f.getContentType, f.getLength, thumbnail)
    }
  }

  /**
   * Attaches all files to an object, given the upload UID
   */
  @Util def markFilesAttached(uid: String, objectId: ObjectId) {
    fileStore.find(MongoDBObject("uid" -> uid)) map {
      f =>
      // yo listen up, this ain't implemented in the mongo driver and throws an UnsupportedOperationException
      // f.removeField("uid")
        f.put(UPLOAD_UID_FIELD, "")
        f.put(ITEM_POINTER_FIELD, objectId)
        f.save()
    }
  }

  /**
   * For all thumbnails and images of a particular file, sets their pointer to a given item, thus enabling direct lookup
   * using the item id.
   */
  @Util def activateThumbnails(fileId: ObjectId, itemId: ObjectId) {
    val thumbnails = fileStore.find(MongoDBObject(FILE_POINTER_FIELD -> fileId))

    // deactive old thumbnails
    fileStore.find(MongoDBObject(THUMBNAIL_ITEM_POINTER_FIELD -> itemId)) foreach {
      theOldOne =>
        theOldOne.put(THUMBNAIL_ITEM_POINTER_FIELD, "")
        theOldOne.save()
    }

    // activate new thumbnails
    thumbnails foreach {
      thumb =>
        thumb.put(THUMBNAIL_ITEM_POINTER_FIELD, itemId)
        thumb.save()
    }

    // deactivate old image
    fileStore.findOne(MongoDBObject(IMAGE_ITEM_POINTER_FIELD -> itemId)) foreach {
      theOldOne =>
        theOldOne.put(IMAGE_ITEM_POINTER_FIELD, "")
        theOldOne.save
    }

    // activate new default image
    fileStore.findOne(fileId) foreach {
      theNewOne =>
        theNewOne.put(IMAGE_ITEM_POINTER_FIELD, itemId)
        theNewOne.save
    }
  }

  @Util def isImage(f: GridFSFile) = f.getContentType.contains("image")


  // ~~~ PRIVATE


  @Util private def uploadFileInternal(uid: String, uploads: Iterable[Upload]): Result = {
    val uploadedFiles = for (upload: play.data.Upload <- uploads) yield {
      val f = fileStore.createFile(upload.asStream())
      f.filename = upload.getFileName
      f.contentType = upload.getContentType
      f.put(UPLOAD_UID_FIELD, uid)
      f.save

      if (f._id == None) return Error("Error saving uploaded file")

      // if this is an image, create a thumbnail for it so we can display it on the fly
      val thumbnailUrl: String = if (f.contentType.contains("image")) {
        fileStore.findOne(f._id.get) match {
          case Some(storedFile) =>
            val thumbnails = createThumbnails(storedFile, fileStore)
            if (thumbnails.size > 0) "/file/" + thumbnails.get(80).getOrElse(emptyThumbnail) else emptyThumbnail
          case None => ""
        }
      } else ""

      FileUploadResponse(upload.getFileName, upload.getSize.longValue(), "/file/" + f._id.get, thumbnailUrl, "/file/" + f._id.get)
    }
    Json(uploadedFiles)
  }

}