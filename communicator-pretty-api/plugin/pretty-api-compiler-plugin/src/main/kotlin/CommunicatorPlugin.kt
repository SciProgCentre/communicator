package space.kscience.communicator.prettyapi.compiler

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class CommunicatorPluginComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration): Unit =
        registerExtensions(project)

    private companion object {
        private fun registerExtensions(project: Project) {
            IrGenerationExtension.registerExtension(project, CommunicatorExtension)
        }
    }
}

internal object CommunicatorExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val lowering = CommunicatorLowering(pluginContext)
        for (file in moduleFragment.files) lowering.lower(file)
    }
}


internal class CommunicatorLowering(
    val baseContext: IrPluginContext,
) : IrElementTransformerVoid(), FileLoweringPass {
    val reporter = baseContext.createDiagnosticReporter("space.kscience.communicator.prettyapi")

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        reporter.report(
            IrMessageLogger.Severity.WARNING,
            expression.symbol.owner.render(),
            IrMessageLogger.Location("123", 1, 1),
        )

        return super.visitCall(expression)
    }
}
