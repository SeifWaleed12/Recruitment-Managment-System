package com.banquemisr.recruitment.cvparsing.model;

import com.banquemisr.recruitment.web.DTOs.respond.CandidateRespond;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public class BulkUploadJob {

    public enum JobStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    private final String jobId;
    private volatile JobStatus status;
    private final int totalFiles;
    private final AtomicInteger processedFiles = new AtomicInteger(0);
    private final AtomicInteger successfulFiles = new AtomicInteger(0);
    private final AtomicInteger failedFiles = new AtomicInteger(0);
    private final List<CandidateRespond> results = Collections.synchronizedList(new ArrayList<>());
    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    private final Instant createdAt;
    private volatile Instant completedAt;

    public BulkUploadJob(String jobId, int totalFiles) {
        this.jobId = jobId;
        this.totalFiles = totalFiles;
        this.status = JobStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void addSuccess(CandidateRespond respond) {
        results.add(respond);
        successfulFiles.incrementAndGet();
        processedFiles.incrementAndGet();
    }

    public void addError(String errorMsg) {
        errors.add(errorMsg);
        failedFiles.incrementAndGet();
        processedFiles.incrementAndGet();
    }

    public int getProgressPercentage() {
        if (totalFiles == 0) return 100;
        return (int) Math.round(((double) processedFiles.get() / totalFiles) * 100);
    }
}
