package in.oneton.idea.spring.assistant.plugin.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.SuggestionDocumentationHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Since the class documentation is tied to the actual class being available in the classpath of the target project the plugin is operating on,
 * it is possible that the target class is no longer available in the classpath due to change of dependencies. So, lets always access target via the proxy so that we dont have to worry about whether the target class exists in classpath or not
 */
public interface MetadataProxy {

    @Nullable
    SuggestionDocumentationHelper findDirectChild(Module module, String pathSegment);

    @Nullable
    Collection<? extends SuggestionDocumentationHelper> findDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix);

    @Nullable
    Collection<? extends SuggestionDocumentationHelper> findDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix, @Nullable Set<String> siblingsToExclude);

    @Nullable
    List<SuggestionNode> findDeepestSuggestionNode(Module module,
                                                   List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                   int pathSegmentStartIndex);

    @Nullable
    SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                           List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                           String[] querySegmentPrefixes, int querySegmentPrefixStartIndex);

    @Nullable
    SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                           List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                           String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
                                                           @Nullable Set<String> siblingsToExclude);

    @Nullable
    SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                        List<SuggestionNode> matchesRootTillMe, String prefix);

    @Nullable
    SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                        List<SuggestionNode> matchesRootTillMe, String prefix,
                                                        @Nullable Set<String> siblingsToExclude);

    @Nullable
    String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                    String originalValue);

    boolean isLeaf(Module module);

    @NotNull
    SuggestionNodeType getSuggestionNodeType(Module module);

    @Nullable
    PsiType getPsiType(Module module);

    boolean targetRepresentsArray();

    boolean targetClassRepresentsIterable(Module module);

}
