package com.zfile.manager.ui.viewer;

import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.zfile.manager.R;
import com.zfile.manager.core.ThreadPoolManager;

import java.io.File;
import java.util.Locale;

/**
 * Mini audio player: {@link MediaPlayer} with play/pause, a seekbar, current /
 * total time, and basic metadata (title / artist via {@link MediaMetadataRetriever}).
 * The player is released in {@code onDestroyView} and paused when backgrounded.
 */
public class AudioPlayerFragment extends BaseViewerFragment {

    @Nullable private MediaPlayer player;
    private boolean prepared = false;

    private SeekBar seekBar;
    private TextView currentTime;
    private TextView durationTime;
    private TextView title;
    private TextView subtitle;
    private FloatingActionButton playPause;

    @NonNull private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (player != null && prepared && player.isPlaying()) {
                seekBar.setProgress(player.getCurrentPosition());
                currentTime.setText(formatTime(player.getCurrentPosition()));
                handler.postDelayed(this, 500);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_audio_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        seekBar = view.findViewById(R.id.audioSeek);
        currentTime = view.findViewById(R.id.audioCurrent);
        durationTime = view.findViewById(R.id.audioDuration);
        title = view.findViewById(R.id.audioTitle);
        subtitle = view.findViewById(R.id.audioSubtitle);
        playPause = view.findViewById(R.id.audioPlayPause);

        Bundle args = getArguments();
        String path = args != null ? args.getString(ViewerActivity.EXTRA_FILE_PATH) : null;
        if (path == null) return;
        File file = new File(path);
        setCurrentFile(file);
        title.setText(file.getName());
        subtitle.setText(path);
        loadMetadata(file);

        playPause.setEnabled(false);
        setupPlayer(path);

        playPause.setOnClickListener(v -> togglePlay());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && player != null && prepared) {
                    player.seekTo(progress);
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
    }

    private void setupPlayer(@NonNull String path) {
        player = new MediaPlayer();
        try {
            player.setDataSource(path);
            player.setOnPreparedListener(mp -> {
                prepared = true;
                seekBar.setMax(mp.getDuration());
                durationTime.setText(formatTime(mp.getDuration()));
                playPause.setEnabled(true);
            });
            player.setOnCompletionListener(mp -> {
                handler.removeCallbacks(ticker);
                seekBar.setProgress(0);
                currentTime.setText(formatTime(0));
                playPause.setImageResource(R.drawable.ic_play_arrow);
            });
            player.prepareAsync();
        } catch (Exception e) {
            player = null;
        }
    }

    private void togglePlay() {
        if (player == null || !prepared) return;
        if (player.isPlaying()) {
            player.pause();
            playPause.setImageResource(R.drawable.ic_play_arrow);
            handler.removeCallbacks(ticker);
        } else {
            player.start();
            playPause.setImageResource(R.drawable.ic_pause);
            handler.post(ticker);
        }
    }

    private void loadMetadata(@NonNull File file) {
        ThreadPoolManager.getInstance().execute(() -> {
            String t = null, artist = null;
            MediaMetadataRetriever r = new MediaMetadataRetriever();
            try {
                r.setDataSource(file.getAbsolutePath());
                t = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            } catch (Exception ignored) {
            } finally {
                try { r.release(); } catch (Exception ignored) { }
            }
            final String ft = t, fa = artist;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                if (ft != null && !ft.isEmpty()) title.setText(ft);
                if (fa != null && !fa.isEmpty()) subtitle.setText(fa + " • " + file.getName());
            });
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) {
            player.pause();
            playPause.setImageResource(R.drawable.ic_play_arrow);
            handler.removeCallbacks(ticker);
        }
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(ticker);
        if (player != null) {
            player.release();
            player = null;
        }
        prepared = false;
        super.onDestroyView();
    }

    @NonNull
    private static String formatTime(int ms) {
        int totalSec = ms / 1000;
        return String.format(Locale.getDefault(), "%d:%02d", totalSec / 60, totalSec % 60);
    }
}
