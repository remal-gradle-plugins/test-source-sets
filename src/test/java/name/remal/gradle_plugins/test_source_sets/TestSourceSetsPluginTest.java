package name.remal.gradle_plugins.test_source_sets;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtensions;
import static name.remal.gradle_plugins.toolkit.IdeaModuleUtils.getTestResourceDirs;
import static name.remal.gradle_plugins.toolkit.IdeaModuleUtils.getTestSourceDirs;
import static name.remal.gradle_plugins.toolkit.reflection.MembersFinder.findMethod;
import static name.remal.gradle_plugins.toolkit.reflection.MembersFinder.getMethod;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.unwrapGeneratedSubclass;
import static name.remal.gradle_plugins.toolkit.testkit.ProjectValidations.executeAfterEvaluateActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.SourceSetUtils;
import name.remal.gradle_plugins.toolkit.reflection.TypedMethod0;
import name.remal.gradle_plugins.toolkit.testkit.ApplyPlugin;
import name.remal.gradle_plugins.toolkit.testkit.MinTestableGradleVersion;
import name.remal.gradle_plugins.toolkit.testkit.TaskValidations;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension;
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class TestSourceSetsPluginTest {

    @ApplyPlugin(type = TestSourceSetsPlugin.class)
    private final Project project;


    @Test
    void pluginTasksDoNotHavePropertyProblems() {
        executeAfterEvaluateActions(project);

        var taskClassNamePrefix = packageNameOf(TestSourceSetsPlugin.class) + '.';
        project.getTasks().stream()
            .filter(task -> {
                var taskClass = unwrapGeneratedSubclass(task.getClass());
                return taskClass.getName().startsWith(taskClassNamePrefix);
            })
            .map(TaskValidations::markTaskDependenciesAsSkipped)
            .forEach(TaskValidations::assertNoTaskPropertiesProblems);
    }


    @Test
    void appliesJavaPlugin() {
        assertTrue(project.getPluginManager().hasPlugin("java"));
    }

    @Test
    @MinTestableGradleVersion("7.3")
    void appliesJvmTestSuitePlugin() {
        assertTrue(project.getPluginManager().hasPlugin("jvm-test-suite"));
    }

    @Test
    void extensionOfTypeTestSourceSetContainerIsCreated() {
        assertNotNull(getExtension(project, TestSourceSetContainer.class));
    }

    @Test
    void testSourceSetsExtensionCreated() {
        assertNotNull(project.getExtensions().getByName("testSourceSets"));
    }

    @Test
    void containsTestSourceSet() {
        var sourceSets = getExtension(project, SourceSetContainer.class);
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);

        var testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        assertTrue(testSourceSets.contains(testSourceSet));
    }

    @Test
    void testSourceSetNameIsCheckedForSuffixByDefault() {
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);
        assertThrows(
            InvalidTestSourceSetNameSuffix.class,
            () -> testSourceSets.create("integration")
        );
    }

    @Test
    void testSourceSetNameIsNotCheckedForSuffixIfTheCheckIsDisabled() {
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);
        testSourceSets.getTestSuffixCheck().set(TestSuffixCheckMode.DISABLE);
        assertDoesNotThrow(
            () -> testSourceSets.create("integration")
        );
    }

    @Test
    void creatingInTestSourceSetsCreatesInSourceSets() {
        var sourceSets = getExtension(project, SourceSetContainer.class);
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);

        var anotherTestSourceSet = testSourceSets.create("anotherTest");
        assertTrue(sourceSets.contains(anotherTestSourceSet));
        assertTrue(testSourceSets.contains(anotherTestSourceSet));
    }

    @Test
    void removalFromSourceSetsRemovesFromTestSourceSets() {
        var sourceSets = getExtension(project, SourceSetContainer.class);
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);

        var testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        sourceSets.remove(testSourceSet);
        assertFalse(testSourceSets.contains(testSourceSet));
    }

    @Test
    void removalFromTestSourceSetsRemovesFromSourceSets() {
        var sourceSets = getExtension(project, SourceSetContainer.class);
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);

        var testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        testSourceSets.remove(testSourceSet);
        assertFalse(testSourceSets.contains(testSourceSet));
    }

    @Test
    void compileClasspathConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        var configurations = project.getConfigurations();
        var sourceSets = getExtension(project, SourceSetContainer.class);
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);

        var testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        var integrationSourceSet = testSourceSets.create("integrationTest");
        assertTrue(
            configurations.getByName(integrationSourceSet.getCompileClasspathConfigurationName())
                .getExtendsFrom()
                .contains(configurations.getByName(testSourceSet.getCompileClasspathConfigurationName())),
            testSourceSet.getCompileClasspathConfigurationName()
        );
    }

    @Test
    void runtimeClasspathConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        var configurations = project.getConfigurations();
        var sourceSets = getExtension(project, SourceSetContainer.class);
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);

        var testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        var integrationSourceSet = testSourceSets.create("integrationTest");
        assertTrue(
            configurations.getByName(integrationSourceSet.getRuntimeClasspathConfigurationName())
                .getExtendsFrom()
                .contains(configurations.getByName(testSourceSet.getRuntimeClasspathConfigurationName())),
            testSourceSet.getRuntimeClasspathConfigurationName()
        );
    }

    @Test
    void implementationConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        var configurations = project.getConfigurations();
        var sourceSets = getExtension(project, SourceSetContainer.class);
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);

        var testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        var integrationSourceSet = testSourceSets.create("integrationTest");
        assertTrue(
            configurations.getByName(integrationSourceSet.getImplementationConfigurationName())
                .getExtendsFrom()
                .contains(configurations.getByName(testSourceSet.getImplementationConfigurationName())),
            testSourceSet.getImplementationConfigurationName()
        );
    }

    @Test
    void compileOnlyConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        var configurations = project.getConfigurations();
        var sourceSets = getExtension(project, SourceSetContainer.class);
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);

        var testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        var integrationSourceSet = testSourceSets.create("integrationTest");
        assertTrue(
            configurations.getByName(integrationSourceSet.getCompileOnlyConfigurationName())
                .getExtendsFrom()
                .contains(configurations.getByName(testSourceSet.getCompileOnlyConfigurationName())),
            testSourceSet.getCompileOnlyConfigurationName()
        );
    }

    @Test
    void runtimeOnlyConfigurationsOfTestSourceSetsExtendCorrespondingConfigurationOfTestSourceSet() {
        var configurations = project.getConfigurations();
        var sourceSets = getExtension(project, SourceSetContainer.class);
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);

        var testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        var integrationSourceSet = testSourceSets.create("integrationTest");
        assertTrue(
            configurations.getByName(integrationSourceSet.getRuntimeOnlyConfigurationName())
                .getExtendsFrom()
                .contains(configurations.getByName(testSourceSet.getRuntimeOnlyConfigurationName())),
            testSourceSet.getRuntimeOnlyConfigurationName()
        );
    }

    @Test
    void testTaskExtensionAddedToEachTestSourceSet() {
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);
        var testTaskProviderType = new TypeOf<TaskProvider<org.gradle.api.tasks.testing.Test>>() { };

        var testSourceSet = testSourceSets.getByName("test");
        var testTask = project.getTasks().named(
            testSourceSet.getName(),
            org.gradle.api.tasks.testing.Test.class
        ).get();
        var testTaskProvider = getExtensions(testSourceSet).getByType(testTaskProviderType);
        assertSame(testTask, testTaskProvider.get());

        var integrationTestSourceSet = testSourceSets.create("integrationTest");
        var integrationTestTask = project.getTasks().named(
            integrationTestSourceSet.getName(),
            org.gradle.api.tasks.testing.Test.class
        ).get();
        var integrationTestTaskProvider = getExtensions(integrationTestSourceSet).getByType(testTaskProviderType);
        assertSame(integrationTestTask, integrationTestTaskProvider.get());
    }


    @Nested
    class CreatedTestTasks {

        private final SourceSet integrationSourceSet;
        private final org.gradle.api.tasks.testing.Test testTask;
        private final org.gradle.api.tasks.testing.Test integrationTestTask;

        {
            var testSourceSets = getExtension(project, TestSourceSetContainer.class);
            integrationSourceSet = testSourceSets.create("integrationTest");

            var testTasks = project.getTasks().withType(org.gradle.api.tasks.testing.Test.class);
            testTask = testTasks.findByName("test");
            integrationTestTask = testTasks.findByName("integrationTest");
        }

        @Test
        void testTaskIsCreatedForEveryTestSourceSet() {
            assertNotNull(testTask);
            assertNotNull(integrationTestTask);
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
                assertThat(allTestsTask.getDependsOn())
                    .contains(testTask.getName());

                assertNotNull(integrationTestTask);
                assertThat(allTestsTask.getDependsOn())
                    .contains(integrationTestTask.getName());
            }

        }

        @Test
        void createdTestTaskHasCorrectTestClassesDirs() {
            assertNotNull(integrationTestTask);
            assertThat(integrationTestTask.getTestClassesDirs().getFiles())
                .isNotEmpty()
                .containsExactlyElementsOf(integrationSourceSet.getOutput().getClassesDirs().getFiles());
        }

        @Test
        void createdTestTaskHasCorrectClasspath() {
            var runtimeClasspath = project.files(
                project.file("integrationSourceSet.runtime.jar")
            );
            integrationSourceSet.setRuntimeClasspath(runtimeClasspath);

            assertNotNull(integrationTestTask);
            assertThat(integrationTestTask.getClasspath().getFiles())
                .isNotEmpty()
                .containsExactlyElementsOf(integrationSourceSet.getRuntimeClasspath().getFiles());
        }

        @Test
        @MinTestableGradleVersion("6.4")
        void createdTestTaskHasCorrectModularitySettings() {
            JavaPluginExtension javaPluginExtension = getExtension(project, JavaPluginExtension.class);
            assertNotNull(integrationTestTask);

            javaPluginExtension.getModularity().getInferModulePath().set(true);
            assertTrue(integrationTestTask.getModularity().getInferModulePath().get());

            javaPluginExtension.getModularity().getInferModulePath().set(false);
            assertFalse(integrationTestTask.getModularity().getInferModulePath().get());
        }


    }


    @Nested
    class IfJacocoPluginIsApplied {

        {
            project.getPluginManager().apply("jacoco");
        }

        @Test
        void correspondingReportTaskIsCreatedForCreatedTestTasks() {
            var testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("integrationTest");

            assertNotNull(project.getTasks().findByName("jacocoIntegrationTestReport"));
            assertNotNull(project.getTasks().findByName("jacocoIntegrationTestCoverageVerification"));
        }

    }


    @Nested
    class IfJavaGradlePluginIsApplied {

        {
            project.getPluginManager().apply("java-gradle-plugin");
        }

        @Test
        void test() {
            var testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("integrationTest");

            var gradlePluginDev = getExtension(project, GradlePluginDevelopmentExtension.class);
            assertThat(gradlePluginDev.getTestSourceSets())
                .isNotEmpty()
                .contains(testSourceSets.getByName("test"), testSourceSets.getByName("integrationTest"));
        }

    }


    @Nested
    class IfKotlinPluginIsApplied {

        {
            project.getPluginManager().apply("kotlin");
        }

        @Test
        @MinTestableGradleVersion("6.1")
        void allTestSourceSetsAreAssociatedWithMainCompilation() {
            var kotlin = getExtension(project, KotlinSingleTargetExtension.class);
            var testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("anotherTest");

            @SuppressWarnings("rawtypes")
            TypedMethod0<KotlinCompilation, Collection> getAssociatedCompilations = findMethod(
                KotlinCompilation.class,
                Collection.class,
                "getAssociatedCompilations"
            );
            if (getAssociatedCompilations == null) {
                getAssociatedCompilations = getMethod(
                    KotlinCompilation.class,
                    Collection.class,
                    "getAssociateWith"
                );
            }

            @SuppressWarnings({"unchecked", "rawtypes"})
            NamedDomainObjectContainer<KotlinCompilation> compilations =
                (NamedDomainObjectContainer) kotlin.getTarget().getCompilations();
            var mainCompilation = compilations.getByName(MAIN_SOURCE_SET_NAME);
            for (var testSourceSet : testSourceSets) {
                var compilation = compilations.getByName(testSourceSet.getName());
                var associatedCompilations = getAssociatedCompilations.invoke(compilation);
                assertTrue(associatedCompilations.contains(mainCompilation), testSourceSet.getName());
            }
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
            var testSourceSets = getExtension(project, TestSourceSetContainer.class);
            var anotherTestSourceSet = testSourceSets.create("anotherTest");

            var compileClasspathConfiguration = project.getConfigurations().getByName(
                anotherTestSourceSet.getCompileClasspathConfigurationName()
            );
            var idea = getExtension(project, IdeaModel.class);
            var module = idea.getModule();
            assertNotNull(module, "idea.module");
            var scopes = module.getScopes();
            assertNotNull(scopes, "idea.module.scopes");
            var testScope = scopes.get("TEST");
            assertNotNull(testScope, "idea.module.scopes.TEST");
            var testPlusScope = testScope.get("plus");
            assertNotNull(testPlusScope, "idea.module.scopes.TEST.plus");
            assertTrue(testPlusScope.contains(compileClasspathConfiguration));
        }

        @Test
        void runtimeClasspathConfigurationIsAddedToIdeaModuleScopeTest() {
            var testSourceSets = getExtension(project, TestSourceSetContainer.class);
            var anotherTestSourceSet = testSourceSets.create("anotherTest");

            var runtimeClasspathConfiguration = project.getConfigurations().getByName(
                anotherTestSourceSet.getRuntimeClasspathConfigurationName()
            );
            var idea = getExtension(project, IdeaModel.class);
            var module = idea.getModule();
            assertNotNull(module, "idea.module");
            var scopes = module.getScopes();
            assertNotNull(scopes, "idea.module.scopes");
            var testScope = scopes.get("TEST");
            assertNotNull(testScope, "idea.module.scopes.TEST");
            var testPlusScope = testScope.get("plus");
            assertNotNull(testPlusScope, "idea.module.scopes.TEST.plus");
            assertTrue(testPlusScope.contains(runtimeClasspathConfiguration));
        }

        @Test
        void ideaModuleTestSourceDirsContainsDirsOfAllTestSourceSets() {
            var testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("anotherTest");

            var idea = getExtension(project, IdeaModel.class);
            var module = idea.getModule();
            assertNotNull(module, "idea.module");
            var testSourceDirs = getTestSourceDirs(module);
            assertNotNull(testSourceDirs, "idea.module.testSourceDirs");
            for (var testSourceSet : testSourceSets) {
                assertTrue(
                    testSourceDirs.containsAll(testSourceSet.getAllJava().getSrcDirs()),
                    testSourceSet.getName()
                );
            }
        }

        @Test
        void ideaModuleTestResourceDirsContainsDirsOfAllTestSourceSets() {
            var testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("anotherTest");

            var idea = getExtension(project, IdeaModel.class);
            var module = idea.getModule();
            assertNotNull(module, "idea.module");
            var testResourceDirs = getTestResourceDirs(module);
            assertNotNull(testResourceDirs, "idea.module.testResourceDirs");
            for (var testSourceSet : testSourceSets) {
                assertTrue(
                    testResourceDirs.containsAll(testSourceSet.getResources().getSrcDirs()),
                    testSourceSet.getName()
                );
            }
        }

        @Test
        void ideaModuleSingleEntryLibrariesContainsDirsOfMainSourceSet() {
            var idea = getExtension(project, IdeaModel.class);
            var module = idea.getModule();
            assertNotNull(module, "idea.module");
            var singleEntryLibraries = module.getSingleEntryLibraries();
            assertNotNull(singleEntryLibraries, "idea.module.singleEntryLibraries");
            var singleEntryLibrariesRuntime = singleEntryLibraries.get("RUNTIME");
            assertNotNull(singleEntryLibrariesRuntime, "idea.module.singleEntryLibraries.RUNTIME");

            var runtimeDirs = stream(singleEntryLibrariesRuntime.spliterator(), false).collect(toList());
            var sourceSets = getExtension(project, SourceSetContainer.class);
            var mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
            assertTrue(runtimeDirs.containsAll(mainSourceSet.getOutput().getDirs().getFiles()));
        }

        @Test
        void ideaModuleSingleEntryLibrariesContainsDirsOfAllTestSourceSets() {
            var testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("anotherTest");

            var idea = getExtension(project, IdeaModel.class);
            var module = idea.getModule();
            assertNotNull(module, "idea.module");
            var singleEntryLibraries = module.getSingleEntryLibraries();
            assertNotNull(singleEntryLibraries, "idea.module.singleEntryLibraries");
            var singleEntryLibrariesTest = singleEntryLibraries.get("TEST");
            assertNotNull(singleEntryLibrariesTest, "idea.module.singleEntryLibraries.TEST");

            var testDirs = stream(singleEntryLibrariesTest.spliterator(), false).collect(toList());
            for (var testSourceSet : testSourceSets) {
                assertTrue(
                    testDirs.containsAll(testSourceSet.getOutput().getDirs().getFiles()),
                    testSourceSet.getName()
                );
            }
        }

    }


    @Nested
    @MinTestableGradleVersion("7.5")
    @SuppressWarnings("UnstableApiUsage")
    class IfEclipsePluginIsApplied {

        {
            project.getPluginManager().apply("eclipse");
            executeAfterEvaluateActions(project);
        }

        @Test
        void eclipseModuleTestSourceSetsContainsAllTestSourceSets() {
            var testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("anotherTest");

            var eclipse = getExtension(project, EclipseModel.class);
            var classpath = eclipse.getClasspath();
            assertNotNull(classpath, "eclipse.classpath");

            assertThat(classpath.getTestSourceSets().get())
                .containsExactlyInAnyOrderElementsOf(testSourceSets);
        }

        @Test
        void eclipseModuleTestConfigurationsContainsAllConfigurationsOfAllTestSourceSets() {
            var testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.create("anotherTest");

            var eclipse = getExtension(project, EclipseModel.class);
            var classpath = eclipse.getClasspath();
            assertNotNull(classpath, "eclipse.classpath");

            var allTestSourceSetConfigurations = testSourceSets.stream()
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
