package name.remal.gradle_plugins.test_source_sets;

import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.test_source_sets.Utils.classOf;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;
import static name.remal.gradle_plugins.toolkit.PluginManagerUtils.withAnyOfPlugins;
import static name.remal.gradle_plugins.toolkit.reflection.MembersFinder.findMethod;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import java.util.List;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

@CustomLog
@NoArgsConstructor(access = PRIVATE)
abstract class TestSourceSetsConfigurerKotlin {

    public static void configureKotlinTestSourceSets(Project project) {
        var kotlinPlugins = List.of(
            "kotlin",
            "kotlin2js",
            "kotlin-platform-common"
        );
        withAnyOfPlugins(
            project.getPluginManager(),
            kotlinPlugins,
            __ -> configureKotlinTarget(project)
        );
    }

    @SuppressWarnings("unchecked")
    private static void configureKotlinTarget(Project project) {
        var testSourceSets = getExtension(project, TestSourceSetContainer.class);
        var kotlin = getExtension(project, "kotlin");

        final Object target;
        var getTarget = findMethod(classOf(kotlin), Object.class, "getTarget");
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
        var getCompilations = findMethod(
            classOf(target),
            NamedDomainObjectContainer.class,
            "getCompilations"
        );
        if (getCompilations != null) {
            compilations = getCompilations.invoke(target);
            if (compilations == null) {
                logger.warn("kotlin.target.compilations == null");
                return;
            }
        } else {
            logger.info("Method not found: {}.{}()", classOf(target).getName(), "getCompilations");
            return;
        }

        var mainCompilation = compilations.getByName(MAIN_SOURCE_SET_NAME);
        var associateWith = findMethod(
            classOf(mainCompilation),
            "associateWith",
            classOf(mainCompilation)
        );
        if (associateWith == null) {
            logger.info("Method not found: {}.{}()", classOf(mainCompilation).getName(), "associateWith");
            return;
        }

        testSourceSets.configureEach(testSourceSet ->
            compilations
                .matching(it -> it.getName().equals(testSourceSet.getName()))
                .configureEach(compilation ->
                    associateWith.invoke(compilation, mainCompilation)
                )
        );
    }

}
