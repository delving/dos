package controllers.dos.ui

import models.dos.{TaskType, TaskState, Task}
import extensions.dos.Extensions
import TaskState._
import play.mvc.Controller
import org.bson.types.ObjectId
import play.mvc.results.Result

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object Tasks extends Controller with Extensions {

  def add(path: String, taskType: String): Result = {
    val tt = TaskType.valueOf(taskType) getOrElse (return Error("Invalid task type " + taskType))
    val task = Task(path = path, taskType = tt)
    Task.insert(task) match {
      case None => Error("Could not create da task")
      case Some(taskId) => Json(task.copy(_id = taskId))
    }
  }

  def cancel(id: ObjectId): Result = {
    val task = Task.findOneByID(id) getOrElse (return NotFound("Could not find task with id " + id))
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
    val task = Task.findOneByID(id) getOrElse (return NotFound("Could not find task with id " + id))
    Json(
      Map(
      "totalItems" -> task.totalItems,
      "processedItems" -> task.processedItems,
      "percentage" -> ((task.processedItems.toDouble / task.totalItems) * 100).round
    ))
  }

}