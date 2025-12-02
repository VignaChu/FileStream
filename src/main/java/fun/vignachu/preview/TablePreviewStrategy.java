package fun.vignachu.preview;

import fun.vignachu.util.I18n;
import fun.vignachu.util.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TablePreviewStrategy implements PreviewStrategy {

    // 显示 excel表格
    @Override
    public Node createPreviewNode(File file, double width, double height) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("preview-box");

        // 1. 初始化容器与加载动画
        StackPane centerStack = new StackPane();
        // 背景色跟随主题
        String bgStyle = ThemeManager.isDark() ? "-fx-background-color: #2b2b2b;" : "-fx-background-color: #ffffff;";
        centerStack.setStyle(bgStyle);

        ProgressIndicator pi = new ProgressIndicator();
        centerStack.getChildren().add(pi);
        root.setCenter(centerStack);

        // 2. 异步加载数据
        Task<List<List<String>>> loadTask = new Task<>() {
            @Override
            protected List<List<String>> call() throws Exception {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".csv")) {
                    return readCSV(file);
                } else {
                    return readExcel(file);
                }
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<List<String>> rawData = loadTask.getValue();

            if (rawData == null || rawData.isEmpty()) {
                centerStack.getChildren().setAll(new Label("No Data / Empty File"));
                return;
            }

            // 3. 构建 TableView
            TableView<List<String>> tableView = new TableView<>();
            tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

            // 3.1 动态创建列 (根据第一行或最大列数)
            // 假设第一行数据量代表列数
            int maxCols = rawData.stream().mapToInt(List::size).max().orElse(0);

            for (int i = 0; i < maxCols; i++) {
                final int colIndex = i;
                // 列名暂定为 A, B, C... 或 Column 1, Column 2
                TableColumn<List<String>, String> column = new TableColumn<>(getColumnName(i));

                // 数据绑定逻辑：取这一行 list 的第 i 个元素
                column.setCellValueFactory(data -> {
                    List<String> row = data.getValue();
                    if (colIndex < row.size()) {
                        return new SimpleStringProperty(row.get(colIndex));
                    } else {
                        return new SimpleStringProperty("");
                    }
                });
                tableView.getColumns().add(column);
            }

            // 3.2 填充数据
            ObservableList<List<String>> items = FXCollections.observableArrayList(rawData);
            tableView.setItems(items);

            centerStack.getChildren().setAll(tableView);
        });

        loadTask.setOnFailed(e -> {
            Label errLabel = new Label();
            errLabel.textProperty().bind(I18n.createStringBinding("msg.read_error", loadTask.getException().getMessage()));
            errLabel.setStyle("-fx-text-fill: red;");
            centerStack.getChildren().setAll(errLabel);
        });

        new Thread(loadTask).start();
        return root;
    }

    // CSV 读取逻辑
    private List<List<String>> readCSV(File file) throws Exception {
        List<List<String>> data = new ArrayList<>();
        // 使用 UTF-8 读取
        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {

            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                for (String cell : record) {
                    row.add(cell);
                }
                data.add(row);
            }
        }
        return data;
    }

    // Excel 读取逻辑 (XLS & XLSX)
    private List<List<String>> readExcel(File file) throws Exception {
        List<List<String>> data = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            // 默认只读取第一个 Sheet
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(); // 用于格式化单元格（日期、数字转文本）

            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                // 获取该行最后一个单元格的索引，防止空列导致错位
                int lastCellNum = row.getLastCellNum();

                for (int i = 0; i < lastCellNum; i++) {
                    Cell cell = row.getCell(i);
                    // 使用 formatter 智能获取显示文本
                    String text = formatter.formatCellValue(cell);
                    rowData.add(text);
                }
                data.add(rowData);
            }
        }
        return data;
    }

    // 生成列名 (0->A, 1->B, ... 26->AA)
    private String getColumnName(int index) {
        StringBuilder sb = new StringBuilder();
        while (index >= 0) {
            sb.insert(0, (char) ('A' + index % 26));
            index = index / 26 - 1;
        }
        return sb.toString();
    }
}