package fun.vignachu.preview;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fun.vignachu.util.I18n;
import fun.vignachu.util.ThemeManager;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class TextPreviewStrategy implements PreviewStrategy {

    // 文本（代码，markdown，ipynb）显示
    private WebView webView;
    private TextArea editorArea;
    private HBox controlBar;
    private StackPane centerStack;

    private File currentFile;
    private String fileContent;
    private String fileExt;
    private boolean isMarkdown;
    private boolean isIpynb;

    private boolean isRenderedMode = false;
    private boolean isEditingMode = false;

    private Button btnToggleRender, btnEdit, btnSave, btnCancel;
    private ChangeListener<Boolean> themeListener;

    @Override
    public Node createPreviewNode(File file, double width, double height) {
        this.currentFile = file;
        BorderPane root = new BorderPane();
        root.getStyleClass().add("preview-box");

        webView = new WebView();
        webView.setContextMenuEnabled(false);

        editorArea = new TextArea();
        editorArea.getStyleClass().add("code-editor");
        editorArea.setVisible(false);

        centerStack = new StackPane(webView, editorArea);
        updateContainerBackground();
        root.setCenter(centerStack);

        try {
            loadFileContent();
            isRenderedMode = false;
            updateWebViewContent();

            controlBar = createControlBar();
            root.setBottom(controlBar);

            // 【主题监听】
            themeListener = (obs, oldVal, isDark) -> {
                updateContainerBackground();
                if (isEditingMode) {
                    saveFile(true); // 静默保存
                    exitEditMode();
                }
                updateWebViewContent();
            };
            ThemeManager.darkProperty().addListener(themeListener);

        } catch (IOException e) {
            Label errLabel = new Label();
            errLabel.textProperty().bind(I18n.createStringBinding("msg.read_error", e.getMessage()));
            return new StackPane(errLabel);
        }

        return root;
    }

    private void updateContainerBackground() {
        String bgStyle = ThemeManager.isDark() ? "-fx-background-color: #2b2b2b;" : "-fx-background-color: #ffffff;";
        centerStack.setStyle("-fx-padding: 1; " + bgStyle);
    }

    // 限制读取大小
    private void loadFileContent() throws IOException {
        String name = currentFile.getName().toLowerCase();
        int dotIndex = name.lastIndexOf(".");
        fileExt = dotIndex > 0 ? name.substring(dotIndex + 1) : "txt";
        isMarkdown = name.endsWith(".md") || name.endsWith(".markdown");
        isIpynb = name.endsWith(".ipynb");

        // 限制最大预览 100KB，防止大文件卡死
        int MAX_CHARS = 100000;
        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(currentFile, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int numRead;
            int total = 0;
            while ((numRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, numRead);
                total += numRead;
                if (total >= MAX_CHARS) {
                    sb.append("\n\n... (Content truncated for performance) ...");
                    // 大文件禁止编辑，防止覆盖原文件导致数据丢失
                    isEditingMode = false;
                    break;
                }
            }
        }

        String raw = sb.toString();
        if (isIpynb) {
            fileContent = convertIpynbToPython(raw);
            fileExt = "py";
        } else {
            fileContent = raw;
        }
    }

    private String convertIpynbToPython(String jsonContent) {
        try {
            StringBuilder sb = new StringBuilder();
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(jsonContent, JsonObject.class);
            if (!root.has("cells")) return jsonContent;
            JsonArray cells = root.getAsJsonArray("cells");
            sb.append("# Jupyter Notebook Preview (Read-Only)\n\n");
            for (JsonElement cellElem : cells) {
                JsonObject cell = cellElem.getAsJsonObject();
                String cellType = cell.has("cell_type") ? cell.get("cell_type").getAsString() : "unknown";
                JsonArray source = cell.has("source") ? cell.getAsJsonArray("source") : null;
                if (source == null) continue;
                sb.append(String.format("\n# %% [%s] --------------------------------\n", cellType.toUpperCase()));
                for (JsonElement lineElem : source) {
                    String line = lineElem.getAsString();
                    if ("markdown".equals(cellType)) sb.append("# ").append(line);
                    else sb.append(line);
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) { return jsonContent; }
    }

    private void updateWebViewContent() {
        String html;
        if (isMarkdown && isRenderedMode) {
            List<org.commonmark.Extension> extensions = Arrays.asList(TablesExtension.create());
            Parser parser = Parser.builder().extensions(extensions).build();
            org.commonmark.node.Node document = parser.parse(fileContent);
            HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();
            html = buildHtmlPage(renderer.render(document), false);
        } else {
            String safeCode = fileContent.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            String bodyHtml = "<pre><code class=\"language-" + fileExt + "\">" + safeCode + "</code></pre>";
            html = buildHtmlPage(bodyHtml, true);
        }
        webView.getEngine().loadContent(html);
        if (ThemeManager.isDark()) webView.setStyle("-fx-page-fill: #2b2b2b;");
        else webView.setStyle("-fx-page-fill: white;");
    }

    private void enterEditMode() {
        isEditingMode = true;
        editorArea.setText(fileContent);
        webView.setVisible(false);
        editorArea.setVisible(true);
        updateButtonVisibility();
    }

    private void onSaveClicked() {
        if (saveFile(false)) {
            exitEditMode();
            updateWebViewContent();
        }
    }

    private boolean saveFile(boolean silent) {
        try {
            String newContent = editorArea.getText();
            Files.writeString(currentFile.toPath(), newContent, StandardCharsets.UTF_8);
            fileContent = newContent;
            if (!silent) new Alert(Alert.AlertType.INFORMATION, I18n.getBundle().getString("msg.save_success")).show();
            return true;
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, I18n.createStringBinding("msg.save_error", e.getMessage()).get()).show();
            return false;
        }
    }

    private void exitEditMode() {
        isEditingMode = false;
        editorArea.setVisible(false);
        webView.setVisible(true);
        updateButtonVisibility();
    }

    private HBox createControlBar() {
        HBox h = new HBox(10); h.setAlignment(Pos.CENTER_RIGHT); h.getStyleClass().add("midi-panel");
        btnToggleRender = new Button(); updateRenderButtonText(); btnToggleRender.getStyleClass().add("btn-blue");
        btnToggleRender.setOnAction(e->{isRenderedMode=!isRenderedMode;updateRenderButtonText();updateWebViewContent();});
        btnEdit = new Button(); btnEdit.textProperty().bind(I18n.createStringBinding("preview.btn.edit")); btnEdit.getStyleClass().add("btn-primary"); btnEdit.setOnAction(e->enterEditMode());
        btnSave = new Button(); btnSave.textProperty().bind(I18n.createStringBinding("preview.btn.save")); btnSave.getStyleClass().add("btn-primary"); btnSave.setOnAction(e->onSaveClicked());
        btnCancel = new Button(); btnCancel.textProperty().bind(I18n.createStringBinding("preview.btn.cancel")); btnCancel.getStyleClass().add("btn-danger"); btnCancel.setOnAction(e->exitEditMode());
        h.getChildren().addAll(btnToggleRender, btnEdit, btnSave, btnCancel);
        updateButtonVisibility();
        return h;
    }

    private void updateRenderButtonText() {
        String key = isRenderedMode ? "preview.btn.source" : "preview.btn.render";
        btnToggleRender.textProperty().bind(I18n.createStringBinding(key));
    }

    private void updateButtonVisibility() {
        boolean showRender = !isEditingMode && isMarkdown;
        btnToggleRender.setVisible(showRender); btnToggleRender.setManaged(showRender);
        boolean canEdit = !isEditingMode && !isIpynb;
        btnEdit.setVisible(canEdit); btnEdit.setManaged(canEdit);
        btnSave.setVisible(isEditingMode); btnSave.setManaged(isEditingMode);
        btnCancel.setVisible(isEditingMode); btnCancel.setManaged(isEditingMode);
    }

    private String buildHtmlPage(String bodyContent, boolean useHighlightJs) {
        boolean isDark = ThemeManager.isDark();
        String bgColor = isDark ? "#2b2b2b" : "#ffffff";
        String textColor = isDark ? "#d4d4d4" : "#333333";
        String borderColor = isDark ? "#555" : "#ccc";
        String tableHeaderBg = isDark ? "#333" : "#f2f2f2";

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><style>")
                .append("body { background-color: ").append(bgColor).append("; color: ").append(textColor).append("; font-family: 'Consolas', 'Menlo', monospace; margin: 0; padding: 10px; }")
                .append("h1, h2, h3 { border-bottom: 1px solid ").append(borderColor).append("; padding-bottom: 5px; }")
                .append("blockquote { border-left: 4px solid #777; padding-left: 10px; color: #888; }")
                .append("code { font-family: monospace; }")
                .append("table { border-collapse: collapse; width: 100%; margin: 10px 0; }")
                .append("th, td { border: 1px solid ").append(borderColor).append("; padding: 8px; text-align: left; }")
                .append("th { background-color: ").append(tableHeaderBg).append("; }")
                .append("</style>");

        if (useHighlightJs) {
            try {
                String cssName = isDark ? "dark.css" : "light.css";
                var cssUrlObj = getClass().getResource("/fun/vignachu/assets/highlight/" + cssName);
                var jsUrlObj = getClass().getResource("/fun/vignachu/assets/highlight/highlight.min.js");
                if (cssUrlObj != null && jsUrlObj != null) {
                    sb.append("<link rel=\"stylesheet\" href=\"").append(cssUrlObj.toExternalForm()).append("\">");
                    sb.append("<script src=\"").append(jsUrlObj.toExternalForm()).append("\"></script>");
                    sb.append("<script>hljs.highlightAll();</script>");
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        sb.append("</head><body>").append(bodyContent).append("</body></html>");
        return sb.toString();
    }

    @Override
    public void cleanup() {
        if (themeListener != null) ThemeManager.darkProperty().removeListener(themeListener);
        if (webView != null) {
            webView.getEngine().load(null);
            webView = null;
        }
        editorArea = null;
    }
}