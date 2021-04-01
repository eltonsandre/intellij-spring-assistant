package in.oneton.idea.spring.assistant.plugin.suggestion.component;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.getExternalRootProjectPath;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.moduleNamesAsStrCommaDelimited;
import static java.util.Arrays.stream;

/**
 * Callback that gets invoked by gradle as soon as the project is imported successfully
 */
@Order(5000)
public class GradleReindexingProjectDataService
        extends AbstractProjectDataService<ModuleData, Void> {

    private static final Logger log = Logger.getInstance(GradleReindexingProjectDataService.class);

    @NotNull
    @Override
    public Key<ModuleData> getTargetDataKey() {
        return ProjectKeys.MODULE;
    }

    @Override
    public void onSuccessImport(@NotNull Collection<DataNode<ModuleData>> imported,
                                @Nullable ProjectData projectData, @NotNull Project project,
                                @NotNull IdeModelsProvider modelsProvider) {
        if (projectData != null) {
            debug(() -> log.debug(
                    "Gradle dependencies are updated for project, will trigger indexing via dumbservice for project "
                            + project.getName()));
            DumbService.getInstance(project).smartInvokeLater(() -> {
                log.debug("Will attempt to trigger indexing for project " + project.getName());
                SuggestionService service = ServiceManager.getService(project, SuggestionService.class);

                try {
                    Module[] validModules = stream(modelsProvider.getModules()).filter(module -> {
                        String externalRootProjectPath = getExternalRootProjectPath(module);
                        return externalRootProjectPath != null && externalRootProjectPath
                                .equals(projectData.getLinkedExternalProjectPath());
                    }).toArray(Module[]::new);

                    if (validModules.length > 0) {
                        service.reindex(project, validModules);
                    } else {
                        debug(() -> log.debug(
                                "None of the modules " + moduleNamesAsStrCommaDelimited(modelsProvider.getModules(),
                                        true) + " are relevant for indexing, skipping for project " + project
                                        .getName()));
                    }
                } catch (Throwable e) {
                    log.error("Error occurred while indexing project " + project.getName() + " & modules "
                            + moduleNamesAsStrCommaDelimited(modelsProvider.getModules(), false), e);
                }
            });
        }
    }

    /**
     * Debug logging can be enabled by adding fully classified class name/package name with # prefix
     * For eg., to enable debug logging, go `Help > Debug log settings` & type `#in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionServiceImpl`
     *
     * @param doWhenDebug code to execute when debug is enabled
     */
    private void debug(Runnable doWhenDebug) {
        if (log.isDebugEnabled()) {
            doWhenDebug.run();
        }
    }

}
