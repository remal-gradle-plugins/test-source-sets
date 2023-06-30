package name.remal.gradle_plugins.test_source_sets;

import static java.lang.String.join;
import static name.remal.gradle_plugins.test_source_sets.TestSourceSetsPlugin.ALL_TESTS_TASK_NAME;
import static name.remal.gradle_plugins.toolkit.StringUtils.escapeGroovy;
import static name.remal.gradle_plugins.toolkit.testkit.GradleDependencyVersions.getCorrespondingKotlinVersion;

import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.toolkit.testkit.MinSupportedGradleVersion;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class TestSourceSetsPluginFunctionalTest {

    private final GradleProject project;

    @BeforeEach
    void beforeEach() {
        project.forBuildFile(build -> {
            build.applyPlugin("name.remal.test-source-sets");
            build.append("testSourceSets { additionalTest }");
            build.registerDefaultTask(ALL_TESTS_TASK_NAME);

            build.append("repositories { mavenCentral() }");
            build.append(join(
                "\n",
                "Dependency junitDependency = dependencies.create('junit:junit:4.13.2')",
                "project.configurations.with { configurations ->",
                "    testSourceSets.all { SourceSet sourceSet ->",
                "        configurations[sourceSet.implementationConfigurationName]",
                "            .dependencies",
                "            .add(junitDependency)",
                "    }",
                "}"
            ));
        });
    }

    @Test
    void emptyBuildPerformsSuccessfully() {
        project.assertBuildSuccessfully();
    }

    @Test
    void buildWithJacocoPerformsSuccessfully() {
        project.forBuildFile(build -> {
            build.applyPlugin("jacoco");
            build.registerDefaultTask("jacocoAdditionalTestReport");
        });
        project.assertBuildSuccessfully();
    }

    @Test
    @MinSupportedGradleVersion("6.1")
    void kotlinBuildWithInternalVisibilityPerformsSuccessfully() {
        project.forBuildFile(build -> {
            val kotlinVersion = getCorrespondingKotlinVersion();
            build.applyPlugin("org.jetbrains.kotlin.jvm", kotlinVersion);

            build.append(
                "dependencies { api 'org.jetbrains.kotlin:kotlin-stdlib:" + escapeGroovy(kotlinVersion) + "' }",
                "file(\"src/main/kotlin/pkg/kotlinFile.kt\").with {",
                "    parentFile.mkdirs()",
                "    write('internal fun internalFunction(): Unit = TODO()', 'UTF-8')",
                "}",
                "file(\"src/additionalTest/kotlin/pkg/kotlinFileTest.kt\").with {",
                "    parentFile.mkdirs()",
                "    write('fun internalFunctionTest() = internalFunction()', 'UTF-8')",
                "}"
            );
        });

        project.withoutConfigurationCache();

        project.assertBuildSuccessfully();
    }

}
