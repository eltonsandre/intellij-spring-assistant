package in.oneton.idea.spring.assistant.plugin.initializr.step;

import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import org.jetbrains.annotations.NotNull;

public interface DependencyAdditionListener {
    void onDependencyAdded(@NotNull Dependency dependency);
}
