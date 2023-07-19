package name.remal.gradle_plugins.test_source_sets;

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
            build.append(
                "Dependency junitDependency = dependencies.create('junit:junit:4.13.2')",
                "project.configurations.with { configurations ->",
                "    testSourceSets.all { SourceSet sourceSet ->",
                "        configurations[sourceSet.implementationConfigurationName]",
                "            .dependencies",
                "            .add(junitDependency)",
                "    }",
                "}",
                "",
                "tasks.withType(Test).configureEach {",
                "    useJUnit()",
                "    enableAssertions = true",
                "    testLogging {",
                "        showExceptions = true",
                "        showCauses = true",
                "        showStackTraces = true",
                "        exceptionFormat = 'FULL'",
                "        stackTraceFilters('GROOVY')",
                "    }",
                "}",
                ""
            );

            build.append(
                "file(\"src/test/java/pkg/JavaTest.java\").with {",
                "    parentFile.mkdirs()",
                "    write([",
                "        'package pkg;',",
                "        'import org.junit.Test;',",
                "        'public class JavaTest {',",
                "        '    @Test',",
                "        '    public void test() {}',",
                "        '}',",
                "    ].join('\\n'), 'UTF-8')",
                "}",
                ""
            );

            build.append(
                "file(\"src/additionalTest/java/pkg/JavaAdditionalTest.java\").with {",
                "    parentFile.mkdirs()",
                "    write([",
                "        'package pkg;',",
                "        'import org.junit.Test;',",
                "        'public class JavaAdditionalTest {',",
                "        '    @Test',",
                "        '    public void test() {}',",
                "        '}',",
                "    ].join('\\n'), 'UTF-8')",
                "}",
                ""
            );
        });
    }

    @Test
    void emptyBuildPerformsSuccessfully() {
        project.getBuildFile().append(
            "file(\"src/test/java\").deleteDir()",
            "file(\"src/additionalTest/java\").deleteDir()",
            ""
        );

        project.assertBuildSuccessfully();
    }

    @Test
    void nonEmptyBuildPerformsSuccessfully() {
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
                "    write([",
                "        'package pkg',",
                "        'internal fun internalFunction(): Unit {}',",
                "    ].join('\\n'), 'UTF-8')",
                "}",
                "",
                "file(\"src/additionalTest/kotlin/pkg/KotlinAdditionalTest.kt\").with {",
                "    parentFile.mkdirs()",
                "    write([",
                "        'package pkg',",
                "        'import org.junit.Test',",
                "        'class KotlinAdditionalTest {',",
                "        '    @Test',",
                "        '    fun test() {',",
                "        '        internalFunction()',",
                "        '    }',",
                "        '}',",
                "    ].join('\\n'), 'UTF-8')",
                "}",
                ""
            );
        });

        project.withoutConfigurationCache();

        project.assertBuildSuccessfully();
    }

}
