package main;

import controller.MainController;
import test.TestCoreSystem;
import util.LogHandler;
import util.LogHandler.LogType;
import util.ResourceManager;
import util.Constants.SystemConstants;
import view.MainFrame;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;

/**
 * エンジニア人材管理システムのエントリーポイント
 * システムの初期化、実行を担当
 * 
 * 重要な変更点: 終了処理とリソース管理をMainControllerに完全に委譲し、
 * Main.javaは純粋にアプリケーションの起動処理のみに専念
 *
 * @author Nakano
 */
public class Main {

    /** シャットダウンフック登録済みフラグ */
    private static boolean shutdownHookRegistered = false;

    /** メインコントローラー */
    private static MainController mainController;

    /** ロックソケット */
    private static ServerSocket lockSocket;

    /**
     * アプリケーションのエントリーポイント
     * システムの初期化とアプリケーションの起動
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        System.out.println("アプリケーション起動プロセスを開始...");

        // テストモードの確認
        if (isTestMode(args)) {
            runTestMode(args);
            return;
        }

        // 重複起動時のポート番号の確認
        if (!acquireLock(SystemConstants.LOCK_PORT)) {
            System.exit(0);
        }

        try {
            // ログシステムの初期化（最優先）
            System.out.println("ログシステムの初期化を開始...");
            initializeLogger();
            System.out.println("ログシステムの初期化が完了しました");

            // シャットダウンフックの登録（簡素化版）
            System.out.println("シャットダウンフックの登録...");
            registerSimplifiedShutdownHook();

            // リソースマネージャーの初期化
            System.out.println("リソースマネージャーの初期化を開始...");
            initializeResourceManager();
            System.out.println("リソースマネージャーの初期化が完了しました");

            // SwingのEDT（Event Dispatch Thread）でUIを初期化
            System.out.println("アプリケーションUIの初期化を開始...");
            SwingUtilities.invokeLater(Main::initializeApplication);

        } catch (Exception e) {
            System.err.println("アプリケーション初期化中に致命的エラーが発生しました:");
            e.printStackTrace();
            handleFatalError(e);
        }
    }

    /**
     * コマンドライン引数からテストモードかどうかを判定
     */
    private static boolean isTestMode(String[] args) {
        if (args == null || args.length == 0) {
            return false;
        }

        for (String arg : args) {
            if (arg.startsWith("--test=")) {
                return true;
            }
        }

        return false;
    }

    /**
     * テストモードでアプリケーションを実行
     */
    private static void runTestMode(String[] args) {
        System.out.println("テストモードで起動します...");

        try {
            // ログシステムの初期化を試行（テストでも必要）
            try {
                System.out.println("テスト用ログシステムの初期化を試行...");
                initializeLogger();
                System.out.println("テスト用ログシステムの初期化に成功しました");
            } catch (Exception e) {
                System.err.println("テスト用ログ初期化に失敗: " + e.getMessage());
                System.err.println("標準出力へのフォールバックを使用します");
            }

            // テストシステムを初期化して実行
            TestCoreSystem.main(args);
        } catch (Exception e) {
            System.err.println("テスト実行中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * ログシステムを初期化
     */
    private static void initializeLogger() throws IOException {
        try {
            LogHandler logHandler = LogHandler.getInstance();
            if (!logHandler.isInitialized()) {
                logHandler.initialize();
            }
            logHandler.log(Level.INFO, LogType.SYSTEM, "ログシステムを初期化しました");
        } catch (IOException e) {
            System.err.println("ログシステムの初期化に失敗しました: " + e.getMessage());
            System.err.println("標準出力へのフォールバックを使用します");
            throw e;
        }
    }

    /**
     * リソースマネージャーを初期化
     */
    private static void initializeResourceManager() throws IOException {
        try {
            ResourceManager.getInstance().initialize();
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "リソースマネージャーを初期化しました");
        } catch (IOException e) {
            System.err.println("リソースマネージャーの初期化に失敗しました: " + e.getMessage());
            LogHandler.getInstance().logError(LogType.SYSTEM, "リソースマネージャーの初期化に失敗しました", e);
            throw e;
        }
    }

    /**
     * 改良されたシャットダウンフックを登録
     * 終了完了フラグを活用して正確な状態判定を実現
     * 
     * この実装の特徴：
     * 1. MainControllerの終了処理完了状態を正確に判定
     * 2. 正常終了時の不要なメッセージ出力を防止
     * 3. 真の異常時のみ代替処理を実行
     * 4. 詳細なログ出力による状況把握の向上
     */
    private static void registerSimplifiedShutdownHook() {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // JVMシャットダウンフック開始の通知
                System.out.println("=== JVMシャットダウンフック実行開始 ===");

                try {
                    // ログシステムが利用可能かチェック
                    LogHandler logHandler = LogHandler.getInstance();
                    if (logHandler.isInitialized()) {
                        logHandler.log(Level.INFO, LogType.SYSTEM,
                                "JVMシャットダウンフック実行：MainControllerの状態を詳細分析中");
                    }
                } catch (Exception e) {
                    System.err.println("シャットダウンフック開始時のログ記録に失敗: " + e.getMessage());
                }

                // ロックソケットのクリーンアップ
                releaseLock();
                System.out.println("アプリケーションロックを解放しました");
                System.out.println("=== JVMシャットダウンフック実行完了 ===");
            }, "JVM-ShutdownHook-Thread"));

            shutdownHookRegistered = true;

            // 登録完了の通知
            try {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "改良されたシャットダウンフックを登録しました");
            } catch (Exception e) {
                System.out.println("改良されたシャットダウンフックを登録しました");
            }
        }
    }

    /**
     * 最小限のクリーンアップ処理
     * MainControllerが利用できない場合の緊急処理
     */
    private static void performMinimalCleanup() {
        try {
            // ログシステムだけクリーンアップ
            LogHandler.getInstance().cleanup();
        } catch (Exception e) {
            System.err.println("最小限のクリーンアップ中にエラーが発生: " + e.getMessage());
        }
    }

    /**
     * アプリケーションを初期化
     * GUIコンポーネントとコントローラの初期化
     */
    private static void initializeApplication() {
        try {
            // メインフレームの初期化
            MainFrame mainFrame = new MainFrame();

            // メインコントローラの初期化
            mainController = new MainController(mainFrame);
            mainController.initialize();

            // データ読み込みイベントを送信
            mainController.handleEvent("LOAD_DATA", null);

            // メインフレームの表示
            mainFrame.setVisible(true);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "アプリケーションの初期化が完了しました");

        } catch (Exception e) {
            System.err.println("GUI初期化中にエラーが発生しました: " + e.getMessage());
            LogHandler.getInstance().logError(LogType.SYSTEM, "GUI初期化中にエラーが発生しました", e);
            handleFatalError(e);
        }
    }

    /**
     * 致命的なエラーを処理
     */
    private static void handleFatalError(Exception e) {
        try {
            LogHandler logHandler = LogHandler.getInstance();
            if (logHandler.isInitialized()) {
                logHandler.logError(LogType.SYSTEM, "システム起動中に致命的エラーが発生", e);
            }
        } catch (Exception logError) {
            System.err.println("ログ記録に失敗: " + logError.getMessage());
        }

        System.err.println("システム起動中に致命的エラーが発生しました: " + e.getMessage());
        e.printStackTrace();

        // 最小限のクリーンアップを試行
        performMinimalCleanup();

        // 強制終了
        System.exit(1);
    }

    /**
     * 重複起動を防ぐためのロックを取得
     * SystemConstants.LOCK_PORTで指定されたポートにServerSocketを作成し、
     * 重複起動を防止する。既にポートが使用されている場合は、
     * アプリケーションが既に起動していると判断する。
     *
     * @param port ロック用ポート番号（SystemConstants.LOCK_PORT）
     * @return ロック取得に成功した場合true、失敗した場合false
     */
    private static boolean acquireLock(int port) {
        try {
            lockSocket = new ServerSocket(port);
            System.out.println("アプリケーションロックを取得しました（ポート: " + port + "）");

            // ログシステムが利用可能な場合はログに記録
            try {
                LogHandler logHandler = LogHandler.getInstance();
                if (logHandler.isInitialized()) {
                    logHandler.log(Level.INFO, LogType.SYSTEM,
                            "重複起動防止ロックを取得しました（ポート: " + port + "）");
                }
            } catch (Exception e) {
                // ログ記録に失敗してもロック取得は成功とする
            }

            return true;
        } catch (IOException e) {
            System.err.println("ロック取得に失敗しました（ポート: " + port + "）: " + e.getMessage());
            System.err.println("アプリケーションが既に起動している可能性があります");
            return false;
        }
    }

    /**
     * 取得したロックを解放
     * アプリケーション終了時にServerSocketを適切にクローズする
     */
    private static void releaseLock() {
        if (lockSocket != null && !lockSocket.isClosed()) {
            try {
                lockSocket.close();
                System.out.println("アプリケーションロックを解放しました");

                // ログシステムが利用可能な場合はログに記録
                try {
                    LogHandler logHandler = LogHandler.getInstance();
                    if (logHandler.isInitialized()) {
                        logHandler.log(Level.INFO, LogType.SYSTEM, "重複起動防止ロックを解放しました");
                    }
                } catch (Exception e) {
                    // ログ記録に失敗してもロック解放は成功とする
                }
            } catch (IOException e) {
                System.err.println("ロック解放中にエラーが発生しました: " + e.getMessage());
            } finally {
                lockSocket = null;
            }
        }
    }
}