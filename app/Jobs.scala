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
