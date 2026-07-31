package name.remal.gradle_plugins.test_source_sets;

import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;

import name.remal.gradle_plugins.toolkit.AbstractSettingsAwarePlugin;
import org.gradle.api.Project;

public class IntegrationTestsPlugin extends AbstractSettingsAwarePlugin {

    public static final String INTEGRATION_SOURCE_SET_NAME = doNotInline("integrationTest");

    @Override
    protected void applyToProject(Project project) {
        project.getPluginManager().apply(TestSourceSetsPlugin.class);
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);
        testSourceSets.create(INTEGRATION_SOURCE_SET_NAME);
    }

}
