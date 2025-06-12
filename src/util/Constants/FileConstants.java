package util.Constants;

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

    /**
     * プライベートコンストラクタ
     * インスタンス化を防止
     */
    private FileConstants() {
        throw new AssertionError("定数クラスはインスタンス化できません");
    }

    // ========== ディレクトリ名 ==========
    /** データディレクトリ名 */
    public static final String DATA_DIR_NAME = "data";

    /** ログディレクトリ名 */
    public static final String LOG_DIR_NAME = "logs";

    /** ソースディレクトリ名 */
    public static final String SRC_DIR_NAME = "src";

    // ========== ファイル名 ==========
    /** デフォルトエンジニアCSVファイル名 */
    public static final String DEFAULT_ENGINEER_CSV = "engineers.csv";

    /** ログファイル名フォーマット */
    public static final String LOG_FILE_FORMAT = "System-%s.log";

    // ========== ファイル拡張子 ==========
    /** CSVファイル拡張子 */
    public static final String CSV_EXTENSION = ".csv";

    /** ログファイル拡張子 */
    public static final String LOG_EXTENSION = ".log";

    // ========== ファイルサイズ制限 ==========
    /** ログファイル最大サイズ（バイト） */
    public static final int MAX_LOG_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    // ========== ファイル名検証 ==========
    /** ファイル名に使用できない文字の正規表現パターン */
    public static final String INVALID_FILENAME_PATTERN = ".*[\\\\/:*?\"<>|].*";

    /** ファイル名に使用できない文字の説明 */
    public static final String INVALID_FILENAME_CHARS = "\\ / : * ? \" < > |";
}