package name.remal.gradleplugins.testsourcesets;

import static java.lang.String.format;
import static java.lang.System.identityHashCode;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static name.remal.gradleplugins.testsourcesets.TestSourceSetsKotlinConfigurer.configureKotlinTestSourceSets;
import static name.remal.gradleplugins.testsourcesets.TestTaskNameExtension.getTestTaskName;
import static name.remal.gradleplugins.toolkit.ConventionUtils.addConventionPlugin;
import static name.remal.gradleplugins.toolkit.ExtensionContainerUtils.createExtension;
import static name.remal.gradleplugins.toolkit.ExtensionContainerUtils.getExtension;
import static name.remal.gradleplugins.toolkit.SneakyThrowUtils.sneakyThrows;
import static org.codehaus.groovy.runtime.StringGroovyMethods.capitalize;
import static org.gradle.api.internal.lambdas.SerializableLambdas.action;
import static org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME;
import static org.gradle.api.reporting.Report.OutputType.DIRECTORY;
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
import lombok.val;
import name.remal.gradleplugins.testsourcesets.internal.DefaultTestTaskNameExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
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
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension;
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification;
import org.gradle.testing.jacoco.tasks.JacocoReport;
import org.gradle.util.GradleVersion;

public class TestSourceSetsPlugin implements Plugin<Project> {

    public static final String TEST_SOURCE_SETS_EXTENSION_NAME = "testSourceSets";

    public static final String ALL_TESTS_TASK_NAME = "allTests";


    private static final boolean IS_MODULARITY_SUPPORTED =
        GradleVersion.current().compareTo(GradleVersion.version("6.4")) >= 0;


    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        val sourceSets = getExtension(project, SourceSetContainer.class);

        val testSourceSets = createTestSourceSetContainer(project, sourceSets);
        project.getExtensions().add(TestSourceSetContainer.class, TEST_SOURCE_SETS_EXTENSION_NAME, testSourceSets);

        val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        testSourceSets.add(testSourceSet);

        configureConfigurations(project);
        configureClasspaths(project);
        configureTestTaskNameExtension(project);
        configureTestTasks(project);
        configureJacoco(project);
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
        val configurationNameMethods = Stream.of(SourceSet.class.getMethods())
            .filter(it -> GET_CONFIGURATION_NAME_METHOD_NAME.matcher(it.getName()).matches())
            .collect(toList());

        val configurations = project.getConfigurations();

        val testSourceSet = getExtension(project, SourceSetContainer.class).getByName(TEST_SOURCE_SET_NAME);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);
        testSourceSets.matching(it -> it != testSourceSet).all(sourceSet -> sneakyThrows(() -> {
            for (val method : configurationNameMethods) {
                val testConfigurationName = method.invoke(testSourceSet);
                val configurationName = method.invoke(sourceSet);
                if (testConfigurationName == null
                    || configurationName == null
                    || Objects.equals(testConfigurationName, configurationName)
                ) {
                    continue;
                }

                configurations.matching(it -> it.getName().equals(testConfigurationName)).all(testConfiguration ->
                    configurations.matching(it -> it.getName().equals(configurationName)).all(configuration ->
                        configuration.extendsFrom(testConfiguration)
                    )
                );
            }
        }));
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


    private static void configureTestTaskNameExtension(Project project) {
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);
        testSourceSets.all(testSourceSet -> {
            val extension = createExtension(
                testSourceSet,
                TestTaskNameExtension.class,
                DefaultTestTaskNameExtension.class,
                testSourceSet
            );
            addConventionPlugin(testSourceSet, extension);
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

            allTestsTask.configure(it -> it.dependsOn(testTaskName));
        });
    }

    private static void configureTestTaskModularity(Project project, Test testTask) {
        JavaPluginExtension javaPluginExtension = getExtension(project, JavaPluginExtension.class);
        testTask.getModularity().getInferModulePath()
            .convention(javaPluginExtension.getModularity().getInferModulePath());
    }


    private static void configureJacoco(Project project) {
        project.getPluginManager().withPlugin("jacoco", __ -> {
            val sourceSets = getExtension(project, SourceSetContainer.class);
            val mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
            val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);

            val jacoco = getExtension(project, JacocoPluginExtension.class);

            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.matching(it -> it != testSourceSet).all(sourceSet -> {
                val testTaskName = getExtension(sourceSet, TestTaskNameExtension.class).getTestTaskName();
                Callable<File> executionDataProvider = () -> {
                    val testTask = project.getTasks().getByName(testTaskName);
                    val testTaskJacoco = getExtension(testTask, JacocoTaskExtension.class);
                    return testTaskJacoco.getDestinationFile();
                };

                project.getTasks().register(
                    "jacoco" + capitalize(testTaskName) + "Report",
                    JacocoReport.class,
                    reportTask -> {
                        reportTask.mustRunAfter(testTaskName);
                        reportTask.setGroup(VERIFICATION_GROUP);
                        reportTask.setDescription(format(
                            "Generates code coverage report for the %s task.",
                            testTaskName
                        ));
                        reportTask.executionData(executionDataProvider);
                        reportTask.sourceSets(mainSourceSet);

                        val reportsDir = jacoco.getReportsDirectory();
                        reportTask.getReports().all(action(report -> {
                            if (report.getOutputType().equals(DIRECTORY)) {
                                report.setDestination(reportsDir.dir(
                                    testTaskName + '/' + report.getName()
                                ).map(Directory::getAsFile));
                            } else {
                                report.setDestination(reportsDir.dir(
                                    testTaskName + "/" + reportTask.getName() + "." + report.getName()
                                ).map(Directory::getAsFile));
                            }
                        }));
                    }
                );

                project.getTasks().register(
                    "jacoco" + capitalize(testTaskName) + "CoverageVerification",
                    JacocoCoverageVerification.class,
                    coverageVerificationTask -> {
                        coverageVerificationTask.mustRunAfter(testTaskName);
                        coverageVerificationTask.setGroup(VERIFICATION_GROUP);
                        coverageVerificationTask.setDescription(format(
                            "Verifies code coverage metrics based on specified rules for the %s task.",
                            testTaskName
                        ));
                        coverageVerificationTask.executionData(executionDataProvider);
                        coverageVerificationTask.sourceSets(mainSourceSet);
                    }
                );
            });
        });
    }


    private static void configureIdea(Project project) {
        project.getPluginManager().withPlugin("idea", __ -> {
            val ideaModel = getExtension(project, IdeaModel.class);
            val module = ideaModel.getModule();
            if (module != null) {
                configureIdeaModule(project, module);
            }
        });
    }

    private static void configureIdeaModule(Project project, IdeaModule module) {
        val sourceSets = getExtension(project, SourceSetContainer.class);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        testSourceSets.all(testSourceSet ->
            project.getConfigurations().all(conf -> {
                if (conf.getName().equals(testSourceSet.getCompileClasspathConfigurationName())
                    || conf.getName().equals(testSourceSet.getRuntimeClasspathConfigurationName())
                ) {
                    val testScope = module.getScopes().computeIfAbsent("TEST", key -> new LinkedHashMap<>());
                    val testPlusScope = testScope.computeIfAbsent("plus", key -> new ArrayList<>());
                    testPlusScope.add(conf);
                }
            })
        );

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


