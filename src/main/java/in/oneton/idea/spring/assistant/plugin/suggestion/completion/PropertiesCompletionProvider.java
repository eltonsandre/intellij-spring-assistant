package in.oneton.idea.spring.assistant.plugin.suggestion.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtilCore;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import in.oneton.idea.spring.assistant.plugin.misc.GenericUtil;
import in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

class PropertiesCompletionProvider extends CompletionProvider<CompletionParameters> {

    private static final String PROP_DOT = ".";

    @Override
    protected void addCompletions(@NotNull final CompletionParameters completionParameters,
                                  final @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {

        PsiElement element = completionParameters.getPosition();

        if (element instanceof PsiComment) {
            return;
        }

        //中途插入不提示
        boolean middle = element.getText().endsWith(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED);

        if (!middle) {
            return;
        }

        Module module = PsiCustomUtil.findModule(element);

        Project project = element.getProject();

        SuggestionService service = ServiceManager.getService(project, SuggestionService.class);

        if (module == null || !service.canProvideSuggestions(module)) {
            return;
        }

        PsiElement elementContext = element.getContext();

        String text = GenericUtil.truncateIdeaDummyIdentifier(Objects.requireNonNull(elementContext).getText());

        List<LookupElementBuilder> suggestions;
        // For top level element, since there is no parent keyValue would be null
        String origin = GenericUtil.truncateIdeaDummyIdentifier(element);

        int pos = origin.lastIndexOf(PROP_DOT);

        String queryWithDotDelimitedPrefixes = origin;

        if (pos != -1) {
            if (pos == origin.length() - 1) {
                queryWithDotDelimitedPrefixes = "";
            } else {
                queryWithDotDelimitedPrefixes = origin.substring(pos + 1);
            }
        }

        //.后缀前面的数据
        List<String> ancestralKeys = GenericUtil.getAncestralKey(text);

        suggestions =
                service.findSuggestionsForQueryPrefix(module, FileType.properties, element, ancestralKeys,
                        queryWithDotDelimitedPrefixes, null);

        resultSet = resultSet.withPrefixMatcher(queryWithDotDelimitedPrefixes);

        if (suggestions != null) {
            suggestions.forEach(resultSet::addElement);
        }
    }
}
