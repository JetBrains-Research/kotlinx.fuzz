package kotlinx.fuzz.jazzer

import java.nio.file.Path
import org.jacoco.agent.AgentJar

val jacocoAgentJar: Path by lazy {
    AgentJar.extractToTempLocation().toPath()
}
