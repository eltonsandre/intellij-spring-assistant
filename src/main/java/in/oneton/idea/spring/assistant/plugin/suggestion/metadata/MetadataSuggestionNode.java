package in.oneton.idea.spring.assistant.plugin.suggestion.metadata;

import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;

public abstract class MetadataSuggestionNode implements SuggestionNode {

    //  static MetadataSuggestionNode NULL_NODE = null;

    /**
     * If {@code matchAllSegments} is true, all {@code pathSegments} starting from {@code pathSegmentStartIndex} will be attempted to be matched. If a result is found, it will be returned. Else null
     * Else, method should attempt to match as deep as it can & return that match
     *
     * @param pathSegments          path segments to match against
     * @param pathSegmentStartIndex index within {@code pathSegments} to start match from
     * @param matchAllSegments      should all {@code pathSegments} be matched or not
     * @return leaf suggestion node that matches path segments starting with {@code pathSegmentStartIndex} or null otherwise
     */
    @Contract("_, _, true -> null; _, _, false -> !null")
    public abstract MetadataSuggestionNode findDeepestMetadataNode(String[] pathSegments,
                                                                   int pathSegmentStartIndex, boolean matchAllSegments);

    @Override
    public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                  List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
                                                                  int querySegmentPrefixStartIndex) {
        return findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
                querySegmentPrefixes, querySegmentPrefixStartIndex, null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                               List<SuggestionNode> matchesRootTillMe, String prefix) {
        return findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix, null);
    }

    public void addRefCascadeTillRoot(String containerPath) {
        MetadataSuggestionNode node = this;
        do {
            if (node.getBelongsTo().contains(containerPath)) {
                break;
            }
            node.getBelongsTo().add(containerPath);
            node = node.getParent();
        } while (node != null && !node.isRoot());
    }

    public abstract Set<String> getBelongsTo();

    /**
     * @param containerPath Represents path to the metadata file container
     * @return true if no children left & this item does not belong to any other source
     */
    public abstract boolean removeRefCascadeDown(String containerPath);

    /**
     * During reindexing lets make sure that we refresh references to proxies so that subsequent searches would be faster
     *
     * @param module idea module
     */
    public abstract void refreshClassProxy(Module module);

    protected abstract String getName();

    @NotNull
    public abstract String getOriginalName();

    @Nullable
    protected abstract MetadataNonPropertySuggestionNode getParent();

    protected abstract boolean isRoot();

    /**
     * @return whether the node expects any children or not
     */
    public abstract boolean isGroup();

    /**
     * @return whether the node expects any children or not
     */
    public abstract boolean isProperty();

    protected abstract boolean hasOnlyOneChild();

    public int numOfHopesToRoot() {
        int hopCount = 0;
        MetadataSuggestionNode current = getParent();
        while (current != null) {
            hopCount++;
            current = current.getParent();
        }
        return hopCount;
    }

    public String getPathFromRoot() {
        Stack<String> leafTillRoot = new Stack<>();
        MetadataSuggestionNode current = this;
        do {
            leafTillRoot.push(current.getOriginalName());
            current = current.getParent();
        } while (current != null);
        return String.join(".", leafTillRoot);
    }

    @Override
    public boolean supportsDocumentation() {
        return isGroup() || isProperty();
    }

    @Nullable
    @Override
    public String getNameForDocumentation(Module module) {
        return getSuggestionNodeType(module).representsArrayOrCollection() ?
                getOriginalName() + "[]" :
                getOriginalName();
    }

    public abstract String toTree();

}
