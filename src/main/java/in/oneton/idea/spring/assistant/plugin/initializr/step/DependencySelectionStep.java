package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import in.oneton.idea.spring.assistant.plugin.initializr.InitializrModuleBuilder;
import in.oneton.idea.spring.assistant.plugin.initializr.ProjectCreationRequest;

import javax.swing.*;
import java.awt.*;

public class DependencySelectionStep extends ModuleWizardStep implements Disposable {

    private final ProjectCreationRequest request;
    private final JPanel rootPanel;
    private DependencySelection dependencySelection;

    public DependencySelectionStep(InitializrModuleBuilder moduleBuilder) {
        request = moduleBuilder.safeGetProjectCreationRequest();
        rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout());
    }

    @Override
    public void _init() {
        rootPanel.removeAll();
        dependencySelection = new DependencySelection();
        dependencySelection.init(request);
        rootPanel.add(dependencySelection.getRootPanel());
    }

    @Override
    public boolean validate() throws ConfigurationException {
        if (request.getDependencyCount() == 0) {
            throw new ConfigurationException("Please select atleast one dependency",
                    "Selection Required");
        }
        return super.validate();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return dependencySelection.getPreferredFocusedComponent();
    }

    @Override
    public JComponent getComponent() {
        return rootPanel;
    }

    @Override
    public void updateDataModel() {

    }

    @Override
    public void dispose() {
        dependencySelection.dispose();
    }

}
