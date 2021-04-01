package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.openapi.ui.popup.IconButton;
import com.intellij.ui.InplaceButton;
import com.intellij.ui.components.JBLabel;

import javax.swing.*;

import static com.intellij.icons.AllIcons.Modules.ExcludeRoot;

public class SelectedDependencyListItem {

    private SelectedDependencyListItemListener listener;

    private InplaceButton deleteButton;
    private JBLabel nameLabel;
    private JPanel root;

    public void init(String name, SelectedDependencyListItemListener listener) {
        this.listener = listener;
        this.nameLabel.setText(name);
    }

    private void createUIComponents() {
        this.deleteButton = new InplaceButton(new IconButton("Click to delete", ExcludeRoot, ExcludeRoot), e -> this.listener.onDeleteClicked());
    }

    public JPanel getRoot() {
        return this.root;
    }

    interface SelectedDependencyListItemListener {
        void onDeleteClicked();
    }

}
