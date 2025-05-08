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
 * 
 * <p>
 * 主な特徴：
 * <ul>
 * <li>シングルトンパターンによる一元管理</li>
 * <li>ログTypeによる「UI」と「SYSTEM」の明確な区分</li>
 * <li>日単位のログファイル自動生成</li>
 * <li>ログローテーションによる容量管理</li>
 * <li>詳細なエラー情報の記録</li>
 * </ul>
 * </p>
 * 
 * <p>
 * ログファイルは以下の特性を持ちます：
 * <ul>
 * <li>命名規則: System-YYYY-MM-DD.log</li>
 * <li>最大サイズ: 10MB（超過時に自動ローテーション）</li>
 * <li>フォーマット: [日時] [ログレベル] [Type] メッセージ</li>
 * <li>エンコーディング: UTF-8</li>
 * </ul>
 * </p>
 * 
 * <p>
 * 使用例：
 * 
 * <pre>
 * // ログシステムの初期化
 * LogHandler.getInstance().initialize();
 * 
 * // ログTypeを指定したログ出力
 * LogHandler.getInstance().log(LogType.SYSTEM, "システムを起動しました");
 * 
 * // パラメータ付きメッセージ（文字列補間）
 * LogHandler.getInstance().log(LogType.UI, String.format("ユーザー%s（%s）がログインしました", "ID00001", "山田太郎"));
 * 
 * // レベルとTypeを指定したログ出力
 * LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "メモリ使用率が高くなっています");
 * 
 * // エラーログの出力（例外付き）
 * try {
 *     // 処理
 * } catch (Exception e) {
 *     LogHandler.getInstance().logError(LogType.SYSTEM, "処理中にエラーが発生しました", e);
 * }
 * </pre>
 * </p>
 * 
 * @author Nakano
 * @version 4.4.2
 * @since 2025-05-08
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
    private static final String DEFAULT_LOG_DIR = "src/logs";
    private static final String LOG_FILE_FORMAT = "System-%s.log";
    private static final int MAX_LOG_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final String LOG_FORMAT = "[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$s] [%7$s] %5$s%6$s%n";

    /** ロガー設定 */
    private Logger logger;
    private boolean isinitialized;
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
     * 
     * @throws IOException ログディレクトリの作成や設定に失敗した場合
     */
    public synchronized void initialize() throws IOException {
        initialize(DEFAULT_LOG_DIR);
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

        if (isinitialized) {
            return;
        }

        try {
            // ログディレクトリのセットアップ ロガーの設定
            this.logDirectory = setupLogDirectory(logDir);
            configureLogger();
            // 初期化完了
            isinitialized = true;
    
            // 初期化完了のログを出力
            log(LogType.SYSTEM, "ログシステムが正常に初期化されました");
            
        } catch (IOException e) {
            System.err.println("ログシステムの初期化に失敗しました: " + e.getMessage());
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
        if (!Files.exists(logPath)) {
            Files.createDirectories(logPath);
            System.out.println("ログディレクトリを作成しました: " + logPath);
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
     * エラーメッセージと例外情報を記録
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
        if (!isinitialized) {
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
     * アプリケーション終了時に呼び出して、リソースを適切に解放
     */
    public synchronized void cleanup() {
        if (fileHandler != null) {
            if (isinitialized) {
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
        return isinitialized;
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
