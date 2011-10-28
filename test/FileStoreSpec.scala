import com.mongodb.casbah.commons.MongoDBObject
import controllers.dos._
import java.io.{ByteArrayInputStream, File}
import controllers.dos.StoredFile
import org.bson.types.ObjectId
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers
import play.data.Upload
import play.libs.{MimeTypes, IO}
import play.mvc.results.{RenderBinary, Result}
import play.test.UnitFlatSpec

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

class FileStoreSpec extends UnitFlatSpec with ShouldMatchers with BeforeAndAfterAll {

  override protected def afterAll() {
    fileStore.db.dropDatabase()
  }

  val testFile = new File(play.Play.applicationPath, "public/dos/images/dummy-object.png")
  val TEST_UID = "123456789"
  val TEST_OID = new ObjectId

  var uploaded_id: ObjectId = null

  it should "upload a file" in {
    val upload = new MockUpload(testFile)
    val uploads = List(upload)

    val res: Result = FileUpload.uploadFileInternal(TEST_UID, uploads)
    res.getClass should not equal (classOf[Error])
  }

  it should "find back files by upload UID" in {
    val fetched: Seq[StoredFile] = FileUpload.getFilesForUID(TEST_UID)
    fetched.length should equal(1)
    fetched.head.name should equal(testFile.getName)
    fetched.head.length should equal(testFile.length())
  }

  it should "attach uploaded files to an object, given an upload UID and an object ID" in {
    FileUpload.markFilesAttached(TEST_UID, TEST_OID)
    val file = fileStore.findOne(MongoDBObject(ITEM_POINTER_FIELD -> TEST_OID))
    file should not equal (None)
    FileUpload.getFilesForUID(TEST_UID).length should equal(0)
    uploaded_id = file.get.get("_id").get.asInstanceOf[ObjectId]
  }

  it should "mark an active thumbnail and an active image given a file pointer and object ID" in {
    FileUpload.activateThumbnails(uploaded_id, TEST_OID)

    val image = ImageDisplay.displayImage(TEST_OID.toString)
    val thumbnail = ImageDisplay.displayThumbnail(id = TEST_OID.toString, orgId = "", collectionId = "", browse = false)

    image.getClass should equal (classOf[RenderBinary])
    thumbnail.getClass should equal (classOf[RenderBinary])
  }


}

class MockUpload(file: File) extends Upload {
  def asBytes() = IO.readContent(file)

  def asStream() = new ByteArrayInputStream(asBytes())

  def getContentType = MimeTypes.getContentType(file.getName)

  def getFileName = file.getName

  def getFieldName = "mock"

  def getSize = file.length()

  def isInMemory = true

  def asFile() = file
}
