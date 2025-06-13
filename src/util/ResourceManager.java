package util;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import util.LogHandler.LogType;
import util.Constants.FileConstants;

/**
 * クラスパスベースのアプリケーションリソース管理シングルトンクラス
 * 従来のファイルシステム依存から、クラスパス内リソースアクセスに変更
 * より安全で可搬性の高いリソース管理を実現
 *
 * @author Nakano
 */
public class ResourceManager {

    /** シングルトンインスタンス */
    private static final ResourceManager INSTANCE = new ResourceManager();

    /** CSVヘッダー定義 */
    private static final String DEFAULT_CSV_HEADER = "社員ID(必須),氏名(必須),フリガナ(必須),生年月日(必須),入社年月(必須),エンジニア歴(必須),扱える言語(必須),経歴,研修の受講歴,技術力,受講態度,コミュニケーション能力,リーダーシップ,備考";

    /** オープンしているリソースの追跡用マップ */
    private final Map<String, Closeable> openResources = new ConcurrentHashMap<>();

    /** 初期化済みフラグ */
    private boolean initialized = false;

    /** クラスローダー（リソースアクセス用） */
    private ClassLoader classLoader;

    /** 一時ディレクトリパス（書き込み用） */
    private Path projectDirectoryPath;
    private Path projectDataDirectoryPath;
    private Path projectEngineerCsvPath;

    /**
     * プライベートコンストラクタ
     * シングルトンパターンを実現するため、外部からのインスタンス化を防止
     */
    private ResourceManager() {
        // クラスローダーを取得（コンテキストクラスローダーを優先）
        this.classLoader = Thread.currentThread().getContextClassLoader();
        if (this.classLoader == null) {
            // フォールバック：このクラスのクラスローダーを使用
            this.classLoader = ResourceManager.class.getClassLoader();
        }
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
     * クラスパスベースのリソース管理システムを構築
     * 
     * @throws IOException リソースアクセスに失敗した場合
     */
    public void initialize() throws IOException {
        if (initialized) {
            return;
        }

        try {
            logInfo("クラスパスベースのResourceManagerを初期化開始");

            // 一時ディレクトリの設定（書き込み可能な場所）
            setupProjectDirectories();

            // クラスパス内のリソースの確認
            verifyClasspathResources();

            // デフォルトCSVファイルの準備
            prepareDefaultCsvFile();

            // 初期化完了
            initialized = true;

            logInfo("クラスパスベースのResourceManagerを初期化完了");
            logResourcePaths();

        } catch (IOException e) {
            logError("ResourceManagerの初期化に失敗", e);
            throw new IOException("ResourceManagerの初期化に失敗", e);
        }
    }

    /**
     * 一時ディレクトリの設定
     * 書き込み操作用の一時領域を準備
     */
    private void setupProjectDirectories() throws IOException {
        // プロジェクトルートディレクトリを取得
        String projectRoot = System.getProperty("user.dir");
        this.projectDirectoryPath = Paths.get(projectRoot);
        this.projectDataDirectoryPath = projectDirectoryPath.resolve("src").resolve(FileConstants.DATA_DIR_NAME);
        this.projectEngineerCsvPath = projectDataDirectoryPath.resolve(FileConstants.DEFAULT_ENGINEER_CSV);

        createDirectoryIfNotExists(projectDataDirectoryPath);
    }

    /**
     * クラスパス内のリソース確認
     * 必要なリソースがクラスパス内に存在することを確認
     */
    private void verifyClasspathResources() {
        logInfo("クラスパス内リソースの確認を開始");

        // リソースディレクトリの存在確認（任意）
        URL dataResource = classLoader.getResource("data/");
        if (dataResource != null) {
            logInfo("データリソースディレクトリを確認: " + dataResource.toString());
        } else {
            logInfo("データリソースディレクトリは存在しません（必要に応じて作成されます）");
        }

        // その他の重要なリソースファイルの確認
        checkOptionalResource("data/" + FileConstants.DEFAULT_ENGINEER_CSV);
        checkOptionalResource("config/app.properties"); // 設定ファイルの例
    }

    /**
     * オプショナルリソースの確認
     */
    private void checkOptionalResource(String resourcePath) {
        URL resource = classLoader.getResource(resourcePath);
        if (resource != null) {
            logInfo("リソースを確認: " + resourcePath + " -> " + resource.toString());
        } else {
            logInfo("オプショナルリソース未検出: " + resourcePath);
        }
    }

    /**
     * デフォルトCSVファイルの準備
     * クラスパス内のリソースまたは一時ディレクトリにCSVファイルを準備
     */
    private void prepareDefaultCsvFile() throws IOException {
        String csvResourcePath = "data/" + FileConstants.DEFAULT_ENGINEER_CSV;
        InputStream csvResourceStream = classLoader.getResourceAsStream(csvResourcePath);

        if (csvResourceStream != null) {
            // クラスパス内にCSVリソースが存在する場合はプロジェクトディレクトリにコピー
            logInfo("クラスパス内のCSVリソースを検出: " + csvResourcePath);
            try (csvResourceStream) {
                copyResourceToTempFile(csvResourceStream, projectEngineerCsvPath); // ←パス変更
                logInfo("CSVリソースをプロジェクトファイルにコピー: " + projectEngineerCsvPath);
            }
        } else {
            // 新規作成
            logInfo("CSVリソースが存在しないため、新規作成: " + projectEngineerCsvPath);
            createInitialCsvFile(projectEngineerCsvPath.toFile()); // ←パス変更
        }
    }

    /**
     * リソースを一時ファイルにコピー
     */
    private void copyResourceToTempFile(InputStream resourceStream, Path targetPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
                BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    /**
     * クラスパス内のリソースを取得
     * リソースパスを指定して、クラスパス内のリソースにアクセス
     * 
     * @param resourcePath リソースパス（"data/engineers.csv" など）
     * @return InputStreamまたはnull（リソースが存在しない場合）
     */
    public InputStream getResourceAsStream(String resourcePath) {
        InputStream stream = classLoader.getResourceAsStream(resourcePath);

        if (stream != null) {
            logInfo("リソースにアクセス: " + resourcePath);
        } else {
            logInfo("リソースが見つかりません: " + resourcePath);
        }

        return stream;
    }

    /**
     * クラスパス内のリソースURLを取得
     * 
     * @param resourcePath リソースパス
     * @return リソースのURLまたはnull
     */
    public URL getResourceURL(String resourcePath) {
        URL url = classLoader.getResource(resourcePath);

        if (url != null) {
            logInfo("リソースURLを取得: " + resourcePath + " -> " + url.toString());
        } else {
            logInfo("リソースURLが見つかりません: " + resourcePath);
        }

        return url;
    }

    /**
     * リソースをテキストとして読み込み
     * クラスパス内のテキストリソースを文字列として取得
     * 
     * @param resourcePath リソースパス
     * @return リソースの内容（文字列）
     * @throws IOException 読み込みに失敗した場合
     */
    public String readResourceAsString(String resourcePath) throws IOException {
        try (InputStream stream = getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("リソースが見つかりません: " + resourcePath);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(System.lineSeparator());
                }
                return content.toString();
            }
        }
    }

    /**
     * ディレクトリが存在しない場合に作成
     * 
     * @param dirPath 作成するディレクトリのパス
     * @throws IOException ディレクトリ作成に失敗した場合
     */
    private void createDirectoryIfNotExists(Path dirPath) throws IOException {
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
                logInfo("ディレクトリを作成: " + dirPath.toString());
            } catch (IOException e) {
                logError("ディレクトリの作成に失敗: " + dirPath, e);
                throw e;
            }
        } else {
            logInfo("既存のディレクトリを使用: " + dirPath);
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
            throw new IllegalStateException("ResourceManagerが初期化されていません");
        }

        try {
            Path newDir = projectDataDirectoryPath.resolve(dirName);
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
     * 初期CSVファイルの作成処理
     * ヘッダー行を含む新しいCSVファイルを作成
     * 
     * @param csvFile 作成するCSVファイル
     * @throws IOException ファイル作成に失敗した場合
     */
    private void createInitialCsvFile(File csvFile) throws IOException {
        String resourceKey = "csv_initial_writer_" + System.currentTimeMillis();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8))) {

            // ResourceManagerにファイルストリームを登録
            registerResource(resourceKey, writer);

            // CSVヘッダーの書き込み
            writer.write(DEFAULT_CSV_HEADER);
            writer.newLine();

            // 処理完了後にResourceManagerからリソースを解除
            releaseResource(resourceKey);

        } catch (IOException e) {
            logError("初期CSVファイルの作成に失敗: " + csvFile.getPath(), e);
            throw e;
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

        if (!failedResources.isEmpty()) {
            logWarning("以下のリソースの解放に失敗: " + String.join(", ", failedResources));
        }

        openResources.clear();
        logInfo("全リソースの解放処理を完了");

        return allSuccess;
    }

    /**
     * 書き込み可能な一時データディレクトリのパスを取得
     * 
     * @return 一時データディレクトリのパス
     */
    public Path getDataDirectoryPath() {
        return projectDataDirectoryPath;
    }

    /**
     * 書き込み可能なエンジニアCSVファイルのパスを取得
     * 
     * @return エンジニアCSVファイルのパス
     */
    public Path getEngineerCsvPath() {
        return projectDataDirectoryPath;
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
     * 一時ディレクトリパスを取得
     * 
     * @return アプリケーションの一時ディレクトリパス
     */
    public Path getTempDirectoryPath() {
        return projectEngineerCsvPath;
    }

    /**
     * 作成したパスをログに出力
     */
    private void logResourcePaths() {
        logInfo("一時ディレクトリ: " + projectDirectoryPath.toString());
        logInfo("データディレクトリ: " + projectDataDirectoryPath.toString());
        logInfo("エンジニアCSVファイル: " + projectEngineerCsvPath.toString());
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