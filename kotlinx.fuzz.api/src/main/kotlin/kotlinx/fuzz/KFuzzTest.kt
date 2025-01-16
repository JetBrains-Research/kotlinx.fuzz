package kotlinx.fuzz

import org.junit.platform.commons.annotation.Testable

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Testable
annotation class KFuzzTest {
}