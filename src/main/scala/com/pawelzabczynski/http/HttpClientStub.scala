package com.pawelzabczynski.http

import cats.effect.Resource
import cats.{MonadError, StackSafeMonad}
import org.http4s.{EntityEncoder, Request, Response, Status}
import org.http4s.Status.{InternalServerError, NotFound, Ok}
import fs2.Stream
import org.http4s.client.Client
import zio.{Task, ZIO}
import zio.interop.catz._

import java.nio.charset.StandardCharsets
import scala.util.{Failure, Success, Try}

class HttpClientStub(
    monad: MonadError[Task, Throwable],
    matchers: PartialFunction[Request[Task], Task[Response[Task]]]
) {
  def whenRequestMatches(p: Request[Task] => Boolean): WhenRequest = new WhenRequest(p)

  class WhenRequest(p: Request[Task] => Boolean) {
    def thenRespondOk(): HttpClientStub          = thenRespondWithCode(Ok, "")
    def thenRespondNotFound(): HttpClientStub    = thenRespondWithCode(NotFound, "")
    def thenRespondServerError(): HttpClientStub = thenRespondWithCode(InternalServerError, "Internal Server Error.")
    def thenRespondWithCode(status: Status, msg: String): HttpClientStub = thenRespond(
      Response[Task](status = status, body = Stream.emits(msg.getBytes(StandardCharsets.UTF_8)))
    )

    def thenRespond[T](body: T)(implicit ee: EntityEncoder[Task, T]): HttpClientStub = thenRespond(Ok, body)
    def thenRespond[T](status: Status, body: T)(implicit ee: EntityEncoder[Task, T]): HttpClientStub = thenRespond(
      Response[Task](status = status).withEntity(body)
    )
    def thenRespond(body: String): HttpClientStub = thenRespond(
      Response[Task](status = Ok, body = Stream.emits(body.getBytes(StandardCharsets.UTF_8)))
    )
    def thenRespond(body: String, status: Status): HttpClientStub = thenRespond(
      Response[Task](status = status, body = Stream.emits(body.getBytes(StandardCharsets.UTF_8)))
    )
    def thenRespond(resp: => Response[Task]): HttpClientStub = {
      val nextM: PartialFunction[Request[Task], Task[Response[Task]]] = {
        case r if p(r) => monad.map(monad.unit)(_ => resp)
      }
      val m = matchers.orElse(nextM)

      new HttpClientStub(monad, m)
    }
  }

  def stub: Client[Task] = {
    Client.apply[Task] { req =>
      Resource.eval {
        Try(matchers.apply(req)) match {
          case Success(response) => response
          case Failure(exception) =>
            monad.raiseError(new RuntimeException(s"Behaviour not stubbed. Request: $req", exception))
        }
      }
    }
  }
}

object HttpClientStub {

  class ZIOMonadError extends MonadError[Task, Throwable] with StackSafeMonad[Task] {
    override def raiseError[A](e: Throwable): Task[A] = ZIO.fail(e)

    override def handleErrorWith[A](fa: Task[A])(f: Throwable => Task[A]): Task[A] = fa.catchAll(f)

    override def pure[A](f: A): Task[A] = ZIO.succeed(f)

    override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] = fa.flatMap(f)
  }

  def apply(): HttpClientStub = {
    new HttpClientStub(new ZIOMonadError(), PartialFunction.empty)
  }
}
