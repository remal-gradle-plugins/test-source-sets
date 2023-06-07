package name.remal.gradle_plugins.test_source_sets;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;

public interface TestSourceSetContainer extends NamedDomainObjectContainer<SourceSet> {

    Property<TestSuffixCheckMode> getTestSuffixCheck();

}
