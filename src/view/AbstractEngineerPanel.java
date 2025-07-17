package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.text.AbstractDocument;
import javax.swing.text.JTextComponent;

import util.LogHandler;
import util.LogHandler.LogType;
import util.PropertiesManager;
import util.TextLengthFilter;
import util.Constants.MessageEnum;
import util.Constants.SystemConstants;
import util.Constants.UIConstants;
import util.validator.FieldValidator;
import util.validator.IDValidator;
import util.validator.ValidationResult;
import util.validator.ValidationService;
import util.validator.ValidatorFactory;

/**
 * エンジニア情報関連パネル（詳細・追加画面）の基本機能を提供する抽象クラス
 * 共通UIコンポーネント管理、レイアウト、入力検証、フォーム作成機能を統合
 *
 * このクラスは、エンジニア情報の詳細表示・編集・追加に関連するパネル（DetailPanel、AddPanel）の
 * 共通基盤として機能します。Template Methodパターンを活用して、共通処理を定義しつつ、
 * サブクラス固有の振る舞いをフックメソッドで拡張できるようにしています。
 *
 * @author Nakano
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

    /** エラー状態のコンポーネントを管理するマップ */
    protected final Map<String, Component> errorComponents;

    /** デフォルトのボーダー保存用マップ */
    protected final Map<JComponent, Border> originalBorders;

    /** エラー表示用のボーダー */
    protected static final Border ERROR_BORDER = BorderFactory.createLineBorder(UIConstants.ERROR_COLOR, 2);

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

    /** バリデーションサービス */
    private final ValidationService validationService;

    /** バリデータマップ */
    private Map<String, FieldValidator> validators;

    /** バリデーション結果 */
    private ValidationResult lastValidationResult;

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
        this.validationService = ValidationService.getInstance();
        this.initializeFieldDisplayNames();
        this.initializeValidators();
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "AbstractEngineerPanelを初期化完了");
    }

    /**
     * バリデータの初期化
     * IDセットを収集してバリデータを作成
     */
    private void initializeValidators() {
        try {
            // 既存のIDセットを取得（重複チェック用）
            Set<String> existingIds = getExistingEngineerIds();

            // バリデータマップを生成
            this.validators = ValidatorFactory.createEngineerValidators(existingIds);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "バリデータを初期化完了: " + validators.size() + "個");
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "バリデータの初期化中にエラーが発生", _e);
            // フォールバック用の空マップ
            this.validators = new HashMap<>();
        }
    }

    /**
     * エンジニアIDセットを取得
     * サブクラスでオーバーライドして実装
     */
    protected Set<String> getExistingEngineerIds() {
        // デフォルトは空セット（サブクラスで実装）
        return new HashSet<>();
    }

    /**
     * フィールド名と表示名のマッピングを初期化
     * バリデーションエラー表示時に使用
     */
    private void initializeFieldDisplayNames() {
        fieldDisplayNames.put("name", "氏名");
        fieldDisplayNames.put("nameKana", "氏名 (カナ)");
        fieldDisplayNames.put("id", "社員ID");
        fieldDisplayNames.put("birthDate", "生年月日");
        fieldDisplayNames.put("joinDate", "入社年月");
        fieldDisplayNames.put("career", "エンジニア歴");
        fieldDisplayNames.put("programmingLanguages", "扱える言語");
        fieldDisplayNames.put("careerHistory", "経歴");
        fieldDisplayNames.put("trainingHistory", "研修の受講歴");
        fieldDisplayNames.put("technicalSkill", "技術力");
        fieldDisplayNames.put("learningAttitude", "受講態度");
        fieldDisplayNames.put("communicationSkill", "コミュニケーション能力");
        fieldDisplayNames.put("leadership", "リーダーシップ");
        fieldDisplayNames.put("note", "備考");
    }

    /**
     * フィールド名からコンポーネント名へのマッピングを取得
     * バリデーションエラー表示で使用
     */
    private String getComponentNameFromFieldName(String fieldName) {
        switch (fieldName) {
            case "name":
                return "nameField";
            case "nameKana":
                return "nameKanaField";
            case "id":
                return "idField";
            case "career":
                return "careerComboBox";
            case "careerHistory":
                return "careerHistoryArea";
            case "trainingHistory":
                return "trainingHistoryArea";
            case "technicalSkill":
                return "technicalSkillComboBox";
            case "learningAttitude":
                return "learningAttitudeComboBox";
            case "communicationSkill":
                return "communicationSkillComboBox";
            case "leadership":
                return "leadershipComboBox";
            case "note":
                return "noteArea";
            case "programmingLanguages":
                return "languages"; // 特別なマッピング
            case "birthDate":
                return "birthDate"; // 日付コンポーネント群
            case "joinDate":
                return "joinDate"; // 日付コンポーネント群
            default:
                return fieldName; // デフォルトはそのまま返す
        }
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

            // 最新のエンジニアIDセットでバリデータを初期化
            Set<String> currentIds = getExistingEngineerIds();
            updateExistingIds(currentIds);

            // 初期化済みフラグをセット
            initialized = true;

            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    this.getClass().getSimpleName() + "を初期化完了");

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.UI,
                    "パネルの初期化中にエラーが発生: " + this.getClass().getSimpleName(), _e);
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
        panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 0));
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
        errorMessageLabel.setForeground(UIConstants.ERROR_COLOR);
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

    // === 共通フォーム作成メソッド ===

    /**
     * 基本情報セクションの作成（エラーラベル対応版）
     * 氏名、氏名(カナ)、社員IDフィールドで適切なエラー表示を実装
     */
    protected void createBasicInfoSection(JPanel container, boolean isDetailMode) {
        // 社員IDフィールド（必須）- 10文字制限
        JLabel idLabel = createFieldLabel("社員ID", true);
        idField = new JTextField(20);
        if (isDetailMode) {
            idField.setEditable(false);
            idField.setBackground(new Color(240, 240, 240));
        } else {
            // 編集可能な場合のみ文字数制限を適用
            applyTextLengthFilter(idField, getEmployeeIdMaxLength(), "社員ID");
        }
        registerComponent("idField", idField);
        container.add(createFormRow(idLabel, idField, "id"));

        // 氏名フィールド（必須）- 20文字制限
        JLabel nameLabel = createFieldLabel("氏名", true);
        nameField = new JTextField(20);
        applyTextLengthFilter(nameField, SystemConstants.MAX_NAME_LENGTH, "氏名");
        registerComponent("nameField", nameField);
        container.add(createFormRow(nameLabel, nameField, "name"));

        // 氏名 (カナ)フィールド（必須）- 20文字制限
        JLabel nameKanaLabel = createFieldLabel("氏名 (カナ)", true);
        nameKanaField = new JTextField(20);
        applyTextLengthFilter(nameKanaField, getNameKanaMaxLength(), "氏名（カナ）");
        registerComponent("nameKanaField", nameKanaField);
        container.add(createFormRow(nameKanaLabel, nameKanaField, "nameKana"));

        // 生年月日（必須）
        createBirthDateSection(container);
        ActionListener birthDateUpdateListener = _e -> updateDayOptions(
                birthYearComboBox, birthMonthComboBox, birthDayComboBox);

        birthYearComboBox.addActionListener(birthDateUpdateListener);
        birthMonthComboBox.addActionListener(birthDateUpdateListener);

        // 入社年月（必須）
        createJoinDateSection(container);

        // エンジニア歴（必須）
        createCareerSection(container);

        container.add(createVerticalSpacer(20));
    }

    /**
     * テキストフィールドに文字数制限を適用
     * Unicode文字（サロゲートペア、絵文字）を考慮した制限を実装
     * 
     * @param textField 対象のテキストフィールド
     * @param maxLength 最大文字数
     * @param fieldName フィールド名（ログ用）
     */
    private void applyTextLengthFilter(JTextComponent textComponent, int maxLength, String fieldName) {
        try {

            // DocumentFilterを適用
            TextLengthFilter filter = new TextLengthFilter(maxLength, fieldName);
            ((AbstractDocument) textComponent.getDocument()).setDocumentFilter(filter);

            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    fieldName + "フィールドに" + maxLength + "文字制限を適用しました");

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.UI,
                    fieldName + "フィールドへの文字数制限適用中にエラーが発生", e);
        }
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
        birthYearComboBox.setPreferredSize(new Dimension(100, 25));
        registerComponent("birthYearComboBox", birthYearComboBox);
        birthDatePanel.add(birthYearComboBox);
        birthDatePanel.add(new JLabel("年"));

        // 月コンボボックス
        birthMonthComboBox = new JComboBox<>(DateOptionUtil.getMonthOptions());
        birthMonthComboBox.setPreferredSize(new Dimension(80, 25));
        registerComponent("birthMonthComboBox", birthMonthComboBox);
        birthDatePanel.add(birthMonthComboBox);
        birthDatePanel.add(new JLabel("月"));

        // 日コンボボックス
        birthDayComboBox = new JComboBox<>(DateOptionUtil.getDayOptions());
        birthDayComboBox.setPreferredSize(new Dimension(80, 25));
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
        joinYearComboBox.setPreferredSize(new Dimension(100, 25));
        registerComponent("joinYearComboBox", joinYearComboBox);
        joinDatePanel.add(joinYearComboBox);
        joinDatePanel.add(new JLabel("年"));

        // 月コンボボックス
        joinMonthComboBox = new JComboBox<>(DateOptionUtil.getMonthOptions());
        joinMonthComboBox.setPreferredSize(new Dimension(80, 25));
        registerComponent("joinMonthComboBox", joinMonthComboBox);
        joinDatePanel.add(joinMonthComboBox);
        joinDatePanel.add(new JLabel("月"));

        createFieldErrorLabel("joinDate");
        container.add(createFormRow(joinDateLabel, joinDatePanel, "joinDate"));
    }

    /**
     * 入社年月部分の作成後に以下のコードを追加：
     * （入社年月は日を使用しないため、日コンボボックスへの影響はなし）
     */

    /**
     * 年月選択に基づいて日の選択肢を動的に更新
     * 
     * @param yearCombo  年選択コンボボックス
     * @param monthCombo 月選択コンボボックス
     * @param dayCombo   日選択コンボボックス（nullの場合は更新しない）
     */
    private void updateDayOptions(JComboBox<String> yearCombo,
            JComboBox<String> monthCombo,
            JComboBox<String> dayCombo) {
        if (dayCombo == null) {
            return;
        }

        // 現在選択されている日を保持
        String currentDay = (String) dayCombo.getSelectedItem();

        // 年と月の選択値を取得
        String selectedYear = (String) yearCombo.getSelectedItem();
        String selectedMonth = (String) monthCombo.getSelectedItem();

        // 新しい日の選択肢を生成
        String[] newDayOptions = DateOptionUtil.getDayOptions(selectedYear, selectedMonth);

        // コンボボックスモデルを更新
        dayCombo.setModel(new DefaultComboBoxModel<>(newDayOptions));

        // 以前の選択値が新しい選択肢に存在する場合は再選択
        if (currentDay != null && currentDay.trim().length() > 0) {
            for (String option : newDayOptions) {
                if (currentDay.equals(option)) {
                    dayCombo.setSelectedItem(currentDay);
                    return;
                }
            }
        }

        // 選択値が存在しない場合は空文字を選択
        dayCombo.setSelectedIndex(0);
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

        createFieldErrorLabel("career");

        container.add(createFormRow(careerLabel, careerPanel, "career"));
    }

    /**
     * 言語スキルセクションの作成
     *
     * @param container 配置先のコンテナ
     */
    protected void createLanguageSection(JPanel container) {
        // 言語選択のラベルを作成（他のフィールドと統一）
        JLabel languageLabel = createFieldLabel("扱える言語", true);

        // MultiSelectComboBoxの作成
        languageComboBox = FormComponentUtil.createLanguageComboBox();
        registerComponent("languageComboBox", languageComboBox);
        registerComponent("languages", languageComboBox);

        // 他のフィールドと同様にcreateFormRowを使用
        // これにより、一貫したレイアウトと左寄せが実現される
        container.add(createFormRow(languageLabel, languageComboBox, "languages"));

        // 他のセクションとの間隔を統一
        container.add(createVerticalSpacer(20));
    }

    /**
     * 経歴セクションの作成
     *
     * @param container 配置先のコンテナ
     */
    protected void createCareerHistorySection(JPanel container) {
        JLabel careerHistoryLabel = createFieldLabel("経歴", false);

        careerHistoryArea = new JTextArea(5, 40);
        careerHistoryArea.setLineWrap(true);
        careerHistoryArea.setWrapStyleWord(true);
        careerHistoryArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        // 200文字制限を適用
        applyTextLengthFilter(careerHistoryArea, SystemConstants.MAX_CAREER_HISTORY_LENGTH, "経歴");

        JScrollPane careerHistoryScrollPane = new JScrollPane(careerHistoryArea);
        registerComponent("careerHistoryArea", careerHistoryArea);

        container.add(createFormRow(careerHistoryLabel, careerHistoryScrollPane, "careerHistory"));
        container.add(createVerticalSpacer(20));
    }

    /**
     * 研修の受講歴セクションの作成
     *
     * @param container 配置先のコンテナ
     */
    protected void createTrainingSection(JPanel container) {
        JLabel trainingHistoryLabel = createFieldLabel("研修の受講歴", false);

        trainingHistoryArea = new JTextArea(5, 40);
        trainingHistoryArea.setLineWrap(true);
        trainingHistoryArea.setWrapStyleWord(true);
        trainingHistoryArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        // 200文字制限を適用
        applyTextLengthFilter(trainingHistoryArea, SystemConstants.MAX_TRAINING_HISTORY_LENGTH, "研修の受講歴");

        JScrollPane trainingHistoryScrollPane = new JScrollPane(trainingHistoryArea);
        registerComponent("trainingHistoryArea", trainingHistoryArea);

        container.add(createFormRow(trainingHistoryLabel, trainingHistoryScrollPane, "trainingHistory"));
        container.add(createVerticalSpacer(20));
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
        technicalSkillComboBox.setSelectedIndex(0);

        // 受講態度
        createSkillField(container, "受講態度",
                learningAttitudeComboBox = new JComboBox<>(DateOptionUtil.getSkillRatingOptions()),
                "learningAttitudeComboBox", skillComboBoxSize);
        learningAttitudeComboBox.setSelectedIndex(0);

        // コミュニケーション能力
        createSkillField(container, "コミュニケーション能力",
                communicationSkillComboBox = new JComboBox<>(DateOptionUtil.getSkillRatingOptions()),
                "communicationSkillComboBox", skillComboBoxSize);
        communicationSkillComboBox.setSelectedIndex(0);

        // リーダーシップ
        createSkillField(container, "リーダーシップ",
                leadershipComboBox = new JComboBox<>(DateOptionUtil.getSkillRatingOptions()),
                "leadershipComboBox", skillComboBoxSize);
        leadershipComboBox.setSelectedIndex(0);

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
        JLabel noteLabel = createFieldLabel("備考", false);

        noteArea = new JTextArea(5, 40);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        // 500文字制限を適用
        applyTextLengthFilter(noteArea, SystemConstants.MAX_NOTE_LENGTH, "備考");

        JScrollPane noteScrollPane = new JScrollPane(noteArea);
        registerComponent("noteArea", noteArea);

        container.add(createFormRow(noteLabel, noteScrollPane, "note"));
        container.add(createVerticalSpacer(20));
    }

    // === 共通バリデーション機能（重複削減） ===

    /**
     * 共通入力検証を実行
     * 
     * @return 検証成功の場合true、失敗の場合false
     */
    protected boolean validateCommonInput() {
        try {
            // フォームデータを収集
            Map<String, String> formData = collectFormData();

            // バリデーション実行
            lastValidationResult = validationService.validateForm(formData, validators);

            // エラー表示をクリア
            clearAllComponentErrors();

            if (lastValidationResult.isValid()) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "フォームバリデーション成功");
                return true;
            } else {
                // エラーを表示
                displayValidationErrors(lastValidationResult);
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "フォームバリデーション失敗: エラー数=" + lastValidationResult.getErrorCount());
                return false;
            }

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "バリデーション実行中にエラーが発生", _e);
            showErrorMessage("入力検証中にエラーが発生: " + _e.getMessage());
            return false;
        }
    }

    /**
     * フォームデータを収集
     * 
     * @return フィールド名と値のマップ
     */
    private Map<String, String> collectFormData() {
        Map<String, String> formData = new HashMap<>();

        // 基本情報
        formData.put("name", nameField.getText());
        formData.put("nameKana", nameKanaField.getText());
        formData.put("id", idField.getText());

        // 生年月日
        formData.put("birthDate", getDateString(birthYearComboBox, birthMonthComboBox, birthDayComboBox));

        // 入社年月
        formData.put("joinDate", getDateString(joinYearComboBox, joinMonthComboBox, null));

        // エンジニア歴
        formData.put("career", getComboBoxValue(careerComboBox));

        // 扱える言語（セミコロン区切り）
        if (languageComboBox != null) {
            List<String> languages = languageComboBox.getSelectedItems();
            formData.put("programmingLanguages", String.join(";", languages));
        }

        // 経歴・研修・備考
        formData.put("careerHistory", careerHistoryArea.getText());
        formData.put("trainingHistory", trainingHistoryArea.getText());
        formData.put("note", noteArea.getText());

        // スキル評価
        formData.put("technicalSkill", getComboBoxValue(technicalSkillComboBox));
        formData.put("learningAttitude", getComboBoxValue(learningAttitudeComboBox));
        formData.put("communicationSkill", getComboBoxValue(communicationSkillComboBox));
        formData.put("leadership", getComboBoxValue(leadershipComboBox));

        return formData;
    }

    /**
     * 社員IDの最大文字数を取得
     * 設定値から取得、未設定の場合はデフォルト値（10）を返す
     * 
     * @return 社員IDの最大文字数
     */
    private int getEmployeeIdMaxLength() {
        try {
            return PropertiesManager.getInstance().getInt("validation.employee.id.max.length", 10);
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "社員ID最大文字数の取得に失敗、デフォルト値を使用", e);
            return 10;
        }
    }

    /**
     * フリガナの最大文字数を取得
     * 設定値から取得、未設定の場合はデフォルト値（20）を返す
     * 
     * @return フリガナの最大文字数
     */
    private int getNameKanaMaxLength() {
        try {
            return PropertiesManager.getInstance().getInt("validation.max.name.kana.length", 20);
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "フリガナ最大文字数の取得に失敗、デフォルト値を使用", e);
            return 20;
        }
    }

    /**
     * 日付コンポーネントから日付文字列を生成
     */
    private String getDateString(JComboBox<String> yearCombo, JComboBox<String> monthCombo,
            JComboBox<String> dayCombo) {
        String year = getComboBoxValue(yearCombo);
        String month = getComboBoxValue(monthCombo);

        if (year.isEmpty() || month.isEmpty()) {
            return "";
        }

        // 月を2桁にパディング
        month = String.format("%02d", Integer.parseInt(month));

        if (dayCombo != null) {
            String day = getComboBoxValue(dayCombo);
            if (day.isEmpty()) {
                return "";
            }
            // 日を2桁にパディング
            day = String.format("%02d", Integer.parseInt(day));
            return year + "-" + month + "-" + day;
        } else {
            // 入社年月の場合は日を01に設定
            return year + "-" + month + "-01";
        }
    }

    /**
     * コンボボックスの値を取得
     */
    private String getComboBoxValue(JComboBox<String> comboBox) {
        if (comboBox == null) {
            return "";
        }
        Object selected = comboBox.getSelectedItem();
        return selected != null ? selected.toString() : "";
    }

    /**
     * バリデーションエラーを表示（エラーラベル対応版）
     * フィールド名とエラーラベルの適切なマッピングを実装
     */
    private void displayValidationErrors(ValidationResult result) {
        Map<String, String> errors = result.getErrors();

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "バリデーションエラー表示開始: " + errors.size() + "個のエラー");

        for (Map.Entry<String, String> entry : errors.entrySet()) {
            String fieldName = entry.getKey();
            String errorMessage = entry.getValue();

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "バリデーションエラー処理: フィールド=" + fieldName + ", メッセージ=" + errorMessage);

            // 特殊なフィールドの処理
            if ("birthDate".equals(fieldName)) {
                showFieldError(fieldName, errorMessage);
                markComponentError("birthYearComboBox", null);
                markComponentError("birthMonthComboBox", null);
                markComponentError("birthDayComboBox", null);
            } else if ("joinDate".equals(fieldName)) {
                showFieldError(fieldName, errorMessage);
                markComponentError("joinYearComboBox", null);
                markComponentError("joinMonthComboBox", null);
            } else if ("career".equals(fieldName)) {
                showFieldError(fieldName, errorMessage);
                markComponentError("careerComboBox", null);
            } else if ("programmingLanguages".equals(fieldName)) {
                showFieldError("languages", errorMessage);
                markComponentError("languageComboBox", null);
            } else if ("careerHistory".equals(fieldName)) {
                // 経歴フィールドの統一されたエラー表示処理
                showFieldError(fieldName, errorMessage);
                markComponentError("careerHistoryArea", null);
            } else if ("trainingHistory".equals(fieldName)) {
                // 研修受講歴フィールドの統一されたエラー表示処理
                showFieldError(fieldName, errorMessage);
                markComponentError("trainingHistoryArea", null);
            } else if ("note".equals(fieldName)) {
                // 備考フィールドの統一されたエラー表示処理
                showFieldError(fieldName, errorMessage);
                markComponentError("noteArea", null);
            } else {
                // 通常のフィールド（氏名、氏名カナ、社員IDなど）
                // フィールド名をそのまま使用してエラーラベルに表示
                showFieldError(fieldName, errorMessage);

                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "通常フィールドエラー表示完了: " + fieldName);
            }
        }

        // 最初のエラーフィールドにフォーカス
        focusFirstErrorField();

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "バリデーションエラー表示完了");
    }

    /**
     * 最初のエラーフィールドにフォーカスを設定
     */
    private void focusFirstErrorField() {
        if (!errorComponents.isEmpty()) {
            Component firstErrorComponent = errorComponents.values().iterator().next();
            if (firstErrorComponent instanceof JComponent) {
                JComponent jComponent = (JComponent) firstErrorComponent;
                SwingUtilities.invokeLater(() -> jComponent.requestFocusInWindow());
            }
        }
    }

    /**
     * 最後のバリデーション結果を取得
     * 
     * @return バリデーション結果（まだ実行されていない場合はnull）
     */
    protected ValidationResult getLastValidationResult() {
        return lastValidationResult;
    }

    /**
     * 既存のIDセットを更新（動的な重複チェック用）
     * 
     * @param existingIds 新しい既存IDセット
     */
    protected void updateExistingIds(Set<String> existingIds) {
        if (validators != null && validators.containsKey("id")) {
            FieldValidator idValidator = validators.get("id");
            if (idValidator instanceof IDValidator) {
                // 新しいIDValidatorインスタンスを作成
                validators.put("id", new IDValidator("id",
                        MessageEnum.VALIDATION_ERROR_EMPLOYEE_ID.getMessage(), existingIds));
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "既存IDセットを更新しました: " + existingIds.size() + "件");
            }
        }
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

        } catch (NumberFormatException | java.time.DateTimeException _e) {
            return null;
        }
    }

    /**
     * フィールドのエラーメッセージラベルを作成
     * ラベルの右部に表示されるエラーメッセージラベルを作成
     */
    protected JLabel createFieldErrorLabel(String fieldName) {
        JLabel errorLabel = new JLabel("");
        errorLabel.setForeground(UIConstants.ERROR_COLOR);
        errorLabel.setVisible(false);
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN, UIConstants.ERROR_MESSAGE_FONT_SIZE));
        errorLabel.setHorizontalAlignment(SwingConstants.LEFT);
        errorLabel.setVerticalAlignment(SwingConstants.CENTER);

        // 最大幅のみ設定し、高さは内容に応じて自動調整
        errorLabel.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));
        // 最小サイズを設定して、短いメッセージでもレイアウトが崩れないようにする
        errorLabel.setMinimumSize(new Dimension(100, 15));

        fieldErrorLabels.put(fieldName, errorLabel);
        registerComponent(fieldName + "ErrorLabel", errorLabel);

        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                "フィールドエラーラベルを作成しました: " + fieldName);

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
        JLabel label = new JLabel(labelText + (required ? UIConstants.REQUIRED_MARK : ""));
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
     * フォーム行パネルを作成（エラーラベル右側表示対応版）
     * フィールドラベルの右側にエラーメッセージを表示するレイアウトを実装
     */
    protected JPanel createFormRow(JLabel label, Component field, String fieldName) {
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.Y_AXIS));
        rowPanel.setBackground(Color.WHITE);

        // 上部パネル：ラベルとエラーメッセージを水平配置
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBackground(Color.WHITE);

        // ラベルを左側に配置
        topPanel.add(label, BorderLayout.WEST);

        // エラーラベルを作成して右側に配置
        JLabel errorLabel = createFieldErrorLabel(fieldName);
        JPanel errorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        errorPanel.setBackground(Color.WHITE);
        errorPanel.add(errorLabel);
        topPanel.add(errorPanel, BorderLayout.CENTER);

        // フィールドパネル：入力コンポーネントを配置
        JPanel fieldPanel = new JPanel(new BorderLayout());
        fieldPanel.setBackground(Color.WHITE);
        fieldPanel.add(field, BorderLayout.WEST);

        // 行パネルに上部パネルとフィールドパネルを追加
        rowPanel.add(topPanel);
        rowPanel.add(Box.createVerticalStrut(UIConstants.LABEL_FIELD_MARGIN));
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
     * createFieldErrorLabelで作成されたラベルに適切にエラーメッセージを表示
     */
    protected void showFieldError(String fieldName, String errorMessage) {
        JLabel errorLabel = fieldErrorLabels.get(fieldName);

        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                "フィールドエラー表示: フィールド=" + fieldName + ", エラーラベル存在=" + (errorLabel != null));

        if (errorLabel != null) {
            // エラーメッセージをラベルに設定
            errorLabel.setText(errorMessage);
            errorLabel.setVisible(true);

            // 対応するコンポーネントにもエラーマークを設定
            String componentName = getComponentNameFromFieldName(fieldName);
            markComponentError(componentName, null);

            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    "フィールドエラーラベルにメッセージを設定: " + fieldName + " - " + errorMessage);
        } else {
            // エラーラベルが見つからない場合は全体エラーメッセージにフォールバック
            LogHandler.getInstance().log(Level.WARNING, LogType.UI,
                    "フィールドエラーラベルが見つからないため全体エラーに表示: " + fieldName);
            showErrorMessage(errorMessage);

            // コンポーネントのエラーマークは設定
            String componentName = getComponentNameFromFieldName(fieldName);
            markComponentError(componentName, null);
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
     * エラーラベルの状態を確実にリセット
     */
    protected void clearAllFieldErrors() {

        for (Map.Entry<String, JLabel> entry : fieldErrorLabels.entrySet()) {
            JLabel errorLabel = entry.getValue();

            if (errorLabel != null) {
                errorLabel.setText("");
                errorLabel.setVisible(false);
            }
        }

        // 全体エラーメッセージもクリア
        clearErrorMessage();

        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                "全フィールドエラークリア完了");
    }

    /**
     * コンポーネントにエラー表示を設定
     * より詳細なログ出力と、テキストフィールドの赤枠表示を強化
     */
    protected void markComponentError(String componentName, String errorMessage) {
        Component component = getComponent(componentName);

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "markComponentError呼び出し: コンポーネント=" + componentName +
                        ", 存在=" + (component != null));

        if (component == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "エラー表示対象のコンポーネントが見つかりません: " + componentName);
            return;
        }

        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;

            // 元のボーダーを保存
            if (!originalBorders.containsKey(jComponent)) {
                originalBorders.put(jComponent, jComponent.getBorder());
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "元のボーダーを保存: " + componentName);
            }

            // エラーボーダーを設定
            jComponent.setBorder(ERROR_BORDER);
            errorComponents.put(componentName, component);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "エラーボーダーを設定: " + componentName +
                            " (" + component.getClass().getSimpleName() + ")");

            // コンポーネントを再描画
            jComponent.repaint();

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
            languageComboBox.repaint();

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "言語選択コンボボックスにエラーボーダーを設定");
        }
    }

    /**
     * コンポーネントのエラー表示をクリア（エラーラベル対応版）
     * ボーダーとエラーラベルの両方を確実にクリア
     */
    protected void clearComponentError(String componentName) {
        // コンポーネントのボーダーをクリア
        Component component = errorComponents.remove(componentName);

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "コンポーネントエラークリア: " + componentName);

        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;

            Border originalBorder = originalBorders.remove(jComponent);
            if (originalBorder != null) {
                jComponent.setBorder(originalBorder);
            } else {
                jComponent.setBorder(null);
            }

            jComponent.repaint();
        }

        // 対応するフィールドエラーラベルもクリア
        String fieldName = getFieldNameFromComponentName(componentName);
        if (fieldName != null) {
            JLabel errorLabel = fieldErrorLabels.get(fieldName);
            if (errorLabel != null) {
                errorLabel.setText("");
                errorLabel.setVisible(false);

                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "対応するエラーラベルもクリア: " + fieldName);
            }
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
            languageComboBox.repaint();
        }
    }

    /**
     * コンポーネント名からフィールド名への逆マッピングを取得
     * エラークリア時に対応するエラーラベルを特定するために使用
     */
    private String getFieldNameFromComponentName(String componentName) {
        switch (componentName) {
            case "nameField":
                return "name";
            case "nameKanaField":
                return "nameKana";
            case "idField":
                return "id";
            case "careerComboBox":
                return "career";
            case "careerHistoryArea":
                return "careerHistory";
            case "trainingHistoryArea":
                return "trainingHistory";
            case "technicalSkillComboBox":
                return "technicalSkill";
            case "learningAttitudeComboBox":
                return "learningAttitude";
            case "communicationSkillComboBox":
                return "communicationSkill";
            case "leadershipComboBox":
                return "leadership";
            case "noteArea":
                return "note";
            case "languageComboBox":
                return "languages";
            case "birthDate":
                return "birthDate";
            case "joinDate":
                return "joinDate";
            default:
                return null;
        }
    }

    /**
     * すべてのコンポーネントのエラー表示をクリア
     * より確実なエラー状態のリセットを実装
     */
    protected void clearAllComponentErrors() {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "全コンポーネントのエラー表示をクリア: " + errorComponents.size() + "個");

        // エラーコンポーネントのリストをコピーして安全にクリア
        List<String> componentNames = new ArrayList<>(errorComponents.keySet());

        for (String componentName : componentNames) {
            clearComponentError(componentName);
        }

        // フィールドエラーラベルもすべてクリア
        clearAllFieldErrors();

        // 全体エラーメッセージもクリア
        clearErrorMessage();

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "全コンポーネントのエラー表示クリアが完了");
    }

    /**
     * テキストフィールドの赤枠表示を強制的に適用
     * 特に氏名、氏名(カナ)、社員IDフィールド用
     */
    protected void forceTextFieldErrorDisplay(String fieldName, String errorMessage) {
        String componentName = getComponentNameFromFieldName(fieldName);
        Component component = getComponent(componentName);

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "テキストフィールドエラー表示強制適用: " + fieldName + " -> " + componentName);

        if (component instanceof JTextField) {
            JTextField textField = (JTextField) component;

            // 元のボーダーを保存
            if (!originalBorders.containsKey(textField)) {
                originalBorders.put(textField, textField.getBorder());
            }

            // 赤枠ボーダーを設定
            textField.setBorder(ERROR_BORDER);
            errorComponents.put(componentName, textField);

            // フィールドエラーラベルも設定
            showFieldError(fieldName, errorMessage);

            // 再描画を強制
            textField.repaint();
            textField.revalidate();

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "テキストフィールドに赤枠を設定: " + componentName);
        }
    }

    /**
     * バリデーションエラーの詳細なデバッグ情報を出力
     */
    protected void debugValidationErrors(ValidationResult result) {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "=== バリデーションエラー詳細情報 ===");
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "有効: " + result.isValid() + ", エラー数: " + result.getErrorCount());

        for (Map.Entry<String, String> error : result.getErrors().entrySet()) {
            String fieldName = error.getKey();
            String componentName = getComponentNameFromFieldName(fieldName);
            Component component = getComponent(componentName);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "フィールド: " + fieldName +
                            " -> コンポーネント: " + componentName +
                            " -> 存在: " + (component != null) +
                            " -> エラー: " + error.getValue());
        }
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "=== バリデーションエラー詳細情報終了 ===");
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
        } catch (Exception _e) {
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