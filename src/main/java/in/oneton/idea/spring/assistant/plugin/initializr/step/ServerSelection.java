package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.TextFieldWithHistory;
import com.intellij.ui.TextFieldWithStoredHistory;
import com.intellij.ui.components.JBRadioButton;
import in.oneton.idea.spring.assistant.plugin.initializr.ProjectCreationRequest;

import javax.swing.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static com.intellij.ide.util.PropertiesComponent.getInstance;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class ServerSelection {
    private static final String SPRING_ASSINSTANT_INITIALIZR_LAST_KNOWN_SERVER_URL =
            "spring.assinstant.initializr.last.known.server.url";
    private static final String SPRING_IO_DEFAULT_INITIALIZR_SERVER_URL = "https://start.spring.io";

    private JBRadioButton defaultRadioBtn;
    private JBRadioButton customRadioBtn;
    private JPanel root;
    private TextFieldWithHistory customTextFieldWithHistory;
    private HyperlinkLabel defaultHyperLink;
    private ProjectCreationRequest request;

    public JPanel getRoot() {
        return root;
    }

    public void init(ProjectCreationRequest request) {
        this.request = request;

        // If user entered default/dataflow url in the custom field & moved ahead, lets remove those values from custom field
        List<String> urlHistory = customTextFieldWithHistory.getHistory();
        urlHistory.removeIf(url -> url.equals(SPRING_IO_DEFAULT_INITIALIZR_SERVER_URL));
        customTextFieldWithHistory.setHistory(urlHistory);

        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(defaultRadioBtn);
        radioButtonGroup.add(customRadioBtn);

        defaultRadioBtn.addActionListener(e -> customTextFieldWithHistory.setEnabled(false));
        customRadioBtn.addActionListener(e -> {
            customTextFieldWithHistory.setEnabled(true);
            customTextFieldWithHistory.requestFocus();
        });

        String serverUrl = request.isServerUrlSet() ?
                request.getServerUrl() :
                getInstance().getValue(SPRING_ASSINSTANT_INITIALIZR_LAST_KNOWN_SERVER_URL,
                        SPRING_IO_DEFAULT_INITIALIZR_SERVER_URL);

        if (SPRING_IO_DEFAULT_INITIALIZR_SERVER_URL.equals(serverUrl)) {
            defaultRadioBtn.doClick();
        } else {
            customRadioBtn.doClick();
            customTextFieldWithHistory.setTextAndAddToHistory(serverUrl);
        }

    }

    public boolean validateServerUrl() throws ConfigurationException {
        if (customRadioBtn.isSelected()) {
            String serverUrl = customTextFieldWithHistory.getText();
            if (isEmpty(serverUrl)) {
                throw new ConfigurationException("Custom Initializr Server url must be set");
            }
            try {
                new URI(serverUrl);
                return true;
            } catch (URISyntaxException e) {
                throw new ConfigurationException("Custom Initializr Server URL must be a valid url");
            }
        }
        return true;
    }

    public void updateDataModel() {
        if (defaultRadioBtn.isSelected()) {
            request.setServerUrl(SPRING_IO_DEFAULT_INITIALIZR_SERVER_URL);
        } else {
            String customRawUrl = customTextFieldWithHistory.getText().trim();
            // lets add http if the url is not specified
            if (!customRawUrl.startsWith("http") && !customRawUrl.startsWith("https")) {
                customTextFieldWithHistory.setText("http://" + customTextFieldWithHistory.getText());
            }
            customTextFieldWithHistory.addCurrentTextToHistory();
            request.setServerUrl(customTextFieldWithHistory.getText());
        }
        getInstance()
                .setValue(SPRING_ASSINSTANT_INITIALIZR_LAST_KNOWN_SERVER_URL, request.getServerUrl());
    }

    private void createUIComponents() {
        defaultHyperLink = new HyperlinkLabel(SPRING_IO_DEFAULT_INITIALIZR_SERVER_URL);
        defaultHyperLink.setHyperlinkTarget(SPRING_IO_DEFAULT_INITIALIZR_SERVER_URL);

        customTextFieldWithHistory =
                new TextFieldWithStoredHistory("spring.assistant.initializr.custom.server.url.history");
    }
}
