package xmltojson

import akka.NotUsed
import akka.stream.alpakka.xml.Characters
import akka.stream.alpakka.xml.EndElement
import akka.stream.alpakka.xml.ParseEvent
import akka.stream.alpakka.xml.StartElement
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import akka.util.ccompat.IterableOnce

object XmlToJson {

  case class Position(nodeName: String, isArray: Boolean = false, hasParsedFirst: Boolean = false) {
    lazy val parsedFirst = if (hasParsedFirst) this else Position(nodeName, isArray, hasParsedFirst = true)
  }

  type Path = Seq[String]

  def convertToJson(longNumbers: Seq[Path], doubleNumbers: Seq[Path], arrays: Seq[Path]): () => ParseEvent => IterableOnce[JsonWriter] = {
    () =>
      var state: Seq[Position] = Seq.empty
      var previousWasEnd: Boolean = false
      var previousArray: Option[String] = None
      def path: Path = state.map(_.nodeName)
      //var previousNode: Option[Position] = None

      {
        case StartElement(localName, _, _, _, _) =>
          previousWasEnd = false
          val requiresSeparator = state.headOption.exists(_.hasParsedFirst)
          state = Position(localName, isArray = arrays.contains(path :+ localName)) +: state.headOption.map(h => h.parsedFirst +: state.tail).getOrElse(Nil)
          println(state)

          val newArray = state.head.isArray && !previousArray.contains(state.head.nodeName)

          // Do we need to end an array? (have we parsed at this level and do we have a previous node
          // that was an array that isn't the same name as the incoming node?
          val endArray =
            if (requiresSeparator && previousArray.isDefined && !state.head.isArray || newArray) Seq(JsonWriters.EndArray)
            else Seq.empty

          val separator =
            if (requiresSeparator) Seq(JsonWriters.FieldSeparator)
            else Seq(JsonWriters.StartObject)

          val arrayStart = {
            if (endArray.isEmpty && newArray) Seq(JsonWriters.FieldName(localName), JsonWriters.KeyValueSeparator, JsonWriters.StartArray)
            else if (state.head.isArray) Seq.empty
            else Seq(JsonWriters.FieldName(localName), JsonWriters.KeyValueSeparator)
          }

          endArray ++ separator ++ arrayStart
        case EndElement(localName) =>
          val currentPos = state.head
          val previousWasEndElement = previousWasEnd
          previousWasEnd = true
          val previousWasArray = previousArray.isDefined
          previousArray = if (currentPos.isArray) Some(currentPos.nodeName) else None
          if (state.head.nodeName == localName) {
            state = state.tail
            println(state)
            val ret = if (previousWasEndElement) {
              if (previousWasArray) Seq(JsonWriters.EndArray)
              else Seq(JsonWriters.EndObject)
            }
            else Seq.empty
            if (state.isEmpty) ret :+ JsonWriters.EndObject
            else ret
          } else {
            throw new IllegalArgumentException(s"Mismatched XML: current local name - $localName, expected: ${state.head.nodeName}") // kill switch
          }
        case Characters(text: String) =>
          val characters = text.trim
          if (characters.nonEmpty) {
            previousWasEnd = false
            val p = path
            if (longNumbers.contains(p)) Seq(JsonWriters.NumberValue(characters.toLong))
            else if (doubleNumbers.contains(p)) Seq(JsonWriters.NumberValue(characters.toDouble))
            else Seq(JsonWriters.StringValue(characters))
          }
          else Seq.empty
        case _ => Seq.empty
      }
  }

  def writeJson: Flow[JsonWriter, ByteString, NotUsed] = Flow.fromFunction(_.write)

}
