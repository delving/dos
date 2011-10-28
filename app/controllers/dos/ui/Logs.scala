package controllers.dos.ui

import models.dos.Log
import extensions.Extensions
import play.mvc.Controller
import org.bson.types.ObjectId
import play.mvc.results.Result
import com.mongodb.casbah.commons.MongoDBObject

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object Logs extends Controller with Extensions {

  def list(taskId: ObjectId, lastCount: Int): Result = {
    if(lastCount != null && lastCount > 0) {
      val logs = Log.find(MongoDBObject("task_id" -> taskId)).sort(MongoDBObject("date" -> 1)).skip(lastCount + 1)
      Json(Map("logs" -> logs))
    } else {
      val logs = Log.find(MongoDBObject("task_id" -> taskId)).sort(MongoDBObject("date" -> 1))
      Json(Map("logs" -> logs))
    }
  }
}