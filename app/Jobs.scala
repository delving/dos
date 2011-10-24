package jobs {

import models.dos.{TaskType, TaskState, Task}
import util.Logging
import play.jobs.{Every, Job}
import processors.{TIFFlatteningProcessor, PTIFTilingProcessor, ThumbnailProcessor}

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
            case TaskType.THUMBNAILS => ThumbnailProcessor.process(task, Map("sizes" -> controllers.dos.thumbnailSizes.values.toList))
            case TaskType.FLATTEN => TIFFlatteningProcessor.process(task)
            case TaskType.TILES => PTIFTilingProcessor.process(task)
          }
        } catch {
          case t => error(task, "Error running task of kind '%s' on path '%s': %s".format(task.taskType.name, task.path, t.getMessage))
        } finally {
          Task.finish(task)
        }
    }
  }


}


}
