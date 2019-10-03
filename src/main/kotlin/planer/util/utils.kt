package planer.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object Util {
    val propMap = getPropMap()
}

fun getPropMap() =
    DUMMY_OBJECT.javaClass.getResource(PROPERTIES)
        .readText()
        .split("\n")
        .map { it.replace("[\\s|\\r]".toRegex(), "").split("=") }
        .filter { it.size == 2 && it[0].isNotBlank() && it[1].isNotBlank() }
        .map { it[0] to it[1] }
        .toMap()

fun getProp(propKey: String, default: String) = Util.propMap[propKey] ?: default

fun getProp(envKey: String, propKey: String, default: String) =
    System.getenv(envKey) ?: Util.propMap[propKey] ?: default

val globalObjectMapper = jacksonObjectMapper().apply {
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
}

