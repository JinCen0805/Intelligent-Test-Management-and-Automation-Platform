package com.testpilot.platform.automation;

import com.testpilot.platform.common.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Service
public class AllureReportStorage {

    private static final long MAX_ARCHIVE_BYTES = 200L * 1024 * 1024;
    private static final int MAX_ENTRIES = 20_000;

    private final Path root;

    public AllureReportStorage(
            @Value("${testpilot.reports.storage-dir:data/reports}") String storageDir
    ) {
        this.root = Path.of(storageDir).toAbsolutePath().normalize();
    }

    public boolean store(Long executionId, byte[] archive) {
        if (archive == null || archive.length == 0) {
            return false;
        }
        if (archive.length > MAX_ARCHIVE_BYTES) {
            throw new BusinessException(50218, "Allure 离线报告超过大小限制");
        }
        Path executionRoot = root.resolve(String.valueOf(executionId)).normalize();
        Path tempRoot = root.resolve("." + executionId + ".tmp").normalize();
        ensureInsideRoot(executionRoot);
        ensureInsideRoot(tempRoot);
        try {
            Files.createDirectories(root);
            deleteRecursively(tempRoot);
            Files.createDirectories(tempRoot);
            Files.write(tempRoot.resolve("allure-report.zip"), archive);
            extract(archive, tempRoot);
            deleteRecursively(executionRoot);
            Files.move(tempRoot, executionRoot, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException exception) {
            deleteRecursively(tempRoot);
            throw new BusinessException(50219, "Allure 离线报告保存失败");
        }
    }

    public Resource reportResource(Long executionId, String requestedPath) {
        Path file = resolveReportPath(executionId, requestedPath);
        try {
            return new UrlResource(file.toUri());
        } catch (IOException exception) {
            throw new BusinessException(40457, "离线 Allure 报告文件不存在");
        }
    }

    public void delete(Long executionId) {
        Path executionRoot = root.resolve(String.valueOf(executionId)).normalize();
        ensureInsideRoot(executionRoot);
        deleteRecursively(executionRoot);
    }

    public boolean hasReport(Long executionId) {
        Path executionRoot = root.resolve(String.valueOf(executionId)).normalize();
        return Files.isRegularFile(executionRoot.resolve("allure-report.zip"))
                && (Files.isRegularFile(executionRoot.resolve("index.html"))
                || Files.isRegularFile(executionRoot.resolve("allure-report/index.html")));
    }

    public Resource archiveResource(Long executionId) {
        Path archive = root.resolve(String.valueOf(executionId))
                .resolve("allure-report.zip").normalize();
        ensureInsideRoot(archive);
        if (!Files.isRegularFile(archive)) {
            throw new BusinessException(40457, "离线 Allure 报告不存在");
        }
        try {
            return new UrlResource(archive.toUri());
        } catch (IOException exception) {
            throw new BusinessException(40457, "离线 Allure 报告不存在");
        }
    }

    private Path resolveReportPath(Long executionId, String requestedPath) {
        String path = requestedPath == null || requestedPath.isBlank()
                ? "index.html" : requestedPath;
        while (path.startsWith("/") || path.startsWith("\\")) {
            path = path.substring(1);
        }
        Path executionRoot = root.resolve(String.valueOf(executionId)).normalize();
        ensureInsideRoot(executionRoot);
        if (!Files.isDirectory(executionRoot)) {
            throw new BusinessException(40457, "离线 Allure 报告不存在");
        }
        Path reportRoot = Files.isRegularFile(executionRoot.resolve("index.html"))
                ? executionRoot
                : executionRoot.resolve("allure-report").normalize();
        if (path.equals("allure-report")) {
            path = "index.html";
        } else if (path.startsWith("allure-report/")) {
            path = path.substring("allure-report/".length());
        }
        Path file = reportRoot.resolve(path).normalize();
        if (!file.startsWith(reportRoot) || !Files.isRegularFile(file)) {
            throw new BusinessException(40457, "离线 Allure 报告文件不存在");
        }
        return file;
    }

    private void extract(byte[] archive, Path target) throws IOException {
        int entries = 0;
        long extractedBytes = 0;
        try (InputStream input = new java.io.ByteArrayInputStream(archive);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) {
                    throw new BusinessException(50218, "Allure 离线报告文件数量超过限制");
                }
                Path file = target.resolve(entry.getName()).normalize();
                if (!file.startsWith(target)) {
                    throw new BusinessException(50218, "Allure 压缩包包含非法路径");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(file);
                    continue;
                }
                Files.createDirectories(file.getParent());
                try (var output = Files.newOutputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = zip.read(buffer)) >= 0) {
                        extractedBytes += count;
                        if (extractedBytes > MAX_ARCHIVE_BYTES * 5) {
                            throw new BusinessException(50218, "Allure 离线报告解压后超过大小限制");
                        }
                        output.write(buffer, 0, count);
                    }
                }
            }
        }
    }

    private void ensureInsideRoot(Path path) {
        if (!path.startsWith(root)) {
            throw new BusinessException(40058, "报告路径不合法");
        }
    }

    private void deleteRecursively(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
        } catch (IOException exception) {
            throw new BusinessException(50219, "Allure 报告临时文件清理失败");
        }
    }
}
