package util.Constants;

/**
 * システムで使用されるパネル（画面）タイプを定義する列挙型
 * 
 * <p>
 * 画面遷移やパネル管理で使用される識別子を統一的に管理
 * </p>
 * 
 * @author Nakano
 */
public enum PanelType {

    /** エンジニア一覧画面 */
    LIST("LIST", "エンジニア一覧", "view.ListPanel"),

    /** エンジニア詳細画面 */
    DETAIL("DETAIL", "エンジニア詳細", "view.DetailPanel"),

    /** エンジニア新規追加画面 */
    ADD("ADD", "エンジニア新規追加", "view.AddPanel");

    /** パネル識別子 */
    private final String id;

    /** パネル表示名 */
    private final String displayName;

    /** パネルクラスの完全修飾名 */
    private final String className;

    /**
     * コンストラクタ
     * 
     * @param id          パネル識別子
     * @param displayName パネル表示名
     * @param className   パネルクラス名
     */
    PanelType(String id, String displayName, String className) {
        this.id = id;
        this.displayName = displayName;
        this.className = className;
    }

    /**
     * パネル識別子を取得
     * 
     * @return パネル識別子
     */
    public String getId() {
        return id;
    }

    /**
     * パネル表示名を取得
     * 
     * @return パネル表示名
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * パネルクラス名を取得
     * 
     * @return パネルクラスの完全修飾名
     */
    public String getClassName() {
        return className;
    }

    /**
     * 識別子から列挙型を取得
     * 
     * @param id パネル識別子
     * @return 対応するPanelType、見つからない場合はnull
     */
    public static PanelType fromId(String id) {
        for (PanelType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}