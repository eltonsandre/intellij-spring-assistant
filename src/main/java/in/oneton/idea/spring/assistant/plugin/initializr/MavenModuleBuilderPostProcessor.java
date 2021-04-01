package in.oneton.idea.spring.assistant.plugin.initializr;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.findFileUnderRootInModule;
import static java.util.Collections.singletonList;
import static org.jetbrains.idea.maven.project.MavenProjectsManager.getInstance;

public class MavenModuleBuilderPostProcessor implements ModuleBuilderPostProcessor {
    @Override
    public boolean postProcess(Module module) {
        // TODO: Find a way to use GradleModuleBuilder instead of GradleProjectImportBuilder when adding a child module to the parent
        Project project = module.getProject();
        VirtualFile pomFile = findFileUnderRootInModule(module, "pom.xml");
        if (pomFile == null) { // not a maven project
            return true;
        } else {
            MavenProjectsManager mavenProjectsManager = getInstance(project);
            mavenProjectsManager.addManagedFiles(singletonList(pomFile));
            return false;
        }
    }
}
