package in.oneton.idea.spring.assistant.plugin.link;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.impl.source.tree.java.PsiAnnotationImpl;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public final class ThisPsiAnnotation extends PsiAnnotationImpl implements PsiAnnotation {

    @Setter
    private String presentableText;

    public ThisPsiAnnotation(final ASTNode node) {
        super(node);
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            private static final int PRESENTABLE_TEXT_LENGTH = 100;

            @Override
            public String getLocationString() {
                return ThisPsiAnnotation.this.getContainingFile().getName();
            }

            @Override
            public String getPresentableText() {
                if (ThisPsiAnnotation.this.presentableText == null || ThisPsiAnnotation.this.presentableText.isEmpty()) {
                    return LinkUtils.truncateStringWithEllipsis(ThisPsiAnnotation.this.getText(), PRESENTABLE_TEXT_LENGTH);
                }
                return LinkUtils.truncateStringWithEllipsis(ThisPsiAnnotation.this.presentableText, PRESENTABLE_TEXT_LENGTH);
            }

            @Override
            public @Nullable Icon getIcon(final boolean unused) {
                return ThisPsiAnnotation.this.getIcon(Iconable.ICON_FLAG_VISIBILITY);
            }

        };
    }

}
