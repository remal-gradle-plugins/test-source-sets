package name.remal.gradleplugins.testsourcesets;

import static name.remal.gradleplugins.toolkit.ExtensionContainerUtils.getExtension;
import static org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;

import lombok.val;
import org.gradle.api.tasks.SourceSet;

public interface TestTaskNameExtension {

    String getTestTaskName();


    static String getTestTaskName(SourceSet sourceSet) {
        if (sourceSet.getName().equals(TEST_SOURCE_SET_NAME)) {
            return TEST_TASK_NAME;
        }
        val extension = getExtension(sourceSet, TestTaskNameExtension.class);
        return extension.getTestTaskName();
    }

}
