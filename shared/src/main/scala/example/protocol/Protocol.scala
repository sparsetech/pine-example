package example.protocol

import io.circe._
import io.circe.generic.auto._
import io.circe.generic.JsonCodec

case class NumberGuessResult(solved: Boolean, message: String)
@JsonCodec case class BookListItem(id: Int, title: String)
case class BookDetailsRecord(id: Int, title: String, author: String, price: Double)

@JsonCodec sealed trait Request

sealed abstract class TypedRequest[T](implicit val encoder: Encoder[T],
                                               val decoder: Decoder[T])
  extends Request

case class NumberGuessReset()
  extends TypedRequest[String] with Request

case class NumberGuessSubmit(guess: Int)
  extends TypedRequest[NumberGuessResult] with Request

case class Books() extends TypedRequest[List[BookListItem]] with Request

case class BookDetails(id: Int)
  extends TypedRequest[Either[String, BookDetailsRecord]] with Request
