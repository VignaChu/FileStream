package fun.vignachu.preview;

import fun.vignachu.service.AudioEngine;
import fun.vignachu.util.I18n;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.text.MessageFormat;
import java.util.Timer;
import java.util.TimerTask;

public class MidiPreviewStrategy implements PreviewStrategy {

    // MIDI文件显示
    private final AudioEngine engine = new AudioEngine();
    private Timer progressTimer;

    @Override
    public Node createPreviewNode(File file, double width, double height) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("media-background"); // 黑底

        try {
            engine.init();
            engine.loadMidi(file);

            // 中间显示文字提示 (绑定 I18n)
            Label centerLabel = new Label();
            centerLabel.textProperty().bind(Bindings.createStringBinding(
                    () -> MessageFormat.format(I18n.getBundle().getString("preview.midi_hint"), file.getName()),
                    I18n.createStringBinding("preview.midi_hint")
            ));
            centerLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 16px; -fx-text-alignment: center;");

            StackPane centerPane = new StackPane(centerLabel);
            root.setCenter(centerPane);

            HBox controls = createControlBar();
            root.setBottom(controls);

            // 自动播放
            engine.play();
            startProgressTimer(controls);

        } catch (Exception e) {
            Label errLabel = new Label();
            errLabel.textProperty().bind(I18n.createStringBinding("preview.midi_error", e.getMessage()));
            errLabel.setStyle("-fx-text-fill: #FF5555;");
            return new StackPane(errLabel);
        }

        return root;
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

        btnPlay.setOnAction(e -> engine.play());
        btnPause.setOnAction(e -> engine.pause());
        btnStop.setOnAction(e -> { engine.stop(); progressSlider.setValue(0); });

        progressSlider.setOnMousePressed(e -> engine.pause());
        progressSlider.setOnMouseReleased(e -> {
            engine.setProgress(progressSlider.getValue());
            engine.play();
        });

        hbox.setUserData(progressSlider);
        hbox.getChildren().addAll(btnPlay, btnPause, btnStop, progressSlider);
        return hbox;
    }

    private void startProgressTimer(HBox controls) {
        progressTimer = new Timer(true);
        Slider slider = (Slider) controls.getUserData();

        progressTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (engine.getTotalTimeMicro() > 0) {
                    double progress = (double) engine.getCurrentTimeMicro() / engine.getTotalTimeMicro() * 100.0;
                    Platform.runLater(() -> {
                        if (!slider.isPressed()) {
                            slider.setValue(progress);
                        }
                    });
                }
            }
        }, 0, 100);
    }

    @Override
    public void cleanup() {
        if (progressTimer != null) {
            progressTimer.cancel();
            progressTimer = null;
        }
        engine.stop();
        engine.close();
    }
}