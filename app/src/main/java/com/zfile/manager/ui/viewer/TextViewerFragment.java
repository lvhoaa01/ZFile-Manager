package com.zfile.manager.ui.viewer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.zfile.manager.R;
import com.zfile.manager.core.MimeTypeHelper;
import com.zfile.manager.core.ThreadPoolManager;
import com.zfile.manager.util.SyntaxHighlighter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Text / code viewer. Reads the file on a background thread, caps very large
 * files at {@link #MAX_BYTES} (showing a warning), renders with a monospace font
 * and a line-number gutter, and applies {@link SyntaxHighlighter} for code. The
 * toolbar adds search, copy-all and font +/- via the base fragment's extra-menu hook.
 */
public class TextViewerFragment extends BaseViewerFragment {

    private static final int MAX_BYTES = 1024 * 1024; // 1 MB
    private static final float MIN_SP = 10f;
    private static final float MAX_SP = 28f;

    private TextView lineNumbers;
    private TextView textContent;
    private View warningBar;
    private EditText searchInput;

    private float textSizeSp = 14f;
    @NonNull private CharSequence baseContent = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_text_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lineNumbers = view.findViewById(R.id.lineNumbers);
        textContent = view.findViewById(R.id.textContent);
        warningBar = view.findViewById(R.id.largeFileWarning);
        searchInput = view.findViewById(R.id.searchInput);
        applyFontSize();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { applySearch(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });

        Bundle args = getArguments();
        String path = args != null ? args.getString(ViewerActivity.EXTRA_FILE_PATH) : null;
        if (path != null) {
            File file = new File(path);
            setCurrentFile(file);
            loadAsync(file);
        }
    }

    private void loadAsync(@NonNull File file) {
        ThreadPoolManager.getInstance().execute(() -> {
            boolean truncated = file.length() > MAX_BYTES;
            String content = readCapped(file);
            String ext = MimeTypeHelper.getExtension(file.getName());
            CharSequence highlighted = SyntaxHighlighter.highlight(content, ext);
            CharSequence gutter = buildGutter(content);
            if (!isAdded() || getView() == null) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                baseContent = highlighted;
                lineNumbers.setText(gutter);
                textContent.setText(highlighted);
                warningBar.setVisibility(truncated ? View.VISIBLE : View.GONE);
            });
        });
    }

    @NonNull
    private static String readCapped(@NonNull File file) {
        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
            byte[] tmp = new byte[8192];
            int total = 0, read;
            while (total < MAX_BYTES && (read = in.read(tmp)) > 0) {
                int toWrite = Math.min(read, MAX_BYTES - total);
                bos.write(tmp, 0, toWrite);
                total += toWrite;
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    @NonNull
    private static CharSequence buildGutter(@NonNull String content) {
        int lines = 1;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') lines++;
        }
        StringBuilder sb = new StringBuilder(lines * 4);
        for (int i = 1; i <= lines; i++) {
            sb.append(i);
            if (i < lines) sb.append('\n');
        }
        return sb;
    }

    @Override
    protected void onInflateExtraMenu(@NonNull MaterialToolbar toolbar) {
        toolbar.inflateMenu(R.menu.menu_text_viewer);
    }

    @Override
    protected boolean onExtraMenuItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.text_action_search) {
            toggleSearch();
            return true;
        }
        if (id == R.id.text_action_copy_all) {
            copyAll();
            return true;
        }
        if (id == R.id.text_action_font_increase) {
            textSizeSp = Math.min(MAX_SP, textSizeSp + 2f);
            applyFontSize();
            return true;
        }
        if (id == R.id.text_action_font_decrease) {
            textSizeSp = Math.max(MIN_SP, textSizeSp - 2f);
            applyFontSize();
            return true;
        }
        return false;
    }

    private void applyFontSize() {
        if (lineNumbers != null) lineNumbers.setTextSize(textSizeSp);
        if (textContent != null) textContent.setTextSize(textSizeSp);
    }

    private void toggleSearch() {
        boolean show = searchInput.getVisibility() != View.VISIBLE;
        searchInput.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            searchInput.requestFocus();
        } else {
            searchInput.setText("");
            applySearch("");
        }
    }

    private void applySearch(@NonNull String query) {
        if (textContent == null) return;
        if (query.isEmpty()) {
            textContent.setText(baseContent);
            return;
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder(baseContent);
        String haystack = baseContent.toString().toLowerCase(Locale.ROOT);
        String needle = query.toLowerCase(Locale.ROOT);
        int from = 0, firstMatch = -1;
        int color = ContextCompat.getColor(requireContext(), R.color.category_apk);
        while (true) {
            int idx = haystack.indexOf(needle, from);
            if (idx < 0) break;
            if (firstMatch < 0) firstMatch = idx;
            ssb.setSpan(new BackgroundColorSpan(color), idx, idx + needle.length(),
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            from = idx + needle.length();
        }
        textContent.setText(ssb);
        if (firstMatch >= 0) scrollToOffset(firstMatch);
    }

    private void scrollToOffset(int offset) {
        textContent.post(() -> {
            if (textContent.getLayout() == null || !(getView() instanceof ViewGroup)) return;
            int line = textContent.getLayout().getLineForOffset(offset);
            int y = textContent.getLayout().getLineTop(line);
            View scroller = requireView().findViewById(R.id.textScroll);
            if (scroller instanceof ScrollView) ((ScrollView) scroller).smoothScrollTo(0, y);
        });
    }

    private void copyAll() {
        Context ctx = requireContext();
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("text", baseContent));
            Toast.makeText(ctx, R.string.viewer_copied, Toast.LENGTH_SHORT).show();
        }
    }
}
