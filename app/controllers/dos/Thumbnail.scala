package controllers.dos

import play.mvc.Util
import org.bson.types.ObjectId
import com.mongodb.casbah.gridfs.{GridFS, GridFSDBFile}
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import com.thebuzzmedia.imgscalr.Scalr
import com.mongodb.casbah.commons.MongoDBObject

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

trait Thumbnail {

  @Util protected def createThumbnails(image: GridFSDBFile, store: GridFS, globalParams: Map[String, String] = Map.empty[String, String]): Map[Int, ObjectId] = {
    thumbnailSizes.map {
      size => storeThumbnail(image.inputStream, image.filename, size._2, store, globalParams + (FILE_POINTER_FIELD -> image._id.get))
    }
  }

  @Util protected def storeThumbnail(imageStream: InputStream, filename: String, width: Int, store: GridFS, params: Map[String, AnyRef] = Map.empty[String, AnyRef]): (Int, ObjectId) = {
    val thumbnailStream = createThumbnail(imageStream, width)
    val thumbnail = store.createFile(thumbnailStream)
    thumbnail.filename = filename
    thumbnail.contentType = "image/jpeg"
    thumbnail.put (THUMBNAIL_WIDTH_FIELD, width.asInstanceOf[AnyRef])
    params foreach { p => thumbnail.put(p._1, p._2)}
    thumbnail.save
    (width, thumbnail._id.get)
  }

  @Util protected def deleteBatchImportThumbnails(path: String, spec: String, org: String, store: GridFS) {
    val thumbs = store.find(MongoDBObject(ORIGIN_PATH_FIELD -> path.r, spec -> spec, org -> org))
    thumbs foreach {
      t => store.remove(t.getId.asInstanceOf[ObjectId])
    }
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