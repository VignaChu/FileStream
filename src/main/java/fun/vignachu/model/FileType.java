package fun.vignachu.model;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum FileType {

    // 定义文件类型
    IMAGE("png", "jpg", "jpeg", "bmp", "webp", "gif", "svg"),
    VIDEO("mp4", "m4v", "flv"),
    AUDIO("mp3", "wav", "m4a", "aac", "aif", "aiff"),
    MIDI("mid", "midi"),
    TEXT("txt", "md", "markdown", "json", "xml", "html", "css", "js", "java", "py", "c", "cpp", "h", "sql", "log", "properties", "yml", "yaml", "sh", "bat", "cmd", "go", "rs", "php", "rb", "lua", "kt", "ts","v", "hpp", "r", "ipynb"),
    PDF("pdf"),
    PPT("pptx", "ppt"),
    WORD("docx", "doc"),
    SHEET("xlsx", "xls", "csv"),
    UNKNOWN;

    private final Set<String> extensions;

    FileType(String... exts) {
        this.extensions = new HashSet<>(Arrays.asList(exts));
    }

    public static FileType determine(File file) {
        if (file == null || !file.exists() || file.isDirectory()) return UNKNOWN;
        String name = file.getName().toLowerCase();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) return UNKNOWN;
        String ext = name.substring(lastDot + 1);

        for (FileType type : values()) {
            if (type.extensions.contains(ext)) return type;
        }
        return UNKNOWN;
    }
}