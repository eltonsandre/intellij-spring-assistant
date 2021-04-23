package in.oneton.idea.spring.assistant.plugin.initializr;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

import static com.intellij.openapi.util.io.FileUtil.copy;
import static com.intellij.openapi.util.io.FileUtil.createTempFile;
import static com.intellij.openapi.util.text.StringUtil.isEmpty;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static com.intellij.openapi.vfs.newvfs.RefreshQueue.getInstance;
import static com.intellij.util.io.HttpRequests.request;
import static com.intellij.util.io.ZipUtil.extract;
import static in.oneton.idea.spring.assistant.plugin.initializr.misc.InitializrUtil.markAsExecutable;
import static in.oneton.idea.spring.assistant.plugin.initializr.misc.InitializrUtil.userAgent;
import static java.util.Objects.requireNonNull;

class InitializerDownloader {
    private static final Logger log = Logger.getInstance(InitializerDownloader.class);

    private final InitializrModuleBuilder builder;

    InitializerDownloader(InitializrModuleBuilder builder) {
        this.builder = builder;
    }

    @NotNull
    private String extractFilenameFromContentDisposition(@Nullable String contentDispositionHeader) {
        String fileName = null;
        if (contentDispositionHeader != null) {
            fileName = contentDispositionHeader
                    .replaceFirst(".*filename=\"?(?<fileName>[^;\"]+);?\"?.*", "${fileName}");
        }
        return !isEmpty(fileName) ? fileName : "unknown";
    }

    void execute(ProgressIndicator indicator) throws IOException {
        File tempFile = createTempFile("spring-assistant-template", ".tmp", true);
        String downloadUrl = builder.safeGetProjectCreationRequest().buildDownloadUrl();
        debug(() -> log.debug("Downloading project from: " + downloadUrl));
        Download download = request(downloadUrl).userAgent(userAgent()).connect(request -> {
            String contentType = request.getConnection().getContentType();
            boolean zip = isNotEmpty(contentType) && contentType.startsWith("application/zip");
            String contentDisposition = request.getConnection().getHeaderField("Content-Disposition");
            String fileName = extractFilenameFromContentDisposition(contentDisposition);
            indicator.setText(fileName);
            request.saveToFile(tempFile, indicator);
            return Download.builder().zip(zip).fileName(fileName).build();
        });
        indicator.setText("Please wait ...");
        File targetExtractionDir = new File(requireNonNull(builder.getContentEntryPath()));
        if (download.zip) {
            extract(tempFile.toPath(), targetExtractionDir.toPath(), null);
            markAsExecutable(targetExtractionDir, "gradlew");
            markAsExecutable(targetExtractionDir, "mvnw");
        } else {
            copy(tempFile, new File(targetExtractionDir, download.fileName));
        }

        VirtualFile targetFile =
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetExtractionDir);
        getInstance().refresh(false, true, null, targetFile);
    }

    /**
     * Debug logging can be enabled by adding fully classified class name/package name with # prefix
     * For eg., to enable debug logging, go `Help > Debug log settings` & type `#in.oneton.idea.spring.assistant
     *
     * @param doWhenDebug code to execute when debug is enabled
     */
    private void debug(Runnable doWhenDebug) {
        if (log.isDebugEnabled()) {
            doWhenDebug.run();
        }
    }


    @Builder
    private static class Download {
        private final boolean zip;
        private final String fileName;
    }

}
