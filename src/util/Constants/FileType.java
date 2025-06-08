package util.Constants;

/**
 * システムで扱うファイルタイプを定義する列挙型
 * 
 * <p>
 * ファイル操作やフィルタリングで使用されるファイルタイプを管理します。
 * </p>
 * 
 * @author Nakano
 */
public enum FileType {

    /** CSVファイル */
    CSV("CSV", "CSVファイル", "csv", "text/csv"),

    /** ログファイル */
    LOG("LOG", "ログファイル", "log", "text/plain"),

    /** テキストファイル */
    TEXT("TEXT", "テキストファイル", "txt", "text/plain"),

    /** すべてのファイル */
    ALL("ALL", "すべてのファイル", "*", "*/*");

    /** ファイルタイプ識別子 */
    private final String typeId;

    /** ファイルタイプ表示名 */
    private final String displayName;

    /** ファイル拡張子（ドットなし） */
    private final String extension;

    /** MIMEタイプ */
    private final String mimeType;

    /**
     * コンストラクタ
     * 
     * @param typeId      ファイルタイプ識別子
     * @param displayName 表示名
     * @param extension   拡張子
     * @param mimeType    MIMEタイプ
     */
    FileType(String typeId, String displayName, String extension, String mimeType) {
        this.typeId = typeId;
        this.displayName = displayName;
        this.extension = extension;
        this.mimeType = mimeType;
    }

    /**
     * ファイルタイプ識別子を取得
     * 
     * @return ファイルタイプ識別子
     */
    public String getTypeId() {
        return typeId;
    }

    /**
     * 表示名を取得
     * 
     * @return 表示名
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 拡張子を取得（ドットなし）
     * 
     * @return 拡張子
     */
    public String getExtension() {
        return extension;
    }

    /**
     * 拡張子を取得（ドット付き）
     * 
     * @return ドット付き拡張子
     */
    public String getExtensionWithDot() {
        return extension.equals("*") ? "" : "." + extension;
    }

    /**
     * MIMEタイプを取得
     * 
     * @return MIMEタイプ
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * ファイルチューザー用のフィルタ説明を取得
     * 
     * @return フィルタ説明文字列
     */
    public String getFilterDescription() {
        if (extension.equals("*")) {
            return displayName;
        }
        return String.format("%s (*.%s)", displayName, extension);
    }

    /**
     * 拡張子からファイルタイプを取得
     * 
     * @param extension 拡張子（ドット有無は問わない）
     * @return 対応するFileType、見つからない場合はALL
     */
    public static FileType fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return ALL;
        }

        String ext = extension.startsWith(".") ? extension.substring(1) : extension;

        for (FileType type : values()) {
            if (type.extension.equalsIgnoreCase(ext)) {
                return type;
            }
        }
        return ALL;
    }
}