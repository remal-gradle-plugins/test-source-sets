package name.remal.gradle_plugins.test_source_sets;

import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.test_source_sets.TestTaskNameUtils.getTestTaskName;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;
import static org.codehaus.groovy.runtime.StringGroovyMethods.capitalize;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP;

import java.io.File;
import lombok.NoArgsConstructor;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension;
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification;
import org.gradle.testing.jacoco.tasks.JacocoReport;

@NoArgsConstructor(access = PRIVATE)
abstract class TestSourceSetsConfigurerJacoco {

    public static void configureJacoco(Project project) {
        project.getPluginManager().withPlugin("jacoco", __ -> {
            var testSourceSets = getExtension(project, TestSourceSetContainer.class);
            testSourceSets.configureEach(sourceSet -> {
                var testTask = project.getTasks().named(getTestTaskName(sourceSet), Test.class);
                createJacocoReportTask(project, testTask);
                createJacocoCoverageVerificationTask(project, testTask);
            });
        });
    }

    private static void createJacocoReportTask(Project project, TaskProvider<?> testTask) {
        var tasks = project.getTasks();

        final TaskProvider<JacocoReport> jacocoReportTask;
        var jacocoReportTaskName = "jacoco" + capitalize(testTask.getName()) + "Report";
        if (tasks.getNames().contains(jacocoReportTaskName)) {
            jacocoReportTask = tasks.named(jacocoReportTaskName, JacocoReport.class);

        } else {
            jacocoReportTask = tasks.register(
                jacocoReportTaskName,
                JacocoReport.class,
                task -> {
                    task.setGroup(VERIFICATION_GROUP);
                    task.setDescription(format(
                        "Generates code coverage report for the %s task.",
                        testTask.getName()
                    ));
                    task.executionData(createExecutionDataProvider(project, testTask));
                    task.sourceSets(getExtension(project, SourceSetContainer.class)
                        .getByName(MAIN_SOURCE_SET_NAME)
                    );
                }
            );
        }

        jacocoReportTask.configure(task -> {
            task.mustRunAfter(testTask);
        });
    }

    private static void createJacocoCoverageVerificationTask(Project project, TaskProvider<?> testTask) {
        var tasks = project.getTasks();

        final TaskProvider<JacocoCoverageVerification> jacocoVerificationTask;
        var jacocoVerificationTaskName = "jacoco" + capitalize(testTask.getName()) + "CoverageVerification";
        if (tasks.getNames().contains(jacocoVerificationTaskName)) {
            jacocoVerificationTask = tasks.named(jacocoVerificationTaskName, JacocoCoverageVerification.class);

        } else {
            jacocoVerificationTask = tasks.register(
                jacocoVerificationTaskName,
                JacocoCoverageVerification.class,
                task -> {
                    task.mustRunAfter(testTask);
                    task.setGroup(VERIFICATION_GROUP);
                    task.setDescription(format(
                        "Verifies code coverage metrics based on specified rules for the %s task.",
                        testTask.getName()
                    ));
                    task.executionData(createExecutionDataProvider(project, testTask));
                    task.sourceSets(getExtension(project, SourceSetContainer.class)
                        .getByName(MAIN_SOURCE_SET_NAME)
                    );
                }
            );
        }

        jacocoVerificationTask.configure(task -> {
            task.mustRunAfter(testTask);
        });
    }

    private static Provider<File> createExecutionDataProvider(Project project, TaskProvider<?> testTask) {
        return project.provider(() -> {
            var testTaskJacoco = getExtension(testTask.get(), JacocoTaskExtension.class);
            return testTaskJacoco.getDestinationFile();
        });
    }

}
