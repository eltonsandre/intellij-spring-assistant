package in.oneton.idea.spring.assistant.plugin.link;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;


public class LinkPropertiesKey implements GotoDeclarationHandler {


    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
        if (sourceElement == null) {
            return null;
        }

        if (sourceElement instanceof PsiJavaToken) {
            return LinkJavaGoToYml.toPropertieKey(sourceElement);
        }

        if (YAMLLanguage.INSTANCE.is(sourceElement.getLanguage())) {
            return LinkYmlToJava.toJava(sourceElement);
        }

        return LinkYmlToJava.DEFAULT_RESULT;
    }

}
