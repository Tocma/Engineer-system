package util;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import util.LogHandler.LogType;

/**
 * アプリケーションリソースを総合的に管理するシングルトンクラス
 * プロジェクトのsrcディレクトリ内に絶対パスでリソースを管理
 *
 * @author Nakano
 */
public class ResourceManager {

    /** シングルトンインスタンス */
    private static final ResourceManager INSTANCE = new ResourceManager();

    /** プロパティマネージャーのインスタンス */
    private final PropertiesManager props = PropertiesManager.getInstance();

    /**
     * CSVヘッダー定義
     */
    private static final String DEFAULT_CSV_HEADER = "社員ID(必須),氏名(必須),フリガナ(必須),生年月日(必須),入社年月(必須),エンジニア歴(必須),扱える言語(必須),経歴,研修の受講歴,技術力,受講態度,コミュニケーション能力,リーダーシップ,備考,登録日";

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
            // プロパティからベースディレクトリを取得
            String projectDir = props.getString("directory.base",
                    System.getProperty("user.home") + File.separator + "EngineerSystem");

            // ディレクトリへの絶対パスを構築
            this.srcDirectoryPath = Paths.get(projectDir).toAbsolutePath();

            System.out.println("プロジェクトディレクトリ: " + projectDir);
            System.out.println("SRCディレクトリの絶対パス: " + srcDirectoryPath);

            // 各ディレクトリパスの設定
            this.setDirectoryPaths();

            // ディレクトリの確認と作成
            this.checkAndCreateDirectories();

            // CSVファイルの確認と作成
            this.checkAndCreateCsvFile();

            // 初期化完了
            initialized = true;

        } catch (IOException _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "リソースマネージャーの初期化に失敗", _e);
            throw new IOException("リソースマネージャーの初期化に失敗", _e);
        }
    }

    /**
     * ディレクトリパスを設定
     */
    private void setDirectoryPaths() {
        String dataDir = props.getString("directory.data", "data");
        String csvFileName = props.getString("file.csv.default", "engineers.csv");

        this.dataDirectoryPath = srcDirectoryPath.resolve(dataDir);
        this.engineerCsvPath = dataDirectoryPath.resolve(csvFileName);
    }

    /**
     * 必要なディレクトリを確認し、存在しない場合は作成
     *
     * @throws IOException ディレクトリの作成に失敗した場合
     */
    private void checkAndCreateDirectories() throws IOException {
        try {
            // dataディレクトリのみを作成（srcDirectoryPathは作成しない）
            createDirectoryIfNotExists(dataDirectoryPath);
        } catch (IOException _e) {
            // エラーログを出力
            LogHandler.getInstance().logError(LogType.SYSTEM, "ディレクトリの作成に失敗", _e);

            throw new IOException("必要なディレクトリの作成に失敗", _e);
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
            } catch (IOException _e) {
                System.err.println("ディレクトリの作成に失敗: " + dirPath + ", エラー: " + _e.getMessage());
                throw _e;
            }
        } else {
            System.out.println("既存のディレクトリを使用: " + dirPath);
        }
    }

    /**
     * CSVファイルの確認と作成
     * ファイルが存在しない場合は、ヘッダー行を持つ新しいファイルを作成
     * ファイルが存在する場合は、1行目にヘッダー行を上書き
     *
     * @throws IOException ファイルの作成に失敗した場合
     */
    private void checkAndCreateCsvFile() throws IOException {
        if (!Files.exists(engineerCsvPath)) {
            try (BufferedWriter writer = Files.newBufferedWriter(engineerCsvPath,
                    StandardCharsets.UTF_8)) {
                // プロパティからCSVヘッダーを取得
                String csvHeader = props.getString("csv.header", DEFAULT_CSV_HEADER);
                writer.write(csvHeader);
                writer.newLine();
            }
            System.out.println("新しいCSVファイルを作成: " + engineerCsvPath);
        } else {
            // 既存ファイルの1行目にヘッダー行を上書き
            overwriteHeaderLine();
            System.out.println("既存のCSVファイルのヘッダー行を更新: " + engineerCsvPath);
        }
    }

    /**
     * 既存CSVファイルの1行目にヘッダー行を上書きする処理
     * 
     * @throws IOException ファイルの読み書きに失敗した場合
     */
    private void overwriteHeaderLine() throws IOException {
        try {
            // 現在のファイル内容を全て読み込み
            List<String> lines = Files.readAllLines(engineerCsvPath, StandardCharsets.UTF_8);

            // プロパティからCSVヘッダーを取得
            String csvHeader = props.getString("csv.header", DEFAULT_CSV_HEADER);

            // ファイルが空の場合またはヘッダーのみの場合の処理
            if (lines.isEmpty()) {
                // 空ファイルの場合はヘッダーのみを追加
                try (BufferedWriter writer = Files.newBufferedWriter(engineerCsvPath,
                        StandardCharsets.UTF_8)) {
                    writer.write(csvHeader);
                    writer.newLine();
                }
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "空のCSVファイルにヘッダー行を追加: " + engineerCsvPath);
                return;
            }

            // 1行目をヘッダー行に置き換え
            lines.set(0, csvHeader);

            // ファイルを書き直し
            try (BufferedWriter writer = Files.newBufferedWriter(engineerCsvPath,
                    StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "CSVファイルのヘッダー行を更新: " + engineerCsvPath);

        } catch (IOException _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "CSVファイルのヘッダー行更新に失敗", _e);
            throw new IOException("CSVファイルのヘッダー行更新に失敗", _e);
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
                // ログに新しいディレクトリの作成を記録
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "新しいディレクトリを作成: " + newDir.toString());

            }
            return newDir;
        } catch (IOException _e) {
            // エラーログを出力
            LogHandler.getInstance().logError(LogType.SYSTEM, "新しいディレクトリの作成に失敗: " + dirName, _e);

            throw new IOException("新しいディレクトリの作成に失敗", _e);
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
        // ログに登録情報を記録
        //LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "リソースを登録: " + key);
    };

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
                // ログに解放情報を記録
                //LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "リソースを解放: " + key);

                return true;
            } catch (IOException _e) {
                // エラーログを出力
                LogHandler.getInstance().logError(LogType.SYSTEM, "リソースの解放に失敗: " + key, _e);

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
                // ログに解放情報を記録
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "リソースを解放: " + entry.getKey());

            } catch (IOException _e) {
                allSuccess = false;
                failedResources.add(entry.getKey());
                // エラーログを出力
                LogHandler.getInstance().logError(LogType.SYSTEM, "リソースの解放に失敗: " + entry.getKey(), _e);
            }
        }

        // 失敗したリソースがあればログに記録
        if (!failedResources.isEmpty()) {
            logWarning("以下のリソースの解放に失敗: " + String.join(", ", failedResources));
        }

        // マップをクリア
        openResources.clear();
        // ログに全リソースの解放完了を記録
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "全リソースの解放処理を完了");

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

}