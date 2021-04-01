package in.oneton.idea.spring.assistant.plugin.suggestion.metadata;

import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import static com.intellij.openapi.fileTypes.FileTypes.ARCHIVE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Stream.of;

@Getter
@Builder
@ToString
public class MetadataContainerInfo {
    public static final String SPRING_CONFIGURATION_METADATA_JSON =
            "spring-configuration-metadata.json";
    public static final String ADDITIONAL_SPRING_CONFIGURATION_METADATA_JSON =
            "additional-spring-configuration-metadata.json";
    /**
     * Can point to archive/directory containing the metadata file
     */
    private final String containerArchiveOrFileRef;
    @Nullable
    private final String fileUrl;
    private final boolean archive;
    /**
     * If containerPath points to archive, then represents the timestamp of the archive
     * else, represents the length of the generated metadata file
     */
    private final long marker;

    public static Stream<String> getContainerArchiveOrFileRefs(VirtualFile fileContainer) {
        if (fileContainer.getFileType() == ARCHIVE) {
            return of(getContainerFile(fileContainer).getUrl());
        } else {
            VirtualFile metadataFile =
                    findMetadataFile(fileContainer, SPRING_CONFIGURATION_METADATA_JSON);
            VirtualFile additionalMetadataFile =
                    findMetadataFile(fileContainer, ADDITIONAL_SPRING_CONFIGURATION_METADATA_JSON);
            if (metadataFile == null && additionalMetadataFile == null) {
                return of(fileContainer.getUrl());
            } else {
                if (metadataFile == null) {
                    return of(additionalMetadataFile.getUrl());
                } else if (additionalMetadataFile == null) {
                    return of(metadataFile.getUrl());
                } else {
                    return of(metadataFile.getUrl(), additionalMetadataFile.getUrl());
                }
            }
        }
    }

    private static VirtualFile getContainerFile(VirtualFile fileContainer) {
        if (fileContainer.getFileType() == ARCHIVE) {
            return requireNonNull(JarFileSystem.getInstance().getLocalVirtualFileFor(fileContainer));
        } else {
            return fileContainer;
        }
    }

    private static VirtualFile findMetadataFile(VirtualFile root, String metadataFileName) {
        if (!root.is(VFileProperty.SYMLINK)) {
            //noinspection UnsafeVfsRecursion
            for (VirtualFile child : root.getChildren()) {
                if (child.getName().equals(metadataFileName)) {
                    return child;
                }
                VirtualFile matchedFile = findMetadataFile(child, metadataFileName);
                if (matchedFile != null) {
                    return matchedFile;
                }
            }
        }
        return null;
    }

    public static Collection<MetadataContainerInfo> newInstances(VirtualFile fileContainer) {
        Collection<MetadataContainerInfo> containerInfos = new ArrayList<>();
        VirtualFile containerFile = getContainerFile(fileContainer);
        boolean archive = fileContainer.getFileType() == ARCHIVE;
        MetadataContainerInfo containerInfo =
                newInstance(fileContainer, containerFile, SPRING_CONFIGURATION_METADATA_JSON, archive);
        containerInfos.add(containerInfo);
        if (!archive) {
            // Even after enabling annotation processor support in intellij, for the projects with `spring-boot-configuration-processor` in classpath, intellij is not merging `spring-configuration-metadata.json` & the generated `additional-spring-configuration-metadata.json`. So lets merge these two ourselves if root is not an archive
            MetadataContainerInfo additionalContainerInfo =
                    newInstance(fileContainer, containerFile, ADDITIONAL_SPRING_CONFIGURATION_METADATA_JSON,
                            false);
            if (additionalContainerInfo != null) {
                containerInfos.add(additionalContainerInfo);
            }
        }
        return containerInfos;
    }

    private static MetadataContainerInfo newInstance(VirtualFile fileContainer,
                                                     VirtualFile containerFile, String metadataFileName, boolean archive) {
        MetadataContainerInfoBuilder builder = MetadataContainerInfo.builder().archive(archive);
        VirtualFile metadataFile = findMetadataFile(fileContainer, metadataFileName);
        if (metadataFile != null) {
            // since build might auto generate the metadata file in the project, its better to rely on
            builder.fileUrl(metadataFile.getUrl())
                    .containerArchiveOrFileRef(archive ? containerFile.getUrl() : metadataFile.getUrl())
                    .marker(
                            archive ? metadataFile.getModificationCount() : metadataFile.getModificationStamp());
        } else {
            builder.containerArchiveOrFileRef(containerFile.getUrl())
                    .marker(containerFile.getModificationCount());
        }
        return builder.build();
    }

    public boolean isModified(MetadataContainerInfo other) {
        return other != null && this.marker != other.marker;
    }

    public boolean containsMetadataFile() {
        return fileUrl != null;
    }

    public VirtualFile getMetadataFile() {
        assert fileUrl != null;
        return VirtualFileManager.getInstance().findFileByUrl(fileUrl);
    }
}
