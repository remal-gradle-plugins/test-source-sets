package name.remal.gradleplugins.testsourcesets;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradleplugins.testsourcesets.Utils.classOf;
import static name.remal.gradleplugins.toolkit.ExtensionContainerUtils.getExtension;
import static name.remal.gradleplugins.toolkit.reflection.MembersFinder.findMethod;
import static org.codehaus.groovy.runtime.StringGroovyMethods.capitalize;
import static org.gradle.api.reporting.Report.OutputType.DIRECTORY;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP;

import java.io.File;
import java.util.concurrent.Callable;
import lombok.NoArgsConstructor;
import lombok.val;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension;
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification;
import org.gradle.testing.jacoco.tasks.JacocoReport;

@NoArgsConstructor(access = PRIVATE)
abstract class TestSourceSetsConfigurerJacoco {

    public static void configureJacoco(Project project) {
        project.getPluginManager().withPlugin("jacoco", __ -> {
            val sourceSets = getExtension(project, SourceSetContainer.class);
            val testSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);

            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.matching(it -> it != testSourceSet).all(sourceSet -> {
                val testTaskName = getExtension(sourceSet, TestTaskNameExtension.class).getTestTaskName();
                createJacocoReportTask(project, testTaskName);
                createJacocoCoverageVerificationTask(project, testTaskName);
            });
        });
    }

    private static void createJacocoReportTask(Project project, String testTaskName) {
        project.getTasks().register(
            "jacoco" + capitalize(testTaskName) + "Report",
            JacocoReport.class,
            task -> {
                task.mustRunAfter(testTaskName);
                task.setGroup(VERIFICATION_GROUP);
                task.setDescription(format(
                    "Generates code coverage report for the %s task.",
                    testTaskName
                ));
                task.executionData(createExecutionDataProvider(project, testTaskName));
                task.sourceSets(getExtension(project, SourceSetContainer.class)
                    .getByName(MAIN_SOURCE_SET_NAME)
                );

                val reportsDirProvider = createBaseJacocoReportsDirProvider(project);
                task.getReports().all(report -> {
                    if (report.getOutputType().equals(DIRECTORY)) {
                        report.setDestination(project.provider(() -> {
                            val reportsDir = reportsDirProvider.call();
                            return new File(
                                reportsDir,
                                testTaskName + '/' + report.getName()
                            );
                        }));
                    } else {
                        report.setDestination(project.provider(() -> {
                            val reportsDir = reportsDirProvider.call();
                            return new File(
                                reportsDir,
                                testTaskName + "/" + task.getName() + "." + report.getName()
                            );
                        }));
                    }
                });
            }
        );
    }

    private static void createJacocoCoverageVerificationTask(Project project, String testTaskName) {
        project.getTasks().register(
            "jacoco" + capitalize(testTaskName) + "CoverageVerification",
            JacocoCoverageVerification.class,
            task -> {
                task.mustRunAfter(testTaskName);
                task.setGroup(VERIFICATION_GROUP);
                task.setDescription(format(
                    "Verifies code coverage metrics based on specified rules for the %s task.",
                    testTaskName
                ));
                task.executionData(createExecutionDataProvider(project, testTaskName));
                task.sourceSets(getExtension(project, SourceSetContainer.class)
                    .getByName(MAIN_SOURCE_SET_NAME)
                );
            }
        );
    }

    private static Callable<File> createExecutionDataProvider(Project project, String testTaskName) {
        return () -> {
            val testTask = project.getTasks().getByName(testTaskName);
            val testTaskJacoco = getExtension(testTask, JacocoTaskExtension.class);
            return testTaskJacoco.getDestinationFile();
        };
    }

    private static Callable<File> createBaseJacocoReportsDirProvider(Project project) {
        return () -> {
            val jacoco = getExtension(project, JacocoPluginExtension.class);
            val getReportsDirectory = findMethod(
                classOf(jacoco),
                DirectoryProperty.class,
                "getReportsDirectory"
            );
            if (getReportsDirectory != null) {
                return requireNonNull(getReportsDirectory.invoke(jacoco)).getAsFile().get();
            }

            val getReportsDir = findMethod(
                classOf(jacoco),
                File.class,
                "getReportsDir"
            );
            if (getReportsDir != null) {
                return requireNonNull(getReportsDir.invoke(jacoco));
            }

            throw new UnsupportedOperationException("Can't get jacoco reports dir");
        };
    }

}
