package com.testpilot.platform.requirement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("requirements")
public class Requirement {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String name;
    private String fileType;
    private String filePath;
    private String version;
    private String parseStatus;
    private String contentText;
    private String parseError;
    private Long uploadedBy;
    private LocalDateTime createdAt;
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
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
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

