package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;
import util.Constants.FileConstants;

/**
 * エンジニア情報管理システムのログ管理を行うシングルトンクラス
 * 自動初期化機能と強化されたエラーハンドリング、詳細な呼び出し元情報を提供
 *
 * @author Nakano
 */
public class LogHandler {

    /**
     * ログの種類を定義するEnum
     * システム内のログをUIとSYSTEMに分類
     */
    public enum LogType {
        /** UIに関連するログ（ユーザーインターフェース操作、画面遷移など） */
        UI("UI"),
        /** システムに関連するログ（起動/終了、内部処理、データ操作など） */
        SYSTEM("SYSTEM");

        private final String code;

        LogType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /** シングルトンインスタンス */
    private static final LogHandler INSTANCE = new LogHandler();

    /** ログ関連の定数定義 */

    /** 拡張されたログフォーマット（クラス・メソッド・行番号を先頭に追加） */
    private static final String LOG_FORMAT = "[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$s] [%7$s] [%8$s.%9$s:%10$s] %5$s%6$s%n";

    /** ロガー設定 */
    private Logger logger;
    private boolean isInitialized;
    private String logDirectory;
    private FileHandler fileHandler;

    /**
     * プライベートコンストラクタ
     * シングルトンパターンを実現するため、外部からのインスタンス化を防ぎます
     */
    private LogHandler() {
        // 初期化はinitializeメソッドで行われるため、コンストラクタでは何も行わない
    }

    /**
     * シングルトンインスタンスを取得
     * 初期化されていない場合は自動的に初期化を試みる
     * 
     * @return LogHandlerの唯一のインスタンス
     */
    public static LogHandler getInstance() {
        // まだ初期化されていない場合は自動初期化を試みる
        if (!INSTANCE.isInitialized) {
            try {
                INSTANCE.initialize();
            } catch (IOException e) {
                // 初期化に失敗した場合は標準出力にフォールバック
                System.err.println("ログシステムの自動初期化に失敗: " + e.getMessage());
                System.err.println("標準出力へのフォールバックを使用");
            }
        }
        return INSTANCE;
    }

    /**
     * デフォルトのログディレクトリでロガーを初期化
     * プロジェクトのsrcディレクトリ配下の絶対パスを使用
     * 
     * @throws IOException ログディレクトリの作成や設定に失敗した場合
     */
    public synchronized void initialize() throws IOException {
        // プロジェクトのベースディレクトリを取得
        String projectDir = System.getProperty("user.dir");

        // src/logsへの絶対パスを構築
        Path defaultLogDir = Paths.get(projectDir, "src", FileConstants.LOG_DIR_NAME).toAbsolutePath();

        System.out.println("ログディレクトリの絶対パス: " + defaultLogDir);
        initialize(defaultLogDir.toString());
    }

    /**
     * 指定されたログディレクトリでロガーを初期化
     * ディレクトリの作成、ロガーの設定、フォーマッタの設定を行います
     * 
     * @param logDir ログファイルを格納するディレクトリパス
     * @throws IOException              ログディレクトリの作成や設定に失敗した場合
     * @throws IllegalArgumentException ログディレクトリのパスがnullまたは空の場合
     */
    public synchronized void initialize(String logDir) throws IOException {
        if (logDir == null || logDir.trim().isEmpty()) {
            throw new IllegalArgumentException("ログディレクトリパスが指定されていません");
        }

        if (isInitialized) {
            return;
        }

        try {
            // ログディレクトリのセットアップとロガーの設定
            this.logDirectory = setupLogDirectory(logDir);
            configureLogger();
            // 初期化完了
            isInitialized = true;

            // 初期化完了のログを出力
            log(LogType.SYSTEM, "ログシステムを初期化完了: " + this.logDirectory);

        } catch (IOException e) {
            System.err.println("ログシステムの初期化に失敗: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("ログシステムの初期化に失敗", e);
        }
    }

    /**
     * ログディレクトリを設定
     * 指定されたディレクトリが存在しない場合は作成
     * 
     * @param logDir ログディレクトリのパス
     * @return 作成されたログディレクトリの絶対パス
     * @throws IOException ディレクトリの作成に失敗した場合
     */
    private String setupLogDirectory(String logDir) throws IOException {
        Path logPath = Paths.get(logDir).toAbsolutePath();
        System.out.println("ログディレクトリを作成: " + logPath);

        if (!Files.exists(logPath)) {
            try {
                Files.createDirectories(logPath);
                System.out.println("ログディレクトリを作成: " + logPath);
            } catch (IOException e) {
                System.err.println("ログディレクトリの作成に失敗: " + e.getMessage());
                throw e;
            }
        } else {
            System.out.println("既存のログディレクトリを使用: " + logPath);
        }

        return logPath.toString();
    }

    /**
     * ロガーの設定
     * 日付ベースのログファイル名とフォーマットを設定
     * 
     * @throws IOException 設定に失敗した場合
     */
    private void configureLogger() throws IOException {
        // ロガーの取得
        logger = Logger.getLogger(LogHandler.class.getName());
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false); // 親ハンドラを使用しない

        // 既存のハンドラをすべて削除
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }

        // 現在の日付でログファイル名を生成
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String logFileName = String.format(FileConstants.LOG_FILE_FORMAT, currentDate);
        String logFilePath = new File(logDirectory, logFileName).getAbsolutePath();

        // FileHandlerの設定
        fileHandler = new FileHandler(logFilePath, FileConstants.MAX_LOG_SIZE_BYTES, 1, true);
        fileHandler.setEncoding("UTF-8");
        fileHandler.setFormatter(new DetailedFormatter());

        // ハンドラの追加
        logger.addHandler(fileHandler);
        System.out.println("ログファイルを設定: " + logFilePath);
    }

    /**
     * 詳細な呼び出し元情報を含むカスタムフォーマッター
     * クラス名（フルパッケージ）、メソッド名、行番号を含む詳細なログフォーマットを提供
     */
    private static class DetailedFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String typeCode = "SYSTEM"; // デフォルト値
            String className = "Unknown";
            String methodName = "unknown";
            String lineNumber = "0";

            // パラメータからLogType情報を取得
            if (record.getParameters() != null && record.getParameters().length > 0) {
                if (record.getParameters()[0] instanceof String) {
                    typeCode = (String) record.getParameters()[0];
                }

                // 呼び出し元情報が設定されている場合は取得
                if (record.getParameters().length >= 4) {
                    if (record.getParameters()[1] instanceof String) {
                        className = (String) record.getParameters()[1];
                    }
                    if (record.getParameters()[2] instanceof String) {
                        methodName = (String) record.getParameters()[2];
                    }
                    if (record.getParameters()[3] instanceof String) {
                        lineNumber = (String) record.getParameters()[3];
                    }
                }
            }

            return String.format(LOG_FORMAT,
                    record.getMillis(), // %1$ - タイムスタンプ
                    record.getSourceClassName(), // %2$ - 使用しない（予約）
                    record.getSourceMethodName(), // %3$ - 使用しない（予約）
                    record.getLevel().getName(), // %4$ - ログレベル
                    record.getMessage(), // %5$ - メッセージ
                    record.getThrown() == null ? "" : "\n" + formatException(record.getThrown()), // %6$ - 例外情報
                    typeCode, // %7$ - ログタイプ
                    className, // %8$ - クラス名（フルパッケージ）
                    methodName, // %9$ - メソッド名
                    lineNumber); // %10$ - 行番号
        }

        /**
         * 例外情報をフォーマット
         * 
         * @param thrown フォーマットする例外
         * @return フォーマット済み例外文字列
         */
        private String formatException(Throwable thrown) {
            StringBuilder exceptionBuilder = new StringBuilder();
            exceptionBuilder.append(thrown.toString());
            for (StackTraceElement element : thrown.getStackTrace()) {
                exceptionBuilder.append("\n\tat ").append(element.toString());
            }
            return exceptionBuilder.toString();
        }
    }

    /**
     * 呼び出し元の詳細情報を取得
     * スタックトレースを解析して、LogHandlerクラス以外の最初の呼び出し元を特定
     * 
     * @return 呼び出し元情報の配列 [クラス名, メソッド名, 行番号]
     */
    private String[] getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // スタックトレースを遡って、LogHandlerクラス以外の最初の要素を見つける
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();

            // LogHandlerクラス、Threadクラス、およびJavaの内部クラスをスキップ
            if (!className.equals(LogHandler.class.getName()) &&
                    !className.equals(Thread.class.getName()) &&
                    !className.startsWith("java.") &&
                    !className.startsWith("sun.") &&
                    !element.getMethodName().equals("getStackTrace")) {

                return new String[] {
                        className,
                        element.getMethodName(),
                        String.valueOf(element.getLineNumber())
                };
            }
        }

        // 呼び出し元が特定できない場合のデフォルト値
        return new String[] { "Unknown", "unknown", "0" };
    }

    /**
     * 指定されたログTypeでINFOレベルのログを記録
     * 
     * @param type    ログタイプ (UI または SYSTEM)
     * @param message ログメッセージ
     * @throws IllegalStateException    LogHandlerが初期化されていない場合
     * @throws IllegalArgumentException メッセージがnullの場合
     */
    public synchronized void log(LogType type, String message) {
        log(Level.INFO, type, message);
    }

    /**
     * 指定されたレベルとタイプでログを記録
     * 初期化されていない場合は標準出力にフォールバック
     * 
     * @param level   ログレベル
     * @param type    ログタイプ (UI または SYSTEM)
     * @param message ログメッセージ
     */
    public synchronized void log(Level level, LogType type, String message) {
        // 初期化前は標準出力にフォールバック
        if (!isInitialized) {
            System.out.println("[" + level + "][" + (type != null ? type : "UNKNOWN") + "] " +
                    (message != null ? message : "null"));
            return;
        }

        if (message == null) {
            message = "null"; // nullメッセージを許容
        }
        if (type == null) {
            type = LogType.SYSTEM; // デフォルトのタイプを使用
        }

        // 呼び出し元情報を取得
        String[] callerInfo = getCallerInfo();

        // typeと呼び出し元情報を含めたログ出力のためのカスタムLogRecordを作成
        LogRecord record = new LogRecord(level, message);
        record.setParameters(new Object[] {
                type.getCode(), // ログタイプ
                callerInfo[0], // クラス名（フルパッケージ）
                callerInfo[1], // メソッド名
                callerInfo[2] // 行番号
        });

        logger.log(record);
    }

    /**
     * エラーログを記録
     * 初期化されていない場合でも動作するよう強化
     * 
     * @param type      ログタイプ (UI または SYSTEM)
     * @param message   エラーメッセージ
     * @param throwable 発生した例外
     */
    public synchronized void logError(LogType type, String message, Throwable throwable) {
        // 初期化前は標準エラー出力にフォールバック
        if (!isInitialized) {
            System.err.println("[ERROR][" + (type != null ? type : "UNKNOWN") + "] " +
                    (message != null ? message : "エラーが発生"));
            if (throwable != null) {
                throwable.printStackTrace();
            }
            return;
        }

        // nullチェックを強化
        if (message == null) {
            message = "エラーが発生";
        }
        if (type == null) {
            type = LogType.SYSTEM;
        }
        if (throwable == null) {
            // 例外情報がない場合は通常のエラーログとして記録
            log(Level.SEVERE, type, message);
            return;
        }

        // 呼び出し元情報を取得
        String[] callerInfo = getCallerInfo();

        // typeと呼び出し元情報を含めたログ出力のためのカスタムLogRecordを作成
        LogRecord record = new LogRecord(Level.SEVERE, message);
        record.setParameters(new Object[] {
                type.getCode(), // ログタイプ
                callerInfo[0], // クラス名（フルパッケージ）
                callerInfo[1], // メソッド名
                callerInfo[2] // 行番号
        });
        record.setThrown(throwable);

        logger.log(record);
    }

    /**
     * 現在のログファイル名を取得
     * 
     * @return 現在の日付に対応するログファイル名
     */
    public String getCurrentLogFileName() {
        return String.format(FileConstants.LOG_FILE_FORMAT,
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    /**
     * 現在のログファイルパスを取得
     * 
     * @return 現在のログファイルの絶対パス
     */
    public String getCurrentLogFilePath() {
        if (logDirectory == null) {
            return null;
        }
        return new File(logDirectory, getCurrentLogFileName()).getAbsolutePath();
    }

    /**
     * ロガーのクリーンアップ
     * アプリケーション終了時に呼び出して、リソースを適切に解放
     */
    public synchronized void cleanup() {
        if (fileHandler != null) {
            if (isInitialized) {
                log(LogType.SYSTEM, "システムをシャットダウンしています");
            }
            fileHandler.close();
        }
    }

    /**
     * 初期化状態を取得
     * 
     * @return 初期化済みの場合true
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * ログディレクトリのパスを取得
     * 
     * @return ログディレクトリの絶対パス
     */
    public String getLogDirectory() {
        return logDirectory;
    }
}