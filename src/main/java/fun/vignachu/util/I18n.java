package fun.vignachu.util;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {
    // 默认简体中文
    private static final ObjectProperty<Locale> locale = new SimpleObjectProperty<>(Locale.SIMPLIFIED_CHINESE);

    public static void setLocale(Locale newLocale) {
        locale.set(newLocale);
    }

    public static Locale getLocale() {
        return locale.get();
    }

    public static ResourceBundle getBundle() {
        return ResourceBundle.getBundle("fun.vignachu.i18n.ui", locale.get());
    }

    /**
     * 创建支持动态参数的 StringBinding
     * @param key 资源文件中的键
     * @param args 动态参数 (对应资源文件中的 {0}, {1}...)
     */
    public static StringBinding createStringBinding(String key, Object... args) {
        return Bindings.createStringBinding(() -> {
            ResourceBundle bundle = getBundle();
            try {
                String value = bundle.getString(key);
                // 如果有参数，使用 MessageFormat 进行格式化
                if (args != null && args.length > 0) {
                    return MessageFormat.format(value, args);
                }
                return value;
            } catch (Exception e) {
                // 找不到键时返回键名，方便调试
                return key;
            }
        }, locale); // 绑定 locale 属性，语言切换时自动重新计算
    }
}