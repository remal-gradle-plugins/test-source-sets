package name.remal.gradle_plugins.test_source_sets;

import static java.lang.String.join;
import static java.util.function.Predicate.not;
import static name.remal.gradle_plugins.test_source_sets.TestSourceSetsPlugin.ALL_TESTS_TASK_NAME;
import static name.remal.gradle_plugins.toolkit.PathUtils.deleteRecursively;
import static name.remal.gradle_plugins.toolkit.testkit.TestClasspath.getTestClasspathLibraryVersion;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
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

            build.block("dependencies", deps -> {
                deps.line(
                    "testImplementation 'org.junit.jupiter:junit-jupiter-api:%s'",
                    getTestClasspathLibraryVersion("org.junit.jupiter:junit-jupiter-api")
                );

                deps.line(
                    "testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:%s'",
                    getTestClasspathLibraryVersion("org.junit.jupiter:junit-jupiter-engine")
                );
                deps.line(
                    "testRuntimeOnly 'org.junit.platform:junit-platform-launcher:%s'",
                    getTestClasspathLibraryVersion("org.junit.platform:junit-platform-launcher")
                );
            });

            build.line(join("\n", new String[]{
                "tasks.withType(Test).configureEach {",
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
                "        'import org.junit.jupiter.api.Test;',",
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
                "        'import org.junit.jupiter.api.Test;',",
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
        deleteRecursively(project.getProjectDir().toPath().resolve("src"));

        project.assertBuildSuccessfully(ALL_TESTS_TASK_NAME);
    }

    @Test
    void nonEmptyBuildPerformsSuccessfully() {
        project.assertBuildSuccessfully(ALL_TESTS_TASK_NAME);
    }

    @Test
    void buildWithJacocoPerformsSuccessfully() {
        project.getBuildFile().applyPlugin("jacoco");

        project.assertBuildSuccessfully("jacocoAdditionalTestReport");
    }

    @Test
    @MinSupportedGradleVersion("6.1")
    void kotlinBuildWithInternalVisibilityPerformsSuccessfully() {
        project.forBuildFile(build -> {
            var kotlinVersion = Optional.ofNullable(System.getProperty("corresponding-kotlin.version"))
                .filter(not(String::isEmpty))
                .orElseThrow();
            build.applyPlugin("org.jetbrains.kotlin.jvm", kotlinVersion);

            build.line(join("\n", new String[]{
                "dependencies { api 'org.jetbrains.kotlin:kotlin-stdlib:" + build.escapeString(kotlinVersion) + "' }",
                "",
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
                "        'import org.junit.jupiter.api.Test',",
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
