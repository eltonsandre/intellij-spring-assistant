package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.TableUtil;
import com.intellij.ui.table.JBTable;
import gnu.trove.THashSet;
import in.oneton.idea.spring.assistant.plugin.initializr.ProjectCreationRequest;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.Version;
import in.oneton.idea.spring.assistant.plugin.initializr.step.DependencySelection.VersionUpdateListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES;
import static com.intellij.ui.speedSearch.SpeedSearchUtil.applySpeedSearchHighlighting;
import static in.oneton.idea.spring.assistant.plugin.initializr.misc.InitializrUtil.resetTableLookAndFeelToSingleSelect;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.awt.event.MouseEvent.BUTTON1;
import static java.util.Collections.emptyList;
import static javax.swing.KeyStroke.getKeyStroke;

public class PerGroupDependencyTableModel extends AbstractTableModel
        implements VersionUpdateListener, DependencySelectionChangeListener, DependencyRemovalListener {

    private static final Logger log = Logger.getInstance(PerGroupDependencyTableModel.class);

    private static final int CHECKBOX_COL_INDEX = 0;
    private static final int DEPENDENCY_NAME_COL_INDEX = 1;

    @NotNull
    private final JBTable perGroupDependencyTable;
    @NotNull
    private final ProjectCreationRequest request;
    @NotNull
    private Version bootVersion;
    @Nullable
    private DependencyGroup dependencyGroup;
    @Nullable
    private Set<Integer> incompatibleDependencyIndexes;
    @NotNull
    private final Map<DependencyGroup, List<Dependency>> filteredGroupAndDependencies;
    private DependencyAdditionListener additionListener;
    private final List<DependencySelectionChangeListener> selectionListeners = new ArrayList<>();
    private DependencyRemovalListener removalListener;

    PerGroupDependencyTableModel(JBTable perGroupDependencyTable,
                                 @NotNull ProjectCreationRequest request, @NotNull DependencyGroup dependencyGroup,
                                 @NotNull Version bootVersion,
                                 @NotNull Map<DependencyGroup, List<Dependency>> filteredGroupAndDependencies) {
        this.perGroupDependencyTable = perGroupDependencyTable;
        this.request = request;
        this.dependencyGroup = dependencyGroup;
        this.bootVersion = bootVersion;
        this.filteredGroupAndDependencies = filteredGroupAndDependencies;
        reindex();

        perGroupDependencyTable.setModel(this);
        resetTableLookAndFeelToSingleSelect(perGroupDependencyTable);

        TableColumnModel columnModel = perGroupDependencyTable.getColumnModel();
        columnModel.setColumnMargin(0);
        TableColumn checkBoxColumn = columnModel.getColumn(CHECKBOX_COL_INDEX);
        TableUtil.setupCheckboxColumn(checkBoxColumn, 0);
        checkBoxColumn.setCellRenderer(new BooleanTableCellRenderer());
        TableColumn dependencyColumn = columnModel.getColumn(DEPENDENCY_NAME_COL_INDEX);
        dependencyColumn.setCellRenderer(new ColoredTableCellRenderer() {
            @Override
            protected void customizeCellRenderer(@NotNull JTable table, @Nullable Object value, boolean selected,
                                                 boolean hasFocus, int row, int column) {
                if (value != null) {
                    Dependency dependency = (Dependency) value;
                    boolean selectable = isCellEditable(row, CHECKBOX_COL_INDEX);
                    if (selectable) {
                        append(dependency.getName());
                    } else {
                        append(dependency.getName(), GRAY_ATTRIBUTES);
                    }
                }
                // Enable search highlighting. This in conjunction with TableSpeedSearch(below) enables type to search capability of intellij
                applySpeedSearchHighlighting(table, this, true, selected);
            }
        });

        new TableSpeedSearch(perGroupDependencyTable, value -> {
            if (value instanceof Dependency) {
                return ((Dependency) value).getName();
            }
            return "";
        });

        // Add listeners

        // Allow user to select via keyboard
        perGroupDependencyTable.getActionMap().put("select_deselect_dependency", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                toggleSelectionIfApplicable(perGroupDependencyTable.getSelectedRow());
            }
        });
        perGroupDependencyTable.getInputMap()
                .put(getKeyStroke(VK_SPACE, 0), "select_deselect_dependency");
        // Allow user to toggle via double click
        perGroupDependencyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() == BUTTON1) {
                    int columIndex = perGroupDependencyTable.columnAtPoint(event.getPoint());
                    if (columIndex == DEPENDENCY_NAME_COL_INDEX) {
                        int rowIndex = perGroupDependencyTable.rowAtPoint(event.getPoint());
                        if (event.getClickCount() == 2) {
                            toggleSelectionIfApplicable(rowIndex);
                        }
                    }
                }
            }
        });

        perGroupDependencyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = perGroupDependencyTable.getSelectedRow();
                if (selectedRow != -1) {
                    selectionListeners
                            .forEach(listener -> listener.onDependencySelected(getDependencyAt(selectedRow)));
                } else {
                    selectionListeners.forEach(listener -> listener.onDependencySelected(null));
                }
            }
        });
    }

    public void setAdditionListener(@NotNull DependencyAdditionListener listener) {
        this.additionListener = listener;
    }

    public void setRemovalListener(@NotNull DependencyRemovalListener listener) {
        this.removalListener = listener;
    }

    public void addSelectionListener(@NotNull DependencySelectionChangeListener listener) {
        this.selectionListeners.add(listener);
    }

    @Override
    public int getRowCount() {
        return getFilteredDependencies().size();
    }

    @Override
    public int getColumnCount() {
        // checkbox & dependency name
        return 2;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == CHECKBOX_COL_INDEX) {
            return Boolean.class;
        } else {
            return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == CHECKBOX_COL_INDEX && (incompatibleDependencyIndexes == null
                || !incompatibleDependencyIndexes.contains(rowIndex));
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Dependency dependency = getFilteredDependencies().get(rowIndex);
        return columnIndex == CHECKBOX_COL_INDEX ? request.containsDependency(dependency) : dependency;
    }

    private Dependency getDependencyAt(int selectedRow) {
        return (Dependency) getValueAt(selectedRow, DEPENDENCY_NAME_COL_INDEX);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Dependency dependency = getFilteredDependencies().get(rowIndex);
        Boolean selected = (Boolean) aValue;
        if (selected) {
            debug(() -> log.debug("Dependency selected: " + dependency));
            additionListener.onDependencyAdded(dependency);
        } else {
            debug(() -> log.debug("Dependency unselected: " + dependency));
            removalListener.onDependencyRemoved(dependency);
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public void update(@Nullable DependencyGroup dependencyGroup) {
        this.dependencyGroup = dependencyGroup;
        if (dependencyGroup != null) {
            reindex();
        } else {
            incompatibleDependencyIndexes = null;
        }
        fireTableDataChanged();
    }

    @NotNull
    private List<Dependency> getFilteredDependencies() {
        List<Dependency> dependencies = filteredGroupAndDependencies.get(dependencyGroup);
        return Objects.requireNonNullElse(dependencies, emptyList());
    }

    private void toggleSelectionIfApplicable(int row) {
        if (isCellEditable(row, CHECKBOX_COL_INDEX)) {
            boolean selected = (Boolean) getValueAt(row, CHECKBOX_COL_INDEX);
            setValueAt(!selected, row, CHECKBOX_COL_INDEX);
        }
    }

    @Override
    public void onVersionUpdated(Version newVersion) {
        this.bootVersion = newVersion;
        reindexAndFireUpdate();
    }

    @Override
    public void onDependencySelected(@Nullable Dependency dependency) {
        if (dependency != null) {
            int dependencyIndex = getFilteredDependencies().indexOf(dependency);
            perGroupDependencyTable.getSelectionModel()
                    .setSelectionInterval(dependencyIndex, dependencyIndex);
        }
    }

    @Override
    public void onDependencyRemoved(@NotNull Dependency dependency) {
        fireTableDataChanged();
    }

    /**
     * Debug logging can be enabled by adding fully classified class name/package name with # prefix
     * For eg., to enable debug logging, go `Help > Debug log settings` & type `#in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionServiceImpl`
     *
     * @param doWhenDebug code to execute when debug is enabled
     */
    private void debug(Runnable doWhenDebug) {
        if (log.isDebugEnabled()) {
            doWhenDebug.run();
        }
    }

    public void setSelection(Dependency selection) {
        onDependencySelected(selection);
    }

    public void reindexAndFireUpdate() {
        reindex();
        fireTableDataChanged();
    }

    private void reindex() {
        if (dependencyGroup != null) {
            incompatibleDependencyIndexes = new THashSet<>();
            List<Dependency> dependencies = filteredGroupAndDependencies.get(dependencyGroup);
            for (int i = 0; i < dependencies.size(); i++) {
                if (!dependencies.get(i).isVersionCompatible(bootVersion)) {
                    incompatibleDependencyIndexes.add(i);
                }
            }
        }
    }

}
