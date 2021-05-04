package name.remal.gradleplugins.testsourcesets.v2;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import lombok.val;
import org.gradle.api.Project;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IntegrationTestsPluginTest extends BaseProjectTestExtension {

    private final Project project = newProject();

    {
        project.getPluginManager().apply(IntegrationTestsPlugin.class);
    }

    @Test
    @DisplayName("'integration' test-source-set is created")
    void integrationTestSourceSetIsCreated() {
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
        assertNotNull(testSourceSets.getByName("integration"));
    }

}
