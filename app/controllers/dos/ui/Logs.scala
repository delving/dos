package controllers.dos.ui

import models.dos.Log
import extensions.dos.Extensions
import play.mvc.Controller
import org.bson.types.ObjectId
import play.mvc.results.Result
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import java.util.Date

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object Logs extends Controller with Extensions {

  def list(taskId: ObjectId, `_`: Long): Result = {
    if(`_` != null && `_` > 0) {
      val date = new Date(`_`)
      val logs = Log.find(MongoDBObject("task_id" -> taskId) ++ ("date" $gt date)).sort(MongoDBObject("date" -> 1))
      Json(Map("logs" -> logs))
    } else {
      val logs = Log.find(MongoDBObject("task_id" -> taskId)).sort(MongoDBObject("date" -> 1))
      Json(Map("logs" -> logs))
    }
  }
}