package controller;

import util.LogHandler;
import util.LogHandler.LogType;
import view.ListPanel;
import view.AddPanel;
import view.DetailPanel;
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
 * このクラスは、エンジニア人材管理システムのMVCアーキテクチャにおいて、
 * 複数のビュー（画面）間の遷移を一元管理する役割を持ちます。具体的には、
 * MainFrameインスタンスを介して各種パネルの切り替えを行い、パネルの生成と
 * キャッシュの管理、トランジション効果の適用、画面状態の追跡などを担当します。
 * </p>
 *
 * <p>
 * 主な責務：
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
 * このコントローラーは、LazyLoading方式でパネルを初期化します。
 * 各パネルは初回表示時に初期化され、それ以降はキャッシュされたインスタンスが再利用されます。
 * これにより、メモリ使用量の最適化と起動時間の短縮を実現しています。
 * </p>
 *
 * <p>
 * 画面遷移コントローラーは、MainControllerから遷移指示を受け取り、
 * 適切なパネルを選択・初期化し、MainFrameを通じて表示します。
 * 遷移操作はSwingのイベントディスパッチスレッド（EDT）上で実行され、
 * 遷移中のフラグ管理により連続遷移による問題を防止します。
 * </p>
 *
 * <p>
 * 対応している画面パネル：
 * <ul>
 * <li>LIST - エンジニア一覧画面</li>
 * <li>DETAIL - エンジニア詳細画面</li>
 * <li>ADD - エンジニア新規追加画面</li>
 * </ul>
 * </p>
 * 
 * <p>
 * 使用例：
 * 
 * <pre>
 * // 一覧画面への遷移
 * screenTransitionController.showPanel("LIST");
 * 
 * // 詳細画面への遷移
 * screenTransitionController.showPanel("DETAIL");
 * 
 * // 新規追加画面への遷移
 * screenTransitionController.showPanel("ADD");
 * </pre>
 * </p>
 *
 * @author Nakano
 * @version 4.3.0
 * @since 2025-05-02
 */
public class ScreenTransitionController {

    /** メインフレーム */
    private final MainFrame mainFrame;

    /** メインコントローラー参照 */
    private MainController mainController;

    /** 一覧パネル */
    private final ListPanel listPanel;

    /** 詳細パネル */
    // private DetailPanel detailPanel;

    /** 新規追加パネル */
    private AddPanel addPanel;

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
     * メインコントローラーを設定
     * パネルとコントローラー間の連携を可能にする
     *
     * @param mainController メインコントローラー
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;

        // すでに作成済みのパネルにコントローラーを設定
        if (addPanel != null) {
            addPanel.setMainController(mainController);
        }
        if (listPanel != null) {
            listPanel.setMainController(mainController);
        }

        // 詳細パネルも同様に設定（実装時に追加）
    }

    /**
     * 指定されたパネルタイプの画面を表示
     * パネルを切り替えて表示します
     *
     * @param panelType 表示するパネルタイプ("LIST", "DETAIL", "ADD")
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

    // ScreenTransitionControllerにメソッドを追加
    public void showPanelWithCallback(String panelType, Runnable callback) {
        // パネルタイプがnullまたは空の場合は処理しない
        if (panelType == null || panelType.trim().isEmpty()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "無効なパネルタイプが指定されました: null または空");
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

                        // コールバックを実行
                        if (callback != null) {
                            callback.run();
                        }

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
     * 遅延初期化（Lazy Initialization）パターンでパネルを管理
     *
     * @param panelType パネルタイプ("LIST", "DETAIL", "ADD")
     * @return 対応するJPanelインスタンス、未対応の場合はnull
     */
    private JPanel getOrCreatePanel(String panelType) {
        // キャッシュにあれば取得
        if (panelCache.containsKey(panelType)) {
            return panelCache.get(panelType);
        }

        // キャッシュになければ新規作成
        JPanel panel = null;
        switch (panelType) {
            case "LIST":
                // 一覧パネル（すでにキャッシュにあるはずだが、念のため）
                panel = listPanel;
                break;

            case "DETAIL":
                /// 詳細パネル
                DetailPanel detailPanel = new DetailPanel();
                // コントローラーの設定
                if (mainController != null) {
                    detailPanel.setMainController(mainController);
                }
                // パネルの初期化
                detailPanel.initialize();
                panel = detailPanel;
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "DetailPanelを作成しました");
                break;

            case "ADD":
                // 新規追加パネル
                if (addPanel == null) {
                    addPanel = new AddPanel();
                    // コントローラーの設定
                    if (mainController != null) {
                        addPanel.setMainController(mainController);
                    }
                    // パネルの初期化
                    addPanel.initialize();
                }
                panel = addPanel;
                break;

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
     * @return 現在のパネルタイプ("LIST", "DETAIL", "ADD"など)
     */
    public String getCurrentPanelType() {
        return currentPanelType;
    }

    /**
     * 登録済みパネル数を取得
     * テスト・デバッグ用
     *
     * @return キャッシュされているパネルの数
     */
    public int getPanelCount() {
        return panelCache.size();
    }

    /**
     * 遷移中かどうかを確認
     * テスト・デバッグ用
     *
     * @return 遷移処理中の場合はtrue
     */
    public boolean isTransitioning() {
        return isTransitioning.get();
    }

    /**
     * 特定タイプのパネルがキャッシュに存在するか確認
     * 
     * @param panelType 確認するパネルタイプ
     * @return 存在する場合はtrue
     */
    public boolean hasPanelInCache(String panelType) {
        return panelCache.containsKey(panelType);
    }

    /**
     * パネルキャッシュをクリア
     * システムメモリ解放などのために使用
     * ただし、現在表示中のパネルはクリアしない
     */
    public void clearPanelCache() {
        // 現在表示中のパネルを保持
        JPanel currentPanel = panelCache.get(currentPanelType);

        // キャッシュをクリア
        panelCache.clear();

        // 現在表示中のパネルを再登録
        if (currentPanel != null && currentPanelType != null) {
            panelCache.put(currentPanelType, currentPanel);
        }

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "パネルキャッシュをクリアしました");
    }
}