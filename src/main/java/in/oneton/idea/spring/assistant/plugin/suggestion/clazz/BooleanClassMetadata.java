package in.oneton.idea.spring.assistant.plugin.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.SuggestionDocumentationHelper;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import static com.intellij.codeInsight.documentation.DocumentationManager.createHyperlink;
import static com.intellij.util.containers.ContainerUtil.isEmpty;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.dotDelimitedOriginalNames;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.BOOLEAN;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.ENUM;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class BooleanClassMetadata extends ClassMetadata {

    @Nullable
    private Trie<String, Boolean> childrenTrie;

    @Override
    protected void init(Module module) {
        childrenTrie = new PatriciaTrie<>();
        childrenTrie.put("true", TRUE);
        childrenTrie.put("false", FALSE);
    }

    @Nullable
    @Override
    protected SuggestionDocumentationHelper doFindDirectChild(Module module, String pathSegment) {
        if ("true".equals(pathSegment)) {
            return new BooleanKeySuggestionDocumentationHelper(true);
        } else if ("false".equals(pathSegment)) {
            return new BooleanKeySuggestionDocumentationHelper(false);
        }
        return null;
    }

    @Override
    protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix) {
        return doFindDirectChildrenForQueryPrefix(module, querySegmentPrefix, null);
    }

    @Override
    protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix, @Nullable Set<String> siblingsToExclude) {
        assert childrenTrie != null;
        SortedMap<String, Boolean> prefixMap = childrenTrie.prefixMap(querySegmentPrefix);
        if (!isEmpty(prefixMap)) {
            Stream<Boolean> matchStream = getMatchStreamAfterExclusion(prefixMap, siblingsToExclude);
            return matchStream.map(BooleanKeySuggestionDocumentationHelper::new).collect(toList());
        }
        return null;
    }

    @Nullable
    @Override
    protected List<SuggestionNode> doFindDeepestSuggestionNode(Module module,
                                                               List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                               int pathSegmentStartIndex) {
        throw new IllegalAccessError(
                "Should not be called. To use as a map key call findDirectChild(..) instead");
    }

    @Nullable
    @Override
    protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
                                                                       FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                                       String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
        throw new IllegalAccessError(
                "Should not be called. To use as a map key call findDirectChild(..) instead");
    }

    @Nullable
    @Override
    protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
                                                                       FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                                       String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
                                                                       @Nullable Set<String> siblingsToExclude) {
        throw new IllegalAccessError(
                "Should not be called. To use as a map key call findDirectChild(..) instead");
    }

    @Override
    protected SortedSet<Suggestion> doFindValueSuggestionsForPrefix(Module module, FileType fileType,
                                                                    List<SuggestionNode> matchesRootTillMe, String prefix,
                                                                    @Nullable Set<String> siblingsToExclude) {
        assert childrenTrie != null;
        SortedMap<String, Boolean> matchesMap = childrenTrie.prefixMap(prefix);
        if (!isEmpty(matchesMap)) {
            Stream<Boolean> matchStream = getMatchStreamAfterExclusion(matchesMap, siblingsToExclude);
            return matchStream.map(
                    val -> newSuggestion(fileType, matchesRootTillMe, matchesRootTillMe.size(), true, val))
                    .collect(toCollection(TreeSet::new));
        }
        return null;
    }

    @Nullable
    @Override
    protected String doGetDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                                String originalValue) {
        // Format for the documentation is as follows
        /*
         * <p><b>a.b.c</b> ({@link com.acme.Generic}<{@link com.acme.Class1}, {@link com.acme.Class2}>)</p>
         * <p>Long description</p>
         * or of this type
         * <p><b>Type</b> {@link com.acme.Array}[]</p>
         * <p><b>Declared at</b>{@link com.acme.GenericRemovedClass#method}></p> <-- only for groups with method info
         */
        StringBuilder builder =
                new StringBuilder().append("<b>").append(nodeNavigationPathDotDelimited).append("</b>");

        String classFqn = PsiType.BOOLEAN.getBoxedTypeName();
        StringBuilder linkBuilder = new StringBuilder();
        createHyperlink(linkBuilder, classFqn, classFqn, false);
        builder.append(" (").append(linkBuilder.toString()).append(")");

        builder.append("<p>").append(originalValue).append("</p>");

        return builder.toString();
    }

    @Override
    public boolean doCheckIsLeaf(Module module) {
        return true;
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType() {
        return BOOLEAN;
    }

    @NotNull
    @Override
    public PsiType getPsiType(Module module) {
        return PsiType.BOOLEAN;
    }

    private Stream<Boolean> getMatchStreamAfterExclusion(SortedMap<String, Boolean> prefixMap,
                                                         @Nullable Set<String> siblingsToExclude) {
        Stream<Boolean> matchStream;
        if (siblingsToExclude != null) {
            matchStream = prefixMap.values().stream()
                    .filter(value -> !siblingsToExclude.contains(value.toString()));
        } else {
            matchStream = prefixMap.values().stream();
        }
        return matchStream;
    }

    private Suggestion newSuggestion(FileType fileType, List<SuggestionNode> matchesRootTillMe,
                                     int numOfAncestors, boolean forValue, boolean value) {
        Suggestion.SuggestionBuilder builder =
                Suggestion.builder().numOfAncestors(numOfAncestors).matchesTopFirst(matchesRootTillMe)
                        .shortType("Boolean").icon(ENUM.getIcon()).fileType(fileType);
        if (forValue) {
            String valueToDisplay = valueOf(value);
            builder.suggestionToDisplay(valueToDisplay);
        } else {
            builder.suggestionToDisplay(dotDelimitedOriginalNames(matchesRootTillMe, numOfAncestors));
        }
        builder.forValue(forValue);
        return builder.build();
    }

    private class BooleanKeySuggestionDocumentationHelper implements SuggestionDocumentationHelper {
        private final boolean value;

        BooleanKeySuggestionDocumentationHelper(boolean value) {
            this.value = value;
        }

        @Nullable
        @Override
        public String getOriginalName() {
            return value ? "true" : "false";
        }

        @NotNull
        @Override
        public Suggestion buildSuggestionForKey(Module module, FileType fileType,
                                                List<SuggestionNode> matchesRootTillMe, int numOfAncestors) {
            return newSuggestion(fileType, matchesRootTillMe, numOfAncestors, false, value);
        }

        @Override
        public boolean supportsDocumentation() {
            return true;
        }

        @NotNull
        @Override
        public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
            /*
             * <p><b>a.b.c</b> ({@link com.acme.Generic}<{@link com.acme.Class1}, {@link com.acme.Class2}>)</p>
             * <p>Long description</p>
             */
            StringBuilder builder =
                    new StringBuilder().append("<b>").append(nodeNavigationPathDotDelimited).append("</b>");

            String classFqn = PsiType.BOOLEAN.getBoxedTypeName();
            StringBuilder linkBuilder = new StringBuilder();
            createHyperlink(linkBuilder, classFqn, classFqn, false);
            builder.append(" (").append(linkBuilder.toString()).append(")");

            return builder.toString();
        }

        @NotNull
        @Override
        public SuggestionNodeType getSuggestionNodeType(Module module) {
            return BOOLEAN;
        }
    }

}
