package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBLoadingPanel;
import in.oneton.idea.spring.assistant.plugin.initializr.InitializrModuleBuilder;
import in.oneton.idea.spring.assistant.plugin.initializr.ProjectCreationRequest;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency.DependencyLinksContainer.DependencyLink;
import in.oneton.idea.spring.assistant.plugin.initializr.misc.JsonUtil;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.intellij.ui.ScrollPaneFactory.createScrollPane;
import static com.intellij.util.io.HttpRequests.createErrorMessage;
import static com.intellij.util.io.HttpRequests.request;
import static in.oneton.idea.spring.assistant.plugin.initializr.InitializrModuleBuilder.CONTENT_TYPE;
import static in.oneton.idea.spring.assistant.plugin.initializr.misc.InitializrUtil.userAgent;
import static javax.swing.SwingUtilities.invokeLater;

public class ProjectDetailsStep extends ModuleWizardStep implements Disposable {

    private static final Type DEPENDENCY_LINKS_LIST_TYPE = new TypeToken<List<DependencyLink>>() {
    }.getType();

    private final JBLoadingPanel loadingPanel = new JBLoadingPanel(new BorderLayout(), this, 100);
    private final InitializrModuleBuilder moduleBuilder;
    private final WizardContext wizardContext;
    private final ProjectCreationRequest request;
    private boolean fetchInProgress;
    private ProjectDetails detailsForm;

    public ProjectDetailsStep(InitializrModuleBuilder moduleBuilder, WizardContext wizardContext) {
        this.moduleBuilder = moduleBuilder;
        this.wizardContext = wizardContext;
        request = moduleBuilder.safeGetProjectCreationRequest();
    }

    @Override
    public void _init() {
        if (request.getMetadata() == null) {
            loadingPanel.getContentPanel().removeAll();
            loadingPanel.startLoading();
            fetchInProgress = true;
            // Lets fetch metadata on a background thread, so as not to block UI thread
            getApplication().executeOnPooledThread(() -> {
                try {
                    InitializerMetadata metadata =
                            request(request.getServerUrl()).accept(CONTENT_TYPE).userAgent(userAgent())
                                    .connect(httpRequest -> {
                                        invokeLater(() -> loadingPanel.setLoadingText(
                                                "Please wait.. Spring Initializr options are being loaded.."));
                                        try (BufferedReader reader = httpRequest.getReader()) {
                                            return JsonUtil.fromJson(reader, InitializerMetadata.class);
                                        } catch (IOException e) {
                                            throw new IOException(createErrorMessage(e, httpRequest, false), e);
                                        }
                                    });
                    request.setMetadata(metadata);
                    invokeLater(() -> {
                        detailsForm = new ProjectDetails();
                        detailsForm.init(moduleBuilder, wizardContext);
                        loadingPanel.add(createScrollPane(detailsForm.getRoot(), true), "North");
                    });
                } catch (IOException e) {
                    invokeLater(() -> showErrorDialog(
                            "Error while fetching metadata from server '" + request.getServerUrl()
                                    + "'\nPlease check URL, network and proxy settings.\n\nError message:\n" + e
                                    .getMessage(), "Fetch Error"));
                } catch (JsonSyntaxException | JsonIOException e) {
                    invokeLater(() -> showErrorDialog(
                            "Error while parsing metadata from server '" + request.getServerUrl()
                                    + "'\nMake sure you connected to the correct initializr server."
                                    + "\n\nError message:\n" + e.getMessage(), "Metadata Error"));
                } finally {
                    invokeLater(() -> {
                        loadingPanel.stopLoading();
                        loadingPanel.revalidate();
                    });
                    fetchInProgress = false;
                }
            });
        }
    }

    @Override
    public JComponent getComponent() {
        return loadingPanel;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        if (request.getMetadata() == null) {
            if (fetchInProgress) {
                throw new ConfigurationException("Metadata is being fetch from server. Please wait",
                        "Please Wait");
            } else {
                throw new ConfigurationException(
                        "Could not fetch metadata from server. Please go back & check server details",
                        "Fetch Error");
            }
        }
        return detailsForm.validate(moduleBuilder, wizardContext);
    }

    @Override
    public void updateDataModel() {
    }

    @Override
    public void dispose() {
    }

}
