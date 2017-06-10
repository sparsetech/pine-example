package example

import example.page.Page
import shapeless._

import scala.concurrent.Future
import pine._
import trail._

object Routes {
  val index       = Root
  val numberGuess = Root / "numberguess"
  val books       = Root / "books"
  val bookDetails = Root / "book" / Arg[Int]

  val api = Root / "api"

  def renderPage(uri: String, service: Service): Future[(Page, String, Node)] =
    uri match {
      case index(_) =>
        Future.successful((page.Index(), "", IndexView.render().asInstanceOf[Tag[Singleton]]))

      case numberGuess(_) =>
        service.requestWith(protocol.NumberGuessReset()) { result =>
          (page.NumberGuess(), "Number guess", NumberGuessView.render(result).asInstanceOf[Tag[Singleton]])
        }

      case books(_) =>
        service.requestWith(protocol.Books()) { result =>
          (page.Books(result), "Books", BooksView.render(result).asInstanceOf[Tag[Singleton]])
        }

      case bookDetails((id: Int) :: HNil) =>
        service.requestWith(protocol.BookDetails(id)) { result =>
          (page.BookDetails(), "Book details", BookDetailsView.render(result).asInstanceOf[Tag[Singleton]])
        }
    }
}
