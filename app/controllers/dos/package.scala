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

package controllers {

import java.io.File
import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.gridfs.GridFS
import play.Play
import models._
package object dos {

  // ~~ connection to mongo
  val testStoreConnection = MongoConnection().getDB("testDoSStore")
  val testing = Play.mode == Play.Mode.DEV && Play.id == "test"

  val fileStoreConnnection = createConnection(Play.configuration.getProperty("dos.db.fileStore.name", "fileStore"))
  val fileStore = if(testing) GridFS(testStoreConnection) else GridFS(fileStoreConnnection)

  val imageCacheStoreConnection = createConnection(Play.configuration.getProperty("dos.db.imageCache.name", "imageCache"))
  val imageCacheStore: GridFS = if(testing) GridFS(testStoreConnection) else GridFS(imageCacheStoreConnection)

  val emptyThumbnail = Play.configuration.getProperty("dos.emptyImagePath", "/public/dos/images/dummy-object.png")
  val emptyThumbnailFile = new File(Play.applicationPath, emptyThumbnail)

  val DEFAULT_THUMBNAIL_WIDTH = 220
  val thumbnailSizes = Map("tiny" -> 80, "thumbnail" -> 100, "smaller" -> 180, "small" -> 220, "story" -> 350, "big" -> 500)

  val THUMBNAIL_WIDTH_FIELD = "thumbnail_width"

  // ~~ images uploaded directly via culturehub
  val UPLOAD_UID_FIELD = "uid" // temporary UID given to files that are not yet attached to an object after upload
  val ITEM_POINTER_FIELD = "object_id" // pointer to the owning item, for cleanup
  val FILE_POINTER_FIELD = "original_file" // pointer from a thumbnail to its parent file
  val IMAGE_ITEM_POINTER_FIELD = "image_object_id" // pointer from an chosen image to its item, useful to lookup an image by item ID
  val THUMBNAIL_ITEM_POINTER_FIELD = "thumbnail_object_id" // pointer from a chosen thumbnail to its item, useful to lookup a thumbnail by item ID

  // ~~ images stored locally (file system)
  val IMAGE_ID_FIELD = "file_id" // identifier (mostly file name without extension) of an image, or of a thumbnail (to refer to the parent image)
  val ORIGIN_PATH_FIELD = "origin_path" // path from where this thumbnail has been ingested

  val ORGANIZATION_IDENTIFIER_FIELD = "orgId"
  val COLLECTION_IDENTIFIER_FIELD = "collectionId"

  val TASK_ID = "task_id" // mongo id of the task that led to the creation of this thing

}

}