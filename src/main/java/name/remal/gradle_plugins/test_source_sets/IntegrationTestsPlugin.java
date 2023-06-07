package name.remal.gradle_plugins.test_source_sets;

import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;

import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class IntegrationTestsPlugin implements Plugin<Project> {

    public static final String INTEGRATION_SOURCE_SET_NAME = doNotInline("integrationTest");

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(TestSourceSetsPlugin.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
        testSourceSets.create(INTEGRATION_SOURCE_SET_NAME);
    }

}
