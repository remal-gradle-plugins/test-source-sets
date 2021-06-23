package name.remal.gradleplugins.testsourcesets.internal;

import static name.remal.gradleplugins.toolkit.ExtensionContainerUtils.getExtension;
import static org.gradle.api.reflect.TypeOf.typeOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradleplugins.testsourcesets.TestTaskNameExtension;
import name.remal.gradleplugins.toolkit.testkit.ApplyPlugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class DefaultTestTaskNameExtensionTest {

    @ApplyPlugin(type = JavaBasePlugin.class)
    private final Project project;

    @Test
    void main() {
        val sourceSet = getExtension(project, SourceSetContainer.class).maybeCreate("main");
        val extension = new DefaultTestTaskNameExtension(sourceSet);
        assertEquals("testMain", extension.getTestTaskName());
    }

    @Test
    void test() {
        val sourceSet = getExtension(project, SourceSetContainer.class).maybeCreate("test");
        val extension = new DefaultTestTaskNameExtension(sourceSet);
        assertEquals("test", extension.getTestTaskName());
    }

    @Test
    void integration() {
        val sourceSet = getExtension(project, SourceSetContainer.class).maybeCreate("integration");
        val extension = new DefaultTestTaskNameExtension(sourceSet);
        assertEquals("testIntegration", extension.getTestTaskName());
    }


    @Test
    void getPublicType() {
        val sourceSet = getExtension(project, SourceSetContainer.class).maybeCreate("main");
        val extension = new DefaultTestTaskNameExtension(sourceSet);
        assertEquals(typeOf(TestTaskNameExtension.class), extension.getPublicType());
    }

}
