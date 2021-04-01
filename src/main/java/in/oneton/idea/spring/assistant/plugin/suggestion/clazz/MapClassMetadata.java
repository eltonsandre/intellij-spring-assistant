package in.oneton.idea.spring.assistant.plugin.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.SuggestionDocumentationHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.intellij.util.containers.ContainerUtil.isEmpty;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.newListWithMembers;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.getTypeParameters;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.isValidType;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.MAP;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.UNKNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.suggestion.clazz.ClassSuggestionNodeFactory.newMetadataProxy;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

public class MapClassMetadata extends ClassMetadata {

    @NotNull
    private final PsiClassType type;

    @Nullable
    private PsiType keyType;
    @Nullable
    private PsiType valueType;
    @Nullable
    private MetadataProxy keyMetadataProxy;
    @Nullable
    private MetadataProxy valueMetadataProxy;

    MapClassMetadata(@NotNull PsiClassType type) {
        this.type = type;
    }

    @Override
    protected void init(Module module) {
        init(module, type);
    }

    @Nullable
    @Override
    protected SuggestionDocumentationHelper doFindDirectChild(Module module, String pathSegment) {
        return doWithKeyDelegateOrReturnNull(delegate -> delegate.findDirectChild(module, pathSegment));
    }

    @Override
    protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix) {
        return doFindDirectChildrenForQueryPrefix(module, querySegmentPrefix, null);
    }

    @Override
    protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix, @Nullable Set<String> siblingsToExclude) {
        return doWithKeyDelegateOrReturnNull(delegate -> delegate
                .findDirectChildrenForQueryPrefix(module, querySegmentPrefix, siblingsToExclude));
    }

    @Nullable
    @Override
    protected List<SuggestionNode> doFindDeepestSuggestionNode(Module module,
                                                               List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                               int pathSegmentStartIndex) {
        return doWithKeyDelegateOrReturnNull(keyProxy -> {
            String pathSegment = pathSegments[pathSegmentStartIndex];
            SuggestionDocumentationHelper directChildKeyMatch =
                    keyProxy.findDirectChild(module, pathSegment);
            if (directChildKeyMatch != null) {
                matchesRootTillParentNode.add(new MapKeySuggestionNode(directChildKeyMatch));
                boolean lastPathSegment = pathSegmentStartIndex == pathSegments.length - 1;
                if (lastPathSegment) {
                    return matchesRootTillParentNode;
                } else {
                    return doWithValueDelegateOrReturnNull(valueProxy -> valueProxy
                            .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
                                    pathSegmentStartIndex + 1));
                }
            }
            return null;
        });
    }

    @Nullable
    @Override
    protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
                                                                       FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                                       String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
        return doFindKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillParentNode,
                numOfAncestors, querySegmentPrefixes, querySegmentPrefixStartIndex, null);
    }

    @Nullable
    @Override
    protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
                                                                       FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                                       String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
                                                                       @Nullable Set<String> siblingsToExclude) {
        boolean lastPathSegment = querySegmentPrefixStartIndex == querySegmentPrefixes.length - 1;
        if (lastPathSegment) {
            return doWithKeyDelegateOrReturnNull(proxy -> {
                String querySegmentPrefix = querySegmentPrefixes[querySegmentPrefixStartIndex];
                Collection<? extends SuggestionDocumentationHelper> matches =
                        proxy.findDirectChildrenForQueryPrefix(module, querySegmentPrefix, siblingsToExclude);
                if (!isEmpty(matches)) {
                    return matches.stream().map(helper -> helper.buildSuggestionForKey(module, fileType,
                            newListWithMembers(matchesRootTillParentNode, new MapKeySuggestionNode(helper)),
                            numOfAncestors)).collect(toCollection(TreeSet::new));
                }
                return null;
            });
        }
        return null;
    }

    @Override
    protected SortedSet<Suggestion> doFindValueSuggestionsForPrefix(Module module, FileType fileType,
                                                                    List<SuggestionNode> matchesRootTillMe, String prefix,
                                                                    @Nullable Set<String> siblingsToExclude) {
        return doWithValueDelegateOrReturnNull(proxy -> {
            boolean valueIsLeaf = proxy.isLeaf(module);
            assert valueIsLeaf;
            return proxy.findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix,
                    siblingsToExclude);
        });
    }

    public SortedSet<Suggestion> findChildKeySuggestionForQueryPrefix(Module module,
                                                                      FileType fileType, List<SuggestionNode> matchesRootTillMe, int numOfAncestors,
                                                                      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
                                                                      @Nullable Set<String> siblingsToExclude) {
        return doWithValueDelegateOrReturnNull(proxy -> proxy
                .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
                        querySegmentPrefixes, querySegmentPrefixStartIndex, siblingsToExclude));
    }

    @Nullable
    @Override
    protected String doGetDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                                String originalValue) {
        return doWithValueDelegateOrReturnNull(
                proxy -> proxy.getDocumentationForValue(module, nodeNavigationPathDotDelimited,
                        originalValue));
    }

    @Override
    public boolean doCheckIsLeaf(Module module) {
        return false;
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType() {
        return MAP;
    }

    @Nullable
    @Override
    public PsiType getPsiType(Module module) {
        return type;
    }

    private void init(Module module, PsiClassType type) {
        if (isValidType(type)) {
            Map<PsiTypeParameter, PsiType> typeParameterToResolvedType = getTypeParameters(type);
            assert typeParameterToResolvedType != null;
            Set<PsiTypeParameter> typeParameterKetSet = typeParameterToResolvedType.keySet();
            Optional<PsiTypeParameter> keyTypeParam =
                    typeParameterKetSet.stream().filter(v -> requireNonNull(v.getName()).equals("K"))
                            .findFirst();
            Optional<PsiTypeParameter> valueTypeParam =
                    typeParameterKetSet.stream().filter(v -> requireNonNull(v.getName()).equals("V"))
                            .findFirst();
            if (keyTypeParam.isPresent()) {
                this.keyType = typeParameterToResolvedType.get(keyTypeParam.get());
                if (this.keyType != null) {
                    keyMetadataProxy = newMetadataProxy(module, this.keyType);
                }
            }

            if (valueTypeParam.isPresent()) {
                this.valueType = typeParameterToResolvedType.get(valueTypeParam.get());
                if (this.valueType != null) {
                    valueMetadataProxy = newMetadataProxy(module, this.valueType);
                }
            }
        }
    }

    @Nullable
    public PsiType getKeyType() {
        return keyType;
    }

    @Nullable
    public PsiType getValueType() {
        return valueType;
    }

    private <T> T doWithKeyDelegateOrReturnNull(
            MetadataProxyInvokerWithReturnValue<T> targetInvokerWithReturnValue) {
        if (keyMetadataProxy != null) {
            return targetInvokerWithReturnValue.invoke(keyMetadataProxy);
        }
        return null;
    }

    private <T> T doWithValueDelegateOrReturnNull(
            MetadataProxyInvokerWithReturnValue<T> targetInvokerWithReturnValue) {
        return doWithValueDelegateAndReturn(targetInvokerWithReturnValue, null);
    }

    private <T> T doWithValueDelegateAndReturn(
            MetadataProxyInvokerWithReturnValue<T> targetInvokerWithReturnValue, T defaultReturnValue) {
        if (valueMetadataProxy != null) {
            return targetInvokerWithReturnValue.invoke(valueMetadataProxy);
        }
        return defaultReturnValue;
    }


    class MapKeySuggestionNode implements SuggestionNode {

        private final SuggestionDocumentationHelper helper;

        MapKeySuggestionNode(SuggestionDocumentationHelper helper) {
            this.helper = helper;
        }

        @Nullable
        @Override
        public List<SuggestionNode> findDeepestSuggestionNode(Module module,
                                                              List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                              int pathSegmentStartIndex) {
            throw new IllegalAccessError("Should never be called");
        }

        @Nullable
        @Override
        public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                      List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
                                                                      int querySegmentPrefixStartIndex) {
            return findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
                    querySegmentPrefixes, querySegmentPrefixStartIndex, null);
        }

        @Nullable
        @Override
        public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                      List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
                                                                      int querySegmentPrefixStartIndex, @Nullable Set<String> siblingsToExclude) {
            return findChildKeySuggestionForQueryPrefix(module, fileType, matchesRootTillMe,
                    numOfAncestors, querySegmentPrefixes, querySegmentPrefixStartIndex, siblingsToExclude);
        }

        @Override
        public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
            return helper.getDocumentationForKey(module, nodeNavigationPathDotDelimited);
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
            return doWithValueDelegateOrReturnNull(proxy -> proxy
                    .findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix,
                            siblingsToExclude));
        }

        @Override
        public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                               String originalValue) {
            return doWithValueDelegateOrReturnNull(
                    proxy -> proxy.getDocumentationForValue(module, nodeNavigationPathDotDelimited,
                            originalValue));
        }

        @Override
        public boolean isLeaf(Module module) {
            return doWithValueDelegateAndReturn(proxy -> proxy.isLeaf(module), true);
        }

        @Override
        public boolean isMetadataNonProperty() {
            return false;
        }

        @Nullable
        @Override
        public String getOriginalName() {
            return helper.getOriginalName();
        }

        @Nullable
        @Override
        public String getNameForDocumentation(Module module) {
            return getOriginalName();
        }

        @Override
        public boolean supportsDocumentation() {
            return helper.supportsDocumentation();
        }

        @NotNull
        @Override
        public SuggestionNodeType getSuggestionNodeType(Module module) {
            return doWithValueDelegateAndReturn(proxy -> proxy.getSuggestionNodeType(module),
                    UNKNOWN_CLASS);
        }

    }

}
