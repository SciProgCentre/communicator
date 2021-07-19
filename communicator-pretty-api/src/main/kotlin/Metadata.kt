package space.kscience.communicator.prettyapi

import kotlinx.serialization.Serializable
import space.kscience.communicator.api.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.*

internal class ApiMetadata(
    val classOfT: KClass<*>,
    val context: CommunicatorContext,
    val functionSet: FunctionSet,
    val mapping: Map<KFunction<*>, FunctionSet.Declaration<*, *>>,
)

internal fun CommunicatorContext.resolveType(type: KType): Codec<*> {
    // TODO lambdas
    val classifier = type.classifier
    require(classifier is KClass<*>) { "API class can't use type parameters." }
    require(!type.isMarkedNullable) { "Nullable types use in API class isn't allowed." }
    codecsRegistry[type.classifier]?.let { return it }
    if (classifier.hasAnnotation<Serializable>()) return serializableFormat(classifier)

    if (classifier == List::class) {
        val typeOfElement = type.arguments[0].type
            ?: throw IllegalArgumentException("List used in API class can't use star projection.")

        return ListCodec(resolveType(typeOfElement))
    }

    if (classifier == Pair::class) {
        val typeOfA = type.arguments[0].type
            ?: throw IllegalArgumentException("Pair used in API class can't use star projection.")

        val typeOfB = type.arguments[1].type
            ?: throw IllegalArgumentException("Pair used in API class can't use star projection.")

        return PairCodec(resolveType(typeOfA), resolveType(typeOfB))
    }

    if (classifier == Triple::class) {
        val typeOfA = type.arguments[0].type
            ?: throw IllegalArgumentException("Triple used in API class can't use star projection.")

        val typeOfB = type.arguments[1].type
            ?: throw IllegalArgumentException("Triple used in API class can't use star projection.")

        val typeOfC = type.arguments[2].type
            ?: throw IllegalArgumentException("Triple used in API class can't use star projection.")

        return TripleCodec(resolveType(typeOfA), resolveType(typeOfB), resolveType(typeOfC))
    }

    throw IllegalArgumentException("Can't resolve type $type.")
}

internal fun <T : Any> CommunicatorContext.scanInterface(
    classOfT: KClass<T>,
    endpoint: ClientEndpoint,
): ApiMetadata {
    require(classOfT.java.isInterface) { "The API class must be declared interface." }
    require(classOfT.typeParameters.isEmpty()) { "API class can't have type parameters." }

    val functions = classOfT.memberFunctions.filterNot {
        it.isEquals() || it.isHashCode() || it.isToString()
    }

    require(functions.all { it.isAbstract }) { "All methods in API class must be abstract." }

    require(
        functions.map(KFunction<*>::name).let {
            val mutSet = mutableSetOf<String>()
            it.all(mutSet::add)
        }
    ) { "All methods in API class must have unique names." }

    val set = FunctionSet(endpoint)
    val mapping = HashMap<KFunction<*>, FunctionSet.Declaration<*, *>>()

    functions.forEach { function ->
        require(function.typeParameters.isEmpty()) { "Function in API class can't have type parameters." }
        require(function.instanceParameter != null) { "Function in API class must have instance parameter." }
        require(function.extensionReceiverParameter == null) { "Function in API class can't be extension." }
        require(function.isSuspend) { "Function in API class must be suspending." }

        require(function.valueParameters.any(KParameter::isOptional)) {
            "Function in API class can't have optional parameters."
        }

        val argumentCodec = when (function.valueParameters.size) {
            0 -> UnitCodec
            1 -> resolveType(function.valueParameters.first().type)
            else -> TupleCodec(function.valueParameters.map(KParameter::type).map { resolveType(it) })
        }

        val resultCodec = resolveType(function.returnType)
        mapping[function] = set.declare(function.name, argumentCodec, resultCodec)
    }

    return ApiMetadata(classOfT, this, set, mapping)
}