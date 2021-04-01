package in.oneton.idea.spring.assistant.plugin.suggestion.completion;

import com.intellij.lang.Language;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.LightElement;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionService;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.lang.java.JavaLanguage.INSTANCE;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.truncateIdeaDummyIdentifier;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.findModule;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

public class YamlDocumentationProvider extends AbstractDocumentationProvider {
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (element instanceof DocumentationProxyElement) {
            DocumentationProxyElement proxyElement = (DocumentationProxyElement) element;
            DocumentationProvider target = proxyElement.target;

            // Intermediate nodes will not have documentation
            if (target != null && target.supportsDocumentation()) {
                Module module = findModule(element);
                if (proxyElement.requestedForTargetValue) {
                    return target
                            .getDocumentationForValue(module, proxyElement.nodeNavigationPathDotDelimited,
                                    proxyElement.value);
                } else {
                    return target.getDocumentationForKey(module, proxyElement.nodeNavigationPathDotDelimited);
                }
            }
        }
        return super.generateDoc(element, originalElement);
    }

    /*
     * This will called if the user tries to lookup documentation for the choices being shown (ctrl+q within suggestion dialog)
     */
    @Override
    public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object,
                                                           @Nullable PsiElement element) {
        if (object instanceof Suggestion) {
            Suggestion suggestion = (Suggestion) object;
            return new DocumentationProxyElement(psiManager, INSTANCE, suggestion.getFullPath(),
                    suggestion.getMatchesTopFirst().get(suggestion.getMatchesTopFirst().size() - 1),
                    suggestion.isForValue(), suggestion.getSuggestionToDisplay());
        }
        return super.getDocumentationElementForLookupItem(psiManager, object, element);
    }

    @Nullable
    @Override
    public PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file,
                                                    @Nullable PsiElement element, int targetOffset) {
        if (element != null) {
            List<SuggestionNode> matchedNodesFromRootTillLeaf;
            boolean requestedForTargetValue = false;

            SuggestionService suggestionService =
                    ServiceManager.getService(element.getProject(), SuggestionService.class);

            Project project = element.getProject();
            Module module = findModule(element);

            List<String> ancestralKeys = null;
            PsiElement elementContext = element.getContext();
            PsiElement context = elementContext;
            do {
                if (context instanceof YAMLKeyValue) {
                    if (ancestralKeys == null) {
                        ancestralKeys = new ArrayList<>();
                    }
                    ancestralKeys.add(0, truncateIdeaDummyIdentifier(((YAMLKeyValue) context).getKeyText()));
                }
                context = requireNonNull(context).getParent();
            } while (context != null);

            String value = null;
            if (elementContext instanceof YAMLKeyValue) {
                value = truncateIdeaDummyIdentifier(((YAMLKeyValue) elementContext).getKeyText());
                requestedForTargetValue = false;
            } else if (elementContext instanceof YAMLPlainTextImpl) {
                value = truncateIdeaDummyIdentifier(element.getText());
                requestedForTargetValue = true;
            }

            if (ancestralKeys != null) {
                matchedNodesFromRootTillLeaf =
                        suggestionService.findMatchedNodesRootTillEnd(module, ancestralKeys);
                if (matchedNodesFromRootTillLeaf != null) {
                    SuggestionNode target =
                            matchedNodesFromRootTillLeaf.get(matchedNodesFromRootTillLeaf.size() - 1);
                    String targetNavigationPathDotDelimited =
                            matchedNodesFromRootTillLeaf.stream().map(v -> v.getNameForDocumentation(module))
                                    .collect(joining("."));
                    return new DocumentationProxyElement(file.getManager(), file.getLanguage(),
                            targetNavigationPathDotDelimited, target, requestedForTargetValue, value);
                }
            }
        }
        return super.getCustomDocumentationElement(editor, file, element, targetOffset);
    }

    @ToString(of = "nodeNavigationPathDotDelimited")
    private static class DocumentationProxyElement extends LightElement {
        private final DocumentationProvider target;
        private final boolean requestedForTargetValue;
        @Nullable
        private final String value;
        private final String nodeNavigationPathDotDelimited;

        DocumentationProxyElement(@NotNull final PsiManager manager, @NotNull final Language language,
                                  String nodeNavigationPathDotDelimited, @NotNull final DocumentationProvider target,
                                  boolean requestedForTargetValue, @Nullable String value) {
            super(manager, language);
            this.nodeNavigationPathDotDelimited = nodeNavigationPathDotDelimited;
            this.target = target;
            this.requestedForTargetValue = requestedForTargetValue;
            this.value = value;
        }

        @Override
        public String getText() {
            return nodeNavigationPathDotDelimited;
        }
    }

}
