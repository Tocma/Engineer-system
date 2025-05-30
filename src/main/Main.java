package main;

import controller.MainController;
import test.TestCoreSystem;
import util.LogHandler;
import util.LogHandler.LogType;
import util.ResourceManager;
import view.MainFrame;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;

/**
 * エンジニア人材管理システムのエントリーポイント
 * システムの初期化、実行、リソース管理、終了処理を担当
 *
 * @author Nakano
 * @version 4.12.7
 * @since 2025-05-30
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
        if (!acquireLock(54321)) {
            System.exit(0);
        }

        try {
            // ログシステムの初期化（最優先）
            System.out.println("ログシステムの初期化を開始...");
            initializeLogger();
            System.out.println("ログシステムの初期化が完了しました");

            // シャットダウンフックの登録
            System.out.println("シャットダウンフックの登録...");
            registerShutdownHook();

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
     * 
     * @param args コマンドライン引数
     * @return テストモードの場合true
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
     * 
     * @param args コマンドライン引数
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
                // テストの実行は続行
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
     * ログハンドラの設定とログディレクトリの作成
     *
     * @throws IOException 初期化に失敗した場合
     */
    private static void initializeLogger() throws IOException {
        try {
            // LogHandlerの取得と初期化（getInstance()で自動初期化も試行される）
            LogHandler logHandler = LogHandler.getInstance();
            // 明示的に初期化を行う場合はこちら
            if (!logHandler.isInitialized()) {
                logHandler.initialize();
            }
            logHandler.log(Level.INFO, LogType.SYSTEM, "ログシステムを初期化しました");
        } catch (IOException e) {
            System.err.println("ログシステムの初期化に失敗しました: " + e.getMessage());
            System.err.println("標準出力へのフォールバックを使用します");
            throw e; // 上位レベルでの処理のために再スロー
        }
    }

    /**
     * リソースマネージャーを初期化
     * 必要なディレクトリとファイルを初期化
     *
     * @throws IOException 初期化に失敗した場合
     */
    private static void initializeResourceManager() throws IOException {
        try {
            // シングルトンインスタンスを取得して初期化
            ResourceManager.getInstance().initialize();
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "リソースマネージャーを初期化しました");
        } catch (IOException e) {
            System.err.println("リソースマネージャーの初期化に失敗しました: " + e.getMessage());
            LogHandler.getInstance().logError(LogType.SYSTEM, "リソースマネージャーの初期化に失敗しました", e);
            throw e; // 上位レベルでの処理のために再スロー
        }
    }

    /**
     * シャットダウンフックを登録
     * アプリケーション終了時のクリーンアップ処理
     */
    private static void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("シャットダウンフックが実行されました");

                try {
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "シャットダウンします");
                } catch (Exception e) {
                    // ログ記録に失敗しても処理を続行
                    System.err.println("シャットダウン時のログ記録に失敗: " + e.getMessage());
                }

                cleanup();
            }));
            shutdownHookRegistered = true;

            try {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "シャットダウンフックを登録しました");
            } catch (Exception e) {
                // ログ記録に失敗しても処理を続行
                System.out.println("シャットダウンフックを登録しました");
            }
        }
    }

    /**
     * アプリケーションを初期化
     * GUIコンポーネントとコントローラの初期化
     * CSVデータの読み込みを開始
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
     * エラーのログ記録とクリーンアップを行います
     *
     * @param e 発生した例外
     */
    private static void handleFatalError(Exception e) {
        // エラーログの記録
        try {
            LogHandler logHandler = LogHandler.getInstance();
            if (logHandler.isInitialized()) {
                logHandler.logError(LogType.SYSTEM, "システム起動中に致命的エラーが発生", e);
            }
        } catch (Exception logError) {
            // ログ記録に失敗しても処理を続行
            System.err.println("ログ記録に失敗: " + logError.getMessage());
        }

        System.err.println("システム起動中に致命的エラーが発生しました: " + e.getMessage());
        e.printStackTrace();

        // 終了処理
        cleanup();
        // 強制終了
        System.exit(1);
    }

    /**
     * クリーンアップ処理
     * リソースの解放と終了処理
     */
    private static void cleanup() {
        try {
            // リソースマネージャーのクリーンアップ
            ResourceManager resourceManager = ResourceManager.getInstance();
            if (resourceManager.isInitialized()) {
                // すべてのリソースを解放
                resourceManager.releaseAllResources();
            }

            // ログハンドラのクリーンアップ
            LogHandler logHandler = LogHandler.getInstance();
            if (logHandler.isInitialized()) {
                logHandler.cleanup();
            }
        } catch (Exception e) {
            System.err.println("クリーンアップ処理に失敗しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 重複起動を防ぐためのロックを取得
     * 指定されたポートでServerSocketを開き、他のインスタンスが起動できないようにする
     *
     * @param port ロック用のポート番号
     * @return ロック取得成功ならtrue、失敗ならfalse
     */
    private static boolean acquireLock(int port) {
        try {
            lockSocket = new ServerSocket(port);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}