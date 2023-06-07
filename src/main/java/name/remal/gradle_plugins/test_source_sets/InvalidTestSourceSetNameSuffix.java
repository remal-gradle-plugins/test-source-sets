package name.remal.gradle_plugins.test_source_sets;

import org.gradle.api.GradleException;

public class InvalidTestSourceSetNameSuffix extends GradleException {

    InvalidTestSourceSetNameSuffix(String message) {
        super(message);
    }

}
