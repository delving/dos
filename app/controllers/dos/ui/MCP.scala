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
      ))
    }
    Template("/dos/ui/MCP/index.html", 'files -> files)
  }

}

case class BrowserFile(path: String,
                       name: String,
                       isDir: Boolean,
                       contentType: String) {
  def isImage = contentType.contains("image")
  def id = if(name.contains(".")) name.split("\\.")(0) else name
}