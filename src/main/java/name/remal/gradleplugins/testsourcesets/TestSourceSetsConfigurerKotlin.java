package name.remal.gradleplugins.testsourcesets;

import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradleplugins.testsourcesets.Utils.classOf;
import static name.remal.gradleplugins.toolkit.ExtensionContainerUtils.getExtension;
import static name.remal.gradleplugins.toolkit.PluginManagerUtils.withAnyOfPlugins;
import static name.remal.gradleplugins.toolkit.reflection.MembersFinder.findMethod;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import lombok.CustomLog;
import lombok.NoArgsConstructor;
import lombok.val;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

@CustomLog
@NoArgsConstructor(access = PRIVATE)
abstract class TestSourceSetsConfigurerKotlin {

    public static void configureKotlinTestSourceSets(Project project) {
        val kotlinPlugins = asList(
            "kotlin",
            "kotlin2js",
            "kotlin-platform-common"
        );
        withAnyOfPlugins(project.getPluginManager(), kotlinPlugins, __ ->
            configureKotlinTarget(project)
        );
    }

    @SuppressWarnings("unchecked")
    private static void configureKotlinTarget(Project project) {
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);
        val kotlin = getExtension(project, "kotlin");

        final Object target;
        val getTarget = findMethod(classOf(kotlin), Object.class, "getTarget");
        if (getTarget != null) {
            target = getTarget.invoke(kotlin);
            if (target == null) {
                logger.warn("kotlin.target == null");
                return;
            }
        } else {
            logger.info("Method not found: {}.{}()", classOf(kotlin).getName(), "getTarget");
            return;
        }

        final NamedDomainObjectContainer<Named> compilations;
        val getCompilations = findMethod(
            classOf(target),
            NamedDomainObjectContainer.class,
            "getCompilations"
        );
        if (getCompilations != null) {
            compilations = (NamedDomainObjectContainer<Named>) getCompilations.invoke(target);
            if (compilations == null) {
                logger.warn("kotlin.target.compilations == null");
                return;
            }
        } else {
            logger.info("Method not found: {}.{}()", classOf(target).getName(), "getCompilations");
            return;
        }

        val mainCompilation = compilations.getByName(MAIN_SOURCE_SET_NAME);
        val associateWith = findMethod(
            classOf(mainCompilation),
            "associateWith",
            classOf(mainCompilation)
        );
        if (associateWith == null) {
            logger.info("Method not found: {}.{}()", classOf(mainCompilation).getName(), "associateWith");
            return;
        }

        testSourceSets.all(testSourceSet ->
            compilations.matching(it -> it.getName().equals(testSourceSet.getName())).all(compilation ->
                associateWith.invoke(compilation, mainCompilation)
            )
        );
    }

}
