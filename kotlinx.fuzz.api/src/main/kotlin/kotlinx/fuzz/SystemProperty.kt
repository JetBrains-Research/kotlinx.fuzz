package kotlinx.fuzz

enum class SystemProperty(name: String) {
    CUSTOM_HOOK_EXCLUDES("kotlinx.fuzz.customHookExcludes"),
    DUMP_COVERAGE("kotlinx.fuzz.dumpCoverage"),
    ENGINE("kotlinx.fuzz.engine"),
    HOOKS("kotlinx.fuzz.hooks"),
    INSTRUMENT("kotlinx.fuzz.instrument"),
    INTELLIJ_DEBUGGER_DISPATCH_PORT("idea.debugger.dispatch.port"),
    JAZZER_ENABLE_LOGGING("kotlinx.fuzz.jazzer.enableLogging"),
    JAZZER_LIBFUZZERARGS_RSS_LIMIT_MB("kotlinx.fuzz.jazzer.libFuzzerArgs.rssLimitMb"),
    KEEP_GOING("kotlinx.fuzz.keepGoing"),
    LOG_LEVEL("kotlinx.fuzz.log.level"),
    MAX_SINGLE_TARGET_FUZZ_TIME("kotlinx.fuzz.maxSingleTargetFuzzTime"),
    REGRESSION("kotlinx.fuzz.regression"),
    REPRODUCER_PATH("kotlinx.fuzz.reproducerPath"),
    THREADS("kotlinx.fuzz.threads"),
    WORK_DIR("kotlinx.fuzz.workDir"),
    ;

    fun get(): String? = System.getProperty(name)

    fun get(default: String): String = System.getProperty(name, default)
}
