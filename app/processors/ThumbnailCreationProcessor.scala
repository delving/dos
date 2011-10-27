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

object ThumbnailCreationProcessor extends Processor with Thumbnail {

  def process(task: Task, processorParams: Map[String, AnyRef]) {
    val p = new File(task.path)
    if (!p.exists()) {
      error(task, "Path '%s' does not exist or is unreachable".format(task.path))
    } else {

      val collectionId = task.params.get(controllers.dos.COLLECTION_IDENTIFIER_FIELD).getOrElse({
        error(task, "No spec passed for task " + task)
        return
      })
      val orgId = task.params.get(controllers.dos.ORGANIZATION_IDENTIFIER_FIELD).getOrElse({
        error(task, "No org passed for task " + task)
        return
      })

      val sizes = processorParams("sizes").asInstanceOf[List[Int]]

      info(task, "Starting to generate thumbnails for path '%s' for sizes %s".format(task.path, sizes.mkString(", ")))

      val images = p.listFiles().filter(f => isImage(f.getName))

      Task.setTotalItems(task, images.size)

      for (image <- images; if (!task.isCancelled)) {
        try {
          for (s <- sizes) {
            val id = createThumbnailFromFile(image, s, task._id, orgId, collectionId)
            info(task, "Created thumbnail of size '%s' for image '%s'".format(s, image.getAbsolutePath), Some(image.getAbsolutePath), Some(id.toString))
          }
          Task.incrementProcessedItems(task, 1)
        } catch {
          case t => error(task, "Error creating thumbnail for image '%s': %s".format(image.getAbsolutePath, t.getMessage), Some(image.getAbsolutePath))
        }
      }
    }
  }

  @Util private def createThumbnailFromFile(image: File, width: Int, taskId: ObjectId, orgId: String, collectionId: String): ObjectId = {
    val imageName = if (image.getName.indexOf(".") > 0) image.getName.substring(0, image.getName.indexOf(".")) else image.getName
    storeThumbnail(new FileInputStream(image), image.getName, width, fileStore, Map(
      ORIGIN_PATH_FIELD -> image.getAbsolutePath,
      IMAGE_ID_FIELD -> imageName,
      TASK_ID -> taskId,
      ORGANIZATION_IDENTIFIER_FIELD -> orgId,
      COLLECTION_IDENTIFIER_FIELD -> collectionId
    ))._2
  }

}