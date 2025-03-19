package kotlinx.fuzz.reproduction

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.squareup.kotlinpoet.*
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

class ListAnyInlineReproducerWriter(
    private val template: TestReproducerTemplate,
    private val instance: Any,
    private val method: Method,
    files: List<Path>,
) : CrashReproducerWriter(template, method) {
    private val topLevelPrivateFunctions = mutableListOf<KtNamedFunction>()
    private val originalFileImports = mutableListOf<String>()
    private val relevantFunction: KtNamedFunction

    private val PsiElement.flattenedChildren: List<PsiElement>
        get() = buildList {
            children.forEach {
                add(it)
                addAll(it.flattenedChildren)
            }
        }

    init {
        val project = KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable("Disposable for dummy project"),
            CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES,
        ).project
        relevantFunction = files.filter { it.extension == "kt" }
            .mapNotNull { file ->
                val ktFile = PsiFileFactoryImpl(project).createFileFromText(
                    file.name,
                    KotlinLanguage.INSTANCE,
                    file.readText(),
                ) as KtFile
                ktFile.flattenedChildren.filterIsInstance<KtNamedFunction>().find {
                    it.fqName?.asString() == "${method.declaringClass.name}.${method.name}"
                }
            }
            .singleOrNull<KtNamedFunction?>() ?: throw RuntimeException("Couldn't find file with method: ${method.name} in ${files.joinToString(", ") { it.absolutePathString() }}")
        relevantFunction.containingKtFile.let { ktFile ->
            ktFile.children.filterIsInstance<KtNamedFunction>().filter {
                it.isTopLevel && it.hasModifier(KtTokens.PRIVATE_KEYWORD)
            }.forEach { topLevelPrivateFunctions.add(it) }
            ktFile.importDirectives.forEach {
                originalFileImports.add(it.text)
            }
        }
    }

    private fun buildExtensionFunction(hash: String, input: ByteArray): FunSpec {
        val parameterName = relevantFunction.valueParameters[0].name!!
        val body = relevantFunction.bodyExpression!!.text
            .trimIndent()
            .trim('{')
            .trim('}')
            .split("\n")
            .filter { it.isNotBlank() }
        val commonBlankPrefixLength = body.minOf { line -> line.takeWhile { it.isWhitespace() }.length }

        return FunSpec.builder("`${method.name} reproducer $hash`")
            .receiver(method.declaringClass)
            .addModifiers(KModifier.PRIVATE)
            .addCode(
                buildCodeBlock {
                    addStatement(
                        "val $parameterName = ListReproducer(listOf<Any?>(${
                            invokeTestAndRegisterOutputs(instance, method, input).joinToString(", ") { toCodeString(it) }
                        }))")
                    body.forEach { addStatement(it.drop(commonBlankPrefixLength)) }
                })
            .build()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
        val hash = MessageDigest.getInstance("SHA-1").digest(input).toHexString()
        val code = buildCodeBlock {
            addStatement("${method.instanceString}.`${method.name} reproducer $hash`()")
        }

        val extension = buildExtensionFunction(hash, input)

        reproducerFile.writeText(
            template.buildReproducer(
                hash,
                code,
                imports = originalFileImports,
                additionalCode = extension.toString() + "\n" +
                    topLevelPrivateFunctions.map { buildCodeBlock { add(it.text) } }.joinToCode("\n") + "\n" +
                    buildListReproducerObject().toString() + "\n",
            ),
        )
    }
}
