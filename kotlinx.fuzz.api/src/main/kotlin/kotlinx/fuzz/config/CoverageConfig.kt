package kotlinx.fuzz.config

private const val NAME_PREFIX = "coverage"

interface CoverageConfig {
    val reportTypes: Set<CoverageReportType>
    val includeDependencies: Set<String>
}

class CoverageConfigImpl internal constructor(builder: KFuzzConfigBuilder) : CoverageConfig {
    override var reportTypes: Set<CoverageReportType> by builder.KFuzzPropProvider(
        nameSuffix = "$NAME_PREFIX.reportTypes",
        intoString = { it.joinToString(",") },
        fromString = { it.split(",").map { CoverageReportType.valueOf(it) }.toSet() },
        default = setOf(CoverageReportType.HTML),
    )
    override var includeDependencies: Set<String> by builder.KFuzzPropProvider(
        nameSuffix = "$NAME_PREFIX.includeDependencies",
        intoString = { it.joinToString(",") },
        fromString = { it.split(",").toSet() },
        default = emptySet(),
    )
}

enum class CoverageReportType {
    CSV, HTML, XML;
}
