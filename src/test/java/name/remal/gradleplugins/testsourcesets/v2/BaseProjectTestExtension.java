package name.remal.gradleplugins.testsourcesets.v2;

import static java.lang.String.format;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.nio.file.Files.createTempDirectory;
import static java.util.stream.Collectors.joining;
import static org.codehaus.groovy.runtime.ResourceGroovyMethods.deleteDir;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Member;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.val;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.util.GradleVersion;
import org.intellij.lang.annotations.Pattern;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

@ExtendWith(BaseProjectTestExtension.class)
public class BaseProjectTestExtension implements BeforeEachCallback, AfterEachCallback, ExecutionCondition {

    private final Queue<File> createdFiles = new LinkedBlockingQueue<>();

    private String dirPrefix = "";

    @SneakyThrows
    protected final File newProjectDirectory() {
        val projectDir = createTempDirectory(dirPrefix).toAbsolutePath().toFile();
        createdFiles.add(projectDir);
        return projectDir;
    }

    protected final Project newProject() {
        val projectDir = newProjectDirectory();
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .withName(projectDir.getName())
            .build();
        return project;
    }


    @Override
    public final void beforeEach(ExtensionContext context) {
        dirPrefix = Stream.of(
            context.getTestClass().map(Class::getName),
            context.getTestMethod().map(Member::getName)
        )
            .map(it -> it.orElse(null))
            .filter(Objects::nonNull)
            .collect(joining("-"));
    }

    @Override
    public final void afterEach(ExtensionContext context) {
        dirPrefix = "";

        while (true) {
            val file = createdFiles.poll();
            if (file == null) {
                break;
            }

            val isExceptionThrown = context.getExecutionException().isPresent();
            if (isExceptionThrown) {
                if (!file.delete()) {
                    deleteDir(file);
                }
            }
        }
    }


    @Target({TYPE, METHOD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    protected @interface MinSupportedGradleVersion {
        @Pattern("\\d+(\\.\\d+)+")
        String value();
    }

    @Override
    public final ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        val annotation = findAnnotation(context.getElement(), MinSupportedGradleVersion.class).orElse(null);
        if (annotation == null) {
            return enabled(format("@%s is not present", MinSupportedGradleVersion.class.getSimpleName()));
        }

        val minGradleVersion = GradleVersion.version(annotation.value());
        val currentGradleVersion = GradleVersion.current();
        if (currentGradleVersion.compareTo(minGradleVersion) >= 0) {
            return enabled(format(
                "current Gradle version %s is greater or equal than min supported version %s",
                currentGradleVersion.getVersion(),
                minGradleVersion.getVersion()
            ));
        } else {
            return disabled(format(
                "current Gradle version %s is less than min supported version %s",
                currentGradleVersion.getVersion(),
                minGradleVersion.getVersion()
            ));
        }
    }


    private static final String CORRESPONDING_KOTLIN_VERSION_PROPERTY = "corresponding-kotlin.version";

    protected String getCorrespondingKotlinVersion() {
        return Optional.ofNullable(System.getProperty(CORRESPONDING_KOTLIN_VERSION_PROPERTY))
            .filter(it -> !it.isEmpty())
            .orElseThrow(() -> new AssertionError(
                CORRESPONDING_KOTLIN_VERSION_PROPERTY + " system property is not set or empty"
            ));
    }

}
