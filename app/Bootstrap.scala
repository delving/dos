import models.Task
import java.util.Date
import play.jobs.{OnApplicationStart, Job}

/**
 * 
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

@OnApplicationStart class Bootstrap extends Job {

  override def doJob() {
    if(Task.count() == 0) {
      val task = Task(new Date(), None, "/some/foo")
      Task.insert(task)
    }
  }
}