package name.remal.gradleplugins.testsourcesets;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradleplugins.toolkit.testkit.ApplyPlugin;
import name.remal.gradleplugins.toolkit.testkit.MinSupportedGradleVersion;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class TestSourceSetsPluginTest {

    @ApplyPlugin(type = TestSourceSetsPlugin.class)
    private final Project project;


    @Test
    @DisplayName("applies 'java' plugin")
    void appliesJavaPlugin() {
        assertTrue(project.getPluginManager().hasPlugin("java"));
    }

    @Test
    @DisplayName("extension of type TestSourceSetContainer is created")
    void extensionOfTypeTestSourceSetContainerIsCreated() {
        assertNotNull(project.getExtensions().getByType(TestSourceSetContainer.class));
    }

    @Test
    @DisplayName("'testSourceSets' extension is created")
    void testSourceSets_extension_created() {
        assertNotNull(project.getExtensions().getByName("testSourceSets"));
    }

    @Test
    @DisplayName("contains 'test' source-set")
    void contains_test_source_set() {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        assertTrue(testSourceSets.contains(testSourceSet));
    }

    @Test
    @DisplayName("creating a source-set in 'testSourceSets' creates the source-set in 'sourceSets'")
    void creatingInTestSourceSetsCreatesInSourceSets() {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

        val anotherTestSourceSet = testSourceSets.create("another");
        assertTrue(sourceSets.contains(anotherTestSourceSet));
        assertTrue(testSourceSets.contains(anotherTestSourceSet));
    }

    @Test
    @DisplayName("removal a source-set from 'sourceSets' removes the source-set in 'testSourceSets'")
    void removalFromSourceSetsRemovesFromTestSourceSets() {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        sourceSets.remove(testSourceSet);
        assertFalse(testSourceSets.contains(testSourceSet));
    }

    @Test
    @DisplayName("removal a source-set from 'testSourceSets' removes the source-set in 'sourceSets'")
    void removalFromTestSourceSetsRemovesFromSourceSets() {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        testSourceSets.remove(testSourceSet);
        assertFalse(testSourceSets.contains(testSourceSet));
    }

    @Test
    @DisplayName(
        "'compileClasspath' configurations of test-source-sets extend corresponding configuration of 'test' source-set"
    )
    void compileClasspathConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        val configurations = project.getConfigurations();
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

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
    @DisplayName(
        "'runtimeClasspath' configurations of test-source-sets extend corresponding configuration of 'test' source-set"
    )
    void runtimeClasspathConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        val configurations = project.getConfigurations();
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

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
    @DisplayName(
        "'implementation' configurations of test-source-sets extend corresponding configuration of 'test' source-set"
    )
    void implementationConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        val configurations = project.getConfigurations();
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

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
    @DisplayName(
        "'compileOnly' configurations of test-source-sets extend corresponding configuration of 'test' source-set"
    )
    void compileOnlyConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        val configurations = project.getConfigurations();
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

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
    @DisplayName(
        "'runtimeOnly' configurations of test-source-sets extend corresponding configuration of 'test' source-set"
    )
    void runtimeOnlyConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        val configurations = project.getConfigurations();
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        val integrationSourceSet = testSourceSets.create("integration");
        assertTrue(
            configurations.getByName(integrationSourceSet.getRuntimeOnlyConfigurationName())
                .getExtendsFrom()
                .contains(configurations.getByName(testSourceSet.getRuntimeOnlyConfigurationName())),
            testSourceSet.getRuntimeOnlyConfigurationName()
        );
    }


    @Nested
    @DisplayName("Test tasks")
    class CreatedTestTasks {

        private final SourceSet integrationSourceSet;
        private final org.gradle.api.tasks.testing.Test testTask;
        private final org.gradle.api.tasks.testing.Test testIntegrationTask;

        {
            val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
            integrationSourceSet = testSourceSets.create("integration");

            val testTasks = project.getTasks().withType(org.gradle.api.tasks.testing.Test.class);
            testTask = testTasks.findByName("test");
            testIntegrationTask = testTasks.findByName("integrationTest");
        }

        @Test
        @DisplayName("Test task is created for every test-source-set")
        void testTaskIsCreatedForEveryTestSourceSet() {
            assertNotNull(testTask);
            assertNotNull(testIntegrationTask);
        }

        @Nested
        @DisplayName("allTests task")
        class AllTestsTask {

            private final Task allTestsTask = project.getTasks().findByName("allTests");

            @Test
            @DisplayName("'allTests' task is created")
            void allTestsTaskIsCreated() {
                assertNotNull(allTestsTask);
            }

            @Test
            @DisplayName("'allTests' task depends on Test task of all test-source-set")
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
        @DisplayName("created Test task don't have repeated word 'test' in its name")
        void createdTestTaskDontHaveRepeatedWordTestInItsName() {
            val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
            asList(
                "functionalTest",
                "new-functional-test"
            ).forEach(sourceSetName -> {
                testSourceSets.create(sourceSetName);
                val functionTestTask = project.getTasks()
                    .withType(org.gradle.api.tasks.testing.Test.class).stream()
                    .filter(it -> it.getName().toLowerCase().contains("functional"))
                    .findAny()
                    .orElse(null);
                assertNotNull(functionTestTask, sourceSetName);
                assertEquals(
                    1,
                    StringGroovyMethods.count(functionTestTask.getName().toLowerCase(), "test"),
                    sourceSetName
                );
            });
        }

        @Test
        @DisplayName("created Test task has correct testClassesDirs")
        void createdTestTaskHasCorrectTestClassesDirs() {
            assertNotNull(testIntegrationTask);
            assertSame(
                testIntegrationTask.getTestClassesDirs(),
                integrationSourceSet.getOutput().getClassesDirs()
            );
        }

        @Test
        @DisplayName("created Test task has correct classpath")
        void createdTestTaskHasCorrectClasspath() {
            assertNotNull(testIntegrationTask);
            assertSame(
                testIntegrationTask.getClasspath(),
                integrationSourceSet.getRuntimeClasspath()
            );
        }

        @Test
        @DisplayName("created Test task has correct modularity settings (Gradle >= 6.4)")
        @MinSupportedGradleVersion("6.4")
        void createdTestTaskHasCorrectModularitySettings() {
            JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            assertNotNull(testIntegrationTask);

            javaPluginExtension.getModularity().getInferModulePath().set(true);
            assertTrue(testIntegrationTask.getModularity().getInferModulePath().get());

            javaPluginExtension.getModularity().getInferModulePath().set(false);
            assertFalse(testIntegrationTask.getModularity().getInferModulePath().get());
        }


    }


    @Nested
    @DisplayName("if 'kotlin' plugin is applied")
    class IfKotlinPluginIsApplied {

        {
            project.getPluginManager().apply("kotlin");
        }

        @Test
        @DisplayName("all test-source-sets are associated with 'main' compilation")
        @MinSupportedGradleVersion("6.1")
        void all_test_source_sets_are_associated_with_main_compilation() {
            val kotlin = project.getExtensions().getByType(KotlinSingleTargetExtension.class);
            val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
            val anotherTestSourceSet = testSourceSets.create("another");

            val compilations = kotlin.getTarget().getCompilations();
            val mainCompilation = compilations.getByName(MAIN_SOURCE_SET_NAME);
            testSourceSets.forEach(testSourceSet -> {
                val compilation = compilations.getByName(testSourceSet.getName());
                assertTrue(compilation.getAssociateWith().contains(mainCompilation), testSourceSet.getName());
            });
        }

    }


    @Nested
    @DisplayName("if 'idea' plugin is applied")
    class IfIdeaPluginIsApplied {

        {
            project.getPluginManager().apply("idea");
        }

        @Test
        @DisplayName("'compileClasspath' configuration is added to idea.module.scope.TEST")
        void compileClasspathConfigurationIsAddedToIdeaModuleScopeTest() {
            val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
            val anotherTestSourceSet = testSourceSets.create("another");

            val compileClasspathConfiguration = project.getConfigurations().getByName(
                anotherTestSourceSet.getCompileClasspathConfigurationName()
            );
            val idea = project.getExtensions().getByType(IdeaModel.class);
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
        @DisplayName("'runtimeClasspath' configuration is added to idea.module.scope.TEST")
        void runtimeClasspathConfigurationIsAddedToIdeaModuleScopeTest() {
            val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
            val anotherTestSourceSet = testSourceSets.create("another");

            val runtimeClasspathConfiguration = project.getConfigurations().getByName(
                anotherTestSourceSet.getRuntimeClasspathConfigurationName()
            );
            val idea = project.getExtensions().getByType(IdeaModel.class);
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
        @DisplayName("idea.module.testSourceDirs contains dirs of all testSourceSets")
        void ideaModuleTestSourceDirsContainsDirsOfAllTestSourceSets() {
            val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
            testSourceSets.create("another");

            val idea = project.getExtensions().getByType(IdeaModel.class);
            val module = idea.getModule();
            assertNotNull(module, "idea.module");
            val testSourceDirs = module.getTestSourceDirs();
            assertNotNull(testSourceDirs, "idea.module.testSourceDirs");
            for (val testSourceSet : testSourceSets) {
                assertTrue(
                    testSourceDirs.containsAll(testSourceSet.getAllJava().getSrcDirs()),
                    testSourceSet.getName()
                );
            }
        }

        @Test
        @DisplayName("idea.module.testResourceDirs contains dirs of all testSourceSets")
        void ideaModuleTestResourceDirsContainsDirsOfAllTestSourceSets() {
            val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
            testSourceSets.create("another");

            val idea = project.getExtensions().getByType(IdeaModel.class);
            val module = idea.getModule();
            assertNotNull(module, "idea.module");
            val testResourceDirs = module.getTestResourceDirs();
            assertNotNull(testResourceDirs, "idea.module.testResourceDirs");
            for (val testSourceSet : testSourceSets) {
                assertTrue(
                    testResourceDirs.containsAll(testSourceSet.getResources().getSrcDirs()),
                    testSourceSet.getName()
                );
            }
        }

        @Test
        @DisplayName("idea.module.singleEntryLibraries contains dirs of 'main' source-set")
        void ideaModuleSingleEntryLibrariesContainsDirsOfMainSourceSet() {
            val idea = project.getExtensions().getByType(IdeaModel.class);
            val module = idea.getModule();
            assertNotNull(module, "idea.module");
            val singleEntryLibraries = module.getSingleEntryLibraries();
            assertNotNull(singleEntryLibraries, "idea.module.singleEntryLibraries");
            val singleEntryLibrariesRuntime = singleEntryLibraries.get("RUNTIME");
            assertNotNull(singleEntryLibrariesRuntime, "idea.module.singleEntryLibraries.RUNTIME");

            val runtimeDirs = stream(singleEntryLibrariesRuntime.spliterator(), false).collect(toList());
            val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            val mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
            assertTrue(runtimeDirs.containsAll(mainSourceSet.getOutput().getDirs().getFiles()));
        }

        @Test
        @DisplayName("idea.module.singleEntryLibraries contains dirs of all testSourceSets")
        void ideaModuleSingleEntryLibrariesContainsDirsOfAllTestSourceSets() {
            val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
            testSourceSets.create("another");

            val idea = project.getExtensions().getByType(IdeaModel.class);
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

}
