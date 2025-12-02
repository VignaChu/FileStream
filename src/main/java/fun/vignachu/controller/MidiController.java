package fun.vignachu.controller;

import fun.vignachu.model.midi.MidiData;
import fun.vignachu.model.midi.MidiParser;
import fun.vignachu.service.AudioEngine;
import fun.vignachu.util.I18n;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import java.io.File;

public class MidiController {

    // 本类为实现MIDI可视化主要逻辑
    @FXML private Button btnMidi, btnSf2, btnClearSf2, btnBg, btnClearBg, btnPlay, btnPause, btnStop;
    @FXML private Label lblMidiName, lblSf2Name, lblTime, lblOpacity, lblSync, lblSyncValue, lblVol;
    @FXML private Slider sliderOpacity, sliderSync, sliderProgress, sliderVolume;
    @FXML private Pane canvasContainer;
    @FXML private Canvas canvas;

    private final AudioEngine engine = new AudioEngine();
    private MidiData midiData;
    private Image bgImage;
    private boolean isDrag = false;
    private static final double HIT_X = 150.0;

    @FXML
    public void initialize() {
        bindI18n();
        try { engine.init(); } catch (Exception ignored) {}

        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());

        sliderVolume.valueProperty().addListener((o,old,v)-> engine.setVolume(v.intValue()));
        sliderProgress.setOnMousePressed(e-> isDrag=true);
        sliderProgress.setOnMouseReleased(e-> { engine.setProgress(sliderProgress.getValue()); isDrag=false; });

        if (lblSyncValue != null) lblSyncValue.setText((int) sliderSync.getValue() + " ms");
        sliderSync.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (lblSyncValue != null) lblSyncValue.setText(newVal.intValue() + " ms");
        });

        new AnimationTimer() {
            @Override public void handle(long now) { render(); updateUI(); }
        }.start();
    }

    private void bindI18n() {
        if(btnMidi!=null) btnMidi.textProperty().bind(I18n.createStringBinding("midi.btn.load"));
        if(btnSf2!=null) btnSf2.textProperty().bind(I18n.createStringBinding("midi.btn.sf2"));
        if(btnClearSf2!=null) btnClearSf2.textProperty().bind(I18n.createStringBinding("midi.btn.clear_sf2"));
        if(btnBg!=null) btnBg.textProperty().bind(I18n.createStringBinding("midi.btn.bg"));
        if(btnClearBg!=null) btnClearBg.textProperty().bind(I18n.createStringBinding("midi.btn.clear_bg"));
        if(lblOpacity!=null) lblOpacity.textProperty().bind(I18n.createStringBinding("midi.lbl.opacity"));
        if(lblSync!=null) lblSync.textProperty().bind(I18n.createStringBinding("midi.lbl.sync"));
        if(lblVol!=null) lblVol.textProperty().bind(I18n.createStringBinding("midi.lbl.volume"));
        if(btnPlay!=null) btnPlay.textProperty().bind(I18n.createStringBinding("midi.btn.play"));
        if(btnPause!=null) btnPause.textProperty().bind(I18n.createStringBinding("midi.btn.pause"));
        if(btnStop!=null) btnStop.textProperty().bind(I18n.createStringBinding("midi.btn.stop"));
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();

        if (bgImage != null) {
            gc.drawImage(bgImage, 0, 0, w, h);
            gc.setFill(Color.rgb(0, 0, 0, 0.4));
            gc.fillRect(0, 0, w, h);
        } else {
            // 无背景图时，强制黑色背景
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, w, h);
        }

        if (midiData == null) return;

        double curMs = (engine.getCurrentTimeMicro()/1000.0) - sliderSync.getValue();
        double kh = h/128.0;

        Color mainColor = Color.WHITE;
        gc.setStroke(mainColor);
        gc.setLineWidth(2);
        gc.strokeLine(HIT_X,0,HIT_X,h);

        gc.setGlobalAlpha(sliderOpacity.getValue());

        for(var t : midiData.tracks) {
            Color c = Color.hsb((t.trackNumber*55)%360, 0.8, 1.0);
            gc.setFill(c);
            for(var n : t.notes) {
                double x1 = HIT_X + (n.startTimeMs - curMs)*0.3;
                double x2 = HIT_X + ((n.startTimeMs+n.durationMs) - curMs)*0.3;
                if(x2>0 && x1<w) {
                    if(x1<=HIT_X && x2>=HIT_X) {
                        gc.setGlobalAlpha(1.0);
                        gc.setFill(mainColor);
                        gc.fillRect(0,(127-n.pitch)*kh,HIT_X,kh);
                        gc.setFill(c.brighter());
                    } else {
                        gc.setGlobalAlpha(sliderOpacity.getValue());
                        gc.setFill(c);
                    }
                    gc.fillRect(x1, (127-n.pitch)*kh, x2-x1, kh-0.5);
                }
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    private void updateUI() {
        if(!isDrag && engine.getTotalTimeMicro()>0) sliderProgress.setValue((double)engine.getCurrentTimeMicro()/engine.getTotalTimeMicro()*100);
        long cur=engine.getCurrentTimeMicro()/1000000, tot=engine.getTotalTimeMicro()/1000000;
        lblTime.setText(String.format("%02d:%02d / %02d:%02d", cur/60, cur%60, tot/60, tot%60));
        lblTime.setStyle("-fx-text-fill: white;");
    }

    @FXML void onLoadMidi() {
        File f = new FileChooser().showOpenDialog(canvas.getScene().getWindow());
        if(f!=null) try { midiData = new MidiParser(f).parse(); engine.loadMidi(f); lblMidiName.setText(f.getName()); } catch(Exception e){e.printStackTrace();}
    }
    @FXML void onLoadSf2() {
        File f = new FileChooser().showOpenDialog(canvas.getScene().getWindow());
        if(f!=null) try { if(engine.loadSf2(f)) lblSf2Name.setText(f.getName()); } catch(Exception e){e.printStackTrace();}
    }
    @FXML void onClearSf2() {
        try { engine.resetSoundbank(); lblSf2Name.setText("Default"); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void onLoadBg() {
        File f = new FileChooser().showOpenDialog(canvas.getScene().getWindow());
        if(f!=null) bgImage = new Image(f.toURI().toString());
    }

    @FXML void onClearBg() {
        bgImage = null;
    }

    @FXML void onPlay() { engine.play(); }
    @FXML void onPause() { pauseMidi(); }
    @FXML void onStop() { engine.stop(); }
    public void pauseMidi() { engine.pause(); }
}