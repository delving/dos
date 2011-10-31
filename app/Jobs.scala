package jobs {

import models.dos.{TaskType, TaskState, Task}
import util.Logging
import play.jobs.{Every, Job}
import processors._

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

@Every("10s")
class TaskQueueJob extends Job with Logging {

  override def doJob() {

    val head = Task.list(TaskState.QUEUED).headOption
    head foreach {
      task =>
        Task.start(task)
        try {
          task.taskType match {
            case TaskType.THUMBNAILS_CREATE => GMThumbnailCreationProcessor.process(task, Map("sizes" -> controllers.dos.thumbnailSizes.values.toList))
//            case TaskType.THUMBNAILS_CREATE => JavaThumbnailCreationProcessor.process(task, Map("sizes" -> controllers.dos.thumbnailSizes.values.toList))
            case TaskType.THUMBNAILS_DELETE => ThumbnailDeletionProcessor.process(task)
            case TaskType.FLATTEN => TIFFlatteningProcessor.process(task)
            case TaskType.TILES => PTIFTilingProcessor.process(task)
          }
        } catch {
          case t =>
            t.printStackTrace()
            error(task, "Error running task of kind '%s' on path '%s': %s".format(task.taskType.name, task.path, t.getMessage))
        } finally {
          Task.finish(task)
        }
    }
  }


}


}
