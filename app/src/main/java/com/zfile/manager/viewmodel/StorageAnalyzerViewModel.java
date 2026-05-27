package com.zfile.manager.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zfile.manager.core.ThreadPoolManager;
import com.zfile.manager.model.CategoryType;
import com.zfile.manager.model.StorageAnalysis;
import com.zfile.manager.repository.StorageAnalyzerRepository;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fragment-scoped ViewModel that drives {@code StorageAnalyzerFragment}.
 * Exposes the latest {@link StorageAnalysis}, a loading flag, an analysis
 * stage label (currently which category is being scanned), and a cancel hook.
 */
public class StorageAnalyzerViewModel extends ViewModel {

    @NonNull private final StorageAnalyzerRepository repository = StorageAnalyzerRepository.getInstance();

    private final MutableLiveData<StorageAnalysis> _analysis = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isAnalyzing = new MutableLiveData<>(false);
    /** Null while walking the filesystem; a {@link CategoryType} while summing that category. */
    private final MutableLiveData<CategoryType> _stage = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    @NonNull private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

    @NonNull public LiveData<StorageAnalysis> getAnalysis() { return _analysis; }
    @NonNull public LiveData<Boolean> getIsAnalyzing() { return _isAnalyzing; }
    @NonNull public LiveData<CategoryType> getStage() { return _stage; }
    @NonNull public LiveData<String> getErrorMessage() { return _errorMessage; }

    public void startAnalysis(@NonNull String volumePath) {
        if (Boolean.TRUE.equals(_isAnalyzing.getValue())) return;
        cancelFlag.set(false);
        _isAnalyzing.postValue(true);
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                StorageAnalysis result = repository.analyze(
                        volumePath,
                        (@Nullable CategoryType category) -> _stage.postValue(category),
                        cancelFlag);
                _analysis.postValue(result);
            } catch (Exception e) {
                _errorMessage.postValue(safeMessage(e));
            } finally {
                _isAnalyzing.postValue(false);
            }
        });
    }

    public void cancel() {
        cancelFlag.set(true);
    }

    public void clearErrorMessage() {
        _errorMessage.setValue(null);
    }

    @NonNull
    private static String safeMessage(@NonNull Throwable t) {
        String m = t.getMessage();
        return m != null ? m : t.getClass().getSimpleName();
    }
}
