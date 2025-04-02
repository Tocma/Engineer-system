package controller;

import java.util.logging.Level;

import util.LogHandler;
import util.LogHandler.LogType;
import view.MainFrame;

/**
 * アプリケーションのメインコントローラー
 * 画面遷移とイベント処理を統括
 *
 * @author Nakano
 * @version 2.0.0
 * @since 2025-03-12
 */
public class MainController {

    private final ScreenTransitionController screenController;

    /**
     * コンストラクタ
     *
     * @param mainFrame メインフレーム
     */
    public MainController(MainFrame mainFrame) {
        this.screenController = new ScreenTransitionController(mainFrame);
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "メインコントローラーを初期化しました");
    }

    /**
     * アプリケーションの初期化
     */
    public void initialize() {
        try {
            screenController.showPanel("LIST");
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "アプリケーションを初期化しました");
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "アプリケーションの初期化に失敗しました", e);
        }
    }

    /**
     * イベントを処理
     *
     * @param event イベント種別
     * @param data  イベントデータ
     */
    public void handleEvent(String event, Object data) {
        try {
            switch (event) {
                case "REFRESH_VIEW" -> screenController.refreshView();
                case "CHANGE_PANEL" -> screenController.showPanel((String) data);
                default -> throw new IllegalArgumentException("未定義のイベント: " + event);
            }
            LogHandler.getInstance().log(
                    Level.INFO, LogType.SYSTEM,
                    String.format("イベントを処理しました: %s", event));
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "イベント処理に失敗しました", e);
        }
    }
}