package in.oneton.idea.spring.assistant.plugin.initializr.misc;

import com.google.common.escape.Escaper;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.table.JBTable;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.IdContainer;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static com.intellij.openapi.projectRoots.JavaSdk.getInstance;
import static com.intellij.util.ObjectUtils.chooseNotNull;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@UtilityClass
public class InitializrUtil {
    public static final int GAP_BETWEEN_COMPONENTS = 5;
    private static final Escaper FORM_PARAMETER_ESCAPER = urlFormParameterEscaper();

    public static String nameAndValueAsUrlParam(String name, String value) {
        return name + "=" + FORM_PARAMETER_ESCAPER.escape(value);
    }

    public static void markAsExecutable(File containingDir, String relativePath) {
        File toFix = new File(containingDir, relativePath);
        if (toFix.exists()) {
            //noinspection ResultOfMethodCallIgnored
            toFix.setExecutable(true, false);
        }
    }

    @NotNull
    public static <T extends IdContainer> CollectionComboBoxModel<T> newCollectionComboBoxModel(
            @NotNull List<T> values, @Nullable String defaultValueId) {
        T defaultIdAndName = null;
        if (!isEmpty(defaultValueId)) {
            for (T idAndName : values) {
                if (idAndName.getId().equals(defaultValueId)) {
                    defaultIdAndName = idAndName;
                    break;
                }
            }
        }
        return new CollectionComboBoxModel<>(values, defaultIdAndName);
    }

    /**
     * Set defaults so that the table looks & acts more like a list
     *
     * @param table table
     */
    public static void resetTableLookAndFeelToSingleSelect(JBTable table) {
        table.setRowMargin(0);
        table.setShowColumns(false);
        table.setShowGrid(false);
        table.setShowVerticalLines(false);
        table.setCellSelectionEnabled(false);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(SINGLE_SELECTION);
    }

    @Nullable
    public static JavaSdkVersion from(WizardContext context, ModuleBuilder builder) {
        Sdk wizardSdk = context.isCreatingNewProject() ?
                context.getProjectJdk() :
                chooseNotNull(builder.getModuleJdk(), context.getProjectJdk());
        return wizardSdk == null ? null : getInstance().getVersion(wizardSdk);
    }

    @NotNull
    public static String userAgent() {
        return ApplicationNamesInfo.getInstance().getFullProductName() + "/" + ApplicationInfo
                .getInstance().getFullVersion();
    }

}
