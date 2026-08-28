package com.testpilot.platform.requirement;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.testcase.TestCaseRepository;
import com.testpilot.platform.user.User;
import java.io.IOException;
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
    private static final List<String> LEGACY_ALLOWED_TYPES = List.of("pdf", "docx", "md", "markdown", "txt");

    private final RequirementRepository requirementRepository;
    private final ProjectAccessService projectAccessService;
    private final RequirementParser requirementParser;
    private final TestCaseRepository testCaseRepository;

    public RequirementService(
            RequirementRepository requirementRepository,
            ProjectAccessService projectAccessService,
            RequirementParser requirementParser,
            TestCaseRepository testCaseRepository
    ) {
        this.requirementRepository = requirementRepository;
        this.projectAccessService = projectAccessService;
        this.requirementParser = requirementParser;
        this.testCaseRepository = testCaseRepository;
    }

    public List<RequirementDtos.Response> list(Long projectId, User currentUser) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.REQUIREMENT_READ);
        return requirementRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(RequirementDtos.Response::from)
                .toList();
    }

    public RequirementDtos.DetailResponse get(Long projectId, Long requirementId, User currentUser) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.REQUIREMENT_READ);
        return RequirementDtos.DetailResponse.from(findRequirement(projectId, requirementId));
    }

    @Transactional
    public RequirementDtos.Response upload(
            Long projectId,
            MultipartFile file,
            String version,
            User currentUser
    ) {
        return upload(projectId, file, version, currentUser, false);
    }

    @Transactional
    public RequirementDtos.Response upload(
            Long projectId,
            MultipartFile file,
            String version,
            User currentUser,
            boolean allowAnyType
    ) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.REQUIREMENT_WRITE);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40003, "请选择项目文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(40002, "文件大小不能超过 10MB");
        }

        String originalName = safeFileName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!allowAnyType && !LEGACY_ALLOWED_TYPES.contains(extension)) {
            throw new BusinessException(40004, "仅支持 PDF、DOCX、Markdown 和 TXT 文件");
        }
        Path uploadDirectory = Path.of("data", "uploads", String.valueOf(projectId));
        Path target = uploadDirectory.resolve(UUID.randomUUID() + "." + extension);
        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException(50002, "项目文件保存失败");
        }

        // 项目文件管理只负责保存原文件。文件内容由使用方（例如 AI 生成）按需读取。
        String contentText = null;
        // Keep the legacy status value for API compatibility; the file itself is not parsed here.
        String parseStatus = "PARSED";

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
        return RequirementDtos.Response.from(requirementRepository.save(requirement));
    }

    @Transactional
    public RequirementDtos.Response parse(Long projectId, Long requirementId, User currentUser) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.REQUIREMENT_WRITE);
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

    @Transactional
    public void delete(Long projectId, Long requirementId, User currentUser) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.REQUIREMENT_WRITE);
        Requirement requirement = findRequirement(projectId, requirementId);
        if (testCaseRepository.countByProjectIdAndRequirementId(projectId, requirementId) > 0) {
            throw new BusinessException(40902, "该文件已关联测试用例，暂不能删除");
        }
        try {
            if (requirement.getFilePath() != null && !requirement.getFilePath().isBlank()) {
                Files.deleteIfExists(Path.of(requirement.getFilePath()));
            }
        } catch (IOException exception) {
            throw new BusinessException(50003, "项目文件删除失败，请稍后重试");
        }
        requirementRepository.delete(requirement);
    }

    private Requirement findRequirement(Long projectId, Long requirementId) {
        return requirementRepository.findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new BusinessException(40402, "项目文件不存在"));
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
