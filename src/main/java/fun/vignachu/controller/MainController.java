package fun.vignachu.controller;

import fun.vignachu.util.I18n;
import fun.vignachu.util.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.Locale;

public class MainController {

    // 本类为主框架的实现逻辑
    @FXML private VBox rootContainer;
    @FXML private Label lblTitle;
    @FXML private MenuButton btnLang;
    @FXML private Button btnTheme;
    @FXML private TabPane mainTabPane;
    @FXML private Tab tabOrganizer, tabMidi, tabAbout;
    @FXML private MidiController midiViewController;
    @FXML private Tab tabExtension;
    @FXML private Label lblAboutTitle, lblAboutDesc;

    @FXML
    public void initialize() {
        ThemeManager.initTheme(rootContainer);
        bindI18n();
        updateThemeBtn();

        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (oldV == tabMidi && midiViewController != null) {
                midiViewController.pauseMidi();
            }
        });
    }

    private void bindI18n() {
        lblTitle.textProperty().bind(I18n.createStringBinding("app.title"));
        btnLang.textProperty().bind(I18n.createStringBinding("btn.lang"));

        tabOrganizer.textProperty().bind(I18n.createStringBinding("tab.organizer"));
        tabMidi.textProperty().bind(I18n.createStringBinding("tab.midi"));
        tabAbout.textProperty().bind(I18n.createStringBinding("tab.about"));

        if (lblAboutTitle != null) lblAboutTitle.textProperty().bind(I18n.createStringBinding("about.title"));
        if(tabExtension!=null) tabExtension.textProperty().bind(I18n.createStringBinding("tab.extension"));
        if (lblAboutDesc != null) lblAboutDesc.textProperty().bind(I18n.createStringBinding("about.desc"));
    }

    @FXML void onToggleTheme() {
        ThemeManager.toggleTheme(rootContainer);
        updateThemeBtn();
    }

    private void updateThemeBtn() {
        String key = ThemeManager.isDark() ? "theme.to_light" : "theme.to_dark";
        btnTheme.textProperty().bind(I18n.createStringBinding(key));
    }

    @FXML void onLangEn() { I18n.setLocale(Locale.US); }
    @FXML void onLangZh() { I18n.setLocale(Locale.SIMPLIFIED_CHINESE); }
}