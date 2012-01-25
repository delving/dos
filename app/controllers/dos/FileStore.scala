/*
 * Copyright 2011 Delving B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.dos

import org.bson.types.ObjectId
import play.mvc.results.{RenderBinary, Result}
import play.mvc.{Util, Controller}
import com.mongodb.gridfs.GridFSDBFile
import com.mongodb.casbah.commons.MongoDBObject

/**
 * Common controller for handling files, no matter from where.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object FileStore extends Controller {

  // ~~~ public HTTP API

  def get(id: String): Result = {
    if (!ObjectId.isValid(id)) return Error("Invalid ID " + id)
    val oid = new ObjectId(id)
    val file = fileStore.findOne(oid) getOrElse (return NotFound("Could not find file with ID " + id))
    new RenderBinary(file.inputStream, file.filename, file.length, file.contentType, false)
  }


  // ~~~ public scala API

  @Util def getFilesForItemId(id: ObjectId): List[StoredFile] = fileStore.find(MongoDBObject(ITEM_POINTER_FIELD -> id)).map(fileToStoredFile).toList

  // ~~~ private

  private[dos] def fileToStoredFile(f: GridFSDBFile) = {
    val id = f.getId.asInstanceOf[ObjectId]
    val thumbnail = if (FileUpload.isImage(f)) {
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