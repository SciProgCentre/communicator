package space.kscience.communicator.prettyapi

import kotlin.reflect.KFunction
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.valueParameters

internal fun KFunction<*>.isEquals() = name == "equals"
        && instanceParameter != null
        && returnType.classifier == Boolean::class
        && valueParameters.size == 1
        && valueParameters.first().type.let { it.classifier == Any::class && it.isMarkedNullable }

internal fun KFunction<*>.isHashCode() =
    name == "hashCode" && instanceParameter != null && returnType.classifier == Int::class && valueParameters.isEmpty()

internal fun KFunction<*>.isToString() = name == "toString"
        && instanceParameter != null
        && returnType.classifier == String::class
        && valueParameters.isEmpty()
