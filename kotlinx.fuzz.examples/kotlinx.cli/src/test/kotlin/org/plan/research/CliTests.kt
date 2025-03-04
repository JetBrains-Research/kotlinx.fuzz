@file:Suppress("unused")

package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.cli.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

object CliTests {

    @FuzzTest(maxDuration = TEST_DURATION)
    fun `fuzz arguments and options`(data: FuzzedDataProvider) {
        val parser = ArgParser(data.consumeAsciiString(10)).apply {
            disableExitProcess()
            setup(data)
        }
        val args = Array(data.consumeInt(0, 20)) { data.consumeAsciiString(20) }
        parser.parseAndIgnoreCommonExceptions(args)
    }

    private fun ArgParser.parseAndIgnoreCommonExceptions(args: Array<String>) {
        try {
            // calling parse repeatedly is not allowed (throws)
            parse(args)
        } catch (e: IllegalStateException) {
            // happens if option/argument name is duplicated
        } catch (e: ExitProcessException) {
            // cannot use even regexes to validate (because options can contain \r or \n or whatever)
            // so forced to simply ignore
        }
    }

    // returns ArgType and a default value for it
    fun pickArgTypeAndValue(data: FuzzedDataProvider): Pair<ArgType<Any>, Any> {
        val argTypeFactories = listOf(
            { ArgType.Int to data.consumeInt() },
            { ArgType.Double to data.consumeDouble() },
            { ArgType.Boolean to data.consumeBoolean() },
            { ArgType.String to data.consumeAsciiString(10) },
            {
                val choices = List(data.consumeInt(1, 5)) { data.consumeAsciiString(10) }.distinct()
                ArgType.Choice(choices, { it }, { it }) to data.pickValue(choices)
            }
        )
        @Suppress("UNCHECKED_CAST")
        return data.pickValue(argTypeFactories).invoke() as Pair<ArgType<Any>, Any>
    }

    private fun ArgParser.addOption(data: FuzzedDataProvider) {
        val (argType, defaultValue) = pickArgTypeAndValue(data)
        val opt = option(
            type = argType,
            fullName = data.consumeAsciiString(10),
            shortName = data.consumeAsciiString(1),
            description = data.consumeAsciiString(10)
        )
        val isRequired = data.consumeBoolean()
        val isMultiple = data.consumeBoolean()
        val hasDefault = data.consumeBoolean()
        when {
            isMultiple && hasDefault -> opt.default(defaultValue).multiple() // default + required = ?!
            isMultiple && isRequired -> opt.multiple().required()
            isMultiple -> opt.multiple()
            isRequired -> opt.required()
            hasDefault -> opt.default(defaultValue)
            else -> {}
        }
    }

    private fun ArgParser.addArgument(data: FuzzedDataProvider) {
        val (argType, _) = pickArgTypeAndValue(data)
        argument(
            type = argType,
            fullName = data.consumeAsciiString(10),
            description = data.consumeAsciiString(10),
        ).apply {
            if (data.consumeBoolean()) optional()
        }
    }

    private fun ArgParser.setup(data: FuzzedDataProvider) {
        val setupChoices = listOf(
            { addOption(data) },
            { addArgument(data) },
        )
        repeat(data.consumeInt(0, 10)) {
            data.pickValue(setupChoices).invoke()
        }
        useDefaultHelpShortName = data.consumeBoolean()
        skipExtraArguments = data.consumeBoolean()
        strictSubcommandOptionsOrder = data.consumeBoolean()
        prefixStyle = data.pickValue(ArgParser.OptionPrefixStyle.entries)
    }

    @OptIn(ExperimentalCli::class)
    @FuzzTest(maxDuration = TEST_DURATION)
    fun `fuzz with subcommands`(data: FuzzedDataProvider) {
        val parser = ArgParser(data.consumeAsciiString(10)).apply {
            disableExitProcess()
            setup(data)
        }

        val subcommand1 = FuzzCommand(data)
        val subcommand2 = FuzzCommand2(data)
        val allSubcommands = if (data.consumeBoolean()) listOf(subcommand1, subcommand2) else listOf(subcommand1)
        parser.subcommands(*allSubcommands.toTypedArray())

        val args = Array(data.consumeInt(0, 30)) { data.consumeAsciiString(20) }
        parser.parseAndIgnoreCommonExceptions(args)
    }

    @OptIn(ExperimentalCli::class)
    class FuzzCommand(data: FuzzedDataProvider) : Subcommand(
        data.consumeAsciiString(10),
        data.consumeAsciiString(20)
    ) {
        init {
            disableExitProcess()
            setup(data)
        }

        @ExperimentalCli
        override fun execute() {
        } // do nothing
    }

    // apparently, it is not possible to clone a class (at least not with Reflection)
    @OptIn(ExperimentalCli::class)
    class FuzzCommand2(data: FuzzedDataProvider) : Subcommand(
        data.consumeAsciiString(10),
        data.consumeAsciiString(20)
    ) {
        init {
            disableExitProcess()
            setup(data)
        }

        @ExperimentalCli
        override fun execute() {
        } // do nothing
    }
}

class ExitProcessException(override val message: String, val exitCode: Int) : RuntimeException(message)
typealias ExitHandler = (String, Int) -> Nothing

// if parse() fails, it calls exitProcess() :|
fun ArgParser.disableExitProcess() {
    // can't avoid it with System.setSecurityManager() because it is terminally deprecated
    // decided to use Reflection to change `internal var outputAndTerminate`
    val newExitHandler: ExitHandler = { message, exitCode ->
        throw ExitProcessException(message, exitCode)
    }
    val handlerProperty = this::class.memberProperties.find { it.name == "outputAndTerminate" }
    @Suppress("UNCHECKED_CAST")
    (handlerProperty as KMutableProperty1<ArgParser, ExitHandler>).set(this, newExitHandler)
}
