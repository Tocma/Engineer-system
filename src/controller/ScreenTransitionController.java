package controller;

import util.LogHandler;
import util.LogHandler.LogType;
import view.ListPanel;
import view.MainFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * 画面遷移を制御するコントローラークラス
 * MainFrameを介して各画面の表示を管理し、パネル間の遷移を制御します
 *
 * <p>
 * このクラスは、アプリケーション内の画面遷移を一元管理し、以下の責務を持ちます：
 * <ul>
 * <li>各種パネル（画面）の初期化と保持</li>
 * <li>パネル間の遷移制御</li>
 * <li>パネル切り替え時のアニメーション効果（オプション）</li>
 * <li>画面状態の追跡</li>
 * <li>パネル間のデータ受け渡し</li>
 * </ul>
 * </p>
 *
 * <p>
 * 基本的な画面遷移フロー：
 * <ol>
 * <li>MainControllerから画面遷移指示を受け取る</li>
 * <li>指定されたパネルタイプに基づいて適切なパネルを選択</li>
 * <li>必要に応じてパネルを初期化または更新</li>
 * <li>MainFrameを通じてパネルを表示</li>
 * <li>画面遷移のログを記録</li>
 * </ol>
 * </p>
 *
 * <p>
 * このコントローラーは、LazyLoading方式でパネルを初期化します。
 * 各パネルは初回表示時に初期化され、それ以降はキャッシュされたインスタンスが再利用されます。
 * これにより、メモリ使用量の最適化と起動時間の短縮を実現しています。
 * </p>
 *
 * @author Nakano
 * @version 2.1.0
 * @since 2025-04-03
 */
public class ScreenTransitionController {

    /** メインフレーム */
    private final MainFrame mainFrame;

    /** 一覧パネル */
    private final ListPanel listPanel;

    /** パネルキャッシュ */
    private final Map<String, JPanel> panelCache;

    /** 遷移中フラグ */
    private final AtomicBoolean isTransitioning;

    /** 現在表示中のパネルタイプ */
    private String currentPanelType;

    /**
     * コンストラクタ
     * 初期パネルを作成し、パネルキャッシュを初期化
     *
     * @param mainFrame メインフレーム
     */
    public ScreenTransitionController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.listPanel = new ListPanel();
        this.panelCache = new HashMap<>();
        this.isTransitioning = new AtomicBoolean(false);
        this.currentPanelType = null;

        // 一覧パネルをキャッシュに追加
        panelCache.put("LIST", listPanel);

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "画面遷移コントローラーを初期化しました");
    }

    /**
     * 指定されたパネルタイプの画面を表示
     * パネルを切り替えて表示します
     *
     * @param panelType 表示するパネルタイプ
     */
    public void showPanel(String panelType) {
        // パネルタイプがnullまたは空の場合は処理しない
        if (panelType == null || panelType.trim().isEmpty()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "無効なパネルタイプが指定されました: null または空");
            return;
        }

        // 同じパネルへの遷移の場合は処理しない（ただしリフレッシュフラグがある場合は除く）
        if (panelType.equals(currentPanelType)) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "同一パネルへの遷移をスキップします: " + panelType);
            return;
        }

        // 既に遷移中の場合は処理をキューイング（本実装では簡易化のため処理をスキップ）
        if (isTransitioning.getAndSet(true)) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "遷移中のため遷移要求をスキップします: " + panelType);
            return;
        }

        try {
            // SwingのEDTで実行
            SwingUtilities.invokeLater(() -> {
                try {
                    // パネルの取得（キャッシュになければ新規作成）
                    JPanel panel = getOrCreatePanel(panelType);

                    if (panel != null) {

                        // アニメーションなしで直接表示
                        mainFrame.showPanel(panel);

                        // 現在のパネルタイプを更新
                        currentPanelType = panelType;

                        // ログ記録
                        LogHandler.getInstance().log(
                                Level.INFO, LogType.SYSTEM,
                                String.format("画面を切り替えました: %s", panelType));
                    } else {
                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "パネルの取得に失敗しました: " + panelType);
                    }
                } catch (Exception e) {
                    LogHandler.getInstance().logError(LogType.SYSTEM, "画面切り替えに失敗しました: " + panelType, e);
                } finally {
                    // 遷移中フラグを解除
                    isTransitioning.set(false);
                }
            });
        } catch (Exception e) {
            // EDT外での例外発生時の処理

            isTransitioning.set(false);
            LogHandler.getInstance().logError(LogType.SYSTEM, "画面切り替え要求の処理に失敗しました", e);
        }
    }

    /**
     * 現在のビューを更新
     * 表示中のパネルを再描画します
     */
    public void refreshView() {
        try {
            mainFrame.refreshView();
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "ビューを更新しました");
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "ビューの更新に失敗しました", e);
        }
    }

    /**
     * パネルをキャッシュから取得、または新規作成
     *
     * @param panelType パネルタイプ
     * @return 対応するJPanelインスタンス、未対応の場合はnull
     */
    private JPanel getOrCreatePanel(String panelType) {
        // キャッシュにあれば取得
        if (panelCache.containsKey(panelType)) {
            return panelCache.get(panelType);
        }

        // 現状ではLISTパネルのみ対応
        JPanel panel = null;
        switch (panelType) {
            case "LIST":
                // 既にキャッシュに存在するはずだが、念のため
                panel = listPanel;
                break;
            // 現段階ではADDとDETAILパネルは実装しない
            default:
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "未定義のパネルタイプ: " + panelType);
                return null;
        }

        // 作成したパネルをキャッシュに追加
        if (panel != null) {
            panelCache.put(panelType, panel);
        }

        return panel;
    }

    /**
     * 現在表示中のパネルを取得
     *
     * @return 現在のパネル
     */
    public JPanel getCurrentPanel() {
        return mainFrame.getCurrentPanel();
    }

    /**
     * 現在表示中のパネルタイプを取得
     *
     * @return 現在のパネルタイプ
     */
    public String getCurrentPanelType() {
        return currentPanelType;
    }

    /**
     * 登録済みパネル数を取得
     *
     * @return キャッシュされているパネルの数
     */
    public int getPanelCount() {
        return panelCache.size();
    }

    /**
     * 遷移中かどうかを確認
     *
     * @return 遷移処理中の場合はtrue
     */
    public boolean isTransitioning() {
        return isTransitioning.get();
    }
}