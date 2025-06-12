package util.Constants;

/**
 * CSV処理関連の定数を定義するクラス
 * 
 * <p>
 * このクラスは、CSVファイルの読み書き処理で使用される
 * ヘッダー定義、フォーマット設定などの定数を管理
 * </p>
 * 
 * @author Nakano
 */
public final class CSVConstants {

    /**
     * プライベートコンストラクタ
     * インスタンス化を防止
     */
    private CSVConstants() {
        throw new AssertionError("定数クラスはインスタンス化できません");
    }

    // ========== CSVヘッダー定義 ==========
    /** CSVヘッダー列名配列 */
    public static final String[] CSV_HEADERS = {
            "社員ID(必須)",
            "氏名(必須)",
            "フリガナ(必須)",
            "生年月日(必須)",
            "入社年月(必須)",
            "エンジニア歴(必須)",
            "扱える言語(必須)",
            "経歴",
            "研修の受講歴",
            "技術力",
            "受講態度",
            "コミュニケーション能力",
            "リーダーシップ",
            "備考",
            "登録日"
    };

    /** デフォルトCSVヘッダー行 */
    public static final String DEFAULT_CSV_HEADER = String.join(",", CSV_HEADERS);

    // ========== CSV列インデックス ==========
    public static final int COLUMN_INDEX_ID = 0;
    public static final int COLUMN_INDEX_NAME = 1;
    public static final int COLUMN_INDEX_NAME_KANA = 2;
    public static final int COLUMN_INDEX_BIRTH_DATE = 3;
    public static final int COLUMN_INDEX_JOIN_DATE = 4;
    public static final int COLUMN_INDEX_CAREER = 5;
    public static final int COLUMN_INDEX_LANGUAGES = 6;
    public static final int COLUMN_INDEX_CAREER_HISTORY = 7;
    public static final int COLUMN_INDEX_TRAINING_HISTORY = 8;
    public static final int COLUMN_INDEX_TECHNICAL_SKILL = 9;
    public static final int COLUMN_INDEX_LEARNING_ATTITUDE = 10;
    public static final int COLUMN_INDEX_COMMUNICATION_SKILL = 11;
    public static final int COLUMN_INDEX_LEADERSHIP = 12;
    public static final int COLUMN_INDEX_NOTE = 13;
    public static final int COLUMN_INDEX_REGISTERED_DATE = 14;

    // ========== CSVフォーマット設定 ==========
    /** プログラミング言語区切り文字 */
    public static final String LANGUAGE_DELIMITER = ";";

    /** CSVフィールド区切り文字 */
    public static final String FIELD_DELIMITER = ",";

    /** 日付フォーマットパターン */
    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";

    /** CSV読み込み時のカラム分割制限値（-1は無制限） */
    public static final int CSV_SPLIT_LIMIT = -1;
}