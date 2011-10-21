package jobs {

import models.dos.{TaskType, TaskState, Task}
import util.Logger
import play.jobs.{Every, Job}
import processors.ThumbnailProcessor
import java.util.Date

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

@Every("10s")
class TaskQueueJob extends Job with Logger {

  override def doJob() {

    val head = Task.list(TaskState.QUEUED).headOption
    head foreach { task =>
      Task.save(task.copy(state = TaskState.RUNNING, startedAt = Some(new Date)))
      try {
         task.taskType match {
          case TaskType.THUMBNAILS => ThumbnailProcessor.process(task, Map("sizes" -> controllers.dos.thumbnailSizes.values.toList))
          case TaskType.TILES => println("no can do")
        }
      } catch {
        case t => error(task, "Error running task of kind '%s' on path '%s': %s".format(task.taskType.name, task.path, t.getMessage))
      } finally {
        Task.save(task.copy(state = TaskState.FINISHED, finishedAt = Some(new Date)))
      }
    }
  }


}


}
