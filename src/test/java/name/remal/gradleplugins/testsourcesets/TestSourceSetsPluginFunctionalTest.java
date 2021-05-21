package name.remal.gradleplugins.testsourcesets;

import static name.remal.gradleplugins.testsourcesets.TestSourceSetsPlugin.ALL_TESTS_TASK_NAME;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestSourceSetsPluginFunctionalTest extends BaseFunctionalTestExtension {

    {
        applyPlugin("name.remal.test-source-sets");
        appendToBuildFile(
            "testSourceSets { additional }",
            "defaultTasks('" + ALL_TESTS_TASK_NAME + "')"
        );
    }

    @Test
    @DisplayName("empty build performs successfully")
    void emptyBuildPerformsSuccessfully() {
        assertBuildSuccessfully();
    }

    @Test
    @DisplayName("Kotlin build with internal visibility performs successfully")
    void kotlinBuildWithInternalVisibilityPerformsSuccessfully() {
        applyPlugin("org.jetbrains.kotlin.jvm", getCorrespondingKotlinVersion());
        appendToBuildFile(
            "repositories { mavenCentral() }",
            "dependencies { api 'org.jetbrains.kotlin:kotlin-stdlib:" + getCorrespondingKotlinVersion() + "' }",
            "file(\"src/main/kotlin/pkg/kotlinFile.kt\").with {",
            "    parentFile.mkdirs()",
            "    write('internal fun internalFunction(): Unit = TODO()', 'UTF-8')",
            "}",
            "file(\"src/additional/kotlin/pkg/kotlinFileTest.kt\").with {",
            "    parentFile.mkdirs()",
            "    write('fun internalFunctionTest() = internalFunction()', 'UTF-8')",
            "}"
        );
        assertBuildSuccessfully();
    }

}
