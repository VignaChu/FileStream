package fun.vignachu.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;

public class ThemeManager {
    private static final String DARK_CLASS = "theme-dark";
    private static final String LIGHT_CLASS = "theme-light";

    // 【核心修改】改为 Property 对象
    private static final BooleanProperty darkProperty = new SimpleBooleanProperty(true);

    public static void initTheme(Parent root) {
        applyTheme(root);

        // 添加监听器：当属性变化时，自动更新根节点的样式类
        darkProperty.addListener((obs, oldVal, isDark) -> applyTheme(root));
    }

    public static void toggleTheme(Parent root) {
        // 取反当前状态，这会自动触发上面的监听器
        darkProperty.set(!darkProperty.get());
    }

    // 私有方法：应用样式
    private static void applyTheme(Parent root) {
        root.getStyleClass().removeAll(DARK_CLASS, LIGHT_CLASS);
        root.getStyleClass().add(darkProperty.get() ? DARK_CLASS : LIGHT_CLASS);
    }

    public static boolean isDark() {
        return darkProperty.get();
    }

    // 【新增】暴露属性供外部监听
    public static BooleanProperty darkProperty() {
        return darkProperty;
    }
}