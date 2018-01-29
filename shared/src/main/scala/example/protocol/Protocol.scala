package example.protocol

import io.circe.{Encoder, Decoder, ObjectEncoder}
import io.circe.generic.auto._

case class NumberGuessResult(solved: Boolean, message: String)
case class BookListItem(id: Int, title: String)
case class BookDetailsRecord(id: Int,
                             title: String,
                             author: String,
                             price: Double)

sealed abstract class Request[T](implicit val encoder: Encoder[T],
                                          val decoder: Decoder[T])

case class NumberGuessReset()
  extends Request[String]

case class NumberGuessSubmit(guess: Int)
  extends Request[NumberGuessResult]

case class Books() extends Request[List[BookListItem]]

case class BookDetails(id: Int)
  extends Request[Either[String, BookDetailsRecord]]

object Request {
  import io.circe.generic.semiauto._
  implicit def encodeRequest[T]: ObjectEncoder[Request[T]] =
    deriveEncoder[Request[_]].asInstanceOf[ObjectEncoder[Request[T]]]
  implicit def decodeRequest[T]: Decoder[Request[T]] =
    deriveDecoder[Request[_]].asInstanceOf[Decoder[Request[T]]]
}