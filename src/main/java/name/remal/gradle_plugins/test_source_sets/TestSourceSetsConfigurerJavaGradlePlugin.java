package name.remal.gradle_plugins.test_source_sets;

import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;

import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.val;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension;

@NoArgsConstructor(access = PRIVATE)
abstract class TestSourceSetsConfigurerJavaGradlePlugin {

    public static void configureJavaGradlePlugin(Project project) {
        project.getPluginManager().withPlugin("java-gradle-plugin", __ -> {
            val testSourceSets = getExtension(project, TestSourceSetContainer.class);
            val gradlePluginDev = getExtension(project, GradlePluginDevelopmentExtension.class);
            testSourceSets.whenObjectAdded(testSourceSet -> {
                gradlePluginDev.testSourceSets(
                    Stream.concat(
                            gradlePluginDev.getTestSourceSets().stream(),
                            Stream.of(testSourceSet)
                        )
                        .distinct()
                        .toArray(SourceSet[]::new)
                );
            });
        });
    }

}
