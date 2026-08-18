package com.banquemisr.recruitment.cvparsing.model;

import com.banquemisr.recruitment.web.DTOs.respond.CandidateRespond;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUploadProgressRespond {

    private String jobId;
    private String status;
    private int totalFiles;
    private int processedFiles;
    private int successfulFiles;
    private int failedFiles;
    private int progressPercentage;
    private Instant createdAt;
    private Instant completedAt;
    private List<CandidateRespond> results;
    private List<String> errors;

    public static BulkUploadProgressRespond fromJob(BulkUploadJob job) {
        if (job == null) return null;
        return BulkUploadProgressRespond.builder()
                .jobId(job.getJobId())
                .status(job.getStatus().name())
                .totalFiles(job.getTotalFiles())
                .processedFiles(job.getProcessedFiles().get())
                .successfulFiles(job.getSuccessfulFiles().get())
                .failedFiles(job.getFailedFiles().get())
                .progressPercentage(job.getProgressPercentage())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .results(job.getResults())
                .errors(job.getErrors())
                .build();
    }
}
