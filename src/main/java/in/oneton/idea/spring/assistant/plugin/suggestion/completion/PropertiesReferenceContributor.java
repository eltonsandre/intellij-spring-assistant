package in.oneton.idea.spring.assistant.plugin.suggestion.completion;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.ResolveResult;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import in.oneton.idea.spring.assistant.plugin.misc.GenericUtil;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionService;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.truncateIdeaDummyIdentifier;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.findModule;
import static java.util.stream.Collectors.joining;

/**
 * @author zhoumengjie02, darren
 */
public class PropertiesReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {

        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyKeyImpl.class), new PsiReferenceProvider() {
            @NotNull
            @Override
            public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {

                PropertyKeyImpl property = (PropertyKeyImpl) element;

                String text = property.getText();

                TextRange range = new TextRange(0, text.length());

                return new PsiReference[]{new PropertiesReference(element, range)};
            }
        });

    }


    static class PropertiesReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

        public PropertiesReference(PsiElement element, TextRange rangeInElement) {
            super(element, rangeInElement);
        }

        @NotNull
        @Override
        public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {

            Project project = myElement.getProject();

            PsiFile file = myElement.getContainingFile();

            SuggestionService service = ServiceManager.getService(project, SuggestionService.class);

            String value = myElement.getText();

            Module module = findModule(myElement);

            List<String> ancestralKey = GenericUtil.getAncestralKey(value);

            if (CollectionUtils.isEmpty(ancestralKey)) {
                return new ResolveResult[0];
            }

            List<SuggestionNode> matchedNodesFromRootTillLeaf =
                    service.findMatchedNodesRootTillEnd(module, ancestralKey);

            if (matchedNodesFromRootTillLeaf != null) {

                SuggestionNode target = matchedNodesFromRootTillLeaf.get(matchedNodesFromRootTillLeaf.size() - 1);

                String targetNavigationPathDotDelimited =
                        matchedNodesFromRootTillLeaf.stream().map(v -> v.getNameForDocumentation(module))
                                .collect(joining("."));

                ReferenceProxyElement element = new ReferenceProxyElement(file.getManager(), file.getLanguage(),
                        targetNavigationPathDotDelimited, target, value);

                return new ResolveResult[]{new PsiElementResolveResult(element)};
            }

            return new ResolveResult[0];
        }

        @Nullable
        @Override
        public PsiElement resolve() {
            ResolveResult[] resolveResults = multiResolve(false);
            return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
        }

        @NotNull
        @Override
        public Object @NotNull [] getVariants() {

            Project project = myElement.getProject();

            SuggestionService service = ServiceManager.getService(project, SuggestionService.class);

            Module module = findModule(myElement);

            String origin = truncateIdeaDummyIdentifier(myElement);

            int pos = origin.lastIndexOf(".");

            String queryWithDotDelimitedPrefixes = origin;

            if (pos != -1) {
                if (pos == origin.length() - 1) {
                    queryWithDotDelimitedPrefixes = "";
                } else {
                    queryWithDotDelimitedPrefixes = origin.substring(pos + 1);
                }
            }

            List<LookupElementBuilder> suggestions =
                    service.findSuggestionsForQueryPrefix(module,
                            FileType.properties, myElement,
                            GenericUtil.getAncestralKey(origin), queryWithDotDelimitedPrefixes,
                            null);

            if (suggestions != null) {
                return suggestions.toArray();
            }
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }


    }
}