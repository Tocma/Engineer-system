package util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

/**
 * エンジニア情報管理システムのログ管理を行うシングルトンクラス
 * 
 * <p>
 * このクラスは、システム全体のログ出力を一元管理し、日付ベースのログファイル名や
 * ログローテーション、適切なフォーマットなどの機能を提供します。ログTypeとして
 * 「UI」と「SYSTEM」を区別し、ログの分類を明確にします。
 * </p>
 * 
 * <p>
 * クラスパスベースのログディレクトリ設定:
 * <ul>
 * <li>開発環境とデプロイ環境の両方で動作</li>
 * <li>ポータブルなパス設定</li>
 * <li>設定ファイルでのカスタマイズ可能</li>
 * <li>フォールバック機能付き</li>
 * </ul>
 * </p>
 * 
 * @author Your Name
 * @version 4.0 - クラスパスベース対応
 * @since 2025-06-13
 */
public class LogHandler {

    /**
     * ログの種類を定義するEnum
     * システム内のログをUIとSYSTEMに分類します
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
    private static final String DEFAULT_LOG_SUBDIR = "logs";
    private static final String LOG_FILE_FORMAT = "System-%s.log";
    private static final int MAX_LOG_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final String LOG_FORMAT = "[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$s] [%7$s] %5$s%6$s%n";

    /** システムプロパティのキー */
    private static final String LOG_DIR_PROPERTY = "engineer.system.log.dir";

    /** ロガー設定 */
    private Logger logger;
    private boolean initialized;
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
     * クラスパスベースの自動検出でロガーを初期化
     * 以下の優先順位でログディレクトリを決定します：
     * 1. システムプロパティ engineer.system.log.dir
     * 2. クラスパスルート + logs
     * 3. ワーキングディレクトリ + logs
     * 
     * @throws IOException ログディレクトリの作成や設定に失敗した場合
     */
    public synchronized void initialize() throws IOException {
        String logDir = determineLogDirectory();
        initialize(logDir);
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

        if (initialized) {
            return;
        }

        try {
            // ログディレクトリのセットアップ
            this.logDirectory = setupLogDirectory(logDir);

            // ロガーの設定
            configureLogger();

            // 初期化完了
            initialized = true;

            // 初期化完了のログを出力
            log(LogType.SYSTEM, "ログシステムが正常に初期化されました (ディレクトリ: " + this.logDirectory + ")");

        } catch (IOException e) {
            System.err.println("ログシステムの初期化に失敗しました: " + e.getMessage());
            throw new IOException("ログシステムの初期化に失敗しました", e);
        }
    }

    /**
     * クラスパスベースでログディレクトリを自動決定
     * 複数の方法を試行し、最も適切なディレクトリを選択します
     * 
     * @return 決定されたログディレクトリのパス
     */
    private String determineLogDirectory() {
        // 1. システムプロパティをチェック
        String systemProperty = System.getProperty(LOG_DIR_PROPERTY);
        if (systemProperty != null && !systemProperty.trim().isEmpty()) {
            System.out.println("システムプロパティからログディレクトリを取得: " + systemProperty);
            return systemProperty;
        }

        // 2. クラスパスルートから相対パスを構築
        try {
            String classpathBasedDir = getClasspathBasedLogDirectory();
            if (classpathBasedDir != null) {
                System.out.println("クラスパスベースのログディレクトリを使用: " + classpathBasedDir);
                return classpathBasedDir;
            }
        } catch (Exception e) {
            System.err.println("クラスパスベースのディレクトリ取得に失敗: " + e.getMessage());
        }

        // 3. ワーキングディレクトリをフォールバックとして使用
        String workingDir = System.getProperty("user.dir") + File.separator + DEFAULT_LOG_SUBDIR;
        System.out.println("フォールバック: ワーキングディレクトリベースを使用: " + workingDir);
        return workingDir;
    }

    /**
     * クラスパスルートからログディレクトリのパスを構築
     * 開発環境とデプロイ環境の両方に対応
     * 
     * @return クラスパスベースのログディレクトリパス、取得できない場合はnull
     * @throws URISyntaxException URIの変換に失敗した場合
     */
    private String getClasspathBasedLogDirectory() throws URISyntaxException {
        // クラスローダーからクラスパスルートを取得
        URI classPathRoot = LogHandler.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI();

        Path classPath = Paths.get(classPathRoot);

        // 開発環境での対応（通常は /target/classes や /bin に配置される）
        if (classPath.toString().contains("target" + File.separator + "classes") ||
                classPath.toString().contains("bin")) {
            // 開発環境: プロジェクトルートに戻る
            Path projectRoot = classPath.getParent().getParent();
            return projectRoot.resolve(DEFAULT_LOG_SUBDIR).toString();
        }

        // JARファイル内での実行（デプロイ環境）
        if (classPath.toString().endsWith(".jar")) {
            // JARファイルと同じディレクトリにlogsフォルダを作成
            Path jarDirectory = classPath.getParent();
            return jarDirectory.resolve(DEFAULT_LOG_SUBDIR).toString();
        }

        // その他の場合: クラスパスと同じ階層にlogsフォルダを作成
        return classPath.resolve(DEFAULT_LOG_SUBDIR).toString();
    }

    /**
     * ログディレクトリを設定
     * 指定されたディレクトリが存在しない場合は作成します
     * 
     * @param logDir ログディレクトリのパス
     * @return 作成されたログディレクトリの絶対パス
     * @throws IOException ディレクトリの作成に失敗した場合
     */
    private String setupLogDirectory(String logDir) throws IOException {
        Path logPath = Paths.get(logDir).toAbsolutePath();
        if (!Files.exists(logPath)) {
            Files.createDirectories(logPath);
            System.out.println("ログディレクトリを作成しました: " + logPath);
        }
        return logPath.toString();
    }

    /**
     * ロガーの設定
     * 日付ベースのログファイル名とフォーマットを設定します
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
        String logFilePath = logDirectory + File.separator + logFileName;

        // FileHandlerの設定
        fileHandler = new FileHandler(logFilePath, MAX_LOG_SIZE_BYTES, 1, true);
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
        checkInitialized();
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
     * エラーメッセージと例外情報を記録します
     * 
     * @param type      ログタイプ (UI または SYSTEM)
     * @param message   エラーメッセージ
     * @param throwable 発生した例外
     * @throws IllegalStateException    LogHandlerが初期化されていない場合
     * @throws IllegalArgumentException メッセージまたは例外がnullの場合
     */
    public synchronized void logError(LogType type, String message, Throwable throwable) {
        checkInitialized();
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
     * 初期化状態をチェック
     * 
     * @throws IllegalStateException 初期化されていない場合
     */
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("LogHandlerが初期化されていません。initialize()メソッドを先に呼び出してください。");
        }
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
     * ロガーのクリーンアップ
     * アプリケーション終了時に呼び出して、リソースを適切に解放します
     */
    public synchronized void cleanup() {
        if (fileHandler != null) {
            if (initialized) {
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
        return initialized;
    }

    /**
     * ログディレクトリのパスを取得
     * 
     * @return ログディレクトリの絶対パス
     */
    public String getLogDirectory() {
        return logDirectory;
    }

    /**
     * デバッグ用: 現在のクラスパス情報を表示
     * 開発時のトラブルシューティングに使用
     */
    public static void printClasspathInfo() {
        try {
            URI classPathRoot = LogHandler.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            System.out.println("=== クラスパス情報 ===");
            System.out.println("クラスパスルート: " + classPathRoot);
            System.out.println("ワーキングディレクトリ: " + System.getProperty("user.dir"));
            System.out.println("システムプロパティ " + LOG_DIR_PROPERTY + ": " +
                    System.getProperty(LOG_DIR_PROPERTY, "未設定"));
            System.out.println("==================");
        } catch (URISyntaxException e) {
            System.err.println("クラスパス情報の取得に失敗: " + e.getMessage());
        }
    }
}