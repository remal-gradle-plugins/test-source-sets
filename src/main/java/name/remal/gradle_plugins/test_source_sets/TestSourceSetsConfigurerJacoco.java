package name.remal.gradle_plugins.test_source_sets;

import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;
import static org.codehaus.groovy.runtime.StringGroovyMethods.capitalize;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP;

import java.io.File;
import lombok.NoArgsConstructor;
import lombok.val;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
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

    private static Provider<File> createExecutionDataProvider(Project project, String testTaskName) {
        return project.provider(() -> {
            val testTask = project.getTasks().getByName(testTaskName);
            val testTaskJacoco = getExtension(testTask, JacocoTaskExtension.class);
            return testTaskJacoco.getDestinationFile();
        });
    }

}
