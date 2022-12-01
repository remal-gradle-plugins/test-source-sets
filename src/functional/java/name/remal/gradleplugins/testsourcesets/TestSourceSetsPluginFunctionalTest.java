package name.remal.gradleplugins.testsourcesets;

import static name.remal.gradleplugins.testsourcesets.TestSourceSetsPlugin.ALL_TESTS_TASK_NAME;
import static name.remal.gradleplugins.toolkit.StringUtils.escapeGroovy;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradleplugins.toolkit.testkit.MinSupportedGradleVersion;
import name.remal.gradleplugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class TestSourceSetsPluginFunctionalTest {

    private final GradleProject project;

    @BeforeEach
    void beforeEach() {
        project.forBuildFile(build -> {
            build.applyPlugin("name.remal.test-source-sets");
            build.append("repositories { mavenCentral() }");
            build.registerDefaultTask(ALL_TESTS_TASK_NAME);
            build.append(
                "testSourceSets { additional }"
            );
        });
    }

    @Test
    @DisplayName("empty build performs successfully")
    void emptyBuildPerformsSuccessfully() {
        project.assertBuildSuccessfully();
    }

    @Test
    @DisplayName("empty build with Jacoco performs successfully")
    void buildWithJacocoPerformsSuccessfully() {
        project.forBuildFile(build -> {
            build.applyPlugin("jacoco");
            build.registerDefaultTask("jacocoTestAdditionalReport");
        });
        project.assertBuildSuccessfully();
    }

    @Test
    @DisplayName("Kotlin build with internal visibility performs successfully")
    @MinSupportedGradleVersion("6.1")
    void kotlinBuildWithInternalVisibilityPerformsSuccessfully() {
        project.forBuildFile(build -> {
            val kotlinVersion = getCorrespondingKotlinVersion();
            build.applyPlugin("org.jetbrains.kotlin.jvm", kotlinVersion);

            build.append(
                "repositories { mavenCentral() }",
                "dependencies { api 'org.jetbrains.kotlin:kotlin-stdlib:" + escapeGroovy(kotlinVersion) + "' }",
                "file(\"src/main/kotlin/pkg/kotlinFile.kt\").with {",
                "    parentFile.mkdirs()",
                "    write('internal fun internalFunction(): Unit = TODO()', 'UTF-8')",
                "}",
                "file(\"src/additional/kotlin/pkg/kotlinFileTest.kt\").with {",
                "    parentFile.mkdirs()",
                "    write('fun internalFunctionTest() = internalFunction()', 'UTF-8')",
                "}"
            );
        });

        project.withoutConfigurationCache();

        project.assertBuildSuccessfully();
    }


    private static final String CORRESPONDING_KOTLIN_VERSION_PROPERTY = "corresponding-kotlin.version";

    private static String getCorrespondingKotlinVersion() {
        return Optional.ofNullable(System.getProperty(CORRESPONDING_KOTLIN_VERSION_PROPERTY))
            .filter(it -> !it.isEmpty())
            .orElseThrow(() -> new AssertionError(
                CORRESPONDING_KOTLIN_VERSION_PROPERTY + " system property is not set or empty"
            ));
    }

}
