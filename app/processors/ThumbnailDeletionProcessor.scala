package processors

import models.dos.Task
import java.io.File
import controllers.dos.Thumbnail

/**
 * 
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object ThumbnailDeletionProcessor extends Processor with Thumbnail {
  def process(task: Task, processorParams: Map[String, AnyRef]) {
    val p = new File(task.path)
    if (!p.exists()) {
      error(task, "Path '%s' does not exist or is unreachable".format(task.path))
    } else {
      if(!task.params.contains("spec") || !task.params.contains("org")) {
        error(task, "No spec or organisation provided")
      } else {
        deleteBatchImportThumbnails(task.path, task.params("spec").toString, task.params("org").toString, controllers.dos.fileStore)
      }
    }
  }
}