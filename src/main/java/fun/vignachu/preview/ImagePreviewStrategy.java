package fun.vignachu.preview;

import fun.vignachu.util.ThemeManager;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import java.io.File;

public class ImagePreviewStrategy implements PreviewStrategy {

    // 图片显示
    private double lastX, lastY;
    private ChangeListener<Boolean> themeListener;
    private StackPane root;

    @Override
    public Node createPreviewNode(File file, double width, double height) {
        root = new StackPane();
        updateBackground(); // 初始背景

        // 裁剪
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        ProgressIndicator pi = new ProgressIndicator();
        root.getChildren().add(pi);

        Task<Image> task = new Task<>() {
            @Override protected Image call() {
                return new Image(file.toURI().toString(), 1200, 1200, true, true, true);
            }
        };

        task.setOnSucceeded(e -> {
            Image img = task.getValue();
            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            // 自适应容器
            iv.fitWidthProperty().bind(root.widthProperty());
            iv.fitHeightProperty().bind(root.heightProperty());

            resetTransform(iv);
            setupGestures(iv, root);

            root.getChildren().setAll(iv);
        });

        themeListener = (obs, oldVal, newVal) -> updateBackground();
        ThemeManager.darkProperty().addListener(themeListener);

        new Thread(task).start();
        return root;
    }

    private void updateBackground() {
        if (root != null) {
            root.setStyle(ThemeManager.isDark() ? "-fx-background-color: #2b2b2b;" : "-fx-background-color: #ffffff;");
        }
    }

    private void setupGestures(ImageView iv, StackPane container) {
        container.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) return;
            double scaleFactor = (event.getDeltaY() > 0) ? 1.1 : 0.9;
            double newScaleX = iv.getScaleX() * scaleFactor;
            double newScaleY = iv.getScaleY() * scaleFactor;
            if (newScaleX >= 0.1 && newScaleX <= 20) {
                iv.setScaleX(newScaleX);
                iv.setScaleY(newScaleY);
            }
            event.consume();
        });

        iv.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                lastX = event.getSceneX();
                lastY = event.getSceneY();
                iv.setCursor(javafx.scene.Cursor.MOVE);
                event.consume();
            }
        });

        iv.setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                double deltaX = event.getSceneX() - lastX;
                double deltaY = event.getSceneY() - lastY;
                iv.setTranslateX(iv.getTranslateX() + deltaX);
                iv.setTranslateY(iv.getTranslateY() + deltaY);
                lastX = event.getSceneX();
                lastY = event.getSceneY();
                event.consume();
            }
        });

        iv.setOnMouseReleased(event -> {
            iv.setCursor(javafx.scene.Cursor.DEFAULT);
            event.consume();
        });

        container.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                resetTransform(iv);
                event.consume();
            }
        });
    }

    private void resetTransform(ImageView iv) {
        iv.setScaleX(1.0);
        iv.setScaleY(1.0);
        iv.setTranslateX(0);
        iv.setTranslateY(0);
    }

    @Override
    public void cleanup() {
        if (themeListener != null) {
            ThemeManager.darkProperty().removeListener(themeListener);
        }
        // ImageView 释放 Image
        if (root != null && !root.getChildren().isEmpty()) {
            Node node = root.getChildren().get(0);
            if (node instanceof ImageView) {
                ((ImageView) node).setImage(null);
            }
        }
    }
}