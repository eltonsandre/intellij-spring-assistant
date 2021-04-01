package in.oneton.idea.spring.assistant.plugin.suggestion.component;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.util.messages.MessageBusConnection;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionService;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.compiler.CompilerTopics.COMPILATION_STATUS;

public class Bootstrap implements ProjectManagerListener {

    private static final Logger log = Logger.getInstance(Bootstrap.class);

    private MessageBusConnection connection;


    @Override
    public void projectOpened(@NotNull Project project) {
        // This will trigger indexing
        SuggestionService service = ServiceManager.getService(project, SuggestionService.class);

        try {
            debug(() -> log.debug("Project " + project.getName() + " is opened, indexing will start"));
            service.init(project);
        } finally {
            debug(() -> log.debug("Indexing complete for project " + project.getName()));
        }

        try {
            debug(() -> log.debug("Subscribing to compilation events for project " + project.getName()));
            connection = project.getMessageBus().connect();
            connection.subscribe(COMPILATION_STATUS, new CompilationStatusListener() {
                @Override
                public void compilationFinished(boolean aborted, int errors, int warnings,
                                                @NotNull CompileContext compileContext) {
                    debug(() -> log
                            .debug("Received compilation status event for project " + project.getName()));
                    if (errors == 0) {
                        CompileScope projectCompileScope = compileContext.getProjectCompileScope();
                        CompileScope compileScope = compileContext.getCompileScope();
                        if (projectCompileScope == compileScope) {
                            service.reIndex(project);
                        } else {
                            service.reindex(project, compileContext.getCompileScope().getAffectedModules());
                        }
                        debug(() -> log.debug("Compilation status processed for project " + project.getName()));
                    } else {
                        debug(() -> log
                                .debug("Skipping reindexing completely as there are " + errors + " errors"));
                    }
                }
            });
            debug(() -> log.debug("Subscribe to compilation events for project " + project.getName()));
        } catch (Throwable e) {
            log.error("Failed to subscribe to compilation events for project " + project.getName(), e);
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
