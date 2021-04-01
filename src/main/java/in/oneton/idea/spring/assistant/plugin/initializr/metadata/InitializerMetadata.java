package in.oneton.idea.spring.assistant.plugin.initializr.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.Version;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.VersionRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Data
@ToString
public class InitializerMetadata {

    @JsonProperty("dependencies")
    private DependencyComposite dependencyComposite;

    @JsonProperty("type")
    private ProjectTypeComposite projectTypeComposite;

    @JsonProperty("packaging")
    private IdAndNameComposite packagingTypeComposite;

    @JsonProperty("javaVersion")
    private IdAndNameComposite javaVersionComposite;

    @JsonProperty("language")
    private IdAndNameComposite languageComposite;

    @JsonProperty("bootVersion")
    private IdAndNameComposite bootVersionComposite;

    @JsonProperty("groupId")
    private DefaultValueHolder groupIdHolder;

    @JsonProperty("artifactId")
    private DefaultValueHolder artifactIdHolder;

    @JsonProperty("version")
    private DefaultValueHolder versionHolder;

    @JsonProperty("name")
    private DefaultValueHolder nameHolder;

    @JsonProperty("description")
    private DefaultValueHolder descriptionHolder;

    @JsonProperty("packageName")
    private DefaultValueHolder packageNameHolder;


    public interface IdContainer {
        String getId();
    }


    @Data
    public static class DependencyComposite {

        @JsonProperty("values")
        private List<DependencyGroup> groups;

        @NotNull
        public Optional<DependencyGroup> findGroupForDependency(Dependency dependency) {
            return groups.stream()
                    .filter(group -> group.getDependencies().stream().anyMatch(dep -> dep.equals(dependency)))
                    .findFirst();
        }

        @Data
        @FieldNameConstants
        @EqualsAndHashCode(onlyExplicitlyIncluded = true)
        public static class DependencyGroup {

            @EqualsAndHashCode.Include
            private String name;

            @JsonProperty("values")
            private List<Dependency> dependencies;


            @Data
            @EqualsAndHashCode(onlyExplicitlyIncluded = true)
            public static class Dependency {

                @EqualsAndHashCode.Include
                private String id;
                private String name;
                private String description;

                @Nullable
                private VersionRange versionRange;

                @Nullable
                @JsonProperty("_links")
                private DependencyLinksContainer linksContainer;

                @SuppressWarnings("BooleanMethodIsAlwaysInverted")
                public boolean isVersionCompatible(Version bootVersion) {
                    return versionRange == null || versionRange.match(bootVersion);
                }


                @Data
                public static class DependencyLinksContainer {

                    @Nullable
                    @JsonProperty("reference")
                    private List<DependencyLink> references;

                    @Nullable
                    @JsonProperty("guide")
                    private List<DependencyLink> guides;


                    @Data
                    @Builder
                    @NoArgsConstructor
                    @AllArgsConstructor
                    public static class DependencyLink {

                        private String href;

                        @Nullable
                        private String title;
                        private boolean templated;

                        @NotNull
                        public String getHrefAfterReplacement(final String bootVersion) {
                            if (templated) { //TODO: > Analisar replace
                                return href.replace("\\{bootVersion}", bootVersion);
                            }
                            return href;
                        }
                    }
                }
            }

            @Override
            public String toString() {
                return name;
            }
        }
    }


    @Data
    public static class ProjectTypeComposite {

        @JsonProperty("default")
        private String defaultValue;

        @JsonProperty("values")
        private List<ProjectType> types;


        @Data
        @EqualsAndHashCode(onlyExplicitlyIncluded = true)
        public static class ProjectType implements IdContainer {

            @EqualsAndHashCode.Include
            private String id;

            private String name;

            private String description;
            private String action;

            @Override
            public String toString() {
                return name;
            }
        }
    }


    @Data
    public static class IdAndNameComposite {

        @JsonProperty("default")
        private String defaultValue;

        private List<IdAndName> values;
    }


    @Data
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class IdAndName implements IdContainer {

        @EqualsAndHashCode.Include
        private String id;

        private String name;

        public Version parseIdAsVersion() {
            return Version.parse(id);
        }

        @Override
        public String toString() {
            return name;
        }
    }


    @Data
    public static class DefaultValueHolder {

        @JsonProperty("default")
        private String defaultValue;
    }

}
