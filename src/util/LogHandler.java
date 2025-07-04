package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import util.Constants.FileConstants;

/**
 * 日付ローテーション機能付きログハンドラークラス
 * シングルトンパターンによる統一ログ管理と日付跨ぎ時の自動ローテーション機能を提供
 * 
 * @author Nakano
 */
public class LogHandler {

    /** シングルトンインスタンス */
    private static final LogHandler INSTANCE = new LogHandler();

    /** ログ出力フォーマット */
    private static final String LOG_FORMAT = "[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$s] [%7$s] [%8$s.%9$s:%10$s] %5$s%6$s%n";

    /** ロガーインスタンス */
    private Logger logger;

    /** ファイルハンドラー */
    private FileHandler fileHandler;

    /** ログディレクトリパス */
    private String logDirectory;

    /** 初期化フラグ */
    private boolean isInitialized = false;

    /** 現在のログファイルの日付 */
    private LocalDate currentLogDate;

    /**
     * ログタイプ列挙型
     * ログの分類と識別のためのタイプ定義
     */
    public enum LogType {
        /** UIログ */
        UI("UI"),
        /** システムログ */
        SYSTEM("SYSTEM");

        private final String code;

        LogType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * プライベートコンストラクタ（シングルトンパターン）
     */
    private LogHandler() {
        this.logger = Logger.getLogger(LogHandler.class.getName());
        this.currentLogDate = LocalDate.now();
    }

    /**
     * シングルトンインスタンスを取得
     * 
     * @return LogHandlerインスタンス
     */
    public static LogHandler getInstance() {
        try {
            if (!INSTANCE.isInitialized) {
                INSTANCE.initialize();
            }
        } catch (IOException e) {
            if (INSTANCE.isInitialized) {
                System.err.println("ログシステムの初期化に失敗しました: " + e.getMessage());
            } else {
                System.err.println("LogHandlerの初期化が失敗しました。");
                System.err.println("標準出力へのフォールバックを使用");
            }
        }
        return INSTANCE;
    }

    /**
     * デフォルトのログディレクトリでロガーを初期化
     * 
     * @throws IOException ログディレクトリの作成や設定に失敗した場合
     */
    public synchronized void initialize() throws IOException {
        String projectDir = System.getProperty("user.home") + File.separator + "EngineerSystem";
        Path defaultLogDir = Paths.get(projectDir, FileConstants.LOG_DIR_NAME).toAbsolutePath();
        System.out.println("ログディレクトリの絶対パス: " + defaultLogDir);
        initialize(defaultLogDir.toString());
    }

    /**
     * 指定されたログディレクトリでロガーを初期化
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
            this.logDirectory = setupLogDirectory(logDir);
            configureLogger();
            this.currentLogDate = LocalDate.now();
            isInitialized = true;
            log(LogType.SYSTEM, "ログシステムを初期化完了: " + this.logDirectory);
        } catch (IOException e) {
            System.err.println("ログシステムの初期化に失敗: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("ログシステムの初期化に失敗", e);
        }
    }

    /**
     * ログディレクトリを設定
     * 
     * @param logDir ログディレクトリパス
     * @return 設定されたログディレクトリの絶対パス
     * @throws IOException ディレクトリの作成に失敗した場合
     */
    private String setupLogDirectory(String logDir) throws IOException {
        Path logPath = Paths.get(logDir);

        if (!Files.exists(logPath)) {
            Files.createDirectories(logPath);
            System.out.println("ログディレクトリを作成: " + logPath.toAbsolutePath());
        }

        return logPath.toAbsolutePath().toString();
    }

    /**
     * ロガーの設定
     * 
     * @throws IOException ファイルハンドラーの作成に失敗した場合
     */
    private void configureLogger() throws IOException {
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        createNewFileHandler();
    }

    /**
     * 新しいFileHandlerを作成
     * 
     * @throws IOException ファイルハンドラーの作成に失敗した場合
     */
    private void createNewFileHandler() throws IOException {
        LocalDate today = LocalDate.now();
        String logFileName = String.format(FileConstants.LOG_FILE_FORMAT,
                today.format(DateTimeFormatter.ISO_LOCAL_DATE));
        String logFilePath = logDirectory + File.separator + logFileName;
        File logFile = new File(logFilePath);

        // 既に同じ日付のファイルハンドラーが設定されている場合は何もしない
        if (fileHandler != null && today.equals(currentLogDate)) {
            return;
        }

        // 既存のファイルハンドラーをクリーンアップ
        if (fileHandler != null) {
            logger.removeHandler(fileHandler);
            fileHandler.close();
        }

        // ログファイルの存在チェックと状態確認
        boolean fileExists = logFile.exists();

        // 新しいFileHandlerを作成（既存ファイルがある場合は追記モード）
        fileHandler = new FileHandler(logFilePath, true);
        fileHandler.setFormatter(new DetailedFormatter());
        logger.addHandler(fileHandler);

        // ログファイルの状態をコンソールに出力
        if (fileExists) {
            System.out.println("既存のログファイルに追記: " + logFilePath);
        } else {
            System.out.println("新規ログファイルを作成: " + logFilePath);
        }
    }

    /**
     * 日付が変わった場合にログファイルをローテーション
     * 
     * @throws IOException 新しいファイルハンドラーの作成に失敗した場合
     */
    private synchronized void checkAndRotateLogFile() throws IOException {
        LocalDate today = LocalDate.now();

        if (!today.equals(currentLogDate)) {
            rotateLogFile(today);
        }
    }

    /**
     * ログファイルをローテーション
     * 
     * @param newDate 新しい日付
     * @throws IOException 新しいファイルハンドラーの作成に失敗した場合
     */
    private void rotateLogFile(LocalDate newDate) throws IOException {
        // 現在の日付でログローテーション開始メッセージを記録
        if (fileHandler != null) {
            log(LogType.SYSTEM, "日付が変更されました。ログファイルをローテーションします: " +
                    currentLogDate + " → " + newDate);
        }

        // 日付を更新
        currentLogDate = newDate;

        // 新しい日付のファイルハンドラーを作成
        createNewFileHandler();

        // 新しいファイルでローテーション完了メッセージを記録
        log(LogType.SYSTEM, "ログファイルローテーション完了: " + getCurrentLogFileName());
    }

    /**
     * 指定されたログタイプでINFOレベルのログを記録
     * 
     * @param type    ログタイプ
     * @param message ログメッセージ
     */
    public synchronized void log(LogType type, String message) {
        log(Level.INFO, type, message);
    }

    /**
     * 指定されたレベルとタイプでログを記録
     * 
     * @param level   ログレベル
     * @param type    ログタイプ
     * @param message ログメッセージ
     */
    public synchronized void log(Level level, LogType type, String message) {
        if (!isInitialized) {
            System.out.println("[" + level + "][" + (type != null ? type : "UNKNOWN") + "] " +
                    (message != null ? message : "エラーが発生"));
            return;
        }

        try {
            checkAndRotateLogFile();
        } catch (IOException e) {
            System.err.println("ログローテーションに失敗: " + e.getMessage());
        }

        if (message == null) {
            message = "エラーが発生";
        }
        if (type == null) {
            type = LogType.SYSTEM;
        }

        String[] callerInfo = getCallerInfo();

        LogRecord record = new LogRecord(level, message);
        record.setParameters(new Object[] {
                type.getCode(),
                callerInfo[0],
                callerInfo[1],
                callerInfo[2]
        });

        logger.log(record);
    }

    /**
     * エラーログを記録
     * 
     * @param type      ログタイプ
     * @param message   ログメッセージ
     * @param throwable 例外情報
     */
    public synchronized void logError(LogType type, String message, Throwable throwable) {
        if (!isInitialized) {
            System.out.println("[SEVERE][" + (type != null ? type : "UNKNOWN") + "] " +
                    (message != null ? message : "エラーが発生"));
            if (throwable != null) {
                throwable.printStackTrace();
            }
            return;
        }

        try {
            checkAndRotateLogFile();
        } catch (IOException e) {
            System.err.println("ログローテーションに失敗: " + e.getMessage());
        }

        if (message == null) {
            message = "エラーが発生";
        }
        if (type == null) {
            type = LogType.SYSTEM;
        }
        if (throwable == null) {
            log(Level.SEVERE, type, message);
            return;
        }

        String[] callerInfo = getCallerInfo();

        LogRecord record = new LogRecord(Level.SEVERE, message);
        record.setParameters(new Object[] {
                type.getCode(),
                callerInfo[0],
                callerInfo[1],
                callerInfo[2]
        });
        record.setThrown(throwable);

        logger.log(record);
    }

    /**
     * 呼び出し元の詳細情報を取得
     * 
     * @return 呼び出し元情報の配列 [クラス名, メソッド名, 行番号]
     */
    private String[] getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();

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

        return new String[] { "Unknown", "unknown", "0" };
    }

    /**
     * 詳細な呼び出し元情報を含むカスタムフォーマッター
     */
    private static class DetailedFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String typeCode = "SYSTEM";
            String className = "Unknown";
            String methodName = "unknown";
            String lineNumber = "0";

            if (record.getParameters() != null && record.getParameters().length > 0) {
                if (record.getParameters()[0] instanceof String) {
                    typeCode = (String) record.getParameters()[0];
                }

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
                    record.getMillis(),
                    record.getSourceClassName(),
                    record.getSourceMethodName(),
                    record.getLevel().getName(),
                    record.getMessage(),
                    record.getThrown() == null ? "" : "\n" + formatException(record.getThrown()),
                    typeCode,
                    className,
                    methodName,
                    lineNumber);
        }

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
     * 現在のログファイル名を取得
     * 
     * @return 現在の日付に対応するログファイル名
     */
    public String getCurrentLogFileName() {
        return String.format(FileConstants.LOG_FILE_FORMAT,
                currentLogDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
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
        String logFileName = String.format(FileConstants.LOG_FILE_FORMAT,
                currentLogDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        return new File(logDirectory, logFileName).getAbsolutePath();
    }

    /**
     * ロガーのクリーンアップ
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