package name.remal.gradleplugins.testsourcesets;

import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class IntegrationTestsPlugin implements Plugin<Project> {

    public static final String INTEGRATION_SOURCE_SET_NAME = "integration";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(TestSourceSetsPlugin.class);
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
        testSourceSets.create(INTEGRATION_SOURCE_SET_NAME);
    }

}
