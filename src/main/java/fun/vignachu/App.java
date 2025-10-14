package fun.vignachu;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;

public class App extends Application {

    private Path recycleBinPath;
    // 用于撤销最后一步的单例类
    private final LastActionManager actionManager = new LastActionManager();

    @Override
    public void start(Stage primaryStage) {
        // 初始化回收站路径
        try {
            Path appDir = getAppDirectory();
            recycleBinPath = appDir.resolve(".recycle_bin");
            Files.createDirectories(recycleBinPath);
        } catch (IOException e) {
            showAlert("初始化失败", "无法创建回收站文件夹: " + e.getMessage());
            return;
        }

        TabPane tabPane = new TabPane();
        Tab tab1 = new Tab("文件分流");
        Tab tab2 = new Tab("关于");
        tab2.setContent(new VBox(
                new Label("程序名称：FileStream"),
                new Label("作者：VignaChu"),
                new Label("版本：ver1.1.0"),
                new Label("发布时间2025/10/14")));

        tabPane.getTabs().addAll(FileTabSet(tab1, primaryStage), tab2);
        tabPane.setMinSize(1200, 800);
        Scene scene = new Scene(tabPane);
        primaryStage.setScene(scene);
        primaryStage.setTitle("FileStream");

        // 定义窗口图标
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
        } catch (NullPointerException e) {
            System.err.println("Icon file not found: /images/icon.png");
        }

        // 重写窗口关闭监听，程序关闭时清空回收站
        primaryStage.setOnCloseRequest(event -> {
            clearRecycleBin();
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    // 获取应用程序运行的目录
    private Path getAppDirectory() {
        String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            File file = new File(decodedPath);
            // 如果是打包好的则在jar包下创建临时文件夹
            if (file.isFile() && decodedPath.toLowerCase().endsWith(".jar")) {
                return Paths.get(file.getParent());
            }
            // IDE测试环境下直接创在C盘
            return Paths.get(System.getProperty("user.dir"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error getting application path", e);
        }
    }

    // 清空回收站
    private void clearRecycleBin() {
        if (recycleBinPath != null && Files.exists(recycleBinPath)) {
            try {
                File[] files = recycleBinPath.toFile().listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            deleteDirectory(file);
                        } else {
                            file.delete();
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to clear recycle bin: " + e.getMessage());
            }
        }
    }

    // 自定义 TreeItem 绑定 File 对象
    class FileTreeItem extends TreeItem<String> {
        private final File file;

        public FileTreeItem(String displayName, File file) {
            super(displayName);
            this.file = file;
        }

        public File getFile() {
            return file;
        }
    }

    class DelButton extends Button {
        private boolean isDelDir = false;

        public DelButton() {
            super("删除文件夹");
            this.setStyle("-fx-border-color: #6a0dad; -fx-border-width: 1px; -fx-background-radius: 3px;");
            this.setOnAction(e -> toggleState());
        }

        public boolean onActive() {
            return isDelDir;
        }

        public void reverseActive() {
            isDelDir = !isDelDir;
        }

        private void toggleState() {
            reverseActive();
            if (isDelDir) {
                // 删除模式样式
                setText("点击文件夹按钮以删除");
                setStyle("-fx-background-color:#eb2323; -fx-text-fill: white; -fx-border-color:#7f0303; -fx-border-width: 1px;");
            } else {
                // 恢复默认模式样式
                setText("删除文件夹");
                setStyle("-fx-border-color: #6a0dad; -fx-border-width: 1px; -fx-background-radius: 3px;");
            }
        }
    }

    public Tab FileTabSet(Tab tab, Stage stage) {
        // 布局初始化
        HBox FileTopPane = new HBox(10);
        HBox DirTopPane = new HBox(10);
        HBox DirHandleTopPane = new HBox(10);
        // HBox FileMainTopPane = new HBox(10);
        VBox ClassifyDirListPane = new VBox(5);
        VBox FileDisplayPane = new VBox(10);

        // 主面板 (Center: FileMainPane, Right: DirMainPane)
        BorderPane MainPane = new BorderPane();
        // 右侧分类目录面板 (Top: DirTopPane, Center: DirHandlePane)
        BorderPane DirMainPane = new BorderPane();
        // 目录操作和列表面板 (Top: DirHandleTopPane, Center: DirShowPane)
        BorderPane DirHandlePane = new BorderPane();
        ScrollPane DirShowPane = new ScrollPane();
        // 左侧文件主面板 (Top: FileTopPane, Left: fileTreeView, Center: FileDisplayPane)
        BorderPane FileMainPane = new BorderPane();

        // 文件树视图
        TreeView<String> fileTreeView = new TreeView<>();
        fileTreeView.setShowRoot(false);
        fileTreeView.setMinWidth(200);

        TreeItem<String> rootItem = new TreeItem<>("文件列表");
        fileTreeView.setRoot(rootItem);
        rootItem.setExpanded(true);

        // 文件列表（用数组包装以便在 Lambda 中修改）
        List<File>[] files = new List[]{Collections.emptyList()};

        Label CurrentDir = new Label("请选择文件夹");
        Label CurrentFile = new Label("请先选择一个文件夹");

        // 图片/文件显示区域
        ImageView imageView = new ImageView();
        imageView.setFitWidth(600);
        imageView.setFitHeight(600);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        ScrollPane imageScrollPane = new ScrollPane(imageView);
        imageScrollPane.setFitToWidth(true);
        imageScrollPane.setFitToHeight(true);
        imageScrollPane.setPrefSize(600, 600);


        Button DelFile = createDelFileButton(CurrentFile, files, fileTreeView, imageView);


        HBox FileControlPane = new HBox(10, CurrentFile, DelFile);

        FileDisplayPane.getChildren().addAll(FileControlPane, imageScrollPane);

        FileMainPane.setTop(FileTopPane);
        FileMainPane.setLeft(fileTreeView);
        FileMainPane.setCenter(FileDisplayPane);

        // 添加撤回按钮
        Button undoButton = createUndoButton(CurrentDir, CurrentFile, fileTreeView, files, imageView);

        Button ChooseDir = createChooseDirButton(stage, CurrentDir, fileTreeView, files, CurrentFile, imageView);

        Label TowardDir = new Label("请选择存储用文件夹");
        DelButton DelDir = new DelButton();

        Button AddDir = createAddDirButton(ClassifyDirListPane, TowardDir, DelDir, files, CurrentFile, fileTreeView); // <--- 修改变量名
        Button ChooseTowardDir = createChooseTowardDirButton(AddDir, stage, ClassifyDirListPane, TowardDir, DelDir, files, CurrentFile, fileTreeView); // <--- 修改变量名

        // 布局组装
        DirTopPane.getChildren().addAll(TowardDir, ChooseTowardDir);
        DirHandleTopPane.getChildren().addAll(AddDir, DelDir);

        DirShowPane.setContent(ClassifyDirListPane);
        DirShowPane.setFitToWidth(true);
        DirHandlePane.setTop(DirHandleTopPane);
        DirHandlePane.setCenter(DirShowPane);
        DirMainPane.setTop(DirTopPane);
        DirMainPane.setCenter(DirHandlePane);

        FileTopPane.getChildren().addAll(CurrentDir, ChooseDir, undoButton);

        MainPane.setCenter(FileMainPane);
        MainPane.setRight(DirMainPane);

        tab.setContent(MainPane);
        return tab;
    }

    private Button createUndoButton(Label currentDirLabel, Label currentFileLabel, TreeView<String> treeView, List<File>[] files, ImageView imageView) { // <--- 修改变量名
        Button undoButton = new Button("撤销上一步");
        undoButton.setOnAction(e -> {
            undoButton.setDisable(true);
            ActionRecord record = actionManager.popAction();

            if (record == null) {
                showAlert("无法撤销", "没有可撤销的操作");
                undoButton.setDisable(false);
                return;
            }

            File originalPath = record.originalPath;
            File targetPath = record.targetPath;
            ActionType type = record.type;
            String message = "";

            try {
                if (type == ActionType.MOVE) {
                    // MOVE 撤销：从 targetPath/文件名 移回 originalPath
                    Path sourcePath = targetPath.toPath().resolve(record.fileName);
                    Path finalPath = originalPath.toPath();

                    Files.move(sourcePath, finalPath, StandardCopyOption.REPLACE_EXISTING);

                    File currentSourceDir = finalPath.getParent().toFile();
                    reloadSourceDirectory(currentSourceDir, currentDirLabel, treeView, files, currentFileLabel, imageView); // <--- 修改变量名

                    message = "文件 '" + record.fileName + "' 已从分类目录撤回到源文件夹";

                } else if (type == ActionType.DELETE_PIC) {
                    // DELETE_PIC 撤销：从 targetPath (回收站中的文件) 移回 originalPath (源目录)
                    Path sourcePath = targetPath.toPath();
                    Path finalPath = originalPath.toPath();

                    Files.move(sourcePath, finalPath, StandardCopyOption.REPLACE_EXISTING);

                    File currentSourceDir = finalPath.getParent().toFile();
                    reloadSourceDirectory(currentSourceDir, currentDirLabel, treeView, files, currentFileLabel, imageView); // <--- 修改变量名

                    message = "文件 '" + record.fileName + "' 已从回收站撤回到源文件夹";

                } else if (type == ActionType.DELETE_DIR) {
                    // DELETE_DIR 撤销：从 targetPath (回收站中的文件夹) 移回 originalPath (目标分类目录)
                    Path sourcePath = targetPath.toPath();
                    Path finalPath = originalPath.toPath();

                    Files.move(sourcePath, finalPath, StandardCopyOption.REPLACE_EXISTING);

                    // 刷新分类按钮列表
                    if (originalPath.getParentFile() != null) {
                        refreshClassifyButtonsFromUndo(treeView, originalPath.getParentFile().getAbsolutePath(), files, currentFileLabel, imageView); // <--- 修改变量名
                    }

                    message = "文件夹 '" + record.fileName + "' 已从回收站撤回到分类目录";
                } else if (type == ActionType.CREATE_DIR) {
                    // CREATE_DIR 撤销：删除新建的文件夹 (originalPath)

                    boolean success = deleteDirectory(originalPath); // originalPath 是新建文件夹的完整路径

                    if (success) {
                        // 刷新分类按钮列表 (targetPath 是父目录)
                        if (targetPath != null && targetPath.isDirectory()) {
                            refreshClassifyButtonsFromUndo(treeView, targetPath.getAbsolutePath(), files, currentFileLabel, imageView); // <--- 修改变量名
                        }
                        message = "新建的文件夹 '" + record.fileName + "' 已被删除";
                    } else {
                        throw new IOException("Failed to delete created directory.");
                    }
                }

                showInfoAlert("撤销成功", message);

            } catch (Exception ex) {
                showAlert("撤销失败", "无法撤销操作:\n" + ex.getMessage());
            }

            undoButton.setDisable(false);
            updateUndoButtonText(treeView);
        });

        if (actionManager.hasAction()) {
            undoButton.setText("撤销上一步 (剩余 " + actionManager.size() + ")");
        } else {
            undoButton.setText("撤销上一步");
        }
        return undoButton;
    }

    // 在撤销操作（DELETE_DIR 或 CREATE_DIR）后，找到并刷新分类按钮列表。
    private void refreshClassifyButtonsFromUndo(TreeView<String> treeView, String towardDirPath, List<File>[] files, Label currentFileLabel, ImageView imageView) { // <--- 修改变量名

        // 1. 查找 MainPane (BorderPane)
        BorderPane picMainPane = (BorderPane) treeView.getParent();
        BorderPane mainPane = (BorderPane) picMainPane.getParent();

        // 2. 查找 DirMainPane (MainPane 的 Right 部分)
        BorderPane dirMainPane = (BorderPane) mainPane.getRight();

        // 3. 查找 DirHandlePane (DirMainPane 的 Center 部分)
        BorderPane dirHandlePane = (BorderPane) dirMainPane.getCenter();

        // 4. 查找 DirShowPane (ScrollPane) (DirHandlePane 的 Center 部分)
        ScrollPane dirShowPane = (ScrollPane) dirHandlePane.getCenter();

        // 5. 查找分类按钮容器 VBox
        VBox container = (VBox) dirShowPane.getContent();

        // 6. 查找 DelButton 所在的 HBox (DirHandlePane 的 Top 部分)
        HBox handleTop = (HBox) dirHandlePane.getTop();

        // 7. 查找 DelButton
        DelButton delButton = (DelButton) handleTop.getChildren().get(1);

        // 8. 查找 TowardDirLabel (在 DirMainPane 的 Top HBox 中)
        HBox dirTopPane = (HBox) dirMainPane.getTop();
        Label towardDirLabel = (Label) dirTopPane.getChildren().get(0);

        // 设置 TowardDirLabel 以便 refreshClassifyButtons 使用
        towardDirLabel.setText(towardDirPath);

        refreshClassifyButtons(container, towardDirLabel, delButton, files, currentFileLabel, treeView, imageView); // <--- 修改变量名
    }


     // 更新撤销按钮文本。在撤回按钮旁显示上条指令的种类】

    private void updateUndoButtonText(TreeView<String> treeView) {

        // 强制将 treeView.getParent() (FileMainPane) 转换为 BorderPane。
        BorderPane fileMainPane = (BorderPane) treeView.getParent();

        // 调用 BorderPane 的 getTop()，并强制转换为 HBox (FileTopPane)。
        HBox fileTopPane = (HBox) fileMainPane.getTop();


        if (fileTopPane.getChildren().size() > 2 && fileTopPane.getChildren().get(2) instanceof Button) { // <--- 修改变量名
            Button undoButton = (Button) fileTopPane.getChildren().get(2); // <--- 修改变量名

            if (actionManager.hasAction()) {
                String lastActionType = actionManager.peekAction().type.toString();
                String actionName = "";
                switch (lastActionType) {
                    case "MOVE": actionName = "移动文件"; break;
                    case "DELETE_PIC": actionName = "删除文件"; break;
                    case "DELETE_DIR": actionName = "删除文件夹"; break;
                    case "CREATE_DIR": actionName = "创建文件夹"; break;
                    default: actionName = "未知操作";
                }

                // 显示上条指令的种类
                undoButton.setText("撤销上一步 [" + actionName + "] (剩余 " + actionManager.size() + ")");
            } else {
                undoButton.setText("撤销上一步");
            }
        }
    }

    private Button createChooseDirButton(Stage stage, Label currentDirLabel, TreeView<String> treeView, List<File>[] files, Label currentFileLabel, ImageView imageView) { // <--- 修改变量名
        Button chooseDir = new Button("选择文件夹");
        chooseDir.setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("选择目标文件夹");
            dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File folder = dirChooser.showDialog(stage);

            if (folder != null) {
                reloadSourceDirectory(folder, currentDirLabel, treeView, files, currentFileLabel, imageView); // <--- 修改变量名
            }
        });
        return chooseDir;
    }

    private void reloadSourceDirectory(File folder, Label currentDirLabel, TreeView<String> treeView, List<File>[] files, Label currentFileLabel, ImageView imageView) { // <--- 修改变量名
        if (folder == null || !folder.isDirectory()) return;

        currentDirLabel.setText("当前文件夹: " + folder.getAbsolutePath());
        Pattern IMG_PATTERN = Pattern.compile("(?i).*\\.(png|jpe?g|gif|bmp|webp|svg)$");
        File[] fileArray = folder.listFiles(f -> f.isFile() && IMG_PATTERN.matcher(f.getName()).matches()); // 保留图像匹配逻辑，因为ImageView依赖
        files[0] = fileArray != null ? Arrays.asList(fileArray) : Collections.emptyList(); // <--- 修改变量名

        if (!files[0].isEmpty()) {
            loadFileTree(treeView, folder, files, currentFileLabel, imageView);
            treeView.getSelectionModel().select(treeView.getRoot().getChildren().get(0));
            loadAndDisplayFile(files[0].get(0), imageView, currentFileLabel);
        } else {
            currentFileLabel.setText("该文件夹中没有找到支持的图像文件");
            treeView.getRoot().getChildren().clear();
            imageView.setImage(null);
        }
    }

    private Button createDelFileButton(Label currentFile, List<File>[] files, TreeView<String> fileTreeView, ImageView imageView){
        Button DelFile = new Button("删除当前文件");
        DelFile.setOnAction(event -> {
            if(currentFile.getText().equals("请选择文件夹") || files[0].isEmpty()){
                showAlert("无法删除", "文件文件夹不可为空");
                return;
            }

            File fileToMove = getSelectedFile(fileTreeView, files);
            if (fileToMove == null) {
                showAlert("无文件选中", "请先在左侧文件列表中选择一个文件");
                return;
            }

            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("FileStream");
            alert.setContentText("确认删除当前文件吗？ (将移入回收站)");
            final Optional<ButtonType> opt = alert.showAndWait();

            if (opt.isPresent() && opt.get() == ButtonType.OK) {
                String uniqueName = fileToMove.getName() + "_" + System.currentTimeMillis();
                Path targetPath = recycleBinPath.resolve(uniqueName);

                try {
                    Files.move(fileToMove.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                    // 记录操作 (DELETE_PIC: 原文件路径, 回收站中的新文件路径)
                    actionManager.recordAction(ActionType.DELETE_PIC, fileToMove, targetPath.toFile()); // 保持 ActionType 不变

                    updateAfterFileMove(fileToMove, files, currentFile, fileTreeView, imageView); // <--- 修改方法名和变量名
                    updateUndoButtonText(fileTreeView);


                } catch (IOException e) {
                    showAlert("删除失败", "无法移动文件到回收站: " + e.getMessage()); // <--- 修改
                }
            }
        });
        return DelFile;
    }

    private Button createAddDirButton(VBox container, Label towardDirLabel, DelButton delButton, List<File>[] files, Label currentFileLabel, TreeView<String> fileTreeView) { // <--- 修改变量名
        Button addDir = new Button("新建文件夹");
        addDir.setDisable(true);
        addDir.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("创建文件夹");
            dialog.setHeaderText("请输入新文件夹的名称：");
            dialog.setContentText("文件夹名:");

            dialog.showAndWait().ifPresent(folderName -> {
                if (folderName.trim().isEmpty()) {
                    showAlert("输入无效", "文件夹名不能为空！");
                    return;
                }
                File parentFile = new File(towardDirLabel.getText());
                Path newDirPath = Paths.get(parentFile.getAbsolutePath(), folderName.trim());
                try {
                    Files.createDirectories(newDirPath);

                    // 记录 CREATE_DIR 操作
                    actionManager.recordAction(ActionType.CREATE_DIR, newDirPath.toFile(), parentFile);
                    updateUndoButtonText(fileTreeView);

                    refreshClassifyButtons(container, towardDirLabel, delButton, files, currentFileLabel, fileTreeView, null); // <--- 修改变量名
                } catch (Exception ex) {
                    showAlert("创建失败", "无法创建文件夹:\n" + ex.getMessage());
                }
            });
        });
        return addDir;
    }

    private Button createChooseTowardDirButton(Button addDir, Stage stage, VBox container, Label towardDirLabel, DelButton delButton, List<File>[] files, Label currentFileLabel, TreeView<String> fileTreeView) { // <--- 修改变量名
        Button chooseTowardDir = new Button("选择文件夹");
        chooseTowardDir.setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("选择分类根目录文件夹");
            dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File folder = dirChooser.showDialog(stage);
            if (folder != null) {
                addDir.setDisable(false);
                towardDirLabel.setText(folder.getAbsolutePath());
                refreshClassifyButtons(container, towardDirLabel, delButton, files, currentFileLabel, fileTreeView, null); // <--- 修改变量名
            }
        });
        return chooseTowardDir;
    }

    // 刷新分类按钮列表
    private void refreshClassifyButtons(VBox container, Label towardDirLabel, DelButton delButton, List<File>[] files, Label currentFileLabel, TreeView<String> fileTreeView, ImageView imageView) { // <--- 修改变量名
        container.getChildren().clear();
        File rootDir = new File(towardDirLabel.getText());
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            return;
        }
        File[] dirs = rootDir.listFiles(File::isDirectory);
        if (dirs == null) return;

        for (File dir : dirs) {
            Button btn = new Button(dir.getName());
            btn.setOnAction(e -> {
                if (delButton.onActive()) {
                    // 文件夹删除逻辑
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("确认删除");
                    alert.setHeaderText("是否删除 '" + dir.getName() + "' 文件夹？");
                    alert.setContentText("里面的文件也会一并删除，并移入回收站。"); // <--- 修改
                    ButtonType yes = new ButtonType("是的", ButtonBar.ButtonData.YES);
                    ButtonType no = new ButtonType("不", ButtonBar.ButtonData.NO);
                    alert.getButtonTypes().setAll(yes, no);
                    alert.showAndWait().ifPresent(result -> {
                        if (result == yes) {

                            File originalDir = dir;
                            String uniqueDirName = dir.getName() + "_DIR_" + System.currentTimeMillis();
                            Path targetRecyclePath = recycleBinPath.resolve(uniqueDirName);

                            try {
                                // 移动文件夹到回收站
                                Files.move(originalDir.toPath(), targetRecyclePath, StandardCopyOption.REPLACE_EXISTING);

                                // 记录操作 (DELETE_DIR: 原文件夹路径, 回收站中的文件夹路径)
                                actionManager.recordAction(ActionType.DELETE_DIR, originalDir, targetRecyclePath.toFile());

                                refreshClassifyButtons(container, towardDirLabel, delButton, files, currentFileLabel, fileTreeView, imageView);
                                updateUndoButtonText(fileTreeView);


                            } catch (IOException ex) {
                                showAlert("删除失败", "无法移动文件夹到回收站: " + ex.getMessage());
                            }
                        }
                    });
                } else {
                    // 文件分类逻辑
                    File fileToMove = getSelectedFile(fileTreeView, files);
                    if (fileToMove == null) {
                        showAlert("无文件选中", "请先在左侧文件列表中选择一个文件");
                        return;
                    }

                    Path targetPath = dir.toPath().resolve(fileToMove.getName());
                    try {
                        File originalFile = fileToMove;

                        Files.move(originalFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                        // 记录操作 (MOVE: 原文件路径, 目标文件夹路径)
                        actionManager.recordAction(ActionType.MOVE, originalFile, dir);

                        updateAfterFileMove(originalFile, files, currentFileLabel, fileTreeView, imageView);
                        updateUndoButtonText(fileTreeView);

                    } catch (IOException ex) {
                        showAlert("移动失败", "无法移动文件: " + ex.getMessage());
                    }
                }
            });
            container.getChildren().add(btn);
        }
    }

    // 获取当前选中的文件
    private File getSelectedFile(TreeView<String> treeView, List<File>[] files) {
        TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem instanceof FileTreeItem) {
            return ((FileTreeItem) selectedItem).getFile();
        }
        if (treeView.getRoot() != null && !treeView.getRoot().getChildren().isEmpty()) {
            TreeItem<String> firstItem = treeView.getRoot().getChildren().get(0);
            if (firstItem instanceof FileTreeItem) {
                return ((FileTreeItem) firstItem).getFile();
            }
        }
        return null;
    }

    // 移动文件后更新界面
    private void updateAfterFileMove(File movedFile, List<File>[] files, Label currentFileLabel, TreeView<String> treeView, ImageView imageView) { // <--- 修改方法名和变量名
        if (files[0] != null && !files[0].isEmpty()) {
            List<File> newList = new ArrayList<>(files[0]);
            newList.remove(movedFile);
            files[0] = newList;

            // 从树中移除
            TreeItem<String> root = treeView.getRoot();
            root.getChildren().removeIf(item -> {
                if (item instanceof FileTreeItem) {
                    return ((FileTreeItem) item).getFile().equals(movedFile);
                }
                return false;
            });

            // 更新显示
            if (!files[0].isEmpty()) {
                if (!root.getChildren().isEmpty()) {
                    treeView.getSelectionModel().select(root.getChildren().get(0));
                }
            } else {
                currentFileLabel.setText("没有更多文件了");
                treeView.getRoot().getChildren().clear();
                imageView.setImage(null);
            }
        }
    }

    // 加载文件树
    private void loadFileTree(TreeView<String> treeView, File folder, List<File>[] files, Label currentFileLabel, ImageView imageView) {
        TreeItem<String> root = treeView.getRoot();
        if (root == null) {
            root = new TreeItem<>("文件列表");
            treeView.setRoot(root);
        }
        root.getChildren().clear();

        Pattern IMG_PATTERN = Pattern.compile("(?i).*\\.(png|jpe?g|gif|bmp|webp|svg)$");
        File[] fileArray = folder.listFiles(f -> f.isFile() && IMG_PATTERN.matcher(f.getName()).matches());

        if (fileArray != null) {
            Arrays.sort(fileArray);
            for (File file : fileArray) {
                FileTreeItem item = new FileTreeItem(file.getName(), file);
                root.getChildren().add(item);
            }
        }

        // 监听选中项变化
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal instanceof FileTreeItem) {
                FileTreeItem fileItem = (FileTreeItem) newVal;
                File selectedFile = fileItem.getFile();
                currentFileLabel.setText("当前文件: " + selectedFile.getName());
                loadAndDisplayFile(selectedFile, imageView, currentFileLabel);

                if (files[0] != null && !files[0].isEmpty()) {
                    List<File> list = new ArrayList<>(files[0]);
                    list.remove(selectedFile);
                    list.add(0, selectedFile);
                    files[0] = list;
                }
            }
        });

        root.setExpanded(true);
    }

    // 异步加载文件/图像
    private void loadAndDisplayFile(File file, ImageView imageView, Label statusLabel) {
        if (file != null && file.exists()) {
            statusLabel.setText("正在加载文件: " + file.getName());
            new Thread(() -> {
                try {
                    Image image = new Image(file.toURI().toString(), true);
                    image.progressProperty().addListener((obs, oldProg, newProg) -> {
                        if (newProg.doubleValue() == 1.0) {
                            Platform.runLater(() -> {
                                if (!image.isError()) {
                                    imageView.setImage(image);
                                    statusLabel.setText("当前文件: " + file.getName() +
                                            " (" + (int) image.getWidth() + "×" + (int) image.getHeight() + ")");
                                } else {
                                    statusLabel.setText("文件加载失败 (非图像文件): " + file.getName()); // 暂时只支持图像
                                    imageView.setImage(null);
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        statusLabel.setText("文件加载异常: " + e.getMessage());
                        imageView.setImage(null);
                    });
                }
            }).start();
        } else {
            statusLabel.setText("文件不存在或无法访问");
            imageView.setImage(null);
        }
    }

    // 递归删除目录
    public static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }

    // 显示错误弹窗
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}