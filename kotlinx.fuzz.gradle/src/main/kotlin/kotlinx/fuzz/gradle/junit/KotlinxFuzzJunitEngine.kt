package kotlinx.fuzz.gradle.junit

import java.lang.reflect.Method
import java.net.URI
import kotlin.reflect.KClass
import kotlinx.fuzz.IgnoreFailures
import kotlinx.fuzz.KFuzzEngine
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.config.JazzerConfig
import kotlinx.fuzz.config.KFuzzConfig
import kotlinx.fuzz.log.LoggerFacade
import kotlinx.fuzz.log.debug
import kotlinx.fuzz.log.info
import kotlinx.fuzz.regression.RegressionEngine
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.HierarchyTraversalMode
import org.junit.platform.commons.support.ReflectionSupport
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.ClasspathRootSelector
import org.junit.platform.engine.discovery.MethodSelector
import org.junit.platform.engine.discovery.PackageSelector
import org.junit.platform.engine.support.descriptor.EngineDescriptor

internal class KotlinxFuzzJunitEngine : TestEngine {
    private val log = LoggerFacade.getLogger<KotlinxFuzzJunitEngine>()

    // KotlinxFuzzJunitEngine can be instantiated at an arbitrary point of time by JunitPlatform
    // To prevent failures due to lack of necessary properties, config is read lazily
    private val config: KFuzzConfig by lazy {
        KFuzzConfig.fromSystemProperties().build()
    }
    private val fuzzEngine: KFuzzEngine by lazy {
        when (config.engine) {
            is JazzerConfig -> Class.forName("kotlinx.fuzz.jazzer.JazzerEngine")
                .getConstructor(KFuzzConfig::class.java).newInstance(config) as KFuzzEngine
        }
    }
    private val isRegression: Boolean by lazy { config.global.regressionEnabled }

    override fun getId(): String = "kotlinx.fuzz"

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId,
    ): TestDescriptor {
        val engineDescriptor = EngineDescriptor(uniqueId, "kotlinx.fuzz")
        discoveryRequest.getSelectorsByType(ClasspathRootSelector::class.java).forEach { selector ->
            appendTestsInClasspathRoot(selector.classpathRoot, engineDescriptor)
        }

        discoveryRequest.getSelectorsByType(PackageSelector::class.java).forEach { selector ->
            appendTestsInPackage(selector!!.packageName, engineDescriptor)
        }

        discoveryRequest.getSelectorsByType(ClassSelector::class.java).forEach { selector ->
            appendTestsInClass(selector!!.javaClass, engineDescriptor)
        }

        discoveryRequest.getSelectorsByType(MethodSelector::class.java).forEach { methodSelector ->
            appendTestsInMethod(methodSelector.javaMethod!!, engineDescriptor)
        }

        return engineDescriptor
    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        fuzzEngine.initialise()
        root.children.forEach { child -> executeImpl(request, child) }
        fuzzEngine.finishExecution()
    }

    private fun handleContainer(request: ExecutionRequest, descriptor: TestDescriptor) {
        request.engineExecutionListener.executionStarted(descriptor)
        descriptor.children.forEach { child -> executeImpl(request, child) }
        request.engineExecutionListener.executionFinished(
            descriptor,
            TestExecutionResult.successful(),
        )
    }

    private fun handleFinding(finding: Throwable?, method: Method) = when {
        finding == null -> TestExecutionResult.successful()
        method.isAnnotationPresent(IgnoreFailures::class.java) -> {
            log.info { "Test failed, but is ignored by @IgnoreFailures: $finding" }
            TestExecutionResult.successful()
        }

        else -> TestExecutionResult.failed(finding)
    }

    private fun executeImpl(request: ExecutionRequest, descriptor: TestDescriptor) {
        when (descriptor) {
            is ClassTestDescriptor -> handleContainer(request, descriptor)
            is MethodRegressionTestDescriptor -> handleContainer(request, descriptor)
            is MethodFuzzTestDescriptor -> {
                log.debug { "Executing method ${descriptor.displayName}" }
                request.engineExecutionListener.executionStarted(descriptor)
                val method = descriptor.testMethod
                val instance = method.declaringClass.kotlin.testInstance()

                val finding = fuzzEngine.runTarget(instance, method)
                val result = handleFinding(finding, method)
                request.engineExecutionListener.executionFinished(descriptor, result)
            }

            is CrashTestDescriptor -> {
                log.debug { "Executing crash ${descriptor.displayName}" }
                request.engineExecutionListener.executionStarted(descriptor)
                val method = descriptor.testMethod
                val instance = method.declaringClass.kotlin.testInstance()

                val finding = RegressionEngine.runOneCrash(instance, method, descriptor.crashFile)
                val result = handleFinding(finding, method)
                request.engineExecutionListener.executionFinished(descriptor, result)
            }
        }
    }

    private fun appendTestsInMethod(method: Method, engineDescriptor: EngineDescriptor) {
        if (!method.isFuzzTarget()) {
            return
        }

        engineDescriptor.addChild(
            when {
                isRegression -> MethodRegressionTestDescriptor(method, engineDescriptor, config)
                else -> MethodFuzzTestDescriptor(method, engineDescriptor)
            },
        )
    }

    private fun appendTestsInClasspathRoot(uri: URI, engineDescriptor: EngineDescriptor) {
        ReflectionSupport.findAllClassesInClasspathRoot(uri, isKFuzzTestContainer) { true }
            .map { klass -> ClassTestDescriptor(klass, engineDescriptor, config, isRegression) }
            .forEach { testDescriptor -> engineDescriptor.addChild(testDescriptor) }
    }

    private fun appendTestsInPackage(packageName: String, engineDescriptor: TestDescriptor) {
        ReflectionSupport.findAllClassesInPackage(packageName, isKFuzzTestContainer) { true }
            .map { aClass -> ClassTestDescriptor(aClass!!, engineDescriptor, config, isRegression) }
            .forEach { descriptor -> engineDescriptor.addChild(descriptor) }
    }

    private fun appendTestsInClass(javaClass: Class<*>, engineDescriptor: TestDescriptor) {
        engineDescriptor.addChild(ClassTestDescriptor(javaClass, engineDescriptor, config, isRegression))
    }

    companion object {
        val isKFuzzTestContainer: (Class<*>) -> Boolean = { klass ->
            ReflectionSupport.findMethods(
                klass,
                { method: Method -> method.isFuzzTarget() },
                HierarchyTraversalMode.TOP_DOWN,
            ).isNotEmpty()
        }

        private fun Method.isFuzzTarget(): Boolean =
            AnnotationSupport.isAnnotated(this, KFuzzTest::class.java) &&
                parameters.size == 1 &&
                parameters[0].type == KFuzzer::class.java

        private fun KClass<*>.testInstance(): Any = objectInstance ?: java.getDeclaredConstructor().newInstance()
    }
}
