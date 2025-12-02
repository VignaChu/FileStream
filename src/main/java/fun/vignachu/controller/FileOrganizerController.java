package fun.vignachu.controller;

import fun.vignachu.model.ActionRecord;
import fun.vignachu.model.ActionType;
import fun.vignachu.model.FileOrganizerModel;
import fun.vignachu.model.FileType;
import fun.vignachu.preview.DefaultPreviewStrategy;
import fun.vignachu.preview.PreviewFactory;
import fun.vignachu.preview.PreviewStrategy;
import fun.vignachu.util.I18n;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.text.MessageFormat;

public class FileOrganizerController {

    // 本类用于管理文件显示与分类的主要逻辑
    @FXML private Button btnChooseSource, btnChooseTarget, btnUndo, btnAddDir, btnDelFile;
    @FXML private ToggleButton tglMode;
    @FXML private Label lblSourceDir, lblTargetDir, lblPreviewHint;
    @FXML private TreeView<String> fileTreeView;
    @FXML private StackPane previewContainer;
    @FXML private VBox classifyContainer;

    private final FileOrganizerModel model = new FileOrganizerModel();
    private PreviewStrategy currentStrategy;
    private boolean hasShownMidiAlert = false;

    @FXML
    public void initialize() {
        bindI18n();
        try { model.initRecycleBin(); } catch (Exception ignored) {}
        updateModeButtonState(false);
        updateUndo();
        clearPreview();

        tglMode.selectedProperty().addListener((o, old, isDel) -> {
            updateModeButtonState(isDel);
            refreshClassifyButtons();
        });

        fileTreeView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                File file = new File(model.getCurrentSourceDir(), newVal.getValue());

                FileType type = FileType.determine(file);
                if (type == FileType.MIDI && !hasShownMidiAlert) {
                    showMidiDialog();
                    hasShownMidiAlert = true;
                }

                updatePreview(file);
            } else {
                clearPreview();
            }
        });
    }

    // 切换文件时强制回收内存
    private void updatePreview(File file) {
        if (currentStrategy != null) {
            currentStrategy.cleanup();
            currentStrategy = null;
            System.gc();
        }

        FileType type = FileType.determine(file);
        currentStrategy = PreviewFactory.getStrategy(type);

        double w = previewContainer.getWidth() - 20;
        double h = previewContainer.getHeight() - 20;

        previewContainer.getChildren().setAll(
                currentStrategy.createPreviewNode(file, w > 0 ? w : 600, h > 0 ? h : 400)
        );
        if (lblPreviewHint != null) lblPreviewHint.setVisible(false);
    }

    private void clearPreview() {
        if (currentStrategy != null) {
            currentStrategy.cleanup();
            currentStrategy = null;
        }
        PreviewStrategy defaultStrategy = new DefaultPreviewStrategy("preview.default_hint");
        previewContainer.getChildren().setAll(defaultStrategy.createPreviewNode(null, 0, 0));
        if (lblPreviewHint != null) lblPreviewHint.setVisible(false);
    }

    private void updateModeButtonState(boolean isDeleteMode) {
        tglMode.textProperty().unbind();
        tglMode.textProperty().bind(I18n.createStringBinding(isDeleteMode ? "mode.delete" : "mode.move"));
        if (isDeleteMode) {
            if(!tglMode.getStyleClass().contains("btn-danger")) tglMode.getStyleClass().add("btn-danger");
            tglMode.getStyleClass().remove("btn-purple");
        } else {
            if(!tglMode.getStyleClass().contains("btn-purple")) tglMode.getStyleClass().add("btn-purple");
            tglMode.getStyleClass().remove("btn-danger");
        }
    }

    private void bindI18n() {
        btnChooseSource.textProperty().bind(I18n.createStringBinding("btn.source"));
        btnChooseTarget.textProperty().bind(I18n.createStringBinding("btn.target"));
        btnAddDir.textProperty().bind(I18n.createStringBinding("btn.new_dir"));
        btnDelFile.textProperty().bind(I18n.createStringBinding("btn.del_file"));
    }

    private void showMidiDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.titleProperty().bind(I18n.createStringBinding("msg.midi_detect_title"));
        alert.setHeaderText(null);
        alert.contentTextProperty().bind(I18n.createStringBinding("msg.midi_detect_content"));
        alert.initOwner(fileTreeView.getScene().getWindow());
        alert.show();
    }

    @FXML void onChooseSource() {
        File dir = chooseDir();
        if (dir != null) { model.setCurrentSourceDir(dir); lblSourceDir.setText(dir.getAbsolutePath()); refreshFileList(); }
    }

    @FXML void onChooseTarget() {
        File dir = chooseDir();
        if (dir != null) { model.setCurrentTargetDir(dir); lblTargetDir.setText(dir.getAbsolutePath()); btnAddDir.setDisable(false); refreshClassifyButtons(); }
    }

    @FXML void onCreateDir() {
        TextInputDialog d = new TextInputDialog();
        d.titleProperty().bind(I18n.createStringBinding("btn.new_dir"));
        d.headerTextProperty().bind(I18n.createStringBinding("msg.folder_name"));
        d.showAndWait().ifPresent(n->{if(n==null||n.trim().isEmpty())return;try{model.createDirectory(n.trim());refreshClassifyButtons();updateUndo();showInfo(I18n.getBundle().getString("msg.success"));}catch(Exception e){showError(e.getMessage());}});
    }

    @FXML void onUndo() {
        try{ActionRecord r=model.undo();if(r!=null){refreshFileList();refreshClassifyButtons();updateUndo();String msg=MessageFormat.format(I18n.getBundle().getString("msg.undo_success"),r.fileName);showInfo(msg);}else{showError("Nothing to undo");}}catch(Exception e){showError(e.getMessage());}
    }

    @FXML void onDeleteFile() {
        TreeItem<String> i=fileTreeView.getSelectionModel().getSelectedItem();
        if(i==null){showError("Please select a file first");return;}
        try{model.deleteFile(i.getValue());refreshFileList();updateUndo();}catch(Exception e){showError(e.getMessage());}
    }

    private void refreshFileList() {
        TreeItem<String> r=new TreeItem<>("root");
        for(File f:model.getSourceFiles())r.getChildren().add(new TreeItem<>(f.getName()));
        fileTreeView.setRoot(r);
        if(!r.getChildren().isEmpty()) fileTreeView.getSelectionModel().select(0); else clearPreview();
    }

    private void refreshClassifyButtons() {
        classifyContainer.getChildren().clear();
        boolean isDel = tglMode.isSelected();
        for (File dir : model.getTargetSubDirs()) {
            Button btn = new Button(dir.getName());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.getStyleClass().add("button");
            if (isDel) btn.setStyle("-fx-border-color: #F44336; -fx-border-width: 0 0 0 3;");
            btn.setOnAction(e -> {
                try {
                    if (isDel) { model.deleteDirectory(dir); refreshClassifyButtons(); }
                    else {
                        TreeItem<String> item = fileTreeView.getSelectionModel().getSelectedItem();
                        if (item != null) { model.moveFile(item.getValue(), dir); refreshFileList(); }
                    }
                    updateUndo();
                } catch (Exception ex) { showError(ex.getMessage()); }
            });
            classifyContainer.getChildren().add(btn);
        }
    }

    private void updateUndo() {
        btnUndo.textProperty().unbind();
        boolean hasAction = model.hasUndo();
        btnUndo.setDisable(!hasAction);
        if (hasAction) {
            ActionType type = model.peekLastActionType();
            if (type != null) {
                btnUndo.textProperty().bind(Bindings.createStringBinding(
                        () -> MessageFormat.format(
                                I18n.createStringBinding("btn.undo_fmt").get(),
                                I18n.createStringBinding("action." + type.name()).get()
                        ),
                        I18n.createStringBinding("btn.undo_fmt"),
                        I18n.createStringBinding("action." + type.name())
                ));
            } else {
                btnUndo.textProperty().bind(I18n.createStringBinding("btn.undo"));
            }
        } else {
            btnUndo.textProperty().bind(I18n.createStringBinding("btn.undo"));
        }
    }

    private File chooseDir() { return new DirectoryChooser().showDialog(fileTreeView.getScene().getWindow()); }
    private void showError(String msg) { Alert a=new Alert(Alert.AlertType.ERROR); a.titleProperty().bind(I18n.createStringBinding("msg.error")); a.setHeaderText(null); String f=MessageFormat.format(I18n.getBundle().getString("msg.op_failed"),msg); a.setContentText(f); a.show(); }
    private void showInfo(String msg) { Alert a=new Alert(Alert.AlertType.INFORMATION); a.titleProperty().bind(I18n.createStringBinding("msg.success")); a.setHeaderText(null); a.setContentText(msg); a.show(); }
}