package example

import protocol.Request

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Service {
  def request[Resp](req: Request[Resp]): Future[Resp]

  def requestWith[Resp, T](req: Request[Resp])(f: Resp => T): Future[T] =
    request(req).map(f)
}
