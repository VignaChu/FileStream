package fun.vignachu.preview;

import fun.vignachu.util.I18n;
import fun.vignachu.util.ThemeManager;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.apache.poi.xwpf.usermodel.*;

import java.io.File;
import java.io.FileInputStream;

public class WordPreviewStrategy implements PreviewStrategy {

    // 纯文本浏览word文档
    private WebView webView;
    // 缓存解析后的 HTML body，切换主题时直接复用，不需重新读文件
    private String cachedBodyHtml;
    private ChangeListener<Boolean> themeListener;
    private StackPane centerStack;

    @Override
    public Node createPreviewNode(File file, double width, double height) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("preview-box");

        centerStack = new StackPane();
        updateContainerBackground(); // 初始背景

        ProgressIndicator pi = new ProgressIndicator();
        centerStack.getChildren().add(pi);
        root.setCenter(centerStack);

        webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.setVisible(false);
        centerStack.getChildren().add(0, webView);

        // 异步解析 Word (IO密集型)
        Task<String> loadTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return parseDocxBody(file);
            }
        };

        loadTask.setOnSucceeded(e -> {
            cachedBodyHtml = loadTask.getValue();
            renderView(); // 渲染视图
            pi.setVisible(false);
            webView.setVisible(true);
        });

        loadTask.setOnFailed(e -> {
            pi.setVisible(false);
            Label errLabel = new Label();
            errLabel.textProperty().bind(I18n.createStringBinding("msg.read_error", loadTask.getException().getMessage()));
            errLabel.setStyle("-fx-text-fill: red; -fx-wrap-text: true;");
            centerStack.getChildren().add(errLabel);
        });

        // 【主题监听】
        themeListener = (obs, oldVal, isDark) -> {
            updateContainerBackground();
            if (cachedBodyHtml != null) {
                renderView();
            }
        };
        ThemeManager.darkProperty().addListener(themeListener);

        new Thread(loadTask).start();
        return root;
    }

    private void updateContainerBackground() {
        String bgStyle = ThemeManager.isDark() ? "-fx-background-color: #2b2b2b;" : "-fx-background-color: #ffffff;";
        centerStack.setStyle(bgStyle);
    }

    private void renderView() {
        if (webView == null) return;
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        appendCss(html);
        html.append("</style></head><body><div class='doc-container'>");
        html.append(cachedBodyHtml);
        html.append("</div></body></html>");

        webView.getEngine().loadContent(html.toString());

        if (ThemeManager.isDark()) webView.setStyle("-fx-page-fill: #2b2b2b;");
        else webView.setStyle("-fx-page-fill: white;");
    }

    private String parseDocxBody(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            StringBuilder sb = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph) {
                    processParagraph((XWPFParagraph) element, sb);
                } else if (element instanceof XWPFTable) {
                    processTable((XWPFTable) element, sb);
                }
            }
            return sb.toString();
        }
    }

    private void appendCss(StringBuilder sb) {
        boolean isDark = ThemeManager.isDark();
        String bgColor = isDark ? "#2b2b2b" : "#ffffff";
        String textColor = isDark ? "#d4d4d4" : "#333333";
        String borderColor = isDark ? "#555" : "#ccc";

        sb.append("body { background-color: ").append(bgColor).append("; color: ").append(textColor)
                .append("; font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif; margin: 0; padding: 20px; }")
                .append(".doc-container { max-width: 800px; margin: 0 auto; }")
                .append("p { margin-bottom: 10px; line-height: 1.6; }")
                .append("table { border-collapse: collapse; width: 100%; margin: 15px 0; }")
                .append("td, th { border: 1px solid ").append(borderColor).append("; padding: 8px; }")
                .append(".bold { font-weight: bold; }")
                .append(".italic { font-style: italic; }")
                .append(".underline { text-decoration: underline; }");
    }

    private void processParagraph(XWPFParagraph p, StringBuilder sb) {
        String tag = "p";
        String style = p.getStyleID();
        if (style != null && style.startsWith("Heading")) {
            try {
                int level = Integer.parseInt(style.replace("Heading", ""));
                tag = "h" + Math.min(level, 6);
            } catch (NumberFormatException ignored) {}
        }
        sb.append("<").append(tag).append(">");
        for (XWPFRun run : p.getRuns()) {
            sb.append("<span class='");
            if (run.isBold()) sb.append("bold ");
            if (run.isItalic()) sb.append("italic ");
            if (run.getUnderline() != UnderlinePatterns.NONE) sb.append("underline ");
            sb.append("'");
            String color = run.getColor();
            if (color != null) sb.append(" style='color:#").append(color).append(";'");
            sb.append(">");
            sb.append(escapeHtml(run.getText(0)));
            sb.append("</span>");
        }
        sb.append("</").append(tag).append(">");
    }

    private void processTable(XWPFTable table, StringBuilder sb) {
        sb.append("<table>");
        for (XWPFTableRow row : table.getRows()) {
            sb.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                sb.append("<td>");
                sb.append(escapeHtml(cell.getText()));
                sb.append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    @Override
    public void cleanup() {
        if (themeListener != null) {
            ThemeManager.darkProperty().removeListener(themeListener);
        }
        if (webView != null) {
            webView.getEngine().load(null); // 【内存优化】释放 WebKit
            webView = null;
        }
        cachedBodyHtml = null;
    }
}