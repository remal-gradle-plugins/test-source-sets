package name.remal.gradle_plugins.test_source_sets;

import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;
import static org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;

import java.util.concurrent.Callable;
import lombok.val;
import org.gradle.api.tasks.SourceSet;

@FunctionalInterface
public interface TestTaskNameGetter extends Callable<String> {

    @Override
    String call();


    static String getTestTaskName(SourceSet sourceSet) {
        if (sourceSet.getName().equals(TEST_SOURCE_SET_NAME)) {
            return TEST_TASK_NAME;
        }

        val extension = getExtension(sourceSet, TestTaskNameGetter.class);
        return extension.call();
    }

}
