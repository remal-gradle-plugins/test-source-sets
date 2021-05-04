# `name.remal.test-source-sets.v2` plugin

A Gradle plugin that provides `testSourceSet` extension for creating new source sets for testing. For all created source sets a [Test](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html) task is created. All dependencies are inherited from `test` source set.

## `testSourceSet` extension

`testSourceSet` extension is very similar to [`sourceSet` extension on Gradle Java plugin](https://docs.gradle.org/current/userguide/java_plugin.html#source_sets). It can be used in exactly the same way. A new source set `integration` for integration tests can be defined like this:

```groovy
testSourceSet {
    integration
}
```
