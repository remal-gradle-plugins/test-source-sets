package name.remal.gradle_plugins.test_source_sets;

import static java.lang.String.join;
import static name.remal.gradle_plugins.test_source_sets.TestSourceSetsPlugin.ALL_TESTS_TASK_NAME;
import static name.remal.gradle_plugins.toolkit.testkit.GradleDependencyVersions.getCorrespondingKotlinVersion;

import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.toolkit.testkit.MinSupportedGradleVersion;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class TestSourceSetsPluginFunctionalTest {

    final GradleProject project;

    @BeforeEach
    void beforeEach() {
        project.forBuildFile(build -> {
            build.applyPlugin("name.remal.test-source-sets");
            build.line("testSourceSets { additionalTest }");

            build.line("repositories { mavenCentral() }");
            build.line(join("\n", new String[]{
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
                "    try {",
                "        useJUnit()",
                "    } catch (IllegalStateException ignored) {",
                "        // do nothing",
                "    }",
                "    enableAssertions = true",
                "    testLogging {",
                "        showExceptions = true",
                "        showCauses = true",
                "        showStackTraces = true",
                "        exceptionFormat = 'FULL'",
                "        stackTraceFilters('GROOVY')",
                "    }",
                "}",
                "",
            }));

            build.line(join("\n", new String[]{
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
            }));

            build.line(join("\n", new String[]{
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
            }));
        });
    }

    @Test
    void emptyBuildPerformsSuccessfully() {
        project.getBuildFile().line(join("\n", new String[]{
            "file(\"src/test/java\").deleteDir()",
            "file(\"src/additionalTest/java\").deleteDir()",
            ""
        }));

        project.assertBuildSuccessfully(ALL_TESTS_TASK_NAME);
    }

    @Test
    void nonEmptyBuildPerformsSuccessfully() {
        project.assertBuildSuccessfully(ALL_TESTS_TASK_NAME);
    }

    @Test
    void buildWithJacocoPerformsSuccessfully() {
        project.forBuildFile(build -> {
            build.applyPlugin("jacoco");
        });
        project.assertBuildSuccessfully("jacocoAdditionalTestReport");
    }

    @Test
    @MinSupportedGradleVersion("6.1")
    void kotlinBuildWithInternalVisibilityPerformsSuccessfully() {
        project.forBuildFile(build -> {
            val kotlinVersion = getCorrespondingKotlinVersion();
            build.applyPlugin("org.jetbrains.kotlin.jvm", kotlinVersion);

            build.line(join("\n", new String[]{
                "dependencies { api 'org.jetbrains.kotlin:kotlin-stdlib:" + build.escapeString(kotlinVersion) + "' }",
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
            }));
        });

        project.withoutConfigurationCache();

        project.assertBuildSuccessfully(ALL_TESTS_TASK_NAME);
    }

}
