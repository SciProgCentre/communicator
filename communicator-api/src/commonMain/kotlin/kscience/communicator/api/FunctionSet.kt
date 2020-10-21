package kscience.communicator.api

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
public abstract class FunctionSet(public val endpoint: Endpoint) {
    internal val functions: MutableMap<String, FunctionSpec<*, *>> = hashMapOf()

    /**
     * Represents a declaration of function in the set.
     *
     * @property owner The set where the function was declared.
     * @property name Tha name of function.
     * @property spec The spec of the function.
     */
    @Suppress("UNCHECKED_CAST")
    public data class Declaration<T, R> internal constructor(
        val owner: FunctionSet,
        val name: String
    ) {
        val spec: FunctionSpec<T, R>
            get() = owner.functions[name] as FunctionSpec<T, R>
    }

    /**
     * Creates a function, stores it and return declaration object pointing to this set.
     *
     * @param T the type the function takes.
     * @param R the type the function returns.
     * @param name the name of function.
     * @param spec the spec of function.
     * @return a new declaration object.
     */
    public fun <T, R> declare(name: String, spec: FunctionSpec<T, R>): Declaration<T, R> {
        functions[name] = spec
        return Declaration(this, name)
    }

    public override fun toString(): String = "FunctionSet(endpoint='$endpoint', functions=$functions)"
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
public operator fun <T, R> FunctionSet.getValue(
    thisRef: FunctionClient,
    property: KProperty<*>
): suspend (T) -> R {
    val name = property.name

    val spec =
        checkNotNull(functions[name] as? FunctionSpec<T, R>) { "Cannot find function $name in function set $this." }

    return thisRef.getFunction(endpoint, property.name, spec)
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
public operator fun <T, R> FunctionSet.Declaration<T, R>.getValue(
    thisRef: FunctionClient,
    property: KProperty<*>
): suspend (T) -> R = owner.getValue(thisRef, property)

/**
 * Creates a function, stores it and return declaration object pointing to this set.
 *
 * @param T the type the function takes.
 * @param R the type the function returns.
 * @param nameToSpec pair of the name and the spec of function.
 * @return a new declaration object.
 */
public fun <T, R> FunctionSet.declare(nameToSpec: Pair<String, FunctionSpec<T, R>>): FunctionSet.Declaration<T, R> =
    declare(nameToSpec.first, nameToSpec.second)

public fun <T, R> declare(spec: FunctionSpec<T, R>): PropertyDelegateProvider<FunctionSet, ReadOnlyProperty<FunctionSet, FunctionSet.Declaration<T, R>>> =
    PropertyDelegateProvider { thisRef, property ->
        val d = thisRef.declare(property.name to spec)
        ReadOnlyProperty { _, _ -> d }
    }

/**
 * Registers a function in [FunctionServer] by its implementation and declaration. Warning, endpoint should be added to
 * the function server independently.
 *
 * @receiver the function server.
 * @param declaration the function's declaration.
 * @param function the function's implementation.
 * @receiver the function's implementation.
 */
public fun <T, R> FunctionServer.impl(
    declaration: FunctionSet.Declaration<T, R>,
    function: suspend (T) -> R
): suspend (T) -> R = register(declaration.name, declaration.spec, function)

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
public suspend operator fun <T, R> FunctionSet.Declaration<T, R>.invoke(client: FunctionClient, arg: T): R =
    client.getFunction(owner.endpoint, name, spec).invoke(arg)

/**
 * Configures this function server with provided function set receiver. It is usually needed to provide functions to
 * the server with [impl].
 *
 * @receiver the function server.
 * @param F the type of function server.
 * @param S the type of function set.
 * @param set the set object.
 * @param action the lambda to apply.
 * @return this function server.
 */
public inline fun <F, S> F.configure(set: S, action: S.(F) -> Unit): F where F : FunctionServer, S : FunctionSet {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    action(set, this)
    return this
}
