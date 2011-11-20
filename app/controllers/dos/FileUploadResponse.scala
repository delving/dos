package controllers.dos

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
case class FileUploadResponse(name: String, size: Long, url: String = "", thumbnail_url: String = "", delete_url: String = "", delete_type: String = "DELETE", error: String = null, selected: Boolean = false, id: String = "")
