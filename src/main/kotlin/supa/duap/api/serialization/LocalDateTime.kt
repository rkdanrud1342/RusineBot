package supa.duap.api.serialization

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withLocale(Locale.KOREA)

class LocalDateTimeSerializer : JsonSerializer<LocalDateTime> {
    override fun serialize(
        localDateTime : LocalDateTime,
        type : Type,
        context : JsonSerializationContext
    ) = JsonPrimitive(formatter.format(localDateTime))
}

class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime> {
    override fun deserialize(
        json : JsonElement,
        type : Type,
        context : JsonDeserializationContext
    ) : LocalDateTime {
        return LocalDateTime.parse(
            json.asString,
            formatter
        )
    }
}
