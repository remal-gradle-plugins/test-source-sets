package name.remal.gradle_plugins.test_source_sets;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;
import static name.remal.gradle_plugins.toolkit.ProjectUtils.afterEvaluateOrNow;
import static name.remal.gradle_plugins.toolkit.SourceSetUtils.getSourceSetConfigurationNames;

import java.util.Objects;
import lombok.NoArgsConstructor;
import lombok.val;
import org.gradle.api.Project;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.util.GradleVersion;

@NoArgsConstructor(access = PRIVATE)
abstract class TestSourceSetsConfigurerEclipse {

    private static final GradleVersion MIN_VERSION_TO_SUPPORT_TEST_SOURCE_SETS = GradleVersion.version("7.5");

    public static void configureEclipse(Project project) {
        if (GradleVersion.current().compareTo(MIN_VERSION_TO_SUPPORT_TEST_SOURCE_SETS) < 0) {
            return;
        }

        project.getPluginManager().withPlugin("eclipse", __ -> {
            afterEvaluateOrNow(project, ___ -> configureEclipseImpl(project));
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void configureEclipseImpl(Project project) {
        val eclipseModel = getExtension(project, EclipseModel.class);
        val eclipseClasspath = eclipseModel.getClasspath();
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);
        testSourceSets.all(testSourceSet -> {
            eclipseClasspath.getTestSourceSets().add(testSourceSet);
            eclipseClasspath.getTestConfigurations().addAll(
                getSourceSetConfigurationNames(testSourceSet).stream()
                    .map(project.getConfigurations()::findByName)
                    .filter(Objects::nonNull)
                    .collect(toList())
            );
        });
    }

}
