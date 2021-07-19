package space.kscience.communicator.prettyapi.compiler

import org.jetbrains.kotlin.ir.builders.IrGeneratorContextInterface
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isClassType
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal object Names {
    val codecFqName = FqName("space.kscience.communicator.api.Codec")

    val communicatorContextFqName = FqName("space.kscience.communicator.prettyapi.CommunicatorContext")
    val communicatorContextRegisterCodecName = Name.identifier("registerCodec")
    val communicatorContextFunctionClientName = Name.identifier("functionClient")
    val communicatorContextFunctionServerName = Name.identifier("functionServer")

    val transportFactoryFqName = FqName("space.kscience.communicator.api.TransportFactory")

    val clientEndpointFqName = FqName("space.kscience.communicator.api.ClientEndpoint")
}


internal fun IrFunction.isRegisterCodec() = name == Names.communicatorContextRegisterCodecName
        && dispatchReceiverParameter?.type?.isClassType(Names.communicatorContextFqName.toUnsafe(), false) == true
        && valueParameters.size == 1
        && valueParameters[0].type.isClassType(Names.codecFqName.toUnsafe(), false)

internal fun IrFunction.isFunctionServer() = name == Names.communicatorContextFunctionServerName
        && dispatchReceiverParameter?.type?.isClassType(Names.communicatorContextFqName.toUnsafe(), false) == true
        && valueParameters.size == 3
        && valueParameters[0].type.isTypeParameter()
        && valueParameters[1].type.isClassType(Names.transportFactoryFqName.toUnsafe(), false)
        && valueParameters[2].type.isClassType(Names.clientEndpointFqName.toUnsafe(), false)

internal fun IrFunction.isFunctionClient() = name == Names.communicatorContextFunctionClientName
        && dispatchReceiverParameter?.type?.isClassType(Names.communicatorContextFqName.toUnsafe(), false) == true
        && valueParameters.size == 2
        && valueParameters[0].type.isClassType(Names.transportFactoryFqName.toUnsafe(), false)
        && valueParameters[1].type.isClassType(Names.clientEndpointFqName.toUnsafe(), false)


fun IrGeneratorContextInterface.constructCodecGetting(type: IrType): IrExpression {
    // code for getting builtin objects
    // code for getting values from storage that are initialized by codec block
    // code for getting values from storage, which construction is synthetic
    TODO()
}
