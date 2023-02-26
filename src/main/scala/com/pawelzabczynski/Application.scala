package com.pawelzabczynski

import cats.effect.kernel.Resource
import com.pawelzabczynski.http.HttpClientStub
import org.http4s.{Uri}
import zio.{ExitCode, URIO, ZIO}
import zio.interop.catz._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.circe.CirceEntityCodec._

case class SomeResponse(id: String, name: String, age: Int)

object Application extends zio.App {

  val stubClient = HttpClientStub()
    .whenRequestMatches(r => r.uri.path.startsWithString("/s1") && r.uri.containsQueryParam("name"))
    .thenRespond(SomeResponse("id-1", "test", 25))

  val response = for  {
    r <- stubClient.expect[SomeResponse](Uri.unsafeFromString("https://www.google.com/s1/s2/s3").withQueryParam("name", "test"))
    _ <- ZIO.effect(println(s"Response body: $r "))
  } yield ()


  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = response.exitCode
}
