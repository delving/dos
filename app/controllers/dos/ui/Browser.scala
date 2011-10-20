package controllers.dos.ui

import play.mvc.Controller
import play.mvc.results.Result
import java.io.File
import extensions.dos.{DoSJson, Extensions}

/**
 * 
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */



case class ViewModel(rootPath: String)

case class Directory(path: String)
