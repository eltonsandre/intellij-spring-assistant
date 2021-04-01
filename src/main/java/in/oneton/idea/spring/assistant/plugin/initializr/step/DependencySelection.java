package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.speedSearch.FilteringListModel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.miguelfonseca.completely.AutocompleteEngine;
import com.miguelfonseca.completely.text.analyze.tokenize.WordTokenizer;
import com.miguelfonseca.completely.text.analyze.transform.LowerCaseTransformer;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import in.oneton.idea.spring.assistant.plugin.initializr.ProjectCreationRequest;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.IdAndName;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.IdAndNameComposite;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.Version;
import in.oneton.idea.spring.assistant.plugin.initializr.step.DependencyGroupAndDependency.DependencyGroupAndDependencyAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

import static com.intellij.openapi.actionSystem.CommonShortcuts.getFind;
import static com.intellij.openapi.util.text.StringUtil.isEmpty;
import static com.intellij.openapi.wm.IdeFocusManager.getGlobalInstance;
import static com.intellij.ui.components.JBList.createDefaultListModel;
import static com.intellij.ui.speedSearch.SpeedSearchUtil.applySpeedSearchHighlighting;
import static in.oneton.idea.spring.assistant.plugin.initializr.misc.InitializrUtil.newCollectionComboBoxModel;
import static java.util.Objects.requireNonNull;

public class DependencySelection implements Disposable, DependencySelectionChangeListener {

    @NotNull
    private final Map<DependencyGroup, List<Dependency>> filteredGroupToDependencies =
            new THashMap<>();
    private InitializerMetadata metadata;
    private SearchTextField filter;
    private ComboBox<IdAndName> bootVersion;
    private JPanel rootPanel;
    private JPanel rightPanel;
    private JPanel leftPanel;
    private JSplitPane leftRightSeperator;
    private JPanel leftTopPanel;
    private JPanel leftBottomPanel;
    private JSplitPane groupChildSeperator;
    private JPanel childPanel;
    private JSplitPane perGroupDependenciesAndDetailSeperator;
    private JPanel detailSection;
    private JPanel selectedDependenciesSection;
    private JBTable perGroupDependencyTable;
    private JBList<DependencyGroup> groups;
    private JScrollPane detailsScrollPane;
    private JPanel detailPanelContainer;
    private JScrollPane groupScollPane;
    private JScrollPane perGroupDependenciesSectionScrollPane;
    private JPanel perGroupDependenciesSection;
    private JScrollPane selectedDependenciesSectionScrollPane;
    private FilteringListModel<DependencyGroup> filteringGroupListModel;
    private JBTable selectedDependencies;
    private DependencyDetail dependencyDetail;
    private PerGroupDependencyTableModel perGroupDependencyTableModel;
    private AutocompleteEngine<DependencyGroupAndDependency> dependencyNameAndDescriptionIndex;
    private final DocumentChangedThrottler searchTextChangesThrottler = new DocumentChangedThrottler();
    private final List<VersionUpdateListener> versionUpdateListeners = new ArrayList<>();

    /**
     * Dependency that should be selected both in the group & per group selections
     */
    private Dependency deferreredDependecySelection;

    public void init(ProjectCreationRequest request) {
        // set empty border, because setting null doesn't always take effect
        Border emptyBorder = JBUI.Borders.empty();
        leftRightSeperator.setBorder(emptyBorder);
        groupChildSeperator.setBorder(emptyBorder);
        perGroupDependenciesAndDetailSeperator.setBorder(emptyBorder);

        groupScollPane.setBorder(emptyBorder);
        perGroupDependenciesSectionScrollPane.setBorder(emptyBorder);
        detailsScrollPane.setBorder(emptyBorder);
        selectedDependenciesSectionScrollPane.setBorder(emptyBorder);

        // Make Ctrl+f shortcut to focus on the filter
        AnAction findFocusAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                getGlobalInstance().doWhenFocusSettlesDown(
                        () -> getGlobalInstance().requestFocus(filter.getTextEditor(), true));
            }
        };
        findFocusAction.registerCustomShortcutSet(getFind(), getRootPanel());

        metadata = request.getMetadata();

        IdAndNameComposite bootVersionComposite = metadata.getBootVersionComposite();
        List<IdAndName> bootVersions = bootVersionComposite.getValues();
        Version defaultBootVersion =
                request.getSetVersion(bootVersions, bootVersionComposite.getDefaultValue());
        String defaultBootVersionId = defaultBootVersion != null ? defaultBootVersion.toString() : null;

        CollectionComboBoxModel<IdAndName> bootVersionComboBoxModel =
                newCollectionComboBoxModel(bootVersions, defaultBootVersionId);
        bootVersion.setModel(bootVersionComboBoxModel);

        ColoredListCellRenderer<DependencyGroup> categoryRenderer =
                new ColoredListCellRenderer<>() {
                    protected void customizeCellRenderer(@NotNull JList<? extends DependencyGroup> list,
                                                         DependencyGroup value, int index, boolean selected, boolean hasFocus) {
                        append(value.getName());
                        applySpeedSearchHighlighting(groups, this, true, selected);
                    }
                };
        groups.setCellRenderer(categoryRenderer);

        new ListSpeedSearch<>(groups,
                value -> ((DependencyGroup) value).getName());

        List<DependencyGroup> dependencyGroups = metadata.getDependencyComposite().getGroups();
        rebuildFilteredGroupsAndDependencies(dependencyGroups);
        filteringGroupListModel = new FilteringListModel<>(createDefaultListModel(dependencyGroups));
        groups.setModel(filteringGroupListModel);
        filteringGroupListModel.setFilter(filteredGroupToDependencies::containsKey);

        // build index for the dependency text
        dependencyNameAndDescriptionIndex =
                new AutocompleteEngine.Builder<DependencyGroupAndDependency>()
                        .setIndex(new DependencyGroupAndDependencyAdapter())
                        .setAnalyzers(new LowerCaseTransformer(), new StopwordsAwareWordTokenizer()).build();

        dependencyGroups.forEach(group -> group.getDependencies().forEach(dependency -> {
            DependencyGroupAndDependency groupAndDependency =
                    DependencyGroupAndDependency.builder().group(group).dependency(dependency).build();
            dependencyNameAndDescriptionIndex.add(groupAndDependency);
        }));

        // select first group
        groups.setSelectedIndex(0);

        // per group dependencies setup
        perGroupDependencyTableModel =
                new PerGroupDependencyTableModel(perGroupDependencyTable, request,
                        requireNonNull(getGroupSelection()), getBootVersion(), filteredGroupToDependencies);
        dependencyDetail.init(getBootVersion());

        // add all listeners
        filter.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                searchTextChangesThrottler.throttle(() -> {
                    filteredGroupToDependencies.clear();
                    String searchText = filter.getText();
                    if (!isEmpty(searchText)) {
                        List<DependencyGroupAndDependency> groupAndDependencies =
                                dependencyNameAndDescriptionIndex.search(searchText);
                        for (DependencyGroupAndDependency groupAndDependency : groupAndDependencies) {
                            List<Dependency> dependencies = filteredGroupToDependencies
                                    .getOrDefault(groupAndDependency.getGroup(), new ArrayList<>());
                            dependencies.add(groupAndDependency.getDependency());
                            filteredGroupToDependencies.put(groupAndDependency.getGroup(), dependencies);
                        }
                    } else {
                        rebuildFilteredGroupsAndDependencies(metadata.getDependencyComposite().getGroups());
                    }
                    filteringGroupListModel.refilter();
                    perGroupDependencyTableModel.reindexAndFireUpdate();
                    if (filteringGroupListModel.getSize() != 0) {
                        // if there is a deferred dependency that needs to be selected, lets select this both in the groups & also in the per group dependency table
                        if (deferreredDependecySelection != null) {
                            Optional<DependencyGroup> groupOptional = metadata.getDependencyComposite()
                                    .findGroupForDependency(deferreredDependecySelection);
                            assert groupOptional.isPresent();
                            DependencyGroup dependencyGroup = groupOptional.get();
                            int groupIndex = filteringGroupListModel.getElementIndex(dependencyGroup);
                            groups.setSelectedIndex(groupIndex);
                            perGroupDependencyTableModel.setSelection(deferreredDependecySelection);
                            deferreredDependecySelection = null;
                        } else {
                            groups.setSelectedIndex(0);
                        }
                    }
                });
            }
        });

        SelectedDependenciesTableModel selectedDependenciesTableModel =
                new SelectedDependenciesTableModel(selectedDependencies, request);

        groups.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                perGroupDependencyTableModel.update(getGroupSelection());
            }
        });
        bootVersion.addActionListener(e -> versionUpdateListeners.forEach(l -> {
            Version bootVersion = getBootVersion();
            request.setBootVersion(bootVersion);
            l.onVersionUpdated(bootVersion);
        }));

        versionUpdateListeners.add(perGroupDependencyTableModel);
        versionUpdateListeners.add(selectedDependenciesTableModel);
        versionUpdateListeners.add(dependencyDetail);

        perGroupDependencyTableModel.setAdditionListener(selectedDependenciesTableModel);
        perGroupDependencyTableModel.setRemovalListener(selectedDependenciesTableModel);
        perGroupDependencyTableModel.addSelectionListener(selectedDependenciesTableModel);
        perGroupDependencyTableModel.addSelectionListener(dependencyDetail);

        selectedDependenciesTableModel.addSelectionListener(this);
        selectedDependenciesTableModel.addSelectionListener(perGroupDependencyTableModel);
        selectedDependenciesTableModel.addRemovalListener(perGroupDependencyTableModel);
    }

    private void rebuildFilteredGroupsAndDependencies(List<DependencyGroup> dependencyGroups) {
        dependencyGroups.forEach(group -> {
            List<Dependency> dependencies =
                    filteredGroupToDependencies.getOrDefault(group, new ArrayList<>());
            dependencies.addAll(group.getDependencies());
            filteredGroupToDependencies.put(group, dependencies);
        });
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    @Nullable
    private DependencyGroup getGroupSelection() {
        return groups.getSelectedValue();
    }

    private Version getBootVersion() {
        return ((IdAndName) requireNonNull(bootVersion.getSelectedItem())).parseIdAsVersion();
    }

    @Override
    public void dispose() {
        searchTextChangesThrottler.dispose();
    }

    public JComponent getPreferredFocusedComponent() {
        return filter;
    }

    @Override
    public void onDependencySelected(@Nullable Dependency dependency) {
        if (dependency != null) {
            Optional<DependencyGroup> groupOptional =
                    metadata.getDependencyComposite().findGroupForDependency(dependency);
            assert groupOptional.isPresent();
            DependencyGroup dependencyGroup = groupOptional.get();
            int groupIndex = filteringGroupListModel.getElementIndex(dependencyGroup);
            if (groupIndex
                    == -1) { // this can happen if the search string is set, lets clear search string in this case
                filter.setText(null);
                deferreredDependecySelection = dependency;
            } else {
                groups.setSelectedIndex(groupIndex);
            }
        } else {
            groups.setSelectedIndex(0);
        }
    }


    interface VersionUpdateListener {
        void onVersionUpdated(Version newVersion);
    }


    static class StopwordsAwareWordTokenizer extends WordTokenizer {

        private final Set<String> stopwords = new THashSet<>();

        StopwordsAwareWordTokenizer() {
            InputStream resourceAsStream = getClass().getResourceAsStream("/stopwords-en.txt");
            try (Scanner scanner = new Scanner(resourceAsStream)) {

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line == null) {
                        continue;
                    }
                    line = line.trim();
                    line = line.toLowerCase();
                    if (line.isEmpty()) {
                        continue;
                    }
                    stopwords.add(line);
                }

            }
        }

        @Override
        public Collection<String> apply(String... input) {
            Collection<String> tokens = super.apply(input);
            tokens.removeAll(stopwords);
            return tokens;
        }

    }

}
