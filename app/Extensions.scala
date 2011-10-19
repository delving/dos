package controllers.dos

import org.codehaus.jackson.map.annotate.JsonCachable
import org.bson.types.ObjectId
import org.codehaus.jackson.map.{DeserializationContext, JsonDeserializer, SerializerProvider, JsonSerializer}
import org.codehaus.jackson.map.module.SimpleModule
import org.codehaus.jackson.{Version, JsonParser, JsonGenerator}
import play.mvc._
import play.mvc.Http.{Response, Request}
import results.{Result, RenderJson}


/**
 * This trait provides additional actions that can be used in controllers
 */
trait AdditionalActions {
  self: Controller =>

  def Json(data: AnyRef): Result = new RenderJson() {
    override def apply(request: Request, response: Response) {
      val encoding = getEncoding
      setContentTypeIfNotSet(response, "application/json; charset=" + encoding)
      response.out.write(DoSJson.generate(data).getBytes(encoding))
    }
  }
}

@JsonCachable
class ObjectIdSerializer extends JsonSerializer[ObjectId] {
  def serialize(id: ObjectId, json: JsonGenerator, provider: SerializerProvider) {
    json.writeString(id.toString)
  }
}

class ObjectIdDeserializer extends JsonDeserializer[ObjectId] {
  def deserialize(jp: JsonParser, context: DeserializationContext) = {
    if (!ObjectId.isValid(jp.getText)) throw context.mappingException("invalid ObjectId " + jp.getText)
    new ObjectId(jp.getText)
  }
}

object DoSJson extends com.codahale.jerkson.Json {
  val module = new SimpleModule("DoS", Version.unknownVersion())
  module.addSerializer(classOf[ObjectId], new ObjectIdSerializer)
  module.addDeserializer(classOf[ObjectId], new ObjectIdDeserializer)
  mapper.registerModule(module)
}