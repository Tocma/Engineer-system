package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import util.LogHandler.LogType;
import util.Constants.FileConstants;

/**
 * アプリケーションリソースを総合的に管理するシングルトンクラス
 * プロジェクトのsrcディレクトリ内に絶対パスでリソースを管理
 *
 * @author Nakano
 */
public class ResourceManager {

    /** シングルトンインスタンス */
    private static final ResourceManager INSTANCE = new ResourceManager();

    /**
     * CSVヘッダー定義
     */
    private static final String DEFAULT_CSV_HEADER = "社員ID(必須),氏名(必須),フリガナ(必須),生年月日(必須),入社年月(必須),エンジニア歴(必須),扱える言語(必須),経歴,研修の受講歴,技術力,受講態度,コミュニケーション能力,リーダーシップ,備考";

    /**
     * オープンしているリソースの追跡用マップ
     */
    private final Map<String, Closeable> openResources = new ConcurrentHashMap<>();

    /**
     * 初期化済みフラグ
     */
    private boolean initialized = false;

    /**
     * ディレクトリパス
     */
    private Path srcDirectoryPath;
    private Path dataDirectoryPath;
    private Path engineerCsvPath;

    /**
     * プライベートコンストラクタ
     * シングルトンパターンを実現するため、外部からのインスタンス化を防止
     */
    private ResourceManager() {
        // 初期化はinitializeメソッドで行う
    }

    /**
     * シングルトンインスタンスを取得
     * 
     * @return ResourceManagerの唯一のインスタンス
     */
    public static ResourceManager getInstance() {
        return INSTANCE;
    }

    /**
     * リソースマネージャーを初期化
     * 
     * @throws IOException ディレクトリやファイルの作成に失敗した場合
     */
    public void initialize() throws IOException {
        if (initialized) {
            return;
        }

        try {
            // プロジェクトのベースディレクトリを取得
            String projectDir = System.getProperty("user.dir");

            // srcディレクトリへの絶対パスを構築
            this.srcDirectoryPath = Paths.get(projectDir, "src").toAbsolutePath();

            System.out.println("プロジェクトディレクトリ: " + projectDir);
            System.out.println("SRCディレクトリの絶対パス: " + srcDirectoryPath);

            // 各ディレクトリパスの設定
            setDirectoryPaths();

            // ディレクトリの確認と作成
            checkAndCreateDirectories();

            // CSVファイルの確認と作成
            checkAndCreateCsvFile();

            // 初期化完了
            initialized = true;

            // ログに記録
            logInfo("リソースマネージャーを初期化完了");
            logPaths();
        } catch (IOException e) {
            // ログに記録
            logError("リソースマネージャーの初期化に失敗", e);
            throw new IOException("リソースマネージャーの初期化に失敗", e);
        }
    }

    /**
     * ディレクトリパスを設定
     */
    private void setDirectoryPaths() {
        dataDirectoryPath = srcDirectoryPath.resolve(FileConstants.DATA_DIR_NAME);
        engineerCsvPath = dataDirectoryPath.resolve(FileConstants.DEFAULT_ENGINEER_CSV);
    }

    /**
     * 作成したパスをログに出力
     */
    private void logPaths() {
        logInfo("SRCディレクトリ: " + srcDirectoryPath.toString());
        logInfo("データディレクトリ: " + dataDirectoryPath.toString());
        logInfo("エンジニアCSVファイル: " + engineerCsvPath.toString());
    }

    /**
     * 必要なディレクトリを確認し、存在しない場合は作成
     *
     * @throws IOException ディレクトリの作成に失敗した場合
     */
    private void checkAndCreateDirectories() throws IOException {
        try {
            // 各ディレクトリの確認と作成
            createDirectoryIfNotExists(srcDirectoryPath);
            createDirectoryIfNotExists(dataDirectoryPath);
        } catch (IOException e) {
            logError("ディレクトリの作成に失敗", e);
            throw new IOException("必要なディレクトリの作成に失敗", e);
        }
    }

    /**
     * ディレクトリが存在しない場合に作成
     * 
     * @param dirPath 作成するディレクトリのパス
     * @throws IOException ディレクトリ作成に失敗した場合
     */
    private void createDirectoryIfNotExists(Path dirPath) throws IOException {
        System.out.println("ディレクトリ確認: " + dirPath);
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
                System.out.println("ディレクトリを作成: " + dirPath.toString());
            } catch (IOException e) {
                System.err.println("ディレクトリの作成に失敗: " + dirPath + ", エラー: " + e.getMessage());
                throw e;
            }
        } else {
            System.out.println("既存のディレクトリを使用: " + dirPath);
        }
    }

    /**
     * CSVファイルの確認と作成
     * ファイルが存在しない場合は、ヘッダー行を持つ新しいファイルを作成
     *
     * @throws IOException ファイルの作成に失敗した場合
     */
    private void checkAndCreateCsvFile() throws IOException {
        try {
            if (!Files.exists(engineerCsvPath)) {
                // 親ディレクトリが存在することを確認
                if (!Files.exists(dataDirectoryPath)) {
                    Files.createDirectories(dataDirectoryPath);
                }

                // CSVファイルが存在しない場合、新規作成、try-with-resourcesを使用
                System.out.println("CSVファイルを作成: " + engineerCsvPath);
                try (BufferedWriter writer = Files.newBufferedWriter(engineerCsvPath, StandardCharsets.UTF_8)) {
                    writer.write(DEFAULT_CSV_HEADER);
                    writer.newLine();
                }

                logInfo("新しいCSVファイルを作成: " + engineerCsvPath.toString());
            } else {
                System.out.println("既存のCSVファイルを使用: " + engineerCsvPath);
            }
        } catch (IOException e) {
            System.err.println("新しいCSVファイルの作成に失敗: " + e.getMessage());
            logError("新しいCSVファイルの作成に失敗", e);
            throw new IOException("新しいCSVファイルの作成に失敗", e);
        }
    }

    /**
     * 指定されたパスに新しいディレクトリを作成
     * 
     * @param dirName 作成するディレクトリ名
     * @return 作成されたディレクトリのパス
     * @throws IOException ディレクトリ作成に失敗した場合
     */
    public Path createDirectory(String dirName) throws IOException {
        if (!initialized) {
            throw new IllegalStateException("リソースマネージャーが初期化されていません");
        }

        try {
            Path newDir = dataDirectoryPath.resolve(dirName);
            if (!Files.exists(newDir)) {
                Files.createDirectories(newDir);
                logInfo("新しいディレクトリを作成: " + newDir.toString());
            }
            return newDir;
        } catch (IOException e) {
            logError("新しいディレクトリの作成に失敗: " + dirName, e);
            throw new IOException("新しいディレクトリの作成に失敗", e);
        }
    }

    /**
     * リソース管理にCloseableオブジェクトを追加
     * シャットダウン時に自動的にクローズされる
     *
     * @param key      リソースを識別するキー
     * @param resource 管理対象のCloseableリソース
     * @throws IllegalArgumentException キーまたはリソースがnullの場合
     */
    public void registerResource(String key, Closeable resource) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("リソースキーがnullまたは空です");
        }
        if (resource == null) {
            throw new IllegalArgumentException("リソースがnullです");
        }

        openResources.put(key, resource);
        logInfo("リソースを登録: " + key);
    }

    /**
     * 特定のリソースを解放
     *
     * @param key 解放するリソースのキー
     * @return 解放が成功した場合はtrue
     */
    public boolean releaseResource(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }

        Closeable resource = openResources.remove(key);
        if (resource != null) {
            try {
                resource.close();
                logInfo("リソースを解放: " + key);
                return true;
            } catch (IOException e) {
                logError("リソースの解放に失敗: " + key, e);
                return false;
            }
        }
        return false;
    }

    /**
     * 全てのリソースを解放
     * シャットダウン時に呼び出す
     *
     * @return 全てのリソースの解放に成功した場合はtrue
     */
    public boolean releaseAllResources() {
        boolean allSuccess = true;
        List<String> failedResources = new ArrayList<>();

        for (Map.Entry<String, Closeable> entry : openResources.entrySet()) {
            try {
                entry.getValue().close();
                logInfo("リソースを解放: " + entry.getKey());
            } catch (IOException e) {
                allSuccess = false;
                failedResources.add(entry.getKey());
                logError("リソースの解放に失敗: " + entry.getKey(), e);
            }
        }

        // 失敗したリソースがあればログに記録
        if (!failedResources.isEmpty()) {
            logWarning("以下のリソースの解放に失敗: " + String.join(", ", failedResources));
        }

        // マップをクリア
        openResources.clear();
        logInfo("全リソースの解放処理を完了");

        return allSuccess;
    }

    /**
     * ゲッター：データディレクトリのパスを取得
     *
     * @return データディレクトリのパス
     */
    public Path getDataDirectoryPath() {
        return dataDirectoryPath;
    }

    /**
     * ゲッター：エンジニアCSVファイルのパスを取得
     *
     * @return エンジニアCSVファイルのパス
     */
    public Path getEngineerCsvPath() {
        return engineerCsvPath;
    }

    /**
     * 初期化済みかどうかを確認
     *
     * @return 初期化されている場合はtrue
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 登録済みリソースの数を取得
     *
     * @return 管理下にあるリソースの数
     */
    public int getResourceCount() {
        return openResources.size();
    }

    /**
     * 特定のリソースが登録されているか確認
     *
     * @param key 確認するリソースのキー
     * @return 登録されている場合はtrue
     */
    public boolean hasResource(String key) {
        return openResources.containsKey(key);
    }

    /**
     * SRCディレクトリパスを取得
     * 
     * @return アプリケーションのSRCディレクトリパス
     */
    public Path getSrcDirectoryPath() {
        return srcDirectoryPath;
    }

    /**
     * INFOログ出力ヘルパーメソッド
     * LogHandlerが初期化されていない場合でも安全にログ出力
     * 
     * @param message ログメッセージ
     */
    private void logInfo(String message) {
        LogHandler logHandler = LogHandler.getInstance();
        if (logHandler.isInitialized()) {
            logHandler.log(Level.INFO, LogType.SYSTEM, message);
        } else {
            System.out.println("[INFO][SYSTEM] " + message);
        }
    }

    /**
     * WARNINGログ出力ヘルパーメソッド
     * 
     * @param message ログメッセージ
     */
    private void logWarning(String message) {
        LogHandler logHandler = LogHandler.getInstance();
        if (logHandler.isInitialized()) {
            logHandler.log(Level.WARNING, LogType.SYSTEM, message);
        } else {
            System.out.println("[WARNING][SYSTEM] " + message);
        }
    }

    /**
     * エラーログ出力ヘルパーメソッド
     * 
     * @param message ログメッセージ
     * @param e       例外オブジェクト
     */
    private void logError(String message, Exception e) {
        LogHandler logHandler = LogHandler.getInstance();
        if (logHandler.isInitialized()) {
            logHandler.logError(LogType.SYSTEM, message, e);
        } else {
            System.err.println("[ERROR][SYSTEM] " + message);
            e.printStackTrace();
        }
    }
}