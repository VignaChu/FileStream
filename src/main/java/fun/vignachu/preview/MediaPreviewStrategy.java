package fun.vignachu.preview;

import fun.vignachu.util.I18n;
import fun.vignachu.util.ThemeManager;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.io.File;

public class MediaPreviewStrategy implements PreviewStrategy {

    // 音视频显示
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private ChangeListener<Boolean> themeListener;
    private HBox controlBar;

    @Override
    public Node createPreviewNode(File file, double width, double height) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("media-background");

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView = new MediaView(mediaPlayer);

            StackPane centerPane = new StackPane(mediaView);
            centerPane.setStyle("-fx-background-color: black;"); // 视频背景始终黑

            mediaView.fitWidthProperty().bind(centerPane.widthProperty());
            mediaView.fitHeightProperty().bind(centerPane.heightProperty());
            mediaView.setPreserveRatio(true);

            root.setCenter(centerPane);

            controlBar = createControlBar();
            root.setBottom(controlBar);

            themeListener = (obs, old, isDark) -> {
                if (controlBar != null) {
                    controlBar.getStyleClass().remove("midi-panel");
                    controlBar.getStyleClass().add("midi-panel");
                }
            };
            ThemeManager.darkProperty().addListener(themeListener);

            initMediaPlayerEvents(root);

        } catch (Exception e) {
            return createErrorNode(e.getMessage());
        }

        return root;
    }

    private void initMediaPlayerEvents(BorderPane root) {
        mediaPlayer.setOnError(() -> {
            String errorMsg = mediaPlayer.getError() != null ? mediaPlayer.getError().getMessage() : "Unknown Error";
            Platform.runLater(() -> {
                root.setCenter(createErrorNode(errorMsg));
                root.setBottom(null);
            });
        });
        mediaPlayer.setOnReady(() -> mediaPlayer.play());
        mediaPlayer.setOnEndOfMedia(() -> {
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.pause();
        });
    }

    private Node createErrorNode(String msg) {
        Label errorLbl = new Label();
        errorLbl.textProperty().bind(I18n.createStringBinding("preview.media_error", msg));
        errorLbl.setStyle("-fx-text-fill: #FF5555; -fx-alignment: center;");
        return new StackPane(errorLbl);
    }

    private HBox createControlBar() {
        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER);
        hbox.getStyleClass().add("midi-panel");

        Button btnPlay = new Button("▶");
        Button btnPause = new Button("⏸");
        Button btnStop = new Button("⏹");

        btnPlay.getStyleClass().add("btn-primary");
        btnPause.getStyleClass().add("btn-purple");
        btnStop.getStyleClass().add("btn-danger");

        Slider progressSlider = new Slider();
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        Label lblTime = new Label("00:00");
        lblTime.getStyleClass().add("label");

        btnPlay.setOnAction(e -> checkAndRun(() -> mediaPlayer.play()));
        btnPause.setOnAction(e -> checkAndRun(() -> mediaPlayer.pause()));
        btnStop.setOnAction(e -> checkAndRun(() -> { mediaPlayer.stop(); mediaPlayer.seek(Duration.ZERO); }));

        progressSlider.setOnMousePressed(e -> checkAndRun(() -> mediaPlayer.pause()));
        progressSlider.setOnMouseReleased(e -> checkAndRun(() -> {
            Duration total = mediaPlayer.getTotalDuration();
            if (total != null && total.toMillis() > 0) mediaPlayer.seek(total.multiply(progressSlider.getValue()/100.0));
            mediaPlayer.play();
        }));

        mediaPlayer.currentTimeProperty().addListener((obs, oldV, newV) -> {
            if (mediaPlayer == null) return;
            if (!progressSlider.isPressed()) {
                Duration total = mediaPlayer.getTotalDuration();
                if (total != null && !total.isUnknown() && total.toMillis() > 0) {
                    progressSlider.setValue(newV.toMillis() / total.toMillis() * 100);
                }
                lblTime.setText(formatTime(newV));
            }
        });

        hbox.getChildren().addAll(btnPlay, btnPause, btnStop, progressSlider, lblTime);
        return hbox;
    }

    private void checkAndRun(Runnable r) { if (mediaPlayer != null) r.run(); }

    private String formatTime(Duration d) {
        if (d == null || d.lessThan(Duration.ZERO)) return "00:00";
        int seconds = (int) d.toSeconds();
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public void cleanup() {
        if (themeListener != null) ThemeManager.darkProperty().removeListener(themeListener);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            if (mediaView != null) mediaView.setMediaPlayer(null);
            mediaPlayer = null;
        }
    }
}