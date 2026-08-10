package name.remal.gradle_plugins.test_source_sets;

import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import lombok.RequiredArgsConstructor;
import org.gradle.api.Project;
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class TestSourceSetsConfigurerJavaGradlePluginTest {

    private final Project project;

    /**
     * Prevents {@link TestSourceSetsPlugin} from using an extension before Gradle finishes
     * creating it.
     *
     * <p>The test follows this sequence:
     * <ol>
     *     <li>The test asks Gradle to apply {@link TestSourceSetsPlugin} when the
     *     {@code java} plugin is applied.</li>
     *     <li>The test starts applying the {@code java-gradle-plugin} plugin.</li>
     *     <li>As part of its setup, {@code java-gradle-plugin} applies the {@code java}
     *     plugin.</li>
     *     <li>Gradle immediately runs the callback from the first step.</li>
     *     <li>The callback applies {@link TestSourceSetsPlugin}. The
     *     {@code java-gradle-plugin} setup is still paused and has not created
     *     {@link GradlePluginDevelopmentExtension} yet.</li>
     * </ol>
     *
     * <p>All calls happen on the same thread. {@link TestSourceSetsPlugin} must wait until
     * {@code java-gradle-plugin} finishes its setup before using the extension.
     */
    @Test
    void canBeAppliedWhileJavaGradlePluginIsBeingApplied() {
        project.getPluginManager().withPlugin("java", ignored ->
            project.getPluginManager().apply(TestSourceSetsPlugin.class)
        );

        assertDoesNotThrow(
            () -> project.getPluginManager().apply("java-gradle-plugin"),
            "TestSourceSetsPlugin should support reentrant application from the Java plugin callback"
        );

        var testSourceSets = getExtension(project, TestSourceSetContainer.class);
        testSourceSets.register("functionalTest").get();

        var gradlePluginDev = getExtension(project, GradlePluginDevelopmentExtension.class);
        assertThat(gradlePluginDev.getTestSourceSets())
            .contains(
                testSourceSets.getByName("test"),
                testSourceSets.getByName("functionalTest")
            );
    }

}
