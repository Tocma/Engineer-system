package controller;

import util.LogHandler;
import util.LogHandler.LogType;
import util.Constants.PanelType;
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
 * MainFrameを介して各画面の表示を管理し、パネル間の遷移を制御
 *
 * このコントローラーは、LazyLoading方式でパネルを初期化
 * 各パネルは初回表示時に初期化され、それ以降はキャッシュされたインスタンスが再利用されます
 * これにより、メモリ使用量の最適化と起動時間の短縮
 *
 * 画面遷移コントローラーは、MainControllerから遷移指示を受け取り、
 * 適切なパネルを選択・初期化し、MainFrameを通じて表示します。
 * 遷移操作はSwingのイベントディスパッチスレッド（EDT）上で実行され、
 * 遷移中のフラグ管理により連続遷移による問題を防止
 *
 * @author Nakano
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

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "画面遷移コントローラーを初期化完了");
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
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "メインコントローラーを設定");
    }

    /**
     * 指定されたパネルタイプの画面を表示
     * パネルを切り替えて表示
     * 削除中のフラグがある場合は、
     * 新規画面への登録ボタンを無効化する処理
     *
     * @param panelType 表示するパネルタイプ(PanelType enum)
     */
    public void showPanel(PanelType panelType) {
        // パネルタイプがnullの場合は処理しない
        if (panelType == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "無効なパネルタイプが指定されました: null");
            return;
        }

        // 現在のパネルタイプを文字列で保持しているため、比較用に変換
        String panelTypeId = panelType.getId();

        // 同じパネルへの遷移の場合は処理しない（ただしリフレッシュフラグがある場合は除く）
        if (panelTypeId.equals(currentPanelType)) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "同一パネルへの遷移をスキップ: " + panelType.getDisplayName());
            return;
        }

        // 既に遷移中の場合は処理をキューイング（本実装では簡易化のため処理をスキップ）
        if (isTransitioning.getAndSet(true)) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "遷移中のため遷移要求をスキップ: " + panelType.getDisplayName());
            return;
        }

        try {
            // SwingのEDTで実行
            SwingUtilities.invokeLater(() -> {
                try {
                    // パネルの取得（キャッシュになければ新規作成）
                    JPanel panel = getOrCreatePanel(panelTypeId);

                    // 遷移前の削除状態を取得
                    boolean wasDeleting = false;
                    JPanel previousPanel = mainFrame.getCurrentPanel();
                    if (panelType == PanelType.ADD && previousPanel instanceof ListPanel listPanel) {
                        wasDeleting = listPanel.isDeleting();
                    }

                    if (panel != null) {

                        // アニメーションなしで直接表示
                        mainFrame.showPanel(panel);

                        // ListPanel遷移後に再描画処理
                        if (panelType == PanelType.LIST && panel instanceof ListPanel listPanel) {
                            listPanel.onScreenShown();
                        }

                        // 現在のパネルタイプを更新
                        currentPanelType = panelTypeId;

                        // AddPanelに切り替えた後に、削除中だったら登録ボタンを無効化
                        if (panelType == PanelType.ADD && panel instanceof AddPanel addPanel) {
                            addPanel.setRegisterButtonEnabled(!wasDeleting);
                        }
                        // ログ記録
                        LogHandler.getInstance().log(
                                Level.INFO, LogType.SYSTEM,
                                String.format("画面を切り替えました: %s (%s)", panelType.getDisplayName(), panelTypeId));
                    } else {
                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                "パネルの取得に失敗: " + panelType.getDisplayName());
                    }
                } catch (Exception e) {
                    LogHandler.getInstance().logError(LogType.SYSTEM, "画面切り替えに失敗: " + panelType.getDisplayName(),
                            e);
                } finally {
                    // 遷移中フラグを解除
                    isTransitioning.set(false);
                }
            });
        } catch (Exception e) {
            // EDT外での例外発生時の処理
            isTransitioning.set(false);
            LogHandler.getInstance().logError(LogType.SYSTEM, "画面切り替え要求の処理に失敗", e);
        }
    }

    /**
     * 文字列IDを使用してパネルを表示（後方互換性のため）
     * 
     * @param panelTypeId パネルタイプID文字列
     */
    public void showPanel(String panelTypeId) {
        PanelType panelType = PanelType.fromId(panelTypeId);
        if (panelType != null) {
            showPanel(panelType);
        } else {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "未定義のパネルタイプID: " + panelTypeId);
        }
    }

    // ScreenTransitionControllerにメソッドを追加
    public void showPanelWithCallback(PanelType panelType, Runnable callback) {
        // パネルタイプがnullの場合は処理しない
        if (panelType == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "無効なパネルタイプが指定されました: null");
            return;
        }

        // 既に遷移中の場合は処理をキューイング
        if (isTransitioning.getAndSet(true)) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "遷移中のため遷移要求をスキップ: " + panelType.getDisplayName());
            return;
        }

        try {
            // SwingのEDTで実行
            SwingUtilities.invokeLater(() -> {
                try {
                    // パネルの取得（キャッシュになければ新規作成）
                    String panelTypeId = panelType.getId();
                    JPanel panel = getOrCreatePanel(panelTypeId);

                    if (panel != null) {
                        // アニメーションなしで直接表示
                        mainFrame.showPanel(panel);

                        // 現在のパネルタイプを更新
                        currentPanelType = panelTypeId;

                        // コールバックを実行
                        if (callback != null) {
                            callback.run();
                        }

                        // ログ記録
                        LogHandler.getInstance().log(
                                Level.INFO, LogType.SYSTEM,
                                String.format("画面を切り替えました: %s (%s)", panelType.getDisplayName(), panelTypeId));
                    } else {
                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                "パネルの取得に失敗: " + panelType.getDisplayName());
                    }
                } catch (Exception e) {
                    LogHandler.getInstance().logError(LogType.SYSTEM, "画面切り替えに失敗: " + panelType.getDisplayName(),
                            e);
                } finally {
                    // 遷移中フラグを解除
                    isTransitioning.set(false);
                }
            });
        } catch (Exception e) {
            // EDT外での例外発生時の処理
            isTransitioning.set(false);
            LogHandler.getInstance().logError(LogType.SYSTEM, "画面切り替え要求の処理に失敗", e);
        }
    }

    /**
     * 文字列IDを使用してパネルを表示
     * 
     * @param panelTypeId パネルタイプID文字列
     * @param callback    コールバック
     * 
     */

    public void showPanelWithCallback(String panelTypeId,
            Runnable callback) {
        PanelType panelType = PanelType.fromId(panelTypeId);
        if (panelType != null) {
            showPanelWithCallback(panelType, callback);
        } else {
            LogHandler.getInstance().log(Level.WARNING,
                    LogType.SYSTEM, "未定義のパネルタイプID: " + panelTypeId);
        }
    }

    /**
     * 指定されたパネルタイプに対応するパネルをキャッシュから取得
     *
     * @param panelType パネルの識別子（例: "LIST", "ADD" など）
     * @return 指定されたパネルに対応する JPanel、存在しない場合は null
     */
    public JPanel getPanelByType(String panelType) {
        return panelCache.get(panelType);
    }

    /**
     * 現在表示中のパネルが AddPanel である場合に、登録ボタンの有効／無効を切り替えます。
     *
     * @param enabled true で有効化、false で無効化
     */
    public void setRegisterButtonEnabled(boolean enabled) {
        JPanel panel = getCurrentPanel();
        if (panel instanceof AddPanel addPanel) {
            addPanel.setRegisterButtonEnabled(enabled);
        } else if (panel instanceof DetailPanel detailPanel) {
            detailPanel.setUpdateButtonEnabled(enabled);
        }
    }

    /**
     * 現在のビューを更新
     * 表示中のパネルを再描画
     */
    public void refreshView() {
        try {
            mainFrame.refreshView();
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "ビューを更新");
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "ビューの更新に失敗", e);
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
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "ディテールパネルを作成");
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

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "パネルキャッシュをクリア");
    }

    // 追加するメソッド
    /**
     * キャッシュから特定タイプのパネルを取得
     * 
     * @param panelType パネルタイプ
     * @return 対応するパネル（存在しない場合はnull）
     */
    public JPanel getPanelFromCache(String panelType) {
        if (panelCache.containsKey(panelType)) {
            return panelCache.get(panelType);
        }
        return null;
    }
}