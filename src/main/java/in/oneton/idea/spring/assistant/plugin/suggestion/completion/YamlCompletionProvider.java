package in.oneton.idea.spring.assistant.plugin.suggestion.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import gnu.trove.THashSet;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.truncateIdeaDummyIdentifier;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.findModule;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode.sanitise;
import static in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType.yaml;
import static java.util.Objects.requireNonNull;

class YamlCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override// TODO -> Refactor this method to reduce its Cognitive Complexity from 34 to the 15 allowed. [+18 locations
    protected void addCompletions(@NotNull final CompletionParameters completionParameters,
                                  final @NotNull ProcessingContext processingContext, @NotNull final CompletionResultSet resultSet) {

        PsiElement element = completionParameters.getPosition();
        if (element instanceof PsiComment) {
            return;
        }

        Project project = element.getProject();
        Module module = findModule(element);

        SuggestionService service = ServiceManager.getService(project, SuggestionService.class);

        if ((module == null || !service.canProvideSuggestions(module))) {
            return;
        }

        Set<String> siblingsToExclude = null;

        PsiElement elementContext = element.getContext();
        PsiElement parent = requireNonNull(elementContext).getParent();
        if (parent instanceof YAMLSequence) {
            // lets force user to create array element prefix before he can ask for suggestions
            return;
        }
        if (parent instanceof YAMLSequenceItem) {

            for (PsiElement child : parent.getParent().getChildren()) {
                if (child != parent) {

                    if (child instanceof YAMLSequenceItem) {
                        final YAMLValue value = ((YAMLSequenceItem) child).getValue();
                        if (value != null) {
                            siblingsToExclude = getNewIfNotPresent(siblingsToExclude);
                            siblingsToExclude.add(sanitise(value.getText()));
                        }

                    } else if (child instanceof YAMLKeyValue) {
                        siblingsToExclude = getNewIfNotPresent(siblingsToExclude);
                        siblingsToExclude.add(sanitise(((YAMLKeyValue) child).getKeyText()));
                    }
                }
            }

        } else if (parent instanceof YAMLMapping) {

            for (final PsiElement child : parent.getChildren()) {
                if (child != elementContext && child instanceof YAMLKeyValue) {
                    siblingsToExclude = getNewIfNotPresent(siblingsToExclude);
                    siblingsToExclude.add(sanitise(((YAMLKeyValue) child).getKeyText()));
                }
            }
        }

        List<LookupElementBuilder> suggestions;
        // For top level element, since there is no parent parentKeyValue would be null
        String queryWithDotDelimitedPrefixes = truncateIdeaDummyIdentifier(element);

        List<String> ancestralKeys = null;
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

        suggestions = service
                .findSuggestionsForQueryPrefix(module, yaml, element, ancestralKeys,
                        queryWithDotDelimitedPrefixes, siblingsToExclude);

        if (suggestions != null) {
            suggestions.forEach(resultSet::addElement);
        }
    }

    @NotNull
    private Set<String> getNewIfNotPresent(@Nullable Set<String> siblingsToExclude) {
        if (siblingsToExclude == null) {
            return new THashSet<>();
        }
        return siblingsToExclude;
    }

}
