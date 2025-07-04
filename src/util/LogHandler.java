package util;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
 */
public class LogHandler {

    private static final LogHandler INSTANCE = new LogHandler();
    private static final String LOG_FORMAT = "[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$s] [%7$s] [%8$s.%9$s:%10$s] %5$s%6$s%n";

    private Logger logger;
    private FileHandler fileHandler;
    private String logDirectory;
    private boolean isInitialized = false;
    private LocalDate currentLogDate;
    private FileLock logFileLock;
    private FileChannel lockChannel;

    // 循環参照防止フラグ
    private volatile boolean isRotating = false;

    // 重複初期化防止のためのロック
    private final Object initializationLock = new Object();

    public enum LogType {
        UI("UI"),
        SYSTEM("SYSTEM");

        private final String code;

        LogType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private LogHandler() {
        this.logger = Logger.getLogger(LogHandler.class.getName());
    }

    public static LogHandler getInstance() {
        synchronized (INSTANCE.initializationLock) {
            if (!INSTANCE.isInitialized) {
                try {
                    INSTANCE.initialize();
                } catch (IOException e) {
                    System.err.println("LogHandlerの初期化に失敗: " + e.getMessage());
                    System.err.println("標準出力へのフォールバックを使用");
                }
            }
        }
        return INSTANCE;
    }

    public synchronized void initialize() throws IOException {
        if (isInitialized) {
            return;
        }

        String projectDir = System.getProperty("user.home") + File.separator + "EngineerSystem";
        Path defaultLogDir = Paths.get(projectDir, FileConstants.LOG_DIR_NAME).toAbsolutePath();
        initialize(defaultLogDir.toString());
    }

    public synchronized void initialize(String logDir) throws IOException {
        if (logDir == null || logDir.trim().isEmpty()) {
            throw new IllegalArgumentException("ログディレクトリパスが指定されていません");
        }

        if (isInitialized) {
            return;
        }

        try {
            this.logDirectory = setupLogDirectory(logDir);
            acquireLogFileLock();
            this.currentLogDate = LocalDate.now();
            configureLogger();
            isInitialized = true;

            log(LogType.SYSTEM, "ログシステムを初期化完了: " + this.logDirectory);

        } catch (IOException e) {
            cleanup();
            throw new IOException("ログシステムの初期化に失敗", e);
        }
    }

    private void acquireLogFileLock() throws IOException {
        String lockFileName = "loghandler.lock";
        Path lockFilePath = Paths.get(logDirectory, lockFileName);

        try {
            lockChannel = FileChannel.open(lockFilePath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);

            logFileLock = lockChannel.tryLock();

            if (logFileLock == null) {
                throw new IOException("ログハンドラーは既に別のプロセスで使用中です");
            }

            System.out.println("ログファイルロックを取得: " + lockFilePath);

        } catch (IOException e) {
            if (lockChannel != null) {
                try {
                    lockChannel.close();
                } catch (IOException closeError) {
                    // クローズエラーは無視
                }
            }
            throw e;
        }
    }

    private void configureLogger() throws IOException {
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        createNewFileHandler();
    }

    private synchronized void createNewFileHandler() throws IOException {
        LocalDate fileDate = currentLogDate != null ? currentLogDate : LocalDate.now();

        String logFileName = String.format(FileConstants.LOG_FILE_FORMAT,
                fileDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        String logFilePath = logDirectory + File.separator + logFileName;

        cleanupExistingFileHandler();

        try {
            fileHandler = new FileHandler(logFilePath, true);
            fileHandler.setFormatter(new DetailedFormatter());
            logger.addHandler(fileHandler);

            System.out.println("ログファイルを設定: " + logFilePath);

        } catch (IOException e) {
            System.err.println("ログファイルハンドラーの作成に失敗: " + e.getMessage());
            throw e;
        }
    }

    private void cleanupExistingFileHandler() {
        if (fileHandler != null) {
            try {
                logger.removeHandler(fileHandler);
                fileHandler.flush();
                fileHandler.close();
            } catch (Exception e) {
                System.err.println("既存のFileHandlerのクリーンアップに失敗: " + e.getMessage());
            } finally {
                fileHandler = null;
            }
        }
    }

    /**
     * 循環参照を防ぐ日付チェック処理
     */
    private synchronized void checkAndRotateLogFile() throws IOException {
        // ローテーション中は再帰チェックを回避
        if (isRotating) {
            return;
        }

        LocalDate today = LocalDate.now();

        if (!today.equals(currentLogDate)) {
            rotateLogFile(today);
        }
    }

    /**
     * 循環参照を防ぐローテーション処理
     */
    private void rotateLogFile(LocalDate newDate) throws IOException {
        // ローテーション開始フラグを設定
        isRotating = true;

        try {
            // ローテーション開始をシステム出力に記録（循環参照を回避）
            System.out.println("[" +
                    java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    +
                    "] [SYSTEM] 日付が変更されました。ログファイルをローテーションします: " +
                    currentLogDate + " → " + newDate);

            currentLogDate = newDate;
            createNewFileHandler();

            // ローテーション完了をシステム出力に記録
            System.out.println("[" +
                    java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    +
                    "] [SYSTEM] ログファイルローテーション完了: " + getCurrentLogFileName());

        } finally {
            // ローテーション終了フラグをリセット
            isRotating = false;
        }

        // ローテーション完了後に通常のログとして記録
        logDirectlyToFile(LogType.SYSTEM, "ログファイルローテーションが正常に完了しました");
    }

    /**
     * 循環参照を回避してファイルに直接ログを記録
     */
    private void logDirectlyToFile(LogType type, String message) {
        if (!isInitialized || fileHandler == null) {
            return;
        }

        try {
            String[] callerInfo = getCallerInfo();

            LogRecord record = new LogRecord(Level.INFO, message);
            record.setParameters(new Object[] {
                    type.getCode(),
                    callerInfo[0],
                    callerInfo[1],
                    callerInfo[2]
            });

            logger.log(record);

        } catch (Exception e) {
            System.err.println("直接ログ記録に失敗: " + e.getMessage());
        }
    }

    public synchronized void log(LogType type, String message) {
        log(Level.INFO, type, message);
    }

    public synchronized void log(Level level, LogType type, String message) {
        if (!isInitialized) {
            System.out.println("[" + level + "][" + (type != null ? type : "UNKNOWN") + "] " +
                    (message != null ? message : "エラーが発生"));
            return;
        }

        // ローテーション中でなければ日付チェックを実行
        try {
            checkAndRotateLogFile();
        } catch (IOException e) {
            System.err.println("ログローテーションに失敗: " + e.getMessage());
        }

        if (message == null)
            message = "エラーが発生";
        if (type == null)
            type = LogType.SYSTEM;

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

        if (message == null)
            message = "エラーが発生";
        if (type == null)
            type = LogType.SYSTEM;

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

    public synchronized void cleanup() {
        if (isInitialized) {
            logDirectlyToFile(LogType.SYSTEM, "ログシステムをシャットダウンしています");
        }

        cleanupExistingFileHandler();
        releaseLogFileLock();

        isInitialized = false;
        System.out.println("ログシステムのクリーンアップ完了");
    }

    private void releaseLogFileLock() {
        if (logFileLock != null) {
            try {
                logFileLock.release();
                System.out.println("ログファイルロックを解放");
            } catch (IOException e) {
                System.err.println("ログファイルロックの解放に失敗: " + e.getMessage());
            } finally {
                logFileLock = null;
            }
        }

        if (lockChannel != null) {
            try {
                lockChannel.close();
            } catch (IOException e) {
                System.err.println("ロックチャンネルのクローズに失敗: " + e.getMessage());
            } finally {
                lockChannel = null;
            }
        }
    }

    private String setupLogDirectory(String logDir) throws IOException {
        Path logPath = Paths.get(logDir);
        if (!Files.exists(logPath)) {
            Files.createDirectories(logPath);
        }
        return logPath.toAbsolutePath().toString();
    }

    private String[] getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (int i = 2; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            if (!element.getClassName().equals(LogHandler.class.getName())) {
                return new String[] {
                        element.getClassName(),
                        element.getMethodName(),
                        String.valueOf(element.getLineNumber())
                };
            }
        }

        return new String[] { "Unknown", "Unknown", "0" };
    }

    public String getCurrentLogFileName() {
        LocalDate fileDate = currentLogDate != null ? currentLogDate : LocalDate.now();
        return String.format(FileConstants.LOG_FILE_FORMAT,
                fileDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    public String getCurrentLogFilePath() {
        if (logDirectory == null)
            return null;
        return new File(logDirectory, getCurrentLogFileName()).getAbsolutePath();
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    // DetailedFormatterクラスは既存のまま
    private static class DetailedFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            Object[] params = record.getParameters();
            String typeCode = params != null && params.length > 0 ? params[0].toString() : "UNKNOWN";
            String className = params != null && params.length > 1 ? params[1].toString() : "Unknown";
            String methodName = params != null && params.length > 2 ? params[2].toString() : "Unknown";
            String lineNumber = params != null && params.length > 3 ? params[3].toString() : "0";

            return String.format(LOG_FORMAT,
                    record.getMillis(),
                    record.getLevel(),
                    record.getMessage(),
                    record.getLevel(),
                    record.getMessage(),
                    record.getThrown() != null ? "\n" + formatException(record.getThrown()) : "",
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
}