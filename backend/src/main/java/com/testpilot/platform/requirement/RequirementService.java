package com.testpilot.platform.requirement;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectRepository;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.user.User;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RequirementService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> ALLOWED_TYPES = List.of("pdf", "docx", "md", "markdown", "txt");

    private final RequirementRepository requirementRepository;
    private final ProjectAccessService projectAccessService;
    private final RequirementParser requirementParser;

    public RequirementService(
            RequirementRepository requirementRepository,
            ProjectAccessService projectAccessService,
            RequirementParser requirementParser
    ) {
        this.requirementRepository = requirementRepository;
        this.projectAccessService = projectAccessService;
        this.requirementParser = requirementParser;
    }

    public List<RequirementDtos.Response> list(Long projectId, User currentUser) {
        projectAccessService.requireRead(projectId, currentUser);
        return requirementRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(RequirementDtos.Response::from)
                .toList();
    }

    public RequirementDtos.DetailResponse get(Long projectId, Long requirementId, User currentUser) {
        projectAccessService.requireRead(projectId, currentUser);
        return RequirementDtos.DetailResponse.from(findRequirement(projectId, requirementId));
    }

    @Transactional
    public RequirementDtos.Response upload(
            Long projectId,
            MultipartFile file,
            String version,
            User currentUser
    ) {
        projectAccessService.requireRead(projectId, currentUser);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40003, "请选择需求文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(40002, "文件大小不能超过 10MB");
        }

        String originalName = safeFileName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_TYPES.contains(extension)) {
            throw new BusinessException(40004, "仅支持 PDF、DOCX、Markdown 和 TXT 文件");
        }

        Path uploadDirectory = Path.of("data", "uploads", String.valueOf(projectId));
        Path target = uploadDirectory.resolve(UUID.randomUUID() + "." + extension);
        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException(50002, "需求文件保存失败");
        }

        String contentText = null;
        String parseStatus = "PARSING";
        String parseError = null;
        try {
            contentText = requirementParser.parse(extension, Files.newInputStream(target));
            parseStatus = "PARSED";
        } catch (IOException exception) {
            parseStatus = "FAILED";
            parseError = exception.getMessage();
        }

        Requirement requirement = new Requirement(
                projectId,
                originalName,
                extension.toUpperCase(Locale.ROOT),
                target.toString(),
                version == null || version.isBlank() ? "V1.0" : version.trim(),
                parseStatus,
                contentText,
                currentUser.getId()
        );
        requirement.setParseError(parseError);
        return RequirementDtos.Response.from(requirementRepository.save(requirement));
    }

    @Transactional
    public RequirementDtos.Response parse(Long projectId, Long requirementId, User currentUser) {
        projectAccessService.requireRead(projectId, currentUser);
        Requirement requirement = findRequirement(projectId, requirementId);
        try {
            try (var inputStream = Files.newInputStream(Path.of(requirement.getFilePath()))) {
                requirement.setContentText(requirementParser.parse(
                        requirement.getFileType(),
                        inputStream
                ));
            }
            requirement.setParseError(null);
            requirement.setParseStatus("PARSED");
        } catch (IOException exception) {
            requirement.setParseStatus("FAILED");
            requirement.setParseError(exception.getMessage());
        }
        return RequirementDtos.Response.from(requirementRepository.save(requirement));
    }

    private Requirement findRequirement(Long projectId, Long requirementId) {
        return requirementRepository.findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new BusinessException(40402, "需求文档不存在"));
    }

    private String safeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(40004, "文件名不能为空");
        }
        return Path.of(originalName).getFileName().toString();
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new BusinessException(40004, "无法识别文件类型");
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
