package in.oneton.idea.spring.assistant.plugin.initializr;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleNameLocationSettings;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.pom.java.LanguageLevel;
import in.oneton.idea.spring.assistant.plugin.initializr.step.DependencySelectionStep;
import in.oneton.idea.spring.assistant.plugin.initializr.step.ProjectDetailsStep;
import in.oneton.idea.spring.assistant.plugin.initializr.step.ServerSelectionStep;
import in.oneton.idea.spring.assistant.plugin.misc.Icons;
import lombok.EqualsAndHashCode;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static com.intellij.openapi.application.ModalityState.current;
import static com.intellij.openapi.module.StdModuleTypes.JAVA;
import static com.intellij.openapi.projectRoots.JavaSdk.getInstance;
import static com.intellij.openapi.projectRoots.JavaSdkVersion.fromLanguageLevel;
import static com.intellij.openapi.roots.ProjectRootManager.getInstance;
import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.intellij.pom.java.LanguageLevel.parse;

@EqualsAndHashCode(callSuper = true)
public class InitializrModuleBuilder extends ModuleBuilder {

    public static final String CONTENT_TYPE = "application/vnd.initializr.v2.1+json";

    private ProjectCreationRequest request;

    @Override
    public void setupRootModel(final @NotNull ModifiableRootModel modifiableRootModel) {
        final Sdk moduleOrProjectSdk = this.getModuleJdk() != null ?
                this.getModuleJdk() :
                getInstance(modifiableRootModel.getProject()).getProjectSdk();
        if (moduleOrProjectSdk != null) {
            modifiableRootModel.setSdk(moduleOrProjectSdk);
        }

        final LanguageLevelModuleExtension languageLevelModuleExtension =
                modifiableRootModel.getModuleExtension(LanguageLevelModuleExtension.class);
        if (languageLevelModuleExtension != null && moduleOrProjectSdk != null) {
            if (this.safeGetProjectCreationRequest().isJavaVersionSet()) {
                final LanguageLevel lastSelectedLanguageLevel =
                        parse(this.safeGetProjectCreationRequest().getJavaVersion().getId());
                if (lastSelectedLanguageLevel != null) {
                    final JavaSdkVersion lastSelectedJavaSdkVersion = fromLanguageLevel(lastSelectedLanguageLevel);
                    final JavaSdkVersion moduleOrProjectLevelJavaSdkVersion =
                            getInstance().getVersion(moduleOrProjectSdk);
                    if (moduleOrProjectLevelJavaSdkVersion != null && moduleOrProjectLevelJavaSdkVersion
                            .isAtLeast(lastSelectedJavaSdkVersion)) {
                        languageLevelModuleExtension.setLanguageLevel(lastSelectedLanguageLevel);
                    }
                }
            }
        }

        this.doAddContentEntry(modifiableRootModel);
    }

    @Override
    @Nullable
    public ModuleWizardStep modifySettingsStep(@NotNull final SettingsStep settingsStep) {
        ModuleNameLocationSettings moduleNameLocationSettings = settingsStep.getModuleNameLocationSettings();
        if (moduleNameLocationSettings != null) {
            moduleNameLocationSettings.setModuleName(this.request.getArtifactId());
        }
        return super.modifySettingsStep(settingsStep);
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull final WizardContext wizardContext,
                                                @NotNull final ModulesProvider modulesProvider) {
        return new ModuleWizardStep[]{new ProjectDetailsStep(this, wizardContext),
                new DependencySelectionStep(this)};
    }

    @Override
    @Nullable
    public ModuleWizardStep getCustomOptionsStep(final WizardContext context, final Disposable parentDisposable) {
        return new ServerSelectionStep(this);
    }

    @NotNull
    @Override
    public Module createModule(@NotNull final ModifiableModuleModel moduleModel)
            throws IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {

        final Module module = super.createModule(moduleModel);

        getApplication().invokeLater(() -> {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                try {
                    final InitializerDownloader downloader = new InitializerDownloader(this);
                    downloader.execute(ProgressManager.getInstance().getProgressIndicator());
                } catch (final IOException var2) {
                    getApplication().invokeLater(() -> showErrorDialog("Error: " + var2.getMessage(), "Creation Failed"));
                }
            }, "Downloading required files...", true, null);
            final ModuleBuilderPostProcessor[] postProcessors =
                    ModuleBuilderPostProcessor.EXTENSION_POINT_NAME.getExtensions();
            for (final ModuleBuilderPostProcessor postProcessor : postProcessors) {
                if (!postProcessor.postProcess(module)) {
                    return;
                }
            }
        }, current());
        return module;
    }

    @Override
    public Icon getNodeIcon() {
        return Icons.SpringBoot;
    }

    @Nullable
    @Override
    public String getBuilderId() {
        return "Spring Boot/Cloud Dataflow Initializr";
    }

    @Override
    public String getDescription() {
        return "Bootstrap spring applications using <b>spring boot</b> & <b>spring cloud dataflow</b> starters";
    }

    @Override
    public String getPresentableName() {
        return "Spring Assistant";
    }

    @Override
    public String getParentGroup() {
        return "Build Tools";
    }

    @Override
    public ModuleType<JavaModuleBuilder> getModuleType() {
        return JAVA;
    }

    public ProjectCreationRequest safeGetProjectCreationRequest() {
        if (this.request == null) {
            this.request = new ProjectCreationRequest();
        }
        return this.request;
    }
}
