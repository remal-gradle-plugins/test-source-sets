package name.remal.gradleplugins.testsourcesets

import static name.remal.gradleplugins.toolkit.PluginManagerUtils.withAnyOfPlugins
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

@Slf4j("logger")
abstract class TestSourceSetsKotlinConfigurer {

    static void configureKotlinTestSourceSets(Project project) {
        List<String> kotlinPlugins = [
            "kotlin",
            "kotlin2js",
            "kotlin-platform-common",
        ]
        withAnyOfPlugins(project.pluginManager, kotlinPlugins) {
            configureKotlinTarget(project)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private static void configureKotlinTarget(Project project) {
        TestSourceSetContainer testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class)
        def kotlin = project.getExtensions().getByName("kotlin")
        NamedDomainObjectContainer kotlinCompilations = kotlin.target.compilations
        testSourceSets.all { testSourceSet ->
            kotlinCompilations.matching(it -> it.getName().equals(testSourceSet.getName())).all { kotlinCompilation ->
                try {
                    kotlinCompilation.associateWith(kotlinCompilations.getByName(MAIN_SOURCE_SET_NAME))
                } catch (MissingMethodException e) {
                    if (e.method == 'associateWith') {
                        logger.debug(e.toString(), e)
                    } else {
                        throw e
                    }
                }
            }
        }
    }


    private TestSourceSetsKotlinConfigurer() {
    }

}
