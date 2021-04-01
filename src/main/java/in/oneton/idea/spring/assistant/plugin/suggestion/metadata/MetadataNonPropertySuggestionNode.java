package in.oneton.idea.spring.assistant.plugin.suggestion.metadata;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiType;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataGroup;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.newListWithMembers;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.newSingleElementSortedSet;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.safeGetValidType;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/**
 * Represents a node in the hierarchy of suggestions
 * Useful for navigating all suggestions & also acts as source of truth.
 * Note that some of the intermediate nodes might be there to just support the hierarchy
 * <p>
 * Also used for dot delimited search search. Each element corresponds to only a single section of the complete suggestion hierarchy
 * i.e if the we are building suggestions for
 * <ul>
 * <li>alpha.childNode11.charlie</li>
 * <li>alpha.childNode12.echo</li>
 * <li>alpha.echo</li>
 * <li>childNode11.charlie</li>
 * <li>childNode11.childNode12</li>
 * </ul>
 * <p>
 * The search for above properties would look like
 * <ul>
 * <li>
 * alpha
 * <ul>
 * <li>
 * childNode11
 * <ul>
 * <li>charlie</li>
 * </ul>
 * </li>
 * <li>
 * childNode12
 * <ul>
 * <li>echo</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li>echo</li>
 * <li>
 * childNode11
 * <ul>
 * <li>charlie</li>
 * <li>childNode12</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * We can expect a total of 5 tries in the complete search tree, each child trie being hosted by the parent trie element (+ a special toplevel trie for the whole tree)
 * <ul>
 * <li>(alpha + echo + childNode11) - for top level elements</li>
 * <li>alpha > (childNode11 + childNode12) - for children of <em>alpha</em></li>
 * <li>alpha > childNode11 > (charlie) - for children of <em>alpha > childNode11</em></li>
 * <li>alpha > childNode12 > (echo) - for children of <em>alpha > childNode12</em></li>
 * <li>childNode11 > (charlie + childNode12) - for children of <em>childNode11</em></li>
 * </ul>
 * <p>
 * <b>NOTE:</b> elements within the trie are indicated by enclosing them in curved brackets
 * <p>
 * <p>
 * This hierarchical trie is useful for searches like `a.ch.c` to correspond to `alpha.childNode11.charlie`
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class MetadataNonPropertySuggestionNode extends MetadataSuggestionNode {

    private static final Logger log = Logger.getInstance(MetadataNonPropertySuggestionNode.class);

    /**
     * Sanitised name used for lookup. `-`, `_` are removed, upper cased characters are converted to lower case
     */
    @EqualsAndHashCode.Include
    private String name;
    /**
     * Section of the group/PROPERTY name. Sole purpose of this is to split all properties into their individual part
     */
    @ToString.Include
    private String originalName;

    // TODO: Make sure that this will be part of search only if type & sourceType are part of the class path
    /**
     * Can be null for intermediate nodes that dont have a group entry in `spring-configuration-metadata.json`
     */
    @Nullable
    private SpringConfigurationMetadataGroup group;

    /**
     * Parent reference, for bidirectional navigation. Can be null for roots
     */
    @Nullable
    private MetadataNonPropertySuggestionNode parent;
    /**
     * Set of sources these suggestions belong to
     */
    private Set<String> belongsTo;
    /**
     * Child name -> child node. Aids in quick lookup. NOTE: All keys are sanitized
     */
    @Nullable
    private Map<String, MetadataSuggestionNode> childLookup;

    /**
     * Child trie for the nodes at next level, aids in prefix based searching. NOTE: All keys are sanitized
     */
    @Nullable
    private Trie<String, MetadataSuggestionNode> childrenTrie;

    /**
     * @param originalName name that is not sanitised
     * @param parent       parent MetadataNonPropertySuggestionNode node
     * @param belongsTo    file/jar containing this property
     * @return newly constructed group node
     */
    public static MetadataNonPropertySuggestionNode newInstance(String originalName,
                                                                @Nullable MetadataNonPropertySuggestionNode parent, String belongsTo) {
        MetadataNonPropertySuggestionNodeBuilder builder =
                MetadataNonPropertySuggestionNode.builder().name(SuggestionNode.sanitise(originalName))
                        .originalName(originalName).parent(parent);
        Set<String> belongsToSet = new THashSet<>();
        belongsToSet.add(belongsTo);
        builder.belongsTo(belongsToSet);
        return builder.build();
    }

    @Override
    public MetadataSuggestionNode findDeepestMetadataNode(String[] pathSegments, int pathSegmentStartIndex, boolean matchAllSegments) {

        MetadataSuggestionNode deepestMatch = null;
        if (!matchAllSegments) {
            deepestMatch = this;
        }

        boolean haveMoreSegments = pathSegmentStartIndex < pathSegments.length;
        if (!haveMoreSegments) {
            return deepestMatch;
        }

        boolean lastSegment = pathSegmentStartIndex == (pathSegments.length - 1);
        String pathSegment = pathSegments[pathSegmentStartIndex];

        if (hasChildren()) {
            assert childLookup != null;
            if (childLookup.containsKey(pathSegment)) {
                MetadataSuggestionNode child = childLookup.get(pathSegment);
                if (lastSegment) {
                    deepestMatch = child;
                } else {
                    deepestMatch = child.findDeepestMetadataNode(pathSegments, pathSegmentStartIndex + 1, matchAllSegments);
                }

                if (matchAllSegments && deepestMatch == null) {
                    deepestMatch = this;
                }
            }

        } else if (lastSegment && name.equals(pathSegment)) {
            deepestMatch = this;
        }
        return deepestMatch;
    }


    @Nullable
    @Override
    public List<SuggestionNode> findDeepestSuggestionNode(Module module,
                                                          List<SuggestionNode> matchesRootTillMe, String[] pathSegments, int pathSegmentStartIndex) {
        List<SuggestionNode> deepestMatch = null;
        boolean haveMoreSegments = pathSegmentStartIndex < pathSegments.length;
        if (haveMoreSegments) {
            String currentPathSegment = pathSegments[pathSegmentStartIndex];
            boolean lastSegment = pathSegmentStartIndex == (pathSegments.length - 1);
            if (hasChildren()) {
                assert childLookup != null;
                if (childLookup.containsKey(currentPathSegment)) {
                    MetadataSuggestionNode child = childLookup.get(currentPathSegment);
                    matchesRootTillMe.add(child);
                    if (lastSegment) {
                        deepestMatch = matchesRootTillMe;
                    } else {
                        deepestMatch = child.findDeepestSuggestionNode(module, matchesRootTillMe, pathSegments,
                                pathSegmentStartIndex + 1);
                    }
                }
            } else if (lastSegment && name.equals(currentPathSegment)) {
                deepestMatch = matchesRootTillMe;
            }
        } else {
            deepestMatch = matchesRootTillMe;
        }

        return deepestMatch;
    }

    public void addChildren(Module module, SpringConfigurationMetadataGroup group,
                            String[] rawPathSegments, int startIndex, String belongsTo) {
        MetadataNonPropertySuggestionNode groupNode =
                addChildren(rawPathSegments, startIndex, rawPathSegments.length - 1, belongsTo);
        groupNode.setGroup(module, group);
    }

    public void addChildren(SpringConfigurationMetadataProperty property, String[] rawPathSegments,
                            int startIndex, String belongsTo) {
        MetadataNonPropertySuggestionNode parentNode;
        // since last property is the actual property, lets only add children only till last but one
        int endIndexIncl = rawPathSegments.length - 2;
        if (startIndex <= endIndexIncl) {
            parentNode = addChildren(rawPathSegments, startIndex, endIndexIncl, belongsTo);
        } else {
            parentNode = this;
            addRefCascadeTillRoot(belongsTo);
        }

        parentNode.addProperty(property, rawPathSegments[rawPathSegments.length - 1], belongsTo);
    }

    @Override
    @Nullable
    public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(final Module module, FileType fileType,
                                                                  final List<SuggestionNode> matchesRootTillMe,
                                                                  final int numOfAncestors, String[] querySegmentPrefixes,
                                                                  int querySegmentPrefixStartIndex,
                                                                  @Nullable final Set<String> siblingsToExclude) {

        boolean lookingForConcreteNode = querySegmentPrefixStartIndex >= querySegmentPrefixes.length;
        if (lookingForConcreteNode) {
            return lookingForConcreteNode(module, fileType, matchesRootTillMe, numOfAncestors, querySegmentPrefixes, querySegmentPrefixStartIndex);

        } else if (hasChildren()) {
            return getSuggestionsChildren(module, fileType, matchesRootTillMe, numOfAncestors, querySegmentPrefixes, querySegmentPrefixStartIndex, siblingsToExclude);
        }
        return null;
    }

    private SortedSet<Suggestion> getSuggestionsChildren(final Module module, final FileType fileType, final List<SuggestionNode> matchesRootTillMe,
                                                         final int numOfAncestors, final String[] querySegmentPrefixes, final int querySegmentPrefixStartIndex,
                                                         final @org.jetbrains.annotations.Nullable Set<String> siblingsToExclude) {
        assert childrenTrie != null;
        assert childLookup != null;
        String querySegmentPrefix = querySegmentPrefixes[querySegmentPrefixStartIndex];
        SortedMap<String, MetadataSuggestionNode> sortedPrefixToMetadataNode =
                childrenTrie.prefixMap(querySegmentPrefix);
        Collection<MetadataSuggestionNode> matchedChildren = sortedPrefixToMetadataNode.values();

        Set<MetadataSuggestionNode> exclusionMembers = null;
        if (siblingsToExclude != null) {
            exclusionMembers = siblingsToExclude.stream().map(childLookup::get).collect(toSet());
        }

        if (!isEmpty(exclusionMembers) && !isEmpty(matchedChildren)) {
            final Set<MetadataSuggestionNode> finalExclusionMembers = exclusionMembers;
            matchedChildren = matchedChildren.stream()
                    .filter(value -> !finalExclusionMembers.contains(value))
                    .collect(toList());
        }

        int segmentPrefixStartIndex = querySegmentPrefixStartIndex;
        if (matchedChildren.size() == 0) {
            matchedChildren = computeChildrenToIterateOver(childLookup, exclusionMembers);
        } else {
            segmentPrefixStartIndex = segmentPrefixStartIndex + 1;
        }
        // lets search in the next level
        return addChildToMatchesAndSearchInNextLevel(module, fileType, matchesRootTillMe, numOfAncestors,
                querySegmentPrefixes, segmentPrefixStartIndex, matchedChildren);
    }

    private SortedSet<Suggestion> lookingForConcreteNode(final Module module, final FileType fileType, final List<SuggestionNode> matchesRootTillMe, final int numOfAncestors, final String[] querySegmentPrefixes, final int querySegmentPrefixStartIndex) {
        if (isGroup()) {
            // If we have only one child, lets send the child value directly instead of this node. This way user does not need trigger suggestion for level, esp. when we know there will is only be one child
            if (hasOnlyOneChild()) {
                assert childrenTrie != null;
                return addChildToMatchesAndSearchInNextLevel(module, fileType, matchesRootTillMe,
                        numOfAncestors, querySegmentPrefixes, querySegmentPrefixStartIndex,
                        childrenTrie.values());
            } else { // either there are no children/multiple children are present. Lets return suggestions
                assert group != null;
                return newSingleElementSortedSet(
                        group.newSuggestion(fileType, matchesRootTillMe, numOfAncestors));
            }
        } else { // intermediate node, lets get all next level groups & properties
            assert childrenTrie != null;
            return addChildToMatchesAndSearchInNextLevel(module, fileType, matchesRootTillMe,
                    numOfAncestors, querySegmentPrefixes, querySegmentPrefixStartIndex,
                    childrenTrie.values());
        }
    }

    @Override
    protected boolean hasOnlyOneChild() {
        return childrenTrie != null && childrenTrie.size() == 1;
        //     && childrenTrie.values().stream()
        //        .allMatch(MetadataSuggestionNode::hasOnlyOneChild)
    }

    @Override
    public String toTree() {
        StringBuilder builder = new StringBuilder(originalName)
                .append(isRoot() ? "(root + group)" : (isGroup() ? "(group)" : "(intermediate)"))
                .append("\n");
        if (childLookup != null) {
            childLookup.forEach(
                    (k, v) -> builder.append(v.toTree().trim().replaceAll("\\^", "  ").replaceAll("\n", "\n  "))
                            .append("\n"));
        }
        return builder.toString();
    }

    /**
     * @param containerPath Represents path to the metadata file container
     * @return true if no children left & this item does not belong to any other source
     */
    @Override
    public boolean removeRefCascadeDown(String containerPath) {
        belongsTo.remove(containerPath);
        // If the current node & all its children belong to a single file, lets remove the whole tree
        if (belongsTo.size() == 0) {
            return true;
        }

        if (hasChildren()) {
            assert childLookup != null;
            assert childrenTrie != null;
            Iterator<MetadataSuggestionNode> iterator = childrenTrie.values().iterator();
            while (iterator.hasNext()) {
                MetadataSuggestionNode child = iterator.next();
                boolean canRemoveReference = child.removeRefCascadeDown(containerPath);
                if (canRemoveReference) {
                    iterator.remove();
                    childLookup.remove(child.getName());
                    childrenTrie.remove(child.getName());
                }
            }
            if (!hasChildren()) {
                childLookup = null;
                childrenTrie = null;
            }
        }
        return false;
    }

    @Override
    protected boolean isRoot() {
        return parent == null;
    }

    @Override
    public boolean isGroup() {
        return group != null;
    }

    @Override
    public boolean isProperty() {
        return false;
    }

    @NotNull
    @Override
    public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
        if (isGroup()) {
            assert group != null;
            return group.getDocumentation(nodeNavigationPathDotDelimited);
        }
        throw new RuntimeException(
                "Documentation not supported for this element. Call supportsDocumentation() first");
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                               List<SuggestionNode> matchesRootTillMe, String prefix,
                                                               @Nullable Set<String> siblingsToExclude) {
        throw new IllegalAccessError("Should never be called");
    }

    @Nullable
    @Override
    public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                           String originalValue) {
        throw new IllegalAccessError("Should never be called");
    }

    @Override
    public boolean isLeaf(Module module) {
        return false;
    }

    @Override
    public boolean isMetadataNonProperty() {
        return true;
    }

    private boolean hasChildren() {
        return childrenTrie != null && childrenTrie.size() != 0;
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType(Module module) {
        if (isGroup()) {
            assert group != null;
            return group.getNodeType();
        } else {
            return SuggestionNodeType.UNDEFINED;
        }
    }

    public void setGroup(Module module, SpringConfigurationMetadataGroup group) {
        updateGroupType(module, group);
        this.group = group;
    }

    @Override
    public void refreshClassProxy(Module module) {
        updateGroupType(module, group);
        if (hasChildren()) {
            assert childLookup != null;
            childLookup.values().forEach(child -> child.refreshClassProxy(module));
        }
    }

    private Collection<MetadataSuggestionNode> computeChildrenToIterateOver(
            @NotNull Map<String, MetadataSuggestionNode> childLookup,
            Set<MetadataSuggestionNode> exclusionMembers) {
        Collection<MetadataSuggestionNode> childrenToIterateOver;
        if (!isEmpty(exclusionMembers)) {
            childrenToIterateOver =
                    childLookup.values().stream().filter(value -> !exclusionMembers.contains(value))
                            .collect(toList());
        } else {
            childrenToIterateOver = childLookup.values();
        }
        return childrenToIterateOver;
    }

    private void addProperty(SpringConfigurationMetadataProperty property, String originalName,
                             String belongsTo) {
        addRefCascadeTillRoot(belongsTo);
        if (!hasChildren()) {
            childLookup = new THashMap<>();
            childrenTrie = new PatriciaTrie<>();
        }

        assert childLookup != null;
        assert childrenTrie != null;
        MetadataSuggestionNode childNode =
                MetadataPropertySuggestionNode.newInstance(originalName, property, this, belongsTo);

        String name = SuggestionNode.sanitise(originalName);
        childLookup.put(name, childNode);
        childrenTrie.put(name, childNode);
    }

    private MetadataNonPropertySuggestionNode addChildren(String[] rawPathSegments, int startIndex,
                                                          int endIndexIncl, String belongsTo) {
        addRefCascadeTillRoot(belongsTo);
        if (!hasChildren()) {
            childLookup = new THashMap<>();
            childrenTrie = new PatriciaTrie<>();
        }

        assert childLookup != null;
        assert childrenTrie != null;

        String rawPathSegment = rawPathSegments[startIndex];
        String pathSegment = SuggestionNode.sanitise(rawPathSegment);
        MetadataNonPropertySuggestionNode childNode =
                (MetadataNonPropertySuggestionNode) childLookup.get(pathSegment);
        if (childNode == null) {
            childNode = MetadataNonPropertySuggestionNode.newInstance(rawPathSegment, this, belongsTo);
            childNode.setParent(this);

            childLookup.put(pathSegment, childNode);
            childrenTrie.put(pathSegment, childNode);
        }

        // If this is the last segment, lets set group
        if (startIndex >= endIndexIncl) {
            return childNode;
        } else {
            return childNode.addChildren(rawPathSegments, startIndex + 1, endIndexIncl, belongsTo);
        }
    }

    private SortedSet<Suggestion> addChildToMatchesAndSearchInNextLevel(Module module,
                                                                        FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                                        String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
                                                                        Collection<MetadataSuggestionNode> childNodes) {
        SortedSet<Suggestion> suggestions = null;
        for (MetadataSuggestionNode child : childNodes) {
            List<SuggestionNode> matchesRootTillChild =
                    unmodifiableList(newListWithMembers(matchesRootTillParentNode, child));
            Set<Suggestion> matchedSuggestions = child
                    .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillChild, numOfAncestors,
                            querySegmentPrefixes, querySegmentPrefixStartIndex, null);
            if (matchedSuggestions != null) {
                if (suggestions == null) {
                    suggestions = new TreeSet<>();
                }
                suggestions.addAll(matchedSuggestions);
            }
        }
        return suggestions;
    }

    private void updateGroupType(Module module, SpringConfigurationMetadataGroup group) {
        if (group != null && group.getClassName() != null) {
            PsiType groupPsiType = safeGetValidType(module, group.getClassName());
            if (groupPsiType != null) {
                group.setNodeType(PsiCustomUtil.getSuggestionNodeType(groupPsiType));
            } else {
                group.setNodeType(SuggestionNodeType.UNKNOWN_CLASS);
            }
        }
    }

}
