package name.remal.gradleplugins.testsourcesets;

import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradleplugins.testsourcesets.Utils.adjustMapProperty;
import static name.remal.gradleplugins.testsourcesets.Utils.adjustMapSetValue;
import static name.remal.gradleplugins.testsourcesets.Utils.adjustSetProperty;
import static name.remal.gradleplugins.toolkit.ExtensionContainerUtils.getExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import lombok.NoArgsConstructor;
import lombok.val;
import org.gradle.api.Project;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;

@NoArgsConstructor(access = PRIVATE)
abstract class TestSourceSetsConfigurerIdea {

    public static void configureIdea(Project project) {
        project.getPluginManager().withPlugin("idea", __ -> {
            if (project.getState().getExecuted()) {
                configureIdeaImpl(project);
            } else {
                project.afterEvaluate(___ -> {
                    configureIdeaImpl(project);
                });
            }
        });
    }

    private static void configureIdeaImpl(Project project) {
        val ideaModel = getExtension(project, IdeaModel.class);
        val module = ideaModel.getModule();
        if (module != null) {
            configureIdeaModule(project, module);
        }
    }

    private static void configureIdeaModule(Project project, IdeaModule module) {
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        testSourceSets.all(testSourceSet -> {
            project.getConfigurations().all(conf -> {
                if (conf.getName().equals(testSourceSet.getCompileClasspathConfigurationName())
                    || conf.getName().equals(testSourceSet.getRuntimeClasspathConfigurationName())
                ) {
                    if (module.getScopes() == null) {
                        module.setScopes(new LinkedHashMap<>());
                    }
                    val testScope = module.getScopes().computeIfAbsent("TEST", key -> new LinkedHashMap<>());
                    val testPlusScope = testScope.computeIfAbsent("plus", key -> new ArrayList<>());
                    testPlusScope.add(conf);
                }
            });


            adjustSetProperty(
                module,
                IdeaModule::getSourceDirs,
                IdeaModule::setSourceDirs,
                set -> testSourceSet.getAllJava().getSrcDirs().forEach(set::remove)
            );

            adjustSetProperty(
                module,
                IdeaModule::getResourceDirs,
                IdeaModule::setResourceDirs,
                set -> testSourceSet.getResources().getSrcDirs().forEach(set::remove)
            );


            adjustSetProperty(
                module,
                IdeaModule::getTestSourceDirs,
                IdeaModule::setTestSourceDirs,
                set -> set.addAll(testSourceSet.getAllJava().getSrcDirs())
            );

            adjustSetProperty(
                module,
                IdeaModule::getTestResourceDirs,
                IdeaModule::setTestResourceDirs,
                set -> set.addAll(testSourceSet.getResources().getSrcDirs())
            );


            adjustMapProperty(
                module,
                IdeaModule::getSingleEntryLibraries,
                IdeaModule::setSingleEntryLibraries,
                map -> {
                    adjustMapSetValue(
                        map,
                        "RUNTIME",
                        set -> testSourceSet.getOutput().getDirs().forEach(set::remove)
                    );

                    adjustMapSetValue(
                        map,
                        "TEST",
                        set -> testSourceSet.getOutput().getDirs().forEach(set::add)
                    );
                }
            );
        });
    }

}
