package com.pawelzabczynski.http

import cats.data.Kleisli
import cats.effect.Resource
import cats.{MonadError, StackSafeMonad}
import org.http4s.{EntityDecoder, EntityEncoder, HttpApp, Request, Response, Status, Uri}
import org.http4s.Status.{InternalServerError, NotFound, Ok}
import fs2.Stream
import org.http4s.client.Client
import zio.{Task, ZIO}
import zio.interop.catz._

import java.nio.charset.StandardCharsets
import scala.util.{Failure, Success, Try}

class HttpClientStub(
    monad: MonadError[Task, Throwable],
    matchers: PartialFunction[Request[Task], Task[Response[Task]]],
    delegateHttp: Client[Task]
) extends Client[Task] {
  def whenRequestMatches(p: Request[Task] => Boolean): WhenRequest = new WhenRequest(p)

  override def run(req: Request[Task]): Resource[Task, Response[Task]] = delegateHttp.run(req)

  class WhenRequest(p: Request[Task] => Boolean) {
    def thenRespondOk(): HttpClientStub          = thenRespondWithCode(Ok, "")
    def thenRespondNotFound(): HttpClientStub    = thenRespondWithCode(NotFound, "")
    def thenRespondServerError(): HttpClientStub = thenRespondWithCode(InternalServerError, "Internal Server Error.")
    def thenRespondWithCode(status: Status, msg: String): HttpClientStub = thenRespond(
      Response[Task](status = status, body = Stream.emits(msg.getBytes(StandardCharsets.UTF_8)))
    )

    def thenRespond[T](body: T)(implicit ee: EntityEncoder[Task, T]): HttpClientStub = thenRespond(Ok, body)
    def thenRespond[T](status: Status, body: T)(implicit ee: EntityEncoder[Task, T]): HttpClientStub = {
        val data = ee.toEntity(body)
        thenRespond(Response[Task](status = status).withEntity(body))
      }
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

      val delegate = Client.apply[Task](req =>
        Resource.eval {
          Try(m.apply(req)) match {
            case Success(response) => response
            case Failure(exception) =>
              monad.raiseError(new RuntimeException(s"Behaviour not stubbed. Request: $req", exception))
          }
        }
      )
      new HttpClientStub(monad, m, delegate)
    }
  }

  override def fetch[A](req: Request[Task])(f: Response[Task] => Task[A]): Task[A] = delegateHttp.fetch(req)(f)

  override def fetch[A](req: Task[Request[Task]])(f: Response[Task] => Task[A]): Task[A] = delegateHttp.fetch(req)(f)

  override def toKleisli[A](f: Response[Task] => Task[A]): Kleisli[Task, Request[Task], A] = delegateHttp.toKleisli(f)

  override def toHttpApp: HttpApp[Task] = delegateHttp.toHttpApp

  override def stream(req: Request[Task]): Stream[Task, Response[Task]] = delegateHttp.stream(req)

  override def expectOr[A](req: Request[Task])(onError: Response[Task] => Task[Throwable])(implicit
      d: EntityDecoder[Task, A]
  ): Task[A] = delegateHttp.expectOr(req)(onError)

  override def expect[A](req: Request[Task])(implicit d: EntityDecoder[Task, A]): Task[A] = delegateHttp.expect(req)

  override def expectOr[A](req: Task[Request[Task]])(onError: Response[Task] => Task[Throwable])(implicit
      d: EntityDecoder[Task, A]
  ): Task[A] = delegateHttp.expectOr(req)(onError)

  override def expect[A](req: Task[Request[Task]])(implicit d: EntityDecoder[Task, A]): Task[A] =
    delegateHttp.expect(req)

  override def expectOr[A](uri: Uri)(onError: Response[Task] => Task[Throwable])(implicit
      d: EntityDecoder[Task, A]
  ): Task[A] = delegateHttp.expectOr(uri)(onError)

  override def expect[A](uri: Uri)(implicit d: EntityDecoder[Task, A]): Task[A] = delegateHttp.expect(uri)

  override def expectOr[A](s: String)(onError: Response[Task] => Task[Throwable])(implicit
      d: EntityDecoder[Task, A]
  ): Task[A] = delegateHttp.expectOr(s)(onError)

  override def expect[A](s: String)(implicit d: EntityDecoder[Task, A]): Task[A] = delegateHttp.expect(s)

  override def expectOptionOr[A](req: Request[Task])(onError: Response[Task] => Task[Throwable])(implicit
      d: EntityDecoder[Task, A]
  ): Task[Option[A]] = delegateHttp.expectOptionOr(req)(onError)

  override def expectOption[A](req: Request[Task])(implicit d: EntityDecoder[Task, A]): Task[Option[A]] =
    delegateHttp.expectOption(req)

  override def fetchAs[A](req: Request[Task])(implicit d: EntityDecoder[Task, A]): Task[A] = delegateHttp.fetchAs(req)

  override def fetchAs[A](req: Task[Request[Task]])(implicit d: EntityDecoder[Task, A]): Task[A] =
    delegateHttp.fetchAs(req)

  override def status(req: Request[Task]): Task[Status] = delegateHttp.status(req)

  override def status(req: Task[Request[Task]]): Task[Status] = delegateHttp.status(req)

  override def statusFromUri(uri: Uri): Task[Status] = delegateHttp.statusFromUri(uri)

  override def statusFromString(s: String): Task[Status] = delegateHttp.statusFromString(s)

  override def successful(req: Request[Task]): Task[Boolean] = delegateHttp.successful(req)

  override def successful(req: Task[Request[Task]]): Task[Boolean] = delegateHttp.successful(req)

  override def get[A](uri: Uri)(f: Response[Task] => Task[A]): Task[A] = delegateHttp.get(uri)(f)

  override def get[A](s: String)(f: Response[Task] => Task[A]): Task[A] = delegateHttp.get(s)(f)
}

object HttpClientStub {

  class ZIOMonadError extends MonadError[Task, Throwable] with StackSafeMonad[Task] {
    override def raiseError[A](e: Throwable): Task[A] = ZIO.fail(e)

    override def handleErrorWith[A](fa: Task[A])(f: Throwable => Task[A]): Task[A] = fa.catchAll(f)

    override def pure[A](x: A): Task[A] = ZIO.succeed(x)

    override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] = fa.flatMap(f)
  }

  def apply(): HttpClientStub = {
    val client = Client.apply[Task](req =>
      Resource.eval(ZIO.succeed(println("Not implemented run")) *> ZIO.effect(PartialFunction.empty.apply(req)))
    )
    new HttpClientStub(new ZIOMonadError(), PartialFunction.empty, client)
  }
}
