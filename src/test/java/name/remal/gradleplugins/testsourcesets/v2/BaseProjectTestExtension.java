package name.remal.gradleplugins.testsourcesets.v2;

import static java.nio.file.Files.createTempDirectory;
import static java.util.stream.Collectors.joining;
import static org.codehaus.groovy.runtime.ResourceGroovyMethods.deleteDir;

import java.lang.reflect.Member;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.val;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

@ExtendWith(BaseProjectTestExtension.class)
public class BaseProjectTestExtension implements BeforeEachCallback, AfterEachCallback {

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

}
