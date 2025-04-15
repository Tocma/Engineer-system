package main;

import controller.MainController;
import test.TestCoreSystem;
import util.LogHandler;
import util.LogHandler.LogType;
import util.ResourceManager;
import view.MainFrame;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.logging.Level;

/**
 * エンジニア人材管理システムのエントリーポイント
 * システムの初期化、実行、リソース管理、終了処理を担当
 *
 * <p>
 * このクラスは、アプリケーションのライフサイクル全体を管理します：
 * <ul>
 * <li>ログシステムの初期化と設定</li>
 * <li>リソースマネージャーの初期化</li>
 * <li>UIコンポーネントの初期化と表示</li>
 * <li>コントローラの初期化と実行</li>
 * <li>CSVデータの読み込み開始</li>
 * <li>例外処理とエラーハンドリング</li>
 * <li>シャットダウンフックによる安全な終了処理</li>
 * <li>リソースの適切な解放</li>
 * </ul>
 * </p>
 *
 * <p>
 * アプリケーションの安全な終了を保証するため、このクラスはシャットダウンフックを登録し、
 * 終了前にすべてのリソースが適切に解放されることを確認します。
 * </p>
 *
 * <p>
 * 起動フローの概要：
 * <ol>
 * <li>ログシステムの初期化</li>
 * <li>シャットダウンフックの登録</li>
 * <li>リソースマネージャーの初期化</li>
 * <li>Swing EDT上でのUIコンポーネントとコントローラの初期化</li>
 * <li>メインウィンドウの表示</li>
 * <li>CSVデータの読み込みの開始</li>
 * </ol>
 * </p>
 *
 * <p>
 * 2025-04-03 追加: テストモードの実装
 * コマンドライン引数を使用してテストモードで起動できるようになりました。
 * 例：
 * 
 * <pre>
 * java -cp bin main.Main --test=startup
 * java -cp bin main.Main --test=shutdown
 * java -cp bin main.Main --test=csv
 * java -cp bin main.Main --test=all
 * </pre>
 * </p>
 *
 * @author Nakano
 * @version 4.0.0
 * @since 2025-04-15
 */
public class Main {

    /** ログディレクトリパス */
    private static final String LOG_DIR = "src/logs";

    /** シャットダウンフック登録済みフラグ */
    private static boolean shutdownHookRegistered = false;

    /** リソースマネージャー */
    private static ResourceManager resourceManager;

    /** メインコントローラー */
    private static MainController mainController;

    /**
     * アプリケーションのエントリーポイント
     * システムの初期化とアプリケーションの起動
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        // テストモードの確認
        if (isTestMode(args)) {
            runTestMode(args);
            return;
        }

        try {
            // ログシステムの初期化（最優先）
            initializeLogger();

            // シャットダウンフックの登録
            registerShutdownHook();

            // リソースマネージャーの初期化
            initializeResourceManager();

            // SwingのEDT（Event Dispatch Thread）でUIを初期化
            SwingUtilities.invokeLater(Main::initializeApplication);

        } catch (Exception e) {
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
        LogHandler logger = LogHandler.getInstance();
        logger.initialize(LOG_DIR);
        logger.log(Level.INFO, LogType.SYSTEM, "ログシステムを初期化します");
    }

    /**
     * リソースマネージャーを初期化
     * 必要なディレクトリとファイルを初期化
     *
     * @throws IOException 初期化に失敗した場合
     */
    private static void initializeResourceManager() throws IOException {
        resourceManager = new ResourceManager();
        resourceManager.initialize();
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "リソースマネージャーを初期化しました");
    }

    /**
     * シャットダウンフックを登録
     * アプリケーション終了時のクリーンアップ処理
     */
    private static void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "シャットダウンします");
                cleanup();
            }));
            shutdownHookRegistered = true;
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "シャットダウンフックを登録しました");
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
        if (LogHandler.getInstance().isInitialized()) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "システム起動中", e);
        } else {
            System.err.println("システム起動中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }

        // 終了処理
        cleanup();
        System.exit(1);
    }

    /**
     * クリーンアップ処理
     * リソースの解放と終了処理
     */
    private static void cleanup() {
        try {
            // リソースマネージャーのクリーンアップ
            if (resourceManager != null && resourceManager.isInitialized()) {
                resourceManager.releaseAllResources();
            }

            // ログハンドラのクリーンアップ
            if (LogHandler.getInstance().isInitialized()) {
                LogHandler.getInstance().cleanup();
            }
        } catch (Exception e) {
            System.err.println("クリーンアップ処理に失敗しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
}