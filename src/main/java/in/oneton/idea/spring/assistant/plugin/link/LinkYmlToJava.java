package in.oneton.idea.spring.assistant.plugin.link;

import com.google.common.collect.Lists;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.jvm.annotation.JvmAnnotationArrayValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import in.oneton.idea.spring.assistant.plugin.misc.AnnotationEnum;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLPsiElement;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class LinkYmlToJava {


    public static final PsiElement[] DEFAULT_RESULT = new PsiElement[0];

    public static PsiElement[] toJava(@NotNull PsiElement sourceElement) {
        String configFullName = YAMLUtil.getConfigFullName(PsiTreeUtil.getParentOfType(sourceElement, YAMLPsiElement.class));
        Pair<PsiElement, String> value = YAMLUtil.getValue((YAMLFile) sourceElement.getContainingFile(), configFullName.split("\\."));

        if (Objects.isNull(value) || !(value.getFirst() instanceof YAMLPlainTextImpl)) {
            return DEFAULT_RESULT;
        }

        Project project = sourceElement.getProject();
        Collection<VirtualFile> files = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project));

        if (CollectionUtils.isEmpty(files)) {
            return DEFAULT_RESULT;
        }
        ArrayList<PsiElement> result = Lists.newArrayList();
        for (VirtualFile file : files) {
            final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

            Collection<PsiAnnotation> childrenOfType = PsiTreeUtil.findChildrenOfType(psiFile, PsiAnnotation.class);
            if (CollectionUtils.isEmpty(childrenOfType)) {
                continue;
            }

            for (PsiAnnotation psiAnnotation : childrenOfType) {
                if (!AnnotationEnum.contains(psiAnnotation.getQualifiedName())) {
                    continue;
                }

                PsiNameValuePair[] attributes1 = psiAnnotation.getParameterList().getAttributes();
                for (PsiNameValuePair psiNameValuePair : attributes1) {
                    JvmAnnotationAttributeValue attributeValue = psiNameValuePair.getAttributeValue();

                    if (checkEquals(configFullName, attributeValue)) {
                        result.add(new ThisPsiAnnotation(psiAnnotation.getNode()));
                        break;
                    }
                }
            }
        }

        final PsiElement[] psiElements = new PsiElement[result.size()];
        return result.toArray(psiElements);
    }

    private static boolean checkEquals(final String configFullName, final JvmAnnotationAttributeValue constantValue) {
        if (constantValue instanceof JvmAnnotationConstantValue) {
            final Object constantValue1 = ((JvmAnnotationConstantValue) constantValue).getConstantValue();

            String valueKey = Objects.requireNonNull(constantValue1).toString();

            if (LinkUtils.isPlaceholderContainer(valueKey)) {
                valueKey = LinkUtils.getKeyToPlaceholder(valueKey);
            }

            return Objects.equals(valueKey, configFullName);
        }

        if (constantValue instanceof JvmAnnotationArrayValue) {
            JvmAnnotationArrayValue attributeValue1 = (JvmAnnotationArrayValue) constantValue;
            List<JvmAnnotationAttributeValue> annotationArrayValues = attributeValue1.getValues();

            for (JvmAnnotationAttributeValue value : annotationArrayValues) {
                if (checkEquals(configFullName, value)) {
                    return true;
                }
            }

        }

        return false;
    }

}
