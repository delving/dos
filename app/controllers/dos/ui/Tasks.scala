package controllers.dos.ui

import models.dos.{TaskType, TaskState, Task}
import extensions.Extensions
import TaskState._
import org.bson.types.ObjectId
import play.mvc.results.Result
import play.mvc.Controller
import scala.collection.JavaConversions.asScalaMap
import play.{Play, Logger}

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object Tasks extends Controller with Extensions {

  def add(path: String, taskType: String): Result = {
    val tt = TaskType.valueOf(taskType) getOrElse ({
      return LoggedError("Invalid task type " + taskType)
    })

    // stoopid play...
    val keyExtract = "params\\[([^\\]]*)\\]".r
    val taskParams: Map[String, String] = asScalaMap[String, Array[String]](params.all()).filter(_._1.startsWith("params")).toMap[String, Array[String]].map((item) => (keyExtract.findFirstMatchIn(item._1).head.group(1), item._2.head))

    val task = Task(node = getNode, path = path, taskType = tt, params = taskParams)
    Logger.info("Adding new task to queue: " + task.toString)
    Task.insert(task) match {
      case None => LoggedError("Could not create da task")
      case Some(taskId) => Json(task.copy(_id = taskId))
    }
  }

  def cancel(id: ObjectId): Result = {
    val task = Task.findOneByID(id) getOrElse (return LoggedNotFound("Could not find task with id " + id))
    Task.cancel(task)
    Ok
  }

  def list(what: String): Result = {
    val tasks = TaskState.valueOf(what) match {
      case Some(state) if (state == QUEUED || state == RUNNING || state == FINISHED || state == CANCELLED) => Some(Task.list(state))
      case None => None
    }
    if (tasks == None) return Error("Invalid task state " + what)
    Json(Map("tasks" -> tasks.get))
  }

  def listAll(): Result = {
    Json(Map("running" -> Task.list(RUNNING), "queued" -> Task.list(QUEUED), "finished" -> Task.list(FINISHED)))
  }

  def status(id: ObjectId): Result = {
    val task = Task.findOneByID(id) getOrElse (return LoggedNotFound("Could not find task with id " + id))
    Json(
      Map(
      "totalItems" -> task.totalItems,
      "processedItems" -> task.processedItems,
      "percentage" -> ((task.processedItems.toDouble / task.totalItems) * 100).round
    ))
  }

  private def getNode = {
    Play.configuration.getProperty("culturehub.nodeName", Play.configuration.getProperty("dos.nodeName"))
  }

}