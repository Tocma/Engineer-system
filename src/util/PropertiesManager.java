package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * アプリケーション設定プロパティを管理するシングルトンクラス
 * application.propertiesファイルから設定を読み込み、システム全体で利用
 *
 * @author Nakano
 */
public class PropertiesManager {

    /** シングルトンインスタンス */
    private static final PropertiesManager INSTANCE = new PropertiesManager();

    /** デフォルトプロパティファイル名 */
    private static final String DEFAULT_PROPERTIES_FILE = "application.properties";

    /** プロパティ格納用 */
    private final Properties properties;

    /** キャッシュ（型変換済み値を保存） */
    private final ConcurrentHashMap<String, Object> cache;

    /** 初期化完了フラグ */
    private boolean initialized = false;

    /**
     * プライベートコンストラクタ
     */
    private PropertiesManager() {
        this.properties = new Properties();
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * シングルトンインスタンスを取得
     * 
     * @return PropertiesManagerインスタンス
     */
    public static PropertiesManager getInstance() {
        return INSTANCE;
    }

    /**
     * プロパティファイルを初期化
     * 
     * @throws IOException 読み込みエラー
     */
    public void initialize() throws IOException {
        if (initialized) {
            return;
        }

        // 1. クラスパスから読み込み
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE)) {
            if (is != null) {
                properties.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                LogHandler.getInstance().log(Level.INFO, LogHandler.LogType.SYSTEM,
                        "プロパティファイルをクラスパスから読み込み: " + DEFAULT_PROPERTIES_FILE);
            } else {
                // 2. ファイルシステムから読み込み
                loadFromFileSystem();
            }
        }

        // システムプロパティで上書き
        overrideWithSystemProperties();

        initialized = true;
        LogHandler.getInstance().log(Level.INFO, LogHandler.LogType.SYSTEM,
                "PropertiesManager初期化完了");
    }

    /**
     * ファイルシステムからプロパティを読み込み
     */
    private void loadFromFileSystem() throws IOException {
        Path[] searchPaths = {
                Paths.get(DEFAULT_PROPERTIES_FILE),
                Paths.get("config", DEFAULT_PROPERTIES_FILE),
                Paths.get(System.getProperty("user.home"), "EngineerSystem", DEFAULT_PROPERTIES_FILE)
        };

        for (Path path : searchPaths) {
            if (Files.exists(path)) {
                try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                    System.out.println("プロパティファイルを読み込み: " + path.toAbsolutePath())
                            ;
                    return;
                }
            }
        }

        // デフォルト値で初期化
        //System.out.println("プロパティファイルが見つかりません。デフォルト値を使用");
        //loadDefaults();
    }

    /**
     * システムプロパティで上書き
     */
    private void overrideWithSystemProperties() {
        properties.stringPropertyNames().forEach(key -> {
            String systemValue = System.getProperty(key);
            if (systemValue != null) {
                properties.setProperty(key, systemValue);
                LogHandler.getInstance().log(Level.INFO, LogHandler.LogType.SYSTEM,
                        "システムプロパティで上書き: " + key + "=" + systemValue);
            }
        });
    }

    /**
     * 文字列値を取得
     * 
     * @param key プロパティキー
     * @return プロパティ値
     */
    public String getString(String key) {
        return getString(key, null);
    }

    /**
     * 文字列値を取得（デフォルト値付き）
     * 
     * @param key          プロパティキー
     * @param defaultValue デフォルト値
     * @return プロパティ値
     */
    public String getString(String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        // ${変数}形式の置換
        if (value != null && value.contains("${")) {
            value = resolveVariables(value);
        }
        return value;
    }

    /**
     * 整数値を取得
     * 
     * @param key          プロパティキー
     * @param defaultValue デフォルト値
     * @return プロパティ値
     */
    public int getInt(String key, int defaultValue) {
        Object cached = cache.get(key);
        if (cached instanceof Integer) {
            return (Integer) cached;
        }

        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            int intValue = Integer.parseInt(value);
            cache.put(key, intValue);
            return intValue;
        } catch (NumberFormatException _e) {
            LogHandler.getInstance().log(Level.WARNING, LogHandler.LogType.SYSTEM,
                    "整数変換エラー: " + key + "=" + value);
            return defaultValue;
        }
    }

    /**
     * 長整数値を取得
     * 
     * @param key          プロパティキー
     * @param defaultValue デフォルト値
     * @return プロパティ値
     */
    public long getLong(String key, long defaultValue) {
        Object cached = cache.get(key);
        if (cached instanceof Long) {
            return (Long) cached;
        }

        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            long longValue = Long.parseLong(value);
            cache.put(key, longValue);
            return longValue;
        } catch (NumberFormatException _e) {
            LogHandler.getInstance().log(Level.WARNING, LogHandler.LogType.SYSTEM,
                    "長整数変換エラー: " + key + "=" + value);
            return defaultValue;
        }
    }

    /**
     * ブール値を取得
     * 
     * @param key          プロパティキー
     * @param defaultValue デフォルト値
     * @return プロパティ値
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object cached = cache.get(key);
        if (cached instanceof Boolean) {
            return (Boolean) cached;
        }

        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }

        boolean boolValue = Boolean.parseBoolean(value);
        cache.put(key, boolValue);
        return boolValue;
    }

    /**
     * 配列値を取得（カンマ区切り）
     * 
     * @param key プロパティキー
     * @return プロパティ値の配列
     */
    public String[] getArray(String key) {
        String value = getString(key);
        if (value == null || value.trim().isEmpty()) {
            return new String[0];
        }
        return value.split(",");
    }

    /**
     * 変数を解決（${変数}形式）
     * 
     * @param value 元の値
     * @return 解決後の値
     */
    private String resolveVariables(String value) {
        String resolved = value;
        int maxIterations = 10;

        while (resolved.contains("${") && maxIterations-- > 0) {
            int start = resolved.indexOf("${");
            int end = resolved.indexOf("}", start);

            if (start >= 0 && end > start) {
                String var = resolved.substring(start + 2, end);
                String replacement = null;

                // システムプロパティから取得
                replacement = System.getProperty(var);

                // プロパティから取得
                if (replacement == null) {
                    replacement = properties.getProperty(var);
                }

                if (replacement != null) {
                    resolved = resolved.substring(0, start) + replacement + resolved.substring(end + 1);
                } else {
                    break;
                }
            }
        }

        return resolved;
    }

    /**
     * プロパティを再読み込み
     * 
     * @throws IOException 読み込みエラー
     */
    public void reload() throws IOException {
        cache.clear();
        properties.clear();
        initialized = false;
        initialize();
    }

    /**
     * 初期化状態を確認
     * 
     * @return 初期化済みの場合true
     */
    public boolean isInitialized() {
        return initialized;
    }
}