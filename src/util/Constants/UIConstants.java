package util.Constants;

import java.awt.Color;
import java.awt.Dimension;

/**
 * UI関連の定数を定義するクラス
 * 
 * <p>
 * このクラスは、ユーザーインターフェース全体で使用される
 * 色、サイズ、マージンなどの視覚的要素の定数を管理します。
 * </p>
 * 
 * @author Nakano
 */
public final class UIConstants {

    /**
     * プライベートコンストラクタ
     * インスタンス化を防止
     */
    private UIConstants() {
        throw new AssertionError("定数クラスはインスタンス化できません");
    }

    // ========== ウィンドウサイズ ==========
    /** デフォルトウィンドウ幅 */
    public static final int DEFAULT_WINDOW_WIDTH = 1000;

    /** デフォルトウィンドウ高さ */
    public static final int DEFAULT_WINDOW_HEIGHT = 800;

    /** 最小ウィンドウサイズ */
    public static final Dimension MINIMUM_WINDOW_SIZE = new Dimension(800, 600);

    // ========== 色定義 ==========
    /** エラー表示色 */
    public static final Color ERROR_COLOR = new Color(204, 0, 0);

    /** 背景色（白） */
    public static final Color BACKGROUND_COLOR = Color.WHITE;

    /** 読み取り専用フィールド背景色 */
    public static final Color READONLY_BACKGROUND_COLOR = new Color(240, 240, 240);

    /** エラーボーダー色 */
    public static final Color ERROR_BORDER_COLOR = ERROR_COLOR;

    // ========== フォントサイズ ==========
    /** セクションタイトルフォントサイズ */
    public static final float SECTION_TITLE_FONT_SIZE = 14f;

    /** エラーメッセージフォントサイズ */
    public static final float ERROR_MESSAGE_FONT_SIZE = 11f;

    // ========== マージン・パディング ==========
    /** 標準パネルパディング */
    public static final int PANEL_PADDING = 20;

    /** ラベルとフィールド間のマージン */
    public static final int LABEL_FIELD_MARGIN = 5;

    /** コンポーネント間の標準間隔 */
    public static final int COMPONENT_SPACING = 10;

    /** セクション間の間隔 */
    public static final int SECTION_SPACING = 20;

    // ========== コンポーネントサイズ ==========
    /** テキストフィールド標準幅 */
    public static final int TEXT_FIELD_WIDTH = 20;

    /** コンボボックス（年）幅 */
    public static final int YEAR_COMBO_WIDTH = 80;

    /** コンボボックス（月日）幅 */
    public static final int MONTH_DAY_COMBO_WIDTH = 60;

    /** スキル評価コンボボックスサイズ */
    public static final Dimension SKILL_COMBO_SIZE = new Dimension(80, 25);

    /** テーブル行高さ */
    public static final int TABLE_ROW_HEIGHT = 25;

    /** スクロール速度 */
    public static final int SCROLL_INCREMENT = 16;

    // ========== 必須項目マーク ==========
    /** 必須項目を示すマーク */
    public static final String REQUIRED_MARK = " *";

    // ========== エラーボーダー幅 ==========
    /** エラーボーダーの線幅 */
    public static final int ERROR_BORDER_WIDTH = 2;
}