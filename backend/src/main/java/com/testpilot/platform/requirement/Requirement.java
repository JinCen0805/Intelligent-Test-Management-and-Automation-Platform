package com.testpilot.platform.requirement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "requirements")
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "file_type", nullable = false, length = 16)
    private String fileType;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(length = 32)
    private String version;

    @Column(name = "parse_status", nullable = false, length = 16)
    private String parseStatus;

    @Column(name = "content_text", columnDefinition = "text")
    private String contentText;

    @Column(name = "parse_error", columnDefinition = "text")
    private String parseError;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Requirement() {
    }

    public Requirement(
            Long projectId,
            String name,
            String fileType,
            String filePath,
            String version,
            String parseStatus,
            String contentText,
            Long uploadedBy
    ) {
        this.projectId = projectId;
        this.name = name;
        this.fileType = fileType;
        this.filePath = filePath;
        this.version = version;
        this.parseStatus = parseStatus;
        this.contentText = contentText;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getFileType() { return fileType; }
    public String getFilePath() { return filePath; }
    public String getVersion() { return version; }
    public String getParseStatus() { return parseStatus; }
    public String getContentText() { return contentText; }
    public String getParseError() { return parseError; }
    public Long getUploadedBy() { return uploadedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setParseStatus(String parseStatus) { this.parseStatus = parseStatus; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public void setParseError(String parseError) { this.parseError = parseError; }
}
