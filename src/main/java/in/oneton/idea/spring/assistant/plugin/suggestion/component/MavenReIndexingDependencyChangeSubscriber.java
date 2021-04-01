package in.oneton.idea.spring.assistant.plugin.suggestion.component;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.util.messages.MessageBusConnection;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenImportListener;

import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.moduleNamesAsStrCommaDelimited;

public class MavenReIndexingDependencyChangeSubscriber implements ProjectManagerListener {

    private static final Logger log =
            Logger.getInstance(MavenReIndexingDependencyChangeSubscriber.class);

    private MessageBusConnection connection;

    @Override
    public void projectOpened(@NotNull Project project) {
        // This will trigger indexing
        SuggestionService service = ServiceManager.getService(project, SuggestionService.class);

        try {
            debug(() -> log
                    .debug("Subscribing to maven dependency updates for project " + project.getName()));
            connection = project.getMessageBus().connect();
            connection.subscribe(MavenImportListener.TOPIC, (importedProjects, newModules) -> {
                boolean proceed = importedProjects.stream().anyMatch(
                        proj -> project.getName().equals(proj.getName()) && proj.getDirectory()
                                .equals(project.getBasePath()));

                if (proceed) {
                    debug(() -> log.debug("Maven dependencies are updated for project " + project.getName()));
                    DumbService.getInstance(project).smartInvokeLater(() -> {
                        log.debug("Will attempt to trigger indexing for project " + project.getName());
                        try {
                            Module[] modules = ModuleManager.getInstance(project).getModules();
                            if (modules.length > 0) {
                                service.reindex(project, modules);
                            } else {
                                debug(() -> log.debug("Skipping indexing for project " + project.getName()
                                        + " as there are no modules"));
                            }
                        } catch (Throwable e) {
                            log.error("Error occurred while indexing project " + project.getName() + " & modules "
                                    + moduleNamesAsStrCommaDelimited(newModules, false), e);
                        }
                    });
                } else {
                    log.debug(
                            "Skipping indexing as none of the imported projects match our project " + project
                                    .getName());
                }
            });
        } catch (Throwable e) {
            log.error("Failed to subscribe to maven dependency updates for project " + project.getName(),
                    e);
        }
    }

    @Override
    public void projectClosed(@NotNull Project project) {
        // TODO: Need to remove current project from index
        connection.disconnect();
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
