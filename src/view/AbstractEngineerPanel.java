package view;

import javax.swing.*;
import javax.swing.border.Border;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import util.LogHandler;
import util.LogHandler.LogType;
import java.util.logging.Level;
import java.util.regex.Pattern;

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
 * @version 4.0.0
 * @since 2025-04-08
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

    /** エラー状態のコンポーネントを管理するマップ */
    protected final Map<String, Component> errorComponents;

    /** デフォルトのボーダー保存用マップ */
    protected final Map<JComponent, Border> originalBorders;

    /** エラー表示用のボーダー */
    protected static final Border ERROR_BORDER = BorderFactory.createLineBorder(ERROR_COLOR, 2);

    /** DialogManager参照 */
    protected final DialogManager dialogManager;

    /**
     * コンストラクタ
     * パネルの基本設定とコンポーネントマップの初期化
     */
    public AbstractEngineerPanel() {
        super(new BorderLayout());
        this.components = new HashMap<>();
        this.errorComponents = new HashMap<>();
        this.originalBorders = new HashMap<>();
        this.initialized = false;
        this.dialogManager = DialogManager.getInstance();
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
     * コンポーネントにエラー表示を設定
     * エラーが発生したコンポーネントに赤枠を表示し、エラーコンポーネントとして管理
     *
     * @param componentName エラーが発生したコンポーネント名
     * @param errorMessage  エラーメッセージ（nullの場合はエラーメッセージを更新しない）
     */
    protected void markComponentError(String componentName, String errorMessage) {
        Component component = getComponent(componentName);
        if (component == null) {
            return;
        }

        // JComponentかどうか確認
        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;

            // 元のボーダーを保存（まだ保存されていない場合）
            if (!originalBorders.containsKey(jComponent)) {
                originalBorders.put(jComponent, jComponent.getBorder());
            }

            // エラーボーダーを設定
            jComponent.setBorder(ERROR_BORDER);

            // エラーコンポーネントとして登録
            errorComponents.put(componentName, component);

            // エラーメッセージが指定されている場合は表示
            if (errorMessage != null) {
                showErrorMessage(errorMessage);
            }
        }
    }

    /**
     * コンポーネントのエラー表示をクリア
     * 特定のコンポーネントのエラー表示を解除
     *
     * @param componentName エラー表示を解除するコンポーネント名
     */
    protected void clearComponentError(String componentName) {
        Component component = errorComponents.remove(componentName);
        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;

            // 元のボーダーに戻す
            Border originalBorder = originalBorders.remove(jComponent);
            if (originalBorder != null) {
                jComponent.setBorder(originalBorder);
            } else {
                jComponent.setBorder(null);
            }
        }
    }

    /**
     * すべてのコンポーネントのエラー表示をクリア
     * エラー表示されているすべてのコンポーネントを元の状態に戻す
     */
    protected void clearAllComponentErrors() {
        // エラーコンポーネントのコピーを作成（反復処理中の変更を回避）
        List<String> componentNames = new ArrayList<>(errorComponents.keySet());

        // 各コンポーネントのエラー表示をクリア
        for (String componentName : componentNames) {
            clearComponentError(componentName);
        }

        // エラーメッセージをクリア
        clearErrorMessage();
    }

    /**
     * コンポーネントがエラー状態かどうかを確認
     *
     * @param componentName 確認するコンポーネント名
     * @return エラー状態の場合はtrue
     */
    protected boolean hasComponentError(String componentName) {
        return errorComponents.containsKey(componentName);
    }

    /**
     * エラー状態のコンポーネント名リストを取得
     *
     * @return エラー状態のコンポーネント名リスト
     */
    protected List<String> getErrorComponentNames() {
        return new ArrayList<>(errorComponents.keySet());
    }

    /**
     * バリデーションエラーをダイアログで表示
     * エラー状態のコンポーネントを元にエラーダイアログを表示
     *
     * @param fieldNameMap コンポーネント名と表示名のマッピング
     */
    protected void showValidationErrorDialog(Map<String, String> fieldNameMap) {
        List<String> errorFields = new ArrayList<>();

        // エラーコンポーネントの表示名を収集
        for (String componentName : errorComponents.keySet()) {
            String displayName = fieldNameMap.getOrDefault(componentName, componentName);
            errorFields.add(displayName);
        }

        // バリデーションエラーダイアログを表示
        if (!errorFields.isEmpty()) {
            dialogManager.showValidationErrorDialog(errorFields);
        }
    }

    /**
     * 入力検証を実行
     * サブクラスでオーバーライドして具体的な検証ロジックを実装
     *
     * @return 検証成功の場合true、失敗の場合false
     */
    protected abstract boolean validateInput();

    /**
     * テキストフィールドの入力検証
     * テキストフィールドの値が指定された条件を満たすか検証
     *
     * @param fieldName    フィールド名
     * @param required     必須項目の場合はtrue
     * @param maxLength    最大文字数（0以下の場合は制限なし）
     * @param pattern      正規表現パターン（nullの場合はパターン検証なし）
     * @param errorMessage エラー時のメッセージ
     * @return 検証成功の場合true、失敗の場合false
     */
    protected boolean validateTextField(String fieldName, boolean required, int maxLength, String pattern,
            String errorMessage) {
        JTextField field = getTextField(fieldName);
        if (field == null) {
            return false;
        }

        String value = field.getText();

        // 必須チェック
        if (required && (value == null || value.trim().isEmpty())) {
            markComponentError(fieldName, errorMessage);
            return false;
        }

        // 最大文字数チェック
        if (maxLength > 0 && value != null && value.length() > maxLength) {
            markComponentError(fieldName, errorMessage);
            return false;
        }

        // パターンチェック
        if (pattern != null && value != null && !value.isEmpty()) {
            Pattern regexPattern = Pattern.compile(pattern);
            if (!regexPattern.matcher(value).matches()) {
                markComponentError(fieldName, errorMessage);
                return false;
            }
        }

        // エラーがない場合はエラー表示をクリア
        clearComponentError(fieldName);
        return true;
    }

    /**
     * テキストエリアの入力検証
     * テキストエリアの値が指定された条件を満たすか検証
     *
     * @param fieldName    フィールド名
     * @param required     必須項目の場合はtrue
     * @param maxLength    最大文字数（0以下の場合は制限なし）
     * @param errorMessage エラー時のメッセージ
     * @return 検証成功の場合true、失敗の場合false
     */
    protected boolean validateTextArea(String fieldName, boolean required, int maxLength, String errorMessage) {
        JTextArea field = getTextArea(fieldName);
        if (field == null) {
            return false;
        }

        String value = field.getText();

        // 必須チェック
        if (required && (value == null || value.trim().isEmpty())) {
            markComponentError(fieldName, errorMessage);
            return false;
        }

        // 最大文字数チェック
        if (maxLength > 0 && value != null && value.length() > maxLength) {
            markComponentError(fieldName, errorMessage);
            return false;
        }

        // エラーがない場合はエラー表示をクリア
        clearComponentError(fieldName);
        return true;
    }

    /**
     * コンボボックスの入力検証
     * コンボボックスで項目が選択されているか検証
     *
     * @param fieldName    フィールド名
     * @param required     必須項目の場合はtrue
     * @param errorMessage エラー時のメッセージ
     * @return 検証成功の場合true、失敗の場合false
     */
    protected boolean validateComboBox(String fieldName, boolean required, String errorMessage) {
        JComboBox<?> field = getComboBox(fieldName);
        if (field == null) {
            return false;
        }

        // 必須チェック（選択されていない場合はnullまたは空文字）
        if (required) {
            Object selectedItem = field.getSelectedItem();
            if (selectedItem == null || selectedItem.toString().trim().isEmpty()) {
                markComponentError(fieldName, errorMessage);
                return false;
            }
        }

        // エラーがない場合はエラー表示をクリア
        clearComponentError(fieldName);
        return true;
    }

    /**
     * チェックボックスグループの入力検証
     * 少なくとも1つのチェックボックスが選択されているか検証
     *
     * @param checkboxes   検証対象のチェックボックスのリスト
     * @param errorMessage エラー時のメッセージ
     * @return 検証成功の場合true、失敗の場合false
     */
    protected boolean validateCheckBoxGroup(List<JCheckBox> checkboxes, String errorMessage) {
        if (checkboxes == null || checkboxes.isEmpty()) {
            return false;
        }

        // いずれかのチェックボックスが選択されているか確認
        boolean anySelected = checkboxes.stream().anyMatch(JCheckBox::isSelected);

        if (!anySelected) {
            // エラーメッセージを表示
            showErrorMessage(errorMessage);

            // 各チェックボックスにエラー表示
            checkboxes.forEach(checkbox -> {
                if (checkbox instanceof JComponent) {
                    JComponent jComponent = (JComponent) checkbox;

                    // 元のボーダーを保存
                    if (!originalBorders.containsKey(jComponent)) {
                        originalBorders.put(jComponent, jComponent.getBorder());
                    }

                    // エラーボーダーを設定
                    jComponent.setBorder(ERROR_BORDER);
                }
            });

            return false;
        }

        // エラーがない場合はエラー表示をクリア
        checkboxes.forEach(checkbox -> {
            if (checkbox instanceof JComponent) {
                JComponent jComponent = (JComponent) checkbox;

                // 元のボーダーに戻す
                Border originalBorder = originalBorders.remove(jComponent);
                if (originalBorder != null) {
                    jComponent.setBorder(originalBorder);
                } else {
                    jComponent.setBorder(null);
                }
            }
        });

        return true;
    }

    /**
     * 日付選択コンポーネントの入力検証
     * 年月日の選択コンボボックスが有効な日付を構成しているか検証
     *
     * @param yearFieldName  年コンボボックスのフィールド名
     * @param monthFieldName 月コンボボックスのフィールド名
     * @param dayFieldName   日コンボボックスのフィールド名（nullの場合は年月のみ検証）
     * @param required       必須項目の場合はtrue
     * @param errorMessage   エラー時のメッセージ
     * @return 検証成功の場合true、失敗の場合false
     */
    protected boolean validateDateComponents(String yearFieldName, String monthFieldName, String dayFieldName,
            boolean required, String errorMessage) {
        JComboBox<?> yearCombo = getComboBox(yearFieldName);
        JComboBox<?> monthCombo = getComboBox(monthFieldName);
        JComboBox<?> dayCombo = dayFieldName != null ? getComboBox(dayFieldName) : null;

        if (yearCombo == null || monthCombo == null || (dayFieldName != null && dayCombo == null)) {
            return false;
        }

        Object yearObj = yearCombo.getSelectedItem();
        Object monthObj = monthCombo.getSelectedItem();
        Object dayObj = dayCombo != null ? dayCombo.getSelectedItem() : null;

        String yearStr = yearObj != null ? yearObj.toString() : "";
        String monthStr = monthObj != null ? monthObj.toString() : "";
        String dayStr = dayObj != null ? dayObj.toString() : "";

        // 必須チェック
        if (required) {
            if (yearStr.isEmpty() || monthStr.isEmpty() || (dayCombo != null && dayStr.isEmpty())) {
                // エラー表示
                if (yearStr.isEmpty())
                    markComponentError(yearFieldName, null);
                if (monthStr.isEmpty())
                    markComponentError(monthFieldName, null);
                if (dayCombo != null && dayStr.isEmpty())
                    markComponentError(dayFieldName, null);

                showErrorMessage(errorMessage);
                return false;
            }
        } else if (yearStr.isEmpty() && monthStr.isEmpty() && (dayCombo == null || dayStr.isEmpty())) {
            // 必須でなく、すべて未選択の場合は有効とする
            clearComponentError(yearFieldName);
            clearComponentError(monthFieldName);
            if (dayFieldName != null)
                clearComponentError(dayFieldName);
            return true;
        }

        // 値が部分的に入力されている場合は、すべての値が必要
        if ((!yearStr.isEmpty() || !monthStr.isEmpty() || (dayCombo != null && !dayStr.isEmpty()))
                && (yearStr.isEmpty() || monthStr.isEmpty() || (dayCombo != null && dayStr.isEmpty()))) {

            // エラー表示
            if (yearStr.isEmpty())
                markComponentError(yearFieldName, null);
            if (monthStr.isEmpty())
                markComponentError(monthFieldName, null);
            if (dayCombo != null && dayStr.isEmpty())
                markComponentError(dayFieldName, null);

            showErrorMessage(errorMessage);
            return false;
        }

        // 日付の妥当性チェック
        try {
            int year = Integer.parseInt(yearStr);
            int month = Integer.parseInt(monthStr);
            int day = dayCombo != null ? Integer.parseInt(dayStr) : 1;

            // java.time.LocalDateで妥当性チェック
            LocalDate.of(year, month, day);

            // 有効な日付の場合、エラー表示をクリア
            clearComponentError(yearFieldName);
            clearComponentError(monthFieldName);
            if (dayFieldName != null)
                clearComponentError(dayFieldName);

            return true;
        } catch (Exception e) {
            // 日付が不正な場合
            markComponentError(yearFieldName, null);
            markComponentError(monthFieldName, null);
            if (dayFieldName != null)
                markComponentError(dayFieldName, null);

            showErrorMessage(errorMessage);
            return false;
        }
    }

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