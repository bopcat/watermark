package controllers

import akka.actor.ActorSystem
import javax.inject._
import controllers.HelloActor.SayHello
import play.api.libs.json._
import play.api.mvc._
import models.Book._

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.libs.ws.WSResponse

@Singleton
class Application @Inject() (system: ActorSystem) extends Controller {

  val helloActor = system.actorOf(HelloActor.props, "hello-actor")
  val cancellable = system.scheduler.schedule(
    0.microseconds, 1000.milliseconds, helloActor, SayHello("" + System.currentTimeMillis))

  def listBooks = Action {
    Ok(Json.toJson(books))
  }

  def saveBook = Action(BodyParsers.parse.json) { request =>
    val b = request.body.validate[Book]
    b.fold(
      errors => {
        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
      },
      book => {
        addBook(book)
        Ok(Json.obj("status" -> "OK"))
      }
    )
  }

  def getRemoteResponse = Action.async { implicit request =>
    val response = WS.url("https://query.yahooapis.com/v1/public/yql?q=select+*+from+yahoo.finance.xchange+where+pair+=+%22USDRUB,EURRUB%22&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=").get
    Thread.sleep(30000)
    response map {response => Ok(response.json)}
  }
}
