package view;

import util.LogHandler;
import util.LogHandler.LogType;
import javax.swing.*;
import controller.MainController;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import util.Constants.SystemConstants;

/**
 * アプリケーションのメインウィンドウを管理するクラス
 * パネルの切り替えとUI関連リソース管理を担当
 * 
 * 注意: 終了処理の制御権はMainControllerに移譲し、
 * このクラスは純粋にUI関連のリソース解放のみを担当
 *
 * @author Nakano
 */
public class MainFrame extends AbstractFrame {

    /** 現在表示中のパネル */
    private JPanel currentPanel;

    /** メインコンテンツを配置するパネル */
    private final JPanel contentPanel;

    /** メインコントローラー */
    private MainController mainController;

    /** ワーカースレッドを管理するExecutorService */
    private final ExecutorService executor;

    /** 終了処理登録済みのスレッド一覧 */
    private final List<Thread> managedThreads;

    /** リストパネル */
    private ListPanel listPanel;

    /**
     * コンストラクタ
     * メインフレームとスレッドプールを初期化
     */
    public MainFrame() {
        super();
        this.contentPanel = new JPanel(new BorderLayout());
        frame.add(contentPanel);

        // スレッド管理用の初期化
        this.executor = Executors.newFixedThreadPool(SystemConstants.WORKER_THREAD_POOL_SIZE);
        this.managedThreads = new ArrayList<>();

        // リストパネルの初期化
        this.listPanel = new ListPanel();

        // ウィンドウ終了イベントのハンドリング
        setupWindowCloseHandler();
    }

    public ListPanel getListPanel() {
        return listPanel;
    }

    @Override
    protected void customizeFrame() {
        frame.setTitle("エンジニア人材管理");
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "メインフレーム初期化完了");
    }

    /**
     * ウィンドウ終了時の処理を設定
     * MainControllerに終了処理を完全に委譲
     */
    private void setupWindowCloseHandler() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 単純にMainControllerに終了処理を委譲するだけ
                if (mainController != null) {
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "ウィンドウ終了イベントをメインコントローラーに委譲します");
                    mainController.initiateShutdown();
                } else {
                    LogHandler.getInstance().log(Level.SEVERE, LogType.SYSTEM,
                            "メインコントローラーが設定されていないため、強制終了します");
                    System.exit(1);
                }
            }
        });
    }

    /**
     * UI関連リソースの解放（MainControllerから呼び出される）
     * ExecutorServiceとスレッドの安全な終了のみを担当
     * 
     * 重要: このメソッドはMainControllerの制御下で実行され、
     * 終了処理の一部分のみを担当
     */
    public void releaseUIResources() {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "UI関連リソースの解放を開始");

        try {
            // ExecutorServiceを安全に終了
            shutdownExecutorService();

            // 登録済みスレッドを安全に終了
            shutdownManagedThreads();

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "UI関連リソースの解放が完了");

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "UI関連リソース解放中にエラーが発生", e);
            // エラーがあってもMainControllerに制御を返す
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "メインフレームにメインコントローラーへの参照を設定");
    }

    /**
     * ExecutorServiceを安全に終了
     */
    private void shutdownExecutorService() {
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "ExecutorServiceを終了しています");

            executor.shutdown();
            boolean terminated = executor.awaitTermination(SystemConstants.THREAD_TERMINATION_TIMEOUT,
                    TimeUnit.MILLISECONDS);

            if (!terminated) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "ExecutorServiceのタスクが時間内に終了しないため、強制終了します");
                executor.shutdownNow();
            } else {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "ExecutorServiceが正常に終了");
            }
        } catch (InterruptedException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "ExecutorServiceの終了中に割り込みが発生", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 登録済みスレッドを安全に終了
     */
    private void shutdownManagedThreads() {
        try {
            if (managedThreads.isEmpty()) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "管理対象のスレッドはありません");
                return;
            }

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "登録済みスレッドを終了しています: " + managedThreads.size() + "件");

            // 全スレッドに割り込みを送信
            for (Thread thread : managedThreads) {
                if (thread.isAlive()) {
                    thread.interrupt();
                }
            }

            // 全スレッドの終了を待機
            for (Thread thread : managedThreads) {
                if (thread.isAlive()) {
                    thread.join(SystemConstants.THREAD_TERMINATION_TIMEOUT);
                    if (thread.isAlive()) {
                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                "スレッド '" + thread.getName() + "' が時間内に終了しませんでした");
                    }
                }
            }

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "管理対象スレッドの終了処理が完了");

        } catch (InterruptedException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "スレッド終了待機中に割り込みが発生", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * パネルを切り替えて表示
     */
    public void showPanel(JPanel panel) {
        if (panel == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "表示するパネルがnullです");
            return;
        }

        if (currentPanel != null) {
            contentPanel.remove(currentPanel);
        }

        currentPanel = panel;
        contentPanel.add(currentPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                String.format("パネルを切り替えました: %s", panel.getClass().getSimpleName()));
    }

    /**
     * 現在のビューを更新
     */
    public void refreshView() {
        contentPanel.revalidate();
        contentPanel.repaint();
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "ビューを更新");
    }

    /**
     * バックグラウンドタスクを実行
     */
    public void executeTask(Runnable task) {
        executor.execute(task);
    }

    /**
     * スレッドを管理対象に登録
     */
    public void registerThread(Thread thread) {
        if (thread != null) {
            managedThreads.add(thread);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "スレッド '" + thread.getName() + "' を管理対象に登録");
        }
    }

    /**
     * 管理対象からスレッドを削除
     */
    public boolean unregisterThread(Thread thread) {
        boolean removed = managedThreads.remove(thread);
        if (removed) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "スレッド '" + thread.getName() + "' を管理対象から削除");
        }
        return removed;
    }

    /**
     * 現在表示中のパネルを取得
     */
    public JPanel getCurrentPanel() {
        return currentPanel;
    }

    /**
     * JFrameを取得
     */
    public JFrame getJFrame() {
        return frame;
    }

    /**
     * 登録済みスレッド数を取得
     */
    public int getManagedThreadCount() {
        return managedThreads.size();
    }
}