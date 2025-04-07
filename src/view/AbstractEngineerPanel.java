package view;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import util.LogHandler;
import util.LogHandler.LogType;
import java.util.logging.Level;

/**
 * エンジニア情報関連パネル（詳細・追加画面）の基本機能を提供する抽象クラス
 * 共通UIコンポーネント管理、レイアウト、入力検証などの基盤を提供する
 *
 * <p>
 * このクラスは、エンジニア情報の詳細表示・編集・追加に関連するパネル（DetailPanel、AddPanel）の
 * 共通基盤として機能します。Template Methodパターンを活用して、共通処理を定義しつつ、
 * サブクラス固有の振る舞いをフックメソッドで拡張できるようにしています。
 * </p>
 *
 * <p>
 * 主な機能：
 * <ul>
 * <li>共通パネルレイアウトの初期化と管理</li>
 * <li>UIコンポーネントの格納と管理機能</li>
 * <li>入力検証の基本フレームワーク</li>
 * <li>エラー表示の共通処理</li>
 * <li>コンポーネントの状態管理（有効/無効、表示/非表示）</li>
 * </ul>
 * </p>
 *
 * <p>
 * このクラスを継承するサブクラスは、{@code initialize()}メソッドをオーバーライドして
 * 独自のUI初期化処理を実装し、{@code validateInput()}メソッドをオーバーライドして
 * 入力検証ロジックを実装する必要があります。
 * </p>
 *
 * <p>
 * 使用例：
 * </p>
 * 
 * <pre>
 * public class DetailPanel extends AbstractEngineerPanel {
 *     &#64;Override
 *     public void initialize() {
 *         // 親クラスの初期化処理
 *         super.initialize();
 * 
 *         // 独自の初期化処理
 *         setupUpdateButton();
 *     }
 * 
 *     @Override
 *     protected boolean validateInput() {
 *         // 入力検証ロジック
 *         return isValid;
 *     }
 * }
 * </pre>
 *
 * @author Nakano
 * @version 3.1.0
 * @since 2025-04-04
 */
public abstract class AbstractEngineerPanel extends JPanel {

    /** パネルのメインコンテンツを配置するパネル */
    protected JPanel panel;

    /** コンポーネントを格納するマップ（キー：コンポーネント名、値：コンポーネント） */
    protected Map<String, Component> components;

    /** エラーメッセージを表示するラベル */
    protected JLabel errorMessageLabel;

    /** パネルの初期化済みフラグ */
    protected boolean initialized;

    /** 入力フォームのセクション間のマージン */
    protected static final int SECTION_MARGIN = 15;

    /** ラベルとフィールド間のマージン */
    protected static final int LABEL_FIELD_MARGIN = 5;

    /** 必須項目を表すマーク */
    protected static final String REQUIRED_MARK = " *";

    /** 必須項目のラベル色 */
    protected static final Color REQUIRED_LABEL_COLOR = new Color(204, 0, 0);

    /** エラーメッセージの色 */
    protected static final Color ERROR_COLOR = new Color(204, 0, 0);

    /**
     * コンストラクタ
     * パネルの基本設定とコンポーネントマップの初期化
     */
    public AbstractEngineerPanel() {
        super(new BorderLayout());
        this.components = new HashMap<>();
        this.initialized = false;
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "AbstractEngineerPanelを初期化しました");
    }

    /**
     * パネルの初期化処理
     * Template Methodパターンに基づく共通初期化フロー
     * サブクラスでオーバーライドする場合は、super.initialize()を呼び出すこと
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            // メインパネルの初期化
            initializePanel();

            // エラーメッセージラベルの設定
            setupErrorMessageLabel();

            // 初期化済みフラグをセット
            initialized = true;

            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    this.getClass().getSimpleName() + "を初期化しました");

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.UI,
                    "パネルの初期化中にエラーが発生しました: " + this.getClass().getSimpleName(), e);
        }
    }

    /**
     * パネルの基本レイアウトを初期化
     * 継承クラスはこのメソッドの後に独自のコンポーネント初期化メソッドを呼び出す
     */
    protected void initializePanel() {
        // メインパネルの作成
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        // スクロールパネルに配置
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // メインパネルをBorderLayoutのCENTERに配置
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * エラーメッセージラベルのセットアップ
     * 下部にエラーメッセージを表示するエリアを確保
     */
    private void setupErrorMessageLabel() {
        // エラーメッセージラベルの作成
        errorMessageLabel = new JLabel("");
        errorMessageLabel.setForeground(ERROR_COLOR);
        errorMessageLabel.setVisible(false);

        // 下部パネルの作成
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(errorMessageLabel);
        bottomPanel.setBackground(Color.WHITE);

        // メインパネルの下部に配置
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * コンポーネントを登録
     * 名前付きでコンポーネントを管理対象に追加
     *
     * @param name      コンポーネント名
     * @param component 登録するコンポーネント
     * @return 登録したコンポーネント（メソッドチェーン用）
     */
    protected Component registerComponent(String name, Component component) {
        components.put(name, component);
        return component;
    }

    /**
     * 名前でコンポーネントを取得
     *
     * @param name コンポーネント名
     * @return 対応するコンポーネント、存在しない場合はnull
     */
    protected Component getComponent(String name) {
        return components.get(name);
    }

    /**
     * 名前でテキストフィールドを取得
     *
     * @param name テキストフィールド名
     * @return 対応するJTextField、存在しないまたは型が異なる場合はnull
     */
    protected JTextField getTextField(String name) {
        Component component = getComponent(name);
        if (component instanceof JTextField) {
            return (JTextField) component;
        }
        return null;
    }

    /**
     * 名前でコンボボックスを取得
     *
     * @param name コンボボックス名
     * @return 対応するJComboBox、存在しないまたは型が異なる場合はnull
     */
    protected JComboBox<?> getComboBox(String name) {
        Component component = getComponent(name);
        if (component instanceof JComboBox) {
            return (JComboBox<?>) component;
        }
        return null;
    }

    /**
     * 名前でテキストエリアを取得
     *
     * @param name テキストエリア名
     * @return 対応するJTextArea、存在しないまたは型が異なる場合はnull
     */
    protected JTextArea getTextArea(String name) {
        Component component = getComponent(name);
        if (component instanceof JTextArea) {
            return (JTextArea) component;
        }
        return null;
    }

    /**
     * 名前でチェックボックスを取得
     *
     * @param name チェックボックス名
     * @return 対応するJCheckBox、存在しないまたは型が異なる場合はnull
     */
    protected JCheckBox getCheckBox(String name) {
        Component component = getComponent(name);
        if (component instanceof JCheckBox) {
            return (JCheckBox) component;
        }
        return null;
    }

    /**
     * 必須項目のラベルを作成
     * 必須項目には赤い*マークを付与
     *
     * @param labelText ラベルテキスト
     * @param required  必須項目の場合true
     * @return 作成したJLabelコンポーネント
     */
    protected JLabel createFieldLabel(String labelText, boolean required) {
        JLabel label = new JLabel(labelText + (required ? REQUIRED_MARK : ""));
        if (required) {
            label.setForeground(REQUIRED_LABEL_COLOR);
        }
        return label;
    }

    /**
     * セクションタイトルを作成
     * フォームセクションの見出しとして使用
     *
     * @param titleText タイトルテキスト
     * @return 作成したJLabelコンポーネント
     */
    protected JLabel createSectionTitle(String titleText) {
        JLabel title = new JLabel(titleText);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        return title;
    }

    /**
     * フォーム行パネルを作成
     * ラベルとフィールドを水平に配置
     *
     * @param label ラベルコンポーネント
     * @param field フィールドコンポーネント
     * @return 行パネル
     */
    protected JPanel createFormRow(JLabel label, Component field) {
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BorderLayout(LABEL_FIELD_MARGIN, 0));
        rowPanel.setBackground(Color.WHITE);

        // ラベルを固定幅で配置
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        labelPanel.setBackground(Color.WHITE);
        labelPanel.setPreferredSize(new Dimension(100, 25));
        labelPanel.add(label);

        rowPanel.add(labelPanel, BorderLayout.WEST);
        rowPanel.add(field, BorderLayout.CENTER);

        return rowPanel;
    }

    /**
     * 水平方向のスペーサーを作成
     *
     * @param height スペーサーの高さ
     * @return スペーサーコンポーネント
     */
    protected Component createVerticalSpacer(int height) {
        return Box.createVerticalStrut(height);
    }

    /**
     * エラーメッセージを表示
     *
     * @param message 表示するエラーメッセージ
     */
    protected void showErrorMessage(String message) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(true);
        LogHandler.getInstance().log(Level.WARNING, LogType.UI, "エラーメッセージを表示: " + message);
    }

    /**
     * エラーメッセージをクリア
     */
    protected void clearErrorMessage() {
        errorMessageLabel.setText("");
        errorMessageLabel.setVisible(false);
    }

    /**
     * 入力検証を実行
     * サブクラスでオーバーライドして具体的な検証ロジックを実装
     *
     * @return 検証成功の場合true、失敗の場合false
     */
    protected abstract boolean validateInput();

    /**
     * 全コンポーネントの有効・無効を切り替え
     * データ処理中などに入力を無効化する際に使用
     *
     * @param enabled 有効化する場合true、無効化する場合false
     */
    protected void setAllComponentsEnabled(boolean enabled) {
        for (Component component : components.values()) {
            component.setEnabled(enabled);
        }
    }

    /**
     * パネルの初期化状態を取得
     *
     * @return 初期化済みの場合true
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * メインパネルを取得
     *
     * @return メインパネル
     */
    protected JPanel getPanel() {
        return panel;
    }

    /**
     * コンポーネントマップを取得
     *
     * @return コンポーネントを格納したマップ
     * 
     *         protected Map<String, Component> getComponents() {
     *         return components;
     *         }
     */
}