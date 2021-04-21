package in.oneton.idea.spring.assistant.plugin.link;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
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

public class LinkJavaGoToYml {

    private static final Logger log = Logger.getInstance(LinkJavaGoToYml.class);

    public static PsiElement[] toPropertieKey(@NotNull PsiElement sourceElement) {
        IElementType tokenType = ((PsiJavaToken) sourceElement).getTokenType();
        if (tokenType != JavaTokenType.STRING_LITERAL) {
            return new PsiElement[0];
        }

        var psiAnnotation = PsiTreeUtil.getParentOfType(sourceElement, PsiAnnotation.class);
        assert psiAnnotation != null;

        final var annotationName = psiAnnotation.getQualifiedName();
        if (AnnotationEnum.notContains(annotationName)) {
            return new PsiElement[0];
        }

        assert annotationName != null;
        final var annotationEnum = AnnotationEnum.fromQualifiedName(annotationName);

        String key = sourceElement.getText();

        if (annotationEnum.isHasPlaceholder()) {
            key = LinkUtils.getKeyToPlaceholder(key);
        } else {
            key = key.substring(1, key.length() - 1);
        }

        Project project = sourceElement.getProject();
        Collection<VirtualFile> files = FileTypeIndex.getFiles(YAMLFileType.YML, GlobalSearchScope.projectScope(project));

        if (CollectionUtils.isEmpty(files)) {
            return new PsiElement[0];
        }

        List<PsiElement> result = new ArrayList<>(files.size());

        final PsiManager instance = PsiManager.getInstance(sourceElement.getProject());

        for (VirtualFile file : files) {
            final YAMLFile yamlFile = (YAMLFile) Objects.requireNonNull(instance.findFile(file));
            final YAMLKeyValue qualifiedKeyInFile = YAMLUtil.getQualifiedKeyInFile(yamlFile, key.split("\\."));

            if (Objects.nonNull(qualifiedKeyInFile)) {
                result.add(new ThisYAMLKeyValueImpl(qualifiedKeyInFile.getNode()));
            }
        }

        final PsiElement[] psiElements = new PsiElement[result.size()];
        return result.toArray(psiElements);
    }

}
