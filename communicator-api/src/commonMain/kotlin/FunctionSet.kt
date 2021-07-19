package space.kscience.communicator.api

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Stores a set of communicator functions signatures (endpoint+name+[FunctionSet]).
 *
 * @property endpoint The endpoint of all functions in this set.
 */
public open class FunctionSet(public val endpoint: ClientEndpoint) {
    internal val functions: MutableMap<String, Pair<Codec<*>, Codec<*>>> = hashMapOf()

    /**
     * Represents a declaration of function in the set.
     *
     * @param T the type the function takes.
     * @param R the type the function returns.
     * @property owner The set where the function was declared.
     * @property name Tha name of function.
     */
    @Suppress("UNCHECKED_CAST")
    public data class Declaration<T : Any, R : Any> internal constructor(
        val owner: FunctionSet,
        val name: String,
    ) {
        /**
         * The codec of [T].
         */
        val argumentCodec: Codec<T>
            get() = checkNotNull(owner.functions[name]) { "Can't find function $name in $owner." }.first as Codec<T>

        /**
         * The codec of [R].
         */
        val resultCodec: Codec<R>
            get() = checkNotNull(owner.functions[name]) { "Can't find function $name in $owner." }.second as Codec<R>
    }

    /**
     * Creates a function, stores it and return declaration object pointing to this set.
     *
     * @param T the type the function takes.
     * @param R the type the function returns.
     * @param name the name of function.
     * @param argumentCodec the codec of [T].
     * @param resultCodec the codec of [R].
     * @return a new declaration object.
     */
    public fun <T : Any, R : Any> declare(
        name: String,
        argumentCodec: Codec<T>,
        resultCodec: Codec<R>,
    ): Declaration<T, R> {
        functions[name] = argumentCodec to resultCodec
        return Declaration(this, name)
    }

    override fun toString(): String = "FunctionSet(endpoint='$endpoint', functions=$functions)"
}

/**
 * Returns the client function with name equal to the property's name. The spec and endpoint are taken from given
 * function set.
 *
 * @receiver the function set.
 * @param thisRef the client object.
 * @param property the property.
 * @return the function invoked by client.
 */
@Suppress("UNCHECKED_CAST")
public operator fun <T : Any, R : Any> FunctionSet.getValue(
    thisRef: FunctionClient,
    property: KProperty<*>,
): suspend (T) -> R {
    val name = property.name

    val (argumentCodec, resultCodec) = checkNotNull(functions[name] as? Pair<Codec<T>, Codec<R>>) {
        "Cannot find function $name in function set $this."
    }

    return thisRef.getFunction(endpoint, property.name, argumentCodec, resultCodec)
}

/**
 * Returns the client function with name equal to the property's name. The spec and endpoint are taken from given
 * declaration.
 *
 * @receiver the declaration from function set.
 * @param thisRef the client object.
 * @param property the property.
 * @return the function invoked by client.
 */
public operator fun <T : Any, R : Any> FunctionSet.Declaration<T, R>.getValue(
    thisRef: FunctionClient,
    property: KProperty<*>,
): suspend (T) -> R = owner.getValue(thisRef, property)

/**
 * Returns [PropertyDelegateProvider] providing [FunctionSet.Declaration] objects by using given spec and name of the
 * property.
 *
 * @param T the type the function takes.
 * @param R the type the function returns.
 * @param argumentCodec the codec of [T].
 * @param resultCodec the codec of [R].
 * @return a new declaration object.
 */
public fun <T : Any, R : Any> declare(
    argumentCodec: Codec<T>,
    resultCodec: Codec<R>,
): PropertyDelegateProvider<FunctionSet, ReadOnlyProperty<FunctionSet, FunctionSet.Declaration<T, R>>> =
    PropertyDelegateProvider { thisRef, property ->
        val d = thisRef.declare(property.name, argumentCodec, resultCodec)
        ReadOnlyProperty { _, _ -> d }
    }

/**
 * Registers a function in [FunctionServer] by its implementation and declaration. Warning, endpoint should be added to
 * the function server independently.
 *
 * @receiver the function server.
 * @param declaration the function's declaration.
 * @param function the function's implementation.
 * @return [function].
 */
public fun <T : Any, R : Any> FunctionServer.impl(
    declaration: FunctionSet.Declaration<T, R>,
    function: suspend (T) -> R,
): suspend (T) -> R {
    register(declaration.name, declaration.argumentCodec, declaration.resultCodec, function)
    return function
}

/**
 * Calls function by its declaration in the given client.
 *
 * @receiver the declaration.
 * @param T the type the function takes.
 * @param R the type the function returns.
 * @param client the client to send query.
 * @param arg the argument of the function.
 * @return the result of the function.
 */
@Suppress("RedundantSuspendModifier")
public suspend operator fun <T : Any, R : Any> FunctionSet.Declaration<T, R>.invoke(client: FunctionClient, arg: T): R =
    client.getFunction(owner.endpoint, name, argumentCodec, resultCodec)(arg)

/**
 * Configures this function server with provided function set receiver. It is usually needed to provide functions to
 * the server with [impl].
 *
 * @receiver the function server.
 * @param S the type of function set.
 * @param set the set object.
 * @param action the lambda to apply.
 * @return this function server.
 */
public inline fun <S> FunctionServer.configure(
    set: S,
    action: S.(FunctionServer) -> Unit,
): FunctionServer where S : FunctionSet {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }

    require(set.endpoint in endpoints) {
        "The endpoint ${set.endpoint} of configured set isn't present in function server."
    }

    action(set, this)
    return this
}
