package in.oneton.idea.spring.assistant.plugin.suggestion.handler;

import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.CARET;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.UNDEFINED;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.UNKNOWN_CLASS;

public class AnnotationsKeyInsertHandler extends PropertiesKeyInsertHandler {


    @NotNull
    @Override
    String getPlaceholderSuffixWithCaret(final Module module, final Suggestion suggestion, final boolean isArray) {
        final SuggestionNodeType nodeType = suggestion.getSuggestionNodeType(module);

        if (UNDEFINED.equals(nodeType) || UNKNOWN_CLASS.equals(nodeType) || nodeType.representsLeaf()) {
            return CARET;
        } else if (nodeType.representsArrayOrCollection()) {
            return "[" + CARET + "]";
        } else if (isArray) {
            return "[" + CARET + "].";
        } else {
            return "." + CARET;
        }
    }


}
