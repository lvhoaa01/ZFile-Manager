package com.zfile.manager.ui.viewer;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zfile.manager.R;

import java.io.File;

/**
 * Plays a video with the framework {@link VideoView} and a {@link MediaController}
 * (play/pause/seek). Resources are released on {@code onDestroyView}; playback is
 * paused (with position remembered) when the fragment is backgrounded.
 */
public class VideoViewerFragment extends BaseViewerFragment {

    @Nullable private VideoView videoView;
    @Nullable private MediaController controller;
    private int resumePosition = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        videoView = view.findViewById(R.id.videoView);

        Bundle args = getArguments();
        String path = args != null ? args.getString(ViewerActivity.EXTRA_FILE_PATH) : null;
        if (path == null) return;
        setCurrentFile(new File(path));

        controller = new MediaController(requireContext());
        controller.setAnchorView(view.findViewById(R.id.videoContainer));
        videoView.setMediaController(controller);
        videoView.setVideoURI(Uri.fromFile(new File(path)));
        videoView.setOnPreparedListener(mp -> {
            if (resumePosition > 0) videoView.seekTo(resumePosition);
            videoView.start();
            controller.show();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            resumePosition = videoView.getCurrentPosition();
            videoView.pause();
        }
    }

    @Override
    public void onDestroyView() {
        if (controller != null) {
            controller.hide();
            controller = null;
        }
        if (videoView != null) {
            videoView.stopPlayback();
            videoView = null;
        }
        super.onDestroyView();
    }
}
