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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        TabPane tabPane = new TabPane();
        Tab tab1 = new Tab("图片分流");
        Tab tab2 = new Tab("关于");
        tab2.setContent(new VBox(
                new Label("程序名称：FileStream"),
                new Label("作者：VignaChu"),
                new Label("版本：ver1.0.0"),
                new Label("发布时间2025/10/13")));

        tabPane.getTabs().addAll(PicTabSet(tab1, primaryStage), tab2);
        tabPane.setMinSize(1200, 800);
        Scene scene = new Scene(tabPane);
        primaryStage.setScene(scene);
        primaryStage.setTitle("FileStream");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
        primaryStage.show();
    }

    // TODO 自定义枚举类型记录前状态

    // TODO 自定义单例类存储上一动作

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

    // 自定义删除按钮（切换状态）
    class DelButton extends Button {
        private boolean isDelDir = false;

        public DelButton() {
            super("删除文件夹");
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
                setText("点击文件夹按钮以删除");
                setStyle("-fx-background-color:#eb2323ff; -fx-border-color:#7f0303ff;");
            } else {
                setText("删除文件夹");
                setStyle("");
            }
        }
    }

    public Tab PicTabSet(Tab tab, Stage stage) {
        // 主布局
        HBox PicTopPane = new HBox(10);
        HBox DirTopPane = new HBox(10);
        HBox DirHandleTopPane = new HBox(10);
        HBox PicMainTopPane = new HBox(10);
        VBox ClassifyDirListPane = new VBox(5);
        VBox ImageDisplayPane = new VBox(10);
        BorderPane MainPane = new BorderPane();
        BorderPane DirMainPane = new BorderPane();
        BorderPane DirHandlePane = new BorderPane();
        ScrollPane DirShowPane = new ScrollPane();
        BorderPane PicMainPane = new BorderPane();

        // 图片树视图
        TreeView<String> fileTreeView = new TreeView<>();
        fileTreeView.setShowRoot(false);
        fileTreeView.setMinWidth(200);

        TreeItem<String> rootItem = new TreeItem<>("图片列表");
        fileTreeView.setRoot(rootItem);
        rootItem.setExpanded(true);

        // 图片列表（用数组包装以便在 Lambda 中修改）
        List<File>[] images = new List[]{Collections.emptyList()};

        Label CurrentDir = new Label("请选择文件夹");
        Label CurrentPic = new Label("请先选择一个文件夹");

        // 图片显示区域
        ImageView imageView = new ImageView();
        imageView.setFitWidth(600);
        imageView.setFitHeight(600);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        ScrollPane imageScrollPane = new ScrollPane(imageView);
        imageScrollPane.setFitToWidth(true);
        imageScrollPane.setFitToHeight(true);
        imageScrollPane.setPrefSize(600, 600);
        Button DelPic =createDelPicButton(CurrentPic, images, fileTreeView);

        PicMainTopPane.getChildren().addAll(CurrentPic, DelPic);
        ImageDisplayPane.getChildren().addAll(PicMainTopPane, imageScrollPane);

        PicMainPane.setTop(PicTopPane);
        PicMainPane.setLeft(fileTreeView);
        PicMainPane.setCenter(ImageDisplayPane);

        // 创建按钮
        Button ChooseDir = createChooseDirButton(stage, CurrentDir, fileTreeView, images, CurrentPic, imageView);

        Label TowardDir = new Label("请选择存储用文件夹");
        DelButton DelDir = new DelButton();

        Button AddDir = createAddDirButton(ClassifyDirListPane, TowardDir, DelDir, images, CurrentPic, fileTreeView);
        Button ChooseTowardDir = createChooseTowardDirButton(AddDir, stage, ClassifyDirListPane, TowardDir, DelDir, images, CurrentPic, fileTreeView);

        // 布局组装
        DirTopPane.getChildren().addAll(TowardDir, ChooseTowardDir);
        DirHandleTopPane.getChildren().addAll(AddDir, DelDir);

        DirShowPane.setContent(ClassifyDirListPane);
        DirShowPane.setFitToWidth(true);
        DirHandlePane.setTop(DirHandleTopPane);
        DirHandlePane.setCenter(DirShowPane);
        DirMainPane.setTop(DirTopPane);
        DirMainPane.setCenter(DirHandlePane);

        PicTopPane.getChildren().addAll(CurrentDir, ChooseDir);

        MainPane.setCenter(PicMainPane);
        MainPane.setRight(DirMainPane);

        tab.setContent(MainPane);
        return tab;
    }

    private Button createChooseDirButton(Stage stage, Label currentDirLabel, TreeView<String> treeView, List<File>[] images, Label currentPicLabel, ImageView imageView) {
        Button chooseDir = new Button("选择文件夹");
        chooseDir.setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("选择目标文件夹");
            dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File folder = dirChooser.showDialog(stage);

            if (folder != null) {
                currentDirLabel.setText("当前文件夹: " + folder.getAbsolutePath());
                Pattern IMG_PATTERN = Pattern.compile("(?i).*\\.(png|jpe?g|gif|bmp|webp|svg)$");
                File[] files = folder.listFiles(f -> f.isFile() && IMG_PATTERN.matcher(f.getName()).matches());
                images[0] = files != null ? Arrays.asList(files) : Collections.emptyList();

                if (!images[0].isEmpty()) {
                    currentPicLabel.setText("当前图片: " + images[0].get(0).getName());
                    loadFileTree(treeView, folder, images, currentPicLabel, imageView);
                    loadAndDisplayImage(images[0].get(0), imageView, currentPicLabel);
                } else {
                    currentPicLabel.setText("该文件夹中没有找到支持的图片文件");
                    treeView.getRoot().getChildren().clear();
                    imageView.setImage(null);
                }
            }
        });
        return chooseDir;
    }

    private Button createDelPicButton(Label currentPic, List<File>[] images, TreeView<String> fileTreeView){
        Button DelPic = new Button("删除当前图片");
        DelPic.setOnAction(event -> {
            if(currentPic.getText().equals("请选择文件夹")||images[0].isEmpty()){
                showAlert("无法删除", "图像文件夹不可为空");
            }
            else{
                File imageToMove = getSelectedImage(fileTreeView, images);
                if (imageToMove == null) {
                    showAlert("无图片选中", "请先在左侧图片列表中选择一张图片");
                }
                else{
                    final Alert alert = new Alert(Alert.AlertType.CONFIRMATION); // 实体化Alert对话框对象，并直接在建构子设置对话框的消息类型
                    alert.setTitle("PicStream");
                    alert.setContentText("确认删除当前图片吗？");
                    final Optional<ButtonType> opt = alert.showAndWait();
                    final ButtonType rtn = opt.get();
                    System.out.println(rtn);
                    if (rtn == ButtonType.OK) {
                        try {
                            Files.deleteIfExists(imageToMove.toPath());
                            updateAfterImageMove(imageToMove, images, currentPic, fileTreeView);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }
            }
        });
        return DelPic;
    }

    private Button createAddDirButton(VBox container, Label towardDirLabel, DelButton delButton, List<File>[] images, Label currentPicLabel, TreeView<String> fileTreeView) {
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
                    refreshClassifyButtons(container, towardDirLabel, delButton, images, currentPicLabel, fileTreeView);
                } catch (Exception ex) {
                    showAlert("创建失败", "无法创建文件夹:\n" + ex.getMessage());
                }
            });
        });
        return addDir;
    }

    private Button createChooseTowardDirButton(Button addDir, Stage stage, VBox container, Label towardDirLabel, DelButton delButton, List<File>[] images, Label currentPicLabel, TreeView<String> fileTreeView) {
        Button chooseTowardDir = new Button("选择文件夹");
        chooseTowardDir.setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("选择分类根目录文件夹");
            dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File folder = dirChooser.showDialog(stage);
            if (folder != null) {
                addDir.setDisable(false);
                towardDirLabel.setText(folder.getAbsolutePath());
                refreshClassifyButtons(container, towardDirLabel, delButton, images, currentPicLabel, fileTreeView);
            }
        });
        return chooseTowardDir;
    }

    // 刷新分类按钮列表
    private void refreshClassifyButtons(VBox container, Label towardDirLabel, DelButton delButton, List<File>[] images, Label currentPicLabel, TreeView<String> fileTreeView) {
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
                    // 删除模式
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("确认删除");
                    alert.setHeaderText("是否删除 '" + dir.getName() + "' 文件夹？");
                    alert.setContentText("里面的文件也会一并删除。");
                    ButtonType yes = new ButtonType("是的", ButtonBar.ButtonData.YES);
                    ButtonType no = new ButtonType("不", ButtonBar.ButtonData.NO);
                    alert.getButtonTypes().setAll(yes, no);
                    alert.showAndWait().ifPresent(result -> {
                        if (result == yes) {
                            deleteDirectory(dir);
                            refreshClassifyButtons(container, towardDirLabel, delButton, images, currentPicLabel, fileTreeView);
                        }
                    });
                } else {
                    // 分类模式：移动当前选中图片
                    File imageToMove = getSelectedImage(fileTreeView, images);
                    if (imageToMove == null) {
                        showAlert("无图片选中", "请先在左侧图片列表中选择一张图片");
                        return;
                    }

                    Path targetPath = dir.toPath().resolve(imageToMove.getName());
                    try {
                        Files.move(imageToMove.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                        updateAfterImageMove(imageToMove, images, currentPicLabel, fileTreeView);
                        new Alert(Alert.AlertType.INFORMATION,
                                "图片 '" + imageToMove.getName() + "' 已成功移动到 '" + dir.getName() + "' 文件夹",
                                ButtonType.OK).show();
                    } catch (IOException ex) {
                        showAlert("移动失败", "无法移动图片: " + ex.getMessage());
                    }
                }
            });
            container.getChildren().add(btn);
        }
    }

    // TODO 添加撤回按钮

    // 获取当前选中的图片
    private File getSelectedImage(TreeView<String> treeView, List<File>[] images) {
        TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem instanceof FileTreeItem) {
            return ((FileTreeItem) selectedItem).getFile();
        }
        return null;
    }

    // 移动图片后更新界面
    private void updateAfterImageMove(File movedImage, List<File>[] images, Label currentPicLabel, TreeView<String> treeView) {
        if (images[0] != null && !images[0].isEmpty()) {
            List<File> newList = new ArrayList<>(images[0]);
            newList.remove(movedImage);
            images[0] = newList;

            // 从树中移除
            TreeItem<String> root = treeView.getRoot();
            root.getChildren().removeIf(item -> {
                if (item instanceof FileTreeItem) {
                    return ((FileTreeItem) item).getFile().equals(movedImage);
                }
                return false;
            });

            // 更新显示
            if (!images[0].isEmpty()) {
                File nextImage = images[0].get(0);
                currentPicLabel.setText("当前图片: " + nextImage.getName());
                // 可选：自动显示下一张
            } else {
                currentPicLabel.setText("没有更多图片了");
                treeView.getRoot().getChildren().clear();
            }
        }
    }

    // 加载文件树
    private void loadFileTree(TreeView<String> treeView, File folder, List<File>[] images, Label currentPicLabel, ImageView imageView) {
        TreeItem<String> root = treeView.getRoot();
        if (root == null) {
            root = new TreeItem<>("图片列表");
            treeView.setRoot(root);
        }
        root.getChildren().clear();

        Pattern IMG_PATTERN = Pattern.compile("(?i).*\\.(png|jpe?g|gif|bmp|webp|svg)$");
        File[] files = folder.listFiles(f -> f.isFile() && IMG_PATTERN.matcher(f.getName()).matches());

        if (files != null) {
            Arrays.sort(files);
            for (File file : files) {
                FileTreeItem item = new FileTreeItem(file.getName(), file);
                root.getChildren().add(item);
            }
        }

        // 监听选中项变化
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal instanceof FileTreeItem) {
                FileTreeItem fileItem = (FileTreeItem) newVal;
                File selectedFile = fileItem.getFile();
                currentPicLabel.setText("当前图片: " + selectedFile.getName());
                loadAndDisplayImage(selectedFile, imageView, currentPicLabel);

                // 更新图片列表顺序
                if (images[0] != null && !images[0].isEmpty()) {
                    List<File> list = new ArrayList<>(images[0]);
                    list.remove(selectedFile);
                    list.add(0, selectedFile);
                    images[0] = list;
                }
            }
        });

        root.setExpanded(true);
    }

    // 异步加载图片
    private void loadAndDisplayImage(File imageFile, ImageView imageView, Label statusLabel) {
        if (imageFile != null && imageFile.exists()) {
            statusLabel.setText("正在加载图片: " + imageFile.getName());
            new Thread(() -> {
                try {
                    Image image = new Image(imageFile.toURI().toString(), true);
                    image.progressProperty().addListener((obs, oldProg, newProg) -> {
                        if (newProg.doubleValue() == 1.0) {
                            Platform.runLater(() -> {
                                if (!image.isError()) {
                                    imageView.setImage(image);
                                    statusLabel.setText("当前图片: " + imageFile.getName() +
                                            " (" + (int) image.getWidth() + "×" + (int) image.getHeight() + ")");
                                } else {
                                    statusLabel.setText("图片加载失败: " + imageFile.getName());
                                    imageView.setImage(null);
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        statusLabel.setText("图片加载异常: " + e.getMessage());
                        imageView.setImage(null);
                    });
                }
            }).start();
        } else {
            statusLabel.setText("图片文件不存在或无法访问");
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

}