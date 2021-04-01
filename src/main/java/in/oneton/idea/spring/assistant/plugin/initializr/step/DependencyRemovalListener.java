package in.oneton.idea.spring.assistant.plugin.initializr.step;

import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import org.jetbrains.annotations.NotNull;

public interface DependencyRemovalListener {
    void onDependencyRemoved(@NotNull Dependency dependency);
}
