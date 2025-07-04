package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import controller.MainController;
import model.EngineerBuilder;
import model.EngineerDTO;
import util.LogHandler;
import util.LogHandler.LogType;

/**
 * エンジニア情報の詳細表示・編集画面を提供するパネルクラス
 * AbstractEngineerPanelの共通機能を活用し、更新機能に特化した実装を提供
 * 
 * エンジニア人材管理システムにおいて既存のエンジニア情報を
 * 表示・編集するためのユーザーインターフェースを提供します。AbstractEngineerPanelを
 * 継承し、共通のフォーム作成機能とバリデーション機能を活用
 *
 * @author Nakano
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

    /** 最新登録日表示ラベル */
    private JLabel registeredDateLabel;

    /** フォーム変更フラグ */
    private boolean formModified = false;

    /**
     * コンストラクタ
     * パネルの初期設定とコンポーネントの初期化
     */
    public DetailPanel() {
        super();
        this.processing = false;
        LogHandler.getInstance().log(Level.INFO, LogType.UI, "DetailPanelを作成しました");
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

            // 変更リスナーの設定
            setupChangeListeners();

            LogHandler.getInstance().log(Level.INFO, LogType.UI, "DetailPanelの初期化完了");
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.UI, "DetailPanelの初期化中にエラーが発生", _e);
        }
    }

    /**
     * フォームコンポーネントの作成と配置
     * 親クラスの共通機能を活用してフォームを構築
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

        // 右側パネルに最新登録日パネルを追加（一番上）
        rightFormPanel.add(registeredDatePanel);
        rightFormPanel.add(createVerticalSpacer(10));

        // 1. 基本情報セクション（左側）- 詳細モード（ID編集不可）
        createBasicInfoSection(leftFormPanel, true);

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
        backButton.addActionListener(_e -> {
            if (!processing) {
                goBack();
            }
        });
        addButton(backButton);

        // 更新ボタン
        updateButton = new JButton("保存");
        updateButton.setEnabled(false); // 初期状態では無効化
        updateButton.addActionListener(_e -> {
            if (!processing) {
                updateEngineer();
            }
        });
        addButton(updateButton);
    }

    /**
     * フォーム変更リスナー設定
     * フィールドの変更を検知して更新ボタンの状態を制御
     */
    private void setupChangeListeners() {
        // テキストフィールドの変更リスナー
        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent _e) {
                setFormModified(true);
            }

            @Override
            public void removeUpdate(DocumentEvent _e) {
                setFormModified(true);
            }

            @Override
            public void changedUpdate(DocumentEvent _e) {
                setFormModified(true);
            }
        };

        // テキストフィールドにリスナー追加
        nameField.getDocument().addDocumentListener(documentListener);
        nameKanaField.getDocument().addDocumentListener(documentListener);
        careerHistoryArea.getDocument().addDocumentListener(documentListener);
        trainingHistoryArea.getDocument().addDocumentListener(documentListener);
        noteArea.getDocument().addDocumentListener(documentListener);

        // コンボボックスにリスナー追加
        ActionListener comboListener = _e -> setFormModified(true);

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
        languageComboBox.addActionListener(_e -> setFormModified(true));

        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                "フォーム変更リスナーを設定しました。保存ボタン状態: " + (updateButton.isEnabled() ? "有効" : "無効"));
    }

    /**
     * フォーム変更状態とボタン状態を連動
     */
    private void setFormModified(boolean modified) {
        this.formModified = modified;

        SwingUtilities.invokeLater(() -> {
            updateButton.setEnabled(modified);
            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    "フォーム変更状態を変更: " + modified + ", 保存ボタン状態: " + (updateButton.isEnabled() ? "有効" : "無効"));
        });
    }

    /**
     * エンジニア情報を設定
     * 選択されたエンジニア情報を画面に表示
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
     * DTOからUIの各フィールドに値を設定
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
                setDateToComboBoxes(currentEngineer.getBirthDate(), birthYearComboBox, birthMonthComboBox,
                        birthDayComboBox);
            }

            // 入社年月の設定
            if (currentEngineer.getJoinDate() != null) {
                setDateToComboBoxes(currentEngineer.getJoinDate(), joinYearComboBox, joinMonthComboBox, null);
            }

            // エンジニア歴の設定
            setComboBoxValue(careerComboBox, String.valueOf(currentEngineer.getCareer()));

            // 扱える言語の設定
            setProgrammingLanguages(currentEngineer.getProgrammingLanguages());

            // 経歴の設定（Unicode文字を確実に表示）
            String careerHistory = currentEngineer.getCareerHistory();
            if (careerHistory != null) {
                careerHistoryArea.setText(careerHistory);
                // テキストエリアの更新を確実に行う
                careerHistoryArea.setCaretPosition(0);
            }

            // 研修の受講歴の設定（Unicode文字を確実に表示）
            String trainingHistory = currentEngineer.getTrainingHistory();
            if (trainingHistory != null) {
                trainingHistoryArea.setText(trainingHistory);
                // テキストエリアの更新を確実に行う
                trainingHistoryArea.setCaretPosition(0);
            }

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

            // 備考の設定（Unicode文字を確実に表示）
            String note = currentEngineer.getNote();
            if (note != null) {
                noteArea.setText(note);
                // テキストエリアの更新を確実に行う
                noteArea.setCaretPosition(0);
            }

            // 登録日の設定
            if (currentEngineer.getRegisteredDate() != null) {
                registeredDateLabel.setText("登録日: " +
                        currentEngineer.getRegisteredDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
            }

            // UIの更新を確実に行う
            SwingUtilities.invokeLater(() -> {
                careerHistoryArea.repaint();
                trainingHistoryArea.repaint();
                noteArea.repaint();
            });

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.UI, "エンジニア情報の画面反映中にエラーが発生", e);
            showErrorMessage("エンジニア情報の表示中にエラーが発生しました");
        }
    }

    /**
     * コンボボックスに値を設定
     * 指定された値を持つアイテムを選択
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

        if (comboBox.getItemCount() > 0) {
            comboBox.setSelectedIndex(0);
        }
    }

    /**
     * 日付をコンボボックスに設定
     * LocalDateオブジェクトから年・月・日をコンボボックスに設定
     */
    private void setDateToComboBoxes(LocalDate date, JComboBox<String> yearCombo,
            JComboBox<String> monthCombo, JComboBox<String> dayCombo) {
        if (date == null) {
            return;
        }

        setComboBoxValue(yearCombo, String.valueOf(date.getYear()));
        setComboBoxValue(monthCombo, String.valueOf(date.getMonthValue()));

        if (dayCombo != null) {
            setComboBoxValue(dayCombo, String.valueOf(date.getDayOfMonth()));
        }
    }

    /**
     * プログラミング言語の設定
     * 言語リストからMultiSelectComboBoxの選択状態を設定
     */
    private void setProgrammingLanguages(List<String> languages) {
        if (languages == null || languages.isEmpty() || languageComboBox == null) {
            return;
        }

        languageComboBox.setSelectedItems(languages);
    }

    /**
     * 更新ボタンのクリックイベント処理
     * データの検証、エンジニア情報の構築、更新処理を実行
     */
    private void updateEngineer() {
        try {
            clearAllComponentErrors();

            if (!validateInput()) {
                return;
            }

            setProcessing(true);

            EngineerDTO updatedEngineer = buildEngineerDTO();

            if (mainController != null) {
                mainController.handleEvent("SAVE_DATA", updatedEngineer);
                LogHandler.getInstance().log(Level.INFO, LogType.UI,
                        "エンジニア更新処理を開始: ID=" + updatedEngineer.getId());
            } else {
                showErrorMessage("システムエラー: コントローラーが設定されていません");
                setProcessing(false);
            }

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.UI, "エンジニア更新処理中にエラーが発生", _e);
            showErrorMessage("エンジニア情報の更新中にエラーが発生: " + _e.getMessage());
            setProcessing(false);
        }
    }

    /**
     * 入力検証を実行
     * 親クラスの共通検証機能を活用し、最初のエラーフィールドにフォーカスを設定
     *
     * @return 検証成功の場合true、失敗の場合false
     */
    @Override
    protected boolean validateInput() {
        clearAllComponentErrors();

        boolean isValid = validateCommonInput();

        if (!isValid && !errorComponents.isEmpty()) {
            Component firstErrorComponent = errorComponents.values().iterator().next();
            if (firstErrorComponent instanceof JComponent) {
                JComponent jComponent = (JComponent) firstErrorComponent;
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
        EngineerBuilder builder = new EngineerBuilder();

        // 基本情報の設定
        String idValue = currentEngineer.getId();
        builder.setId(idValue);
        builder.setName(nameField.getText().trim());
        builder.setNameKana(nameKanaField.getText().trim());

        // 生年月日の設定
        LocalDate birthDate = getDateFromComponents(birthYearComboBox, birthMonthComboBox, birthDayComboBox);
        if (birthDate != null) {
            builder.setBirthDate(birthDate);
        }

        // 入社年月の設定
        LocalDate joinDate = getDateFromComponents(joinYearComboBox, joinMonthComboBox, null);
        if (joinDate != null) {
            builder.setJoinDate(joinDate);
        }

        // エンジニア歴の設定
        String careerText = (String) careerComboBox.getSelectedItem();
        if (careerText != null && !careerText.isEmpty()) {
            try {
                int career = Integer.parseInt(careerText);
                builder.setCareer(career);
            } catch (NumberFormatException _e) {
                builder.setCareer(0);
            }
        } else {
            builder.setCareer(0);
        }

        // 扱える言語の設定
        List<String> languages = languageComboBox.getSelectedItems();
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

        return builder.build();
    }

    /**
     * スキル評価をビルダーに設定
     * コンボボックスから選択された評価値をビルダーに設定
     */
    private void setSkillRating(EngineerBuilder builder) {
        setSkillRatingField(technicalSkillComboBox, builder::setTechnicalSkill);
        setSkillRatingField(learningAttitudeComboBox, builder::setLearningAttitude);
        setSkillRatingField(communicationSkillComboBox, builder::setCommunicationSkill);
        setSkillRatingField(leadershipComboBox, builder::setLeadership);
    }

    /**
     * 個別のスキル評価フィールドを設定するヘルパーメソッド
     */
    private void setSkillRatingField(JComboBox<String> comboBox, java.util.function.Consumer<Double> setter) {
        String skillText = (String) comboBox.getSelectedItem();
        if (skillText != null && !skillText.isEmpty()) {
            try {
                double skill = Double.parseDouble(skillText);
                setter.accept(skill);
            } catch (NumberFormatException _e) {
                setter.accept(null);
            }
        } else {
            setter.accept(null);
        }
    }

    /**
     * 更新完了処理を実行
     * エンジニア情報の更新処理が正常に完了した後に呼び出されます
     *
     * @param engineer 更新されたエンジニア情報
     */
    public void handleUpdateComplete(EngineerDTO engineer) {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "DetailPanel.handleUpdateComplete開始: ID=" + engineer.getId());

        try {
            setProcessing(false);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "処理中状態を解除");

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "更新完了ダイアログを表示: ID=" + engineer.getId());

            DialogManager.getInstance().showCompletionDialog(
                    "エンジニア情報の更新が完了: ID=" + engineer.getId() + ", 名前=" + engineer.getName(),
                    () -> {
                        if (mainController != null) {
                            mainController.handleEvent("LOAD_DATA", null);
                            mainController.handleEvent("CHANGE_PANEL", "LIST");
                            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "一覧画面に遷移");
                        } else {
                            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                    "メインコントローラが設定されていないため画面遷移できません");
                        }
                    });

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "DetailPanel.handleUpdateComplete完了: ID=" + engineer.getId());

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "handleUpdateComplete処理中にエラーが発生: " + engineer.getId(), _e);

            try {
                DialogManager.getInstance().showErrorDialog("エラー",
                        "更新完了処理中にエラーが発生: " + _e.getMessage());
            } catch (Exception dialogError) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "エラーダイアログの表示にも失敗", dialogError);
            }

            setProcessing(false);
        } finally {
            if (processing) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "処理中状態が解除されていません - 強制的に解除");
                setProcessing(false);
            }
        }
    }

    /**
     * 一覧画面に戻る
     * コントローラーを通じて画面遷移
     */
    private void goBack() {
        if (mainController != null) {
            mainController.handleEvent("CHANGE_PANEL", "LIST");
            LogHandler.getInstance().log(Level.INFO, LogType.UI, "一覧画面に戻ります");
        } else {
            LogHandler.getInstance().log(Level.WARNING, LogType.UI,
                    "メインコントローラが設定されていないため画面遷移できません");
        }
    }

    /**
     * 処理中状態の設定
     *
     * @param processing 処理中の場合true
     */
    public void setProcessing(boolean processing) {
        this.processing = processing;

        setAllComponentsEnabled(!processing);

        updateButton.setEnabled(!processing);
        backButton.setEnabled(!processing);

        progressLabel.setVisible(processing);
    }

    /**
     * 更新ボタンの有効/無効制御
     */
    public void setUpdateButtonEnabled(boolean enabled) {
        if (enabled != formModified) {
            formModified = enabled;
        }
        updateButton.setEnabled(enabled);
        LogHandler.getInstance().log(Level.INFO, LogType.UI, "外部から保存ボタン状態を更新: " + enabled);
    }
}