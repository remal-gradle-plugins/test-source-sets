package name.remal.gradleplugins.testsourcesets.v2

import static java.util.Arrays.asList
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

abstract class TestSourceSetsKotlinConfigurer {

    static void configureKotlinTestSourceSets(Project project) {
        AtomicBoolean isConfigured = new AtomicBoolean()
        asList(
            "kotlin",
            "kotlin2js",
            "kotlin-platform-common"
        ).forEach { pluginId ->
            project.getPluginManager().withPlugin(pluginId) {
                if (isConfigured.compareAndSet(false, true)) {
                    configureKotlinTarget(project)
                }
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private static void configureKotlinTarget(Project project) {
        TestSourceSetContainer testSourceSets = project.getExtensions().getByType(TestSourceSetContainer.class)
        def kotlin = project.getExtensions().getByName("kotlin")
        NamedDomainObjectContainer kotlinCompilations = kotlin.target.compilations
        testSourceSets.all { testSourceSet ->
            kotlinCompilations.matching(it -> it.getName().equals(testSourceSet.getName())).all { kotlinCompilation ->
                kotlinCompilation.associateWith(kotlinCompilations.getByName(MAIN_SOURCE_SET_NAME))
            }
        }
    }


    private TestSourceSetsKotlinConfigurer() {
    }

}
