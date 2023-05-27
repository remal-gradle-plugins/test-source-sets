package name.remal.gradle_plugins.test_source_sets;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.findExtension;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;
import static name.remal.gradle_plugins.toolkit.IdeaModuleUtils.getTestResourceDirs;
import static name.remal.gradle_plugins.toolkit.IdeaModuleUtils.getTestSourceDirs;
import static name.remal.gradle_plugins.toolkit.testkit.ProjectAfterEvaluateActionsExecutor.executeAfterEvaluateActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.toolkit.SourceSetUtils;
import name.remal.gradle_plugins.toolkit.testkit.ApplyPlugin;
import name.remal.gradle_plugins.toolkit.testkit.MinSupportedGradleVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class TestSourceSetsPluginTest {

    @ApplyPlugin(type = TestSourceSetsPlugin.class)
    private final Project project;


    @Test
    void appliesJavaPlugin() {
        assertTrue(project.getPluginManager().hasPlugin("java"));
    }

    @Test
    void extensionOfTypeTestSourceSetContainerIsCreated() {
        assertNotNull(getExtension(project, TestSourceSetContainer.class));
    }

    @Test
    void testSourceSets_extension_created() {
        assertNotNull(project.getExtensions().getByName("testSourceSets"));
    }

    @Test
    void contains_test_source_set() {
        val sourceSets = getExtension(project, SourceSetContainer.class);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        assertTrue(testSourceSets.contains(testSourceSet));
    }

    @Test
    void creatingInTestSourceSetsCreatesInSourceSets() {
        val sourceSets = getExtension(project, SourceSetContainer.class);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        val anotherTestSourceSet = testSourceSets.create("another");
        assertTrue(sourceSets.contains(anotherTestSourceSet));
        assertTrue(testSourceSets.contains(anotherTestSourceSet));
    }

    @Test
    void removalFromSourceSetsRemovesFromTestSourceSets() {
        val sourceSets = getExtension(project, SourceSetContainer.class);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        sourceSets.remove(testSourceSet);
        assertFalse(testSourceSets.contains(testSourceSet));
    }

    @Test
    void removalFromTestSourceSetsRemovesFromSourceSets() {
        val sourceSets = getExtension(project, SourceSetContainer.class);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        testSourceSets.remove(testSourceSet);
        assertFalse(testSourceSets.contains(testSourceSet));
    }

    @Test
    void compileClasspathConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        val configurations = project.getConfigurations();
        val sourceSets = getExtension(project, SourceSetContainer.class);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        val integrationSourceSet = testSourceSets.create("integration");
        assertTrue(
            configurations.getByName(integrationSourceSet.getCompileClasspathConfigurationName())
                .getExtendsFrom()
                .contains(configurations.getByName(testSourceSet.getCompileClasspathConfigurationName())),
            testSourceSet.getCompileClasspathConfigurationName()
        );
    }

    @Test
    void runtimeClasspathConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        val configurations = project.getConfigurations();
        val sourceSets = getExtension(project, SourceSetContainer.class);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        val integrationSourceSet = testSourceSets.create("integration");
        assertTrue(
            configurations.getByName(integrationSourceSet.getRuntimeClasspathConfigurationName())
                .getExtendsFrom()
                .contains(configurations.getByName(testSourceSet.getRuntimeClasspathConfigurationName())),
            testSourceSet.getRuntimeClasspathConfigurationName()
        );
    }

    @Test
    void implementationConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        val configurations = project.getConfigurations();
        val sourceSets = getExtension(project, SourceSetContainer.class);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        val integrationSourceSet = testSourceSets.create("integration");
        assertTrue(
            configurations.getByName(integrationSourceSet.getImplementationConfigurationName())
                .getExtendsFrom()
                .contains(configurations.getByName(testSourceSet.getImplementationConfigurationName())),
            testSourceSet.getImplementationConfigurationName()
        );
    }

    @Test
    void compileOnlyConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        val configurations = project.getConfigurations();
        val sourceSets = getExtension(project, SourceSetContainer.class);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        val integrationSourceSet = testSourceSets.create("integration");
        assertTrue(
            configurations.getByName(integrationSourceSet.getCompileOnlyConfigurationName())
                .getExtendsFrom()
                .contains(configurations.getByName(testSourceSet.getCompileOnlyConfigurationName())),
            testSourceSet.getCompileOnlyConfigurationName()
        );
    }

    @Test
    void runtimeOnlyConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        val configurations = project.getConfigurations();
        val sourceSets = getExtension(project, SourceSetContainer.class);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        val integrationSourceSet = testSourceSets.create("integration");
        assertTrue(
            configurations.getByName(integrationSourceSet.getRuntimeOnlyConfigurationName())
                .getExtendsFrom()
                .contains(configurations.getByName(testSourceSet.getRuntimeOnlyConfigurationName())),
            testSourceSet.getRuntimeOnlyConfigurationName()
        );
    }


    @Test
    void testTaskNameExtensionIsAdded() {
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        val testSourceSet = testSourceSets.getByName(TEST_SOURCE_SET_NAME);
        assertNotNull(findExtension(testSourceSet, TestTaskNameExtension.class));

        val integrationSourceSet = testSourceSets.create("integration");
        assertNotNull(findExtension(integrationSourceSet, TestTaskNameExtension.class));
    }


    @Nested
    class CreatedTestTasks {

        private final SourceSet integrationSourceSet;
        private final org.gradle.api.tasks.testing.Test testTask;
        private final org.gradle.api.tasks.testing.Test testIntegrationTask;

        {
            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            integrationSourceSet = testSourceSets.create("integration");

            val testTasks = project.getTasks().withType(org.gradle.api.tasks.testing.Test.class);
            testTask = testTasks.findByName("test");
            testIntegrationTask = testTasks.findByName("testIntegration");
        }

        @Test
        void testTaskIsCreatedForEveryTestSourceSet() {
            assertNotNull(testTask);
            assertNotNull(testIntegrationTask);
        }

        @Nested
        class AllTestsTask {

            private final Task allTestsTask = project.getTasks().findByName("allTests");

            @Test
            void allTestsTaskIsCreated() {
                assertNotNull(allTestsTask);
            }

            @Test
            void allTestsTaskDependsOnTestTaskOfAllTestSourceSet() {
                assertNotNull(allTestsTask);
                assertNotNull(testTask);
                assertTrue(
                    allTestsTask.getDependsOn().contains(testTask.getName()),
                    testTask.getName()
                );
                assertNotNull(testIntegrationTask);
                assertTrue(
                    allTestsTask.getDependsOn().contains(testIntegrationTask.getName()),
                    testIntegrationTask.getName()
                );
            }

        }

        @Test
        void createdTestTaskHasCorrectTestClassesDirs() {
            assertNotNull(testIntegrationTask);
            assertSame(
                testIntegrationTask.getTestClassesDirs(),
                integrationSourceSet.getOutput().getClassesDirs()
            );
        }

        @Test
        void createdTestTaskHasCorrectClasspath() {
            assertNotNull(testIntegrationTask);
            assertSame(
                testIntegrationTask.getClasspath(),
                integrationSourceSet.getRuntimeClasspath()
            );
        }

        @Test
        @MinSupportedGradleVersion("6.4")
        void createdTestTaskHasCorrectModularitySettings() {
            JavaPluginExtension javaPluginExtension = getExtension(project, JavaPluginExtension.class);
            assertNotNull(testIntegrationTask);

            javaPluginExtension.getModularity().getInferModulePath().set(true);
            assertTrue(testIntegrationTask.getModularity().getInferModulePath().get());

            javaPluginExtension.getModularity().getInferModulePath().set(false);
            assertFalse(testIntegrationTask.getModularity().getInferModulePath().get());
        }


    }


    @Nested
    class IfJacocoPluginIsApplied {

        {
            project.getPluginManager().apply("jacoco");
        }

        @Test
        void correspondingReportTaskIsCreatedForCreatedTestTasks() {
            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("integration");

            assertNotNull(project.getTasks().findByName("jacocoTestIntegrationReport"));
            assertNotNull(project.getTasks().findByName("jacocoTestIntegrationCoverageVerification"));
        }

    }


    @Nested
    class IfKotlinPluginIsApplied {

        {
            project.getPluginManager().apply("kotlin");
        }

        @Test
        @MinSupportedGradleVersion("6.1")
        void allTestSourceSetsAreAssociatedWithMainCompilation() {
            val kotlin = getExtension(project, KotlinSingleTargetExtension.class);
            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("another");

            val compilations = kotlin.getTarget().getCompilations();
            val mainCompilation = compilations.getByName(MAIN_SOURCE_SET_NAME);
            testSourceSets.forEach(testSourceSet -> {
                val compilation = compilations.getByName(testSourceSet.getName());
                assertTrue(compilation.getAssociateWith().contains(mainCompilation), testSourceSet.getName());
            });
        }

    }


    @Nested
    class IfIdeaPluginIsApplied {

        {
            project.getPluginManager().apply("idea");
            executeAfterEvaluateActions(project);
        }

        @Test
        void compileClasspathConfigurationIsAddedToIdeaModuleScopeTest() {
            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            val anotherTestSourceSet = testSourceSets.create("another");

            val compileClasspathConfiguration = project.getConfigurations().getByName(
                anotherTestSourceSet.getCompileClasspathConfigurationName()
            );
            val idea = getExtension(project, IdeaModel.class);
            val module = idea.getModule();
            assertNotNull(module, "idea.module");
            val scopes = module.getScopes();
            assertNotNull(scopes, "idea.module.scopes");
            val testScope = scopes.get("TEST");
            assertNotNull(testScope, "idea.module.scopes.TEST");
            val testPlusScope = testScope.get("plus");
            assertNotNull(testPlusScope, "idea.module.scopes.TEST.plus");
            assertTrue(testPlusScope.contains(compileClasspathConfiguration));
        }

        @Test
        void runtimeClasspathConfigurationIsAddedToIdeaModuleScopeTest() {
            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            val anotherTestSourceSet = testSourceSets.create("another");

            val runtimeClasspathConfiguration = project.getConfigurations().getByName(
                anotherTestSourceSet.getRuntimeClasspathConfigurationName()
            );
            val idea = getExtension(project, IdeaModel.class);
            val module = idea.getModule();
            assertNotNull(module, "idea.module");
            val scopes = module.getScopes();
            assertNotNull(scopes, "idea.module.scopes");
            val testScope = scopes.get("TEST");
            assertNotNull(testScope, "idea.module.scopes.TEST");
            val testPlusScope = testScope.get("plus");
            assertNotNull(testPlusScope, "idea.module.scopes.TEST.plus");
            assertTrue(testPlusScope.contains(runtimeClasspathConfiguration));
        }

        @Test
        void ideaModuleTestSourceDirsContainsDirsOfAllTestSourceSets() {
            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("another");

            val idea = getExtension(project, IdeaModel.class);
            val module = idea.getModule();
            assertNotNull(module, "idea.module");
            val testSourceDirs = getTestSourceDirs(module);
            assertNotNull(testSourceDirs, "idea.module.testSourceDirs");
            for (val testSourceSet : testSourceSets) {
                assertTrue(
                    testSourceDirs.containsAll(testSourceSet.getAllJava().getSrcDirs()),
                    testSourceSet.getName()
                );
            }
        }

        @Test
        void ideaModuleTestResourceDirsContainsDirsOfAllTestSourceSets() {
            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("another");

            val idea = getExtension(project, IdeaModel.class);
            val module = idea.getModule();
            assertNotNull(module, "idea.module");
            val testResourceDirs = getTestResourceDirs(module);
            assertNotNull(testResourceDirs, "idea.module.testResourceDirs");
            for (val testSourceSet : testSourceSets) {
                assertTrue(
                    testResourceDirs.containsAll(testSourceSet.getResources().getSrcDirs()),
                    testSourceSet.getName()
                );
            }
        }

        @Test
        void ideaModuleSingleEntryLibrariesContainsDirsOfMainSourceSet() {
            val idea = getExtension(project, IdeaModel.class);
            val module = idea.getModule();
            assertNotNull(module, "idea.module");
            val singleEntryLibraries = module.getSingleEntryLibraries();
            assertNotNull(singleEntryLibraries, "idea.module.singleEntryLibraries");
            val singleEntryLibrariesRuntime = singleEntryLibraries.get("RUNTIME");
            assertNotNull(singleEntryLibrariesRuntime, "idea.module.singleEntryLibraries.RUNTIME");

            val runtimeDirs = stream(singleEntryLibrariesRuntime.spliterator(), false).collect(toList());
            val sourceSets = getExtension(project, SourceSetContainer.class);
            val mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
            assertTrue(runtimeDirs.containsAll(mainSourceSet.getOutput().getDirs().getFiles()));
        }

        @Test
        void ideaModuleSingleEntryLibrariesContainsDirsOfAllTestSourceSets() {
            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("another");

            val idea = getExtension(project, IdeaModel.class);
            val module = idea.getModule();
            assertNotNull(module, "idea.module");
            val singleEntryLibraries = module.getSingleEntryLibraries();
            assertNotNull(singleEntryLibraries, "idea.module.singleEntryLibraries");
            val singleEntryLibrariesTest = singleEntryLibraries.get("TEST");
            assertNotNull(singleEntryLibrariesTest, "idea.module.singleEntryLibraries.TEST");

            val testDirs = stream(singleEntryLibrariesTest.spliterator(), false).collect(toList());
            for (val testSourceSet : testSourceSets) {
                assertTrue(
                    testDirs.containsAll(testSourceSet.getOutput().getDirs().getFiles()),
                    testSourceSet.getName()
                );
            }
        }

    }


    @Nested
    @MinSupportedGradleVersion("7.5")
    class IfEclipsePluginIsApplied {

        {
            project.getPluginManager().apply("eclipse");
            executeAfterEvaluateActions(project);
        }

        @Test
        void eclipseModuleTestSourceSetsContainsAllTestSourceSets() {
            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("another");

            val eclipse = getExtension(project, EclipseModel.class);
            val classpath = eclipse.getClasspath();
            assertNotNull(classpath, "eclipse.classpath");

            assertThat(classpath.getTestSourceSets().get())
                .containsExactlyInAnyOrderElementsOf(testSourceSets);
        }

        @Test
        void eclipseModuleTestConfigurationsContainsAllConfigurationsOfAllTestSourceSets() {
            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("another");

            val eclipse = getExtension(project, EclipseModel.class);
            val classpath = eclipse.getClasspath();
            assertNotNull(classpath, "eclipse.classpath");

            val allTestSourceSetConfigurations = testSourceSets.stream()
                .map(SourceSetUtils::getSourceSetConfigurationNames)
                .flatMap(Collection::stream)
                .map(project.getConfigurations()::findByName)
                .filter(Objects::nonNull)
                .collect(toList());

            assertThat(classpath.getTestConfigurations().get())
                .containsExactlyInAnyOrderElementsOf(allTestSourceSetConfigurations);
        }

    }

}
