package in.oneton.idea.spring.assistant.plugin.suggestion.completion;

import com.intellij.lang.HelpID;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.lang.properties.PropertiesBundle;
import com.intellij.lang.properties.parsing.PropertiesWordsScanner;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import in.oneton.idea.spring.assistant.plugin.misc.GenericUtil;
import in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @author zhoumengjie02, darren
 */
public class CustomPropertiesFindUsagesProvider implements FindUsagesProvider {

    @Nullable
    @Override
    public WordsScanner getWordsScanner() {
        return new PropertiesWordsScanner();
    }

    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
        return findElement(psiElement) != null;
    }

    @Nullable
    @Override
    public String getHelpId(@NotNull PsiElement psiElement) {
        return HelpID.FIND_OTHER_USAGES;
    }

    @NotNull
    @Override
    public String getType(@NotNull PsiElement element) {
        return PropertiesBundle.message("terms.property");
    }

    @NotNull
    @Override
    public String getDescriptiveName(@NotNull PsiElement element) {

        ReferenceProxyElement proxyElement = findElement(element);

        if (proxyElement == null) {
            return "Not Found";
        }

        return proxyElement.getTarget().getDocumentationForKey(PsiCustomUtil.findModule(element),
                proxyElement.getNodeNavigationPathDotDelimited());

    }

    @NotNull
    @Override
    public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        return getDescriptiveName(element);
    }

    private ReferenceProxyElement findElement(PsiElement myElement) {

        Project project = myElement.getProject();

        PsiFile file = myElement.getContainingFile();

        SuggestionService service = ServiceManager.getService(project, SuggestionService.class);

        String value = myElement.getText();

        Module module = PsiCustomUtil.findModule(myElement);

        List<SuggestionNode> matchedNodesFromRootTillLeaf =
                service.findMatchedNodesRootTillEnd(module, GenericUtil.getAncestralKey(value));

        if (matchedNodesFromRootTillLeaf != null) {

            SuggestionNode target = matchedNodesFromRootTillLeaf.get(matchedNodesFromRootTillLeaf.size() - 1);

            String targetNavigationPathDotDelimited =
                    matchedNodesFromRootTillLeaf.stream().map(v -> v.getNameForDocumentation(module))
                            .collect(Collectors.joining("."));

            return new ReferenceProxyElement(file.getManager(), file.getLanguage(),
                    targetNavigationPathDotDelimited, target, value);
        }

        return null;
    }
}