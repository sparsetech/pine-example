package example

import java.io.File

import scala.util.{Success, Failure}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import fs2.{Stream, Task}
import fs2.interop.cats._

import org.http4s._
import org.http4s.dsl._
import org.http4s.util.StreamApp
import org.http4s.server.blaze.BlazeBuilder

import pine._

import example.page.Page
import example.protocol.TypedRequest

object Server extends StreamApp {
  implicit val strategy = fs2.Strategy.fromFixedDaemonPool(2)

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

  def sendResponse(future: Future[String], tpe: MediaType): Task[Response] =
    Task.fromFuture(
      future.transform {
        case Success(value) =>
          Success(Ok(value).withContentType(Some(headers.`Content-Type`(tpe))))

        case Failure(e) =>
          e.printStackTrace()
          Success(InternalServerError(e.toString))
      }
    ).flatMap(identity)

  val service = HttpService {
    case _ @ GET -> Root / "js" / "templates.js" =>
      val json    = Template.all().asJson.noSpaces
      val content = "var templates = " + json

      Ok(content)
        .withContentType(Some(
          headers
            .`Content-Type`(MediaType.`application/javascript`)
            .withCharset(Charset.`UTF-8`)))

    case request @ GET -> Root / "js" / fileName =>
      StaticFile.fromFile(new File("assets/js/", fileName), Some(request))
        .getOrElseF(NotFound())

    case request @ POST -> _ if Routes.api.parse(request.pathInfo).isDefined =>
      EntityDecoder.decodeString(request).flatMap { bodyString =>
        decode[protocol.Request](bodyString) match {
          case Left(_) => BadRequest("Malformed JSON payload")
          case Right(req: TypedRequest[_]) =>
            val response = JvmService.request(req).map(
              _.asJson(req.encoder).noSpaces)
            sendResponse(response, MediaType.`application/json`)
        }
      }

    case request @ GET -> _ =>
      val path = request.queryString match {
        case ""  => request.pathInfo
        case qry => request.pathInfo + "?" + qry
      }

      val page = Routes.renderPage(path, JvmService)
      sendResponse(renderPage(page), MediaType.`text/html`)
  }

  override def stream(args: List[String]): Stream[Task, Nothing] =
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(service)
      .serve
}