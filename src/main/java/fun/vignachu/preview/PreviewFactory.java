package fun.vignachu.preview;

import fun.vignachu.model.FileType;

public class PreviewFactory {

    // 根据不同后缀的文件返回不同类型的浏览(都具有统一接口)
    public static PreviewStrategy getStrategy(FileType type) {
        switch (type) {
            case IMAGE:
                return new ImagePreviewStrategy();

            case VIDEO:
            case AUDIO:
                return new MediaPreviewStrategy();

            case MIDI:
                return new MidiPreviewStrategy();

            case TEXT:
                return new TextPreviewStrategy();

            case PDF:
                return new PdfPreviewStrategy();

            case WORD:
                return new WordPreviewStrategy();

            case SHEET:
                return new TablePreviewStrategy();

            case PPT:
                return new PptPreviewStrategy();

            default:
                return new DefaultPreviewStrategy("preview.not_supported");
        }
    }
}