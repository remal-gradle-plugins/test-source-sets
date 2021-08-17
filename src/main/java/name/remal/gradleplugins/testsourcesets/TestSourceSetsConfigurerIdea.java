package name.remal.gradleplugins.testsourcesets;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static name.remal.gradleplugins.toolkit.ExtensionContainerUtils.getExtension;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.val;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;

abstract class TestSourceSetsConfigurerIdea {

    public static void configureIdea(Project project) {
        project.getPluginManager().withPlugin("idea", __ -> {
            val ideaModel = getExtension(project, IdeaModel.class);
            val module = ideaModel.getModule();
            if (module != null) {
                configureIdeaModule(project, module);
            }
        });
    }

    private static void configureIdeaModule(Project project, IdeaModule module) {
        val sourceSets = getExtension(project, SourceSetContainer.class);
        val testSourceSets = getExtension(project, TestSourceSetContainer.class);

        testSourceSets.all(testSourceSet ->
            project.getConfigurations().all(conf -> {
                if (conf.getName().equals(testSourceSet.getCompileClasspathConfigurationName())
                    || conf.getName().equals(testSourceSet.getRuntimeClasspathConfigurationName())
                ) {
                    val testScope = module.getScopes().computeIfAbsent("TEST", key -> new LinkedHashMap<>());
                    val testPlusScope = testScope.computeIfAbsent("plus", key -> new ArrayList<>());
                    testPlusScope.add(conf);
                }
            })
        );

        ConventionMapping moduleConvention = ((IConventionAware) module).getConventionMapping();
        moduleConvention.map("testSourceDirs", (Callable<Set<File>>) () ->
            testSourceSets.stream()
                .flatMap(it -> it.getAllJava().getSrcDirs().stream())
                .collect(toCollection(LinkedHashSet::new))
        );
        moduleConvention.map("testResourceDirs", (Callable<Set<File>>) () ->
            testSourceSets.stream()
                .flatMap(it -> it.getResources().getSrcDirs().stream())
                .collect(toCollection(LinkedHashSet::new))
        );
        moduleConvention.map("singleEntryLibraries", (Callable<Map<String, FileCollection>>) () ->
            sourceSets.stream()
                .filter(it -> it.getName().equals(MAIN_SOURCE_SET_NAME) || testSourceSets.contains(it))
                .collect(toMap(
                    it -> it.getName().equals(MAIN_SOURCE_SET_NAME) ? "RUNTIME" : "TEST",
                    it -> it.getOutput().getDirs(),
                    FileCollection::plus,
                    LinkedHashMap::new
                ))
        );
    }


    private TestSourceSetsConfigurerIdea() {
    }

}
