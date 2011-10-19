package controllers.dos

import org.bson.types.ObjectId

/**
 * A File Stored by the FileStore
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
case class StoredFile(id: ObjectId, name: String, contentType: String, length: Long, thumbnail: Option[ObjectId]) {
  def thumbnailUrl = thumbnail match {
    case Some(fid) => "/file/" + fid
    case None => ""
  }
}