package fun.vignachu.controller;

import fun.vignachu.model.ExtensionChangerModel;
import fun.vignachu.util.I18n;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

public class ExtensionController {

    // 本类用于管理修改扩展名的逻辑
    @FXML private Label lblCurrentDir, lblInputTip, lblTitle, lblFooterTip;
    @FXML private Button btnApply, btnChooseDir;
    @FXML private VBox fileListContainer;
    @FXML private TextField tfExtension;
    @FXML private CheckBox chkSelectAll;

    private final ExtensionChangerModel model = new ExtensionChangerModel();
    private File currentDir;
    private final Set<File> selectedFiles = new HashSet<>();

    @FXML
    public void initialize() {
        bindI18n();

        chkSelectAll.selectedProperty().addListener((obs, oldVal, isSelected) -> fileListContainer.getChildren().forEach(node -> {
            if (node instanceof ToggleButton) {
                ((ToggleButton) node).setSelected(isSelected);
            }
        }));
    }

    private void bindI18n() {
        lblInputTip.textProperty().bind(I18n.createStringBinding("ext.lbl.input"));
        btnApply.textProperty().bind(I18n.createStringBinding("ext.btn.apply"));
        lblTitle.textProperty().bind(I18n.createStringBinding("ext.title"));
        lblFooterTip.textProperty().bind(I18n.createStringBinding("ext.tip"));
        btnChooseDir.textProperty().bind(I18n.createStringBinding("ext.btn.choose_dir"));
        chkSelectAll.textProperty().bind(I18n.createStringBinding("ext.chk.select_all"));
    }

    @FXML
    void onChooseDir() {
        DirectoryChooser dc = new DirectoryChooser();
        File dir = dc.showDialog(fileListContainer.getScene().getWindow());
        if (dir != null) {
            loadDirectory(dir);
        }
    }

    private void loadDirectory(File dir) {
        this.currentDir = dir;
        lblCurrentDir.setText(dir.getAbsolutePath());
        refreshFileList();
    }

    private void refreshFileList() {
        fileListContainer.getChildren().clear();
        selectedFiles.clear();
        chkSelectAll.setSelected(false);

        if (currentDir == null || !currentDir.exists()) return;

        File[] files = currentDir.listFiles(File::isFile);
        if (files == null) return;

        for (File file : files) {
            ToggleButton btn = new ToggleButton(file.getName());

            // 样式设置
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            btn.getStyleClass().add("file-item-btn");

            // 省略文件名
            btn.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
            btn.setEllipsisString(" ... "); // 可选：设置省略号样式

            // 鼠标悬停显示完整名字
            btn.setTooltip(new Tooltip(file.getName()));

            // 监听逻辑
            btn.selectedProperty().addListener((obs, oldVal, isSelected) -> {
                if (isSelected) selectedFiles.add(file);
                else selectedFiles.remove(file);
            });

            fileListContainer.getChildren().add(btn);
        }
    }

    @FXML
    void onApply() {
        if (currentDir == null) {
            showError(I18n.createStringBinding("ext.msg.select_dir").get());
            return;
        }
        if (selectedFiles.isEmpty()) {
            showError(I18n.createStringBinding("ext.msg.select_files").get());
            return;
        }
        String newExt = tfExtension.getText().trim();
        if (newExt.isEmpty()) {
            showError(I18n.createStringBinding("ext.msg.input_empty").get());
            return;
        }
        if (newExt.startsWith(".")) newExt = newExt.substring(1);

        int success = 0;
        int fail = 0;

        for (File file : selectedFiles) {
            boolean result = model.changeExtension(file, newExt);
            if (result) success++;
            else fail++;
        }

        String msg = MessageFormat.format(I18n.getBundle().getString("ext.msg.success"), success, fail);
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();

        refreshFileList();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }
}