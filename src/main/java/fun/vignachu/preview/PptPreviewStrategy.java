package fun.vignachu.preview;

import fun.vignachu.util.I18n;
import fun.vignachu.util.ThemeManager;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

public class PptPreviewStrategy implements PreviewStrategy {

    // 显示PPT
    private XMLSlideShow ppt;
    private List<XSLFSlide> slides;
    private int totalPages = 0;
    private int currentPage = 0;
    private Dimension pptSize;

    private ImageView imageView;
    private Label lblPageInfo;
    private Button btnPrev, btnNext;
    private StackPane imageContainer;
    private StackPane loadingOverlay;
    private HBox controlBar;

    private double lastX, lastY;
    private ChangeListener<Boolean> themeListener;

    @Override
    public Node createPreviewNode(File file, double width, double height) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("preview-box");

        // 1. 图片容器
        imageContainer = new StackPane();
        updateBackground();

        // 剪裁溢出部分
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(width, height);
        imageContainer.setClip(clip);
        imageContainer.widthProperty().addListener((o,old,v)-> clip.setWidth(v.doubleValue()));
        imageContainer.heightProperty().addListener((o,old,v)-> clip.setHeight(v.doubleValue()));

        imageView = new ImageView();
        imageView.setPreserveRatio(true);

        loadingOverlay = new StackPane(new ProgressIndicator());
        loadingOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        loadingOverlay.setVisible(false);

        imageContainer.getChildren().addAll(imageView, loadingOverlay);
        root.setCenter(imageContainer);

        // 2. 底部控制栏
        controlBar = createControlBar();
        root.setBottom(controlBar);

        // 3. 交互
        setupGestures();

        // 4. 加载 PPT
        loadPptDocument(file);

        // 5. 主题监听
        themeListener = (obs, oldVal, isDark) -> {
            updateBackground();
            if (controlBar != null) {
                // 刷新控制栏样式
                controlBar.getStyleClass().remove("midi-panel");
                controlBar.getStyleClass().add("midi-panel");
            }
        };
        ThemeManager.darkProperty().addListener(themeListener);

        return root;
    }

    private void updateBackground() {
        String bg = ThemeManager.isDark() ? "#333333" : "#e0e0e0";
        if (imageContainer != null) imageContainer.setStyle("-fx-background-color: " + bg + ";");
    }

    private void loadPptDocument(File file) {
        loadingOverlay.setVisible(true);
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 加载 PPTX (如果需要支持老版 .ppt，需引入 poi-scratchpad 并使用 HSLFSlideShow)
                try (FileInputStream fis = new FileInputStream(file)) {
                    ppt = new XMLSlideShow(fis);
                    slides = ppt.getSlides();
                    totalPages = slides.size();
                    pptSize = ppt.getPageSize();
                }
                return null;
            }
        };

        loadTask.setOnSucceeded(e -> {
            if (totalPages > 0) renderPage(0);
            else loadingOverlay.setVisible(false);
            updateControls();
        });

        loadTask.setOnFailed(e -> {
            loadingOverlay.setVisible(false);
            Label err = new Label();
            err.textProperty().bind(I18n.createStringBinding("msg.ppt_error", loadTask.getException().getMessage()));
            err.setStyle("-fx-text-fill: red; -fx-wrap-text: true;");
            imageContainer.getChildren().setAll(err);
        });

        new Thread(loadTask).start();
    }

    private void renderPage(int pageIndex) {
        if (ppt == null || pageIndex < 0 || pageIndex >= totalPages) return;
        loadingOverlay.setVisible(true);

        Task<Image> renderTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                double scale = 2.0;
                int w = (int) (pptSize.width * scale);
                int h = (int) (pptSize.height * scale);

                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = img.createGraphics();

                // 抗锯齿设置
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                // 缩放绘图上下文
                graphics.scale(scale, scale);

                // 绘制白色背景 (防止透明背景)
                graphics.setPaint(java.awt.Color.WHITE);
                graphics.fill(new Rectangle2D.Float(0, 0, pptSize.width, pptSize.height));

                // 渲染幻灯片
                slides.get(pageIndex).draw(graphics);
                graphics.dispose();

                return SwingFXUtils.toFXImage(img, null);
            }
        };

        renderTask.setOnSucceeded(e -> {
            Image img = renderTask.getValue();
            imageView.setImage(img);
            resetView();

            // 适配窗口大小
            double containerW = imageContainer.getWidth();
            double containerH = imageContainer.getHeight();
            if (containerW > 0 && containerH > 0) {
                double scale = Math.min(containerW / img.getWidth(), containerH / img.getHeight()) * 0.95;
                imageView.setFitWidth(img.getWidth() * scale);
                imageView.setFitHeight(img.getHeight() * scale);
            }

            currentPage = pageIndex;
            updateControls();
            loadingOverlay.setVisible(false);
        });

        renderTask.setOnFailed(e -> loadingOverlay.setVisible(false));
        new Thread(renderTask).start();
    }

    // 交互逻辑 (复用图片浏览逻辑)
    private void setupGestures() {
        imageContainer.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) return;
            double scaleFactor = (event.getDeltaY() > 0) ? 1.1 : 0.9;
            double newScale = imageView.getScaleX() * scaleFactor;
            if (newScale >= 0.1 && newScale <= 5) {
                imageView.setScaleX(newScale);
                imageView.setScaleY(newScale);
            }
            event.consume();
        });

        imageView.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                lastX = event.getSceneX();
                lastY = event.getSceneY();
                imageView.setCursor(javafx.scene.Cursor.MOVE);
            }
        });

        imageView.setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                double dx = event.getSceneX() - lastX;
                double dy = event.getSceneY() - lastY;
                imageView.setTranslateX(imageView.getTranslateX() + dx);
                imageView.setTranslateY(imageView.getTranslateY() + dy);
                lastX = event.getSceneX();
                lastY = event.getSceneY();
            }
        });

        imageView.setOnMouseReleased(event -> imageView.setCursor(javafx.scene.Cursor.DEFAULT));
        imageView.setOnMouseClicked(event -> { if (event.getClickCount() == 2) resetView(); });
    }

    private void resetView() {
        imageView.setScaleX(1.0); imageView.setScaleY(1.0);
        imageView.setTranslateX(0); imageView.setTranslateY(0);
    }

    private HBox createControlBar() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("midi-panel");

        btnPrev = new Button();
        btnPrev.textProperty().bind(I18n.createStringBinding("preview.ppt.prev"));
        btnPrev.getStyleClass().add("btn-blue");

        btnNext = new Button();
        btnNext.textProperty().bind(I18n.createStringBinding("preview.ppt.next"));
        btnNext.getStyleClass().add("btn-blue");

        lblPageInfo = new Label();
        lblPageInfo.getStyleClass().add("label");

        btnPrev.setOnAction(e -> renderPage(currentPage - 1));
        btnNext.setOnAction(e -> renderPage(currentPage + 1));

        box.getChildren().addAll(btnPrev, lblPageInfo, btnNext);
        return box;
    }

    private void updateControls() {
        lblPageInfo.textProperty().bind(Bindings.createStringBinding(
                () -> MessageFormat.format(I18n.getBundle().getString("preview.ppt.page"), currentPage + 1, totalPages),
                I18n.createStringBinding("preview.ppt.page")
        ));
        btnPrev.setDisable(currentPage <= 0);
        btnNext.setDisable(currentPage >= totalPages - 1);
    }

    @Override
    public void cleanup() {
        if (themeListener != null) ThemeManager.darkProperty().removeListener(themeListener);
        try {
            if (ppt != null) {
                ppt.close();
                ppt = null;
            }
        } catch (IOException e) { e.printStackTrace(); }
        if (imageView != null) imageView.setImage(null);
    }
}