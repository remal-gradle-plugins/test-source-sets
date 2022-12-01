**Min supported Gradle version: <!--property:gradle-api.min-version-->6.0<!--/property-->**

# `name.remal.test-source-sets` plugin

![configuration% cache: supported](https://img.shields.io/static/v1?label=configuration%20cache&message=supported&color=success)

A Gradle plugin that provides `testSourceSets` extension for creating new source sets for testing. For all created source sets a [Test](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html) task is created. All dependencies are inherited from `test` source set.

## `testSourceSets` extension

`testSourceSets` extension is very similar to [`sourceSet` extension on Gradle Java plugin](https://docs.gradle.org/current/userguide/java_plugin.html#source_sets). It can be used in exactly the same way. A new source set `integration` for integration tests can be defined like this:

```groovy
testSourceSets {
  integration
}
```

After executing it, `sourceSets` will contain these source sets:

* `main`
* `test`
* `integration` (this source set is added here, because creation of test source sets is done by delegating it to `sourceSet` extension)

And `testSourceSets` will contain these source sets:

* `test`
* `integration`

## Test source sets configurations

All configurations of test source sets extend corresponding configurations of `test` source set.

So, after defining `integration` test source set:

```groovy
testSourceSets {
  integration
}
```

... you'll get these configurations:

* `integrationCompileOnly` (extends `testCompileOnly`)
* `integrationImplementation` (extends `testImplementation`)
* `integrationRuntimeOnly` (extends `testRuntimeOnly`)
* And so on. You can find all source set configurations by search for `get*ConfigurationName()` methods of [SourceSet](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/SourceSet.html).

## [Test](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html) tasks creation

[Test](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html) task is created by the plugin for each test source set.

These [Test](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html) tasks do **not** inherit main `test` task settings, so you need to configure it explicitly, or by using `withType()`:

```groovy
tasks.withType(Test).configureEach {
  useJUnitPlatform()
}
```

## `allTests` task

A task named `allTests` is created by the plugin. This task simply depends on [Test](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html) task of each test source set.

## Test task name extension

A special extension is added to all test source set, that provides `getTestTaskName()` method. This method can be used like this:

```groovy
testSourceSets.configureEach { sourceSet ->
  String testTaskName = sourceSet.testTaskName
  println testTaskName // print corresponding Test task name
}
```

## Kotlin specifics

Internal members of `main` source set **are** accessible in all test source sets. It works for Kotlin Gradle plugin >=1.3.60. The way it's done is described [here](https://youtrack.jetbrains.com/issue/KT-34901#focus=streamItem-27-3810442.0-0).

# `name.remal.integration-tests` plugin

![configuration% cache: supported](https://img.shields.io/static/v1?label=configuration%20cache&message=supported&color=success)

A Gradle plugin that applies `name.remal.test-source-sets` plugin and creates `integration` test source set.
