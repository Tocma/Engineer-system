package util.Constants;

/**
 * システムで使用されるイベントタイプを定義する列挙型
 * 
 * <p>
 * MainControllerで処理されるイベントの識別子を統一的に管理します。
 * </p>
 * 
 * @author Nakano
 */
public enum EventType {

    /** ビュー更新イベント */
    REFRESH_VIEW("REFRESH_VIEW", "画面更新"),

    /** パネル切り替えイベント */
    CHANGE_PANEL("CHANGE_PANEL", "画面遷移"),

    /** データ保存イベント */
    SAVE_DATA("SAVE_DATA", "データ保存"),

    /** データ読み込みイベント */
    LOAD_DATA("LOAD_DATA", "データ読み込み"),

    /** 詳細表示イベント */
    VIEW_DETAIL("VIEW_DETAIL", "詳細表示"),

    /** エンジニア検索イベント */
    SEARCH_ENGINEERS("SEARCH_ENGINEERS", "エンジニア検索"),

    /** テンプレート出力イベント */
    TEMPLATE("TEMPLATE", "テンプレート出力"),

    /** CSV出力イベント */
    EXPORT_CSV("EXPORT_CSV", "CSV出力"),

    /** CSVインポートイベント */
    IMPORT_CSV("IMPORT_CSV", "CSVインポート"),

    /** エンジニア削除イベント */
    DELETE_ENGINEER("DELETE_ENGINEER", "エンジニア削除"),

    /** シャットダウンイベント */
    SHUTDOWN("SHUTDOWN", "システム終了");

    /** イベント識別子 */
    private final String eventName;

    /** イベント説明 */
    private final String description;

    /**
     * コンストラクタ
     * 
     * @param eventName   イベント識別子
     * @param description イベント説明
     */
    EventType(String eventName, String description) {
        this.eventName = eventName;
        this.description = description;
    }

    /**
     * イベント識別子を取得
     * 
     * @return イベント識別子
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * イベント説明を取得
     * 
     * @return イベント説明
     */
    public String getDescription() {
        return description;
    }

    /**
     * イベント名から列挙型を取得
     * 
     * @param eventName イベント識別子
     * @return 対応するEventType、見つからない場合はnull
     */
    public static EventType fromEventName(String eventName) {
        for (EventType type : values()) {
            if (type.eventName.equals(eventName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 有効なイベント名かどうかを判定
     * 
     * @param eventName 検証するイベント名
     * @return 有効な場合true
     */
    public static boolean isValidEvent(String eventName) {
        return fromEventName(eventName) != null;
    }
}