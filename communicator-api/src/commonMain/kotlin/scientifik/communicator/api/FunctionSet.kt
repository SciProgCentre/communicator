package scientifik.communicator.api

import kotlin.reflect.KProperty

abstract class FunctionSet(val endpoint: Endpoint) {
    internal val functions: MutableMap<String, FunctionSpec<*, *>> = hashMapOf()

    class Declaration<T, R>(
        val owner: FunctionSet,
        val name: String,
        val spec: FunctionSpec<T, R>
    ) {
        override fun toString(): String = "Declaration(owner=$owner, name='$name', spec=$spec)"
    }

    fun <T, R> declare(name: String, spec: FunctionSpec<T, R>): Declaration<T, R> {
        functions[name] = spec
        return Declaration(this, name, spec)
    }

    override fun toString(): String = "FunctionSet(endpoint='$endpoint', functions=$functions)"
}

@Suppress("UNCHECKED_CAST")
operator fun <T, R> FunctionSet.getValue(
    thisRef: FunctionClient,
    property: KProperty<*>
): suspend (T) -> R {
    val name = property.name

    val spec =
        checkNotNull(functions[name] as? FunctionSpec<T, R>) { "Cannot find function $name in function set $this." }

    return thisRef.getFunction(endpoint, property.name, spec)
}

@Suppress("UNCHECKED_CAST")
operator fun <T, R> FunctionSet.Declaration<T, R>.getValue(
    thisRef: FunctionClient,
    property: KProperty<*>
): suspend (T) -> R = owner.getValue(thisRef, property)

fun <T, R> FunctionSet.declare(nameToSpec: Pair<String, FunctionSpec<T, R>>): FunctionSet.Declaration<T, R> =
    declare(nameToSpec.first, nameToSpec.second)

fun <T, R> FunctionServer.impl(declaration: FunctionSet.Declaration<T, R>, function: suspend (T) -> R) {
    register(declaration.name, declaration.spec, function)
}

@Suppress("RedundantSuspendModifier")
suspend operator fun <T, R> FunctionSet.Declaration<T, R>.invoke(client: FunctionClient, arg: T): R =
    client.getFunction(owner.endpoint, name, spec)(arg)

inline fun <F, S> F.configure(set: S, action: S.(_: F) -> Unit): F where F : FunctionServer, S : FunctionSet {
    action(set, this)
    return this
}
