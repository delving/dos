package processors

import models.dos.Task
import play.mvc.Util
import org.bson.types.ObjectId
import controllers.dos._
import java.io.{FileInputStream, File}

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object ThumbnailProcessor extends Processor with Thumbnail {
  def process(task: Task, params: Map[String, AnyRef]) {
    val p = new File(task.path)
    if (!p.exists()) {
      error(task, "Path '%s' does not exist or is unreachable".format(task.path))
    } else {

      val sizes = params("sizes").asInstanceOf[List[Int]]

      info(task, "Starting to generate thumbnails for path '%s' for sizes %s".format(task.path, sizes.mkString(", ")))

      val images = p.listFiles().filter(f => isImage(f.getName))

      Task.setTotalItems(task, images.size)

      images foreach {
        image => try {
          for (s <- sizes) storeThumbnail(image, s)
          Task.incrementProcessedItems(task, 1)
        } catch {
          case _ => error(task, "Error creating thumbnail for image '%s'".format(image.getAbsolutePath))
        }
        info(task, "Created thumbnail for image '%s'".format(image.getAbsolutePath))
      }
    }
  }

  def isImage(name: String) = name.contains(".") && !name.startsWith(".") && (
          name.split("\\.")(1).toLowerCase match {
            case "jpg" | "tif" | "tiff" => true
            case _ => false
          })

  @Util private def storeThumbnail(image: File, width: Int): Option[ObjectId] = {
    val imageName = if (image.getName.indexOf(".") > 0) image.getName.substring(0, image.getName.indexOf(".")) else image.getName
    storeThumbnail(new FileInputStream(image), image.getName, width, fileStore, Map(ORIGIN_PATH_FIELD -> image.getAbsolutePath, IMAGE_ID_FIELD -> imageName))._2
  }

}