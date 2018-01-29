package example.page

import example.protocol.BookListItem

sealed trait Page

case class Index()                          extends Page
case class NumberGuess()                    extends Page
case class Books(books: List[BookListItem]) extends Page
case class BookDetails()                    extends Page
