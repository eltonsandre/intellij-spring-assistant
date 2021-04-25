package in.oneton.idea.spring.assistant.plugin.suggestion.handler;

import com.google.common.base.Splitter;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.misc.GenericUtil;
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
    public void handleInsert(@NotNull final InsertionContext context, @NotNull final LookupElement lookupElement) {
        if (!this.nextCharAfterSpacesAndQuotesIsColon(this.getStringAfterAutoCompletedValue(context))) {

            final var existingIndentation = this.getStringBeforeAutoCompletedValue(context, lookupElement);
            var lookupString = lookupElement.getLookupString();

            String pathDotDelimitedRootToLeaf = ((Suggestion) lookupElement.getObject()).getPathDotDelimitedRootToLeaf();

            var isArray = false;
            if (lookupString.equals(pathDotDelimitedRootToLeaf) && StringUtils.isNotEmpty(existingIndentation)) {
                String patternString = "^" + pathDotDelimitedRootToLeaf
                        .replace(GenericUtil.PROP_DOT, "\\.") + "\\[\\d+].*";

                var pattern = Pattern.compile(patternString);

                final List<String> delimitedPrefixList = Splitter.on("\n").splitToList(existingIndentation)
                        .stream()
                        .filter(prop -> prop.startsWith(pathDotDelimitedRootToLeaf))
                        .collect(Collectors.toList());

                if (CollectionUtils.isNotEmpty(delimitedPrefixList)) {
                    isArray = delimitedPrefixList.stream()
                            .allMatch(prop -> pattern.matcher(prop).matches());
                }

            }

            this.suggestionInsertStringAtCaret(context, lookupElement, isArray);
        }

    }

    private void suggestionInsertStringAtCaret(final InsertionContext context, final @NotNull LookupElement lookupElement, final boolean isArray) {
        Suggestion suggestion = (Suggestion) lookupElement.getObject();
        Module module = findModule(context);
        String suggestionWithCaret = this.getSuggestionReplacementWithCaret(module, suggestion, isArray);

        final String suggestionWithoutCaret = suggestionWithCaret.replace(CARET, StringUtils.EMPTY);

        this.deleteLookupTextAndRetrieveOldValue(context);

        insertStringAtCaret(context.getEditor(),
                suggestionWithoutCaret,
                false, true,
                this.getCaretIndex(suggestionWithCaret));
    }

    private void deleteLookupTextAndRetrieveOldValue(final InsertionContext context) {
        this.deleteLookupPlain(context);
    }

    private void deleteLookupPlain(final InsertionContext context) {
        final var document = context.getDocument();
        document.deleteString(context.getStartOffset(), context.getTailOffset());
        context.commitDocument();
    }

    @NotNull
    private String getSuggestionReplacementWithCaret(final Module module, final Suggestion suggestion, final boolean isArray) {
        final var builder = new StringBuilder();
        var i = 0;

        final List<? extends OriginalNameProvider> matchesTopFirst = suggestion.getMatchesForReplacement();
        do {
            OriginalNameProvider nameProvider = matchesTopFirst.get(i);
            builder.append(nameProvider.getOriginalName());
            if (i != matchesTopFirst.size() - 1) {
                builder.append(GenericUtil.PROP_DOT);
            }
            i++;
        } while (i < matchesTopFirst.size());

        String suffix = this.getPlaceholderSuffixWithCaret(module, suggestion, isArray);
        builder.append(suffix);
        return builder.toString();
    }

    @NotNull
    String getPlaceholderSuffixWithCaret(final Module module, final Suggestion suggestion, final boolean isArray) {
        final SuggestionNodeType nodeType = suggestion.getSuggestionNodeType(module);

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
        return context.getDocument().getText()
                .substring(context.getTailOffset());
    }

    private boolean nextCharAfterSpacesAndQuotesIsColon(final String string) {
        for (var i = 0; i < string.length(); i++) {
            final var charI = string.charAt(i);

            if (charI != ' ' && charI != '"') {
                return charI == '=';
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