package in.oneton.idea.spring.assistant.plugin.suggestion.service;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import in.oneton.idea.spring.assistant.plugin.initializr.misc.JsonUtil;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.MetadataContainerInfo;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.MetadataNonPropertySuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.MetadataPropertySuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.MetadataSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadata;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataGroup;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataHint;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataProperty;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Future;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.modifiableList;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.truncateIdeaDummyIdentifier;
import static in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion.PERIOD_DELIMITER;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode.sanitise;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class SuggestionServiceImpl implements SuggestionService {

    private static final Logger log = Logger.getInstance(SuggestionServiceImpl.class);

    private final Map<String, Map<String, MetadataContainerInfo>>
            moduleNameToSeenContainerPathToContainerInfo;
    /**
     * Within the trie, all keys are stored in sanitised format to enable us find keys without worrying about hiphens, underscores, e.t.c in the keys themselves
     */
    private final Map<String, Trie<String, MetadataSuggestionNode>> moduleNameToRootSearchIndex;
    private Future<?> currentExecution;
    private volatile boolean indexingInProgress;

    SuggestionServiceImpl() {
        moduleNameToSeenContainerPathToContainerInfo = new THashMap<>();
        moduleNameToRootSearchIndex = new THashMap<>();
    }

    private static String[] toSanitizedPathSegments(String element) {
        String[] splits = element.trim().split(PERIOD_DELIMITER, -1);
        for (int i = 0; i < splits.length; i++) {
            splits[i] = sanitise(splits[i]);
        }
        return splits;
    }

    private static String[] toRawPathSegments(String element) {
        String[] splits = element.trim().split(PERIOD_DELIMITER, -1);
        for (int i = 0; i < splits.length; i++) {
            splits[i] = splits[i].trim();
        }
        return splits;
    }

    private static String firstPathSegment(String element) {
        return element.trim().split(PERIOD_DELIMITER, -1)[0];
    }

    @Override
    public void init(Project project) {
        reIndex(project);
    }

    @Override
    public void reIndex(Project project) {
        if (indexingInProgress) {
            currentExecution.cancel(false);
        }
        currentExecution = getApplication().executeOnPooledThread(() -> getApplication()
                        .runReadAction(() -> {
                            indexingInProgress = true;

                            StopWatch timer = new StopWatch();
                            timer.start();

                            try {
                                debug(() -> log.debug("-> Indexing requested for project " + project.getName()));
                                // OrderEnumerator.orderEntries(project) is returning everything from all modules including root level
                                // module(which is called project in gradle terms) So, we should not be doing anything with this
//                                improvment
                                stream(ModuleManager.getInstance(project).getModules())
                                        .forEach(module -> reindexModule(emptyList(), emptyList(), module));
//                                Module[] modules = ModuleManager.getInstance(project).getModules();
//                                for (Module module : modules) { reindexModule(emptyList(), emptyList(), module); }
                            } finally {
                                indexingInProgress = false;
                                timer.stop();
                                debug(() -> log.debug("<- Indexing took " + timer.toString() + " for project " + project.getName()));
                            }
                        })
        );
    }

    @Override
    public void reindex(final Project project, final Module[] modules) {
        if (this.indexingInProgress && currentExecution != null) {
            this.currentExecution.cancel(false);
        }
        this.currentExecution = getApplication().executeOnPooledThread(() ->
                getApplication().runReadAction(() -> {
                    debug(() -> log.debug("-> Indexing requested for a subset of modules of project " + project.getName()));
                    this.indexingInProgress = true;

                    final StopWatch timer = new StopWatch();
                    timer.start();

                    try {
                        stream(modules).forEach(module -> {
                            debug(() -> log.debug("--> Indexing requested for module " + module.getName()));
                            final StopWatch moduleTimer = new StopWatch();
                            moduleTimer.start();

                            try {
                                reindexModule(emptyList(), emptyList(), module);
                            } finally {
                                moduleTimer.stop();
                                debug(() -> log.debug("<-- Indexing took " + moduleTimer.toString() + " for module " + module.getName()));
                            }
                        });

                    } finally {
                        indexingInProgress = false;
                        timer.stop();
                        debug(() -> log.debug("<- Indexing took " + timer.toString() + " for project " + project.getName()));
                    }
                })
        );
    }

    @Override
    public void reindex(Project project, Module module) {
        reindex(project, new Module[]{module});
    }

    @Nullable
    @Override
    public List<SuggestionNode> findMatchedNodesRootTillEnd(Module module,
                                                            List<String> containerElements) {
        if (moduleNameToRootSearchIndex.containsKey(module.getName())) {
            String[] pathSegments =
                    containerElements.stream().flatMap(element -> stream(toSanitizedPathSegments(element)))
                            .toArray(String[]::new);
            MetadataSuggestionNode searchStartNode =
                    moduleNameToRootSearchIndex.get(module.getName()).get(pathSegments[0]);
            if (searchStartNode != null) {
                List<SuggestionNode> matches = modifiableList(searchStartNode);
                if (pathSegments.length > 1) {
                    return searchStartNode.findDeepestSuggestionNode(module, matches, pathSegments, 1);
                }
                return matches;
            }
        }
        return null;
    }

    @Override
    public boolean canProvideSuggestions(Module module) {
        Trie<String, MetadataSuggestionNode> rootSearchIndex =
                moduleNameToRootSearchIndex.get(module.getName());
        return rootSearchIndex != null && rootSearchIndex.size() != 0;
    }

    @Override
    public List<LookupElementBuilder> findSuggestionsForQueryPrefix(Module module,
                                                                    FileType fileType, PsiElement element, @Nullable List<String> ancestralKeys,
                                                                    String queryWithDotDelimitedPrefixes, @Nullable Set<String> siblingsToExclude) {
        return doFindSuggestionsForQueryPrefix(module,
                moduleNameToRootSearchIndex.get(module.getName()), fileType, element, ancestralKeys,
                queryWithDotDelimitedPrefixes, siblingsToExclude);
    }

    private List<MetadataContainerInfo> computeNewContainersToProcess(OrderEnumerator orderEnumerator,
                                                                      Map<String, MetadataContainerInfo> seenContainerPathToContainerInfo) {
        List<MetadataContainerInfo> containersToProcess = new ArrayList<>();
        for (VirtualFile metadataFileContainer : orderEnumerator.recursively().classes().getRoots()) {
            Collection<MetadataContainerInfo> metadataContainerInfos =
                    MetadataContainerInfo.newInstances(metadataFileContainer);
            for (MetadataContainerInfo metadataContainerInfo : metadataContainerInfos) {
                boolean seenBefore = seenContainerPathToContainerInfo
                        .containsKey(metadataContainerInfo.getContainerArchiveOrFileRef());

                boolean updatedSinceLastSeen = false;
                if (seenBefore) {
                    MetadataContainerInfo seenMetadataContainerInfo = seenContainerPathToContainerInfo
                            .get(metadataContainerInfo.getContainerArchiveOrFileRef());
                    updatedSinceLastSeen = metadataContainerInfo.isModified(seenMetadataContainerInfo);
                    if (updatedSinceLastSeen) {
                        debug(() -> log.debug("Container seems to have been updated. Previous version: "
                                + seenMetadataContainerInfo + "; Newer version: " + metadataContainerInfo));
                    }
                }

                boolean looksFresh = !seenBefore || updatedSinceLastSeen;
                boolean processMetadata = looksFresh && metadataContainerInfo.containsMetadataFile();
                if (processMetadata) {
                    containersToProcess.add(metadataContainerInfo);
                }

                if (looksFresh) {
                    seenContainerPathToContainerInfo
                            .put(metadataContainerInfo.getContainerArchiveOrFileRef(), metadataContainerInfo);
                }
            }
        }

        if (containersToProcess.size() == 0) {
            debug(() -> log.debug("No (new)metadata files to index"));
        }
        return containersToProcess;
    }

    private List<LookupElementBuilder> doFindSuggestionsForQueryPrefix(Module module,
                                                                       Trie<String, MetadataSuggestionNode> rootSearchIndex, FileType fileType, PsiElement element,
                                                                       @Nullable List<String> ancestralKeys, String queryWithDotDelimitedPrefixes,
                                                                       @Nullable Set<String> siblingsToExclude) {
        debug(() -> log.debug("Search requested for " + queryWithDotDelimitedPrefixes));
        StopWatch timer = new StopWatch();
        timer.start();
        try {
            String[] querySegmentPrefixes = toSanitizedPathSegments(queryWithDotDelimitedPrefixes);
            Set<Suggestion> suggestions = null;
            if (ancestralKeys != null) {
                String[] ancestralKeySegments =
                        ancestralKeys.stream().flatMap(key -> stream(toRawPathSegments(key)))
                                .toArray(String[]::new);
                MetadataSuggestionNode rootNode = rootSearchIndex.get(sanitise(ancestralKeySegments[0]));
                if (rootNode != null) {
                    List<SuggestionNode> matchesRootToDeepest;
                    SuggestionNode startSearchFrom = null;
                    if (ancestralKeySegments.length > 1) {
                        String[] sanitisedAncestralPathSegments =
                                stream(ancestralKeySegments).map(SuggestionNode::sanitise).toArray(String[]::new);
                        matchesRootToDeepest = rootNode
                                .findDeepestSuggestionNode(module, modifiableList(rootNode),
                                        sanitisedAncestralPathSegments, 1);
                        if (matchesRootToDeepest != null && matchesRootToDeepest.size() != 0) {
                            startSearchFrom = matchesRootToDeepest.get(matchesRootToDeepest.size() - 1);
                        }
                    } else {
                        startSearchFrom = rootNode;
                        matchesRootToDeepest = singletonList(rootNode);
                    }

                    if (startSearchFrom != null) {
                        // if search start node is a leaf, this means, the user is looking for values for the given key, lets find the suggestions for values
                        if (startSearchFrom.isLeaf(module)) {
                            suggestions = startSearchFrom.findValueSuggestionsForPrefix(module, fileType,
                                    unmodifiableList(matchesRootToDeepest),
                                    sanitise(truncateIdeaDummyIdentifier(element.getText())), siblingsToExclude);
                        } else {
                            suggestions = startSearchFrom.findKeySuggestionsForQueryPrefix(module, fileType,
                                    unmodifiableList(matchesRootToDeepest), matchesRootToDeepest.size(),
                                    querySegmentPrefixes, 0, siblingsToExclude);
                        }
                    }
                }
            } else {
                String rootQuerySegmentPrefix = querySegmentPrefixes[0];
                SortedMap<String, MetadataSuggestionNode> topLevelQueryResults =
                        rootSearchIndex.prefixMap(rootQuerySegmentPrefix);

                Collection<MetadataSuggestionNode> childNodes;
                int querySegmentPrefixStartIndex;

                // If no results are found at the top level, let dive deeper and find matches
                if (topLevelQueryResults == null || topLevelQueryResults.size() == 0) {
                    childNodes = rootSearchIndex.values();
                    querySegmentPrefixStartIndex = 0;
                } else {
                    childNodes = topLevelQueryResults.values();
                    querySegmentPrefixStartIndex = 1;
                }

                Collection<MetadataSuggestionNode> nodesToSearchAgainst;
                if (siblingsToExclude != null) {
                    Set<MetadataSuggestionNode> nodesToExclude = siblingsToExclude.stream()
                            .flatMap(exclude -> rootSearchIndex.prefixMap(exclude).values().stream())
                            .collect(toSet());
                    nodesToSearchAgainst =
                            childNodes.stream().filter(node -> !nodesToExclude.contains(node)).collect(toList());
                } else {
                    nodesToSearchAgainst = childNodes;
                }

                suggestions = doFindSuggestionsForQueryPrefix(module, fileType, nodesToSearchAgainst,
                        querySegmentPrefixes, querySegmentPrefixStartIndex);
            }

            if (suggestions != null) {
                return toLookupElementBuilders(suggestions);
            }
            return null;
        } finally {
            timer.stop();
            debug(() -> log.debug("Search took " + timer.toString()));
        }
    }

    @Nullable
    private Set<Suggestion> doFindSuggestionsForQueryPrefix(Module module, FileType fileType,
                                                            Collection<MetadataSuggestionNode> nodesToSearchWithin, String[] querySegmentPrefixes,
                                                            int querySegmentPrefixStartIndex) {
        Set<Suggestion> suggestions = null;
        for (MetadataSuggestionNode suggestionNode : nodesToSearchWithin) {
            Set<Suggestion> matchedSuggestions = suggestionNode
                    .findKeySuggestionsForQueryPrefix(module, fileType, modifiableList(suggestionNode), 0,
                            querySegmentPrefixes, querySegmentPrefixStartIndex);
            if (matchedSuggestions != null) {
                if (suggestions == null) {
                    suggestions = new THashSet<>();
                }
                suggestions.addAll(matchedSuggestions);
            }
        }
        return suggestions;
    }

    @Nullable
    private List<LookupElementBuilder> toLookupElementBuilders(
            @Nullable Set<Suggestion> suggestions) {
        if (suggestions != null) {
            return suggestions.stream().map(Suggestion::newLookupElement).collect(toList());
        }
        return null;
    }

    /**
     * Finds the containers that are not reachable from current classpath
     *
     * @param orderEnumerator                  classpath roots to work with
     * @param seenContainerPathToContainerInfo seen container paths
     * @return list of container paths that are no longer valid
     */
    private List<MetadataContainerInfo> computeContainersToRemove(OrderEnumerator orderEnumerator,
                                                                  Map<String, MetadataContainerInfo> seenContainerPathToContainerInfo) {
        Set<String> newContainerPaths = stream(orderEnumerator.recursively().classes().getRoots())
                .flatMap(MetadataContainerInfo::getContainerArchiveOrFileRefs).collect(toSet());
        Set<String> knownContainerPathSet = new THashSet<>(seenContainerPathToContainerInfo.keySet());
        knownContainerPathSet.removeAll(newContainerPaths);
        return knownContainerPathSet.stream().map(seenContainerPathToContainerInfo::get)
                .collect(toList());
    }

    private void processContainers(final Module module,
                                   final List<MetadataContainerInfo> containersToProcess,
                                   final List<MetadataContainerInfo> containersToRemove,
                                   final Map<String, MetadataContainerInfo> seenContainerPathToContainerInfo,
                                   final Trie<String, MetadataSuggestionNode> rootSearchIndex) {
        // Lets remove references to files that are no longer present in classpath
        containersToRemove.forEach(container -> removeReferences(seenContainerPathToContainerInfo, rootSearchIndex, container));

        for (MetadataContainerInfo metadataContainerInfo : containersToProcess) {
            // lets remove existing references from search index, as these files are modified, so that we can rebuild index
            if (seenContainerPathToContainerInfo.containsKey(metadataContainerInfo.getContainerArchiveOrFileRef())) {
                removeReferences(seenContainerPathToContainerInfo, rootSearchIndex, metadataContainerInfo);
            }

            final String metadataFilePath = metadataContainerInfo.getFileUrl();

            try (final var inputStream = metadataContainerInfo.getMetadataFile().getInputStream()) {
                final SpringConfigurationMetadata springConfigurationMetadata = JsonUtil.fromJson(inputStream, SpringConfigurationMetadata.class);

                buildMetadataHierarchy(module, rootSearchIndex, metadataContainerInfo, springConfigurationMetadata);

                seenContainerPathToContainerInfo.put(metadataContainerInfo.getContainerArchiveOrFileRef(), metadataContainerInfo);

            } catch (final IOException e) {
                log.error("Exception encountered while processing metadata file: " + metadataFilePath, e);
                removeReferences(seenContainerPathToContainerInfo, rootSearchIndex, metadataContainerInfo);
            }
        }
    }

    private void reindexModule(List<MetadataContainerInfo> newProjectSourcesToProcess,
                               List<MetadataContainerInfo> projectContainersToRemove, Module module) {

        final var moduleSeenContainerPathToSeenContainerInfo = moduleNameToSeenContainerPathToContainerInfo
                .computeIfAbsent(module.getName(), k -> new THashMap<>());

        var moduleRootSearchIndex = moduleNameToRootSearchIndex.get(module.getName());
        if (moduleRootSearchIndex == null) {
            moduleRootSearchIndex = new PatriciaTrie<>();
            moduleNameToRootSearchIndex.put(module.getName(), moduleRootSearchIndex);
        }

        final OrderEnumerator moduleOrderEnumerator = OrderEnumerator.orderEntries(module);

        final List<MetadataContainerInfo> newModuleContainersToProcess = computeNewContainersToProcess(moduleOrderEnumerator,
                moduleSeenContainerPathToSeenContainerInfo);
        newModuleContainersToProcess.addAll(newProjectSourcesToProcess);

        final List<MetadataContainerInfo> moduleContainersToRemove = computeContainersToRemove(moduleOrderEnumerator,
                moduleSeenContainerPathToSeenContainerInfo);
        moduleContainersToRemove.addAll(projectContainersToRemove);

        processContainers(module, newModuleContainersToProcess, moduleContainersToRemove,
                moduleSeenContainerPathToSeenContainerInfo, moduleRootSearchIndex);
    }

    private void buildMetadataHierarchy(Module module,
                                        Trie<String, MetadataSuggestionNode> rootSearchIndex,
                                        MetadataContainerInfo metadataContainerInfo,
                                        SpringConfigurationMetadata springConfigurationMetadata) {
        debug(() -> log.debug("Adding container to index " + metadataContainerInfo));

        final String containerPath = metadataContainerInfo.getContainerArchiveOrFileRef();
        addGroupsToIndex(module, rootSearchIndex, springConfigurationMetadata, containerPath);
        addPropertiesToIndex(module, rootSearchIndex, springConfigurationMetadata, containerPath);
        addHintsToIndex(module, rootSearchIndex, springConfigurationMetadata, containerPath);

        debug(() -> log.debug("Done adding container to index"));
    }

    private void addHintsToIndex(Module module, Trie<String, MetadataSuggestionNode> rootSearchIndex,
                                 SpringConfigurationMetadata springConfigurationMetadata, String containerPath) {
        List<SpringConfigurationMetadataHint> hints = springConfigurationMetadata.getHints();
        if (hints != null) {
            hints.sort(comparing(SpringConfigurationMetadataHint::getName));
            for (SpringConfigurationMetadataHint hint : hints) {
                String[] pathSegments = toSanitizedPathSegments(hint.getExpectedPropertyName());
                MetadataSuggestionNode closestMetadata =
                        findDeepestMetadataMatch(rootSearchIndex, pathSegments, true);
                if (closestMetadata != null) {
                    if (!closestMetadata.isProperty()) {
                        log.warn(
                                "Unexpected hint " + hint.getName() + " is assigned to  group " + closestMetadata
                                        .getPathFromRoot()
                                        + " found. Hints can be only assigned to property. Ignoring the hint completely.Existing group belongs to ("
                                        + String.join(",", closestMetadata.getBelongsTo())
                                        + "), New hint belongs " + containerPath);
                    } else {
                        MetadataPropertySuggestionNode propertySuggestionNode =
                                (MetadataPropertySuggestionNode) closestMetadata;
                        if (hint.representsValueOfMap()) {
                            propertySuggestionNode.getProperty().setValueHint(hint);
                        } else {
                            propertySuggestionNode.getProperty().setGenericOrKeyHint(hint);
                        }
                    }
                }
            }
        }
    }

    private void addPropertiesToIndex(Module module,
                                      Trie<String, MetadataSuggestionNode> rootSearchIndex,
                                      SpringConfigurationMetadata springConfigurationMetadata, String containerArchiveOrFileRef) {
        List<SpringConfigurationMetadataProperty> properties =
                springConfigurationMetadata.getProperties();
        properties.sort(comparing(SpringConfigurationMetadataProperty::getName));
        for (SpringConfigurationMetadataProperty property : properties) {
            String[] pathSegments = toSanitizedPathSegments(property.getName());
            String[] rawPathSegments = toRawPathSegments(property.getName());
            MetadataSuggestionNode closestMetadata =
                    findDeepestMetadataMatch(rootSearchIndex, pathSegments, false);

            int startIndex;
            if (closestMetadata == null) { // path does not have a corresponding root element
                boolean onlyRootSegmentExists = pathSegments.length == 1;
                if (onlyRootSegmentExists) {
                    closestMetadata = MetadataPropertySuggestionNode
                            .newInstance(rawPathSegments[0], property, null, containerArchiveOrFileRef);
                } else {
                    closestMetadata = MetadataNonPropertySuggestionNode
                            .newInstance(rawPathSegments[0], null, containerArchiveOrFileRef);
                }
                rootSearchIndex.put(pathSegments[0], closestMetadata);

                // since we already handled the root level item, let addChildren start from index 1 of pathSegments
                startIndex = 1;
            } else {
                startIndex = closestMetadata.numOfHopesToRoot() + 1;
            }

            boolean haveMoreSegmentsLeft = startIndex < rawPathSegments.length;

            if (haveMoreSegmentsLeft) {
                if (!closestMetadata.isProperty()) {
                    ((MetadataNonPropertySuggestionNode) closestMetadata)
                            .addChildren(property, rawPathSegments, startIndex, containerArchiveOrFileRef);
                } else {
                    log.warn("Detected conflict between a new group & existing property for suggestion path "
                            + closestMetadata.getPathFromRoot()
                            + ". Ignoring property. Existing non property node belongs to (" + String.join(",", closestMetadata
                            .getBelongsTo()) + "), New property belongs to "
                            + containerArchiveOrFileRef);
                }
            } else {
                if (!closestMetadata.isProperty()) {
                    log.warn(
                            "Detected conflict between a new metadata property & existing non property node for suggestion path "
                                    + closestMetadata.getPathFromRoot()
                                    + ". Ignoring property. Existing non property node belongs to (" + String.join(",", closestMetadata
                                    .getBelongsTo()) + "), New property belongs to "
                                    + containerArchiveOrFileRef);
                } else {
                    closestMetadata.addRefCascadeTillRoot(containerArchiveOrFileRef);
                    log.debug("Detected a duplicate metadata property for suggestion path " + closestMetadata
                            .getPathFromRoot() + ". Ignoring property. Existing property belongs to ("
                            + String.join(",", closestMetadata.getBelongsTo())
                            + "), New property belongs to " + containerArchiveOrFileRef);
                }
            }
        }
    }

    private void addGroupsToIndex(Module module, Trie<String, MetadataSuggestionNode> rootSearchIndex,
                                  SpringConfigurationMetadata springConfigurationMetadata, String containerArchiveOrFileRef) {
        final var springConfigurationMetadataGroups = springConfigurationMetadata.getGroups();

        if (springConfigurationMetadataGroups == null) {
            return;
        }

        springConfigurationMetadataGroups.sort(comparing(SpringConfigurationMetadataGroup::getName));

        springConfigurationMetadataGroups.forEach(springConfigurationMetadataGroup -> {
            final String[] pathSegments = toSanitizedPathSegments(springConfigurationMetadataGroup.getName());
            final String[] rawPathSegments = toRawPathSegments(springConfigurationMetadataGroup.getName());

            MetadataSuggestionNode closestMetadata = findDeepestMetadataMatch(rootSearchIndex, pathSegments, false);

            int startIndex;
            if (closestMetadata == null) { // path does not have a corresponding root element
                // lets build just the root element. Rest of the path segments will be taken care of by the addChildren method
                boolean onlyRootSegmentExists = pathSegments.length == 1;
                MetadataNonPropertySuggestionNode newGroupSuggestionNode = MetadataNonPropertySuggestionNode
                        .newInstance(rawPathSegments[0], null, containerArchiveOrFileRef);

                if (onlyRootSegmentExists) {
                    newGroupSuggestionNode.setGroup(module, springConfigurationMetadataGroup);
                }

                rootSearchIndex.put(pathSegments[0], newGroupSuggestionNode);
                closestMetadata = newGroupSuggestionNode;
                // since we already handled the root level item, let addChildren start from index 1 of pathSegments
                startIndex = 1;
            } else {
                startIndex = closestMetadata.numOfHopesToRoot() + 1;
            }

            if (closestMetadata.isProperty()) {
                log.warn("Detected conflict between an existing metadata property & new springConfigurationMetadataGroup for suggestion path "
                        + closestMetadata.getPathFromRoot()
                        + ". Ignoring new springConfigurationMetadataGroup. Existing Property belongs to ("
                        + String.join(",", closestMetadata.getBelongsTo())
//                        + closestMetadata.getBelongsTo().stream().collect(joining(","))
                        + "), New Group belongs to " + containerArchiveOrFileRef);
            } else {
                // lets add container as a reference till root
                final var groupSuggestionNode = (MetadataNonPropertySuggestionNode) closestMetadata;
                groupSuggestionNode.addRefCascadeTillRoot(containerArchiveOrFileRef);

                final boolean haveMoreSegmentsLeft = startIndex < rawPathSegments.length;
                if (haveMoreSegmentsLeft) {
                    groupSuggestionNode.addChildren(module, springConfigurationMetadataGroup, rawPathSegments, startIndex, containerArchiveOrFileRef);
                } else {
                    // Node is an intermediate node that has neither springConfigurationMetadataGroup nor property assigned to it, lets assign this springConfigurationMetadataGroup to it
                    // Can happen when `a.b.c` is already added to the metadata tree from an earlier metadata source & now we are trying to add a springConfigurationMetadataGroup for `a.b`
                    // In this e.g, startIndex would be 2. So, there is no point in adding children. We only need to update the tree appropriately
                    groupSuggestionNode.setGroup(module, springConfigurationMetadataGroup);
                }
            }
        });
    }

    private MetadataSuggestionNode findDeepestMetadataMatch(final Map<String, MetadataSuggestionNode> roots,
                                                            final String[] pathSegments,
                                                            final boolean matchAllSegments) {
        final MetadataSuggestionNode closestMatchedRoot = roots.get(pathSegments[0]);
        return closestMatchedRoot == null ? null
                : closestMatchedRoot.findDeepestMetadataNode(pathSegments, 1, matchAllSegments);
    }

    private void removeReferences(final Map<String, MetadataContainerInfo> containerPathToContainerInfo,
                                  final Trie<String, MetadataSuggestionNode> rootSearchIndex,
                                  final MetadataContainerInfo metadataContainerInfo) {
        debug(() -> log.debug("Removing references to " + metadataContainerInfo));

        final String containerPath = metadataContainerInfo.getContainerArchiveOrFileRef();
        containerPathToContainerInfo.remove(containerPath);

        final Iterator<String> searchIndexIterator = rootSearchIndex.keySet().iterator();

        while (searchIndexIterator.hasNext()) {
            MetadataSuggestionNode suggestionNode = rootSearchIndex.get(searchIndexIterator.next());
            if (suggestionNode != null && suggestionNode.removeRefCascadeDown(metadataContainerInfo.getContainerArchiveOrFileRef())) {
                searchIndexIterator.remove();
            }
        }

    }

    @SuppressWarnings("unused")
    private String toTree() {
        final StringBuilder builder = new StringBuilder();
        moduleNameToRootSearchIndex.forEach((key, value) -> {
            builder.append("Module: ")
                    .append(key)
                    .append(StringUtils.LF);

            value.values().forEach(root -> builder
                    .append(root.toTree().trim().replaceAll("^", "  ").replaceAll(StringUtils.LF, "\n  "))
                    .append(StringUtils.LF));
        });
        return builder.toString();
    }

    /**
     * Debug logging can be enabled by adding fully classified class name/package name with # prefix
     * For eg., to enable debug logging, go `Help > Debug log settings` & type `#in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionServiceImpl`
     *
     * @param doWhenDebug code to execute when debug is enabled
     */
    private void debug(final Runnable doWhenDebug) {
        if (log.isDebugEnabled()) {
            doWhenDebug.run();
        }
    }
}
