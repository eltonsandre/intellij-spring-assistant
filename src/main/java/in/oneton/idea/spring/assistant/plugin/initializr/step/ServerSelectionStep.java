package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import in.oneton.idea.spring.assistant.plugin.initializr.InitializrModuleBuilder;
import in.oneton.idea.spring.assistant.plugin.initializr.ProjectCreationRequest;

import javax.swing.*;

public class ServerSelectionStep extends ModuleWizardStep {

    private final ProjectCreationRequest request;
    private ServerSelection serverSelection;

    public ServerSelectionStep(InitializrModuleBuilder moduleBuilder) {
        request = moduleBuilder.safeGetProjectCreationRequest();
    }

    @Override
    public JComponent getComponent() {
        serverSelection = new ServerSelection();
        serverSelection.init(request);
        return serverSelection.getRoot();
    }

    @Override
    public boolean validate() throws ConfigurationException {
        return serverSelection.validateServerUrl();
    }

    @Override
    public void updateDataModel() {
        serverSelection.updateDataModel();
    }

}
