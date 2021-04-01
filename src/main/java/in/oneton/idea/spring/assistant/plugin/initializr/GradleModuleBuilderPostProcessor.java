package in.oneton.idea.spring.assistant.plugin.initializr;

import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.projectImport.ProjectImportProvider;

import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.findFileUnderRootInModule;

public class GradleModuleBuilderPostProcessor implements ModuleBuilderPostProcessor {

    /**
     * TODO: Find a way to use GradleModuleBuilder instead of GradleProjectImportBuilder when adding a child module to the parent
     */
    @Override
    public boolean postProcess(final Module module) {
        final VirtualFile gradleFile = findFileUnderRootInModule(module, "build.gradle");

        if (gradleFile == null) {// not a gradle project
            return true;
        }

        final ProjectImportBuilder<?> importBuilder = ProjectImportBuilder.EXTENSIONS_POINT_NAME.getExtensions()[0];
        final ProjectImportProvider importProvider = ProjectImportProvider.PROJECT_IMPORT_PROVIDER.getExtensions()[0];

        final AddModuleWizard addModuleWizard = new AddModuleWizard(module.getProject(), gradleFile.getPath(), importProvider);

        if (addModuleWizard.getStepCount() > 0 && !addModuleWizard.showAndGet()) { // user has cancelled import project prompt
            return true;
        }

        // user chose to import via the gradle import prompt
        importBuilder.commit(module.getProject(), null, null);

        return false;
    }

}
