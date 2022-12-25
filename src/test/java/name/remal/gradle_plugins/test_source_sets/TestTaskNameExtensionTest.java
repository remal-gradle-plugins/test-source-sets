package name.remal.gradle_plugins.test_source_sets;

import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.addExtension;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.test_source_sets.internal.DefaultTestTaskNameExtension;
import name.remal.gradle_plugins.toolkit.testkit.ApplyPlugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class TestTaskNameExtensionTest {

    @ApplyPlugin(type = JavaBasePlugin.class)
    private final Project project;

    @Test
    void source_set_without_extension() {
        val sourceSet = getExtension(project, SourceSetContainer.class).maybeCreate("main");
        assertThrows(UnknownDomainObjectException.class, () -> TestTaskNameExtension.getTestTaskName(sourceSet));
    }

    @Test
    void source_set_without_extension_but_with_name_test() {
        val sourceSet = getExtension(project, SourceSetContainer.class).maybeCreate("test");
        assertEquals("test", TestTaskNameExtension.getTestTaskName(sourceSet));
    }

    @Test
    void source_set_with_extension() {
        val sourceSet = getExtension(project, SourceSetContainer.class).maybeCreate("main");
        val extension = addExtension(sourceSet, new DefaultTestTaskNameExtension(sourceSet));
        assertEquals(extension.getTestTaskName(), TestTaskNameExtension.getTestTaskName(sourceSet));
    }

}
