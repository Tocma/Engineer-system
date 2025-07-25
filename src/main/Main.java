package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import controller.MainController;
import test.TestCoreSystem;
import util.LogHandler;
import util.LogHandler.LogType;
import util.PropertiesManager;
import util.ResourceManager;
import util.Constants.SystemConstants;
import view.MainFrame;

/**
 * エンジニア人材管理システムのエントリーポイント
 * システムの初期化、実行を担当
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

        try {
            // 1. PropertiesManagerの初期化（最優先）
            System.out.println("設定プロパティの初期化を開始...");
            initializeProperties();
            System.out.println("設定プロパティの初期化完了");

            // Look and Feelの設定
            System.out.println("Look and Feelの設定を開始...");
            try {
                // Nimbus Look and Feelを設定（OS間でUIを統一）
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | UnsupportedLookAndFeelException _e) {
                // Nimbusが利用できない場合は、システムデフォルトを使用
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex) {
                    // エラーログを記録
                    System.err.println("Look and Feelの設定に失敗: " + ex.getMessage());
                }
            }
            System.out.println("Look and Feelの設定完了");


            // 2. 重複起動時のポート番号の確認（PropertiesManager初期化後）
            if (!acquireLock(SystemConstants.LOCK_PORT)) {
                System.err.println("アプリケーションを終了します...");
                // バッチファイルで検出できるよう異常終了コードを返す
                System.exit(1); // 0 → 1 に変更
            }

            // 3. ログシステムの初期化
            System.out.println("ログシステムの初期化を開始...");
            initializeLogger();
            System.out.println("ログシステムの初期化完了");

            // 4. シャットダウンフックの登録
            System.out.println("シャットダウンフックの登録...");
            registerSimplifiedShutdownHook();

            // 5. リソースマネージャーの初期化
            System.out.println("リソースマネージャーの初期化を開始...");
            initializeResourceManager();
            System.out.println("リソースマネージャーの初期化完了");

            // 6. SwingのEDT（Event Dispatch Thread）でUIを初期化
            System.out.println("アプリケーションUIの初期化を開始...");
            SwingUtilities.invokeLater(Main::initializeApplication);

        } catch (Exception _e) {
            System.err.println("アプリケーション初期化中に致命的エラーが発生:");
            _e.printStackTrace();
            handleFatalError(_e);
        }
    }

    /**
     * 設定プロパティを初期化
     * PropertiesManagerの初期化を行い、プロパティファイルを読み込む
     * 
     * @throws IOException プロパティファイルの読み込みエラー
     */
    private static void initializeProperties() throws IOException {
        try {
            PropertiesManager propertiesManager = PropertiesManager.getInstance();
            propertiesManager.initialize();

            // プロパティファイルの読み込み状況を確認
            String systemName = propertiesManager.getString("system.name");
            String version = propertiesManager.getString("system.version");

            System.out.println("システム名: " + systemName);
            System.out.println("バージョン: " + version);

        } catch (Exception _e) {
            // プロパティファイルが見つからない場合もデフォルト値で継続
            System.err.println("プロパティファイルの読み込み中にエラーが発生しました: " + _e.getMessage());
            System.out.println("デフォルト設定値を使用して継続します");

            // PropertiesManagerは内部でデフォルト値を設定するため、
            // エラーが発生してもアプリケーションは継続可能
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
            // テストモードでもPropertiesManagerを初期化
            try {
                System.out.println("テスト用プロパティの初期化を試行...");
                initializeProperties();
                System.out.println("テスト用プロパティの初期化完了");
            } catch (Exception _e) {
                System.err.println("テスト用プロパティ初期化に失敗: " + _e.getMessage());
                System.out.println("デフォルト値を使用します");
            }

            // ログシステムの初期化を試行（テストでも必要）
            try {
                System.out.println("テスト用ログシステムの初期化を試行...");
                initializeLogger();
                System.out.println("テスト用ログシステムの初期化完了");
            } catch (Exception _e) {
                System.err.println("テスト用ログ初期化に失敗: " + _e.getMessage());
                System.err.println("標準出力へのフォールバックを使用します");
            }

            // テストシステムを初期化して実行
            TestCoreSystem.main(args);
        } catch (Exception _e) {
            System.err.println("テスト実行中にエラーが発生: " + _e.getMessage());
            _e.printStackTrace();
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
            logHandler.log(Level.INFO, LogType.SYSTEM, "ログシステムを初期化完了");
        } catch (IOException _e) {
            System.err.println("ログシステムの初期化に失敗: " + _e.getMessage());
            System.err.println("標準出力へのフォールバックを使用します");
            throw _e;
        }
    }

    /**
     * リソースマネージャーを初期化
     */
    private static void initializeResourceManager() throws IOException {
        try {
            ResourceManager.getInstance().initialize();
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "リソースマネージャーを初期化完了");
        } catch (IOException _e) {
            System.err.println("リソースマネージャーの初期化に失敗: " + _e.getMessage());
            LogHandler.getInstance().logError(LogType.SYSTEM, "リソースマネージャーの初期化に失敗", _e);
            throw _e;
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

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "アプリケーションの初期化完了");

        } catch (Exception _e) {
            System.err.println("GUI初期化中にエラーが発生: " + _e.getMessage());
            LogHandler.getInstance().logError(LogType.SYSTEM, "GUI初期化中にエラーが発生", _e);
            handleFatalError(_e);
        }
    }

    /**
     * 重複起動を防ぐためのロックを取得
     * SystemConstants.LOCK_PORTで指定されたポートにServerSocketを作成し、
     * 重複起動を防止する。既に他のインスタンスが起動している場合はfalseを返す
     *
     * @param port ロック用ポート番号
     * @return ロックの取得に成功した場合はtrue、失敗した場合はfalse
     */
    private static boolean acquireLock(int port) throws DuplicateInstanceException {
        try {
            lockSocket = new ServerSocket(port);
            System.out.println("アプリケーションロックを取得（ポート: " + port + "）");
            return true;
        } catch (IOException _e) {
            // コンソール出力のみ（GUIダイアログは削除）
            System.err.println("重複起動が検出されました（ポート: " + port + "）");
            System.err.println("既存のアプリケーションが実行中です。");

            // 重複起動例外をスローして起動スクリプト側で処理
            throw new DuplicateInstanceException("ポート " + port + " は既に使用されています", _e);
        }
    }

    /**
     * 重複起動検出用のカスタム例外
     */
    public static class DuplicateInstanceException extends Exception {
        public DuplicateInstanceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * アプリケーションロックを解放
     * ServerSocketをクローズしてポートを解放する
     */
    private static void releaseLock() {
        if (lockSocket != null && !lockSocket.isClosed()) {
            try {
                lockSocket.close();
                System.out.println("アプリケーションロックを解放");
            } catch (IOException _e) {
                System.err.println("ロックの解放に失敗: " + _e.getMessage());
            }
        }
    }

    /**
     * シャットダウンフックを登録
     * JVMがシャットダウンする際に実行され、
     * アプリケーションのロックを解放し、必要なクリーンアップ処理を行う
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
                } catch (Exception _e) {
                    System.err.println("シャットダウンフック開始時のログ記録に失敗: " + _e.getMessage());
                }

                // ロックソケットのクリーンアップ
                releaseLock();
                System.out.println("アプリケーションロックを解放");
                System.out.println("=== JVMシャットダウンフック実行完了 ===");
            }, "JVM-ShutdownHook-Thread"));

            shutdownHookRegistered = true;

            // 登録完了の通知
            try {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "改良されたシャットダウンフックを登録");
            } catch (Exception _e) {
                System.out.println("改良されたシャットダウンフックを登録");
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
        } catch (Exception _e) {
            System.err.println("最小限のクリーンアップ中にエラーが発生: " + _e.getMessage());
        }
    }

    /**
     * 致命的なエラーを処理
     */
    private static void handleFatalError(Exception _e) {
        try {
            LogHandler logHandler = LogHandler.getInstance();
            if (logHandler.isInitialized()) {
                logHandler.logError(LogType.SYSTEM, "システム起動中に致命的エラーが発生", _e);
            }
        } catch (Exception logError) {
            System.err.println("ログ記録に失敗: " + logError.getMessage());
        }

        System.err.println("システム起動中に致命的エラーが発生: " + _e.getMessage());
        _e.printStackTrace();

        // 最小限のクリーンアップを試行
        performMinimalCleanup();

        // 強制終了
        System.exit(1);
    }
}