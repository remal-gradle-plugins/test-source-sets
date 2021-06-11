package name.remal.gradleplugins.testsourcesets;

import static java.lang.String.format;
import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.gradle.api.Project.DEFAULT_BUILD_FILE;
import static org.gradle.api.initialization.Settings.DEFAULT_SETTINGS_FILE;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.GradleVersion;

public class BaseFunctionalTestExtension extends BaseProjectTestExtension {

    protected final File projectDir = newProjectDirectory();

    private final File settingsFile = new File(projectDir, DEFAULT_SETTINGS_FILE);
    private final List<Object> settingsFileChunks = new ArrayList<>();

    private final File buildFile = new File(projectDir, DEFAULT_BUILD_FILE);
    private final AppliedPlugins appliedPlugins = new AppliedPlugins();
    private final List<Object> buildFileChunks = new ArrayList<>();

    {
        settingsFileChunks.add("rootProject.name = '" + projectDir.getName() + "'");
        buildFileChunks.add(appliedPlugins);
    }

    protected final void applyPlugin(String pluginId) {
        appliedPlugins.add(pluginId);
    }

    protected final void applyPlugin(String pluginId, String pluginVersion) {
        appliedPlugins.add(pluginId, pluginVersion);
    }

    @SneakyThrows
    protected final void appendToSettingsFile(CharSequence... contentParts) {
        settingsFileChunks.addAll(asList(contentParts));
    }

    @SneakyThrows
    protected final void appendToBuildFile(CharSequence... contentParts) {
        buildFileChunks.addAll(asList(contentParts));
    }


    private static final boolean IS_IN_DEBUG = getRuntimeMXBean().getInputArguments().toString().contains("jdwp");
    private static final Pattern TRIM_RIGHT = Pattern.compile("\\s+$");
    private static final Pattern STACK_TRACE_LINE = Pattern.compile("^\\s+at ");

    private static final List<String> DEPRECATION_MESSAGES = unmodifiableList(asList(
        "has been deprecated and is scheduled to be removed in Gradle",
        "Deprecated Gradle features were used in this build",
        "This is scheduled to be removed in Gradle",
        "This will fail with an error in Gradle"
    ));

    private static final List<SuppressedDeprecation> SUPPRESSED_DEPRECATIONS = unmodifiableList(asList(
        new SuppressedDeprecation(
            "The DefaultSourceDirectorySet constructor has been deprecated",
            "org.jetbrains.kotlin.gradle.plugin."
        ),
        new SuppressedDeprecation(
            "Classpath configuration has been deprecated for dependency declaration",
            "org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilation."
        ),
        new SuppressedDeprecation(
            "Internal API constructor TaskReportContainer(Class<T>, Task) has been deprecated",
            "com.github.spotbugs."
        )
    ));

    private static final List<String> MUTABLE_PROJECT_STATE_WARNING_MESSAGES = unmodifiableList(asList(
        "was resolved without accessing the project in a safe manner",
        "This may happen when a configuration is resolved from a thread not managed by Gradle"
    ));

    @SneakyThrows
    protected final void assertBuildSuccessfully() {
        createDirectories(requireNonNull(settingsFile.getParentFile()).toPath());
        write(settingsFile.toPath(), settingsFileChunks.stream()
            .map(Object::toString)
            .collect(joining("\n"))
            .getBytes(UTF_8)
        );

        buildFileChunks.add("if (project.defaultTasks.isEmpty()) "
            + "project.defaultTasks(tasks.create('_defaultEmptyTask').name)"
        );
        createDirectories(requireNonNull(buildFile.getParentFile()).toPath());
        write(buildFile.toPath(), buildFileChunks.stream()
            .map(Object::toString)
            .collect(joining("\n"))
            .getBytes(UTF_8)
        );

        val runner = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withDebug(IS_IN_DEBUG)
            .withArguments("--stacktrace", "--warning-mode=all");

        String gradleDistribMirror = System.getenv("GRADLE_DISTRIBUTIONS_MIRROR");
        if (gradleDistribMirror == null || gradleDistribMirror.isEmpty()) {
            runner.withGradleVersion(GradleVersion.current().getVersion());
        } else {
            while (gradleDistribMirror.endsWith("/")) {
                gradleDistribMirror = gradleDistribMirror.substring(0, gradleDistribMirror.length() - 1);
            }
            val distributionUri = new URI(gradleDistribMirror + format(
                "/gradle-%s-bin.zip",
                GradleVersion.current().getVersion()
            ));
            runner.withGradleDistribution(distributionUri);
        }

        val buildResult = runner.build();

        val output = buildResult.getOutput();
        List<String> outputLines = Stream.of(output.split("\n"))
            .map(it -> TRIM_RIGHT.matcher(it).replaceFirst(""))
            .filter(it -> !it.isEmpty())
            .collect(toList());

        {
            Collection<String> deprecations = new LinkedHashSet<>();
            forEachLine:
            for (int lineIndex = 0; lineIndex < outputLines.size(); ++lineIndex) {
                val line = outputLines.get(lineIndex);
                val hasWarning = DEPRECATION_MESSAGES.stream().anyMatch(line::contains);
                if (!hasWarning) {
                    continue;
                }

                for (val suppressedDeprecation : SUPPRESSED_DEPRECATIONS) {
                    if (line.contains(suppressedDeprecation.getMessage())) {
                        for (int i = lineIndex + 1; i < outputLines.size(); ++i) {
                            val stackTraceLine = outputLines.get(i);
                            if (!STACK_TRACE_LINE.matcher(stackTraceLine).find()) {
                                break;
                            }
                            val stackTracePackagePrefix = suppressedDeprecation.getStackTracePackagePrefix();
                            if (stackTracePackagePrefix != null
                                && !stackTracePackagePrefix.isEmpty()
                                && stackTraceLine.contains(stackTracePackagePrefix)
                            ) {
                                continue forEachLine;
                            }
                        }
                    }
                }

                deprecations.add(line);
            }
            if (!deprecations.isEmpty()) {
                val sb = new StringBuilder();
                sb.append("Deprecation warnings were found:");
                deprecations.forEach(it -> sb.append("\n  * ").append(it));
                throw new AssertionError(sb.toString());
            }
        }

        {
            Collection<String> mutableProjectStateWarnings = new LinkedHashSet<>();
            for (final String line : outputLines) {
                val hasWarning = MUTABLE_PROJECT_STATE_WARNING_MESSAGES.stream().anyMatch(line::contains);
                if (hasWarning) {
                    mutableProjectStateWarnings.add(line);
                }
            }
            if (!mutableProjectStateWarnings.isEmpty()) {
                val sb = new StringBuilder();
                sb.append("Mutable Project State warnings were found:");
                mutableProjectStateWarnings.forEach(it -> sb.append("\n  * ").append(it));
                throw new AssertionError(sb.toString());
            }
        }
    }


    private static final class AppliedPlugins {
        private final Map<String, String> pluginToVersion = new LinkedHashMap<>();

        public void add(String pluginId) {
            pluginToVersion.putIfAbsent(pluginId, null);
        }

        public void add(String pluginId, String pluginVersion) {
            pluginToVersion.put(pluginId, pluginVersion);
        }

        @Override
        public String toString() {
            val sb = new StringBuilder();
            sb.append("plugins {");
            pluginToVersion.forEach((pluginId, pluginVersion) -> {
                sb.append("\n    id '").append(pluginId).append("'");
                if (pluginVersion != null && !pluginVersion.isEmpty()) {
                    sb.append(" version '").append(pluginVersion).append("'");
                }
            });
            sb.append("\n}");
            return sb.toString();
        }
    }


    @Value
    @RequiredArgsConstructor
    private static class SuppressedDeprecation {
        String message;
        @Nullable
        String stackTracePackagePrefix;
    }

}
