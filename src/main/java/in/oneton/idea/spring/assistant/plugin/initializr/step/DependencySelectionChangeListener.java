package in.oneton.idea.spring.assistant.plugin.initializr.step;

import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import org.jetbrains.annotations.Nullable;

public interface DependencySelectionChangeListener {
    /**
     * @param dependency dependency if selected, null if selection is reset
     */
    void onDependencySelected(@Nullable Dependency dependency);
}
