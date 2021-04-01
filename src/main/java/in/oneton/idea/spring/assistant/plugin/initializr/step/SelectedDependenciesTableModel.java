package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.openapi.ui.popup.IconButton;
import com.intellij.openapi.util.Couple;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.InplaceButton;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.TableUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import in.oneton.idea.spring.assistant.plugin.initializr.ProjectCreationRequest;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.Version;
import in.oneton.idea.spring.assistant.plugin.initializr.step.DependencySelection.VersionUpdateListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.icons.AllIcons.Actions.Cancel;
import static com.intellij.ui.speedSearch.SpeedSearchUtil.applySpeedSearchHighlighting;
import static in.oneton.idea.spring.assistant.plugin.initializr.misc.InitializrUtil.resetTableLookAndFeelToSingleSelect;
import static java.awt.event.MouseEvent.BUTTON1;

public class SelectedDependenciesTableModel extends AbstractTableModel
        implements VersionUpdateListener, DependencyAdditionListener, DependencySelectionChangeListener,
        DependencyRemovalListener {

    private static final int DEPENDENCY_COL_INDEX = 0;
    private static final int DELETE_ICON_INDEX = 1;
    private final ProjectCreationRequest request;
    private final JBTable selectedDependencies;
    private final List<DependencySelectionChangeListener> selectionListeners = new ArrayList<>();
    private DependencyRemovalListener removalListener;
    private boolean ignoreNextSelection;

    SelectedDependenciesTableModel(JBTable selectedDependencies, ProjectCreationRequest request) {
        this.request = request;
        this.selectedDependencies = selectedDependencies;
        selectedDependencies.setModel(this);
        resetTableLookAndFeelToSingleSelect(selectedDependencies);

        TableColumnModel columnModel = selectedDependencies.getColumnModel();
        columnModel.setColumnMargin(0);
        TableColumn dependencyColumn = columnModel.getColumn(DEPENDENCY_COL_INDEX);
        dependencyColumn.setCellRenderer(new ColoredTableCellRenderer() {
            @Override
            protected void customizeCellRenderer(@NotNull JTable table, @Nullable Object value, boolean selected,
                                                 boolean hasFocus, int row, int column) {
                if (value != null) {
                    Dependency dependency = (Dependency) value;
                    append(dependency.getName());
                }
                // Enable search highlighting. This in conjunction with TableSpeedSearch(below) enables type to search capability of intellij
                applySpeedSearchHighlighting(table, this, true, selected);
            }
        });

        new TableSpeedSearch(selectedDependencies, value -> {
            if (value instanceof Dependency) {
                return ((Dependency) value).getName();
            }
            return "";
        });

        TableColumn deleteIconColumn = columnModel.getColumn(DELETE_ICON_INDEX);
        TableUtil.setupCheckboxColumn(deleteIconColumn, 0);
        deleteIconColumn.setCellRenderer(new InplaceButtonTableCellRenderer());

        selectedDependencies.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                if (!ignoreNextSelection) {
                    int rowIndex = selectedDependencies.getSelectedRow();
                    Dependency dependency = (Dependency) getValueAt(rowIndex, DEPENDENCY_COL_INDEX);
                    selectionListeners.forEach(listener -> listener.onDependencySelected(dependency));
                }
                ignoreNextSelection = false;
            }
        });
        // Since clicking on the same row to delete dependency thats already selected does not trigger a new ListSelection event, we should rely on mouse click events for deletes
        selectedDependencies.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() == BUTTON1) {
                    int columnIndex = selectedDependencies.columnAtPoint(event.getPoint());
                    int rowIndex = selectedDependencies.rowAtPoint(event.getPoint());
                    Dependency dependency = (Dependency) getValueAt(rowIndex, DEPENDENCY_COL_INDEX);
                    if (columnIndex == DELETE_ICON_INDEX) {
                        ignoreNextSelection = true;
                        removeDependency(dependency, request);
                    }
                }
            }
        });
    }

    @Override
    public void onVersionUpdated(Version newVersion) {
        int oldSize = request.getDependencyCount();
        request.removeIncompatibleDependencies(newVersion);
        int newSize = request.getDependencyCount();
        if (oldSize != newSize) {
            fireTableDataChanged();
        }
    }


    @Override
    public int getRowCount() {
        return request.getDependencyCount();
    }

    @Override
    public int getColumnCount() {
        // dependency name, delete icon
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return request.getDependencyAtIndex(rowIndex);
    }

    public void addSelectionListener(DependencySelectionChangeListener listener) {
        selectionListeners.add(listener);
    }

    public void addRemovalListener(DependencyRemovalListener listener) {
        removalListener = listener;
    }

    private void removeDependency(Dependency dependency, ProjectCreationRequest request) {
        request.removeDependency(dependency);
        fireTableDataChanged();
        if (removalListener != null) {
            removalListener.onDependencyRemoved(dependency);
        }
    }

    @Override
    public void onDependencyAdded(@NotNull Dependency dependency) {
        boolean added = request.addDependency(dependency);
        if (added) {
            int indexOfDependency = request.getIndexOfDependency(dependency);
            ignoreNextSelection = true;
            fireTableDataChanged();
            selectedDependencies.getSelectionModel()
                    .setSelectionInterval(indexOfDependency, indexOfDependency);
        }
    }

    @Override
    public void onDependencyRemoved(@NotNull Dependency dependency) {
        boolean removed = request.removeDependency(dependency);
        if (removed) {
            ignoreNextSelection = true;
            fireTableDataChanged();
        }
    }

    @Override
    public void onDependencySelected(@Nullable Dependency dependency) {
        ignoreNextSelection = true;
        if (dependency == null) {
            selectedDependencies.getSelectionModel().clearSelection();
        } else {
            int index = request.getIndexOfDependency(dependency);
            if (index != -1) {
                selectedDependencies.getSelectionModel().setSelectionInterval(index, index);
            } else {
                selectedDependencies.getSelectionModel().clearSelection();
            }
        }
    }


    static class InplaceButtonTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            InplaceButton inplaceButton = new InplaceButton(
                    new IconButton("Click to delete", Cancel, Cancel),
                    e -> {
                    });
            Couple<Color> colors = UIUtil.getCellColors(table, isSelected, row, column);
            setForeground(colors.getFirst());
            setBackground(colors.getSecond());
            setEnabled(true);
            inplaceButton.setOpaque(false);
            return inplaceButton;
        }
    }

}
