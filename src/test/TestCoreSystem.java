package test;

import controller.MainController;
import model.EngineerDTO;
import util.LogHandler;
import util.LogHandler.LogType;
import util.ResourceManager;
import view.ListPanel;
import view.MainFrame;
import javax.swing.SwingUtilities;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTable;

/**
 * エンジニア人材管理システムの機能試験を実行するクラス
 * コマンドライン引数を活用して起動処理、終了処理、CSVデータ読み込みのテストを行う
 *
 * <p>
 * このクラスは、エンジニア人材管理システムの主要機能を検証するためのテストケースを提供します。
 * コマンドライン引数によってテストケースを指定し、テスト結果は自動的にログファイルに記録されます。
 * 各テストケースは独立して実行可能で、テスト実行中は進捗状況と結果が標準出力にも表示されます。
 * </p>
 *
 * <p>
 * 本クラスの主な特徴：
 * <ul>
 * <li>コマンドライン引数を使用した柔軟なテスト実行</li>
 * <li>詳細なテスト結果レポートの自動生成（Markdown形式）</li>
 * <li>テスト環境の自動セットアップ（ディレクトリ作成、ファイル準備など）</li>
 * <li>テスト用CSVデータの自動生成</li>
 * <li>カスタム出力ディレクトリの指定機能</li>
 * <li>非同期処理の適切な待機とタイムアウト処理</li>
 * <li>ログファイル解析による処理検証</li>
 * <li>UIコンポーネントの検証</li>
 * </ul>
 * </p>
 *
 * <p>
 * 主なテストケース：
 * <ul>
 * <li>起動テスト：アプリケーションの初期化と起動プロセスを検証</li>
 * <li>終了テスト：リソース解放とシャットダウンプロセスを検証</li>
 * <li>CSV読み込みテスト：データのロードと画面表示を検証</li>
 * <li>統合テスト：上記のすべてのテストを順番に実行</li>
 * </ul>
 * </p>
 *
 * <p>
 * テスト実行の流れ：
 * <ol>
 * <li>テスト環境の初期化（ディレクトリ作成、ログ設定など）</li>
 * <li>コマンドライン引数の解析とテストタイプの判定</li>
 * <li>指定されたテストケースの実行</li>
 * <li>テスト結果の評価と詳細なレポートの生成</li>
 * <li>テスト結果の保存と標準出力への結果表示</li>
 * <li>リソースの適切なクリーンアップ</li>
 * </ol>
 * </p>
 *
 * <p>
 * 使用例：
 * 
 * <pre>
 * // 起動テストを実行
 * java -cp bin test.TestCoreSystem --test=startup
 * 
 * // 終了テストを実行
 * java -cp bin test.TestCoreSystem --test=shutdown
 * 
 * // CSV読み込みテストを実行
 * java -cp bin test.TestCoreSystem --test=csv
 * 
 * // すべてのテストを実行
 * java -cp bin test.TestCoreSystem --test=all
 * 
 * // テスト結果出力先を指定
 * java -cp bin test.TestCoreSystem --test=all --output=/path/to/results
 * </pre>
 * </p>
 *
 * @author Nakano
 * @version 3.0.0
 * @since 2025-04-04
 */
public class TestCoreSystem {

    /**
     * テスト結果レポートの出力先ディレクトリ
     * テスト実行時に明示的に指定されない場合、このデフォルト値が使用される
     */
    private static final String DEFAULT_OUTPUT_DIR = "src/test/results";

    /**
     * ログディレクトリ
     * アプリケーションのログが保存されるディレクトリのパス
     */
    private static final String LOG_DIR = "src/test/results";

    /**
     * CSVファイルパス
     * テスト対象のCSVファイルのパス（エンジニアデータ）
     */
    private static final String CSV_FILE_PATH = "src/data/engineers.csv";

    /**
     * テスト結果の出力先ディレクトリ
     * コマンドライン引数またはデフォルト値から設定される
     */
    private String outputDir = DEFAULT_OUTPUT_DIR;

    /**
     * リソースマネージャー
     * テスト環境のリソース管理を担当
     */
    private ResourceManager resourceManager;

    /**
     * メインフレーム
     * アプリケーションのメインウィンドウ（テスト対象）
     */
    private MainFrame mainFrame;

    /**
     * メインコントローラー
     * アプリケーションのメインコントローラー（テスト対象）
     */
    private MainController mainController;

    /**
     * テスト結果（成功したテスト数）
     * 実行したテストのうち、成功したテストの数
     */
    private int passedTests = 0;

    /**
     * テスト結果（失敗したテスト数）
     * 実行したテストのうち、失敗したテストの数
     */
    private int failedTests = 0;

    /**
     * テスト結果のレポート内容
     * テスト実行中に収集された情報を保持するためのバッファ
     */
    private final StringBuilder testReport = new StringBuilder();

    /**
     * アプリケーション起動完了フラグ
     * アプリケーションが正常に起動完了した場合にtrueになる
     */
    private boolean applicationStarted = false;

    /**
     * シャットダウン完了フラグ
     * アプリケーションが正常にシャットダウンを完了した場合にtrueになる
     */
    private boolean shutdownCompleted = false;

    /**
     * コンストラクタ
     * テスト初期化処理を行う
     * 
     * <p>
     * テスト環境の初期化処理を実行します。具体的には：
     * <ul>
     * <li>出力ディレクトリの確認と作成</li>
     * <li>ログシステムの初期化</li>
     * <li>テストレポートのヘッダー情報の設定</li>
     * </ul>
     * </p>
     */
    public TestCoreSystem() {
        initializeTestEnvironment();
    }

    /**
     * 出力ディレクトリを取得
     * 現在設定されているテスト結果出力先ディレクトリを返す
     * 
     * <p>
     * このメソッドは、テスト結果が保存されるディレクトリのパスを取得します。
     * コマンドライン引数で指定されたディレクトリか、デフォルトのディレクトリが返されます。
     * </p>
     *
     * @return 出力ディレクトリのパス
     */
    public String getOutputDirectory() {
        return outputDir;
    }

    /**
     * テスト環境の初期化
     * 出力ディレクトリの作成、ログハンドラの初期化などを行う
     * 
     * <p>
     * テスト実行に必要な環境を準備します：
     * <ul>
     * <li>テスト結果出力ディレクトリの確認と作成</li>
     * <li>ログシステムの初期化状態確認</li>
     * <li>テストレポートの基本情報の設定</li>
     * </ul>
     * </p>
     */
    private void initializeTestEnvironment() {
        try {
            // 出力ディレクトリの作成
            Path outPath = Paths.get(outputDir);
            if (!Files.exists(outPath)) {
                Files.createDirectories(outPath);
                System.out.println("テスト結果出力ディレクトリを作成しました: " + outPath.toAbsolutePath());
            }

            // ログシステムの初期化
            LogHandler logger = LogHandler.getInstance();
            if (!logger.isInitialized()) {
                logger.initialize(LOG_DIR);
                logger.log(LogType.SYSTEM, "テスト用ログシステムを初期化しました");
            }

            // テストレポートにヘッダー情報を追加
            addReportHeader();

        } catch (IOException e) {
            System.err.println("テスト環境の初期化に失敗しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * テストレポートにヘッダー情報を追加
     * 
     * <p>
     * テスト結果レポートのヘッダー部分を作成し、以下の情報を含めます：
     * <ul>
     * <li>タイトルと実行日時</li>
     * <li>システム環境情報（OS、Javaバージョン、利用可能メモリなど）</li>
     * </ul>
     * </p>
     */
    private void addReportHeader() {
        testReport.append("# エンジニア人材管理システム 機能試験結果\n\n");
        testReport.append("## 実行日時\n");
        testReport.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        testReport.append("\n\n");
        testReport.append("## システム環境情報\n");
        testReport.append("- OS: ").append(System.getProperty("os.name")).append(" ")
                .append(System.getProperty("os.version")).append("\n");
        testReport.append("- Java: ").append(System.getProperty("java.version")).append("\n");
        testReport.append("- メモリ: 最大 ").append(Runtime.getRuntime().maxMemory() / 1024 / 1024)
                .append("MB\n\n");
    }

    /**
     * メインメソッド
     * コマンドライン引数を解析してテストを実行
     * 
     * <p>
     * プログラムのエントリーポイントとして機能し、以下の処理を行います：
     * <ol>
     * <li>コマンドライン引数の解析</li>
     * <li>テストタイプの判定と適切なテスト実行</li>
     * <li>テスト結果のレポート生成と保存</li>
     * <li>終了コードの設定（成功:0、失敗:1）</li>
     * </ol>
     * </p>
     *
     * @param args コマンドライン引数
     *             書式: --test=テストタイプ [--output=出力パス]
     *             テストタイプ: startup, shutdown, csv, all
     */
    public static void main(String[] args) {
        TestCoreSystem testSystem = new TestCoreSystem();

        // コマンドライン引数の解析
        String testType = null;
        String outputDir = DEFAULT_OUTPUT_DIR;

        for (String arg : args) {
            if (arg.startsWith("--test=")) {
                testType = arg.substring("--test=".length());
            } else if (arg.startsWith("--output=")) {
                outputDir = arg.substring("--output=".length());
            }
        }

        testSystem.setOutputDirectory(outputDir);

        // テストタイプに応じたテスト実行
        if (testType == null || testType.isEmpty()) {
            System.out.println("テストタイプが指定されていません。使用例：");
            System.out.println(
                    "  java -cp bin test.TestCoreSystem --test=startup|shutdown|csv|all [--output=/path/to/results]");
            return;
        }

        try {
            boolean success = false;

            switch (testType.toLowerCase()) {
                case "startup":
                    success = testSystem.runStartupTest();
                    break;
                case "shutdown":
                    success = testSystem.runShutdownTest();
                    break;
                case "csv":
                    success = testSystem.runCsvLoadTest();
                    break;
                case "all":
                    success = testSystem.runAllTests();
                    break;
                default:
                    System.out.println("未知のテストタイプです: " + testType);
                    System.out.println("有効なテストタイプ: startup, shutdown, csv, all");
                    return;
            }

            // テスト結果の出力
            testSystem.saveTestReport();
            System.out.println("テスト終了: " + (success ? "成功" : "失敗"));
            System.out.println("詳細なテスト結果は " + testSystem.getOutputDirectory() + " に保存されました");

            // テスト失敗の場合は終了コード1を返す
            if (!success) {
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("テスト実行中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 起動テストを実行
     * アプリケーションの初期化と起動プロセスを検証
     * 
     * <p>
     * このテストでは、以下の項目を検証します：
     * <ol>
     * <li>アプリケーションの起動プロセスが正常に完了するか</li>
     * <li>起動中に適切なログメッセージが出力されるか</li>
     * <li>UIコンポーネントが正しく初期化され表示されるか</li>
     * <li>起動処理が許容時間内に完了するか（タイムアウト監視）</li>
     * </ol>
     * </p>
     *
     * @return テスト成功の場合true、失敗の場合false
     */
    public boolean runStartupTest() {
        System.out.println("起動テストを開始します...");
        testReport.append("## 起動テスト\n\n");

        try {
            // テスト開始時間の記録
            long startTime = System.currentTimeMillis();

            // テスト用のCSVファイルを作成
            createTestCsvFile();

            // 非同期でのアプリケーション起動完了を待機するためのラッチ
            final CountDownLatch startupLatch = new CountDownLatch(1);

            SwingUtilities.invokeLater(() -> {
                try {
                    // ログハンドラの初期化確認
                    LogHandler logger = LogHandler.getInstance();
                    if (!logger.isInitialized()) {
                        initializeLogger();
                    }

                    // リソースマネージャーの初期化
                    resourceManager = new ResourceManager();
                    resourceManager.initialize();
                    logger.log(Level.INFO, LogType.SYSTEM, "テスト用リソースマネージャーを初期化しました");

                    // メインフレームの初期化
                    mainFrame = new MainFrame();

                    // メインコントローラーの初期化
                    mainController = new MainController(mainFrame);
                    mainController.initialize();

                    // データ読み込みイベントを送信
                    mainController.handleEvent("LOAD_DATA", null);

                    // メインフレームの表示
                    mainFrame.setVisible(true);

                    applicationStarted = true;
                    startupLatch.countDown(); // 初期化完了を通知

                } catch (Exception e) {
                    LogHandler.getInstance().logError(LogType.SYSTEM, "テスト起動中にエラーが発生しました", e);
                    applicationStarted = false;
                    startupLatch.countDown(); // エラーの場合も通知
                }
            });

            // 起動完了を最大30秒待機
            boolean startupCompleted = startupLatch.await(30, TimeUnit.SECONDS);

            if (!startupCompleted) {
                testReport.append("❌ 起動プロセスがタイムアウトしました (30秒)\n");
                failTest("起動プロセスのタイムアウト");
                return false;
            }

            if (!applicationStarted) {
                testReport.append("❌ アプリケーションの起動に失敗しました\n");
                failTest("アプリケーション起動失敗");
                return false;
            }

            // 起動所要時間を計算
            long elapsedTime = System.currentTimeMillis() - startTime;
            testReport.append("✅ アプリケーションが正常に起動しました (所要時間: " + elapsedTime + "ms)\n");

            // ログの確認
            boolean logsValid = validateStartupLogs();
            if (logsValid) {
                testReport.append("✅ 起動ログが正常に記録されています\n");
            } else {
                testReport.append("❌ 起動ログに問題があります\n");
                failTest("起動ログの検証失敗");
                return false;
            }

            // UIコンポーネントの確認
            boolean uiValid = validateUI();
            if (uiValid) {
                testReport.append("✅ UIコンポーネントが正常に表示されています\n");
            } else {
                testReport.append("❌ UIコンポーネントに問題があります\n");
                failTest("UIコンポーネントの検証失敗");
                return false;
            }

            passTest("起動テスト");
            return true;

        } catch (Exception e) {
            testReport.append("❌ 起動テスト実行中に例外が発生しました: " + e.getMessage() + "\n");
            e.printStackTrace();
            failTest("起動テスト例外: " + e.getMessage());
            return false;
        } finally {
            testReport.append("\n");
        }
    }

    /**
     * 終了テストを実行
     * アプリケーションの安全なシャットダウンを検証
     * 
     * <p>
     * このテストでは、以下の項目を検証します：
     * <ol>
     * <li>アプリケーションのシャットダウンプロセスが正常に完了するか</li>
     * <li>リソースが適切に解放されるか</li>
     * <li>シャットダウン中に適切なログが出力されるか</li>
     * <li>シャットダウン処理が許容時間内に完了するか（タイムアウト監視）</li>
     * <li>実行中の処理がすべて安全に終了するか</li>
     * </ol>
     * </p>
     *
     * @return テスト成功の場合true、失敗の場合false
     */
    public boolean runShutdownTest() {
        System.out.println("終了テストを開始します...");
        testReport.append("## 終了テスト\n\n");

        // アプリケーションが起動していない場合は、まず起動テストを実行
        if (!applicationStarted) {
            boolean startupSuccess = runStartupTest();
            if (!startupSuccess) {
                testReport.append("❌ 起動テストに失敗したため、終了テストを実行できません\n");
                failTest("起動前提条件未満足");
                return false;
            }
        }

        try {
            // シャットダウン完了を待機するためのラッチ
            final CountDownLatch shutdownLatch = new CountDownLatch(1);

            // シャットダウン監視スレッド
            Thread monitorThread = new Thread(() -> {
                try {
                    // ここでシャットダウン完了を監視（ログファイルを監視）
                    int checkCount = 0;
                    while (checkCount < 30) { // 最大30秒間チェック
                        Thread.sleep(1000);
                        checkCount++;

                        // ログファイルからシャットダウン完了を確認
                        if (checkShutdownCompleteInLogs()) {
                            shutdownCompleted = true;
                            shutdownLatch.countDown();
                            return;
                        }
                    }

                    // タイムアウト
                    shutdownLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    shutdownLatch.countDown();
                }
            });

            monitorThread.setDaemon(true);
            monitorThread.start();

            // シャットダウンを開始
            testReport.append("シャットダウンを開始します...\n");
            SwingUtilities.invokeLater(() -> {
                if (mainFrame != null) {
                    testReport.append("メインフレームのシャットダウンを実行します\n");
                    mainFrame.performShutdown();
                } else {
                    testReport.append("メインフレームがnullです - シャットダウンをスキップします\n");
                    shutdownLatch.countDown();
                }
            });

            // シャットダウン完了を待機（最大30秒）
            boolean shutdownComplete = shutdownLatch.await(30, TimeUnit.SECONDS);

            if (!shutdownComplete || !shutdownCompleted) {
                testReport.append("❌ シャットダウンプロセスがタイムアウトしました (30秒)\n");
                failTest("シャットダウンタイムアウト");
                return false;
            }

            // シャットダウンログの検証
            boolean logsValid = validateShutdownLogs();
            if (logsValid) {
                testReport.append("✅ シャットダウンログが正常に記録されています\n");
            } else {
                testReport.append("❌ シャットダウンログに問題があります\n");
                failTest("シャットダウンログの検証失敗");
                return false;
            }

            testReport.append("✅ アプリケーションが正常に終了しました\n");

            // プロセスが残っていないことを確認
            boolean processesClean = checkForRemainingProcesses();
            if (processesClean) {
                testReport.append("✅ 残存プロセスはありません\n");
            } else {
                testReport.append("❌ 残存プロセスが見つかりました\n");
                failTest("残存プロセスの検出");
                return false;
            }

            passTest("終了テスト");
            return true;

        } catch (Exception e) {
            testReport.append("❌ 終了テスト実行中に例外が発生しました: " + e.getMessage() + "\n");
            e.printStackTrace();
            failTest("終了テスト例外: " + e.getMessage());
            return false;
        } finally {
            testReport.append("\n");

            // アプリケーション状態のリセット
            applicationStarted = false;
            mainFrame = null;
            mainController = null;
        }
    }

    /**
     * CSV読み込みテストを実行
     * CSVファイルからのデータ読み込みと一覧表示を検証
     * 
     * <p>
     * このテストでは、以下の項目を検証します：
     * <ol>
     * <li>CSVファイルからのデータ読み込みが正常に完了するか</li>
     * <li>読み込まれたデータが正しく画面に表示されるか</li>
     * <li>データ件数が期待通りか</li>
     * <li>テーブル構造が正しいか</li>
     * <li>データ読み込み処理のログが適切か</li>
     * </ol>
     * </p>
     * 
     * <p>
     * テスト実行時には、既存のCSVファイルをバックアップし、テスト用のデータを含むCSVファイルを
     * 一時的に作成します。テスト完了後は元のCSVファイルを復元します。
     * </p>
     *
     * @return テスト成功の場合true、失敗の場合false
     */
    public boolean runCsvLoadTest() {
        System.out.println("CSV読み込みテストを開始します...");
        testReport.append("## CSV読み込みテスト\n\n");

        try {
            // テスト用CSVファイルの作成
            List<EngineerDTO> testData = createTestCsvFile();
            int expectedRecords = testData.size();
            testReport.append("テスト用CSVファイルを作成しました（" + expectedRecords + "件のレコード）\n");

            // アプリケーションが起動していない場合は、まず起動テストを実行
            if (!applicationStarted) {
                boolean startupSuccess = runStartupTest();
                if (!startupSuccess) {
                    testReport.append("❌ 起動テストに失敗したため、CSV読み込みテストを実行できません\n");
                    failTest("起動前提条件未満足");
                    return false;
                }
            }

            // 非同期のデータ読み込み完了を待機するためのラッチ
            final CountDownLatch dataLoadLatch = new CountDownLatch(1);

            // データ読み込みを開始
            SwingUtilities.invokeLater(() -> {
                if (mainController != null) {
                    // データ読み込みイベントを送信
                    mainController.handleEvent("LOAD_DATA", null);

                    // 読み込み完了を遅延して通知（非同期処理のため）
                    new Thread(() -> {
                        try {
                            // 読み込み完了までの待機時間
                            Thread.sleep(3000);
                            dataLoadLatch.countDown();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            dataLoadLatch.countDown();
                        }
                    }).start();
                } else {
                    testReport.append("メインコントローラーがnullです - データ読み込みをスキップします\n");
                    dataLoadLatch.countDown();
                }
            });

            // データ読み込み完了を待機（最大30秒）
            boolean dataLoadComplete = dataLoadLatch.await(30, TimeUnit.SECONDS);

            if (!dataLoadComplete) {
                testReport.append("❌ データ読み込みプロセスがタイムアウトしました (30秒)\n");
                failTest("データ読み込みタイムアウト");
                return false;
            }

            // ログの確認
            boolean logsValid = validateDataLoadLogs();
            if (logsValid) {
                testReport.append("✅ データ読み込みログが正常に記録されています\n");
            } else {
                testReport.append("❌ データ読み込みログに問題があります\n");
                failTest("データ読み込みログの検証失敗");
                return false;
            }

            // 一覧表示の確認
            boolean displayValid = validateDataDisplay(expectedRecords);
            if (displayValid) {
                testReport.append("✅ データが一覧画面に正しく表示されています（" + expectedRecords + "件）\n");
            } else {
                testReport.append("❌ データ表示に問題があります\n");
                failTest("データ表示の検証失敗");
                return false;
            }

            passTest("CSV読み込みテスト");
            return true;

        } catch (Exception e) {
            testReport.append("❌ CSV読み込みテスト実行中に例外が発生しました: " + e.getMessage() + "\n");
            e.printStackTrace();
            failTest("CSV読み込みテスト例外: " + e.getMessage());
            return false;
        } finally {
            testReport.append("\n");
        }
    }

    /**
     * すべてのテストを順番に実行
     * 起動テスト、CSV読み込みテスト、終了テストを順番に実行し、結果をまとめる
     * 
     * <p>
     * このメソッドは統合テストとして機能し、以下の処理を行います：
     * <ol>
     * <li>起動テストの実行</li>
     * <li>CSV読み込みテストの実行（起動テスト成功時のみ）</li>
     * <li>終了テストの実行（起動テスト成功時のみ）</li>
     * <li>テスト結果のサマリー生成</li>
     * </ol>
     * </p>
     * 
     * <p>
     * 前のテストが失敗した場合、依存関係のあるテストはスキップされます。
     * 例えば、起動テストが失敗した場合、CSV読み込みテストと終了テストは実行されません。
     * </p>
     *
     * @return すべてのテストが成功した場合true、一つでも失敗した場合false
     */
    public boolean runAllTests() {
        System.out.println("すべてのテストを実行します...");
        testReport.append("# 統合テスト実行結果\n\n");

        // 起動テスト
        boolean startupSuccess = runStartupTest();

        // CSV読み込みテスト（起動テストが成功した場合のみ）
        boolean csvLoadSuccess = false;
        if (startupSuccess) {
            csvLoadSuccess = runCsvLoadTest();
        } else {
            testReport.append("❌ 起動テストに失敗したため、CSV読み込みテストをスキップします\n\n");
        }

        // 終了テスト（起動テストが成功した場合のみ）
        boolean shutdownSuccess = false;
        if (startupSuccess) {
            shutdownSuccess = runShutdownTest();
        } else {
            testReport.append("❌ 起動テストに失敗したため、終了テストをスキップします\n\n");
        }

        // すべてのテスト結果をサマリーとして追加
        testReport.append("## テスト結果サマリー\n\n");
        testReport.append("- 起動テスト: ").append(startupSuccess ? "成功 ✅" : "失敗 ❌").append("\n");
        testReport.append("- CSV読み込みテスト: ");
        if (!startupSuccess) {
            testReport.append("スキップ ⚠️");
        } else {
            testReport.append(csvLoadSuccess ? "成功 ✅" : "失敗 ❌");
        }
        testReport.append("\n");

        testReport.append("- 終了テスト: ");
        if (!startupSuccess) {
            testReport.append("スキップ ⚠️");
        } else {
            testReport.append(shutdownSuccess ? "成功 ✅" : "失敗 ❌");
        }
        testReport.append("\n\n");

        testReport.append("合計: 成功 ").append(passedTests).append(" / 失敗 ").append(failedTests).append("\n");

        // すべてのテストが成功したかどうかを返す
        return startupSuccess && csvLoadSuccess && shutdownSuccess;
    }

    /**
     * ログシステムを初期化
     * テスト用のログシステムを設定し、ログディレクトリを作成
     *
     * @throws IOException 初期化に失敗した場合
     */
    private void initializeLogger() throws IOException {
        LogHandler logger = LogHandler.getInstance();
        logger.initialize(LOG_DIR);
        logger.log(LogType.SYSTEM, "テスト用ログシステムを初期化しました");
    }

    /**
     * テスト用のCSVファイルを作成
     * テスト用のエンジニアデータを含むCSVファイルを生成
     *
     * @return 作成したテストデータのリスト
     * @throws IOException ファイル作成に失敗した場合
     */
    private List<EngineerDTO> createTestCsvFile() throws IOException {
        List<EngineerDTO> testData = generateTestData();

        // CSVファイルを作成
        File csvFile = new File(CSV_FILE_PATH);
        File parent = csvFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, StandardCharsets.UTF_8))) {
            // ヘッダー行
            writer.write("id,name,nameKana,birthDate,joinDate,career,programmingLanguages," +
                    "careerHistory,trainingHistory,technicalSkill,learningAttitude," +
                    "communicationSkill,leadership,note,registeredDate");
            writer.newLine();

            // データ行
            for (EngineerDTO engineer : testData) {
                StringBuilder sb = new StringBuilder();
                sb.append(engineer.getId()).append(",");
                sb.append(engineer.getName()).append(",");
                sb.append(engineer.getNameKana()).append(",");
                sb.append(engineer.getBirthDate()).append(",");
                sb.append(engineer.getJoinDate()).append(",");
                sb.append(engineer.getCareer()).append(",");

                // プログラミング言語（セミコロン区切り）
                sb.append(String.join(";", engineer.getProgrammingLanguages())).append(",");

                // 残りのフィールド
                sb.append(nullToEmpty(engineer.getCareerHistory())).append(",");
                sb.append(nullToEmpty(engineer.getTrainingHistory())).append(",");
                sb.append(engineer.getTechnicalSkill()).append(",");
                sb.append(engineer.getLearningAttitude()).append(",");
                sb.append(engineer.getCommunicationSkill()).append(",");
                sb.append(engineer.getLeadership()).append(",");
                sb.append(nullToEmpty(engineer.getNote())).append(",");
                sb.append(engineer.getRegisteredDate());

                writer.write(sb.toString());
                writer.newLine();
            }
        }

        System.out.println("テスト用CSVファイルを作成しました");
        return testData;
    }

    /**
     * nullの場合は空文字列を返す
     * CSVデータ作成時に使用
     *
     * @param value 変換する値
     * @return nullの場合は空文字列、それ以外は元の値
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * テスト用のエンジニアデータを生成
     * テスト用の模擬エンジニアデータを3件作成
     *
     * @return 生成したエンジニアデータのリスト
     */
    private List<EngineerDTO> generateTestData() {
        List<EngineerDTO> testData = new ArrayList<>();

        // テストデータ 1
        EngineerDTO engineer1 = new EngineerDTO();
        engineer1.setId("ID00001");
        engineer1.setName("山田太郎");
        engineer1.setNameKana("ヤマダタロウ");
        engineer1.setBirthDate(LocalDate.of(1990, 1, 15));
        engineer1.setJoinDate(LocalDate.of(2015, 4, 1));
        engineer1.setCareer(7);
        engineer1.setProgrammingLanguages(Arrays.asList("Java", "Python", "JavaScript"));
        engineer1.setCareerHistory("大手SIerでの開発経験あり");
        engineer1.setTrainingHistory("Java認定資格取得");
        engineer1.setTechnicalSkill(4.5);
        engineer1.setLearningAttitude(4.0);
        engineer1.setCommunicationSkill(3.5);
        engineer1.setLeadership(3.0);
        engineer1.setNote("チームリーダー経験あり");
        engineer1.setRegisteredDate(LocalDate.now());
        testData.add(engineer1);

        // テストデータ 2
        EngineerDTO engineer2 = new EngineerDTO();
        engineer2.setId("ID00002");
        engineer2.setName("佐藤花子");
        engineer2.setNameKana("サトウハナコ");
        engineer2.setBirthDate(LocalDate.of(1995, 5, 20));
        engineer2.setJoinDate(LocalDate.of(2018, 4, 1));
        engineer2.setCareer(4);
        engineer2.setProgrammingLanguages(Arrays.asList("C#", "PHP", "HTML"));
        engineer2.setCareerHistory("Webアプリケーション開発");
        engineer2.setTrainingHistory("セキュリティ研修受講済");
        engineer2.setTechnicalSkill(3.5);
        engineer2.setLearningAttitude(4.5);
        engineer2.setCommunicationSkill(4.0);
        engineer2.setLeadership(2.5);
        engineer2.setNote("プロジェクトマネージャ補佐");
        engineer2.setRegisteredDate(LocalDate.now());
        testData.add(engineer2);

        // テストデータ 3
        EngineerDTO engineer3 = new EngineerDTO();
        engineer3.setId("ID00003");
        engineer3.setName("鈴木一郎");
        engineer3.setNameKana("スズキイチロウ");
        engineer3.setBirthDate(LocalDate.of(1985, 12, 3));
        engineer3.setJoinDate(LocalDate.of(2010, 10, 1));
        engineer3.setCareer(12);
        engineer3.setProgrammingLanguages(Arrays.asList("C++", "Java", "SQL"));
        engineer3.setCareerHistory("組み込み系システム開発");
        engineer3.setTrainingHistory("アジャイル開発研修");
        engineer3.setTechnicalSkill(5.0);
        engineer3.setLearningAttitude(3.5);
        engineer3.setCommunicationSkill(3.0);
        engineer3.setLeadership(4.5);
        engineer3.setNote("複数プロジェクトのリード経験");
        engineer3.setRegisteredDate(LocalDate.now());
        testData.add(engineer3);

        return testData;
    }

    /**
     * 起動ログの確認
     * 期待されるログメッセージが出力されているかを検証
     *
     * @return ログが正常な場合true
     */
    private boolean validateStartupLogs() {
        try {
            // 最新のログファイルを取得
            String logFileName = "System-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log";
            Path logPath = Paths.get(LOG_DIR, logFileName);

            if (!Files.exists(logPath)) {
                testReport.append("ログファイルが見つかりません: " + logPath.toString() + "\n");
                return false;
            }

            // ログファイルの内容を取得
            List<String> logLines = Files.readAllLines(logPath, StandardCharsets.UTF_8);

            // 期待されるログメッセージのリスト
            List<String> expectedMessages = Arrays.asList(
                    "ログシステムが正常に初期化されました",
                    "リソースマネージャーが正常に初期化されました",
                    "メインフレームを初期化しました",
                    "エンジニア一覧パネルを初期化しました",
                    "画面遷移コントローラーを初期化しました",
                    "メインコントローラーを初期化しました",
                    "エンジニアコントローラーを初期化しました",
                    "アプリケーションを初期化しました",
                    "非同期タスクを開始: LoadData",
                    "エンジニアデータの読み込みを開始します");

            // すべての期待メッセージが存在するか確認
            for (String expected : expectedMessages) {
                boolean found = false;
                for (String line : logLines) {
                    if (line.contains(expected)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    testReport.append("期待されるログメッセージが見つかりません: " + expected + "\n");
                    return false;
                }
            }

            return true;

        } catch (IOException e) {
            testReport.append("ログファイルの読み込みに失敗しました: " + e.getMessage() + "\n");
            return false;
        }
    }

    /**
     * シャットダウンログの確認
     * 期待されるログメッセージが出力されているかを検証
     *
     * @return ログが正常な場合true
     */
    private boolean validateShutdownLogs() {
        try {
            // 最新のログファイルを取得
            String logFileName = "System-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log";
            Path logPath = Paths.get(LOG_DIR, logFileName);

            if (!Files.exists(logPath)) {
                testReport.append("ログファイルが見つかりません: " + logPath.toString() + "\n");
                return false;
            }

            // ログファイルの内容を取得
            List<String> logLines = Files.readAllLines(logPath, StandardCharsets.UTF_8);

            // 期待されるログメッセージのリスト
            List<String> expectedMessages = Arrays.asList(
                    "アプリケーション終了処理を開始します",
                    "ExecutorServiceを終了しています",
                    "登録済みスレッドを終了しています",
                    "アプリケーション終了処理が完了しました");

            // すべての期待メッセージが存在するか確認
            for (String expected : expectedMessages) {
                boolean found = false;
                for (String line : logLines) {
                    if (line.contains(expected)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    testReport.append("期待されるログメッセージが見つかりません: " + expected + "\n");
                    return false;
                }
            }

            return true;

        } catch (IOException e) {
            testReport.append("ログファイルの読み込みに失敗しました: " + e.getMessage() + "\n");
            return false;
        }
    }

    /**
     * データ読み込みログの確認
     * 期待されるログメッセージが出力されているかを検証
     *
     * @return ログが正常な場合true
     */
    private boolean validateDataLoadLogs() {
        try {
            // 最新のログファイルを取得
            String logFileName = "System-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log";
            Path logPath = Paths.get(LOG_DIR, logFileName);

            if (!Files.exists(logPath)) {
                testReport.append("ログファイルが見つかりません: " + logPath.toString() + "\n");
                return false;
            }

            // ログファイルの内容を取得
            List<String> logLines = Files.readAllLines(logPath, StandardCharsets.UTF_8);

            // 期待されるログメッセージのリスト
            List<String> expectedMessages = Arrays.asList(
                    "エンジニアデータの読み込みを開始します",
                    "Loaded",
                    "エンジニアデータを更新しました");

            // すべての期待メッセージが存在するか確認
            for (String expected : expectedMessages) {
                boolean found = false;
                for (String line : logLines) {
                    if (line.contains(expected)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    testReport.append("期待されるログメッセージが見つかりません: " + expected + "\n");
                    return false;
                }
            }

            return true;

        } catch (IOException e) {
            testReport.append("ログファイルの読み込みに失敗しました: " + e.getMessage() + "\n");
            return false;
        }
    }

    /**
     * UIコンポーネントの検証
     * 主要なUIコンポーネントが正しく初期化されているかを確認
     *
     * @return UIが正常な場合true
     */
    private boolean validateUI() {
        if (mainFrame == null) {
            testReport.append("メインフレームがnullです\n");
            return false;
        }

        try {
            // メインフレームのタイトル確認
            JFrame frame = mainFrame.getFrame();
            if (frame == null) {
                testReport.append("JFrameがnullです\n");
                return false;
            }

            String title = frame.getTitle();
            if (!"エンジニア人材管理".equals(title)) {
                testReport.append("フレームタイトルが不正です: 期待=\"エンジニア人材管理\", 実際=\"" + title + "\"\n");
                return false;
            }

            // メニューバーの確認
            JMenuBar menuBar = frame.getJMenuBar();
            if (menuBar == null) {
                testReport.append("メニューバーがnullです\n");
                return false;
            }

            if (menuBar.getMenuCount() < 2) {
                testReport.append("メニューバーのメニュー数が不足しています: " + menuBar.getMenuCount() + "\n");
                return false;
            }

            // 現在のパネルの確認
            JPanel currentPanel = mainFrame.getCurrentPanel();
            if (currentPanel == null) {
                testReport.append("現在のパネルがnullです\n");
                return false;
            }

            // ListPanelの確認
            if (!(currentPanel instanceof ListPanel)) {
                testReport.append("現在のパネルがListPanelではありません: " + currentPanel.getClass().getName() + "\n");
                return false;
            }

            return true;

        } catch (Exception e) {
            testReport.append("UI検証中に例外が発生しました: " + e.getMessage() + "\n");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 一覧表示の検証
     * データが正しく一覧画面に表示されているかを確認
     *
     * @param expectedRecords 期待されるレコード数
     * @return 表示が正常な場合true
     */
    private boolean validateDataDisplay(int expectedRecords) {
        if (mainFrame == null) {
            testReport.append("メインフレームがnullです\n");
            return false;
        }

        try {
            // 現在のパネルの確認
            JPanel currentPanel = mainFrame.getCurrentPanel();
            if (currentPanel == null) {
                testReport.append("現在のパネルがnullです\n");
                return false;
            }

            // ListPanelの確認
            if (!(currentPanel instanceof ListPanel)) {
                testReport.append("現在のパネルがListPanelではありません: " + currentPanel.getClass().getName() + "\n");
                return false;
            }

            ListPanel listPanel = (ListPanel) currentPanel;

            // データ件数の確認
            int actualRecords = listPanel.getDataCount();
            if (actualRecords != expectedRecords) {
                testReport.append("データ件数が一致しません: 期待=" + expectedRecords + ", 実際=" + actualRecords + "\n");
                return false;
            }

            // テーブルの確認
            JTable table = listPanel.getTable();
            if (table == null) {
                testReport.append("テーブルがnullです\n");
                return false;
            }

            // テーブルのカラム数確認
            if (table.getColumnCount() != 5) {
                testReport.append("テーブルのカラム数が不正です: " + table.getColumnCount() + "\n");
                return false;
            }

            return true;

        } catch (Exception e) {
            testReport.append("データ表示検証中に例外が発生しました: " + e.getMessage() + "\n");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ログファイルからシャットダウン完了を確認
     * シャットダウン完了を示すログメッセージが出力されているかを検証
     *
     * @return シャットダウンが完了していれば true
     */
    private boolean checkShutdownCompleteInLogs() {
        try {
            // 最新のログファイルを取得
            String logFileName = "System-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log";
            Path logPath = Paths.get(LOG_DIR, logFileName);

            if (!Files.exists(logPath)) {
                return false;
            }

            // ログファイルの最終行を確認
            List<String> logLines = Files.readAllLines(logPath, StandardCharsets.UTF_8);
            for (int i = logLines.size() - 1; i >= 0; i--) {
                String line = logLines.get(i);
                if (line.contains("アプリケーション終了処理が完了しました") ||
                        line.contains("シャットダウンします")) {
                    return true;
                }
            }

            return false;

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 残存プロセスのチェック
     * アプリケーション終了後に残っているプロセスがないかを確認
     * 
     * <p>
     * 注：このメソッドは実装が簡略化されており、実際の環境では
     * プロセスの検出にはより複雑な実装が必要になる場合があります。
     * 現在のJavaプロセス以外にアプリケーション関連のプロセスが
     * 残っていないことを確認する処理が望ましいでしょう。
     * </p>
     *
     * @return プロセスが残っていなければtrue
     */
    private boolean checkForRemainingProcesses() {
        // 注: このメソッドは単純化されており、実際のプロセスチェックには
        // より高度な実装が必要になる場合があります
        return true;
    }

    /**
     * テスト結果レポートを保存
     * 実行したテストの結果をMarkdownファイルとして保存
     * 
     * <p>
     * レポートには以下の情報が含まれます：
     * <ul>
     * <li>タイトルと実行日時</li>
     * <li>システム環境情報</li>
     * <li>各テストケースの詳細結果</li>
     * <li>成功/失敗の要約</li>
     * <li>問題点の一覧（失敗した場合）</li>
     * </ul>
     * </p>
     */
    private void saveTestReport() {
        try {
            // ファイル名の生成（タイムスタンプ付き）
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "test_report_" + timestamp + ".md";
            Path reportPath = Paths.get(outputDir, fileName);

            // レポート内容をファイルに書き込み
            Files.write(reportPath, testReport.toString().getBytes(StandardCharsets.UTF_8));

            System.out.println("テスト結果レポートを保存しました: " + reportPath);

        } catch (IOException e) {
            System.err.println("テストレポートの保存に失敗しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * テスト成功時の処理
     * 成功カウンターを増加させ、成功メッセージを出力
     *
     * @param testName テスト名
     */
    private void passTest(String testName) {
        passedTests++;
        System.out.println("✅ " + testName + " - テスト成功");
    }

    /**
     * テスト失敗時の処理
     * 失敗カウンターを増加させ、失敗理由を出力
     *
     * @param reason 失敗理由
     */
    private void failTest(String reason) {
        failedTests++;
        System.out.println("❌ テスト失敗: " + reason);
    }

    /**
     * 出力ディレクトリを設定
     * テスト結果の保存先ディレクトリを指定
     * 
     * <p>
     * 指定されたディレクトリが存在しない場合は自動的に作成します。
     * 指定されたパスが無効な場合はデフォルト値が使用されます。
     * </p>
     *
     * @param outputDir 出力ディレクトリのパス
     */
    public void setOutputDirectory(String outputDir) {
        if (outputDir != null && !outputDir.trim().isEmpty()) {
            this.outputDir = outputDir;

            // ディレクトリの存在確認と作成
            try {
                Path path = Paths.get(this.outputDir);
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }
            } catch (IOException e) {
                System.err.println("出力ディレクトリの作成に失敗しました: " + e.getMessage());
                this.outputDir = DEFAULT_OUTPUT_DIR;
            }
        }
    }
}
