package fun.vignachu.preview;

import javafx.scene.Node;
import java.io.File;

public interface PreviewStrategy {

    // 定义文件浏览的接口
    Node createPreviewNode(File file, double width, double height);
    default void cleanup() {}
}