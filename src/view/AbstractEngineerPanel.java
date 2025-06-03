package view;

import util.LogHandler;
import util.Constants.MessageEnum;
import util.LogHandler.LogType;
import util.validator.IDValidator;
import util.validator.Validator;
import util.validator.ValidatorEnum;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * エンジニア情報関連パネル（詳細・追加画面）の基本機能を提供する抽象クラス
 * 共通UIコンポーネント管理、レイアウト、入力検証、フォーム作成機能を統合
 *
 * <p>
 * このクラスは、エンジニア情報の詳細表示・編集・追加に関連するパネル（DetailPanel、AddPanel）の
 * 共通基盤として機能します。Template Methodパターンを活用して、共通処理を定義しつつ、
 * サブクラス固有の振る舞いをフックメソッドで拡張できるようにしています。
 * </p>
 *
 * <p>
 * バージョン4.11.7で追加された主な機能：
 * <ul>
 * <li>共通フォーム作成機能の統合</li>
 * <li>共通フィールド管理の実装</li>
 * <li>統一されたバリデーション機能</li>
 * <li>重複コードの大幅削減</li>
 * </ul>
 * </p>
 *
 * @author Nakano
 * @version 4.11.7
 * @since 2025-05-30
 */
public abstract class AbstractEngineerPanel extends JPanel {

    /** パネルのメインコンテンツを配置するパネル */
    protected JPanel panel;

    /** コンポーネントを格納するマップ（キー：コンポーネント名、値：コンポーネント） */
    protected Map<String, Component> components;

    /** エラーメッセージを表示するラベル（全体的なエラー表示用） */
    protected JLabel errorMessageLabel;

    /** フィールドごとのエラーメッセージラベルを格納するマップ */
    protected Map<String, JLabel> fieldErrorLabels;

    /** パネルの初期化済みフラグ */
    protected boolean initialized;

    /** ラベルとフィールド間のマージン */
    protected static final int LABEL_FIELD_MARGIN = 5;

    /** 必須項目を表すマーク */
    protected static final String REQUIRED_MARK = " *";

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

    /** ボタンを配置するパネル */
    protected JPanel buttonPanel;

    /** 下部パネル（エラーメッセージとボタンを格納） */
    protected JPanel bottomPanel;

    // === 共通フィールド（重複削減） ===

    /** 年のコンボボックス（生年月日用） */
    protected JComboBox<String> birthYearComboBox;

    /** 月のコンボボックス（生年月日用） */
    protected JComboBox<String> birthMonthComboBox;

    /** 日のコンボボックス（生年月日用） */
    protected JComboBox<String> birthDayComboBox;

    /** 年のコンボボックス（入社年月用） */
    protected JComboBox<String> joinYearComboBox;

    /** 月のコンボボックス（入社年月用） */
    protected JComboBox<String> joinMonthComboBox;

    /** エンジニア歴コンボボックス */
    protected JComboBox<String> careerComboBox;

    /** 技術力コンボボックス */
    protected JComboBox<String> technicalSkillComboBox;

    /** 受講態度コンボボックス */
    protected JComboBox<String> learningAttitudeComboBox;

    /** コミュニケーション能力コンボボックス */
    protected JComboBox<String> communicationSkillComboBox;

    /** リーダーシップコンボボックス */
    protected JComboBox<String> leadershipComboBox;

    /** 言語選択コンボボックス */
    protected FormComponentUtil.MultiSelectComboBox languageComboBox;

    /** 氏名フィールド */
    protected JTextField nameField;

    /** 氏名 (カナ)フィールド */
    protected JTextField nameKanaField;

    /** 社員IDフィールド */
    protected JTextField idField;

    /** 経歴テキストエリア */
    protected JTextArea careerHistoryArea;

    /** 研修の受講歴テキストエリア */
    protected JTextArea trainingHistoryArea;

    /** 備考テキストエリア */
    protected JTextArea noteArea;

    /** フィールド名と表示名のマッピング */
    protected Map<String, String> fieldDisplayNames;

    /**
     * コンストラクタ
     * パネルの基本設定とコンポーネントマップの初期化
     */
    public AbstractEngineerPanel() {
        super(new BorderLayout());
        this.components = new HashMap<>();
        this.fieldErrorLabels = new HashMap<>();
        this.errorComponents = new HashMap<>();
        this.originalBorders = new HashMap<>();
        this.initialized = false;
        this.dialogManager = DialogManager.getInstance();
        this.fieldDisplayNames = new HashMap<>();
        initializeFieldDisplayNames();
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "AbstractEngineerPanelを初期化しました");
    }

    /**
     * フィールド名と表示名のマッピングを初期化
     * バリデーションエラー表示時に使用
     */
    private void initializeFieldDisplayNames() {
        fieldDisplayNames.put("nameField", "氏名");
        fieldDisplayNames.put("nameKanaField", "氏名 (カナ)");
        fieldDisplayNames.put("idField", "社員ID");
        fieldDisplayNames.put("birthDate", "生年月日");
        fieldDisplayNames.put("joinDate", "入社年月");
        fieldDisplayNames.put("careerComboBox", "エンジニア歴");
        fieldDisplayNames.put("languages", "扱える言語");
        fieldDisplayNames.put("careerHistoryArea", "経歴");
        fieldDisplayNames.put("trainingHistoryArea", "研修の受講歴");
        fieldDisplayNames.put("technicalSkillComboBox", "技術力");
        fieldDisplayNames.put("learningAttitudeComboBox", "受講態度");
        fieldDisplayNames.put("communicationSkillComboBox", "コミュニケーション能力");
        fieldDisplayNames.put("leadershipComboBox", "リーダーシップ");
        fieldDisplayNames.put("noteArea", "備考");
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

        // 下部パネルの作成 - BorderLayoutに変更
        bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);

        // エラーメッセージ用パネル
        JPanel errorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        errorPanel.setBackground(Color.WHITE);
        errorPanel.add(errorMessageLabel);

        // エラーメッセージを左側に配置
        bottomPanel.add(errorPanel, BorderLayout.WEST);

        // ボタンパネルを初期化（右寄せ）
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);

        // ボタンパネルを右側に配置
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        // メインパネルの下部に配置
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // === 共通フォーム作成メソッド（重複削減） ===

    /**
     * 基本情報セクションの作成
     * 氏名、社員ID、生年月日などの基本情報入力フィールドを配置
     *
     * @param container    配置先のコンテナ
     * @param isDetailMode 詳細モードかどうか（社員IDを編集不可にするか）
     */
    protected void createBasicInfoSection(JPanel container, boolean isDetailMode) {
        // 氏名フィールド（必須）
        JLabel nameLabel = createFieldLabel("氏名", true);
        nameField = new JTextField(20);
        registerComponent("nameField", nameField);
        container.add(createFormRow(nameLabel, nameField, "nameField"));

        // 氏名 (カナ)フィールド（必須）
        JLabel nameKanaLabel = createFieldLabel("氏名 (カナ)", true);
        nameKanaField = new JTextField(20);
        registerComponent("nameKanaField", nameKanaField);
        container.add(createFormRow(nameKanaLabel, nameKanaField, "nameKanaField"));

        // 社員IDフィールド（必須）
        JLabel idLabel = createFieldLabel("社員ID", true);
        idField = new JTextField(20);
        if (isDetailMode) {
            idField.setEditable(false);
            idField.setBackground(new Color(240, 240, 240));
        }
        registerComponent("idField", idField);
        container.add(createFormRow(idLabel, idField, "idField"));

        // 生年月日（必須）
        createBirthDateSection(container);

        // 入社年月（必須）
        createJoinDateSection(container);

        // エンジニア歴（必須）
        createCareerSection(container);

        container.add(createVerticalSpacer(20));
    }

    /**
     * 生年月日セクションの作成
     */
    private void createBirthDateSection(JPanel container) {
        JLabel birthDateLabel = createFieldLabel("生年月日", true);
        JPanel birthDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        birthDatePanel.setBackground(Color.WHITE);

        // 年コンボボックス
        birthYearComboBox = new JComboBox<>(DateOptionUtil.getBirthYearOptions());
        birthYearComboBox.setPreferredSize(new Dimension(80, 25));
        registerComponent("birthYearComboBox", birthYearComboBox);
        birthDatePanel.add(birthYearComboBox);
        birthDatePanel.add(new JLabel("年"));

        // 月コンボボックス
        birthMonthComboBox = new JComboBox<>(DateOptionUtil.getMonthOptions());
        birthMonthComboBox.setPreferredSize(new Dimension(60, 25));
        registerComponent("birthMonthComboBox", birthMonthComboBox);
        birthDatePanel.add(birthMonthComboBox);
        birthDatePanel.add(new JLabel("月"));

        // 日コンボボックス
        birthDayComboBox = new JComboBox<>(DateOptionUtil.getDayOptions());
        birthDayComboBox.setPreferredSize(new Dimension(60, 25));
        registerComponent("birthDayComboBox", birthDayComboBox);
        birthDatePanel.add(birthDayComboBox);
        birthDatePanel.add(new JLabel("日"));

        createFieldErrorLabel("birthDate");
        container.add(createFormRow(birthDateLabel, birthDatePanel, "birthDate"));
    }

    /**
     * 入社年月セクションの作成
     */
    private void createJoinDateSection(JPanel container) {
        JLabel joinDateLabel = createFieldLabel("入社年月", true);
        JPanel joinDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        joinDatePanel.setBackground(Color.WHITE);

        // 年コンボボックス
        joinYearComboBox = new JComboBox<>(DateOptionUtil.getJoinYearOptions());
        joinYearComboBox.setPreferredSize(new Dimension(80, 25));
        registerComponent("joinYearComboBox", joinYearComboBox);
        joinDatePanel.add(joinYearComboBox);
        joinDatePanel.add(new JLabel("年"));

        // 月コンボボックス
        joinMonthComboBox = new JComboBox<>(DateOptionUtil.getMonthOptions());
        joinMonthComboBox.setPreferredSize(new Dimension(60, 25));
        registerComponent("joinMonthComboBox", joinMonthComboBox);
        joinDatePanel.add(joinMonthComboBox);
        joinDatePanel.add(new JLabel("月"));

        createFieldErrorLabel("joinDate");
        container.add(createFormRow(joinDateLabel, joinDatePanel, "joinDate"));
    }

    /**
     * エンジニア歴セクションの作成
     */
    private void createCareerSection(JPanel container) {
        JLabel careerLabel = createFieldLabel("エンジニア歴", true);
        JPanel careerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        careerPanel.setBackground(Color.WHITE);

        careerComboBox = new JComboBox<>(DateOptionUtil.getCareerOptions());
        careerComboBox.setPreferredSize(new Dimension(80, 25));
        registerComponent("careerComboBox", careerComboBox);
        careerPanel.add(careerComboBox);
        careerPanel.add(new JLabel("年"));

        container.add(createFormRow(careerLabel, careerPanel, "careerComboBox"));
    }

    /**
     * 言語スキルセクションの作成
     *
     * @param container 配置先のコンテナ
     */
    protected void createLanguageSection(JPanel container) {
        // セクションタイトル
        JLabel languageTitle = createSectionTitle("扱える言語");
        JLabel requiredMark = new JLabel(REQUIRED_MARK);
        JLabel errorLabel = createFieldErrorLabel("languages");

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(languageTitle);
        titlePanel.add(requiredMark);

        errorLabel.setVisible(false);
        errorLabel.setForeground(ERROR_COLOR);
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN, 11f));
        titlePanel.add(Box.createHorizontalStrut(10));
        titlePanel.add(errorLabel);

        container.add(titlePanel);

        // MultiSelectComboBoxの作成
        languageComboBox = FormComponentUtil.createLanguageComboBox();
        registerComponent("languageComboBox", languageComboBox);
        registerComponent("languages", languageComboBox);

        container.add(createVerticalSpacer(5));
        container.add(languageComboBox);
        container.add(createVerticalSpacer(20));
    }

    /**
     * 経歴セクションの作成
     *
     * @param container 配置先のコンテナ
     */
    protected void createCareerHistorySection(JPanel container) {
        JLabel careerHistoryTitle = createSectionTitle("経歴");
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(careerHistoryTitle);
        container.add(titlePanel);

        careerHistoryArea = new JTextArea(5, 20);
        careerHistoryArea.setLineWrap(true);
        careerHistoryArea.setWrapStyleWord(true);
        JScrollPane careerScrollPane = new JScrollPane(careerHistoryArea);
        registerComponent("careerHistoryArea", careerHistoryArea);

        container.add(createFormRow(new JLabel(""), careerScrollPane, "careerHistoryArea"));
    }

    /**
     * 研修の受講歴セクションの作成
     *
     * @param container 配置先のコンテナ
     */
    protected void createTrainingSection(JPanel container) {
        JLabel trainingTitle = createSectionTitle("研修の受講歴");
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(trainingTitle);
        container.add(titlePanel);

        trainingHistoryArea = new JTextArea(2, 20);
        trainingHistoryArea.setLineWrap(true);
        trainingHistoryArea.setWrapStyleWord(true);
        JScrollPane trainingScrollPane = new JScrollPane(trainingHistoryArea);
        registerComponent("trainingHistoryArea", trainingHistoryArea);

        container.add(createFormRow(new JLabel(""), trainingScrollPane, "trainingHistoryArea"));
    }

    /**
     * スキルセクションの作成
     *
     * @param container 配置先のコンテナ
     */
    protected void createSkillSection(JPanel container) {
        Dimension skillComboBoxSize = new Dimension(80, 25);

        // 技術力
        createSkillField(container, "技術力",
                technicalSkillComboBox = new JComboBox<>(DateOptionUtil.getSkillRatingOptions()),
                "technicalSkillComboBox", skillComboBoxSize);

        // 受講態度
        createSkillField(container, "受講態度",
                learningAttitudeComboBox = new JComboBox<>(DateOptionUtil.getSkillRatingOptions()),
                "learningAttitudeComboBox", skillComboBoxSize);

        // コミュニケーション能力
        createSkillField(container, "コミュニケーション能力",
                communicationSkillComboBox = new JComboBox<>(DateOptionUtil.getSkillRatingOptions()),
                "communicationSkillComboBox", skillComboBoxSize);

        // リーダーシップ
        createSkillField(container, "リーダーシップ",
                leadershipComboBox = new JComboBox<>(DateOptionUtil.getSkillRatingOptions()),
                "leadershipComboBox", skillComboBoxSize);

        container.add(createVerticalSpacer(20));
    }

    /**
     * 個別スキルフィールドの作成
     */
    private void createSkillField(JPanel container, String labelText, JComboBox<String> comboBox,
            String componentName, Dimension size) {
        JLabel label = createFieldLabel(labelText, false);

        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        comboPanel.setBackground(Color.WHITE);
        comboPanel.setPreferredSize(size);
        comboPanel.add(comboBox);

        registerComponent(componentName, comboBox);
        container.add(createFormRow(label, comboPanel, componentName));
    }

    /**
     * 備考セクションの作成
     *
     * @param container 配置先のコンテナ
     */
    protected void createNoteSection(JPanel container) {
        JLabel noteTitle = createSectionTitle("備考");
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(noteTitle);
        container.add(titlePanel);

        noteArea = new JTextArea(5, 20);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        JScrollPane noteScrollPane = new JScrollPane(noteArea);
        registerComponent("noteArea", noteArea);

        container.add(createFormRow(new JLabel(""), noteScrollPane, "noteArea"));
    }

    // === 共通バリデーション機能（重複削減） ===

    /**
     * 共通入力検証を実行
     * サブクラスでオーバーライドして追加の検証を実装可能
     *
     * @return 検証成功の場合true、失敗の場合false
     */
    protected boolean validateCommonInput() {
        boolean isValid = true;

        // 氏名の検証
        if (isEmpty(nameField)) {
            showFieldError("nameField", MessageEnum.VALIDATION_ERROR_NAME.getMessage());
            isValid = false;
        } else if (nameField.getText().length() > 20) {
            showFieldError("nameField", MessageEnum.VALIDATION_ERROR_NAME.getMessage());
            isValid = false;
        }

        // 氏名 (カナ)の検証
        Validator kanaValidator = ValidatorEnum.NAME_KANA.getValidator();
        if (!kanaValidator.validate(nameKanaField.getText())) {
            showFieldError("nameKanaField", kanaValidator.getErrorMessage());
            isValid = false;
        }

        // 社員IDの検証（詳細画面では編集不可のためスキップ）
        if (idField.isEditable()) {
            if (isEmpty(idField)) {
                showFieldError("idField", MessageEnum.VALIDATION_ERROR_EMPLOYEE_ID.getMessage());
                isValid = false;
            } else {
                String idValue = IDValidator.convertFullWidthToHalfWidth(idField.getText().trim());
                if (!IDValidator.checkIdFormat(idValue)) {
                    showFieldError("idField", "社員IDは5桁以内の数字で入力してください");
                    isValid = false;
                } else if (IDValidator.isForbiddenId(idValue)) {
                    showFieldError("idField", "ID00000は使用できません");
                    isValid = false;
                }
            }
        }

        // 生年月日の検証
        if (!validateDateComponents("birthYearComboBox", "birthMonthComboBox", "birthDayComboBox",
                "birthDate", true, MessageEnum.VALIDATION_ERROR_BIRTH_DATE.getMessage())) {
            isValid = false;
        }

        // 入社年月の検証
        if (!validateDateComponents("joinYearComboBox", "joinMonthComboBox", null,
                "joinDate", true, MessageEnum.VALIDATION_ERROR_JOIN_DATE.getMessage())) {
            isValid = false;
        }

        // エンジニア歴の検証
        if (isEmptyComboBox(careerComboBox)) {
            showFieldError("careerComboBox", MessageEnum.VALIDATION_ERROR_CAREER.getMessage());
            isValid = false;
        }

        // 扱える言語の検証
        if (languageComboBox.getSelectedItems().isEmpty()) {
            showFieldError("languages", MessageEnum.VALIDATION_ERROR_PROGRAMMING_LANGUAGES.getMessage());
            markComponentError("languageComboBox", null);
            isValid = false;
        }

        // 経歴の文字数検証
        if (careerHistoryArea.getText().length() > 200) {
            showFieldError("careerHistoryArea", MessageEnum.VALIDATION_ERROR_CAREER_HISTORY.getMessage());
            isValid = false;
        }

        // 研修の受講歴の文字数検証
        if (trainingHistoryArea.getText().length() > 200) {
            showFieldError("trainingHistoryArea", MessageEnum.VALIDATION_ERROR_TRAINING_HISTORY.getMessage());
            isValid = false;
        }

        // 備考の文字数検証
        if (noteArea.getText().length() > 500) {
            showFieldError("noteArea", MessageEnum.VALIDATION_ERROR_NOTE.getMessage());
            isValid = false;
        }

        return isValid;
    }

    /**
     * コンボボックスから日付オブジェクトを取得
     */
    protected LocalDate getDateFromComponents(JComboBox<String> yearComboBox,
            JComboBox<String> monthComboBox, JComboBox<String> dayComboBox) {
        try {
            String yearStr = (String) yearComboBox.getSelectedItem();
            String monthStr = (String) monthComboBox.getSelectedItem();

            if (yearStr == null || yearStr.isEmpty() || monthStr == null || monthStr.isEmpty()) {
                return null;
            }

            int year = Integer.parseInt(yearStr);
            int month = Integer.parseInt(monthStr);

            if (dayComboBox != null) {
                String dayStr = (String) dayComboBox.getSelectedItem();
                if (dayStr == null || dayStr.isEmpty()) {
                    return null;
                }
                int day = Integer.parseInt(dayStr);
                return LocalDate.of(year, month, day);
            }

            return LocalDate.of(year, month, 1);

        } catch (NumberFormatException | java.time.DateTimeException e) {
            return null;
        }
    }

    // === 既存の共通機能（保持） ===

    /**
     * フィールドのエラーメッセージラベルを作成
     */
    protected JLabel createFieldErrorLabel(String fieldName) {
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(ERROR_COLOR);
        errorLabel.setVisible(false);
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN, 11f));
        errorLabel.setPreferredSize(new Dimension(300, 15));

        fieldErrorLabels.put(fieldName, errorLabel);
        registerComponent(fieldName + "ErrorLabel", errorLabel);

        return errorLabel;
    }

    /**
     * ボタンパネルにボタンを追加
     */
    protected JButton addButton(JButton button) {
        if (buttonPanel != null) {
            buttonPanel.add(button);
            return button;
        }
        return null;
    }

    /**
     * ボタンパネルにコンポーネントを追加
     */
    protected Component addButtonPanelComponent(Component component) {
        if (buttonPanel != null) {
            buttonPanel.add(component);
            return component;
        }
        return null;
    }

    /**
     * コンポーネントを登録
     */
    protected Component registerComponent(String name, Component component) {
        components.put(name, component);
        return component;
    }

    /**
     * 名前でコンポーネントを取得
     */
    protected Component getComponent(String name) {
        return components.get(name);
    }

    /**
     * 必須項目のラベルを作成
     */
    protected JLabel createFieldLabel(String labelText, boolean required) {
        JLabel label = new JLabel(labelText + (required ? REQUIRED_MARK : ""));
        return label;
    }

    /**
     * セクションタイトルを作成
     */
    protected JLabel createSectionTitle(String titleText) {
        JLabel title = new JLabel(titleText);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        return title;
    }

    /**
     * フォーム行パネルを作成
     */
    protected JPanel createFormRow(JLabel label, Component field, String fieldName) {
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.Y_AXIS));
        rowPanel.setBackground(Color.WHITE);

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBackground(Color.WHITE);

        topPanel.add(label, BorderLayout.WEST);

        JLabel errorLabel = createFieldErrorLabel(fieldName);
        topPanel.add(errorLabel, BorderLayout.CENTER);

        JPanel fieldPanel = new JPanel(new BorderLayout());
        fieldPanel.setBackground(Color.WHITE);
        fieldPanel.add(field, BorderLayout.CENTER);

        rowPanel.add(topPanel);
        rowPanel.add(Box.createVerticalStrut(LABEL_FIELD_MARGIN));
        rowPanel.add(fieldPanel);

        return rowPanel;
    }

    /**
     * 水平方向のスペーサーを作成
     */
    protected Component createVerticalSpacer(int height) {
        return Box.createVerticalStrut(height);
    }

    /**
     * エラーメッセージを表示
     */
    protected void showErrorMessage(String message) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(true);
        LogHandler.getInstance().log(Level.WARNING, LogType.UI, "エラーメッセージを表示: " + message);
    }

    /**
     * フィールド固有のエラーメッセージを表示
     */
    protected void showFieldError(String fieldName, String errorMessage) {
        JLabel errorLabel = fieldErrorLabels.get(fieldName);

        if (errorLabel != null) {
            errorLabel.setText(errorMessage);
            errorLabel.setVisible(true);
            markComponentError(fieldName, null);

            LogHandler.getInstance().log(Level.WARNING, LogType.UI,
                    "フィールドエラーを表示: " + fieldName + " - " + errorMessage);
        } else {
            showErrorMessage(errorMessage);
            markComponentError(fieldName, null);

            LogHandler.getInstance().log(Level.WARNING, LogType.UI,
                    "フィールドエラーラベルが見つからないため全体エラーに表示: " + fieldName);
        }
    }

    /**
     * エラーメッセージをクリア
     */
    protected void clearErrorMessage() {
        errorMessageLabel.setText("");
        errorMessageLabel.setVisible(false);
    }

    /**
     * すべてのフィールドエラーメッセージをクリア
     */
    protected void clearAllFieldErrors() {
        for (JLabel errorLabel : fieldErrorLabels.values()) {
            errorLabel.setText(" ");
            errorLabel.setVisible(false);
        }
        clearErrorMessage();
    }

    /**
     * コンポーネントにエラー表示を設定
     */
    protected void markComponentError(String componentName, String errorMessage) {
        Component component = getComponent(componentName);
        if (component == null) {
            return;
        }

        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;

            if (!originalBorders.containsKey(jComponent)) {
                originalBorders.put(jComponent, jComponent.getBorder());
            }

            jComponent.setBorder(ERROR_BORDER);
            errorComponents.put(componentName, component);

            if (errorMessage != null) {
                showErrorMessage(errorMessage);
            }
        }

        // MultiSelectComboBoxの特別処理
        if ("languages".equals(componentName) && languageComboBox != null) {
            if (!originalBorders.containsKey(languageComboBox)) {
                originalBorders.put(languageComboBox, languageComboBox.getBorder());
            }
            languageComboBox.setBorder(ERROR_BORDER);
            errorComponents.put("languageComboBox", languageComboBox);
        }
    }

    /**
     * コンポーネントのエラー表示をクリア
     */
    protected void clearComponentError(String componentName) {
        Component component = errorComponents.remove(componentName);
        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;

            Border originalBorder = originalBorders.remove(jComponent);
            if (originalBorder != null) {
                jComponent.setBorder(originalBorder);
            } else {
                jComponent.setBorder(null);
            }
        }

        JLabel errorLabel = fieldErrorLabels.get(componentName);
        if (errorLabel != null) {
            errorLabel.setText(" ");
            errorLabel.setVisible(false);
        }

        // MultiSelectComboBoxの特別処理
        if ("languages".equals(componentName) && languageComboBox != null) {
            Border originalBorder = originalBorders.remove(languageComboBox);
            if (originalBorder != null) {
                languageComboBox.setBorder(originalBorder);
            } else {
                languageComboBox.setBorder(null);
            }
            errorComponents.remove("languageComboBox");
        }
    }

    /**
     * すべてのコンポーネントのエラー表示をクリア
     */
    protected void clearAllComponentErrors() {
        List<String> componentNames = new ArrayList<>(errorComponents.keySet());

        for (String componentName : componentNames) {
            clearComponentError(componentName);
        }

        clearAllFieldErrors();
        clearErrorMessage();
    }

    /**
     * 日付選択コンポーネントの入力検証
     */
    protected boolean validateDateComponents(String yearFieldName, String monthFieldName, String dayFieldName,
            String groupName, boolean required, String errorMessage) {
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

        if (required) {
            if (yearStr.isEmpty() || monthStr.isEmpty() || (dayCombo != null && dayStr.isEmpty())) {
                showFieldError(groupName, errorMessage);
                if (yearStr.isEmpty())
                    markComponentError(yearFieldName, null);
                if (monthStr.isEmpty())
                    markComponentError(monthFieldName, null);
                if (dayCombo != null && dayStr.isEmpty())
                    markComponentError(dayFieldName, null);
                return false;
            }
        } else if (yearStr.isEmpty() && monthStr.isEmpty() && (dayCombo == null || dayStr.isEmpty())) {
            clearComponentError(yearFieldName);
            clearComponentError(monthFieldName);
            if (dayFieldName != null)
                clearComponentError(dayFieldName);
            clearComponentError(groupName);
            return true;
        }

        if ((!yearStr.isEmpty() || !monthStr.isEmpty() || (dayCombo != null && !dayStr.isEmpty()))
                && (yearStr.isEmpty() || monthStr.isEmpty() || (dayCombo != null && dayStr.isEmpty()))) {
            showFieldError(groupName, errorMessage);
            if (yearStr.isEmpty())
                markComponentError(yearFieldName, null);
            if (monthStr.isEmpty())
                markComponentError(monthFieldName, null);
            if (dayCombo != null && dayStr.isEmpty())
                markComponentError(dayFieldName, null);
            return false;
        }

        try {
            int year = Integer.parseInt(yearStr);
            int month = Integer.parseInt(monthStr);
            int day = dayCombo != null ? Integer.parseInt(dayStr) : 1;

            java.time.LocalDate.of(year, month, day);

            clearComponentError(yearFieldName);
            clearComponentError(monthFieldName);
            if (dayFieldName != null)
                clearComponentError(dayFieldName);
            clearComponentError(groupName);

            return true;
        } catch (Exception e) {
            showFieldError(groupName, errorMessage);
            markComponentError(yearFieldName, null);
            markComponentError(monthFieldName, null);
            if (dayFieldName != null)
                markComponentError(dayFieldName, null);
            return false;
        }
    }

    /**
     * 全コンポーネントの有効・無効を切り替え
     */
    protected void setAllComponentsEnabled(boolean enabled) {
        for (Component component : components.values()) {
            if (!(component instanceof JLabel && fieldErrorLabels.containsValue(component))) {
                component.setEnabled(enabled);
            }
        }
    }

    /**
     * テキストコンポーネントが空かどうかを確認
     */
    protected boolean isEmpty(JTextComponent component) {
        return component == null || component.getText() == null || component.getText().trim().isEmpty();
    }

    /**
     * コンボボックスが空の選択肢かどうかを確認
     */
    protected boolean isEmptyComboBox(JComboBox<?> comboBox) {
        Object selected = comboBox.getSelectedItem();
        return selected == null || selected.toString().isEmpty();
    }

    // Getter用メソッド群
    protected JComboBox<?> getComboBox(String name) {
        Component component = getComponent(name);
        if (component instanceof JComboBox) {
            return (JComboBox<?>) component;
        }
        return null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    protected JPanel getPanel() {
        return panel;
    }

    protected JLabel getFieldErrorLabel(String fieldName) {
        return fieldErrorLabels.get(fieldName);
    }

    /**
     * 入力検証を実行
     * サブクラスでオーバーライドして具体的な検証ロジックを実装
     */
    protected abstract boolean validateInput();
}