package com.zfile.manager.util;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight syntax highlighter (not an IDE-grade lexer). Detects a language
 * family from the file extension and colours comments, strings, numbers and
 * keywords using a single alternation regex — left-to-right matching means
 * spans never overlap, so comments/strings naturally win over keywords inside them.
 *
 * <p>Colours are mid-tone so they read on both light and dark surfaces.</p>
 */
public final class SyntaxHighlighter {

    private SyntaxHighlighter() { }

    private static final int COLOR_COMMENT = Color.parseColor("#9E9E9E");
    private static final int COLOR_STRING  = Color.parseColor("#81C784");
    private static final int COLOR_NUMBER  = Color.parseColor("#FFB74D");
    private static final int COLOR_KEYWORD = Color.parseColor("#4FC3F7");

    private static final String JAVA_KW = "abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|"
            + "default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|"
            + "interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|"
            + "synchronized|this|throw|throws|transient|try|void|volatile|while|true|false|null|var|record|sealed|yield";

    private static final String KOTLIN_KW = "abstract|actual|annotation|as|break|by|catch|class|companion|const|"
            + "constructor|continue|crossinline|data|do|dynamic|else|enum|expect|external|final|finally|for|fun|get|if|"
            + "import|in|infix|init|inline|inner|interface|internal|is|lateinit|noinline|object|open|operator|out|"
            + "override|package|private|protected|public|reified|return|sealed|set|super|suspend|tailrec|this|throw|try|"
            + "typealias|val|var|vararg|when|where|while|true|false|null";

    private static final String JS_KW = "async|await|break|case|catch|class|const|continue|debugger|default|delete|do|"
            + "else|export|extends|finally|for|function|if|import|in|instanceof|let|new|of|return|super|switch|this|"
            + "throw|try|typeof|var|void|while|with|yield|true|false|null|undefined";

    private static final String PY_KW = "and|as|assert|async|await|break|class|continue|def|del|elif|else|except|False|"
            + "finally|for|from|global|if|import|in|is|lambda|None|nonlocal|not|or|pass|raise|return|True|try|while|with|yield";

    private static final String CSS_KW = ""; // CSS handled via property/selector colouring below
    private static final String JSON_KW = "true|false|null";

    private enum Lang { JAVA, KOTLIN, JS, PY, JSON, MARKUP, CSS, NONE }

    @NonNull
    public static CharSequence highlight(@NonNull String code, @Nullable String extension) {
        Lang lang = langFor(extension);
        SpannableString out = new SpannableString(code);
        if (lang == Lang.NONE) return out;

        Pattern pattern = patternFor(lang);
        if (pattern == null) return out;

        Matcher m = pattern.matcher(code);
        while (m.find()) {
            int color;
            boolean bold = false;
            if (group(m, 1) != null) {            // comment
                color = COLOR_COMMENT;
            } else if (group(m, 2) != null) {     // string
                color = COLOR_STRING;
            } else if (group(m, 3) != null) {     // number
                color = COLOR_NUMBER;
            } else if (group(m, 4) != null) {     // keyword / tag / property
                color = COLOR_KEYWORD;
                bold = true;
            } else {
                continue;
            }
            out.setSpan(new ForegroundColorSpan(color), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (bold) {
                out.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), m.start(), m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return out;
    }

    @Nullable
    private static String group(@NonNull Matcher m, int idx) {
        return idx <= m.groupCount() ? m.group(idx) : null;
    }

    @NonNull
    private static Lang langFor(@Nullable String ext) {
        if (ext == null) return Lang.NONE;
        switch (ext.toLowerCase(Locale.ROOT)) {
            case "java": return Lang.JAVA;
            case "kt": case "kts": return Lang.KOTLIN;
            case "js": case "mjs": return Lang.JS;
            case "py": return Lang.PY;
            case "json": return Lang.JSON;
            case "html": case "htm": case "xml": return Lang.MARKUP;
            case "css": return Lang.CSS;
            default: return Lang.NONE;
        }
    }

    // Groups (consistent across languages): 1=comment, 2=string, 3=number, 4=keyword/tag/property.
    private static final String STRING = "(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')";
    private static final String NUMBER = "(\\b\\d+\\.?\\d*\\b)";
    private static final String C_COMMENT = "(//[^\\n]*|/\\*[\\s\\S]*?\\*/)";

    @Nullable
    private static Pattern patternFor(@NonNull Lang lang) {
        switch (lang) {
            case JAVA:
                return Pattern.compile(C_COMMENT + "|" + STRING + "|" + NUMBER + "|(\\b(?:" + JAVA_KW + ")\\b)");
            case KOTLIN:
                return Pattern.compile(C_COMMENT + "|" + STRING + "|" + NUMBER + "|(\\b(?:" + KOTLIN_KW + ")\\b)");
            case JS:
                return Pattern.compile(C_COMMENT + "|" + STRING + "|" + NUMBER + "|(\\b(?:" + JS_KW + ")\\b)");
            case PY:
                return Pattern.compile("(#[^\\n]*)|" + STRING + "|" + NUMBER + "|(\\b(?:" + PY_KW + ")\\b)");
            case JSON:
                // No comments in JSON → group 1 is a never-matching placeholder to keep numbering.
                return Pattern.compile("((?!))|" + STRING + "|" + NUMBER + "|(\\b(?:" + JSON_KW + ")\\b)");
            case CSS:
                // 1=comment(/*..*/), 2=string, 3=number, 4=property/selector word
                return Pattern.compile("(/\\*[\\s\\S]*?\\*/)|" + STRING + "|" + NUMBER + "|([A-Za-z-]+(?=\\s*:))");
            case MARKUP:
                // 1=comment(<!--..-->), 2=string(attr value), 3=never-match placeholder, 4=tag name
                return Pattern.compile("(<!--[\\s\\S]*?-->)|" + STRING + "|((?!))|(</?[A-Za-z][\\w:-]*)");
            case NONE:
            default:
                return null;
        }
    }
}
