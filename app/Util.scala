package util {

import models.dos.{Log, LogLevel, Task}

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

trait Logger {

  def info(task: Task, message: String) {
    log(task, message, LogLevel.INFO)
  }

  def error(task: Task, message: String) {
    log(task, message, LogLevel.ERROR)
  }

  def log(task: Task, message: String, level: LogLevel = LogLevel.INFO) {
    Log.insert(Log(message = message, level = level, task_id = task._id))
  }

}


}
