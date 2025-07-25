package controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import util.LogHandler;
import util.LogHandler.LogType;
import util.Constants.PanelType;
import view.AddPanel;
import view.DetailPanel;
import view.ListPanel;
import view.MainFrame;

/**
 * 画面遷移を制御するコントローラークラス
 * MainFrameを介して各画面の表示を管理し、パネル間の遷移を制御
 *
 * このコントローラーは、アプリ起動時に全パネルを初期化します
 * 全てのパネルは起動時に作成・初期化され、メモリ上にキャッシュされます
 * これにより、画面遷移時の初期化処理が不要となり、高速な画面切り替えが可能になります
 *
 * 画面遷移コントローラーは、MainControllerから遷移指示を受け取り、
 * 適切なパネルを選択し、MainFrameを通じて表示します。
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
    private final DetailPanel detailPanel;

    /** 新規追加パネル */
    private final AddPanel addPanel;

    /** パネルキャッシュ */
    private final Map<String, JPanel> panelCache;

    /** 遷移中フラグ */
    private final AtomicBoolean isTransitioning;

    /** 現在表示中のパネルタイプ */
    private String currentPanelType;

    /**
     * コンストラクタ
     * 全パネルを初期化し、パネルキャッシュに登録
     *
     * @param mainFrame メインフレーム
     */
    public ScreenTransitionController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.panelCache = new HashMap<>();
        this.isTransitioning = new AtomicBoolean(false);
        this.currentPanelType = null;

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "画面遷移コントローラーの初期化を開始");

        // 全パネルを起動時に初期化
        // 一覧パネルの初期化
        this.listPanel = new ListPanel();
        listPanel.initialize();
        panelCache.put("LIST", listPanel);
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "リストパネルを初期化完了");

        // 詳細パネルの初期化
        this.detailPanel = new DetailPanel();
        detailPanel.initialize();
        panelCache.put("DETAIL", detailPanel);
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "ディテールパネルを初期化完了");

        // 新規追加パネルの初期化
        this.addPanel = new AddPanel();
        addPanel.initialize();
        panelCache.put("ADD", addPanel);
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "追加パネルを初期化完了");

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "画面遷移コントローラーを初期化完了 - 全パネル数: " + panelCache.size());
    }

    /**
     * メインコントローラーを設定
     * パネルとコントローラー間の連携を可能にする
     *
     * @param mainController メインコントローラー
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;

        // 全パネルにコントローラーを設定
        if (listPanel != null) {
            listPanel.setMainController(mainController);
        }
        if (detailPanel != null) {
            detailPanel.setMainController(mainController);
        }
        if (addPanel != null) {
            addPanel.setMainController(mainController);
        }

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "全パネルにメインコントローラーを設定");
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
        if (isTransitioning.get()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "遷移中のため遷移要求をスキップ: " + panelType.getDisplayName());
            return;
        }

        isTransitioning.set(true);
        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    JPanel panel = getPanel(panelTypeId);
                    if (panel != null) {
                        // AddPanelが表示される際にフィールドをクリア
                        if ("ADD".equals(panelType.getId()) && panel instanceof AddPanel) {
                            ((AddPanel) panel).clearFields();
                        }

                        mainFrame.showPanel(panel);
                        currentPanelType = panelType.getId();
                        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                "パネル表示完了: " + panelType);
                    } else {
                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                "パネルの作成に失敗: " + panelType);
                    }
                } finally {
                    isTransitioning.set(false);
                }
            });
        } catch (Exception _e) {
            isTransitioning.set(false);
            LogHandler.getInstance().logError(LogType.SYSTEM, "パネル表示中にエラー", _e);
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
                    // パネルの取得（キャッシュから）
                    String panelTypeId = panelType.getId();
                    JPanel panel = getPanel(panelTypeId);

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
                } catch (Exception _e) {
                    LogHandler.getInstance().logError(LogType.SYSTEM, "画面切り替えに失敗: " + panelType.getDisplayName(),
                            _e);
                } finally {
                    // 遷移中フラグを解除
                    isTransitioning.set(false);
                }
            });
        } catch (Exception _e) {
            // EDT外での例外発生時の処理
            isTransitioning.set(false);
            LogHandler.getInstance().logError(LogType.SYSTEM, "画面切り替え要求の処理に失敗", _e);
        }
    }

    /**
     * 文字列IDを使用してパネルを表示
     * 
     * @param panelTypeId パネルタイプID文字列
     * @param callback    コールバック
     */
    public void showPanelWithCallback(String panelTypeId, Runnable callback) {
        PanelType panelType = PanelType.fromId(panelTypeId);
        if (panelType != null) {
            showPanelWithCallback(panelType, callback);
        } else {
            LogHandler.getInstance().log(Level.WARNING,
                    LogType.SYSTEM, "未定義のパネルタイプID: " + panelTypeId);
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
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "ビューの更新に失敗", _e);
        }
    }

    /**
     * パネルをキャッシュから取得
     * 事前初期化されたパネルを返す
     *
     * @param panelType パネルタイプ("LIST", "DETAIL", "ADD")
     * @return 対応するJPanelインスタンス、未対応の場合はnull
     */
    private JPanel getPanel(String panelType) {
        // キャッシュから取得
        JPanel panel = panelCache.get(panelType);

        if (panel == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "パネルがキャッシュに存在しません: " + panelType);
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
        // 現在表示中のパネルを処理
        JPanel currentPanel = getCurrentPanel();
        if (currentPanel instanceof AddPanel addPanel) {
            addPanel.setRegisterButtonEnabled(enabled);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "現在表示中のAddPanelのボタンを" + (enabled ? "有効化" : "無効化"));
        } else if (currentPanel instanceof DetailPanel detailPanel) {
            detailPanel.setUpdateButtonEnabled(enabled);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "現在表示中のDetailPanelのボタンを" + (enabled ? "有効化" : "無効化"));
        }

        // キャッシュ内の全パネルも処理（事前初期化版の場合）
        // AddPanelの処理
        if (addPanel != null && addPanel != currentPanel) {
            addPanel.setRegisterButtonEnabled(enabled);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "キャッシュ内のAddPanelのボタンを" + (enabled ? "有効化" : "無効化"));
        }

        // DetailPanelの処理
        if (detailPanel != null && detailPanel != currentPanel) {
            detailPanel.setUpdateButtonEnabled(enabled);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "キャッシュ内のDetailPanelのボタンを" + (enabled ? "有効化" : "無効化"));
        }

        // 遅延初期化版の場合のキャッシュチェック
        JPanel cachedAddPanel = panelCache.get("ADD");
        if (cachedAddPanel instanceof AddPanel && cachedAddPanel != currentPanel) {
            ((AddPanel) cachedAddPanel).setRegisterButtonEnabled(enabled);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "パネルキャッシュ内のAddPanelのボタンを" + (enabled ? "有効化" : "無効化"));
        }

        JPanel cachedDetailPanel = panelCache.get("DETAIL");
        if (cachedDetailPanel instanceof DetailPanel && cachedDetailPanel != currentPanel) {
            ((DetailPanel) cachedDetailPanel).setUpdateButtonEnabled(enabled);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "パネルキャッシュ内のDetailPanelのボタンを" + (enabled ? "有効化" : "無効化"));
        }
    }

    public void ensureAllPanelsInitialized() {
        String[] panelTypes = { "LIST", "DETAIL", "ADD" };
        for (String type : panelTypes) {
            if (!panelCache.containsKey(type)) {
                getPanel(type); // パネルを強制作成
            }
        }
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "全パネルの初期化を確認完了");
    }

    /**
     * すべての関連パネル（詳細・追加）の処理中状態を一括で設定します。
     * CSV処理中など、システム全体でUI操作を制限する場合に使用します。
     *
     * @param processing trueの場合、処理中状態に設定
     */
    public void setPanelsProcessing(boolean processing) {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "全パネルの処理中状態を設定: " + processing);

        // DetailPanelの状態を設定 (戻るボタンは有効のまま)
        if (detailPanel != null) {
            detailPanel.setProcessing(processing, false);
        }

        // AddPanelの状態を設定 (戻るボタンは有効のまま)
        if (addPanel != null) {
            addPanel.setProcessing(processing, false);
        }
    }
}