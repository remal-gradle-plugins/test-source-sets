package name.remal.gradle_plugins.test_source_sets;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;
import org.gradle.api.tasks.SourceSet;

@NoArgsConstructor(access = PRIVATE)
abstract class TestTaskNameUtils {

    public static String getTestTaskName(SourceSet sourceSet) {
        return sourceSet.getName();
    }

}
