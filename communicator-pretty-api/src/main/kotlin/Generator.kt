@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "LocalVariableName")

package space.kscience.communicator.prettyapi

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.InstructionAdapter
import space.kscience.communicator.api.ClientEndpoint
import space.kscience.communicator.api.FunctionClient
import java.nio.file.Paths
import kotlin.coroutines.Continuation
import kotlin.io.path.writeBytes
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod

private val JAVA_LANG_BOOLEAN_TYPE = java.lang.Boolean::class.asm
private val JAVA_LANG_BYTE_TYPE = java.lang.Byte::class.asm
private val CHARACTER_TYPE = java.lang.Character::class.asm
private val CLIENT_ENDPOINT_TYPE = ClientEndpoint::class.asm
private val COLLECTIONS_KT_TYPE = Type.getObjectType("kotlin/collections/CollectionsKt")!!
private val CONTINUATION_TYPE = Continuation::class.asm
private val CONTINUATION_IMPL_TYPE = Type.getObjectType("kotlin/coroutines/jvm/internal/ContinuationImpl")!!
private val COROUTINES_INTRINSICS_TYPE = Type.getObjectType("kotlin/coroutines/intrinsics/IntrinsicsKt")!!
private val JAVA_LANG_DOUBLE_TYPE = java.lang.Double::class.asm
private val JAVA_LANG_FLOAT_TYPE = java.lang.Float::class.asm
private val FUNCTION_2_TYPE = Type.getObjectType("kotlin/jvm/functions/Function2")!!
private val FUNCTION_CLIENT_TYPE = FunctionClient::class.asm
private val ILLEGAL_STATE_EXCEPTION_TYPE = java.lang.IllegalStateException::class.asm
private val IMPLEMENTATION_BASE_TYPE = ImplementationBase::class.asm
private val INTEGER_TYPE = java.lang.Integer::class.asm
private val JVM_INTRINSICS_TYPE = Type.getObjectType("kotlin/jvm/internal/Intrinsics")!!
private val LIST_TYPE = java.util.List::class.asm
private val JAVA_LANG_LONG_TYPE = java.lang.Long::class.asm
private val MAP_TYPE = java.util.Map::class.asm
private val OBJECT_TYPE = Object::class.asm
private val OBJECT_ARRAY_TYPE = Array<Object>::class.asm
private val RESULT_KT_TYPE = Type.getObjectType("kotlin/ResultKt")!!
private val JAVA_LANG_SHORT_TYPE = java.lang.Short::class.asm
private val STRING_TYPE = java.lang.String::class.asm
private val UNIT_TYPE = Unit::class.asm

internal fun makeObject(
    apiClass: KClass<*>,
    client: FunctionClient,
    endpoint: ClientEndpoint,
    metadata: ApiMetadata,
): Any {
    val name = buildName(apiClass)
    val dump = Dump(Type.getObjectType(name), apiClass.asm, metadata)
    val `class` = dump.dump()

    return `class`
        .getDeclaredConstructor(FunctionClient::class.java, ClientEndpoint::class.java, Map::class.java)
        .newInstance(client, endpoint, metadata.mapping.mapKeys { (k, _) -> k.name })
}

private class Dump(val thisType: Type, val apiType: Type, val apiMetadata: ApiMetadata) {
    @Throws(Exception::class)
    fun dump(): Class<*> {
        val loader = object : ClassLoader(javaClass.classLoader) {
            fun defineClass(name: String?, b: ByteArray): Class<*> = defineClass(name, b, 0, b.size)
        }

        val classes = mutableListOf<ByteArray>()

        val classWriter = object : ClassWriter(COMPUTE_FRAMES) {
            override fun getClassLoader(): ClassLoader = loader
        }

        classWriter.visit(
            V1_8,
            ACC_PUBLIC or ACC_FINAL or ACC_SUPER,
            thisType.internalName,
            null,
            IMPLEMENTATION_BASE_TYPE.internalName,
            arrayOf(apiType.internalName),
        )

        classWriter.visitMethod(
            ACC_PUBLIC,
            "<init>",
            Type.VOID_TYPE(FUNCTION_CLIENT_TYPE, CLIENT_ENDPOINT_TYPE, MAP_TYPE),
            null,
            null,
        ).instructionAdapter {
            visitCode()
            val label0 = Label()
            visitLabel(label0)
            load(1, FUNCTION_CLIENT_TYPE)
            aconst("client")
            invokestatic(JVM_INTRINSICS_TYPE, "checkNotNullParameter", Type.VOID_TYPE(OBJECT_TYPE, STRING_TYPE), false)
            load(2, CLIENT_ENDPOINT_TYPE)
            aconst("endpoint")
            invokestatic(JVM_INTRINSICS_TYPE, "checkNotNullParameter", Type.VOID_TYPE(OBJECT_TYPE, STRING_TYPE), false)
            load(3, MAP_TYPE)
            aconst("declarations")
            invokestatic(JVM_INTRINSICS_TYPE, "checkNotNullParameter", Type.VOID_TYPE(OBJECT_TYPE, STRING_TYPE), false)
            val label1 = Label()
            visitLabel(label1)
            load(0, thisType)
            load(1, FUNCTION_CLIENT_TYPE)
            load(2, CLIENT_ENDPOINT_TYPE)
            load(3, MAP_TYPE)

            invokespecial(
                IMPLEMENTATION_BASE_TYPE,
                "<init>",
                Type.VOID_TYPE(FUNCTION_CLIENT_TYPE, CLIENT_ENDPOINT_TYPE, MAP_TYPE),
                false,
            )

            val label2 = Label()
            visitLabel(label2)
            visitInsn(RETURN)
            val label3 = Label()
            visitLabel(label3)
            visitLocalVariable("this", thisType.descriptor, null, label0, label3, 0)
            visitLocalVariable("client", FUNCTION_CLIENT_TYPE.descriptor, null, label0, label3, 1)
            visitLocalVariable("endpoint", CLIENT_ENDPOINT_TYPE.descriptor, null, label0, label3, 2)
            visitLocalVariable("declarations", MAP_TYPE.descriptor, null, label0, label3, 3)
            visitMaxs(0, 0)
            visitEnd()
        }

        apiMetadata.mapping.forEach { (function, declaration) ->
            val interfaceMethod = checkNotNull(function.javaMethod)
            val targetFunctionName = interfaceMethod.name
            val targetFunctionDescriptor = Type.getMethodDescriptor(interfaceMethod)
            val innerType = Type.getObjectType("${thisType.internalName}\$$targetFunctionName\$1")

            // assuming that classifiers are Classes because of verification
            val targetFunctionParameters = interfaceMethod.parameters.map { it.name!! to (it.type as Class<*>).asm }

            classWriter.visitInnerClass(innerType, null, null, ACC_FINAL or ACC_STATIC)

            val innerDump = InnerDump(
                innerType,
                thisType,
                targetFunctionName,
                targetFunctionDescriptor,
                targetFunctionParameters.map(Pair<String, Type>::second),
            ).dump()

            classes += innerDump
            loader.defineClass(innerType.className, innerDump)

            classWriter.visitMethod(
                ACC_PUBLIC,
                targetFunctionName,
                targetFunctionDescriptor,
                null,
                null,
            ).instructionAdapter {
                val thisVar = 0
                // the last one is always CONTINUATION_TYPE

                val parametersVars = ArrayList<Int>(targetFunctionParameters.size)
                val parametersTypes = LinkedHashMap<Int, Type>()
                var previousSum = thisType.size

                for ((index, parameter) in targetFunctionParameters.withIndex()) {
                    val (_, type) = parameter
                    val parameterVar = index + previousSum
                    previousSum += type.size - 1
                    parametersVars += parameterVar
                    parametersTypes[parameterVar] = type
                }

                val arrayVar = if (parametersVars.size > 1) parametersVars.last() + 1 else parametersVars.last()
                val `$resultVar` = arrayVar + 1
                val `$continuationVar` = `$resultVar` + 1
                val unknownVar = `$continuationVar` + 1
                visitCode()
                println(parametersTypes)
                println(parametersTypes[parametersVars.last()])
                load(parametersVars.last(), CONTINUATION_TYPE)
                instanceOf(innerType)
                val label0 = Label()
                ifeq(label0)
                load(parametersVars.last(), CONTINUATION_TYPE)
                checkcast(innerType)
                store(`$continuationVar`, CONTINUATION_TYPE)
                load(`$continuationVar`, CONTINUATION_TYPE)
                getfield(innerType, "label", Type.INT_TYPE)
                iconst(-2147483648)
                and(Type.INT_TYPE)
                ifeq(label0)
                load(`$continuationVar`, CONTINUATION_TYPE)
                dup()
                getfield(innerType, "label", Type.INT_TYPE)
                iconst(-2147483648)
                sub(Type.INT_TYPE)
                putfield(innerType, "label", Type.INT_TYPE)
                val label1 = Label()
                goTo(label1)
                visitLabel(label0)
                anew(innerType)
                dup()
                load(thisVar, thisType)
                load(parametersVars.last(), CONTINUATION_TYPE)
                invokespecial(innerType, "<init>", Type.VOID_TYPE(thisType, CONTINUATION_TYPE), false)
                store(`$continuationVar`, CONTINUATION_TYPE)
                visitLabel(label1)
                load(`$continuationVar`, CONTINUATION_TYPE)
                getfield(innerType, "result", OBJECT_TYPE)
                store(`$resultVar`, OBJECT_TYPE)
                val label2 = Label()
                visitLabel(label2)
                invokestatic(COROUTINES_INTRINSICS_TYPE, "getCOROUTINE_SUSPENDED", OBJECT_TYPE(), false)
                val label3 = Label()
                visitLabel(label3)
                visitVarInsn(ASTORE, unknownVar)
                load(`$continuationVar`, CONTINUATION_TYPE)
                getfield(innerType, "label", Type.INT_TYPE)
                val label4 = Label()
                val label5 = Label()
                val label6 = Label()
                visitTableSwitchInsn(0, 1, label6, label4, label5)
                visitLabel(label4)
                load(`$resultVar`, OBJECT_TYPE)
                invokestatic(RESULT_KT_TYPE, "throwOnFailure", Type.VOID_TYPE(OBJECT_TYPE), false)
                val label7 = Label()
                visitLabel(label7)
                load(thisVar, thisType)
                aconst(declaration.name)
                invokevirtual(thisType, "getRawFunction", FUNCTION_2_TYPE(STRING_TYPE), false)

                // The last parameter is skipped since it is continuation.

                when (val s = parametersVars.size - 1) {
                    0 -> getstatic(UNIT_TYPE, "INSTANCE", UNIT_TYPE)

                    1 -> {
                        val type = parametersTypes.getValue(parametersVars.first())
                        load(parametersVars.first(), type)
                        boxingIfNeeded(type)
                    }

                    else -> {
                        iconst(s)
                        newarray(OBJECT_TYPE)
                        store(arrayVar, OBJECT_ARRAY_TYPE)

                        parametersVars.dropLast(1).forEachIndexed { idx, parameterVar ->
                            load(arrayVar, OBJECT_ARRAY_TYPE)
                            iconst(idx)
                            val type = parametersTypes.getValue(parameterVar)
                            load(parameterVar, type)
                            boxingIfNeeded(type)
                            astore(OBJECT_TYPE)
                        }

                        load(arrayVar, OBJECT_ARRAY_TYPE)
                        invokestatic(COLLECTIONS_KT_TYPE, "listOf", LIST_TYPE(OBJECT_ARRAY_TYPE), false)
                    }
                }

                load(`$continuationVar`, CONTINUATION_TYPE)
                load(`$continuationVar`, CONTINUATION_TYPE)
                iconst(1)
                putfield(innerType, "label", Type.INT_TYPE)
                invokeinterface(FUNCTION_2_TYPE, "invoke", OBJECT_TYPE(OBJECT_TYPE, OBJECT_TYPE))
                val label8 = Label()
                visitLabel(label8)
                dup()
                visitVarInsn(ALOAD, unknownVar)
                val label9 = Label()
                ifacmpne(label9)
                val label10 = Label()
                visitLabel(label10)
                visitVarInsn(ALOAD, unknownVar)
                visitInsn(ARETURN)
                visitLabel(label5)
                load(`$resultVar`, OBJECT_TYPE)
                invokestatic(RESULT_KT_TYPE, "throwOnFailure", Type.VOID_TYPE(OBJECT_TYPE), false)
                load(`$resultVar`, OBJECT_TYPE)
                visitLabel(label9)
                visitInsn(ARETURN)
                visitLabel(label6)
                anew(ILLEGAL_STATE_EXCEPTION_TYPE)
                dup()
                aconst("call to 'resume' before 'invoke' with coroutine")
                invokespecial(ILLEGAL_STATE_EXCEPTION_TYPE, "<init>", Type.VOID_TYPE(STRING_TYPE), false)
                athrow()
                visitLocalVariable("this", thisType.descriptor, null, label7, label8, thisVar)

                for ((idx, parameter) in targetFunctionParameters.withIndex()) {
                    val (name, type) = parameter
                    visitLocalVariable(name, type.descriptor, null, label7, label8, parametersVars[idx])
                }

                visitLocalVariable("\$result", OBJECT_TYPE.descriptor, null, label2, label6, `$resultVar`)

                visitLocalVariable(
                    "\$continuation",
                    CONTINUATION_TYPE.descriptor,
                    null,
                    label1,
                    label6,
                    `$continuationVar`,
                )

                visitMaxs(0, 0)
                visitEnd()
            }
        }

        classWriter.visitEnd()
        val result = classWriter.toByteArray()
        val resultClass = loader.defineClass(thisType.className, classWriter.toByteArray())
        classes += result

        if (System.getProperty("space.kscience.communicator.prettyapi.dump.generated.classes") == "1") {
            val t = System.currentTimeMillis()
            classes.forEachIndexed { idx, array -> Paths.get("dump-$t-$idx.class").writeBytes(array) }
        }

        return resultClass
    }

    private fun InstructionAdapter.boxingIfNeeded(type: Type) {
        fun doBoxing(primitiveType: Type, boxType: Type) =
            invokestatic(boxType.internalName, "valueOf", boxType(primitiveType), false)

        when (type.sort) {
            Type.BOOLEAN -> doBoxing(Type.BOOLEAN_TYPE, JAVA_LANG_BOOLEAN_TYPE)
            Type.BYTE -> doBoxing(Type.BYTE_TYPE, JAVA_LANG_BYTE_TYPE)
            Type.CHAR -> doBoxing(Type.CHAR_TYPE, CHARACTER_TYPE)
            Type.SHORT -> doBoxing(Type.SHORT_TYPE, JAVA_LANG_SHORT_TYPE)
            Type.INT -> doBoxing(Type.INT_TYPE, INTEGER_TYPE)
            Type.FLOAT -> doBoxing(Type.FLOAT_TYPE, JAVA_LANG_FLOAT_TYPE)
            Type.LONG -> doBoxing(Type.LONG_TYPE, JAVA_LANG_LONG_TYPE)
            Type.DOUBLE -> doBoxing(Type.DOUBLE_TYPE, JAVA_LANG_DOUBLE_TYPE)
        }
    }
}

private class InnerDump(
    val thisType: Type,
    val outerType: Type,
    val targetFunctionName: String,
    val targetFunctionDescriptor: String,
    val targetFunctionParameterTypes: List<Type>,
) {
    @Throws(Exception::class)
    fun dump(): ByteArray {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        classWriter.visit(
            V1_8,
            ACC_FINAL or ACC_SUPER,
            thisType.internalName,
            null,
            CONTINUATION_IMPL_TYPE.internalName,
            null,
        )

        classWriter.visitOuterClass(outerType, targetFunctionName, targetFunctionDescriptor)
        classWriter.visitInnerClass(thisType, null, null, ACC_FINAL or ACC_STATIC)

        classWriter.visitField(ACC_SYNTHETIC, "result", OBJECT_TYPE.descriptor, null, null).run {
            visitEnd()
        }

        classWriter.visitField(
            ACC_FINAL or ACC_SYNTHETIC,
            "this$0",
            outerType.descriptor,
            null,
            null,
        ).run {
            visitEnd()
        }

        classWriter.visitField(0, "label", Type.INT_TYPE.descriptor, null, null).run {
            visitEnd()
        }

        classWriter.visitMethod(
            0,
            "<init>",
            Type.VOID_TYPE(outerType, CONTINUATION_TYPE),
            null,
            null,
        ).instructionAdapter {
            visitCode()
            val label0 = Label()
            visitLabel(label0)
            load(0, thisType)
            load(1, outerType)
            putfield(thisType, "this$0", outerType)
            load(0, thisType)
            load(2, CONTINUATION_TYPE)
            invokespecial(CONTINUATION_IMPL_TYPE, "<init>", Type.VOID_TYPE(CONTINUATION_TYPE), false)
            visitInsn(RETURN)
            val label1 = Label()
            visitLabel(label1)
            visitLocalVariable("this", thisType.descriptor, null, label0, label1, 0)
            visitLocalVariable("this$0", outerType.descriptor, null, label0, label1, 1)
            visitLocalVariable("\$completion", CONTINUATION_TYPE.descriptor, null, label0, label1, 2)
            visitMaxs(0, 0)
            visitEnd()
        }

        classWriter.visitMethod(
            ACC_PUBLIC or ACC_FINAL,
            "invokeSuspend",
            OBJECT_TYPE(OBJECT_TYPE),
            null,
            null,
        ).instructionAdapter {
            visitCode()
            val label0 = Label()
            visitLabel(label0)
            load(0, thisType)
            visitVarInsn(ALOAD, 1)
            putfield(thisType, "result", OBJECT_TYPE)
            load(0, thisType)
            load(0, thisType)
            getfield(thisType, "label", Type.INT_TYPE)
            iconst(-2147483648)
            or(Type.INT_TYPE)
            putfield(thisType, "label", Type.INT_TYPE)
            load(0, thisType)
            getfield(thisType, "this$0", outerType)

            // Dropping continuation, too
            targetFunctionParameterTypes.dropLast(1).forEach { type ->
                when (type.sort) {
                    Type.INT -> iconst(0)
                    Type.LONG -> lconst(0)
                    Type.DOUBLE -> dconst(0.0)
                    Type.FLOAT -> fconst(0f)
                    Type.BYTE -> iconst(0)
                    Type.SHORT -> iconst(0)
                    Type.BOOLEAN -> iconst(0)
                    Type.CHAR -> iconst(0)
                    else -> aconst(null)
                }
            }

            load(0, thisType)
            checkcast(CONTINUATION_TYPE)
            invokevirtual(outerType, targetFunctionName, targetFunctionDescriptor, false)
            visitInsn(ARETURN)
            val label1 = Label()
            visitLabel(label1)
            visitLocalVariable("this", thisType.descriptor, null, label0, label1, 0)
            visitLocalVariable("\$result", OBJECT_TYPE.descriptor, null, label0, label1, 1)
            visitMaxs(0, 0)
            visitEnd()
        }

        classWriter.visitEnd()
        return classWriter.toByteArray()
    }
}
