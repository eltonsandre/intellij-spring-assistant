package in.oneton.idea.spring.assistant.plugin.suggestion.completion;


import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import org.jetbrains.annotations.Nullable;

public class PropertiesDocumentationProvider extends AbstractDocumentationProvider {

    @Override
    public String getQuickNavigateInfo(PsiElement element, @Nullable PsiElement originalElement) {

        if (element instanceof ReferenceProxyElement) {

            ReferenceProxyElement proxyElement =
                    (ReferenceProxyElement) element;

            SuggestionNode target = (SuggestionNode) proxyElement.getTarget();
            boolean requestedForTargetValue = proxyElement.isRequestedForTargetValue();

            // Only group & leaf are expected to have documentation
            if (target != null && target.supportsDocumentation()) {

                Module module = PsiCustomUtil.findModule(element);

                if (requestedForTargetValue) {
                    return target.getDocumentationForValue(module,
                            proxyElement.getNodeNavigationPathDotDelimited(), proxyElement.getValue());
                } else {
                    return target.getDocumentationForKey(module,
                            proxyElement.getNodeNavigationPathDotDelimited());
                }
            }
        }
        return super.getQuickNavigateInfo(element, originalElement);
    }
}
