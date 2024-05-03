package name.remal.gradle_plugins.test_source_sets;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.test_source_sets.DocUtils.PLUGIN_REPOSITORY_HTML_URL;
import static name.remal.gradle_plugins.test_source_sets.TestSourceSetsConfigurerEclipse.configureEclipse;
import static name.remal.gradle_plugins.test_source_sets.TestSourceSetsConfigurerIdea.configureIdea;
import static name.remal.gradle_plugins.test_source_sets.TestSourceSetsConfigurerJacoco.configureJacoco;
import static name.remal.gradle_plugins.test_source_sets.TestSourceSetsConfigurerKotlin.configureKotlinTestSourceSets;
import static name.remal.gradle_plugins.test_source_sets.TestTaskNameUtils.getTestTaskName;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.addExtension;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtensions;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static name.remal.gradle_plugins.toolkit.ProxyUtils.toDynamicInterface;
import static org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.UnknownPluginException;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testing.base.TestingExtension;
import org.gradle.util.GradleVersion;

@CustomLog
public class TestSourceSetsPlugin implements Plugin<Project> {

    public static final String TEST_SOURCE_SETS_EXTENSION_NAME = doNotInline("testSourceSets");

    public static final String ALL_TESTS_TASK_NAME = doNotInline("allTests");

    public static final String TEST_TASK_EXTENSION_NAME = doNotInline("testTask");


    private static final boolean IS_MODULARITY_SUPPORTED =
        GradleVersion.current().compareTo(GradleVersion.version("6.4")) >= 0;


    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);

        try {
            project.getPluginManager().apply("jvm-test-suite");
        } catch (UnknownPluginException ignored) {
            // do nothing
        }

        val testSourceSets = createTestSourceSetContainer(project);
        addExtension(project, TestSourceSetContainer.class, TEST_SOURCE_SETS_EXTENSION_NAME, testSourceSets);

        val sourceSets = getExtension(project, SourceSetContainer.class);
        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        testSourceSets.add(testSourceSet);

        configureConfigurations(project);
        configureClasspaths(project);
        configureTestTasks(project);
        configureTestTaskExtensions(project);
        configureJacoco(project);
        configureIdea(project);
        configureEclipse(project);

        configureKotlinTestSourceSets(project);
    }

    @SneakyThrows
    private static TestSourceSetContainer createTestSourceSetContainer(Project project) {
        val testSuffixCheck = project.getObjects().property(TestSuffixCheckMode.class);
        testSuffixCheck.convention(TestSuffixCheckMode.FAIL);

        Consumer<String> checkTestSourceSetName = name -> {
            if (!name.endsWith("Test")) {
                val message = format(
                    "Test source set name does NOT end with \"Test\": \"%s\". It violates principles of"
                        + " `jvm-test-suite` Gradle plugin. Please add \"Test\" suffix to the test source set name."
                        + " If you want to configure or completely disable this check, see the plugin's"
                        + " documentation: %s#test-source-set-name-suffix-check",
                    name,
                    PLUGIN_REPOSITORY_HTML_URL
                );
                val testSuffixCheckMode = testSuffixCheck.getOrNull();
                if (testSuffixCheckMode == TestSuffixCheckMode.FAIL) {
                    throw new InvalidTestSourceSetNameSuffix(message);
                } else if (testSuffixCheckMode == TestSuffixCheckMode.WARN) {
                    logger.warn(message);
                }
            }
        };

        val testSourceSets = project.container(SourceSet.class, name -> {
            checkTestSourceSetName.accept(name);
            return createSourceSet(project, name);
        });

        val sourceSets = getExtension(project, SourceSetContainer.class);
        sourceSets.whenObjectRemoved(testSourceSets::remove);
        testSourceSets.whenObjectRemoved(sourceSets::remove);

        val getTestSuffixCheckMethod = TestSourceSetContainer.class.getMethod("getTestSuffixCheck");
        return toDynamicInterface(testSourceSets, TestSourceSetContainer.class, invocationHandler -> {
            invocationHandler.add(getTestSuffixCheckMethod::equals, (proxy, method, args) -> testSuffixCheck);
        });
    }

    private static SourceSet createSourceSet(Project project, String name) {
        if (project.getPluginManager().hasPlugin("jvm-test-suite")) {
            return createTestSuiteSourceSet(project, name);

        } else {
            return createSourceSetDefault(project, name);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private static SourceSet createTestSuiteSourceSet(Project project, String name) {
        val testing = getExtension(project, TestingExtension.class);
        val testSuite = testing.getSuites().create(name, JvmTestSuite.class);
        return testSuite.getSources();
    }

    private static SourceSet createSourceSetDefault(Project project, String name) {
        return getExtension(project, SourceSetContainer.class).create(name);
    }


    private static void configureConfigurations(Project project) {
        val configurations = project.getConfigurations();

        val testSourceSet = getExtension(project, SourceSetContainer.class).getByName(TEST_SOURCE_SET_NAME);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);
        testSourceSets.matching(it -> it != testSourceSet).all(sourceSet ->
            forConfigurations(testSourceSet, sourceSet, (testConfigurationName, configurationName) -> {
                configurations.matching(it -> it.getName().equals(testConfigurationName)).all(testConfiguration ->
                    configurations.matching(it -> it.getName().equals(configurationName)).all(configuration ->
                        configuration.extendsFrom(testConfiguration)
                    )
                );
            })
        );
    }

    private static final Pattern GET_CONFIGURATION_NAME_METHOD_NAME = Pattern.compile(
        "^get[A-Z].*[a-z]ConfigurationName$"
    );

    private static final List<Method> GET_CONFIGURATION_NAME_METHODS = Stream.of(SourceSet.class.getMethods())
        .filter(it -> GET_CONFIGURATION_NAME_METHOD_NAME.matcher(it.getName()).matches())
        .collect(toList());

    @SneakyThrows
    private static void forConfigurations(
        SourceSet sourceSet1,
        SourceSet sourceSet2,
        BiConsumer<String, String> action
    ) {
        for (val method : GET_CONFIGURATION_NAME_METHODS) {
            val configurationName1 = method.invoke(sourceSet1);
            val configurationName2 = method.invoke(sourceSet2);
            if (configurationName1 == null
                || configurationName2 == null
                || Objects.equals(configurationName1, configurationName2)
            ) {
                continue;
            }

            action.accept(configurationName1.toString(), configurationName2.toString());
        }
    }

    private static void configureClasspaths(Project project) {
        val sourceSets = getExtension(project, SourceSetContainer.class);
        val mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        val configurations = project.getConfigurations();

        val testSourceSets = getExtension(project, TestSourceSetContainer.class);
        testSourceSets.matching(it -> it != testSourceSet).all(sourceSet -> {
            sourceSet.setCompileClasspath(
                mainSourceSet.getOutput()
                    .plus(configurations.getByName(sourceSet.getCompileClasspathConfigurationName()))
            );
            sourceSet.setRuntimeClasspath(
                sourceSet.getOutput()
                    .plus(mainSourceSet.getOutput())
                    .plus(configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName()))
            );
        });
    }


    private static void configureTestTasks(Project project) {
        val allTestsTask = project.getTasks().register(ALL_TESTS_TASK_NAME, task -> {
            task.setGroup(VERIFICATION_GROUP);
            task.setDescription("Run test task for each test-source-set");
            task.dependsOn(TEST_TASK_NAME);
        });

        val testSourceSets = getExtension(project, TestSourceSetContainer.class);
        testSourceSets.whenObjectAdded(testSourceSet -> {
            val testTaskName = getTestTaskName(testSourceSet);
            allTestsTask.configure(it -> it.dependsOn(testTaskName));

            if (project.getTasks().getNames().contains(testTaskName)) {
                return;
            }

            project.getTasks().register(testTaskName, Test.class, task -> {
                task.setGroup(VERIFICATION_GROUP);
                task.setDescription("Runs " + testSourceSet.getName() + " tests");

                ConventionMapping conventionMapping = task.getConventionMapping();
                conventionMapping.map(
                    "testClassesDirs",
                    (Callable<FileCollection>) () -> testSourceSet.getOutput().getClassesDirs()
                );
                conventionMapping.map(
                    "classpath",
                    (Callable<FileCollection>) testSourceSet::getRuntimeClasspath
                );
                if (IS_MODULARITY_SUPPORTED) {
                    configureTestTaskModularity(project, task);
                }
            });
        });
    }

    private static void configureTestTaskModularity(Project project, Test testTask) {
        JavaPluginExtension javaPluginExtension = getExtension(project, JavaPluginExtension.class);
        testTask.getModularity().getInferModulePath()
            .convention(javaPluginExtension.getModularity().getInferModulePath());
    }


    private static void configureTestTaskExtensions(Project project) {
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);
        testSourceSets.configureEach(testSourceSet -> {
            val testTaskName = getTestTaskName(testSourceSet);
            val testTaskProvider = project.getTasks().named(testTaskName, Test.class);
            getExtensions(testSourceSet).add(
                new TypeOf<TaskProvider<Test>>() { },
                TEST_TASK_EXTENSION_NAME,
                testTaskProvider
            );
        });
    }

}


