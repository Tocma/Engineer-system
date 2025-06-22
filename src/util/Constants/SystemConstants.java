package util.Constants;

/**
 * システム全体の基本設定値を定義する定数クラス
 * 
 * <p>
 * このクラスは、エンジニア人材管理システム全体で使用される
 * 基本的な設定値や制限値を一元管理
 * </p>
 * 
 * @author Nakano
 */
public final class SystemConstants {

    /**
     * プライベートコンストラクタ
     * インスタンス化を防止
     */
    private SystemConstants() {
        throw new AssertionError("定数クラスはインスタンス化できません");
    }

    // ========== データ制限値 ==========
    /** エンジニア登録可能最大件数 */
    public static final int MAX_ENGINEER_RECORDS = 1000;

    /** 1ページあたりの表示件数 */
    public static final int PAGE_SIZE = 100;

    /** 一括選択可能最大件数 */
    public static final int MAX_SELECTION_COUNT = 100;

    // ========== 文字数制限値 ==========
    /** 氏名最大文字数 */
    public static final int MAX_NAME_LENGTH = 20;

    /** 経歴最大文字数 */
    public static final int MAX_CAREER_HISTORY_LENGTH = 200;

    /** 研修受講歴最大文字数 */
    public static final int MAX_TRAINING_HISTORY_LENGTH = 200;

    /** 備考最大文字数 */
    public static final int MAX_NOTE_LENGTH = 500;

    /** 社員ID桁数 */
    public static final int EMPLOYEE_ID_LENGTH = 5;

    /** 禁止社員ID */
    public static final String FORBIDDEN_EMPLOYEE_ID = "ID00000";

    // ========== スレッド・タイムアウト設定 ==========
    /** スレッド終了待機時間（ミリ秒） */
    public static final long THREAD_TERMINATION_TIMEOUT = 5000L;

    /** 非同期処理デフォルトタイムアウト（ミリ秒） */
    public static final long ASYNC_OPERATION_TIMEOUT = 30000L;

    /** ワーカースレッドプールサイズ */
    public static final int WORKER_THREAD_POOL_SIZE = 5;

    // ========== システム設定 ==========
    /** アプリケーション重複起動防止用ポート番号 */
    public static final int LOCK_PORT = 54321;

    /** デフォルトエンコーディング */
    public static final String DEFAULT_ENCODING = "UTF-8";

    /** システムバージョン */
    public static final String SYSTEM_VERSION = "4.15.22";

    /** システム名 */
    public static final String SYSTEM_NAME = "エンジニア人材管理システム";
}