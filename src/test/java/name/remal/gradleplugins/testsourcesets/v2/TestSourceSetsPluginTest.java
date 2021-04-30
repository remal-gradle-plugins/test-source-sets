package name.remal.gradleplugins.testsourcesets.v2;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.val;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TestSourceSetsPluginTest extends BaseProjectTestExtension {

    private final Project project = newProject();

    {
        project.getPluginManager().apply(TestSourceSetsPlugin.class);
    }


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
    @DisplayName("if 'kotlin' plugin is applied")
    class IfKotlinPluginIsApplied {

        {
            project.getPluginManager().apply("kotlin");
        }

        @Test
        @DisplayName("all test-source-sets are associated with 'main' compilation")
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