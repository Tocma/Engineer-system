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
    private static final long MAX_LOG_SIZE = 10 * 1024 * 1024;
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
                } catch (IOException _e) {
                    System.err.println("LogHandlerの初期化に失敗: " + _e.getMessage());
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

        } catch (IOException _e) {
            cleanup();
            throw new IOException("ログシステムの初期化に失敗", _e);
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

            // JVMの終了時にファイルを削除するように登録
            lockFilePath.toFile().deleteOnExit();
            System.out.println("ログファイルロックを取得: " + lockFilePath);

        } catch (IOException _e) {
            if (lockChannel != null) {
                try {
                    lockChannel.close();
                } catch (IOException closeError) {
                    // クローズエラーは無視
                }
            }
            throw _e;
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

        } catch (IOException _e) {
            System.err.println("ログファイルハンドラーの作成に失敗: " + _e.getMessage());
            throw _e;
        }
    }

    private void cleanupExistingFileHandler() {
        if (fileHandler != null) {
            try {
                logger.removeHandler(fileHandler);
                fileHandler.flush();
                fileHandler.close();
            } catch (Exception _e) {
                System.err.println("既存のFileHandlerのクリーンアップに失敗: " + _e.getMessage());
            } finally {
                fileHandler = null;
            }
        }
    }

    /**
     * 循環参照を防ぐ日付チェック処理
     */
    private synchronized void checkAndRotateLogFile() throws IOException {
        if (isRotating) {
            return;
        }

        LocalDate today = LocalDate.now();
        String currentLogFilePath = getCurrentLogFilePath();

        try {
            // ファイルサイズチェック
            if (currentLogFilePath != null && Files.exists(Paths.get(currentLogFilePath))) {
                long fileSize = Files.size(Paths.get(currentLogFilePath));

                if (fileSize >= MAX_LOG_SIZE) {
                    isRotating = true;
                    rotateLogBySizeLimit(currentLogFilePath);
                    return;
                }
            }

            // 日付ベースのローテーション
            if (currentLogDate == null || !currentLogDate.equals(today)) {
                isRotating = true;
                currentLogDate = today;
                createNewFileHandler();
            }
        } finally {
            isRotating = false;
        }
    }

    private void rotateLogBySizeLimit(String currentLogFilePath) throws IOException {
        log(LogType.SYSTEM, "ログファイルサイズが10MBを超過、ローテーションを実行します");

        // 現在のハンドラーを安全にクローズ
        cleanupExistingFileHandler();

        // アーカイブファイル名を生成
        String archiveFileName = generateArchiveFileName(currentLogFilePath);
        Path archivePath = Paths.get(logDirectory, archiveFileName);

        // 既存ファイルをアーカイブにリネーム
        Files.move(Paths.get(currentLogFilePath), archivePath);

        // 新しいログファイルハンドラーを作成
        createNewFileHandler();

        log(LogType.SYSTEM, "ログローテーション完了: " + archiveFileName);
    }

    private String generateArchiveFileName(String currentLogFilePath) {
        String baseName = new File(currentLogFilePath).getName();
        String nameWithoutExt = baseName.substring(0, baseName.lastIndexOf('.'));
        String extension = baseName.substring(baseName.lastIndexOf('.'));

        // 既存のアーカイブファイル数を確認
        int archiveNumber = 1;
        while (Files.exists(Paths.get(logDirectory, nameWithoutExt + "_" + archiveNumber + extension))) {
            archiveNumber++;
        }

        return nameWithoutExt + "_" + archiveNumber + extension;
    }

    private void logDirectlyToFile(LogType type, String message) {
        if (!isInitialized || logDirectory == null) {
            System.out.println("[" + (type != null ? type.getCode() : "UNKNOWN") + "] " +
                    (message != null ? message : "エラーが発生"));
            return;
        }

        // サイズチェックを無効化して循環参照を防ぐ
        try (java.io.FileWriter writer = new java.io.FileWriter(getCurrentLogFilePath(), true)) {
            String timestamp = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write(String.format("[%s] [%s] %s%n",
                    timestamp,
                    type != null ? type.getCode() : "UNKNOWN",
                    message != null ? message : ""));
            writer.flush();
        } catch (IOException _e) {
            System.err.println("直接ログ書き込みに失敗: " + _e.getMessage());
        }
    }

    public long getCurrentLogFileSize() {
        String currentLogFilePath = getCurrentLogFilePath();
        if (currentLogFilePath != null && Files.exists(Paths.get(currentLogFilePath))) {
            try {
                return Files.size(Paths.get(currentLogFilePath));
            } catch (IOException _e) {
                logError(LogType.SYSTEM, "ログファイルサイズの取得に失敗", _e);
            }
        }
        return 0;
    }

    public void cleanupOldArchives(int maxArchives) {
        if (logDirectory == null)
            return;

        try {
            Path logDir = Paths.get(logDirectory);
            Files.list(logDir)
                    .filter(path -> path.getFileName().toString().matches(".*_\\d+\\.log$"))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException _e) {
                            return 0;
                        }
                    })
                    .skip(maxArchives)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            log(LogType.SYSTEM, "古いアーカイブファイルを削除: " + path.getFileName());
                        } catch (IOException _e) {
                            logError(LogType.SYSTEM, "アーカイブファイル削除に失敗: " + path.getFileName(), _e);
                        }
                    });
        } catch (IOException _e) {
            logError(LogType.SYSTEM, "アーカイブクリーンアップ処理でエラー", _e);
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
        } catch (IOException _e) {
            System.err.println("ログローテーションに失敗: " + _e.getMessage());
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
        } catch (IOException _e) {
            System.err.println("ログローテーションに失敗: " + _e.getMessage());
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

        deleteLockFiles();

        isInitialized = false;
        System.out.println("ログシステムのクリーンアップ完了");
    }

    /**
     * アプリケーション終了時にロックファイルを削除します。
     */
    private void deleteLockFiles() {
        if (logDirectory == null) {
            return;
        }

        // System-YYYY-MM-DD.log.lck の削除
        try {
            String logFileName = getCurrentLogFileName();
            if (logFileName != null && !logFileName.isEmpty()) {
                Path lckFilePath = Paths.get(logDirectory, logFileName + ".lck");
                if (Files.exists(lckFilePath)) {
                    Files.delete(lckFilePath);
                    System.out.println(lckFilePath.getFileName() + " を削除しました。");
                }
            }
        } catch (Exception _e) {
            // ファイル名取得でエラーになる可能性も考慮
            System.err.println("ログの.lckファイルの削除に失敗しました: " + _e.getMessage());
        }
    }

    private void releaseLogFileLock() {
        if (logFileLock != null) {
            try {
                logFileLock.release();
                System.out.println("ログファイルロックを解放");
            } catch (IOException _e) {
                System.err.println("ログファイルロックの解放に失敗: " + _e.getMessage());
            } finally {
                logFileLock = null;
            }
        }

        if (lockChannel != null) {
            try {
                lockChannel.close();
            } catch (IOException _e) {
                System.err.println("ロックチャンネルのクローズに失敗: " + _e.getMessage());
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