package example

import example.page.Page
import example.protocol.Request
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import pine._
import pine.dom._
import trail._

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSGlobalScope
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._

@js.native
@JSGlobalScope
object JsGlobals extends js.Object {
  val templates: js.Dictionary[String] = js.native
  val page     : js.Dictionary[js.Any] = js.native
}

object Template {
  def get(template: String): Tag[_] =
    HtmlParser.fromString(JsGlobals.templates(template))
}

object AjaxService extends Service {
  override def request[Resp](req: Request[Resp]): Future[Resp] = {
    // See http://stackoverflow.com/a/5175782
    dom.window.document.body.className = "waiting"  // Set waiting cursor

    val response = Ajax.post(Routes.api(), req.asJson.noSpaces).flatMap(r =>
      Future.fromTry(decode(r.responseText)(req.decoder).toTry))

    // Reset cursor
    response.onComplete(_ => dom.window.document.body.className = "")
    response.failed.foreach(Console.err.println(_))
    response
  }
}

object App {
  def render(url: String): Unit = {
    val path = PathParser.parsePathAndQuery(url)
    Routes.renderPage(path, AjaxService).map { case (page, title, node) =>
      DOM.render(implicit ctx => TagRef[tag.Body] := node)
      Manage.page(page)
      dom.document.title = title
    }
  }

  def redirect(url: String): Unit = {
    render(url)
    dom.window.history.pushState("", "", url)
    dom.document.body.scrollTop = 0
  }

  def serverPage()(implicit decoder: Decoder[Page]): Page = {
    val json = JSON.stringify(JsGlobals.page)
    decode[Page](json).right.get
  }

  def main(args: Array[String]): Unit = {
    Window.load     := Manage.page(serverPage())
    Window.popState := render(dom.window.location.href)
    Window.click    := { e =>
      // The user may click a child within <a>, traverse parents too
      def handle: dom.Node => Unit = {
        case null =>
        case a: dom.html.Anchor
          if a.href.startsWith(dom.window.location.origin.get) =>
          if (a.onclick == null) {  // We have not defined any custom behaviour
            e.preventDefault()
            redirect(a.href)
          }
        case n => handle(n.parentNode)
      }

      handle(e.target.asInstanceOf[dom.Node])
    }
  }
}
