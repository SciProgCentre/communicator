package space.kscience.communicator.prettyapi

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.Type.getMethodDescriptor
import org.objectweb.asm.commons.InstructionAdapter
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

internal tailrec fun buildName(origin: Any, collision: Int = 0): String {
    val name = "space/kscience/communicator/prettyapi/generated/Compiled_${origin.hashCode()}_$collision"

    try {
        Class.forName(name)
    } catch (ignored: ClassNotFoundException) {
        return name
    }

    return buildName(origin, collision + 1)
}

/**
 * Returns ASM [Type] for given [Class].
 */
internal inline val Class<*>.asm: Type
    get() = Type.getType(this)

/**
 * Returns ASM [Type] for given [KClass].
 */
internal inline val KClass<*>.asm: Type
    get() = Type.getType(java)

/**
 * Creates an [InstructionAdapter] from this [MethodVisitor].
 */
internal fun MethodVisitor.instructionAdapter(): InstructionAdapter = InstructionAdapter(this)

/**
 * Creates an [InstructionAdapter] from this [MethodVisitor] and applies [block] to it.
 */
internal inline fun MethodVisitor.instructionAdapter(block: InstructionAdapter.() -> Unit): InstructionAdapter {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return instructionAdapter().apply(block)
}

internal operator fun Type.invoke(vararg arguments: Type): String = getMethodDescriptor(this, *arguments)

internal fun InstructionAdapter.invokeinterface(ownerType: Type, name: String, descriptor: String) =
    invokeinterface(ownerType.internalName, name, descriptor)

internal fun InstructionAdapter.invokespecial(ownerType: Type, name: String, descriptor: String, isInterface: Boolean) =
    invokespecial(ownerType.internalName, name, descriptor, isInterface)

internal fun InstructionAdapter.invokestatic(ownerType: Type, name: String, descriptor: String, isInterface: Boolean) =
    invokestatic(ownerType.internalName, name, descriptor, isInterface)

internal fun InstructionAdapter.invokevirtual(ownerType: Type, name: String, descriptor: String, isInterface: Boolean) =
    invokevirtual(ownerType.internalName, name, descriptor, isInterface)

internal fun InstructionAdapter.putfield(ownerType: Type, name: String, descriptorType: Type) =
    putfield(ownerType.internalName, name, descriptorType.descriptor)

internal fun InstructionAdapter.getfield(ownerType: Type, name: String, descriptorType: Type) =
    getfield(ownerType.internalName, name, descriptorType.descriptor)

internal fun InstructionAdapter.getstatic(ownerType: Type, name: String, descriptorType: Type) =
    getstatic(ownerType.internalName, name, descriptorType.descriptor)

internal fun ClassVisitor.visitInnerClass(
    type: Type,
    outerType: Type?,
    innerName: String?,
    access: Int,
) = visitInnerClass(type.internalName, outerType?.internalName, innerName, access)

internal fun ClassVisitor.visitOuterClass(
    ownerType: Type,
    name: String?,
    descriptor: String?,
) = visitOuterClass(ownerType.internalName, name, descriptor)
