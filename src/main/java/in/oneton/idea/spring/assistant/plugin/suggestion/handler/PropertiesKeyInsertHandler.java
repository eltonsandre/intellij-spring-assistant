package in.oneton.idea.spring.assistant.plugin.suggestion.handler;

import com.google.common.base.Splitter;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.suggestion.OriginalNameProvider;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.intellij.openapi.editor.EditorModificationUtil.insertStringAtCaret;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.findModule;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.CARET;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.UNDEFINED;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.UNKNOWN_CLASS;

/**
 * @author zhoumengjie02, darren
 */
public class PropertiesKeyInsertHandler implements InsertHandler<LookupElement> {

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {
        if (!nextCharAfterSpacesAndQuotesIsColon(getStringAfterAutoCompletedValue(context))) {

            final String existingIndentation = getStringBeforeAutoCompletedValue(context, lookupElement);

            String lookupString = lookupElement.getLookupString();

            String pathDotDelimitedRootToLeaf = ((Suggestion) lookupElement.getObject()).getPathDotDelimitedRootToLeaf();

            boolean isArray = false;

            if (lookupString.equals(pathDotDelimitedRootToLeaf)
                    && StringUtils.isNotEmpty(existingIndentation)) {

                String patternString =
                        "^" + pathDotDelimitedRootToLeaf.replace(".", "\\.") + "\\[\\d+].*";

                Pattern pattern = Pattern.compile(patternString);

                List<String> splitToList = Splitter.on("\n").splitToList(existingIndentation);

                List<String> delimitedPrefixList = splitToList.stream()
                        .filter(prop -> prop.startsWith(pathDotDelimitedRootToLeaf))
                        .collect(Collectors.toList());

                if (CollectionUtils.isNotEmpty(delimitedPrefixList)) {

                    isArray = delimitedPrefixList.stream()
                            .allMatch(prop -> pattern.matcher(prop).matches());
                }

            }

            Suggestion suggestion = (Suggestion) lookupElement.getObject();
            Module module = findModule(context);
            String suggestionWithCaret =
                    getSuggestionReplacementWithCaret(module, suggestion, isArray);

            String suggestionWithoutCaret = suggestionWithCaret.replace(CARET, "");

            this.deleteLookupTextAndRetrieveOldValue(context);

            insertStringAtCaret(context.getEditor(),
                    suggestionWithoutCaret,
                    false, true,
                    getCaretIndex(suggestionWithCaret));
        }
    }

    private void deleteLookupTextAndRetrieveOldValue(InsertionContext context) {
        deleteLookupPlain(context);
    }

    private void deleteLookupPlain(InsertionContext context) {
        Document document = context.getDocument();
        document.deleteString(context.getStartOffset(), context.getTailOffset());
        context.commitDocument();
    }

    @NotNull
    private String getSuggestionReplacementWithCaret(Module module, Suggestion suggestion, boolean isArray) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        List<? extends OriginalNameProvider> matchesTopFirst = suggestion.getMatchesForReplacement();
        do {
            OriginalNameProvider nameProvider = matchesTopFirst.get(i);
            builder.append(nameProvider.getOriginalName());
            if (i != matchesTopFirst.size() - 1) {
                builder.append(".");
            }
            i++;
        } while (i < matchesTopFirst.size());
        String suffix = getPlaceholderSuffixWithCaret(module, suggestion, isArray);
        builder.append(suffix);
        return builder.toString();
    }

    @NotNull
    private String getPlaceholderSuffixWithCaret(Module module, Suggestion suggestion, boolean isArray) {
        SuggestionNodeType nodeType = suggestion.getSuggestionNodeType(module);
        if (nodeType == UNDEFINED || nodeType == UNKNOWN_CLASS) {
            return CARET;
        } else if (nodeType.representsLeaf()) {
            return "=" + CARET;
        } else if (nodeType.representsArrayOrCollection()) {
            return "[" + CARET + "]";
        } else if (isArray) {
            return "[" + CARET + "].";
        } else {
            // map or class
            return "." + CARET;
        }
    }

    private int getCaretIndex(final String suggestionWithCaret) {
        return suggestionWithCaret.indexOf(CARET);
    }

    @NotNull
    private String getStringAfterAutoCompletedValue(final InsertionContext context) {
        return context.getDocument().getText().substring(context.getTailOffset());
    }

    private boolean nextCharAfterSpacesAndQuotesIsColon(final String string) {
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (c != ' ' && c != '"') {
                return c == '=';
            }
        }
        return false;
    }

    @NotNull
    private String getStringBeforeAutoCompletedValue(final InsertionContext context,
                                                     final LookupElement item) {
        return context.getDocument().getText()
                .substring(0, context.getTailOffset() - item.getLookupString().length());
    }
}