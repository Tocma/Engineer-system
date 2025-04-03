package controller;

import model.EngineerDTO;
import util.LogHandler;
import util.LogHandler.LogType;
import util.MessageEnum;
import util.ResourceManager;
import view.ListPanel;
import view.MainFrame;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import javax.swing.JPanel;

/**
 * アプリケーションのメインコントローラー
 * 画面遷移、イベント処理、スレッド管理を統括
 *
 * <p>
 * このクラスは、エンジニア人材管理システムの中心的なコントローラーとして機能し、
 * ビュー（画面）とモデル（データ）を連携させる役割を担います。主な責務は以下の通りです：
 * <ul>
 * <li>画面遷移の制御</li>
 * <li>UIイベントの処理</li>
 * <li>非同期処理とスレッド管理</li>
 * <li>アプリケーション終了処理の調整</li>
 * <li>モデル操作の統合管理</li>
 * </ul>
 * </p>
 *
 * <p>
 * このコントローラーはMVCアーキテクチャにおけるControllerの役割を果たし、
 * ユーザーインターフェースとビジネスロジックの分離を実現します。
 * また、アプリケーション全体の状態を管理し、複数の子コントローラーを調整します。
 * </p>
 *
 * <p>
 * スレッド管理機能により、バックグラウンドタスクの実行状態を監視し、
 * アプリケーション終了時には実行中のすべての処理が適切に終了するよう制御します。
 * これにより、データの整合性を保ちながら安全な終了を実現します。
 * </p>
 *
 * @author Nakano
 * @version 2.0.0
 * @since 2025-03-22
 */
public class MainController {

    /** 画面遷移コントローラー */
    private final ScreenTransitionController screenController;

    /** エンジニアデータコントローラー */
    private EngineerController engineerController;

    /** リソースマネージャー */
    private ResourceManager resourceManager;

    /** メインフレーム */
    private final MainFrame mainFrame;

    /** 実行中非同期タスクの追跡マップ */
    private final ConcurrentMap<String, Thread> runningTasks;

    /** シャットダウン中フラグ */
    private final AtomicBoolean isShuttingDown;

    /** 未保存変更フラグ */
    private boolean hasUnsavedChanges;

    /**
     * コンストラクタ
     * 必要な初期化を行いますが、完全な初期化はinitializeメソッドで行います
     *
     * @param mainFrame メインフレーム
     */
    public MainController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.screenController = new ScreenTransitionController(mainFrame);
        this.runningTasks = new ConcurrentHashMap<>();
        this.isShuttingDown = new AtomicBoolean(false);
        this.hasUnsavedChanges = false;

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "メインコントローラーを初期化しました");
    }

    /**
     * アプリケーションの初期化
     * すべてのコンポーネントを初期化し、初期画面を表示します
     */
    public void initialize() {
        try {
            // リソースマネージャーの取得
            resourceManager = new ResourceManager();

            // エンジニアコントローラーの初期化
            engineerController = new EngineerController();

            // 初期画面の表示
            screenController.showPanel("LIST");

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "アプリケーションを初期化しました");

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "アプリケーションの初期化に失敗しました", e);
            handleFatalError(e);
        }
    }

    /**
     * イベントを処理
     * アプリケーション全体のイベントをディスパッチします
     *
     * @param event イベント種別
     * @param data  イベントデータ
     */
    public void handleEvent(String event, Object data) {
        try {
            // シャットダウン中は新しいイベントを処理しない
            if (isShuttingDown.get()) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "シャットダウン中のためイベントを無視します: " + event);
                return;
            }

            switch (event) {
                case "REFRESH_VIEW":
                    screenController.refreshView();
                    break;
                case "CHANGE_PANEL":
                    screenController.showPanel((String) data);
                    break;
                case "SAVE_DATA":
                    handleSaveData(data);
                    break;
                case "LOAD_DATA":
                    handleLoadData();
                    break;
                case "SHUTDOWN":
                    initiateShutdown();
                    break;
                default:
                    handleUnknownEvent(event, data);
                    break;
            }

            LogHandler.getInstance().log(
                    Level.INFO, LogType.SYSTEM,
                    String.format("イベントを処理しました: %s", event));

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "イベント処理に失敗しました: " + event, e);
            handleError(e);
        }
    }

    /**
     * 非同期タスクを開始
     * タスクをバックグラウンドで実行し、完了時のコールバックを設定します
     *
     * @param taskId タスク識別子
     * @param task   実行するRunnable
     */
    public void startAsyncTask(String taskId, Runnable task) {
        // シャットダウン中は新しいタスクを開始しない
        if (isShuttingDown.get()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "シャットダウン中のため非同期タスクを開始できません: " + taskId);
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "非同期タスクを開始: " + taskId);
                task.run();
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "非同期タスクが完了: " + taskId);
            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "非同期タスクの実行中にエラーが発生: " + taskId, e);
            } finally {
                runningTasks.remove(taskId);
            }
        }, "AsyncTask-" + taskId);

        // 実行中タスクマップに追加
        runningTasks.put(taskId, thread);

        // スレッドを登録して開始
        mainFrame.registerThread(thread);
        thread.start();
    }

    /**
     * データ保存処理
     * 
     * @param data 保存するデータ
     */
    private void handleSaveData(Object data) {
        if (data instanceof List<?>) {
            try {
                @SuppressWarnings("unchecked")
                List<EngineerDTO> engineers = (List<EngineerDTO>) data;

                // 非同期タスクとして保存処理を実行
                startAsyncTask("SaveData", () -> {
                    try {
                        engineerController.saveEngineers(engineers);
                        hasUnsavedChanges = false;

                        // UI更新はSwingのEDTで実行
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            screenController.refreshView();
                        });

                    } catch (Exception e) {
                        LogHandler.getInstance().logError(LogType.SYSTEM, "データ保存に失敗しました", e);
                    }
                });

            } catch (ClassCastException e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "保存データの型が不正です", e);
            }
        }
    }

    /**
     * データ読み込み処理
     */
    private void handleLoadData() {
        // 非同期タスクとして読み込み処理を実行
        startAsyncTask("LoadData", () -> {
            try {
                List<EngineerDTO> engineers = engineerController.loadEngineers();

                // UI更新はSwingのEDTで実行
                javax.swing.SwingUtilities.invokeLater(() -> {
                    // ListPanelを取得してエンジニアデータを設定
                    JPanel currentPanel = screenController.getCurrentPanel();
                    if (currentPanel instanceof ListPanel) {
                        ((ListPanel) currentPanel).setEngineerData(engineers);
                    }
                    screenController.refreshView();
                });

            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "データ読み込みに失敗しました", e);
            }
        });
    }

    /**
     * 未知のイベント処理
     * 
     * @param event イベント種別
     * @param data  イベントデータ
     */
    private void handleUnknownEvent(String event, Object data) {
        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "未定義のイベントを検出: " + event);
    }

    /**
     * エラー処理
     * 
     * @param e 発生した例外
     */
    private void handleError(Exception e) {
        // 通常のエラー処理
        LogHandler.getInstance().logError(LogType.SYSTEM, "エラーが発生しました", e);
    }

    /**
     * 致命的なエラー処理
     * アプリケーションを安全に終了します
     * 
     * @param e 発生した例外
     */
    private void handleFatalError(Exception e) {
        LogHandler.getInstance().logError(LogType.SYSTEM, "致命的なエラー", e);
        initiateShutdown();
    }

    /**
     * シャットダウン処理の開始
     * アプリケーションの安全な終了処理を開始します
     */
    public void initiateShutdown() {
        // 既にシャットダウン中なら処理しない
        if (isShuttingDown.getAndSet(true)) {
            return;
        }

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "アプリケーションのシャットダウンを開始します");

        // 未保存データがあれば保存処理
        handleUnsavedChanges();

        // 実行中のタスクをすべて終了
        terminateRunningTasks();

        // メインフレームに終了を通知
        mainFrame.performShutdown();
    }

    /**
     * 未保存データの処理
     */
    private void handleUnsavedChanges() {
        if (hasUnsavedChanges) {
            try {
                // 最終的なデータ保存処理
                engineerController.saveCurrentState();
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "終了前の未保存データを保存しました");
            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "終了前のデータ保存に失敗しました", e);
            }
        }
    }

    /**
     * 実行中のタスクを終了
     */
    private void terminateRunningTasks() {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "実行中の非同期タスクを終了します: " + runningTasks.size() + "件");

        // すべての実行中タスクを中断
        for (Thread thread : runningTasks.values()) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }

        // マップをクリア
        runningTasks.clear();
    }

    /**
     * 実行中タスク数を取得
     * 
     * @return 実行中のタスク数
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }

    /**
     * 未保存変更状態を設定
     * 
     * @param hasChanges 未保存変更がある場合はtrue
     */
    public void setUnsavedChanges(boolean hasChanges) {
        this.hasUnsavedChanges = hasChanges;
    }

    /**
     * 未保存変更の有無を確認
     * 
     * @return 未保存変更がある場合はtrue
     */
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    /**
     * シャットダウン状態の確認
     * 
     * @return シャットダウン中の場合はtrue
     */
    public boolean isShuttingDown() {
        return isShuttingDown.get();
    }
}