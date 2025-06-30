package util.Constants;

import util.PropertiesManager;

/**
 * システム全体の基本設定値を定義する定数クラス
 * application.propertiesから値を読み込む改良版
 * 
 * @author Nakano
 */
public final class SystemConstants {

    private static final PropertiesManager props = PropertiesManager.getInstance();

    /**
     * プライベートコンストラクタ
     * インスタンス化を防止
     */
    private SystemConstants() {
        throw new AssertionError("定数クラスはインスタンス化できません");
    }

    // ========== データ制限値 ==========
    /** エンジニア登録可能最大件数 */
    public static final int MAX_ENGINEER_RECORDS = props.getInt("data.max.engineer.records", 1000);

    /** 1ページあたりの表示件数 */
    public static final int PAGE_SIZE = props.getInt("data.page.size", 100);

    /** 一括選択可能最大件数 */
    public static final int MAX_SELECTION_COUNT = props.getInt("data.max.selection.count", 100);

    // ========== 文字数制限値 ==========
    /** 氏名最大文字数 */
    public static final int MAX_NAME_LENGTH = props.getInt("validation.max.name.length", 20);

    /** 経歴最大文字数 */
    public static final int MAX_CAREER_HISTORY_LENGTH = props.getInt("validation.max.career.history.length", 200);

    /** 研修受講歴最大文字数 */
    public static final int MAX_TRAINING_HISTORY_LENGTH = props.getInt("validation.max.training.history.length", 200);

    /** 備考最大文字数 */
    public static final int MAX_NOTE_LENGTH = props.getInt("validation.max.note.length", 500);

    /** 社員ID桁数 */
    public static final int EMPLOYEE_ID_LENGTH = props.getInt("validation.employee.id.length", 5);

    /** 禁止社員ID */
    public static final String FORBIDDEN_EMPLOYEE_ID = props.getString("validation.forbidden.employee.id", "ID00000");

    // ========== スレッド・タイムアウト設定 ==========
    /** スレッド終了待機時間（ミリ秒） */
    public static final long THREAD_TERMINATION_TIMEOUT = props.getLong("thread.termination.timeout", 5000L);

    /** 非同期処理デフォルトタイムアウト（ミリ秒） */
    public static final long ASYNC_OPERATION_TIMEOUT = props.getLong("thread.async.operation.timeout", 30000L);

    /** ワーカースレッドプールサイズ */
    public static final int WORKER_THREAD_POOL_SIZE = props.getInt("thread.worker.pool.size", 5);

    // ========== システム設定 ==========
    /** アプリケーション重複起動防止用ポート番号 */
    public static final int LOCK_PORT = props.getInt("system.lock.port", 54321);

    /** デフォルトエンコーディング */
    public static final String DEFAULT_ENCODING = props.getString("system.encoding", "UTF-8");

    /** システムバージョン */
    public static final String SYSTEM_VERSION = props.getString("system.version", "4.16.0");

    /** システム名 */
    public static final String SYSTEM_NAME = props.getString("system.name", "エンジニア人材管理システム");
}