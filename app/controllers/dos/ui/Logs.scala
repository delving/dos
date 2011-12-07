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

package controllers.dos.ui

import models.dos.Log
import extensions.Extensions
import play.mvc.Controller
import org.bson.types.ObjectId
import play.mvc.results.Result
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.SalatMongoCursor

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object Logs extends Controller with Extensions {

  def list(taskId: ObjectId, lastCount: Int): Result = {
    val cursor: SalatMongoCursor[Log] = Log.find(MongoDBObject("task_id" -> taskId)).sort(MongoDBObject("date" -> 1))
    val (logs, skipped) = if(lastCount != null && lastCount > 0) {
      if(cursor.count - lastCount > 100) {
        (cursor.skip(cursor.count - 100), true)
      } else {
        (cursor.skip(lastCount + 1), false)
      }
    } else {
      if(cursor.count > 100) {
        (cursor.skip(cursor.count - 100), true)
      } else {
        (cursor, false)
      }
    }
    Json(Map("logs" -> logs, "skipped" -> skipped))
  }

  def view(taskId: ObjectId): Result = {
    val cursor: SalatMongoCursor[Log] = Log.find(MongoDBObject("task_id" -> taskId)).sort(MongoDBObject("date" -> 1))
    Text(cursor.map(log => log.date + "\t" + log.level.name.toUpperCase  + "\t" +  log.node + "\t" + log.message).mkString("\n"))
  }
}