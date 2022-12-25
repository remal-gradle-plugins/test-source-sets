package name.remal.gradle_plugins.test_source_sets.internal;

import static org.gradle.api.reflect.TypeOf.typeOf;

import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.test_source_sets.TestTaskNameExtension;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.ApiStatus.Internal;

@RequiredArgsConstructor
@Internal
public class DefaultTestTaskNameExtension implements TestTaskNameExtension, HasPublicType {

    private static final String VERB = "test";

    private final SourceSet sourceSet;

    @Override
    public String getTestTaskName() {
        val name = sourceSet.getName();
        if (VERB.equals(name)) {
            return VERB;
        }
        return sourceSet.getTaskName(VERB, null);
    }

    @Override
    public TypeOf<?> getPublicType() {
        return typeOf(TestTaskNameExtension.class);
    }

}
