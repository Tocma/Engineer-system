package view;

import util.LogHandler;
import util.LogHandler.LogType;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * アプリケーションのメインウィンドウを管理するクラス
 * パネルの切り替えとスレッド管理を担当
 *
 * <p>
 * このクラスは、エンジニア人材管理システムのメインウィンドウとして機能し、以下の責務を持ちます：
 * <ul>
 * <li>メインウィンドウのUIコンポーネントの初期化と管理</li>
 * <li>アプリケーション中の様々なパネルの切り替え</li>
 * <li>ワーカースレッドプールの管理</li>
 * <li>アプリケーション終了時のスレッド安全な終了処理</li>
 * <li>メニューバーの管理</li>
 * </ul>
 * </p>
 *
 * <p>
 * スレッド管理機能は、バックグラウンドタスクを処理するためのExecutorServiceを提供し、
 * アプリケーション終了時にはすべてのスレッドが安全に終了するよう制御します。これにより、
 * リソースリークやデータ不整合を防止します。
 * </p>
 *
 * <p>
 * ウィンドウ終了時の処理：
 * <ol>
 * <li>実行中のすべてのスレッドに終了を要求</li>
 * <li>設定された待機時間内にスレッドの終了を待機</li>
 * <li>終了しないスレッドがある場合は強制終了</li>
 * <li>アプリケーションリソースの解放</li>
 * </ol>
 * </p>
 *
 * @author Nakano
 * @version 4.0.0
 * @since 2025-04-15
 */
public class MainFrame extends AbstractFrame {

    /** 現在表示中のパネル */
    private JPanel currentPanel;

    /** メインコンテンツを配置するパネル */
    private final JPanel contentPanel;

    /** ワーカースレッドを管理するExecutorService */
    private final ExecutorService executor;

    /** 終了処理登録済みのスレッド一覧 */
    private final List<Thread> managedThreads;

    /** スレッド終了待機時間5秒（ミリ秒） */
    private static final long THREAD_TERMINATION_TIMEOUT = 5000L;

    /**
     * コンストラクタ
     * メインフレームとスレッドプールを初期化
     */
    public MainFrame() {
        super();
        this.contentPanel = new JPanel(new BorderLayout());
        frame.add(contentPanel);

        // スレッド管理用の初期化
        this.executor = Executors.newFixedThreadPool(5); // 5スレッドのプール
        this.managedThreads = new ArrayList<>();

        // ウィンドウ終了イベントのハンドリング
        setupWindowCloseHandler();
    }

    @Override
    protected void customizeFrame() {
        frame.setTitle("エンジニア人材管理");
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "メインフレームを初期化しました");
    }

    /**
     * ウィンドウ終了時の処理を設定
     * スレッドの安全な終了と後処理を行います
     */
    private void setupWindowCloseHandler() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performShutdown();
            }
        });
    }

    /**
     * アプリケーションの終了処理を実行
     * スレッドの安全な終了とリソースの解放を行います
     */
    public void performShutdown() {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "アプリケーション終了処理を開始します");

        // ExecutorServiceを安全に終了
        shutdownExecutorService();

        // 登録済みスレッドを安全に終了
        shutdownManagedThreads();

        // ウィンドウを閉じる
        frame.dispose();

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "アプリケーション終了処理が完了しました");

        // アプリケーションを終了
        System.exit(0);
    }

    /**
     * ExecutorServiceを安全に終了
     */
    private void shutdownExecutorService() {
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "ExecutorServiceを終了しています");

            // 新しいタスクを受け付けない
            executor.shutdown();

            // 既存タスクが終了するのを待機
            boolean terminated = executor.awaitTermination(THREAD_TERMINATION_TIMEOUT, TimeUnit.MILLISECONDS);

            if (!terminated) {
                // タイムアウトした場合は強制終了
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "ExecutorServiceのタスクが時間内に終了しないため、強制終了します");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // 割り込みが発生した場合は強制終了
            LogHandler.getInstance().logError(LogType.SYSTEM, "ExecutorServiceの終了中に割り込みが発生しました", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt(); // 割り込みステータスを保持
        }
    }

    /**
     * 登録済みスレッドを安全に終了
     */
    private void shutdownManagedThreads() {
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "登録済みスレッドを終了しています");

            // 全スレッドに割り込みを送信
            for (Thread thread : managedThreads) {
                if (thread.isAlive()) {
                    thread.interrupt();
                }
            }

            // 全スレッドの終了を待機
            for (Thread thread : managedThreads) {
                if (thread.isAlive()) {
                    thread.join(THREAD_TERMINATION_TIMEOUT);
                    if (thread.isAlive()) {
                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                "スレッド '" + thread.getName() + "' が時間内に終了しませんでした");
                    }
                }
            }
        } catch (InterruptedException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "スレッド終了待機中に割り込みが発生しました", e);
            Thread.currentThread().interrupt(); // 割り込みステータスを保持
        }
    }

    /**
     * パネルを切り替えて表示
     *
     * @param panel 表示するパネル
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

        LogHandler.getInstance().log(
                Level.INFO, LogType.SYSTEM,
                String.format("パネルを切り替えました: %s", panel.getClass().getSimpleName()));
    }

    /**
     * 現在のビューを更新
     */
    public void refreshView() {
        contentPanel.revalidate();
        contentPanel.repaint();
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "ビューを更新しました");
    }

    /**
     * バックグラウンドタスクを実行
     *
     * @param task 実行するRunnable
     */
    public void executeTask(Runnable task) {
        executor.execute(task);
    }

    /**
     * スレッドを管理対象に登録
     * アプリケーション終了時に安全に終了されるよう管理
     *
     * @param thread 管理対象のスレッド
     */
    public void registerThread(Thread thread) {
        if (thread != null) {
            managedThreads.add(thread);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "スレッド '" + thread.getName() + "' を管理対象に登録しました");
        }
    }

    /**
     * 管理対象からスレッドを削除
     *
     * @param thread 削除するスレッド
     * @return 削除に成功した場合はtrue
     */
    public boolean unregisterThread(Thread thread) {
        boolean removed = managedThreads.remove(thread);
        if (removed) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "スレッド '" + thread.getName() + "' を管理対象から削除しました");
        }
        return removed;
    }

    /**
     * 現在表示中のパネルを取得
     *
     * @return 現在のパネル
     */
    public JPanel getCurrentPanel() {
        return currentPanel;
    }

    /**
     * JFrameを取得
     *
     * @return JFrameインスタンス
     */
    public JFrame getJFrame() {
        return frame;
    }

    /**
     * 登録済みスレッド数を取得
     *
     * @return 管理しているスレッドの数
     */
    public int getManagedThreadCount() {
        return managedThreads.size();
    }
}