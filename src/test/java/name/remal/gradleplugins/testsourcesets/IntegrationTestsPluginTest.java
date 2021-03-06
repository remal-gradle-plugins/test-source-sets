package name.remal.gradleplugins.testsourcesets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class IntegrationTestsPluginTest {

    private final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(IntegrationTestsPlugin.class);
    }


    @Test
    @DisplayName("'integration' test-source-set is created")
    void integrationTestSourceSetIsCreated() {
        val testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class);
        assertNotNull(testSourceSets.getByName("integration"));
    }

}
