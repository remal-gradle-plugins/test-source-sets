**Tested on Java LTS versions from <!--property:java-runtime.min-version-->11<!--/property--> to <!--property:java-runtime.max-version-->21<!--/property-->.**

**Tested on Gradle versions from <!--property:gradle-api.min-version-->7.0<!--/property--> to <!--property:gradle-api.max-version-->9.0.0-rc-2<!--/property-->.**

# `name.remal.test-source-sets` plugin

[![configuration cache: supported](https://img.shields.io/static/v1?label=configuration%20cache&message=supported&color=success)](https://docs.gradle.org/current/userguide/configuration_cache.html)

Usage:

<!--plugin-usage:name.remal.test-source-sets-->
```groovy
plugins {
    id 'name.remal.test-source-sets' version '5.0.2'
}
```
<!--/plugin-usage-->

&nbsp;

A Gradle plugin that provides `testSourceSets` extension for creating new source sets for testing. For all created source sets a [Test](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html) task is created. All dependencies are inherited from `test` source set.

## `testSourceSets` extension

`testSourceSets` extension is very similar to [`sourceSet` extension on Gradle Java plugin](https://docs.gradle.org/current/userguide/java_plugin.html#source_sets). It can be used in exactly the same way. A new source set `integrationTest` for integration tests can be defined like this:

```groovy
testSourceSets {
  integrationTest
}
```

After executing it, `sourceSets` will contain these source sets:

* `main`
* `test`
* `integrationTest` (this source set is added here, because creation of test source sets is done by delegating it to `sourceSet` extension)

And `testSourceSets` will contain these source sets:

* `test`
* `integrationTest`

## Test source sets configurations

All configurations of test source sets extend corresponding configurations of `test` source set.

So, after defining `integrationTest` test source set:

```groovy
testSourceSets {
  integrationTest
}
```

... you'll get these configurations:

* `integrationTestCompileOnly` (extends `testCompileOnly`)
* `integrationTestImplementation` (extends `testImplementation`)
* `integrationTestRuntimeOnly` (extends `testRuntimeOnly`)
* And so on. You can find all source set configurations by search for `get*ConfigurationName()` methods of [SourceSet](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/SourceSet.html).

## [Test](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html) tasks creation

[Test](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html) task is created by the plugin for each test source set.

These [Test](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html) tasks do **not** inherit main `test` task settings, so you need to configure it explicitly, or by using `withType()`:

```groovy
tasks.withType(Test).configureEach {
  useJUnitPlatform()
}
```

## Integration with JVM Test Suite plugin

Starting from Gradle 7.3, this plugin applies [JVM Test Suite plugin](https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html).

Creating a new test source set, creates a new JVM test suite.

### Test source set name suffix check

Gradle authors believe that it's important to distinguish test sources from production sources. So, it's expected that a source set for tests ends with `Test`. See [an issue about naming](https://github.com/gradle/gradle/issues/25223).

JVM Test Suite plugin creates a Test task named the same, as the corresponding test source set. So, by having `integration` test source set, we'll have `integration` Test task. Which is confusing.

This plugin checks that all test source set names end with `Test`. By default, an exception is thrown if it's not so.

This check can be configured:

```groovy
testSourceSets {
  testSuffixCheck = 'FAIL' // to throw an exception, default
  testSuffixCheck = 'WARN' // to log a warning message
  testSuffixCheck = 'DISABLE' // to disable the check
}
```

## `allTests` task

A task named `allTests` is created by the plugin. This task simply depends on [Test](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html) task of each test source set.

## `testTask` extension for all test source sets

Every test source set (including default `test`) has `testTask` extension.
This extension has type `TaskProvider<Test>`
and it's a provider of corresponding `Test` task for the current test source set.

It can be used like this:

```groovy
testSourceSets.configureEach {
  println testTask.name // prints corresponding name of `Test` task for this source set
}
```

## Kotlin specifics

Internal members of `main` source set **are** accessible in all test source sets. It works for Kotlin Gradle plugin >=1.3.60. The way it's done is described [here](https://youtrack.jetbrains.com/issue/KT-34901#focus=streamItem-27-3810442.0-0).

# `name.remal.integration-tests` plugin

[![configuration cache: supported](https://img.shields.io/static/v1?label=configuration%20cache&message=supported&color=success)](https://docs.gradle.org/current/userguide/configuration_cache.html)

Usage:

<!--plugin-usage:name.remal.integration-tests-->
```groovy
plugins {
    id 'name.remal.integration-tests' version '5.0.2'
}
```
<!--/plugin-usage-->

&nbsp;

A Gradle plugin that applies `name.remal.test-source-sets` plugin and creates `integrationTest` test source set.

# Migration guide

## Version 4.* to 5.*

The minimum Java version is 11 (from 8).
The minimum Gradle version is 7.0 (from 6.0).

## Version 3.* to 4.*

`testTaskName` convention property is not added anymore to each test source set. Test tasks now have their names equal to test source set names.

`name.remal.integration-tests` plugin creates `integrationTest` test source set instead of `integration`.

## Version 2.* to 3.*

Package name was changed from `name.remal.gradleplugins.testsourcesets` to `name.remal.gradle_plugins.test_source_sets`.
