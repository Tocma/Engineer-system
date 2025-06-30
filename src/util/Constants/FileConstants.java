package util.Constants;

import util.PropertiesManager;

/**
 * ファイル・ディレクトリ関連の定数を定義するクラス
 * 
 * <p>
 * このクラスは、ファイルパス、ディレクトリ名、拡張子など
 * ファイルシステム関連の定数を一元管理
 * </p>
 * 
 * @author Nakano
 */
public final class FileConstants {

    /** プロパティマネージャのインスタンス */
    private static final PropertiesManager props = PropertiesManager.getInstance();

    /**
     * プライベートコンストラクタ
     * インスタンス化を防止
     */
    private FileConstants() {
        throw new AssertionError("定数クラスはインスタンス化できません");
    }

    // ========== ディレクトリ名 ==========
    /** データディレクトリ名 */
    public static final String DATA_DIR_NAME = props.getString("directory.data", "data");

    /** 設定ディレクトリ名 */
    public static final String LOG_DIR_NAME = props.getString("directory.logs", "logs");

    /** 設定ディレクトリ名 */
    public static final String SRC_DIR_NAME = props.getString("directory.src", "src");

    // ========== ファイル名 ==========
    /** デフォルトエンジニアCSVファイル名 */
    public static final String DEFAULT_ENGINEER_CSV = props.getString("file.csv.default", "engineers.csv");
    
    /** デフォルト設定ファイル名 */
    public static final String LOG_FILE_FORMAT = props.getString("file.log.format", "System-%s.log");

    // ========== ファイル拡張子 ==========
    /** CSVファイル拡張子 */
    public static final String CSV_EXTENSION = ".csv";

    /** ログファイル拡張子 */
    public static final String LOG_EXTENSION = ".log";

    // ========== ファイル名検証 ==========
    /** ファイル名に使用できない文字の正規表現パターン */
    public static final String INVALID_FILENAME_PATTERN = ".*[\\\\/:*?\"<>|].*";

    // ファイルサイズ制限
    public static final int MAX_LOG_SIZE_BYTES = props.getInt("file.log.max.size", 10485760);

    // ファイル名検証
    public static final String INVALID_FILENAME_CHARS = props.getString("file.invalid.chars", "\\ / : * ? \" < > |");

}