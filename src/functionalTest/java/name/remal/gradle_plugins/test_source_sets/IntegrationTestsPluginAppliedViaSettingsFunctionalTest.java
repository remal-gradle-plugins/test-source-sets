package name.remal.gradle_plugins.test_source_sets;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class IntegrationTestsPluginAppliedViaSettingsFunctionalTest {

    final GradleProject project;

    @Test
    void appliedViaSettingsIsAppliedToProject() {
        project.forSettingsFile(settings -> settings.applyPlugin("name.remal.integration-tests"));

        // The plugin must NOT be applied via the project's build file: it should reach the project
        // solely through the Settings-level application propagating via GradleLifecycle.beforeProject.
        // The assertion runs at configuration time (not inside a task action), since accessing
        // `project` from a task action at execution time is unsupported with the configuration cache.
        project.getBuildFile().line(
            "assert project.pluginManager.hasPlugin('name.remal.integration-tests')"
        );

        project.assertBuildSuccessfully("help");
    }

}
