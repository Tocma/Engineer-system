package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

/**
 * エンジニア情報管理システムのログ管理を行うシングルトンクラス
 * プロジェクトのsrcディレクトリ内に絶対パスでログを出力
 *
 * @author Nakano
 * @version 4.8.0
 * @since 2025-05-19
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
    private static final String LOG_DIR_NAME = "logs";
    private static final String LOG_FILE_FORMAT = "System-%s.log";
    private static final int MAX_LOG_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final String LOG_FORMAT = "[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$s] [%7$s] %5$s%6$s%n";

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
     * 
     * @return LogHandlerの唯一のインスタンス
     */
    public static LogHandler getInstance() {
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
        Path defaultLogDir = Paths.get(projectDir, "src", LOG_DIR_NAME).toAbsolutePath();

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
            log(LogType.SYSTEM, "ログシステムが正常に初期化されました: " + this.logDirectory);

        } catch (IOException e) {
            System.err.println("ログシステムの初期化に失敗しました: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("ログシステムの初期化に失敗しました", e);
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
        System.out.println("ログディレクトリを作成します: " + logPath);

        if (!Files.exists(logPath)) {
            try {
                Files.createDirectories(logPath);
                System.out.println("ログディレクトリを作成しました: " + logPath);
            } catch (IOException e) {
                System.err.println("ログディレクトリの作成に失敗しました: " + e.getMessage());
                throw e;
            }
        } else {
            System.out.println("既存のログディレクトリを使用します: " + logPath);
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
        String logFileName = String.format(LOG_FILE_FORMAT, currentDate);
        String logFilePath = new File(logDirectory, logFileName).getAbsolutePath();

        // FileHandlerの設定
        fileHandler = new FileHandler(logFilePath, MAX_LOG_SIZE_BYTES, 1, true);
        fileHandler.setEncoding("UTF-8");
        fileHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                String typeCode = "SYSTEM"; // デフォルト値
                if (record.getParameters() != null && record.getParameters().length > 0
                        && record.getParameters()[0] instanceof String) {
                    typeCode = (String) record.getParameters()[0];
                }

                return String.format(LOG_FORMAT,
                        record.getMillis(),
                        record.getSourceClassName(),
                        record.getSourceMethodName(),
                        record.getLevel().getName(),
                        record.getMessage(),
                        record.getThrown() == null ? "" : "\n" + formatException(record.getThrown()),
                        typeCode);
            }

            private String formatException(Throwable thrown) {
                StringBuilder sb = new StringBuilder();
                sb.append(thrown.toString());
                for (StackTraceElement element : thrown.getStackTrace()) {
                    sb.append("\n\tat ").append(element.toString());
                }
                return sb.toString();
            }
        });

        // ハンドラの追加
        logger.addHandler(fileHandler);
        System.out.println("ログファイルを設定しました: " + logFilePath);
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
     * 
     * @param level   ログレベル
     * @param type    ログタイプ (UI または SYSTEM)
     * @param message ログメッセージ
     * @throws IllegalStateException    LogHandlerが初期化されていない場合
     * @throws IllegalArgumentException メッセージがnullの場合
     */
    public synchronized void log(Level level, LogType type, String message) {
        // 初期化前は標準出力にフォールバック
        if (!isInitialized) {
            System.out.println("[" + level + "][" + type + "] " + message);
            return;
        }

        if (message == null) {
            throw new IllegalArgumentException("ログメッセージがnullです");
        }
        if (type == null) {
            throw new IllegalArgumentException("ログタイプがnullです");
        }

        // typeを含めたログ出力のためのカスタムLogRecordを作成
        LogRecord record = new LogRecord(level, message);
        record.setParameters(new Object[] { type.getCode() });

        logger.log(record);
    }

    /**
     * エラーログを記録
     * エラーメッセージと例外情報を記録
     * 
     * @param type      ログタイプ (UI または SYSTEM)
     * @param message   エラーメッセージ
     * @param throwable 発生した例外
     * @throws IllegalStateException    LogHandlerが初期化されていない場合
     * @throws IllegalArgumentException メッセージまたは例外がnullの場合
     */
    public synchronized void logError(LogType type, String message, Throwable throwable) {
        // 初期化前は標準エラー出力にフォールバック
        if (!isInitialized) {
            System.err.println("[ERROR][" + type + "] " + message);
            if (throwable != null) {
                throwable.printStackTrace();
            }
            return;
        }

        if (message == null || throwable == null) {
            throw new IllegalArgumentException("メッセージと例外情報は必須です");
        }
        if (type == null) {
            throw new IllegalArgumentException("ログタイプがnullです");
        }

        // typeを含めたログ出力のためのカスタムLogRecordを作成
        LogRecord record = new LogRecord(Level.SEVERE, message);
        record.setParameters(new Object[] { type.getCode() });
        record.setThrown(throwable);

        logger.log(record);
    }

    /**
     * 現在のログファイル名を取得
     * 
     * @return 現在の日付に対応するログファイル名
     */
    public String getCurrentLogFileName() {
        return String.format(LOG_FILE_FORMAT,
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