package com.zfile.manager.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Progress snapshot emitted by {@code FileTransferService} via LiveData.
 * Use the static factories rather than the full constructor — they document
 * the valid combinations of fields for each lifecycle state.
 */
public final class TransferProgress {

    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public enum Operation {
        COPY,
        MOVE,
        DELETE,
        ZIP,
        UNZIP
    }

    @NonNull private final Status status;
    @NonNull private final Operation operation;
    private final long totalBytes;
    private final long transferredBytes;
    private final int totalFiles;
    private final int processedFiles;
    @Nullable private final String currentFile;
    @Nullable private final String errorMessage;

    private TransferProgress(@NonNull Status status,
                             @NonNull Operation operation,
                             long totalBytes,
                             long transferredBytes,
                             int totalFiles,
                             int processedFiles,
                             @Nullable String currentFile,
                             @Nullable String errorMessage) {
        this.status = status;
        this.operation = operation;
        this.totalBytes = totalBytes;
        this.transferredBytes = transferredBytes;
        this.totalFiles = totalFiles;
        this.processedFiles = processedFiles;
        this.currentFile = currentFile;
        this.errorMessage = errorMessage;
    }

    @NonNull public Status getStatus() { return status; }
    @NonNull public Operation getOperation() { return operation; }
    public long getTotalBytes() { return totalBytes; }
    public long getTransferredBytes() { return transferredBytes; }
    public int getTotalFiles() { return totalFiles; }
    public int getProcessedFiles() { return processedFiles; }
    @Nullable public String getCurrentFile() { return currentFile; }
    @Nullable public String getErrorMessage() { return errorMessage; }

    public int getPercentage() {
        if (totalBytes <= 0) return status == Status.COMPLETED ? 100 : 0;
        long pct = (transferredBytes * 100L) / totalBytes;
        return (int) Math.min(100L, Math.max(0L, pct));
    }

    public boolean isTerminal() {
        return status == Status.COMPLETED || status == Status.FAILED || status == Status.CANCELLED;
    }

    @NonNull
    public static TransferProgress pending(@NonNull Operation op, long totalBytes, int totalFiles) {
        return new TransferProgress(Status.PENDING, op, totalBytes, 0L, totalFiles, 0, null, null);
    }

    @NonNull
    public static TransferProgress inProgress(@NonNull Operation op,
                                              long totalBytes,
                                              long transferredBytes,
                                              int totalFiles,
                                              int processedFiles,
                                              @Nullable String currentFile) {
        return new TransferProgress(Status.IN_PROGRESS, op, totalBytes, transferredBytes,
                totalFiles, processedFiles, currentFile, null);
    }

    @NonNull
    public static TransferProgress completed(@NonNull Operation op, long totalBytes, int totalFiles) {
        return new TransferProgress(Status.COMPLETED, op, totalBytes, totalBytes,
                totalFiles, totalFiles, null, null);
    }

    @NonNull
    public static TransferProgress failed(@NonNull Operation op,
                                          long totalBytes,
                                          long transferredBytes,
                                          @NonNull String error) {
        return new TransferProgress(Status.FAILED, op, totalBytes, transferredBytes,
                0, 0, null, error);
    }

    @NonNull
    public static TransferProgress cancelled(@NonNull Operation op) {
        return new TransferProgress(Status.CANCELLED, op, 0L, 0L, 0, 0, null, null);
    }
}
