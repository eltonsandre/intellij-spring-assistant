package in.oneton.idea.spring.assistant.plugin.suggestion.clazz;

import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ClassSuggestionNode extends SuggestionNode {
    @NotNull
    Suggestion buildSuggestionForKey(Module module, FileType fileType,
                                     List<SuggestionNode> matchesRootTillMe, int numOfAncestors);
}
