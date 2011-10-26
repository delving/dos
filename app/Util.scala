package util {

import models.dos.{Log, LogLevel, Task}

/**
 * Logger that traces everything in relation to tasks.
 * Log entries are "smart" - they both are human-readable and machine-readable so we can turn the log entries into events that help build the history of everything that has happened to a thing.
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

trait Logging {

  def info(task: Task, message: String, sourceItem: Option[String] = None, resultItem: Option[String] = None) {
    log(task, message, LogLevel.INFO, sourceItem, resultItem)
  }

  def error(task: Task, message: String, sourceItem: Option[String] = None, resultItem: Option[String] = None) {
    log(task, message, LogLevel.ERROR, sourceItem, resultItem)
  }

  def log(task: Task, message: String, level: LogLevel = LogLevel.INFO, sourceItem: Option[String] = None, resultItem: Option[String] = None) {
    Log.insert(Log(message = message, level = level, task_id = task._id, taskType = task.taskType, sourceItem = sourceItem, resultItem = resultItem))
  }

}


}
