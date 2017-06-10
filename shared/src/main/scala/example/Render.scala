package example

import shapeless.HNil
import pine._

object Templates {
  // Must be defs, so that the latest templates are served
  def index: Tag[_]        = Template.get("Index")
  def layout: Tag[_]       = Template.get("Layout")
  def bookDetails: Tag[_]  = Template.get("BookDetails")
  def books: Tag[_]        = Template.get("Books")
  def numberGuess: Tag[_]  = Template.get("NumberGuess")
}

object IndexView {
  val btnGuess = TagRef[tag.Button]("guess")
  val btnBooks = TagRef[tag.Button]("books")

  def render(): Tag[_] = Templates.index
}

object BooksView {
  val lstBooks        = TagRef[tag.Ul]("books")
  val edtAddBookTitle = TagRef[tag.Input]("add-book-title")
  val btnAddBook      = TagRef[tag.Button]("add-book")
  val btnRevert       = TagRef[tag.Button]("revert")

  def render(items: List[protocol.BookListItem]): Tag[_] =
    Templates.books.update { implicit ctx =>
      lstBooks := items.map(new BooksItemView(_).render())
    }
}

class BooksItemView(book: protocol.BookListItem) {
  var hidden   = true

  val id       = book.id.toString
  val template = Templates.books.byId("book")

  val root            = TagRef[tag.Div]("book", id)
  val title           = TagRef[tag.A]("title", id)
  val btnRemove       = TagRef[tag.Button]("remove", id)
  val btnRenameToggle = TagRef[tag.Button]("rename-toggle", id)
  val divRenameBox    = TagRef[tag.Div]("rename-box", id)
  val txtRename       = TagRef[tag.Input]("rename-text", id)
  val btnRenameSave   = TagRef[tag.Button]("rename-save", id)

  def render(): Tag[_] =
    template.suffixIds(id).update { implicit ctx =>
      title := book.title
      title.href := Routes.bookDetails(book.id :: HNil)
      txtRename.value := book.title
      divRenameBox.hide(hidden)
    }
}

object BookDetailsView {
  val txtError  = TagRef[tag.Div]("error")
  val ulDetails = TagRef[tag.Ul]("details")
  val txtTitle  = TagRef[tag.Span]("title")
  val txtAuthor = TagRef[tag.Span]("author")
  val txtPrice  = TagRef[tag.Span]("price")

  def render(result: Either[String, protocol.BookDetailsRecord]): Tag[_] =
    Templates.bookDetails.update { implicit ctx =>
      result match {
        case Left(message) =>
          txtError.hide(false)
          ulDetails.hide(true)
          txtError := message

        case Right(details) =>
          txtError.hide(true)
          ulDetails.hide(false)
          txtTitle  := details.title
          txtAuthor := details.author
          txtPrice  := details.price
      }
    }
}

object NumberGuessView {
  val input   = TagRef[tag.Input]("input")
  val guess   = TagRef[tag.Button]("guess")
  val message = TagRef[tag.H5]("message")
  val form    = TagRef[tag.Form]("form")
  val reset   = TagRef[tag.Button]("reset")

  def render(text: String): Tag[_] =
    Templates.numberGuess.update { implicit ctx =>
      message := text
    }
}
