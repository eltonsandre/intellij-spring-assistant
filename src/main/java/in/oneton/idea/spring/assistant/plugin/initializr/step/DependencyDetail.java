package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.panels.VerticalLayout;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency.DependencyLinksContainer;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency.DependencyLinksContainer.DependencyLink;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.Version;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

import static com.intellij.icons.AllIcons.General.BalloonWarning;
import static com.intellij.util.ui.JBUI.Fonts.smallFont;
import static in.oneton.idea.spring.assistant.plugin.initializr.misc.InitializrUtil.GAP_BETWEEN_COMPONENTS;
import static java.util.Objects.requireNonNull;

public class DependencyDetail
        implements DependencySelectionChangeListener, DependencySelection.VersionUpdateListener {

    private Version version;

    private JPanel root;
    private JBLabel name;
    private JBLabel description;
    private JBLabel guidesTitle;
    private JPanel guidesContainer;
    private JBLabel referenceTitle;
    private JPanel referenceContainer;
    private JBLabel incompatibleLabel;

    public void init(Version version) {
        this.version = version;
    }

    @Override
    public void onDependencySelected(@Nullable Dependency dependency) {
        reset();
        if (dependency != null) {
            name.setText(dependency.getName());
            name.setVisible(true);
            description.setText(dependency.getDescription());
            description.setVisible(true);

            if (!dependency.isVersionCompatible(version)) {
                incompatibleLabel.setIcon(BalloonWarning);
                incompatibleLabel.setText(
                        "Requires Spring boot/cloud version " + requireNonNull(dependency.getVersionRange())
                                .toString());
                incompatibleLabel.setVisible(true);
            }

            DependencyLinksContainer linksContainer = dependency.getLinksContainer();
            if (linksContainer != null) {
                List<DependencyLink> guideLinks = linksContainer.getGuides();
                if (guideLinks != null) {
                    guidesContainer.setVisible(true);
                    guideLinks.forEach(
                            guideLink -> guidesContainer.add(newHyperLink(guideLink, version.toString())));
                    guidesTitle.setVisible(true);
                }

                List<DependencyLink> references = linksContainer.getReferences();
                if (references != null) {
                    referenceContainer.setVisible(true);
                    references.forEach(reference -> referenceContainer.add(newHyperLink(reference, version.toString())));
                    referenceTitle.setVisible(true);
                }
            }
        }
    }

    @Override
    public void onVersionUpdated(Version newVersion) {
        version = newVersion;
    }

    public JPanel getRoot() {
        return root;
    }

    private void reset() {
        name.setVisible(false);
        incompatibleLabel.setVisible(false);
        description.setVisible(false);

        guidesTitle.setVisible(false);
        guidesContainer.setVisible(false);
        guidesContainer.removeAll();

        referenceTitle.setVisible(false);
        referenceContainer.setVisible(false);
        referenceContainer.removeAll();
    }

    private HyperlinkLabel newHyperLink(DependencyLink dependencyLink, String bootVersion) {
        String title = dependencyLink.getTitle();
        String href = dependencyLink.getHrefAfterReplacement(bootVersion);
        String text = title != null ? title : href;
        HyperlinkLabel hyperlinkLabel = new HyperlinkLabel(text);
        hyperlinkLabel.setToolTipText(text);
        hyperlinkLabel.setHyperlinkTarget(href);
        hyperlinkLabel.setFont(smallFont());
        hyperlinkLabel.setBackground(JBColor.RED);
        hyperlinkLabel.setForeground(JBColor.YELLOW);
        return hyperlinkLabel;
    }

    private void createUIComponents() {
        referenceContainer = new JPanel();
        referenceContainer.setLayout(new VerticalLayout(GAP_BETWEEN_COMPONENTS));
    }

}
