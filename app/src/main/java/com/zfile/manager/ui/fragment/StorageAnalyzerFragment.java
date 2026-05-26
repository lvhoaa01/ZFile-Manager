package com.zfile.manager.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.zfile.manager.R;
import com.zfile.manager.core.FileSystemManager;
import com.zfile.manager.model.CategoryType;
import com.zfile.manager.ui.adapter.TopFilesAdapter;
import com.zfile.manager.ui.view.StoragePieChart;
import com.zfile.manager.util.FileUtils;
import com.zfile.manager.util.IntentHelper;
import com.zfile.manager.viewmodel.StorageAnalyzerViewModel;

/**
 * Storage Analyzer screen. Pie chart of per-category bytes plus two top-N lists
 * (largest individual files; largest root-level folders). Tapping a pie slice
 * navigates to the corresponding category detail; tapping a top-file row opens
 * the file via {@link IntentHelper}.
 */
public class StorageAnalyzerFragment extends Fragment {

    private StorageAnalyzerViewModel viewModel;
    private StoragePieChart pieChart;
    private TextView header;
    private TextView stageLabel;
    private ProgressBar progressBar;
    private TopFilesAdapter topFilesAdapter;
    private TopFilesAdapter topFoldersAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_storage_analyzer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(StorageAnalyzerViewModel.class);

        pieChart = view.findViewById(R.id.storagePieChart);
        header = view.findViewById(R.id.analyzerHeader);
        stageLabel = view.findViewById(R.id.analyzerStage);
        progressBar = view.findViewById(R.id.analyzerProgress);

        RecyclerView topFilesRecycler = view.findViewById(R.id.topFilesRecycler);
        RecyclerView topFoldersRecycler = view.findViewById(R.id.topFoldersRecycler);
        topFilesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        topFoldersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        topFilesAdapter = new TopFilesAdapter();
        topFoldersAdapter = new TopFilesAdapter();
        topFilesRecycler.setAdapter(topFilesAdapter);
        topFoldersRecycler.setAdapter(topFoldersAdapter);

        topFilesAdapter.setOnClickListener(fi -> IntentHelper.openWith(requireContext(), fi.asFile()));
        topFoldersAdapter.setOnClickListener(fi -> {
            // Folders surface as click targets too — but we currently don't deep-link to
            // the browser. Show a snackbar with the path so users can navigate manually.
            if (getView() != null) {
                Snackbar.make(getView(), fi.getPath(), Snackbar.LENGTH_LONG).show();
            }
        });

        pieChart.setOnSliceClickListener(this::navigateToCategoryDetail);

        viewModel.getAnalysis().observe(getViewLifecycleOwner(), analysis -> {
            if (analysis == null) return;
            pieChart.setData(analysis.getCategoryBytes(),
                    analysis.getTotalUsed(),
                    analysis.getTotalCapacity());
            header.setText(getString(R.string.analyzer_used_format,
                    FileUtils.formatSize(analysis.getTotalUsed()),
                    FileUtils.formatSize(analysis.getTotalCapacity())));
            topFilesAdapter.submitList(analysis.getTopFiles());
            topFoldersAdapter.submitList(analysis.getTopFolders());
        });
        viewModel.getIsAnalyzing().observe(getViewLifecycleOwner(), analyzing -> {
            boolean a = Boolean.TRUE.equals(analyzing);
            progressBar.setVisibility(a ? View.VISIBLE : View.GONE);
            stageLabel.setVisibility(a ? View.VISIBLE : View.GONE);
        });
        viewModel.getStageLabel().observe(getViewLifecycleOwner(), label -> {
            if (label != null) stageLabel.setText(label);
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty() && getView() != null) {
                Snackbar.make(getView(), msg, Snackbar.LENGTH_SHORT).show();
                viewModel.clearErrorMessage();
            }
        });

        // Kick off analysis if not done yet.
        if (viewModel.getAnalysis().getValue() == null
                && !Boolean.TRUE.equals(viewModel.getIsAnalyzing().getValue())) {
            String root = FileSystemManager.getInstance().getCurrentRootPath();
            if (root != null) viewModel.startAnalysis(root);
        }
    }

    @Override
    public void onDestroyView() {
        viewModel.cancel();
        super.onDestroyView();
    }

    private void navigateToCategoryDetail(@NonNull CategoryType type) {
        NavController nav = NavHostFragment.findNavController(this);
        Bundle args = new Bundle();
        args.putString(CategoryDetailFragment.ARG_CATEGORY, type.name());
        // Direct navigate by destination id — no action defined, but Navigation
        // resolves the destination from the graph directly.
        nav.navigate(R.id.categoryDetailFragment, args);
    }
}
