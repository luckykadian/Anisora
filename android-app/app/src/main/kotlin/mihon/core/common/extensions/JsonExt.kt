package mihon.core.common.extensions

import kotlinx.serialization.json.JsonObject

val JsonObject.Companion.EMPTY: JsonObject
    get() = JsonObject(emptyMap())
