package view;

import model.EngineerDTO;
import model.EngineerBuilder;
import controller.MainController;
import util.LogHandler;
import util.LogHandler.LogType;
import util.validator.IDValidator;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;

/**
 * エンジニア情報の新規登録画面を提供するパネルクラス
 * AbstractEngineerPanelの共通機能を活用し、新規登録に特化した機能を実装
 *
 * <p>
 * このクラスは、エンジニア人材管理システムにおいて新規エンジニア情報を
 * 登録するためのユーザーインターフェースを提供します。AbstractEngineerPanelを
 * 継承し、共通のフォーム作成機能とバリデーション機能を活用することで、
 * コードの重複を大幅に削減しています。
 * </p>
 *
 * <p>
 * バージョン4.11.7での主な改善点：
 * <ul>
 * <li>共通機能の親クラス移動による重複削減</li>
 * <li>新規登録に特化した機能への集約</li>
 * <li>保守性の向上</li>
 * </ul>
 * </p>
 *
 * @author Nakano
 */
public class AddPanel extends AbstractEngineerPanel {

    /** メインコントローラー参照 */
    private MainController mainController;

    /** 登録ボタン */
    private JButton addButton;

    /** 戻るボタン */
    private JButton backButton;

    /** プログレスインジケーター */
    private JLabel progressLabel;

    /** 処理中フラグ */
    private boolean processing;

    /** 完了処理の成功フラグ */
    private boolean handleSaveCompleteSuccess = false;

    /**
     * コンストラクタ
     * パネルの初期設定とコンポーネントの初期化
     */
    public AddPanel() {
        super();
        this.processing = false;
        LogHandler.getInstance().log(Level.INFO, LogType.UI, "AddPanelを作成しました");
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

            LogHandler.getInstance().log(Level.INFO, LogType.UI, "AddPanelの初期化が完了しました");
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.UI, "AddPanelの初期化中にエラーが発生しました", e);
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

        // 1. 基本情報セクション（左側）- 新規登録モード
        createBasicInfoSection(leftFormPanel, false);

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
     * 「登録」ボタンと「戻る」ボタンを配置
     */
    private void createButtonArea() {
        // 処理中表示ラベル
        progressLabel = new JLabel("登録中...");
        progressLabel.setVisible(false);
        addButtonPanelComponent(progressLabel);

        // 戻るボタン
        backButton = new JButton("戻る");
        backButton.addActionListener(e -> {
            if (!processing) {
                goBack();
            }
        });
        addButton(backButton);

        // 登録ボタン
        addButton = new JButton("登録");
        addButton.addActionListener(e -> {
            if (!processing) {
                addEngineer();
            }
        });
        addButton(addButton);
    }

    /**
     * 登録ボタンのクリックイベント処理
     * データの検証、エンジニア情報の構築、保存処理を実行
     */
    private void addEngineer() {
        try {
            // エラーメッセージのクリア
            clearAllComponentErrors();

            // 入力検証
            if (!validateInput()) {
                return;
            }

            // 処理中状態に設定
            setProcessing(true);

            // エンジニア情報の構築
            EngineerDTO engineer = buildEngineerDTO();

            // 保存処理の実行（非同期）
            if (mainController != null) {
                // 保存イベント発行
                mainController.handleEvent("SAVE_DATA", engineer);
            } else {
                // コントローラー未設定エラー
                showErrorMessage("システムエラー: コントローラーが設定されていません");
                setProcessing(false);
            }

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.UI, "エンジニア追加処理中にエラーが発生しました", e);
            showErrorMessage("エンジニア情報の登録中にエラーが発生しました: " + e.getMessage());
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
        // エラーメッセージのクリア
        clearAllComponentErrors();

        // 親クラスの共通検証を実行
        boolean isValid = validateCommonInput();

        // 検証に失敗した場合、最初のエラーフィールドにフォーカスを設定
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
        String idValue = IDValidator.convertFullWidthToHalfWidth(idField.getText().trim());
        String normalizedId = IDValidator.standardizeId(idValue);
        builder.setId(normalizedId);
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
            } catch (NumberFormatException e) {
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

        return builder.build();
    }

    /**
     * スキル評価をビルダーに設定
     * コンボボックスから選択された評価値をビルダーに設定
     *
     * @param builder エンジニアビルダー
     */
    private void setSkillRating(EngineerBuilder builder) {
        // 技術力
        setSkillRatingField(technicalSkillComboBox, builder::setTechnicalSkill);

        // 受講態度
        setSkillRatingField(learningAttitudeComboBox, builder::setLearningAttitude);

        // コミュニケーション能力
        setSkillRatingField(communicationSkillComboBox, builder::setCommunicationSkill);

        // リーダーシップ
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
            } catch (NumberFormatException e) {
                setter.accept(null);
            }
        } else {
            setter.accept(null);
        }
    }

    /**
     * 保存完了処理を実行します
     * エンジニア情報の保存処理が正常に完了した後に呼び出されます
     *
     * @param engineer 保存されたエンジニア情報
     */
    public void handleSaveComplete(EngineerDTO engineer) {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "AddPanel.handleSaveComplete開始: ID=" + engineer.getId());

        try {
            // 処理中状態を解除
            setProcessing(false);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "処理中状態を解除しました");

            // 登録完了ダイアログを表示し、次のアクションを取得
            String action = dialogManager.showRegisterCompletionDialog(engineer);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "選択されたアクション: " + action);

            // 選択されたアクションに応じた処理
            switch (action) {
                case "CONTINUE":
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "「続けて登録」が選択されました - フォームをクリアします");
                    clearFields();
                    break;

                case "LIST":
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "「一覧に戻る」が選択されました - 一覧画面に遷移します");
                    if (mainController != null) {
                        mainController.handleEvent("LOAD_DATA", null);
                        mainController.handleEvent("CHANGE_PANEL", "LIST");
                    } else {
                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                "MainControllerが設定されていないため画面遷移できません");
                        clearFields();
                    }
                    break;

                case "DETAIL":
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "「詳細を表示」が選択されました - 詳細画面に遷移します: ID=" + engineer.getId());
                    if (mainController != null) {
                        mainController.handleEvent("VIEW_DETAIL", engineer.getId());
                    } else {
                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                "MainControllerが設定されていないため画面遷移できません");
                        clearFields();
                    }
                    break;

                default:
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "未知のアクションが選択されました: " + action + " - デフォルト処理を実行します");
                    clearFields();
                    break;
            }

            // 処理成功フラグを設定
            handleSaveCompleteSuccess = true;
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "AddPanel.handleSaveComplete完了: ID=" + engineer.getId());

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "handleSaveComplete処理中にエラーが発生しました: " + engineer.getId(), e);

            try {
                DialogManager.getInstance().showErrorDialog("エラー",
                        "登録完了処理中にエラーが発生しました: " + e.getMessage());
            } catch (Exception dialogError) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "エラーダイアログの表示にも失敗しました", dialogError);
            }

            setProcessing(false);
            handleSaveCompleteSuccess = false;
        } finally {
            if (processing) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "処理中状態が解除されていません - 強制的に解除します");
                setProcessing(false);
            }
        }
    }

    /**
     * 入力フィールドをクリア
     * すべての入力フィールドを初期状態にリセットし、エラー表示もクリアします
     */
    private void clearFields() {
        // テキストフィールドのクリア
        nameField.setText("");
        idField.setText("");
        nameKanaField.setText("");

        // テキストエリアのクリア
        careerHistoryArea.setText("");
        trainingHistoryArea.setText("");
        noteArea.setText("");

        // コンボボックスのリセット
        birthYearComboBox.setSelectedIndex(0);
        birthMonthComboBox.setSelectedIndex(0);
        birthDayComboBox.setSelectedIndex(0);
        joinYearComboBox.setSelectedIndex(0);
        joinMonthComboBox.setSelectedIndex(0);
        careerComboBox.setSelectedIndex(0);
        technicalSkillComboBox.setSelectedIndex(0);
        learningAttitudeComboBox.setSelectedIndex(0);
        communicationSkillComboBox.setSelectedIndex(0);
        leadershipComboBox.setSelectedIndex(0);

        // MultiSelectComboBoxのクリア
        if (languageComboBox != null) {
            languageComboBox.clearSelection();
        }

        // フォーカスを名前フィールドに設定
        nameField.requestFocus();

        // エラーメッセージのクリア
        clearAllComponentErrors();
    }

    /**
     * 一覧画面に戻る
     * コントローラーを通じて画面遷移を行う
     */
    private void goBack() {
        // 入力フィールドをクリア
        clearFields();

        if (mainController != null) {
            if (ListPanel.isRefreshNeeded()) {
                ListPanel.setNeedsRefresh(false);
                mainController.getListPanel().setEngineerData(mainController.getEngineerController().loadEngineers());
            }
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
        addButton.setEnabled(!processing);
        backButton.setEnabled(!processing);

        // プログレスインジケーターの表示/非表示を切り替え
        progressLabel.setVisible(processing);
    }

    /**
     * 完了処理の成功状態を取得します
     *
     * @return 完了処理が成功した場合true、そうでなければfalse
     */
    public boolean isHandleSaveCompleteSuccess() {
        return handleSaveCompleteSuccess;
    }

    /**
     * 現在の処理中状態を取得します
     *
     * @return 処理中の場合true、そうでなければfalse
     */
    public boolean isProcessing() {
        return this.processing;
    }

    /**
     * 登録ボタンの有効/無効制御
     */
    public void setRegisterButtonEnabled(boolean enabled) {
        if (addButton != null) {
            addButton.setEnabled(enabled);
        }
    }
}