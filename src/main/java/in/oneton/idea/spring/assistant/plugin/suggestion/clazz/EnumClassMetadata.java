package in.oneton.idea.spring.assistant.plugin.suggestion.clazz;

import com.intellij.lang.java.JavaDocumentationProvider;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import gnu.trove.THashMap;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.SuggestionDocumentationHelper;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import static com.intellij.util.containers.ContainerUtil.isEmpty;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.dotDelimitedOriginalNames;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.computeDocumentation;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.isValidType;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.toClassNonQualifiedName;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.toValidPsiClass;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode.sanitise;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.ENUM;
import static in.oneton.idea.spring.assistant.plugin.suggestion.handler.YamlValueInsertHandler.unescapeValue;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class EnumClassMetadata extends ClassMetadata {

    @NotNull
    private final PsiClassType type;

    @Nullable
    private Map<String, PsiField> childLookup;
    @Nullable
    private Trie<String, PsiField> childrenTrie;

    EnumClassMetadata(@NotNull PsiClassType type) {
        this.type = type;
        PsiClass enumClass = requireNonNull(toValidPsiClass(type));
        assert enumClass.isEnum();
    }

    @Override
    protected void init(Module module) {
        init(type);
    }

    @Nullable
    @Override
    protected SuggestionDocumentationHelper doFindDirectChild(Module module, String pathSegment) {
        if (childLookup != null && childLookup.containsKey(pathSegment)) {
            return new EnumKeySuggestionDocumentationHelper(childLookup.get(pathSegment));
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
        if (childrenTrie != null && childLookup != null) {
            SortedMap<String, PsiField> prefixMap = childrenTrie.prefixMap(querySegmentPrefix);
            if (!isEmpty(prefixMap)) {
                return getMatchStreamAfterExclusions(childLookup, prefixMap.values(), siblingsToExclude)
                        .map(EnumKeySuggestionDocumentationHelper::new).collect(toList());
            }
        }
        return null;
    }

    private Stream<PsiField> getMatchStreamAfterExclusions(@NotNull Map<String, PsiField> childLookup,
                                                           Collection<PsiField> values, @Nullable Set<String> siblingsToExclude) {
        if (siblingsToExclude != null) {
            Set<PsiField> exclusionMembers =
                    siblingsToExclude.stream().map(childLookup::get).collect(toSet());
            return values.stream().filter(value -> !exclusionMembers.contains(value));
        } else {
            return values.stream();
        }
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
                "Should not be called. To use as a map key call doFindDirectChildrenForQueryPrefix(..) instead");
    }

    @Nullable
    @Override
    protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
                                                                       FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                                       String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
                                                                       @Nullable Set<String> siblingsToExclude) {
        throw new IllegalAccessError(
                "Should not be called. To use as a map key call doFindDirectChildrenForQueryPrefix(..) instead");
    }

    @Override
    protected SortedSet<Suggestion> doFindValueSuggestionsForPrefix(Module module, FileType fileType,
                                                                    List<SuggestionNode> matchesRootTillMe, String prefix,
                                                                    @Nullable Set<String> siblingsToExclude) {
        if (childrenTrie != null && childLookup != null) {
            SortedMap<String, PsiField> prefixMap = childrenTrie.prefixMap(prefix);
            if (!isEmpty(prefixMap)) {
                return getMatchStreamAfterExclusions(childLookup, prefixMap.values(), siblingsToExclude)
                        .map(psiField -> newSuggestion(fileType, matchesRootTillMe, matchesRootTillMe.size(),
                                true, psiField)).collect(toCollection(TreeSet::new));
            }
        }
        return null;
    }

    @Nullable
    @Override
    protected String doGetDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                                String originalValue) {
        if (childLookup != null) {
            PsiField type = childLookup.get(sanitise(originalValue));
            return "<b>" + nodeNavigationPathDotDelimited + "</b> = <b>" + unescapeValue(originalValue)
                    + "</b>" + new JavaDocumentationProvider().generateDoc(type, type);
        }
        return null;
    }

    @Override
    public boolean doCheckIsLeaf(Module module) {
        return true;
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType() {
        return ENUM;
    }

    @Nullable
    @Override
    public PsiType getPsiType(Module module) {
        return type;
    }

    private void init(@NotNull PsiClassType type) {
        if (isValidType(type)) {
            PsiField[] fields = requireNonNull(toValidPsiClass(type)).getFields();
            List<PsiField> acceptableFields = new ArrayList<>();
            for (PsiField field : fields) {
                if (field != null && field.getType().equals(type)) {
                    acceptableFields.add(field);
                }
            }
            if (acceptableFields.size() != 0) {
                childLookup = new THashMap<>();
                childrenTrie = new PatriciaTrie<>();
                acceptableFields.forEach(field -> {
                    childLookup.put(sanitise(requireNonNull(field.getName())), field);
                    childrenTrie.put(sanitise(field.getName()), field);
                });
            }
        } else {
            childLookup = null;
            childrenTrie = null;
        }
    }

    private Suggestion newSuggestion(FileType fileType, List<SuggestionNode> matchesRootTillMe,
                                     int numOfAncestors, boolean forValue, @NotNull PsiField value) {
        Suggestion.SuggestionBuilder builder =
                Suggestion.builder().numOfAncestors(numOfAncestors).matchesTopFirst(matchesRootTillMe)
                        .shortType(toClassNonQualifiedName(type)).description(computeDocumentation(value))
                        .icon(ENUM.getIcon()).fileType(fileType);
        if (forValue) {
            builder.suggestionToDisplay(requireNonNull(value.getName()));
        } else {
            builder.suggestionToDisplay(dotDelimitedOriginalNames(matchesRootTillMe, numOfAncestors));
        }
        builder.forValue(forValue);
        return builder.build();
    }


    class EnumKeySuggestionDocumentationHelper implements SuggestionDocumentationHelper {
        private final PsiField field;

        EnumKeySuggestionDocumentationHelper(PsiField field) {
            this.field = field;
        }

        @Nullable
        @Override
        public String getOriginalName() {
            return field.getName();
        }

        @NotNull
        @Override
        public Suggestion buildSuggestionForKey(Module module, FileType fileType,
                                                List<SuggestionNode> matchesRootTillMe, int numOfAncestors) {
            return newSuggestion(fileType, matchesRootTillMe, numOfAncestors, false, field);
        }

        @Override
        public boolean supportsDocumentation() {
            return true;
        }

        @NotNull
        @Override
        public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
            return "<b>" + nodeNavigationPathDotDelimited + "</b>" + new JavaDocumentationProvider()
                    .generateDoc(field, field);
        }

        @NotNull
        @Override
        public SuggestionNodeType getSuggestionNodeType(Module module) {
            return ENUM;
        }
    }

}
