package name.remal.gradleplugins.testsourcesets.v2;

import static java.lang.System.identityHashCode;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static name.remal.gradleplugins.testsourcesets.v2.TestSourceSetsKotlinConfigurer.configureKotlinTestSourceSets;
import static org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.val;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;

public class TestSourceSetsPlugin implements Plugin<Project> {

    public static final String TEST_SOURCE_SETS_EXTENSION_NAME = "testSourceSets";

    public static final String ALL_TESTS_TASK_NAME = "allTests";


    private static final boolean IS_MODULARITY_SUPPORTED =
        GradleVersion.current().compareTo(GradleVersion.version("6.4")) >= 0;


    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

        val testSourceSets = createTestSourceSetContainer(project, sourceSets);
        project.getExtensions().add(TestSourceSetContainer.class, TEST_SOURCE_SETS_EXTENSION_NAME, testSourceSets);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        testSourceSets.add(testSourceSet);

        configureConfigurations(project);
        configureTestTasks(project);
        configureIdea(project);

        configureKotlinTestSourceSets(project);
    }

    private static TestSourceSetContainer createTestSourceSetContainer(Project project, SourceSetContainer sourceSets) {
        val container = project.container(SourceSet.class, sourceSets::create);
        sourceSets.whenObjectRemoved(container::remove);
        container.whenObjectRemoved(sourceSets::remove);

        return (TestSourceSetContainer) newProxyInstance(
            TestSourceSetContainer.class.getClassLoader(),
            new Class<?>[]{TestSourceSetContainer.class},
            (proxy, method, args) -> {
                if (method.getParameterTypes().length == 1 && method.getName().equals("equals")) {
                    return proxy == args[0];
                } else if (method.getParameterTypes().length == 0 && method.getName().equals("hashCode")) {
                    return identityHashCode(proxy);
                }

                return method.invoke(container, args);
            }
        );
    }


    private static final Pattern GET_CONFIGURATION_NAME_METHOD_NAME = Pattern.compile(
        "^get[A-Z].*[a-z]ConfigurationName$"
    );

    private static void configureConfigurations(Project project) {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

        val configurationNameMethods = Stream.of(SourceSet.class.getMethods())
            .filter(it -> GET_CONFIGURATION_NAME_METHOD_NAME.matcher(it.getName()).matches())
            .collect(toList());

        val configurations = project.getConfigurations();

        //noinspection Convert2Lambda
        testSourceSets.matching(it -> it != testSourceSet).all(new Action<SourceSet>() {
            @Override
            @SneakyThrows
            public void execute(@NotNull SourceSet sourceSet) {
                for (val method : configurationNameMethods) {
                    val testConfigurationName = method.invoke(testSourceSet);
                    val configurationName = method.invoke(sourceSet);
                    if (testConfigurationName == null
                        || configurationName == null
                        || Objects.equals(testConfigurationName, configurationName)
                    ) {
                        continue;
                    }

                    configurations.matching(it -> it.getName().equals(testConfigurationName)).all(testConfiguration -> {
                        configurations.matching(it -> it.getName().equals(configurationName)).all(configuration -> {
                            configuration.extendsFrom(testConfiguration);
                        });
                    });
                }
            }
        });
    }


    private static final Pattern WORD_TEST_REGEXP = Pattern.compile("(^|[a-z0-9_]|\\b)[Tt]est([A-Z0-9_]|\\b|$)");

    private static void configureTestTasks(Project project) {
        val allTestsTask = project.getTasks().register(ALL_TESTS_TASK_NAME, task -> {
            task.setGroup(VERIFICATION_GROUP);
            task.setDescription("Run test task for each test-source-set");
            task.dependsOn(TEST_TASK_NAME);
        });

        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
        testSourceSets.whenObjectAdded(testSourceSet -> {
            final String taskName;
            {
                val hasWordTest = WORD_TEST_REGEXP.matcher(testSourceSet.getName()).find();
                if (hasWordTest) {
                    taskName = testSourceSet.getTaskName(null, null);
                } else {
                    taskName = testSourceSet.getTaskName(null, "test");
                }
            }

            project.getTasks().register(taskName, Test.class, task -> {
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

            allTestsTask.configure(it -> it.dependsOn(taskName));
        });
    }

    private static void configureTestTaskModularity(Project project, Test testTask) {
        JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        testTask.getModularity().getInferModulePath()
            .convention(javaPluginExtension.getModularity().getInferModulePath());
    }


    private static void configureIdea(Project project) {
        project.getPluginManager().withPlugin("idea", __ -> {
            val ideaModel = project.getExtensions().getByType(IdeaModel.class);
            val module = ideaModel.getModule();
            if (module != null) {
                configureIdeaModule(project, module);
            }
        });
    }

    private static void configureIdeaModule(Project project, IdeaModule module) {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);

        testSourceSets.all(testSourceSet -> {
            project.getConfigurations().all(conf -> {
                if (conf.getName().equals(testSourceSet.getCompileClasspathConfigurationName())
                    || conf.getName().equals(testSourceSet.getRuntimeClasspathConfigurationName())
                ) {
                    val testScope = module.getScopes().computeIfAbsent("TEST", key -> new LinkedHashMap<>());
                    val testPlusScope = testScope.computeIfAbsent("plus", key -> new ArrayList<>());
                    testPlusScope.add(conf);
                }
            });
        });

        ConventionMapping moduleConvention = ((IConventionAware) module).getConventionMapping();
        moduleConvention.map("testSourceDirs", (Callable<Set<File>>) () ->
            testSourceSets.stream()
                .flatMap(it -> it.getAllJava().getSrcDirs().stream())
                .collect(toCollection(LinkedHashSet::new))
        );
        moduleConvention.map("testResourceDirs", (Callable<Set<File>>) () ->
            testSourceSets.stream()
                .flatMap(it -> it.getResources().getSrcDirs().stream())
                .collect(toCollection(LinkedHashSet::new))
        );
        moduleConvention.map("singleEntryLibraries", (Callable<Map<String, FileCollection>>) () ->
            sourceSets.stream()
                .filter(it -> it.getName().equals(MAIN_SOURCE_SET_NAME) || testSourceSets.contains(it))
                .collect(toMap(
                    it -> it.getName().equals(MAIN_SOURCE_SET_NAME) ? "RUNTIME" : "TEST",
                    it -> it.getOutput().getDirs(),
                    FileCollection::plus,
                    LinkedHashMap::new
                ))
        );
    }

}


