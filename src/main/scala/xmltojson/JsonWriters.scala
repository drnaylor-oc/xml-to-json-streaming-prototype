package xmltojson

import akka.util.ByteString

trait JsonWriter {
  def write: ByteString
}

abstract class JsonString(string: String) extends JsonWriter {
  lazy val write = ByteString("\"" + string + "\"")
}

object JsonWriters {

  case object StartObject extends JsonWriter {
    val write = ByteString("{")
  }

  case object EndObject extends JsonWriter {
    val write = ByteString("}")
  }

  case object StartArray extends JsonWriter {
    val write = ByteString("[")
  }

  case object EndArray extends JsonWriter {
    val write = ByteString("]")
  }

  case object FieldSeparator extends JsonWriter {
    val write = ByteString(",")
  }

  case object KeyValueSeparator extends JsonWriter {
    val write = ByteString(":")
  }

  case class FieldName(string: String) extends JsonString(string)
  case class StringValue(string: String) extends JsonString(string)

  case class NumberValue(number: Number) extends JsonWriter {
    lazy val write: ByteString = ByteString(number.toString)
  }

  case class BooleanValue(value: Boolean) extends JsonWriter {
    lazy val write: ByteString = ByteString(value.toString)
  }

}
