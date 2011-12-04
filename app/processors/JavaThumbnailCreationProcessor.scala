package processors

import org.bson.types.ObjectId
import controllers.dos._
import java.io.{FileInputStream, File}
import models.dos.Task

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object JavaThumbnailCreationProcessor extends ThumbnailCreationProcessor with Thumbnail {


  protected def createThumbnailsForSize(images: Seq[File], width: Int, task: Task, orgId: String, collectionId: String) {
    for (image <- images; if(!task.isCancelled)) {
      try {
        val id = createThumbnailFromFile(image, width, task._id, orgId, collectionId)
        info(task, "Created thumbnail of size '%s' for image '%s'".format(width, image.getAbsolutePath), Some(image.getAbsolutePath), Some(id.toString))
      } catch {
        case t => error(task, "Error creating thumbnail for image '%s': %s".format(image.getAbsolutePath, t.getMessage), Some(image.getAbsolutePath))
      } finally {
        Task.incrementProcessedItems(task, 1)
      }
    }
  }

  protected def createThumbnailFromFile(image: File, width: Int, taskId: ObjectId, orgId: String, collectionId: String): ObjectId = {
    val imageName = getImageName(image.getName)
    createThumbnailFromStream(new FileInputStream(image), image.getName, width, fileStore, Map(
      ORIGIN_PATH_FIELD -> image.getAbsolutePath,
      IMAGE_ID_FIELD -> imageName,
      TASK_ID -> taskId,
      ORGANIZATION_IDENTIFIER_FIELD -> orgId,
      COLLECTION_IDENTIFIER_FIELD -> collectionId
    ))._2
  }

}