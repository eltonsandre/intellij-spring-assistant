package in.oneton.idea.spring.assistant.plugin.link;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import in.oneton.idea.spring.assistant.plugin.misc.AnnotationEnum;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LinkJavaGoToYml {

    private static final Logger log = Logger.getInstance(LinkJavaGoToYml.class);

    public static PsiElement[] toPropertieKey(@NotNull PsiElement sourceElement) {
        IElementType tokenType = ((PsiJavaToken) sourceElement).getTokenType();
        if (tokenType != JavaTokenType.STRING_LITERAL) {
            return new PsiElement[0];
        }

        var psiAnnotation = PsiTreeUtil.getParentOfType(sourceElement, PsiAnnotation.class);
        if (psiAnnotation == null) {
            log.debug("no annotation");
            return new PsiElement[0];
        }

        final var annotationName = psiAnnotation.getQualifiedName();
        if (AnnotationEnum.notContains(annotationName) || annotationName == null) {
            log.debug("no spring annotation");
            return new PsiElement[0];
        }

        final var project = sourceElement.getProject();
        final Collection<VirtualFile> files = FileTypeIndex.getFiles(YAMLFileType.YML, GlobalSearchScope.projectScope(project));

        if (CollectionUtils.isEmpty(files)) {
            return new PsiElement[0];
        }

        final var instance = PsiManager.getInstance(sourceElement.getProject());
        final String key =  qualifiedKey( AnnotationEnum.fromQualifiedName(annotationName).isHasPlaceholder(),
                sourceElement.getText());

        final List<PsiElement> result = new ArrayList<>(files.size());

        files.forEach(file -> {
            final var yamlFile = (YAMLFile) Objects.requireNonNull(instance.findFile(file));
            final YAMLKeyValue qualifiedKeyInFile = YAMLUtil.getQualifiedKeyInFile(yamlFile, key.split("\\."));

            if (Objects.nonNull(qualifiedKeyInFile)) {
                result.add(new ThisYAMLKeyValueImpl(qualifiedKeyInFile.getNode()));
            }
        });

        final var psiElements = new PsiElement[result.size()];
        return result.toArray(psiElements);
    }


    private static String qualifiedKey(boolean isHasPlaceholder, String key){
        if (isHasPlaceholder) {
            return LinkUtils.getKeyToPlaceholder(key);
        } else {
            return key.substring(1, key.length() - 1);
        }
    }

}
