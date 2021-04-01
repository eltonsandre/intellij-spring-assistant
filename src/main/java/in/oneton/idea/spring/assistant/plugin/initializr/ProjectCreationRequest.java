package in.oneton.idea.spring.assistant.plugin.initializr;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.pom.java.LanguageLevel;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.IdAndName;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.IdContainer;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.ProjectTypeComposite.ProjectType;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.Version;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.intellij.openapi.projectRoots.JavaSdkVersion.fromLanguageLevel;
import static com.intellij.openapi.util.io.FileUtil.sanitizeFileName;
import static com.intellij.pom.java.LanguageLevel.parse;
import static com.intellij.psi.impl.PsiNameHelperImpl.getInstance;
import static in.oneton.idea.spring.assistant.plugin.initializr.misc.InitializrUtil.from;
import static in.oneton.idea.spring.assistant.plugin.initializr.misc.InitializrUtil.nameAndValueAsUrlParam;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Data
public class ProjectCreationRequest {

    private String serverUrl;
    private InitializerMetadata metadata;

    private ProjectType type;
    private String groupId;
    private String artifactId;
    private String version;
    private String name;
    private String description;
    private String packageName;
    private IdAndName language;
    private IdAndName javaVersion;
    private IdAndName packaging;

    private Version bootVersion;
    private LinkedHashSet<Dependency> dependencies = new LinkedHashSet<>();

    private static String sanitize(String input) {
        return sanitizeFileName(input, false).replace(' ', '-').toLowerCase();
    }

    public void setServerUrl(String serverUrl) {
        if (!serverUrl.equals(this.serverUrl)) {
            this.serverUrl = serverUrl;
            metadata = null;
        }
    }

    public boolean isJavaVersionSet() {
        return javaVersion != null;
    }

    @Nullable
    public Dependency getDependencyAtIndex(int index) {
        Iterator<Dependency> iterator = dependencies.iterator();
        Dependency dependency = null;
        while (index >= 0) {
            if (iterator.hasNext()) {
                dependency = iterator.next();
            } else {
                dependency = null;
                break;
            }
            index--;
        }
        return dependency;
    }

    public int getIndexOfDependency(@NotNull Dependency dependency) {
        Iterator<Dependency> iterator = dependencies.iterator();
        int dependencyIndex = -1;
        int index = 0;
        while (iterator.hasNext()) {
            if (iterator.next().equals(dependency)) {
                dependencyIndex = index;
                break;
            }
            index++;
        }
        return dependencyIndex;
    }

    public boolean addDependency(Dependency dependency) {
        return dependencies.add(dependency);
    }

    public boolean removeDependency(Dependency dependency) {
        return dependencies.removeIf(v -> v.equals(dependency));
    }

    public void removeIncompatibleDependencies(Version newVersion) {
        dependencies.removeIf(dependency -> !dependency.isVersionCompatible(newVersion));
    }

    public int getDependencyCount() {
        return dependencies.size();
    }

    public boolean containsDependency(Dependency dependency) {
        return getIndexOfDependency(dependency) != -1;
    }

    public boolean hasValidGroupId() {
        return !isEmpty(groupId);
    }

    public boolean hasValidArtifactId() {
        return !isEmpty(artifactId) && sanitize(artifactId).equals(artifactId);
    }

    public boolean hasValidVersion() {
        return !isEmpty(version);
    }

    public boolean hasValidName() {
        return !isEmpty(name);
    }

    public boolean hasCompatibleJavaVersion(ModuleBuilder moduleBuilder,
                                            WizardContext wizardContext) {
        JavaSdkVersion wizardSdkVersion = from(wizardContext, moduleBuilder);
        if (wizardSdkVersion != null) {
            LanguageLevel selectedLanguageLevel = parse(javaVersion.getId());
            JavaSdkVersion selectedSdkVersion =
                    selectedLanguageLevel != null ? fromLanguageLevel(selectedLanguageLevel) : null;
            // only if selected java version is compatible with wizard version
            return selectedSdkVersion == null || wizardSdkVersion.isAtLeast(selectedSdkVersion);
        }
        return true;
    }

    public boolean hasValidPackageName() {
        return !isEmpty(packageName) && getInstance().isQualifiedName(packageName);
    }

    public boolean isServerUrlSet() {
        return !isEmpty(serverUrl);
    }

    public String buildDownloadUrl() {
        //@formatter:off
        return serverUrl + "/" + type.getAction() + "?"
                + nameAndValueAsUrlParam("type", type.getId()) + "&"
                + nameAndValueAsUrlParam("groupId", groupId) + "&"
                + nameAndValueAsUrlParam("artifactId", artifactId) + "&"
                + nameAndValueAsUrlParam("version", version) + "&"
                + nameAndValueAsUrlParam("name", name) + "&"
                + nameAndValueAsUrlParam("description", description) + "&"
                + nameAndValueAsUrlParam("packageName", packageName) + "&"
                + nameAndValueAsUrlParam("language", language.getId()) + "&"
                + nameAndValueAsUrlParam("javaVersion", javaVersion.getId()) + "&"
                + nameAndValueAsUrlParam("packaging", packaging.getId()) + "&"
                + nameAndValueAsUrlParam("bootVersion", bootVersion.toString()) + "&"
                + dependencies.stream().map(Dependency::getId).map(dependencyId -> nameAndValueAsUrlParam("dependencies", dependencyId)).collect(joining("&"));
        //@formatter:on
    }

    public <T> T getSetProperty(@NotNull Consumer<T> setter, @NotNull Supplier<T> getter,
                                @Nullable T defaultValue) {
        if (getter.get() == null) {
            setter.accept(defaultValue);
        }
        return getter.get();
    }

    @Nullable
    public <T extends IdContainer> T getSetIdContainer(@NotNull Consumer<T> setter,
                                                       @NotNull Supplier<T> getter, @NotNull Collection<T> containers, @Nullable String defaultId) {
        if (getter.get() == null && defaultId != null) {
            containers.stream().filter(packagingType -> packagingType.getId().equals(defaultId))
                    .findFirst().ifPresent(setter);
        }
        return getter.get();
    }

    @Nullable
    public Version getSetVersion(@NotNull Collection<IdAndName> containers,
                                 @Nullable String defaultVersionId) {
        if (bootVersion == null && defaultVersionId != null) {
            Version defaultVersion = Version.parse(defaultVersionId);
            containers.stream().filter(idAndName -> idAndName.parseIdAsVersion().equals(defaultVersion))
                    .findFirst().ifPresent(v -> bootVersion = v.parseIdAsVersion());
        }
        return bootVersion;
    }

}
