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

import play.mvc.Controller
import java.io.File
import play.mvc.results.Result
import controllers.dos.{FileUpload, ImageDisplay}

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object MCP extends Controller {

  def index() = Template

  def browse(path: String): Result = {
    val f = new File(path)
    if (!f.exists()) return Error("Directory '%s' does not exist".format(path))
    if (!f.isDirectory) return Error("Trying to browse a file: " + path)
    val files = if (f.listFiles == null) {
      List()
    } else {
      f.listFiles.map(f => BrowserFile(
        path = f.getAbsolutePath,
        name = f.getName,
        isDir = f.isDirectory,
        contentType = play.libs.MimeTypes.getContentType(f.getName)
      )).sortBy(!_.isDir)
    }
    Template("/dos/ui/MCP/index.html", 'files -> files)
  }

}

case class BrowserFile(path: String,
                       name: String,
                       isDir: Boolean,
                       contentType: String) {
  def isImage = contentType.contains("image")
  def id = if(name.contains(".") && !name.startsWith(".")) name.split("\\.")(0) else name
}