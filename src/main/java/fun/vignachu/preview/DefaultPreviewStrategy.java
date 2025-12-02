package fun.vignachu.preview;

import fun.vignachu.util.I18n;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.File;

public class DefaultPreviewStrategy implements PreviewStrategy {

    // 不支持的文件类型
    private final String key;
    private final Object[] args;

    public DefaultPreviewStrategy(String key, Object... args) {
        this.key = key;
        this.args = args;
    }

    @Override
    public Node createPreviewNode(File file, double width, double height) {
        StackPane pane = new StackPane();

        // 1. 主提示信息 (绑定 I18n)
        Label lbl = new Label();
        lbl.textProperty().bind(I18n.createStringBinding(key, args));
        lbl.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 14px; -fx-wrap-text: true; -fx-text-alignment: center;");

        // 2. 文件名 (如果有)
        Label lblFile = new Label(file != null ? file.getName() : "");
        lblFile.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12px; -fx-opacity: 0.7;");

        VBox box = new VBox(10, lbl, lblFile);
        box.setAlignment(Pos.CENTER);

        pane.getChildren().add(box);
        return pane;
    }
}