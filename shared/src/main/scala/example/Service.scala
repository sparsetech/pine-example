package example

import protocol.TypedRequest

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Service {
  def request[Resp](req: TypedRequest[Resp]): Future[Resp]

  def requestWith[Resp, T](req: TypedRequest[Resp])(f: Resp => T): Future[T] =
    request(req).map(f)
}
