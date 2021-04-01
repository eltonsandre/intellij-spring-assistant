package in.oneton.idea.spring.assistant.plugin.suggestion.component;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionService;
import org.jetbrains.idea.maven.project.MavenConsole;
import org.jetbrains.idea.maven.project.MavenEmbeddersManager;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.utils.MavenProgressIndicator;

public class MavenProcessorTask implements MavenProjectsProcessorTask {

    private static final Logger log = Logger.getInstance(MavenProcessorTask.class);

    private final Module module;

    MavenProcessorTask(Module module) {
        this.module = module;
    }


    @Override
    public void perform(Project project, MavenEmbeddersManager mavenEmbeddersManager,
                        MavenConsole mavenConsole, MavenProgressIndicator mavenProgressIndicator) {
        debug(() -> log.debug(
                "Project imported successfully, will trigger indexing via dumbservice for project "
                        + project.getName()));
        DumbService.getInstance(project).smartInvokeLater(() -> {
            log.debug("Will attempt to trigger indexing for project " + project.getName());

            try {
                SuggestionService service = ServiceManager.getService(project, SuggestionService.class);

                if (!service.canProvideSuggestions(module)) {
                    service.reindex(project, module);
                } else {
                    debug(() -> log.debug(
                            "Index is already built, no point in rebuilding index for project " + project
                                    .getName()));
                }
            } catch (Throwable e) {
                log.error("Error occurred while indexing project " + project.getName(), e);
            }
        });
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
