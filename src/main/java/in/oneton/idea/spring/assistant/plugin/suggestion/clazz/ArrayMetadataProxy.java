package in.oneton.idea.spring.assistant.plugin.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiArrayType;
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
import java.util.TreeSet;

import static com.intellij.util.containers.ContainerUtil.isEmpty;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.newListWithMembers;
import static in.oneton.idea.spring.assistant.plugin.suggestion.clazz.ClassSuggestionNodeFactory.newMetadataProxy;
import static java.util.stream.Collectors.toCollection;

public class ArrayMetadataProxy implements MetadataProxy {

    @NotNull
    private final PsiArrayType type;
    @Nullable
    private final MetadataProxy delegate;

    ArrayMetadataProxy(Module module, @NotNull PsiArrayType type) {
        this.type = type;
        delegate = newMetadataProxy(module, type.getComponentType());
    }

    @Nullable
    @Override
    public SuggestionDocumentationHelper findDirectChild(Module module, String pathSegment) {
        return doWithDelegateAndReturn(delegate -> delegate.findDirectChild(module, pathSegment), null);
    }

    @Nullable
    @Override
    public Collection<? extends SuggestionDocumentationHelper> findDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix) {
        return findDirectChildrenForQueryPrefix(module, querySegmentPrefix, null);
    }

    @Nullable
    @Override
    public Collection<? extends SuggestionDocumentationHelper> findDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix, @Nullable Set<String> siblingsToExclude) {
        // TODO: Should each element be wrapped inside Iterale Suggestion element?
        return doWithDelegateAndReturn(delegate -> delegate
                .findDirectChildrenForQueryPrefix(module, querySegmentPrefix, siblingsToExclude), null);
    }

    @Nullable
    @Override
    public List<SuggestionNode> findDeepestSuggestionNode(Module module,
                                                          List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                          int pathSegmentStartIndex) {
        return doWithDelegateAndReturn(delegate -> {
            String pathSegment = pathSegments[pathSegmentStartIndex];
            SuggestionDocumentationHelper directChildKeyMatch =
                    delegate.findDirectChild(module, pathSegment);
            if (directChildKeyMatch != null) {
                // TODO: Need to identify a better mechanism than this dirty way. Probably use ClassSuggestionNode as return type for findDirectChildrenForQueryPrefix
                // since we are in an iterable(multiple values), keys would be requested, only if the object we are referring is not a leaf => GenericClassWrapper
                assert directChildKeyMatch instanceof SuggestionNode;
                matchesRootTillParentNode
                        .add(new IterableKeySuggestionNode((SuggestionNode) directChildKeyMatch));
                boolean lastPathSegment = pathSegmentStartIndex == pathSegments.length - 1;
                if (lastPathSegment) {
                    return matchesRootTillParentNode;
                } else {
                    return delegate.findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
                            pathSegmentStartIndex);
                }
            }
            return null;
        }, null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                  List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                                  String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
        return findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillParentNode,
                numOfAncestors, querySegmentPrefixes, querySegmentPrefixStartIndex, null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                  List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                                  String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
                                                                  @Nullable Set<String> siblingsToExclude) {
        return doWithDelegateAndReturn(delegate -> {
            String querySegmentPrefix = querySegmentPrefixes[querySegmentPrefixStartIndex];
            Collection<? extends SuggestionDocumentationHelper> matches =
                    delegate.findDirectChildrenForQueryPrefix(module, querySegmentPrefix, siblingsToExclude);
            if (!isEmpty(matches)) {
                return matches.stream().map(helper -> {
                    // TODO: Need to identify a better mechanism than this dirty way. Probably use ClassSuggestionNode as return type for findDirectChildrenForQueryPrefix
                    // since we are in an iterable(multiple values), keys would be requested, only if the object we are referring is not a leaf => GenericClassWrapper
                    assert helper instanceof SuggestionNode;
                    List<SuggestionNode> rootTillMe = newListWithMembers(matchesRootTillParentNode,
                            new IterableKeySuggestionNode((SuggestionNode) helper));
                    return helper.buildSuggestionForKey(module, fileType, rootTillMe, numOfAncestors);
                }).collect(toCollection(TreeSet::new));
            }
            return null;
        }, null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                               List<SuggestionNode> matchesRootTillMe, String prefix) {
        return findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix, null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                               List<SuggestionNode> matchesRootTillMe, String prefix,
                                                               @Nullable Set<String> siblingsToExclude) {
        return doWithDelegateAndReturn(delegate -> delegate
                .findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix,
                        siblingsToExclude), null);
    }

    @Nullable
    @Override
    public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                           String originalValue) {
        return doWithDelegateAndReturn(delegate -> delegate
                .getDocumentationForValue(module, nodeNavigationPathDotDelimited, originalValue), null);
    }

    @Override
    public boolean isLeaf(Module module) {
        return doWithDelegateAndReturn(delegate -> delegate.isLeaf(module), true);
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType(Module module) {
        return SuggestionNodeType.ARRAY;
    }

    @Nullable
    @Override
    public PsiType getPsiType(Module module) {
        return type;
    }

    @Override
    public boolean targetRepresentsArray() {
        return true;
    }

    @Override
    public boolean targetClassRepresentsIterable(Module module) {
        return false;
    }

    private <T> T doWithDelegateAndReturn(
            MetadataProxyInvokerWithReturnValue<T> targetInvokerWithReturnValue, T defaultReturnValue) {
        if (delegate != null) {
            return targetInvokerWithReturnValue.invoke(delegate);
        }
        return defaultReturnValue;
    }

    private void doWithDelegate(MetadataProxyInvoker targetInvoker) {
        if (delegate != null) {
            targetInvoker.invoke(delegate);
        }
    }

}
