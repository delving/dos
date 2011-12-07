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

import play.mvc.Util
import org.bson.types.ObjectId
import com.mongodb.casbah.gridfs.{GridFS, GridFSDBFile}
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import com.thebuzzmedia.imgscalr.Scalr

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

trait Thumbnail {

  @Util protected def createThumbnails(image: GridFSDBFile, store: GridFS, globalParams: Map[String, String] = Map.empty[String, String]): Map[Int, ObjectId] = {
    thumbnailSizes.map {
      size => createThumbnailFromStream(image.inputStream, image.filename, size._2, store, globalParams + (FILE_POINTER_FIELD -> image._id.get))
    }
  }

  @Util protected def createThumbnailFromStream(imageStream: InputStream, filename: String, width: Int, store: GridFS, params: Map[String, AnyRef] = Map.empty[String, AnyRef]): (Int, ObjectId) = {
    val resizedStream = createThumbnail(imageStream, width)
    storeThumbnail(resizedStream, filename, width, store, params)
  }

  @Util protected def storeThumbnail(thumbnailStream: InputStream, filename: String, width: Int, store: GridFS, params: Map[String, AnyRef] = Map.empty[String, AnyRef]): (Int, ObjectId) = {
    val thumbnail = store.createFile(thumbnailStream)
    thumbnail.filename = filename
    thumbnail.contentType = "image/jpeg"
    thumbnail.put (THUMBNAIL_WIDTH_FIELD, width.asInstanceOf[AnyRef])
    params foreach { p => thumbnail.put(p._1, p._2)}
    thumbnail.save
    (width, thumbnail._id.get)
  }

  private def createThumbnail(sourceStream: InputStream, thumbnailWidth: Int, boundingBox: Boolean = true): InputStream = {
    val thumbnail: BufferedImage = resizeImage(sourceStream, thumbnailWidth, boundingBox)
    val os: ByteArrayOutputStream = new ByteArrayOutputStream()
    ImageIO.write(thumbnail, "jpg", os)
    new ByteArrayInputStream(os.toByteArray)
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