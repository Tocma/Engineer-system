package view;

import model.EngineerDTO;
import model.EngineerBuilder;
import controller.MainController;
import util.LogHandler;
import util.LogHandler.LogType;
import util.MessageEnum;
import util.Validator;
import util.ValidatorEnum;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.event.ActionListener;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * エンジニア情報の詳細表示・編集画面を提供するパネルクラス
 * 一覧画面から選択されたエンジニアの情報を表示・編集し、更新機能を提供します
 *
 * <p>
 * このクラスは、エンジニア人材管理システムにおいて既存のエンジニア情報を
 * 表示・編集するためのユーザーインターフェースを提供します。AbstractEngineerPanelを
 * 継承し、エンジニア情報の入力フォームと更新処理を実装しています。
 * </p>
 *
 * <p>
 * 主な機能：
 * <ul>
 * <li>一覧から選択されたエンジニア情報の表示</li>
 * <li>エンジニア情報の編集</li>
 * <li>更新ボタンによるデータ保存</li>
 * <li>一覧画面への戻り機能</li>
 * <li>入力データの検証</li>
 * <li>フィールド毎のエラー表示</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用方法：
 * <ol>
 * <li>一覧画面でエンジニア行をダブルクリック</li>
 * <li>詳細画面に選択したエンジニア情報が表示される</li>
 * <li>情報を編集</li>
 * <li>更新ボタンをクリックして変更を保存</li>
 * </ol>
 * </p>
 *
 * @author Nakano
 * @version 4.8.4
 * @since 2025-05-20
 */
public class DetailPanel extends AbstractEngineerPanel {

    /** メインコントローラー参照 */
    private MainController mainController;

    /** 表示・編集中のエンジニア情報 */
    private EngineerDTO currentEngineer;

    /** 更新ボタン */
    private JButton updateButton;

    /** 戻るボタン */
    private JButton backButton;

    /** プログレスインジケーター */
    private JLabel progressLabel;

    /** 処理中フラグ */
    private boolean processing;

    /** 完了処理の成功フラグ */
    private boolean handleUpdateCompleteSuccess = false;

    /** 年のコンボボックス（生年月日用） */
    private JComboBox<String> birthYearComboBox;

    /** 月のコンボボックス（生年月日用） */
    private JComboBox<String> birthMonthComboBox;

    /** 日のコンボボックス（生年月日用） */
    private JComboBox<String> birthDayComboBox;

    /** 年のコンボボックス（入社年月用） */
    private JComboBox<String> joinYearComboBox;

    /** 月のコンボボックス（入社年月用） */
    private JComboBox<String> joinMonthComboBox;

    /** エンジニア歴コンボボックス */
    private JComboBox<String> careerComboBox;

    /** 技術力コンボボックス */
    private JComboBox<String> technicalSkillComboBox;

    /** 受講態度コンボボックス */
    private JComboBox<String> learningAttitudeComboBox;

    /** コミュニケーション能力コンボボックス */
    private JComboBox<String> communicationSkillComboBox;

    /** リーダーシップコンボボックス */
    private JComboBox<String> leadershipComboBox;

    /** 扱える言語リスト */
    private List<JCheckBox> languageCheckBoxes;

    /** 言語選択コンボボックス */
    private AddPanel.MultiSelectComboBox languageComboBox;

    /** 氏名フィールド */
    private JTextField nameField;

    /** 氏名 (カナ)フィールド */
    private JTextField nameKanaField;

    /** 社員IDフィールド */
    private JTextField idField;

    /** 経歴テキストエリア */
    private JTextArea careerHistoryArea;

    /** 研修の受講歴テキストエリア */
    private JTextArea trainingHistoryArea;

    /** 備考テキストエリア */
    private JTextArea noteArea;

    /** 最新登録日表示ラベル */
    private JLabel registeredDateLabel;

    /** ダイアログマネージャー */
    private DialogManager dialogManager;

    /** フィールド名と表示名のマッピング */
    private Map<String, String> fieldDisplayNames;

    private boolean formModified = false;

    /**
     * コンストラクタ
     * パネルの初期設定とコンポーネントの初期化
     */
    public DetailPanel() {
        super();
        this.processing = false;
        this.languageCheckBoxes = new ArrayList<>();
        this.dialogManager = DialogManager.getInstance();
        this.fieldDisplayNames = new HashMap<>();
        initializeFieldDisplayNames();
        LogHandler.getInstance().log(Level.INFO, LogType.UI, "DetailPanelを作成しました");
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
     * メインコントローラーを設定
     * コントローラーを通じてイベント処理や画面遷移を行う
     *
     * @param mainController メインコントローラー
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * 現在表示中のエンジニア情報を取得
     * 
     * @return 現在表示中のエンジニア情報
     */
    public EngineerDTO getCurrentEngineer() {
        return currentEngineer;
    }

    /**
     * パネルの初期化
     * UIコンポーネントの生成と配置を行う
     */
    @Override
    public void initialize() {
        // 親クラスの初期化
        super.initialize();

        try {
            // フォームコンポーネントの作成
            createFormComponents();

            // ボタン領域の作成
            createButtonArea();

            // 入力検証の設定
            setupValidation();

            // 変更リスナーの設定（この行を追加）
            setupChangeListeners();

            LogHandler.getInstance().log(Level.INFO, LogType.UI, "DetailPanelの初期化が完了しました");
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.UI, "DetailPanelの初期化中にエラーが発生しました", e);
        }
    }

    /**
     * フォームコンポーネントの作成と配置
     * 入力フィールドとラベルをパネルに配置
     */
    private void createFormComponents() {
        // 左側のフォームパネル（基本情報）
        JPanel leftFormPanel = new JPanel();
        leftFormPanel.setLayout(new BoxLayout(leftFormPanel, BoxLayout.Y_AXIS));
        leftFormPanel.setBackground(Color.WHITE);

        // 右側のフォームパネル（スキル情報）
        JPanel rightFormPanel = new JPanel();
        rightFormPanel.setLayout(new BoxLayout(rightFormPanel, BoxLayout.Y_AXIS));
        rightFormPanel.setBackground(Color.WHITE);

        // 最新登録日表示パネル（右上に配置）
        JPanel registeredDatePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        registeredDatePanel.setBackground(Color.WHITE);

        // 最新登録日ラベル
        registeredDateLabel = new JLabel("最新登録日: ");
        registeredDateLabel.setFont(registeredDateLabel.getFont().deriveFont(Font.ITALIC));
        registeredDatePanel.add(registeredDateLabel);

        // 右側パネルに最新登録日日時パネルを追加（一番上）
        rightFormPanel.add(registeredDatePanel);
        rightFormPanel.add(createVerticalSpacer(10));

        // 1. 基本情報セクション（左側）
        createBasicInfoSection(leftFormPanel);

        // 2. 言語スキルセクション（左側）
        createLanguageSection(leftFormPanel);

        // 3. 経歴セクション（左側）
        createCareerHistorySection(leftFormPanel);

        // 4. 研修の受講歴セクション（右側）
        createTrainingSection(rightFormPanel);

        // 5. スキルセクション（右側）
        createSkillSection(rightFormPanel);

        // 6. 備考セクション（右側）
        createNoteSection(rightFormPanel);

        // 左右のパネルを水平に配置
        JPanel formContainer = new JPanel(new GridLayout(1, 2, 20, 0));
        formContainer.setBackground(Color.WHITE);
        formContainer.add(leftFormPanel);
        formContainer.add(rightFormPanel);

        // メインパネルに追加
        panel.add(formContainer);
    }

    /**
     * 基本情報セクションの作成
     * 氏名、社員ID、生年月日などの基本情報入力フィールドを配置
     *
     * @param container 配置先のコンテナ
     */
    private void createBasicInfoSection(JPanel container) {
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
        idField.setEditable(false); // 編集不可（主キー）
        idField.setBackground(new Color(240, 240, 240)); // 編集不可の視覚的表示
        registerComponent("idField", idField);
        container.add(createFormRow(idLabel, idField, "idField"));

        // 生年月日（必須）- 年・月・日のコンボボックス
        JLabel birthDateLabel = createFieldLabel("生年月日", true);
        JPanel birthDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        birthDatePanel.setBackground(Color.WHITE);

        // 年コンボボックス
        birthYearComboBox = new JComboBox<>(getYearOptions(1950, LocalDate.now().getYear()));
        birthYearComboBox.setPreferredSize(new Dimension(80, 25));
        registerComponent("birthYearComboBox", birthYearComboBox);
        birthDatePanel.add(birthYearComboBox);
        birthDatePanel.add(new JLabel("年"));

        // 月コンボボックス
        birthMonthComboBox = new JComboBox<>(getMonthOptions());
        birthMonthComboBox.setPreferredSize(new Dimension(60, 25));
        registerComponent("birthMonthComboBox", birthMonthComboBox);
        birthDatePanel.add(birthMonthComboBox);
        birthDatePanel.add(new JLabel("月"));

        // 日コンボボックス
        birthDayComboBox = new JComboBox<>(getDayOptions());
        birthDayComboBox.setPreferredSize(new Dimension(60, 25));
        registerComponent("birthDayComboBox", birthDayComboBox);
        birthDatePanel.add(birthDayComboBox);
        birthDatePanel.add(new JLabel("日"));

        // 生年月日のグループエラー表示用
        createFieldErrorLabel("birthDate");
        container.add(createFormRow(birthDateLabel, birthDatePanel, "birthDate"));

        // 入社年月 - 年・月のコンボボックス
        JLabel joinDateLabel = createFieldLabel("入社年月", true);
        JPanel joinDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        joinDatePanel.setBackground(Color.WHITE);

        // 年コンボボックス
        joinYearComboBox = new JComboBox<>(getYearOptions(1990, LocalDate.now().getYear()));
        joinYearComboBox.setPreferredSize(new Dimension(80, 25));
        registerComponent("joinYearComboBox", joinYearComboBox);
        joinDatePanel.add(joinYearComboBox);
        joinDatePanel.add(new JLabel("年"));

        // 月コンボボックス
        joinMonthComboBox = new JComboBox<>(getMonthOptions());
        joinMonthComboBox.setPreferredSize(new Dimension(60, 25));
        registerComponent("joinMonthComboBox", joinMonthComboBox);
        joinDatePanel.add(joinMonthComboBox);
        joinDatePanel.add(new JLabel("月"));

        // 入社年月のグループエラー表示用
        createFieldErrorLabel("joinDate");
        container.add(createFormRow(joinDateLabel, joinDatePanel, "joinDate"));

        // エンジニア歴（必須）
        JLabel careerLabel = createFieldLabel("エンジニア歴", true);
        JPanel careerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        careerPanel.setBackground(Color.WHITE);

        // エンジニア歴コンボボックス
        careerComboBox = new JComboBox<>(getCareerOptions());
        careerComboBox.setPreferredSize(new Dimension(80, 25));
        registerComponent("careerComboBox", careerComboBox);
        careerPanel.add(careerComboBox);
        careerPanel.add(new JLabel("年"));

        container.add(createFormRow(careerLabel, careerPanel, "careerComboBox"));
        container.add(createVerticalSpacer(20));
    }

    /**
     * 言語スキルセクションの作成
     * プログラミング言語の選択機能をMultiSelectComboBoxで実装
     *
     * @param container 配置先のコンテナ
     */
    private void createLanguageSection(JPanel container) {
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
        titlePanel.add(Box.createHorizontalStrut(10)); // ラベルとエラーの間隔
        titlePanel.add(errorLabel);

        container.add(titlePanel);

        // 利用可能な言語リスト
        String[] availableLanguages = {
                "C++", "C#", "Java", "Python", "JavaScript",
                "TypeScript", "PHP", "Ruby", "Go", "Swift",
                "Kotlin", "SQL", "HTML/CSS",
        };

        // CheckableItemの配列を作成
        AddPanel.CheckableItem[] items = new AddPanel.CheckableItem[availableLanguages.length];
        for (int i = 0; i < availableLanguages.length; i++) {
            items[i] = new AddPanel.CheckableItem(availableLanguages[i]);
        }

        // MultiSelectComboBoxの作成
        languageComboBox = new AddPanel.MultiSelectComboBox(items);
        languageComboBox.setPreferredSize(new Dimension(300, 25));
        registerComponent("languageComboBox", languageComboBox);
        registerComponent("languages", languageComboBox); // エラー表示用に別名でも登録

        container.add(createVerticalSpacer(5));
        container.add(languageComboBox);

        // 既存のチェックボックスリストは初期化しておく（互換性のため）
        languageCheckBoxes = new ArrayList<>();

    }

    /**
     * 経歴セクションの作成
     * 経歴を入力するテキストエリアを配置
     *
     * @param container 配置先のコンテナ
     */
    private void createCareerHistorySection(JPanel container) {
        // セクションタイトル - 左寄せ
        JLabel careerHistoryTitle = createSectionTitle("経歴");
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(careerHistoryTitle);
        container.add(titlePanel);

        // 経歴テキストエリア
        careerHistoryArea = new JTextArea(5, 20);
        careerHistoryArea.setLineWrap(true);
        careerHistoryArea.setWrapStyleWord(true);
        JScrollPane careerScrollPane = new JScrollPane(careerHistoryArea);
        registerComponent("careerHistoryArea", careerHistoryArea);

        container.add(createFormRow(new JLabel(""), careerScrollPane, "careerHistoryArea"));

    }

    /**
     * 研修の受講歴セクションの作成
     * 研修の受講歴を入力するテキストエリアを配置
     *
     * @param container 配置先のコンテナ
     */
    private void createTrainingSection(JPanel container) {
        // セクションタイトル - 左寄せ
        JLabel trainingTitle = createSectionTitle("研修の受講歴");
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(trainingTitle);
        container.add(titlePanel);

        // 研修の受講歴テキストエリア
        trainingHistoryArea = new JTextArea(2, 20);
        trainingHistoryArea.setLineWrap(true);
        trainingHistoryArea.setWrapStyleWord(true);
        JScrollPane trainingScrollPane = new JScrollPane(trainingHistoryArea);
        registerComponent("trainingHistoryArea", trainingHistoryArea);

        container.add(createFormRow(new JLabel(""), trainingScrollPane, "trainingHistoryArea"));

    }

    /**
     * スキルセクションの作成
     * 技術力、受講態度などのスキル評価用コンボボックスを配置
     *
     * @param container 配置先のコンテナ
     */
    private void createSkillSection(JPanel container) {
        // スキルコンボボックスの共通サイズを定義
        Dimension skillComboBoxSize = new Dimension(80, 25);

        // 技術力
        JLabel technicalSkillLabel = createFieldLabel("技術力", false);
        technicalSkillComboBox = new JComboBox<>(getSkillRatingOptions());

        // コンボボックスをパネルで囲み、サイズを固定
        JPanel techSkillComboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        techSkillComboPanel.setBackground(Color.WHITE);
        techSkillComboPanel.setPreferredSize(skillComboBoxSize);
        techSkillComboPanel.add(technicalSkillComboBox);

        registerComponent("technicalSkillComboBox", technicalSkillComboBox);
        container.add(createFormRow(technicalSkillLabel, techSkillComboPanel, "technicalSkillComboBox"));

        // 受講態度
        JLabel learningAttitudeLabel = createFieldLabel("受講態度", false);
        learningAttitudeComboBox = new JComboBox<>(getSkillRatingOptions());

        // コンボボックスをパネルで囲み、サイズを固定
        JPanel attitudeComboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        attitudeComboPanel.setBackground(Color.WHITE);
        attitudeComboPanel.setPreferredSize(skillComboBoxSize);
        attitudeComboPanel.add(learningAttitudeComboBox);

        registerComponent("learningAttitudeComboBox", learningAttitudeComboBox);
        container.add(createFormRow(learningAttitudeLabel, attitudeComboPanel, "learningAttitudeComboBox"));

        // コミュニケーション能力
        JLabel communicationSkillLabel = createFieldLabel("コミュニケーション能力", false);
        communicationSkillComboBox = new JComboBox<>(getSkillRatingOptions());

        // コンボボックスをパネルで囲み、サイズを固定
        JPanel commSkillComboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        commSkillComboPanel.setBackground(Color.WHITE);
        commSkillComboPanel.setPreferredSize(skillComboBoxSize);
        commSkillComboPanel.add(communicationSkillComboBox);

        registerComponent("communicationSkillComboBox", communicationSkillComboBox);
        container.add(createFormRow(communicationSkillLabel, commSkillComboPanel, "communicationSkillComboBox"));

        // リーダーシップ
        JLabel leadershipLabel = createFieldLabel("リーダーシップ", false);
        leadershipComboBox = new JComboBox<>(getSkillRatingOptions());

        // コンボボックスをパネルで囲み、サイズを固定
        JPanel leadershipComboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leadershipComboPanel.setBackground(Color.WHITE);
        leadershipComboPanel.setPreferredSize(skillComboBoxSize);
        leadershipComboPanel.add(leadershipComboBox);

        registerComponent("leadershipComboBox", leadershipComboBox);
        container.add(createFormRow(leadershipLabel, leadershipComboPanel, "leadershipComboBox"));
        container.add(createVerticalSpacer(20));
    }

    /**
     * 備考セクションの作成
     * 備考を入力するテキストエリアを配置
     *
     * @param container 配置先のコンテナ
     */
    private void createNoteSection(JPanel container) {
        // セクションタイトル - 左寄せ
        JLabel noteTitle = createSectionTitle("備考");
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(noteTitle);
        container.add(titlePanel);

        // 備考テキストエリア
        noteArea = new JTextArea(5, 20);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        JScrollPane noteScrollPane = new JScrollPane(noteArea);
        registerComponent("noteArea", noteArea);

        container.add(createFormRow(new JLabel(""), noteScrollPane, "noteArea"));
    }

    /**
     * ボタン領域の作成
     * 「更新」ボタンと「戻る」ボタンを配置
     */
    private void createButtonArea() {
        // 処理中表示ラベル
        progressLabel = new JLabel("保存中...");
        progressLabel.setVisible(false);
        addButtonPanelComponent(progressLabel);

        // 戻るボタン
        backButton = new JButton("一覧へ戻る");
        backButton.addActionListener(e -> {
            if (!processing) {
                goBack();
            }
        });
        addButton(backButton);

        // 更新ボタン
        updateButton = new JButton("保存");
        updateButton.setEnabled(false); // 初期状態では無効化
        updateButton.addActionListener(e -> {
            if (!processing) {
                updateEngineer();
            }
        });
        addButton(updateButton);
    }

    /**
     * 入力検証の設定
     * フォーカス移動時の入力検証など
     */
    private void setupValidation() {
        // 各フィールドのフォーカス喪失時にバリデーションを実行することもできます
        // この実装ではフォーカス時のバリデーションは行わず、更新ボタン押下時に一括検証を行います
    }

    // フォーム変更リスナー設定用メソッド
    private void setupChangeListeners() {
        // テキストフィールドの変更リスナー
        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setFormModified(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setFormModified(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setFormModified(true);
            }
        };

        // リスナー追加前に既存のリスナーを削除（重複防止）
        removeExistingListeners();

        // テキストフィールドにリスナー追加
        nameField.getDocument().addDocumentListener(documentListener);
        nameKanaField.getDocument().addDocumentListener(documentListener);
        careerHistoryArea.getDocument().addDocumentListener(documentListener);
        trainingHistoryArea.getDocument().addDocumentListener(documentListener);
        noteArea.getDocument().addDocumentListener(documentListener);

        // コンボボックスにリスナー追加
        ActionListener comboListener = e -> setFormModified(true);

        birthYearComboBox.addActionListener(comboListener);
        birthMonthComboBox.addActionListener(comboListener);
        birthDayComboBox.addActionListener(comboListener);
        joinYearComboBox.addActionListener(comboListener);
        joinMonthComboBox.addActionListener(comboListener);
        careerComboBox.addActionListener(comboListener);
        technicalSkillComboBox.addActionListener(comboListener);
        learningAttitudeComboBox.addActionListener(comboListener);
        communicationSkillComboBox.addActionListener(comboListener);
        leadershipComboBox.addActionListener(comboListener);

        // 言語選択コンボボックスのリスナー設定
        // 元々のAddPanelのMultiSelectComboBoxの内部リスナーと同等の機能を追加
        languageComboBox.addActionListener(e -> {
            // まず選択状態をトグル
            Object selected = languageComboBox.getSelectedItem();
            if (selected instanceof AddPanel.CheckableItem item) {
                item.setSelected(!item.isSelected());
                languageComboBox.repaint(); // 表示更新
            }

            // そして変更フラグを設定
            setFormModified(true);
        });

        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                "フォーム変更リスナーを設定しました。保存ボタン状態: " + (updateButton.isEnabled() ? "有効" : "無効"));
    }

    /**
     * 既存のイベントリスナーを削除するヘルパーメソッド
     * フォームコンポーネントに登録されているリスナーを削除して重複登録を防止
     */
    private void removeExistingListeners() {
        // テキストフィールド/エリアからDocumentListenerを削除
        // (Documentから直接リスナーを取得する方法がないため、いったん新しいDocumentに置き換える)

        // nameFieldのリスナー削除
        if (nameField != null) {
            Document doc = nameField.getDocument();
            Document newDoc = new PlainDocument();
            try {
                newDoc.insertString(0, doc.getText(0, doc.getLength()), null);
                nameField.setDocument(newDoc);
            } catch (BadLocationException e) {
                LogHandler.getInstance().logError(LogType.UI, "リスナー削除中にエラーが発生しました", e);
            }
        }

        // nameKanaFieldのリスナー削除
        if (nameKanaField != null) {
            Document doc = nameKanaField.getDocument();
            Document newDoc = new PlainDocument();
            try {
                newDoc.insertString(0, doc.getText(0, doc.getLength()), null);
                nameKanaField.setDocument(newDoc);
            } catch (BadLocationException e) {
                LogHandler.getInstance().logError(LogType.UI, "リスナー削除中にエラーが発生しました", e);
            }
        }

        // careerHistoryAreaのリスナー削除
        if (careerHistoryArea != null) {
            Document doc = careerHistoryArea.getDocument();
            Document newDoc = new PlainDocument();
            try {
                newDoc.insertString(0, doc.getText(0, doc.getLength()), null);
                careerHistoryArea.setDocument(newDoc);
            } catch (BadLocationException e) {
                LogHandler.getInstance().logError(LogType.UI, "リスナー削除中にエラーが発生しました", e);
            }
        }

        // trainingHistoryAreaのリスナー削除
        if (trainingHistoryArea != null) {
            Document doc = trainingHistoryArea.getDocument();
            Document newDoc = new PlainDocument();
            try {
                newDoc.insertString(0, doc.getText(0, doc.getLength()), null);
                trainingHistoryArea.setDocument(newDoc);
            } catch (BadLocationException e) {
                LogHandler.getInstance().logError(LogType.UI, "リスナー削除中にエラーが発生しました", e);
            }
        }

        // noteAreaのリスナー削除
        if (noteArea != null) {
            Document doc = noteArea.getDocument();
            Document newDoc = new PlainDocument();
            try {
                newDoc.insertString(0, doc.getText(0, doc.getLength()), null);
                noteArea.setDocument(newDoc);
            } catch (BadLocationException e) {
                LogHandler.getInstance().logError(LogType.UI, "リスナー削除中にエラーが発生しました", e);
            }
        }

        // コンボボックスからActionListenerを削除
        // 生年月日関連コンボボックス
        if (birthYearComboBox != null) {
            for (ActionListener listener : birthYearComboBox.getActionListeners()) {
                birthYearComboBox.removeActionListener(listener);
            }
        }

        if (birthMonthComboBox != null) {
            for (ActionListener listener : birthMonthComboBox.getActionListeners()) {
                birthMonthComboBox.removeActionListener(listener);
            }
        }

        if (birthDayComboBox != null) {
            for (ActionListener listener : birthDayComboBox.getActionListeners()) {
                birthDayComboBox.removeActionListener(listener);
            }
        }

        // 入社年月関連コンボボックス
        if (joinYearComboBox != null) {
            for (ActionListener listener : joinYearComboBox.getActionListeners()) {
                joinYearComboBox.removeActionListener(listener);
            }
        }

        if (joinMonthComboBox != null) {
            for (ActionListener listener : joinMonthComboBox.getActionListeners()) {
                joinMonthComboBox.removeActionListener(listener);
            }
        }

        // エンジニア歴コンボボックス
        if (careerComboBox != null) {
            for (ActionListener listener : careerComboBox.getActionListeners()) {
                careerComboBox.removeActionListener(listener);
            }
        }

        // スキル評価関連コンボボックス
        if (technicalSkillComboBox != null) {
            for (ActionListener listener : technicalSkillComboBox.getActionListeners()) {
                technicalSkillComboBox.removeActionListener(listener);
            }
        }

        if (learningAttitudeComboBox != null) {
            for (ActionListener listener : learningAttitudeComboBox.getActionListeners()) {
                learningAttitudeComboBox.removeActionListener(listener);
            }
        }

        if (communicationSkillComboBox != null) {
            for (ActionListener listener : communicationSkillComboBox.getActionListeners()) {
                communicationSkillComboBox.removeActionListener(listener);
            }
        }

        if (leadershipComboBox != null) {
            for (ActionListener listener : leadershipComboBox.getActionListeners()) {
                leadershipComboBox.removeActionListener(listener);
            }
        }

        // 言語選択コンボボックス（特殊コンポーネント）のリスナーは
        // 内部機能を維持するために削除しない
        // MultiSelectComboBoxの選択トグル機能を保持するため、リスナー削除のコードを削除

        LogHandler.getInstance().log(Level.INFO, LogType.UI, "すべてのフォームコンポーネントから既存のリスナーを削除しました");
    }

    // フォーム変更状態とボタン状態を連動させるメソッド
    private void setFormModified(boolean modified) {
        this.formModified = modified;

        // 明示的にSwingのEDTで実行して確実に反映させる
        SwingUtilities.invokeLater(() -> {
            updateButton.setEnabled(modified);
            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    "フォーム変更状態を変更: " + modified + ", 保存ボタン状態: " + (updateButton.isEnabled() ? "有効" : "無効"));
        });
    }

    /**
     * エンジニア情報を設定
     * 選択されたエンジニア情報を画面に表示します
     *
     * @param engineer 表示するエンジニア情報
     */
    public void setEngineerData(EngineerDTO engineer) {
        if (engineer == null) {
            return;
        }

        this.currentEngineer = engineer;
        updateFieldsWithEngineerData();

        // リスナーを設定する前に明示的にボタンを無効化
        formModified = false;
        updateButton.setEnabled(false);

        // データロード後にリスナーを設定
        setupChangeListeners();

        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                String.format("エンジニア情報を表示: ID=%s, 名前=%s", engineer.getId(), engineer.getName()));
    }

    /**
     * エンジニア情報をフィールドに反映
     * DTOからUIの各フィールドに値を設定します
     */
    private void updateFieldsWithEngineerData() {
        if (currentEngineer == null) {
            return;
        }

        try {
            // 基本情報の設定
            nameField.setText(currentEngineer.getName());
            idField.setText(currentEngineer.getId());
            nameKanaField.setText(currentEngineer.getNameKana());

            // 生年月日の設定
            if (currentEngineer.getBirthDate() != null) {
                setDateToComboBoxes(
                        currentEngineer.getBirthDate(),
                        birthYearComboBox,
                        birthMonthComboBox,
                        birthDayComboBox);
            }

            // 入社年月の設定
            if (currentEngineer.getJoinDate() != null) {
                setDateToComboBoxes(
                        currentEngineer.getJoinDate(),
                        joinYearComboBox,
                        joinMonthComboBox,
                        null);
            }

            // エンジニア歴の設定
            setComboBoxValue(careerComboBox, String.valueOf(currentEngineer.getCareer()));

            // 扱える言語の設定
            setProgrammingLanguages(currentEngineer.getProgrammingLanguages());

            // 経歴の設定
            careerHistoryArea.setText(currentEngineer.getCareerHistory());

            // 研修の受講歴の設定
            trainingHistoryArea.setText(currentEngineer.getTrainingHistory());

            // スキル評価の設定
            if (currentEngineer.getTechnicalSkill() != null) {
                setComboBoxValue(technicalSkillComboBox, String.valueOf(currentEngineer.getTechnicalSkill()));
            }
            if (currentEngineer.getLearningAttitude() != null) {
                setComboBoxValue(learningAttitudeComboBox, String.valueOf(currentEngineer.getLearningAttitude()));
            }
            if (currentEngineer.getCommunicationSkill() != null) {
                setComboBoxValue(communicationSkillComboBox, String.valueOf(currentEngineer.getCommunicationSkill()));
            }
            if (currentEngineer.getLeadership() != null) {
                setComboBoxValue(leadershipComboBox, String.valueOf(currentEngineer.getLeadership()));
            }

            // 備考の設定
            noteArea.setText(currentEngineer.getNote());

            // 最新登録日の設定
            if (currentEngineer.getRegisteredDate() != null) {
                String formattedDate = currentEngineer.getRegisteredDate().format(
                        DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
                registeredDateLabel.setText("最新登録日: " + formattedDate);
            } else {
                registeredDateLabel.setText("最新登録日: 不明");
            }

            // エラーメッセージをクリア
            clearAllComponentErrors();

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.UI, "エンジニア情報の表示中にエラーが発生しました", e);
            showErrorMessage("エンジニア情報の表示中にエラーが発生しました: " + e.getMessage());
        }
        try {
            // 変更状態をリセット
            setFormModified(false);

            // エラーメッセージをクリア
            clearAllComponentErrors();
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.UI, "エンジニア情報の表示中にエラーが発生しました", e);
            showErrorMessage("エンジニア情報の表示中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * コンボボックスに値を設定
     * 指定された値を持つアイテムを選択します
     *
     * @param comboBox コンボボックス
     * @param value    設定する値
     */
    private void setComboBoxValue(JComboBox<String> comboBox, String value) {
        if (comboBox == null || value == null) {
            return;
        }

        for (int i = 0; i < comboBox.getItemCount(); i++) {
            String item = comboBox.getItemAt(i);
            if (value.equals(item)) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }

        // 一致するアイテムがない場合は先頭（空白）を選択
        if (comboBox.getItemCount() > 0) {
            comboBox.setSelectedIndex(0);
        }
    }

    /**
     * 日付をコンボボックスに設定
     * LocalDateオブジェクトから年・月・日をコンボボックスに設定します
     *
     * @param date       日付オブジェクト
     * @param yearCombo  年コンボボックス
     * @param monthCombo 月コンボボックス
     * @param dayCombo   日コンボボックス（nullの場合は不使用）
     */
    private void setDateToComboBoxes(LocalDate date, JComboBox<String> yearCombo,
            JComboBox<String> monthCombo, JComboBox<String> dayCombo) {
        if (date == null) {
            return;
        }

        // 年の設定
        setComboBoxValue(yearCombo, String.valueOf(date.getYear()));

        // 月の設定
        setComboBoxValue(monthCombo, String.valueOf(date.getMonthValue()));

        // 日の設定（dayComboがnullでない場合のみ）
        if (dayCombo != null) {
            setComboBoxValue(dayCombo, String.valueOf(date.getDayOfMonth()));
        }
    }

    /**
     * プログラミング言語の設定
     * 言語リストからMultiSelectComboBoxの選択状態を設定します
     *
     * @param languages 言語リスト
     */
    private void setProgrammingLanguages(List<String> languages) {
        if (languages == null || languages.isEmpty() || languageComboBox == null) {
            return;
        }

        // すべての項目の選択状態をリセット
        for (int i = 0; i < languageComboBox.getModel().getSize(); i++) {
            AddPanel.CheckableItem item = languageComboBox.getModel().getElementAt(i);
            item.setSelected(false);
        }

        // 指定された言語を選択状態に設定
        for (int i = 0; i < languageComboBox.getModel().getSize(); i++) {
            AddPanel.CheckableItem item = languageComboBox.getModel().getElementAt(i);
            if (languages.contains(item.getLabel())) {
                item.setSelected(true);
            }
        }

        // コンボボックスの表示を更新
        languageComboBox.repaint();
    }

    /**
     * 更新ボタンのクリックイベント処理
     * データの検証、エンジニア情報の構築、更新処理を実行
     */
    private void updateEngineer() {
        try {
            // エラーメッセージのクリア
            clearAllComponentErrors();

            // 入力検証
            if (!validateInput()) {
                // エラーメッセージは validateInput() 内で表示されます
                return;
            }

            // 処理中状態に設定
            setProcessing(true);

            // エンジニア情報の構築
            EngineerDTO updatedEngineer = buildEngineerDTO();

            // 保存処理の実行（非同期）
            if (mainController != null) {
                // 更新イベント発行
                mainController.handleEvent("SAVE_DATA", updatedEngineer);
                LogHandler.getInstance().log(Level.INFO, LogType.UI,
                        "エンジニア更新処理を開始: ID=" + updatedEngineer.getId());
            } else {
                // コントローラー未設定エラー
                showErrorMessage("システムエラー: コントローラーが設定されていません");
                // 処理中状態の解除
                setProcessing(false);
            }

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.UI, "エンジニア更新処理中にエラーが発生しました", e);
            showErrorMessage("エンジニア情報の更新中にエラーが発生しました: " + e.getMessage());
            // 処理中状態の解除
            setProcessing(false);
        }
    }

    /**
     * 入力検証を実行
     * 各入力フィールドの値を検証し、エラーがあればフィールドごとにエラーメッセージを表示
     *
     * @return 検証成功の場合true、失敗の場合false
     */
    @Override
    protected boolean validateInput() {
        // 検証結果フラグ
        boolean isValid = true;

        // コンポーネントエラーをクリア
        clearAllComponentErrors();

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

        // 扱える言語の検証（1つ以上選択）
        if (languageComboBox.getSelectedItems().isEmpty()) {
            showFieldError("languages", MessageEnum.VALIDATION_ERROR_PROGRAMMING_LANGUAGES.getMessage());
            markComponentError("languageComboBox", null);
            isValid = false;
        }

        // 経歴の文字数検証（200文字以内）
        if (careerHistoryArea.getText().length() > 200) {
            showFieldError("careerHistoryArea", MessageEnum.VALIDATION_ERROR_CAREER_HISTORY.getMessage());
            isValid = false;
        }

        // 研修の受講歴の文字数検証（200文字以内）
        if (trainingHistoryArea.getText().length() > 200) {
            showFieldError("trainingHistoryArea", MessageEnum.VALIDATION_ERROR_TRAINING_HISTORY.getMessage());
            isValid = false;
        }

        // 備考の文字数検証（500文字以内）
        if (noteArea.getText().length() > 500) {
            showFieldError("noteArea", MessageEnum.VALIDATION_ERROR_NOTE.getMessage());
            isValid = false;
        }

        // 検証に失敗した場合、最初のエラーフィールドにフォーカスを設定
        if (!isValid && !errorComponents.isEmpty()) {
            // エラーコンポーネントの最初の要素を取得
            Component firstErrorComponent = errorComponents.values().iterator().next();

            // フォーカス可能なコンポーネントかどうか確認
            if (firstErrorComponent instanceof JComponent) {
                JComponent jComponent = (JComponent) firstErrorComponent;

                // フォーカスを設定
                jComponent.requestFocusInWindow();
            }
        }

        return isValid;
    }

    /**
     * エンジニア情報DTOを構築
     * 入力フィールドの値を取得してEngineerDTOオブジェクトを構築
     *
     * @return 構築したEngineerDTOオブジェクト
     */
    private EngineerDTO buildEngineerDTO() {
        // EngineerBuilderを使用してDTOを構築
        EngineerBuilder builder = new EngineerBuilder();

        // 基本情報の設定
        // 社員IDの設定（変更不可）
        String idValue = currentEngineer.getId();
        builder.setId(idValue);

        builder.setName(nameField.getText().trim());
        builder.setNameKana(nameKanaField.getText().trim());

        // 生年月日の設定
        LocalDate birthDate = getDateFromComponents(
                birthYearComboBox, birthMonthComboBox, birthDayComboBox);
        if (birthDate != null) {
            builder.setBirthDate(birthDate);
        }

        // 入社年月の設定
        LocalDate joinDate = getDateFromComponents(
                joinYearComboBox, joinMonthComboBox, null);
        if (joinDate != null) {
            builder.setJoinDate(joinDate);
        }

        // エンジニア歴の設定
        String careerText = (String) careerComboBox.getSelectedItem();
        if (careerText != null && !careerText.isEmpty()) {
            try {
                int career = Integer.parseInt(careerText);
                builder.setCareer(career);
            } catch (NumberFormatException e) {
                builder.setCareer(0);
            }
        } else {
            builder.setCareer(0);
        }

        // 扱える言語の設定
        List<String> languages = getSelectedLanguages();
        builder.setProgrammingLanguages(languages);

        // 経歴の設定
        String careerHistory = careerHistoryArea.getText().trim();
        if (!careerHistory.isEmpty()) {
            builder.setCareerHistory(careerHistory);
        }

        // 研修の受講歴の設定
        String trainingHistory = trainingHistoryArea.getText().trim();
        if (!trainingHistory.isEmpty()) {
            builder.setTrainingHistory(trainingHistory);
        }

        // スキル評価の設定
        setSkillRating(builder);

        // 備考の設定
        String note = noteArea.getText().trim();
        if (!note.isEmpty()) {
            builder.setNote(note);
        }

        // 最新登録日を現在の日時に更新
        builder.setRegisteredDate(LocalDate.now());

        // DTOの構築と返却
        return builder.build();
    }

    /**
     * コンポーネントにエラー表示を設定
     * エラーが発生したコンポーネントに赤枠を表示し、エラーコンポーネントとして管理
     * MultiSelectComboBoxのサポートを追加
     *
     * @param componentName エラーが発生したコンポーネント名
     * @param errorMessage  エラーメッセージ（nullの場合はエラーメッセージを更新しない）
     */
    @Override
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

        // 特別な処理: languageComboBoxがコンポーネント名の場合、そのエラー表示も処理
        if ("languages".equals(componentName) && languageComboBox != null) {
            // MultiSelectComboBoxに赤枠を設定
            if (!originalBorders.containsKey(languageComboBox)) {
                originalBorders.put(languageComboBox, languageComboBox.getBorder());
            }
            languageComboBox.setBorder(ERROR_BORDER);
            errorComponents.put("languageComboBox", languageComboBox);
        }
    }

    /**
     * コンポーネントのエラー表示をクリア
     * 特定のコンポーネントのエラー表示を解除し、関連するエラーメッセージも非表示にします
     * MultiSelectComboBoxのサポートを追加
     *
     * @param componentName エラー表示を解除するコンポーネント名
     */
    @Override
    protected void clearComponentError(String componentName) {
        super.clearComponentError(componentName);

        // 特別な処理: languagesのエラーがクリアされた場合、languageComboBoxも処理
        if ("languages".equals(componentName) && languageComboBox != null) {
            // 元のボーダーに戻す
            Border originalBorder = originalBorders.remove(languageComboBox);
            if (originalBorder != null) {
                languageComboBox.setBorder(originalBorder);
            } else {
                languageComboBox.setBorder(null);
            }

            // エラーコンポーネントから削除
            errorComponents.remove("languageComboBox");
        }
    }

    /**
     * スキル評価をビルダーに設定
     * コンボボックスから選択された評価値をビルダーに設定
     *
     * @param builder エンジニアビルダー
     */
    private void setSkillRating(EngineerBuilder builder) {
        // 技術力
        String technicalSkill = (String) technicalSkillComboBox.getSelectedItem();
        if (technicalSkill != null && !technicalSkill.isEmpty()) {
            try {
                double skill = Double.parseDouble(technicalSkill);
                builder.setTechnicalSkill(skill);
            } catch (NumberFormatException e) {
                // nullを設定（未評価）
                builder.setTechnicalSkill(null);
            }
        } else {
            // 選択なしの場合もnullを設定
            builder.setTechnicalSkill(null);
        }

        // 受講態度
        String learningAttitude = (String) learningAttitudeComboBox.getSelectedItem();
        if (learningAttitude != null && !learningAttitude.isEmpty()) {
            try {
                double attitude = Double.parseDouble(learningAttitude);
                builder.setLearningAttitude(attitude);
            } catch (NumberFormatException e) {
                // nullを設定（未評価）
                builder.setLearningAttitude(null);
            }
        } else {
            // 選択なしの場合もnullを設定
            builder.setLearningAttitude(null);
        }

        // コミュニケーション能力
        String communicationSkill = (String) communicationSkillComboBox.getSelectedItem();
        if (communicationSkill != null && !communicationSkill.isEmpty()) {
            try {
                double skill = Double.parseDouble(communicationSkill);
                builder.setCommunicationSkill(skill);
            } catch (NumberFormatException e) {
                // nullを設定（未評価）
                builder.setCommunicationSkill(null);
            }
        } else {
            // 選択なしの場合もnullを設定
            builder.setCommunicationSkill(null);
        }

        // リーダーシップ
        String leadership = (String) leadershipComboBox.getSelectedItem();
        if (leadership != null && !leadership.isEmpty()) {
            try {
                double lead = Double.parseDouble(leadership);
                builder.setLeadership(lead);
            } catch (NumberFormatException e) {
                // nullを設定（未評価）
                builder.setLeadership(null);
            }
        } else {
            // 選択なしの場合もnullを設定
            builder.setLeadership(null);
        }
    }

    /**
     * 選択された言語のリストを取得
     * MultiSelectComboBoxから選択された言語の一覧を取得
     *
     * @return 選択された言語のリスト
     */
    private List<String> getSelectedLanguages() {
        if (languageComboBox == null) {
            return new ArrayList<>();
        }

        return languageComboBox.getSelectedItems();
    }

    /**
     * コンボボックスから日付オブジェクトを取得
     * 年・月・日のコンボボックスから日付を構築
     *
     * @param yearComboBox  年コンボボックス
     * @param monthComboBox 月コンボボックス
     * @param dayComboBox   日コンボボックス（null可）
     * @return 構築した日付、無効な場合はnull
     */
    private LocalDate getDateFromComponents(JComboBox<String> yearComboBox,
            JComboBox<String> monthComboBox,
            JComboBox<String> dayComboBox) {
        try {
            String yearStr = (String) yearComboBox.getSelectedItem();
            String monthStr = (String) monthComboBox.getSelectedItem();

            if (yearStr == null || yearStr.isEmpty() || monthStr == null || monthStr.isEmpty()) {
                return null;
            }

            int year = Integer.parseInt(yearStr);
            int month = Integer.parseInt(monthStr);

            // 日コンボボックスがある場合（生年月日）
            if (dayComboBox != null) {
                String dayStr = (String) dayComboBox.getSelectedItem();
                if (dayStr == null || dayStr.isEmpty()) {
                    return null;
                }
                int day = Integer.parseInt(dayStr);
                return LocalDate.of(year, month, day);
            }

            // 日コンボボックスがない場合（入社年月）
            return LocalDate.of(year, month, 1);

        } catch (NumberFormatException | java.time.DateTimeException e) {
            // 無効な日付の場合
            return null;
        }
    }

    /**
     * 更新完了処理を実行します
     * <p>
     * このメソッドは、エンジニア情報の更新処理が正常に完了した後に呼び出されます。
     * 主な役割は以下の通りです：
     * </p>
     * 
     * <ol>
     * <li>処理中状態の解除（プログレスインジケーターの非表示化）</li>
     * <li>更新完了ダイアログの表示</li>
     * <li>次のアクションに基づく画面遷移またはフォームクリア</li>
     * </ol>
     * 
     * @param engineer 更新されたエンジニア情報（{@link EngineerDTO}オブジェクト）
     */
    public void handleUpdateComplete(EngineerDTO engineer) {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "DetailPanel.handleUpdateComplete開始: ID=" + engineer.getId());

        try {
            // 処理中状態を解除（プログレスインジケーターの非表示化とUI操作の有効化）
            setProcessing(false);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "処理中状態を解除しました");

            // 更新成功後のダイアログ表示と後続処理
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "更新完了ダイアログを表示します: ID=" + engineer.getId());

            // 更新完了ダイアログを表示（コールバック付き）
            DialogManager.getInstance().showCompletionDialog(
                    "エンジニア情報の更新が完了しました: ID=" + engineer.getId() + ", 名前=" + engineer.getName(),
                    () -> {
                        // ダイアログが閉じられた後の処理
                        if (mainController != null) {
                            // データ再読込を実行してから画面遷移
                            mainController.handleEvent("LOAD_DATA", null);
                            // 画面遷移
                            mainController.handleEvent("CHANGE_PANEL", "LIST");
                            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                    "一覧画面に遷移します");
                        } else {
                            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                    "MainControllerが設定されていないため画面遷移できません");
                        }
                    });

            // 処理成功フラグを設定
            handleUpdateCompleteSuccess = true;
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "DetailPanel.handleUpdateComplete完了: ID=" + engineer.getId());

        } catch (Exception e) {
            // エラー処理は変更なし
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "handleUpdateComplete処理中にエラーが発生しました: " + engineer.getId(), e);

            // UIスレッドでエラーダイアログを表示
            try {
                DialogManager.getInstance().showErrorDialog(
                        "エラー",
                        "更新完了処理中にエラーが発生しました: " + e.getMessage());
            } catch (Exception dialogError) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "エラーダイアログの表示にも失敗しました", dialogError);
            }

            setProcessing(false);
            handleUpdateCompleteSuccess = false;
        } finally {
            if (processing) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "処理中状態が解除されていません - 強制的に解除します");
                setProcessing(false);
            }
        }
    }

    /**
     * 一覧画面に戻る
     * コントローラーを通じて画面遷移を行う
     */
    private void goBack() {
        if (mainController != null) {
            mainController.handleEvent("CHANGE_PANEL", "LIST");
            LogHandler.getInstance().log(Level.INFO, LogType.UI, "一覧画面に戻ります");
        } else {
            LogHandler.getInstance().log(Level.WARNING, LogType.UI,
                    "MainControllerが設定されていないため画面遷移できません");
        }
    }

    /**
     * 処理中状態の設定
     * 処理中はUIコンポーネントを無効化し、プログレスインジケーターを表示します
     *
     * @param processing 処理中の場合true
     */
    public void setProcessing(boolean processing) {
        this.processing = processing;

        // UIコンポーネントの有効/無効を切り替え
        setAllComponentsEnabled(!processing);

        // ボタンの有効/無効を切り替え
        updateButton.setEnabled(!processing);
        backButton.setEnabled(!processing);

        // プログレスインジケーターの表示/非表示を切り替え
        progressLabel.setVisible(processing);
    }

    /**
     * 年の選択肢を取得
     *
     * @param startYear 開始年
     * @param endYear   終了年
     * @return 年の選択肢配列
     */
    private String[] getYearOptions(int startYear, int endYear) {
        List<String> years = new ArrayList<>();
        years.add(""); // 空の選択肢

        for (int year = startYear; year <= endYear; year++) {
            years.add(String.valueOf(year));
        }

        return years.toArray(new String[0]);
    }

    /**
     * 月の選択肢を取得
     *
     * @return 月の選択肢配列
     */
    private String[] getMonthOptions() {
        String[] months = new String[13]; // 空 + 1-12月
        months[0] = "";

        for (int i = 1; i <= 12; i++) {
            months[i] = String.valueOf(i);
        }

        return months;
    }

    /**
     * 日の選択肢を取得
     *
     * @return 日の選択肢配列
     */
    private String[] getDayOptions() {
        String[] days = new String[32]; // 空 + 1-31日
        days[0] = "";

        for (int i = 1; i <= 31; i++) {
            days[i] = String.valueOf(i);
        }

        return days;
    }

    /**
     * エンジニア歴の選択肢を取得
     *
     * @return エンジニア歴の選択肢配列
     */
    private String[] getCareerOptions() {
        String[] careers = new String[51]; // 空 + 0-19年
        careers[0] = "";

        for (int i = 1; i <= 50; i++) {
            careers[i] = String.valueOf(i);
        }

        return careers;
    }

    /**
     * スキル評価の選択肢を取得（1.0-5.0、0.5刻み）
     *
     * @return スキル評価の選択肢配列
     */
    private String[] getSkillRatingOptions() {
        String[] ratings = new String[10]; // 空 + 1.0-5.0（0.5刻み）
        ratings[0] = "";

        double rating = 1.0;
        for (int i = 1; i < ratings.length; i++) {
            ratings[i] = String.valueOf(rating);
            rating += 0.5;
        }

        return ratings;
    }

    public void setUpdateButtonEnabled(boolean enabled) {
        // 外部から呼ばれた場合は、変更状態と一致させる
        if (enabled != formModified) {
            formModified = enabled;
        }
        updateButton.setEnabled(enabled);
        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                "外部から保存ボタン状態を更新: " + enabled);
    }

}