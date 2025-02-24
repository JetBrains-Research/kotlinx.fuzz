package kotlinx.fuzz.config

interface CoverageConfig {
    val reportTypes: Set<CoverageReportType>
    val includeDependencies: Set<String>
}

class CoverageConfigImpl internal constructor(builder: KFuzzConfigBuilder) : CoverageConfig {
    override var reportTypes by builder.KFuzzPropProvider<Set<CoverageReportType>>(
        nameSuffix = "coverage.reportTypes",
        intoString = { it.joinToString(",") },
        fromString = { it.split(",").map { CoverageReportType.valueOf(it) }.toSet() },
        default = setOf(CoverageReportType.HTML),
    )
    override var includeDependencies by builder.KFuzzPropProvider<Set<String>>(
        nameSuffix = "coverage.includeDependencies",
        intoString = { it.joinToString(",") },
        fromString = { it.split(",").toSet() },
        default = emptySet(),
    )
}

enum class CoverageReportType {
    CSV, HTML, XML;
}
