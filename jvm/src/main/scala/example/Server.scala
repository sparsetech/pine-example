package example

import java.io.File

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.http4s.dsl._
import org.http4s.{server, _}
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.syntax._
import pine._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaz.concurrent.Task
import scalaz.{-\/, \/-}

import example.page.Page
import example.protocol.TypedRequest

object Server extends server.ServerApp {
  // From http://www.andrewconner.org/scalaz-task-scala-future
  implicit class FutureExtensionOps[A](x: => Future[A]) {
    import scalaz.Scalaz._
    def asTask: Task[A] =
      Task.async { register =>
        x.onComplete {
          case Failure(ex) => register(ex.left)
          case Success(v)  => register(v.right)
        }
      }
  }

  def renderPage(result: Future[(Page, String, Node)]): Future[String] =
    result.map { case (page, title, node) =>
      Templates.layout.update { implicit ctx =>
        TagRef[tag.Body ] := node
        TagRef[tag.Title] := title
        TagRef[tag.Head ].append(
          tag.Script.setAttr("type", "text/javascript").set(
            s"var page = ${page.asJson.noSpaces};"
          )
        )
      }.toHtml
    }

  def serveFile(path: String,
                fileName: String,
                request: org.http4s.Request): Task[org.http4s.Response] =
    StaticFile.fromFile(new File(path, fileName), Some(request)) match {
      case None    => NotFound()
      case Some(f) => Task.now(f)
    }

  def sendResponse(future: Future[String], tpe: MediaType): Task[Response] =
    future.asTask.attempt.flatMap {
      case \/-(value) =>
        Ok(value).withContentType(Some(headers.`Content-Type`(tpe)))

      case -\/(e) =>
        println(e)
        e.printStackTrace()
        InternalServerError(e.toString)
    }

  val templates = HttpService { case _ @ GET -> Root / "js" / "templates.js" =>
    val json    = Template.all().asJson.noSpaces
    val content = "var templates = " + json

    Ok(content)
      .withContentType(Some(
        headers
          .`Content-Type`(MediaType.`application/javascript`)
          .withCharset(Charset.`UTF-8`)))
  }

  val page = HttpService { case request @ GET -> _ =>
    val path = request.queryString match {
      case ""  => request.pathInfo
      case qry => request.pathInfo + "?" + qry
    }

    val page = Routes.renderPage(path, JvmService)
    sendResponse(renderPage(page), MediaType.`text/html`)
  }

  val api = HttpService {
    case request @ POST -> _ if Routes.api.parse(request.pathInfo).isDefined =>
      val bodyString = EntityDecoder.decodeString(request).run
      decode[protocol.Request](bodyString) match {
        case Left(_) => BadRequest("Malformed JSON payload")
        case Right(req: TypedRequest[_]) =>
          val response = JvmService.request(req).map(
            _.asJson(req.encoder).noSpaces)
          sendResponse(response, MediaType.`application/json`)
      }
  }

  val javaScript = HttpService {
    case request @ GET -> Root / "js" / fileName =>
      serveFile("assets/js/", fileName, request)
  }

  val services = templates.orElse(javaScript).orElse(api).orElse(page)

  override def server(args: List[String]) =
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(services)
      .start
}