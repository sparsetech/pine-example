package example

import example.protocol._

import scala.concurrent.Future
import scala.util.{Random, Try}

object DataModel {
  case class Book(id: Int, author: String, title: String, price: Double)

  val ScalaBooks = List(
    Book(id = 1, author = "Martin Odersky", title = "Programming in Scala", price = 10.0),
    Book(id = 2, author = "Cay S. Horstmann", title = "Scala for the Impatient", price = 15.0),
    Book(id = 3, author = "Joshua Sureth", title = "Effective Scala", price = 20.0),
    Book(id = 4, author = "Nilanjan Raychaudhuri", title = "Scala in Action", price = 12.0),
    Book(id = 5, author = "Dean Wampler", title = "Programming Scala", price = 9.0))
}

object JvmService extends Service {
  val Max = 50
  var solution = 0
  var guesses  = 0

  val books = { req: Books =>
    DataModel.ScalaBooks.map { book =>
      BookListItem(book.id, book.title)
    }
  }

  val bookDetails = { req: BookDetails =>
    DataModel.ScalaBooks.find(_.id == req.id) match {
      case None => Left("Book not found")
      case Some(b) => Right(BookDetailsRecord(b.id, b.title, b.author, b.price))
    }
  }

  val numberGuessReset = { req: NumberGuessReset =>
    solution = new Random().nextInt(Max)
    guesses  = 0
    s"Guess a number between 0 and $Max."
  }

  val numberGuessSubmit = { req: NumberGuessSubmit =>
    guesses += 1
    req.guess match {
      case _ if req.guess < 0 || req.guess > Max =>
        val message = s"Please enter a valid number between 0 and $Max."
        NumberGuessResult(solved = false, message)
      case _ if req.guess < solution =>
        val message = s"${req.guess} is too low, try a higher number!"
        NumberGuessResult(solved = false, message)
      case _ if req.guess > solution =>
        val message = s"${req.guess} is too high, try a lower number!"
        NumberGuessResult(solved = false, message)
      case _ =>
        val message = s"${req.guess} is correct! You got it in $guesses tries!"
        NumberGuessResult(solved = true, message)
    }
  }

  override def request[Resp](req: TypedRequest[Resp]): Future[Resp] = req match {
    case r: Books => Future.fromTry(Try(books(r)))
    case r: BookDetails => Future.fromTry(Try(bookDetails(r)))
    case r: NumberGuessReset => Future.fromTry(Try(numberGuessReset(r)))
    case r: NumberGuessSubmit => Future.fromTry(Try(numberGuessSubmit(r)))
  }
}
