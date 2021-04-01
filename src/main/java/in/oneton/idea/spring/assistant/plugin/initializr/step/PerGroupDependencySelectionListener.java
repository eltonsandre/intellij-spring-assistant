package in.oneton.idea.spring.assistant.plugin.initializr.step;

import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;

public interface PerGroupDependencySelectionListener {
    void onPerGroupDependencySelected(Dependency dependency);

    void onPerGroupDependencyUnselected(Dependency dependency);
}
