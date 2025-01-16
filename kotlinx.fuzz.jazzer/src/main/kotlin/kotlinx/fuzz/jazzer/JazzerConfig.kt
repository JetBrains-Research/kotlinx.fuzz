package kotlinx.fuzz.jazzer

data class JazzerConfig(
    val libFuzzerRssLimit: Int,  // TODO: Other settings
) {
    companion object {
        fun fromSystemProperties(): JazzerConfig = JazzerConfig(
            libFuzzerRssLimit = System.getProperty("kotlinx.fuzz.jazzer.libFuzzerArgs.rssLimitMb", "0").toInt(),
        )
    }
}
