package example

import java.io.File

import scala.util.{Failure, Success}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._

import cats.effect.IO

import fs2.StreamApp
import fs2.Stream
import fs2.StreamApp.ExitCode

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server.blaze.BlazeBuilder

import pine._

import example.page.Page

object Server extends StreamApp[IO] {
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

  def sendResponse(future: Future[String], tpe: MediaType): IO[Response[IO]] =
    IO.fromFuture(IO(
      future.transform {
        case Success(value) =>
          Success(Ok(value, headers.`Content-Type`(tpe)))

        case Failure(e) =>
          e.printStackTrace()
          Success(InternalServerError(e.toString))
      }
    )).flatMap(identity)

  val service = HttpService[IO] {
    case _ @ GET -> Root / "js" / "templates.js" =>
      val json    = Template.all().asJson.noSpaces
      val content = "var templates = " + json

      Ok(content, headers.`Content-Type`(MediaType.`application/javascript`))

    case request @ GET -> Root / "js" / fileName =>
      StaticFile.fromFile(new File("assets/js/", fileName), Some(request))
        .getOrElseF(NotFound())

    case request @ POST -> _ if Routes.api.parse(request.pathInfo).isDefined =>
      EntityDecoder.decodeString(request).flatMap { bodyString =>
        decode[protocol.Request[Any]](bodyString) match {
          case Left(_)    => BadRequest("Malformed JSON payload")
          case Right(req) =>
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

      Routes.renderPage(path, JvmService) match {
        case None       => NotFound()
        case Some(page) => sendResponse(renderPage(page), MediaType.`text/html`)
      }
  }

  override def stream(args: List[String],
                      requestShutdown: IO[Unit]): Stream[IO, ExitCode] =
    BlazeBuilder[IO]
      .bindHttp(8080, "localhost")
      .mountService(service)
      .serve
}