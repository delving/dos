package processors

import models.dos.Task
import java.io.File

/**
 * 
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

trait ThumbnailCreationProcessor extends Processor {

  def process(task: Task, processorParams: Map[String, AnyRef]) {
    val p = new File(task.path)
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

    Task.setTotalItems(task, images.size * sizes.length)

    for (s <- sizes) createThumbnailsForSize(images, s, task, orgId, collectionId)
  }

  protected def createThumbnailsForSize(images: Seq[File], width: Int, task: Task, orgId: String, collectionId: String)

}