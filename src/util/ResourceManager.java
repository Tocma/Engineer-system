package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import util.LogHandler.LogType;

/**
 * アプリケーションリソースを総合的に管理するクラス
 * ファイルやディレクトリの作成、リソースの解放などを担当
 *
 * <p>
 * このクラスは以下の主要な責務を持ちます：
 * <ul>
 * <li>アプリケーションに必要なディレクトリの確認と作成</li>
 * <li>CSVファイルの存在確認と初期ファイルの作成</li>
 * <li>シャットダウン時のリソース解放</li>
 * <li>アプリケーション実行中のリソース管理</li>
 * </ul>
 * </p>
 *
 * <p>
 * リソース管理の主要な機能：
 * <ul>
 * <li>ディレクトリ構造の初期化</li>
 * <li>CSVファイルの作成/存在確認</li>
 * <li>オープンしているリソースの追跡</li>
 * <li>全リソースの解放</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用例：
 * 
 * <pre>
 * // リソースマネージャーの初期化
 * ResourceManager resourceManager = new ResourceManager();
 * resourceManager.initialize();
 *
 * // アプリケーション終了時のリソース解放
 * resourceManager.releaseAllResources();
 * </pre>
 * </p>
 *
 * @author Nakano
 * @version 3.0.0
 * @since 2025-04-04
 */
public class ResourceManager {

    /**
     * デフォルトのCSVディレクトリとファイル名を定義
     */
    private static final String DEFAULT_DATA_DIR = "src/data";
    private static final String DEFAULT_ENGINEER_CSV = "engineers.csv";

    /**
     * CSVヘッダー定義
     */
    private static final String DEFAULT_CSV_HEADER = "id,name,nameKana,birthDate,joinDate,career,programmingLanguages,careerHistory,trainingHistory,technicalSkill,learningAttitude,communicationSkill,leadership,note,registeredDate";

    /**
     * オープンしているリソースの追跡用マップ
     */
    private final Map<String, Closeable> openResources = new ConcurrentHashMap<>();

    /**
     * 初期化済みフラグ
     */
    private boolean initialized = false;

    /**
     * データディレクトリパス
     */
    private Path dataDirectoryPath;

    /**
     * エンジニアCSVファイルパス
     */
    private Path engineerCsvPath;

    /**
     * コンストラクタ
     */
    public ResourceManager() {
        // 初期化はinitializeメソッドで行う
    }

    /**
     * リソースマネージャーを初期化
     * 
     * <p>
     * アプリケーションの実行に必要なディレクトリ構造を確認・作成し、
     * 初期ファイルが存在しない場合は作成
     * </p>
     *
     * @throws IOException ディレクトリやファイルの作成に失敗した場合
     */
    public void initialize() throws IOException {
        if (initialized) {
            return;
        }

        try {
            // ディレクトリパスの設定
            setDirectoryPaths();

            // ディレクトリの確認と作成
            checkAndCreateDirectories();

            // CSVファイルの確認と作成
            checkAndCreateCsvFile();

            // 初期化完了
            initialized = true;

            // ログに記録
            if (LogHandler.getInstance().isInitialized()) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "リソースマネージャーが正常に初期化されました");
            }
        } catch (IOException e) {
            // ログに記録
            if (LogHandler.getInstance().isInitialized()) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "リソースマネージャーの初期化に失敗しました", e);
            }
            throw new IOException("リソースマネージャーの初期化に失敗しました", e);
        }
    }

    /**
     * ディレクトリパスを設定
     */
    private void setDirectoryPaths() {
        dataDirectoryPath = Paths.get(DEFAULT_DATA_DIR).toAbsolutePath();
        engineerCsvPath = dataDirectoryPath.resolve(DEFAULT_ENGINEER_CSV);
    }

    /**
     * 必要なディレクトリを確認し、存在しない場合は作成
     *
     * @throws IOException ディレクトリの作成に失敗した場合
     */
    private void checkAndCreateDirectories() throws IOException {
        try {
            // データディレクトリの確認と作成
            if (!Files.exists(dataDirectoryPath)) {
                Files.createDirectories(dataDirectoryPath);
                if (LogHandler.getInstance().isInitialized()) {
                    LogHandler.getInstance().log(LogType.SYSTEM, dataDirectoryPath.toString());
                }
            }
        } catch (IOException e) {
            if (LogHandler.getInstance().isInitialized()) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "ディレクトリの作成に失敗しました", e);
            }
            throw new IOException("必要なディレクトリの作成に失敗しました", e);
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
                // CSVファイルが存在しない場合、新規作成、try-with-resourcesを使用
                try (BufferedWriter writer = Files.newBufferedWriter(engineerCsvPath, StandardCharsets.UTF_8)) {
                    writer.write(DEFAULT_CSV_HEADER);
                    writer.newLine();
                }

                if (LogHandler.getInstance().isInitialized()) {
                    LogHandler.getInstance().log(LogType.SYSTEM, engineerCsvPath.toString());
                }
            }
        } catch (IOException e) {
            if (LogHandler.getInstance().isInitialized()) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "CSVファイルの作成に失敗しました", e);
            }
            throw new IOException("CSVファイルの作成に失敗しました", e);
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

        if (LogHandler.getInstance().isInitialized()) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "リソースを登録しました: " + key);
        }
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
                if (LogHandler.getInstance().isInitialized()) {
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "リソースを解放しました: " + key);
                }
                return true;
            } catch (IOException e) {
                if (LogHandler.getInstance().isInitialized()) {
                    LogHandler.getInstance().logError(LogType.SYSTEM, "リソースの解放に失敗しました: " + key, e);
                }
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
                if (LogHandler.getInstance().isInitialized()) {
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "リソースを解放しました: " + entry.getKey());
                }
            } catch (IOException e) {
                allSuccess = false;
                failedResources.add(entry.getKey());
                if (LogHandler.getInstance().isInitialized()) {
                    LogHandler.getInstance().logError(LogType.SYSTEM, "リソースの解放に失敗しました: " + entry.getKey(), e);
                }
            }
        }

        // 失敗したリソースがあればログに記録
        if (!failedResources.isEmpty() && LogHandler.getInstance().isInitialized()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "以下のリソースの解放に失敗しました: " + String.join(", ", failedResources));
        }

        // マップをクリア
        openResources.clear();

        if (LogHandler.getInstance().isInitialized()) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "全リソースの解放処理を完了しました");
        }

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
}
