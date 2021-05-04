# `name.remal.test-source-sets.v2` plugin

A Gradle plugin that provides `testSourceSet` extension for creating new source sets for testing. For all created source sets a [Test](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html) task is created. All dependencies are inherited from `test` source set.

## `testSourceSet` extension

`testSourceSet` extension is very similar to [`sourceSet` extension on Gradle Java plugin](https://docs.gradle.org/current/userguide/java_plugin.html#source_sets). It can be used in exactly the same way. A new source set `integration` for integration tests can be defined like this:

```groovy
testSourceSet {
  integration
}
```

After executing it, `sourceSets` will contain these source sets:

* `main`
* `test`
* `integration` (this source set is added here, because creation of test source sets is done by delegating it to `sourceSets`)

And `testSourceSet` will contain these source sets:

* `test`
* `integration`

## Test source sets configurations

All configurations of test source sets extend corresponding configurations of `test` source set.

So, after defining `integration` test source set:

```groovy
testSourceSet {
  integration
}
```

... you'll get these configurations:

* `integrationCompileOnly` (extends `testCompileOnly`)
* `integrationImplementation` (extends `testImplementation`)
* `integrationRuntimeOnly` (extends `testRuntimeOnly`)
* And so on. You can find all source set configurations by search for `get*ConfigurationName()` methods of [SourceSet](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/SourceSet.html).

## Kotlin specifics

Internal members of `main` source set **are** accessible in all test source sets. The way it's done is described [here](https://youtrack.jetbrains.com/issue/KT-34901#focus=streamItem-27-3810442.0-0).

# `name.remal.integration-tests.v2` plugin

A Gradle plugin that applies `name.remal.test-source-sets.v2` plugin and creates `integration` test source set.
