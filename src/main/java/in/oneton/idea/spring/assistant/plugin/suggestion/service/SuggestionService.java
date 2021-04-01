package in.oneton.idea.spring.assistant.plugin.suggestion.service;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public interface SuggestionService {
    static SuggestionService getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, SuggestionService.class);
    }

    void init(Project project);

    void reIndex(Project project);

    void reindex(Project project, Module[] modules);

    void reindex(Project project, Module module);

    @Nullable
    List<SuggestionNode> findMatchedNodesRootTillEnd(Module module,
                                                     List<String> containerElements);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean canProvideSuggestions(Module module);

    /**
     * @param module                        module to which these suggestions should be shown
     * @param fileType                      type of file requesting suggestion
     * @param element                       element on which search is triggered. Useful for cases like identifying chioces that were already selected incase of an enum, e.t.c
     * @param ancestralKeys                 hierarchy of element from where the suggestion is requested. i.e if in yml user is trying to get suggestions for `s.a` under `spring:\n\trabbitmq.listener:` element, then this value would ['spring', 'rabbitmq.listener']
     * @param queryWithDotDelimitedPrefixes query string user is trying to search for. In the above example, the value for this would be `s.a`
     * @param siblingsToExclude             siblings to exclude from search
     * @return results matching query string (without the containerElementsLeafToRoot). In the above example the values would be `simple.acknowledge-mode` & `simple.auto-startup`
     */
    @Nullable
    List<LookupElementBuilder> findSuggestionsForQueryPrefix(Module module,
                                                             FileType fileType, PsiElement element, @Nullable List<String> ancestralKeys,
                                                             String queryWithDotDelimitedPrefixes, @Nullable Set<String> siblingsToExclude);

}
