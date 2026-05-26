package com.zfile.manager.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zfile.manager.model.StorageAnalysis;
import com.zfile.manager.service.StorageAnalyzerService;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thin facade over {@link StorageAnalyzerService}. Exposed as a singleton so
 * the ViewModel layer doesn't construct services directly — matches the rest
 * of the repository / service / viewmodel layout in this project.
 */
public final class StorageAnalyzerRepository {

    private static volatile StorageAnalyzerRepository instance;

    @NonNull private final StorageAnalyzerService service = new StorageAnalyzerService();

    private StorageAnalyzerRepository() { }

    @NonNull
    public static StorageAnalyzerRepository getInstance() {
        StorageAnalyzerRepository local = instance;
        if (local == null) {
            synchronized (StorageAnalyzerRepository.class) {
                local = instance;
                if (local == null) {
                    local = new StorageAnalyzerRepository();
                    instance = local;
                }
            }
        }
        return local;
    }

    @NonNull
    public StorageAnalysis analyze(@NonNull String volumePath,
                                   @Nullable StorageAnalyzerService.ProgressCallback callback,
                                   @NonNull AtomicBoolean cancelled) {
        return service.analyze(volumePath, callback, cancelled);
    }
}
