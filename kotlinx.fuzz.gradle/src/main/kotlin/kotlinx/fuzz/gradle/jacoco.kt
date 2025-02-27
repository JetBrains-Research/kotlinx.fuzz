package kotlinx.fuzz.gradle

import java.io.File
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import kotlinx.fuzz.config.CoverageReportType
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.tools.ExecFileLoader
import org.jacoco.report.DirectorySourceFileLocator
import org.jacoco.report.FileMultiReportOutput
import org.jacoco.report.IReportVisitor
import org.jacoco.report.MultiReportVisitor
import org.jacoco.report.csv.CSVFormatter
import org.jacoco.report.html.HTMLFormatter
import org.jacoco.report.xml.XMLFormatter

private fun CoverageReportType.toVisitor(reportDir: Path): IReportVisitor = when (this) {
    CoverageReportType.HTML -> HTMLFormatter().createVisitor(FileMultiReportOutput(reportDir.toFile()))
    CoverageReportType.XML ->
        XMLFormatter().createVisitor(reportDir.resolve("jacoco.xml").outputStream().buffered())

    CoverageReportType.CSV ->
        CSVFormatter().createVisitor(reportDir.resolve("jacoco.csv").outputStream().buffered())
}

/**
 * Merges all JaCoCo .exec files in [execDir] into a single .exec result file at [result].
 *
 * @param execDir
 * @param result
 */
internal fun jacocoMerge(execDir: Path, result: Path) {
    // Use a single loader for merging. Each subsequent load() merges additional .exec data.
    val mergedLoader = ExecFileLoader()
    execDir.listDirectoryEntries("*.exec").forEach { execFile ->
        execFile.inputStream().buffered().use { mergedLoader.load(it) }
    }

    // Save merged execution data to the result .exec file
    result.outputStream().buffered().use { mergedLoader.save(it) }
}

/**
 * Generates an HTML coverage report (and optional XML) for [execFile].
 *
 * @param execFile Path to the .exec file
 * @param classPath Directory containing compiled .class files
 * @param sourceDirectories Directory containing source files (for line coverage information)
 * @param reportDir Output directory where the coverage report will be generated
 * @param reports Jacoco reports to generate (xml, html, csv)
 */
internal fun jacocoReport(
    execFile: Path,
    classPath: Set<File>,
    sourceDirectories: Set<File>,
    reportDir: Path,
    reports: Set<CoverageReportType>,
) {
    val execLoader = ExecFileLoader()
    execFile.inputStream().buffered().use { execLoader.load(it) }

    val coverageBuilder = CoverageBuilder()
    val analyzer = Analyzer(execLoader.executionDataStore, coverageBuilder)
    classPath.filter { it.exists() }.forEach { classFile ->
        analyzer.analyzeAll(classFile)
    }

    val visitors = reports.map { it.toVisitor(reportDir) }
    val reportVisitor = MultiReportVisitor(visitors)

    reportVisitor.visitInfo(
        execLoader.sessionInfoStore.infos,
        execLoader.executionDataStore.contents,
    )

    val bundle = coverageBuilder.getBundle("Coverage Report")
    sourceDirectories.forEach { directory ->
        reportVisitor.visitBundle(
            bundle,
            DirectorySourceFileLocator(directory, "UTF-8", 4),
        )
    }
    reportVisitor.visitEnd()
}
