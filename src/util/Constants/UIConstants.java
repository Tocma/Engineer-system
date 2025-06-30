package util.Constants;

import util.PropertiesManager;
import java.awt.Color;
import java.awt.Dimension;

/**
 * UI関連の定数を定義するクラス
 * 
 * <p>
 * このクラスは、ユーザーインターフェース全体で使用される
 * 色、サイズ、マージンなどの視覚的要素の定数を管理
 * </p>
 * 
 * @author Nakano
 */
public final class UIConstants {

    /** プロパティマネージャのインスタンス */
    private static final PropertiesManager props = PropertiesManager.getInstance();

    /**
     * プライベートコンストラクタ
     * インスタンス化を防止
     */
    private UIConstants() {
        throw new AssertionError("定数クラスはインスタンス化できません");
    }

    // ウィンドウサイズ
    public static final int DEFAULT_WINDOW_WIDTH = props.getInt("ui.window.default.width", 1000);
    public static final int DEFAULT_WINDOW_HEIGHT = props.getInt("ui.window.default.height", 800);
    public static final Dimension MINIMUM_WINDOW_SIZE = new Dimension(
            props.getInt("ui.window.min.width", 800),
            props.getInt("ui.window.min.height", 600));

    // 色定義（RGB値から Color オブジェクトを生成）
    public static final Color ERROR_COLOR = parseColor("ui.color.error", "204,0,0");
    public static final Color BACKGROUND_COLOR = parseColor("ui.color.background", "255,255,255");
    public static final Color READONLY_BACKGROUND_COLOR = parseColor("ui.color.readonly.background", "240,240,240");

    // その他のUI設定
    public static final int TABLE_ROW_HEIGHT = props.getInt("ui.table.row.height", 25);
    public static final int SCROLL_INCREMENT = props.getInt("ui.scroll.increment", 16);
    public static final int PANEL_PADDING = props.getInt("ui.panel.padding", 20);
    public static final int COMPONENT_SPACING = props.getInt("ui.component.spacing", 10);
    public static final int SECTION_SPACING = props.getInt("ui.section.spacing", 20);

    /**
     * プロパティからRGB値を読み込んでColorオブジェクトを生成
     */
    private static Color parseColor(String key, String defaultValue) {
        String rgb = props.getString(key, defaultValue);
        String[] parts = rgb.split(",");
        if (parts.length == 3) {
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return new Color(r, g, b);
            } catch (NumberFormatException _e) {
                // デフォルト値を使用
            }
        }
        // エラー時はデフォルト値をパース
        String[] defaults = defaultValue.split(",");
        return new Color(
                Integer.parseInt(defaults[0]),
                Integer.parseInt(defaults[1]),
                Integer.parseInt(defaults[2]));
    }

    // ========== 色定義 ==========

    /** エラーボーダー色 */
    public static final Color ERROR_BORDER_COLOR = ERROR_COLOR;

    // ========== フォントサイズ ==========
    /** セクションタイトルフォントサイズ */
    public static final float SECTION_TITLE_FONT_SIZE = 15f;

    /** エラーメッセージフォントサイズ */
    public static final float ERROR_MESSAGE_FONT_SIZE = 11f;

    // ========== コンポーネントサイズ ==========
    /** テキストフィールド標準幅 */
    public static final int TEXT_FIELD_WIDTH = 40;

    /** コンボボックス（年）幅 */
    public static final int YEAR_COMBO_WIDTH = 80;

    /** コンボボックス（月日）幅 */
    public static final int MONTH_DAY_COMBO_WIDTH = 60;

    /** スキル評価コンボボックスサイズ */
    public static final Dimension SKILL_COMBO_SIZE = new Dimension(80, 25);

    /** 言語選択コンボボックスのサイズ */
    public static final Dimension LANGUAGE_COMBO_SIZE = new Dimension(200, 25);

    // ========== 必須項目マーク ==========
    /** 必須項目を示すマーク */
    public static final String REQUIRED_MARK = " *";

    // ========== エラーボーダー幅 ==========
    /** エラーボーダーの線幅 */
    public static final int ERROR_BORDER_WIDTH = 2;
    public static final int LABEL_FIELD_MARGIN = 0;
}