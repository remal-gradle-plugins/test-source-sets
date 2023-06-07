package name.remal.gradle_plugins.test_source_sets;

public enum TestSuffixCheckMode {

    /**
     * Throw an exception
     */
    FAIL,

    /**
     * Log a warning message
     */
    WARN,

    /**
     * Disable the check
     */
    DISABLE,

}
