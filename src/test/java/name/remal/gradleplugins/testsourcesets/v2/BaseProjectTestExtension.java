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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Member;
import java.util.Objects;
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

    private final Queue<Project> createdProjects = new LinkedBlockingQueue<>();

    private String dirPrefix = "";

    @SneakyThrows
    protected Project newProject() {
        val projectDir = createTempDirectory(dirPrefix).toFile();
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .withName(projectDir.getName())
            .build();

        createdProjects.add(project);

        return project;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        dirPrefix = Stream.of(
            context.getTestClass().map(Class::getName),
            context.getTestMethod().map(Member::getName)
        )
            .map(it -> it.orElse(null))
            .filter(Objects::nonNull)
            .collect(joining("-"));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        dirPrefix = "";

        while (true) {
            val project = createdProjects.poll();
            if (project == null) {
                break;
            }

            val isExceptionThrown = context.getExecutionException().isPresent();
            if (isExceptionThrown) {
                val projectDir = project.getProjectDir();
                if (!projectDir.delete()) {
                    deleteDir(projectDir);
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
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
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

}
