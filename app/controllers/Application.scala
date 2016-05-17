package controllers

import akka.actor.ActorSystem
import javax.inject._

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

import play.api.mvc.BodyParsers.parse.multipartFormData
import play.core.parsers.Multipart.handleFilePart
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.iteratee.{Enumerator, Iteratee}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import scala.util.control.Exception.allCatch
import play.api.mvc.MultipartFormData
import play.core.parsers.Multipart.{FileInfo, PartHandler}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.{GridFS, GridFSDBFile}
import org.bson.types.ObjectId

@Singleton
class Application @Inject() (system: ActorSystem) extends Controller {

  val host = "localhost"
  val port = 27017
  val dbName = "watermark"

  val gridFs = GridFS(
    MongoClient(
      new ServerAddress(host, port),
      MongoClientOptions(connectTimeout = 500, socketTimeout = 500)
    )(dbName)
  )

  val watermarker = system.actorOf(Watermarker.props, "watermarker")

  def fileUploader = Action(multipartFormData(handleFilePartAsByteArray)) { request =>
    //println(request.body.asFormUrlEncoded.get("title").mkString(""))
    saveFileAndReturnId(request, "document") map { id =>
      val hexString = id.toHexString()
      watermarker ! hexString
      Ok(hexString)
    } getOrElse Ok("oops")
  }

  def saveFileAndReturnId(
                                   request: Request[MultipartFormData[Array[Byte]]],
                                   fileFieldName: String
                                 ) = {
    request.body.file(fileFieldName) flatMap {
      filePart =>
        if (filePart.contentType.getOrElse("") != "application/pdf") None
        else
          gridFs(new ByteArrayInputStream(filePart.ref)) {
            f =>
              f.filename = filePart.filename
              f.contentType = filePart.contentType.getOrElse("")
          } map { x => x.asInstanceOf[ObjectId] }
    }
  }

  def handleFilePartAsByteArray: PartHandler[FilePart[Array[Byte]]] =
    handleFilePart {
      case FileInfo(partName, filename, contentType) =>
        Iteratee.fold[Array[Byte], ByteArrayOutputStream](
          new ByteArrayOutputStream()) { (os, data) =>
          os.write(data)
          os
        }.map { os =>
          os.close()
          os.toByteArray
        }
    }

  def getDocument(id: String) = Action { request =>
    stringIdToResponse(id)(file => Result(
      header = ResponseHeader(200),
      body = Enumerator.fromStream(file.underlying.getInputStream)
    ))
  }

  def getDocumentWatermarkingStatus(id: String) = Action { request =>
    stringIdToResponse(id)(file => Ok(String.valueOf(file.underlying.containsField("watermark"))))
  }

  def stringIdToResponse(id: String)(fileToResponse: GridFSDBFile => Result) = (
    for {
      objectId <- allCatch opt new ObjectId(id)
      file <- gridFs.findOne(objectId)
    } yield fileToResponse(file)
  ) getOrElse NotFound("No such document")

  def index() = Action {
    Ok(views.html.upload.render())
  }
}
