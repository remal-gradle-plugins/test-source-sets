package name.remal.gradle_plugins.test_source_sets;

import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.test_source_sets.Utils.adjustMapProperty;
import static name.remal.gradle_plugins.test_source_sets.Utils.adjustMapSetValue;
import static name.remal.gradle_plugins.test_source_sets.Utils.adjustSetProperty;
import static name.remal.gradle_plugins.toolkit.ExtensionContainerUtils.getExtension;
import static name.remal.gradle_plugins.toolkit.ProjectUtils.afterEvaluateOrNow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import lombok.NoArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.toolkit.IdeaModuleUtils;
import org.gradle.api.Project;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;

@NoArgsConstructor(access = PRIVATE)
abstract class TestSourceSetsConfigurerIdea {

    public static void configureIdea(Project project) {
        project.getPluginManager().withPlugin("idea", __ -> {
            afterEvaluateOrNow(project, ___ -> configureIdeaImpl(project));
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

        testSourceSets.configureEach(testSourceSet -> {
            project.getConfigurations().configureEach(conf -> {
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
                IdeaModuleUtils::getTestSourceDirs,
                IdeaModuleUtils::setTestSourceDirs,
                set -> set.addAll(testSourceSet.getAllJava().getSrcDirs())
            );

            adjustSetProperty(
                module,
                IdeaModuleUtils::getTestResourceDirs,
                IdeaModuleUtils::setTestResourceDirs,
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
