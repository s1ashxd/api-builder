package ru.s1ash.apibuilder.structs

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import ru.s1ash.apibuilder.structs.interfaces.IHasIdentifier
import ru.s1ash.apibuilder.structs.types.ReplaceType
import java.lang.reflect.Type

data class ReplaceField(
    override val id: String,
    val name: String,
    val replaceType: ReplaceType,
    val targets: List<String>,
    val defaults: String?
) : IHasIdentifier {

    object Deserializer : JsonDeserializer<ReplaceField> {
        override fun deserialize(p0: JsonElement, p1: Type, p2: JsonDeserializationContext): ReplaceField {
            val obj = p0.asJsonObject
            val id = obj.get("id").asString
            return ReplaceField(
                id,
                obj.get("name")?.asString ?: id,
                ReplaceType.valueOf(obj.get("replace_type").asString),
                obj.get("targets")?.asJsonArray?.map { it.asString } ?: emptyList(),
                obj.get("defaults")?.asString
            )
        }
    }

}