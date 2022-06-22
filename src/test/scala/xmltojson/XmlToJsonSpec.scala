package xmltojson

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.xml.scaladsl.XmlParsing
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.DurationInt

class XmlToJsonSpec extends AnyFreeSpec with Matchers with ScalaFutures {

  val actorSystem = ActorSystem("test")
  implicit val materializer = Materializer(actorSystem)

  val xml = <node>
    <child>1</child>
    <other>
      <otherother>final</otherother>
      <po>alsofinal</po>
    </other>
  </node>

  "conversion" - {

    "basic node to Json" in {
      val source = Source.single(ByteString(xml.mkString, StandardCharsets.UTF_8))
      val result = source
        .via(XmlParsing.parser)
        .statefulMapConcat(XmlToJson.convertToJson(Seq.empty, Seq.empty, Seq.empty))
        .via(XmlToJson.writeJson)
        .fold(ByteString())(_ ++ _)
        .map(_.utf8String)
        .via(Flow.fromFunction {
          string =>
            println(string)
            string
        })
        .map(Json.parse)
        .runWith(Sink.head)

      whenReady(result, timeout = Timeout(1.second)) {
        _ mustBe Json.obj(
          "node" -> Json.obj(
            "child" -> "1",
            "other" -> Json.obj(
              "otherother" -> "final",
              "po" -> "alsofinal"
            )
          )
        )
      }
    }

  }

}
