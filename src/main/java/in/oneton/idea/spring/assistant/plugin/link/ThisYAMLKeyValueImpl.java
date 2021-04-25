package in.oneton.idea.spring.assistant.plugin.link;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Iconable;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl;

import javax.swing.*;

class ThisYAMLKeyValueImpl extends YAMLKeyValueImpl implements YAMLKeyValue {

    @Setter
    private String presentableText;

    ThisYAMLKeyValueImpl(@NotNull final ASTNode node) {
        super(node);
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            private static final int PRESENTABLE_TEXT_LENGTH = 100;

            @Override
            public String getLocationString() {
                return ThisYAMLKeyValueImpl.this.getContainingFile().getName();
            }

            @Override
            public String getPresentableText() {
                if (ThisYAMLKeyValueImpl.this.presentableText == null || ThisYAMLKeyValueImpl.this.presentableText.isEmpty()) {
                    return LinkUtils.truncateStringWithEllipsis(ThisYAMLKeyValueImpl.this.getText(), PRESENTABLE_TEXT_LENGTH);
                }
                return LinkUtils.truncateStringWithEllipsis(ThisYAMLKeyValueImpl.this.presentableText, PRESENTABLE_TEXT_LENGTH);
            }

            @Override
            public @Nullable Icon getIcon(final boolean unused) {
                return ThisYAMLKeyValueImpl.this.getIcon(Iconable.ICON_FLAG_VISIBILITY);
            }

        };
    }

}
