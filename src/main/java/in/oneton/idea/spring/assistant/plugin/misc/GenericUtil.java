package in.oneton.idea.spring.assistant.plugin.misc;

import com.google.common.base.Splitter;
import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED;
import static com.intellij.codeInsight.documentation.DocumentationManager.createHyperlink;
import static com.intellij.openapi.util.text.StringUtil.containsChar;
import static com.intellij.openapi.util.text.StringUtil.endsWithChar;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static com.intellij.openapi.util.text.StringUtil.replace;
import static java.text.BreakIterator.getSentenceInstance;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

@UtilityClass
public class GenericUtil {

    private static final Pattern PACKAGE_REMOVAL_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*\\.");

    private static final Pattern GENERIC_SECTION_REMOVAL_PATTERN = Pattern.compile("<(?<commaDelimitedTypes>[^>]+)>");

    private static final Pattern methodToFragmentConverter = Pattern.compile("(.+)\\.(.+)\\(.*\\)");

    public static String typeForDocumentationNavigation(final String type) {
        return type.replaceAll("\\$", ".");
    }

    public static void updateClassNameAsJavadocHtml(final StringBuilder buffer, final String type) {
        final Matcher matcher = GENERIC_SECTION_REMOVAL_PATTERN.matcher(type);
        String baseClass = type;

        final boolean parametersPresent = matcher.find();
        String[] typeParameters = null;

        if (parametersPresent) {
            typeParameters = matcher.group("commaDelimitedTypes").split(",");
            baseClass = matcher.replaceAll(StringUtils.EMPTY);
        }

        createHyperlink(buffer, typeForDocumentationNavigation(baseClass), baseClass, false);
        if (typeParameters != null) {
            buffer.append("&lt;");

            for (int i = 0; i < typeParameters.length; i++) {
                updateClassNameAsJavadocHtml(buffer, typeParameters[i]);

                if (i != typeParameters.length - 1) {
                    buffer.append(", ");
                }
            }

            buffer.append("&gt;");
        }
    }

    public static String methodForDocumentationNavigation(final String typeAndMethod) {
        return methodToFragmentConverter.matcher(typeForDocumentationNavigation(typeAndMethod))
                .replaceAll("$1#$2");
    }

    @NotNull
    public static String getCodeStyleIntent(final InsertionContext insertionContext) {
        final CodeStyleSettings currentSettings = CodeStyle.getSettings(insertionContext.getProject());
        final CommonCodeStyleSettings.IndentOptions indentOptions =
                currentSettings.getIndentOptions(insertionContext.getFile().getFileType());

        return indentOptions.USE_TAB_CHARACTER ? "\t" : StringUtil.repeatSymbol(' ', indentOptions.INDENT_SIZE);
    }

    @NotNull
    public static String getFirstSentenceWithoutDot(String fullSentence) {
        if (containsChar(fullSentence, '.')) {
            final BreakIterator breakIterator = getSentenceInstance(Locale.US);
            breakIterator.setText(fullSentence);
            fullSentence = fullSentence.substring(breakIterator.first(), breakIterator.next()).trim();
        }

        if (isNotEmpty(fullSentence)) {
            final String withoutDot = endsWithChar(fullSentence, '.') ?
                    fullSentence.substring(0, fullSentence.length() - 1) : fullSentence;

            return replace(withoutDot, StringUtils.LF, StringUtils.EMPTY);
        }

        return StringUtils.EMPTY;
    }

    public static String moduleNamesAsStrCommaDelimited(final List<Module> newModules, final boolean includeProjectName) {
        return moduleNamesAsStrCommaDelimited(newModules.stream(), includeProjectName);
    }

    public static String moduleNamesAsStrCommaDelimited(final Module[] newModules, final boolean includeProjectName) {
        return moduleNamesAsStrCommaDelimited(stream(newModules), includeProjectName);
    }

    private static String moduleNamesAsStrCommaDelimited(final Stream<Module> moduleStream, final boolean includeProjectName) {
        return moduleStream.map(module -> includeProjectName ?
                module.getProject().getName() + ":" + module.getName() :
                module.getName()).collect(joining(", "));
    }

    public static String truncateIdeaDummyIdentifier(@NotNull final PsiElement element) {
        return truncateIdeaDummyIdentifier(element.getText());
    }

    public static String truncateIdeaDummyIdentifier(final String text) {
        return text.replace(DUMMY_IDENTIFIER_TRIMMED, StringUtils.EMPTY);
    }

    @SafeVarargs
    public static <T> List<T> modifiableList(final T... items) {
        return new ArrayList<>(asList(items));
    }

    public static <T> List<T> newListWithMembers(final List<T> itemsToCopy, final T newItem) {
        final ArrayList<T> newModifiableList = new ArrayList<>(itemsToCopy);
        newModifiableList.add(newItem);
        return newModifiableList;
    }

    public static String removeGenerics(final String type) {
        final Matcher matcher = GENERIC_SECTION_REMOVAL_PATTERN.matcher(type);
        if (matcher.find()) {
            return matcher.replaceAll(StringUtils.EMPTY);
        }
        return type;
    }

    public static String shortenedType(final String type) {
        if (type == null) {
            return null;
        }
        final Matcher matcher = PACKAGE_REMOVAL_PATTERN.matcher(type);
        if (matcher.find()) {
            return matcher.replaceAll(StringUtils.EMPTY);
        }
        return type;
    }

    public static String dotDelimitedOriginalNames(
            final List<? extends SuggestionNode> matchesTopFirstTillParentNode, final SuggestionNode currentNode) {
        final StringBuilder builder = new StringBuilder();

        for (final SuggestionNode aMatchesTopFirstTillParentNode : matchesTopFirstTillParentNode) {
            final String originalName = aMatchesTopFirstTillParentNode.getOriginalName();
            if (originalName != null) {
                builder.append(originalName).append(".");
            }
        }

        final String originalName = currentNode.getOriginalName();
        if (originalName != null) {
            builder.append(originalName);
        }
        return builder.toString();
    }

    public static String dotDelimitedOriginalNames(final List<? extends SuggestionNode> matches) {
        return dotDelimitedOriginalNames(matches, 0);
    }

    public static String dotDelimitedOriginalNames(final List<? extends SuggestionNode> matches, final int startIndex) {
        final StringBuilder builder = new StringBuilder();

        for (int i = startIndex; i < matches.size(); i++) {
            final String originalName = matches.get(i).getOriginalName();
            if (originalName != null) {
                builder.append(originalName);
                final boolean appendDot = i < matches.size() - 1;
                if (appendDot) {
                    builder.append(".");
                }
            }
        }
        return builder.toString();
    }

    @NotNull
    public static String getIndent(final String indent, final int numOfHops) {
        if (numOfHops == 0) {
            return StringUtils.EMPTY;
        }

        return String.valueOf(indent).repeat(Math.max(0, numOfHops));
    }

    @NotNull
    public static String getOverallIndent(final String existingIndentation, final String indentPerLevel, final int numOfLevels) {
        return existingIndentation + getIndent(indentPerLevel, numOfLevels);
    }

    @NotNull
    public static <T extends Comparable<T>> SortedSet<T> newSingleElementSortedSet(final T t) {
        final SortedSet<T> suggestions = new TreeSet<>();
        suggestions.add(t);

        return suggestions;
    }

    public static Optional<String> getKeyNameOfObject(final PsiElement psiElement) {
        return Optional.of(psiElement).filter(el -> el instanceof YAMLKeyValue)
                .map(YAMLKeyValue.class::cast).map(YAMLKeyValue::getName);
    }

    public static List<String> getAncestralKey(String elementVal) {
        String ancestralStr = elementVal.replaceAll("\\[\\d+]", "");
        List<String> split = Splitter.on(".").splitToList(ancestralStr);
        if (split.size() == 1) {
            return null;
        }
        return split.subList(0, split.size() - 1);
    }
}
