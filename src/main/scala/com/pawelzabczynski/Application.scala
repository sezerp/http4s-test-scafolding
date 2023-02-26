package com.pawelzabczynski

import com.pawelzabczynski.http.HttpClientStub
import org.http4s.Uri
import zio.{ExitCode, Task, URIO, ZIO}
import zio.interop.catz._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client

case class SomeResponse(id: String, name: String, age: Int)

object Application extends zio.App {

  val stubClient = HttpClientStub()
    .whenRequestMatches(r => r.uri.path.startsWithString("/s1") && r.uri.query.has("name", "test"))
    .thenRespond(SomeResponse("id-1", "test", 25))

  def makeCall(httpClient: Client[Task]): Task[SomeResponse] = {
    httpClient.expect[SomeResponse](Uri.unsafeFromString("https://www.google.com/s1/s2/s3").withQueryParam("name", "test"))
  }

  val response = for  {
    r <- makeCall(stubClient)
    _ <- ZIO.effect(println(s"Response body: $r "))
  } yield ()


  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = response.exitCode
}
