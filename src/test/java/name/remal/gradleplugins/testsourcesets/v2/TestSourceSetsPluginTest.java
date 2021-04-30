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
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TestSourceSetsPluginTest extends BaseProjectTestExtension {

    private final Project project = newProject();

    {
        project.getPluginManager().apply(TestSourceSetsPlugin.class);
    }


    @Test
    void applies_java_plugin() {
        assertTrue(project.getPluginManager().hasPlugin("java"));
    }

    @Test
    void TestSourceSetContainer_extension_created() {
        assertNotNull(project.getExtensions().getByType(TestSourceSetContainer.class));
    }

    @Test
    void testSourceSets_extension_created() {
        assertNotNull(project.getExtensions().getByName("testSourceSets"));
    }

    @Test
    void contains_test_source_set() {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        assertTrue(testSourceSets.contains(testSourceSet));
    }

    @Test
    void creating_in_testSourceSets_creates_in_sourceSets() {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

        val anotherTestSourceSet = testSourceSets.create("another");
        assertTrue(sourceSets.contains(anotherTestSourceSet));
        assertTrue(testSourceSets.contains(anotherTestSourceSet));
    }

    @Test
    void removal_from_sourceSets_removes_from_testSourceSets() {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        sourceSets.remove(testSourceSet);
        assertFalse(testSourceSets.contains(testSourceSet));
    }

    @Test
    void removal_from_testSourceSets_removes_from_sourceSets() {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        testSourceSets.remove(testSourceSet);
        assertFalse(testSourceSets.contains(testSourceSet));
    }

    @Test
    void compileClasspath_configurations_of_testSourceSets_extend_implementation_configurations_of_test_source_set() {
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
    void runtimeClasspath_configurations_of_testSourceSets_extend_implementation_configurations_of_test_source_set() {
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
    void implementation_configurations_of_testSourceSets_extend_implementation_configurations_of_test_source_set() {
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
    void compileOnly_configurations_of_testSourceSets_extend_implementation_configurations_of_test_source_set() {
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
    void runtimeOnly_configurations_of_testSourceSets_extend_implementation_configurations_of_test_source_set() {
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
    class If_idea_plugin_is_applied {

        {
            project.getPluginManager().apply(IdeaPlugin.class);
        }

        @Test
        void compile_classpath_configuration_is_added_to_TEST_idea_module_scope() {
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
        void runtime_classpath_configuration_is_added_to_TEST_idea_module_scope() {
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
        void idea_module_testSourceDirs_contains_dirs_of_all_testSourceSets() {
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
        void idea_module_testResourceDirs_contains_dirs_of_all_testSourceSets() {
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
        void idea_module_singleEntryLibraries_contains_dirs_of_main_source_set() {
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
        void idea_module_singleEntryLibraries_contains_dirs_of_all_testSourceSets() {
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
