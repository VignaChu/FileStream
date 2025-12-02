package fun.vignachu;

import fun.vignachu.model.FileOrganizerModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class FileStreamApp extends Application {

    // 程序启动类
    @Override
    public void start(Stage stage) {
        try {
            URL fxmlUrl = getClass().getResource("/fun/vignachu/view/main-view.fxml");

            if (fxmlUrl == null) {
                throw new RuntimeException("❌ 找不到 main-view.fxml！请检查路径。");
            }

            FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(fxmlLoader.load(), 1280, 800);

            // 加载 CSS
            URL cssUrl = getClass().getResource("/fun/vignachu/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setTitle("FileStream Ultimate");
            try {
                URL iconUrl = getClass().getResource("/fun/vignachu/images/icon.png");
                if (iconUrl != null) stage.getIcons().add(new Image(iconUrl.toExternalForm()));
            } catch (Exception ignored) {}

            stage.setOnCloseRequest(e -> {
                System.out.println("正在关闭程序...");

                // 1. 执行清理任务
                FileOrganizerModel.cleanUp();

                // 2. 退出 JavaFX
                Platform.exit();

                // 3. 强制杀死 JVM (确保 MIDI 线程也停止)
                System.exit(0);
            });

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}