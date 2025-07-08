package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import controller.MainController;
import model.EngineerBuilder;
import model.EngineerDTO;
import util.LogHandler;
import util.LogHandler.LogType;
import util.validator.ValidationResult;

/**
 * エンジニア情報の新規登録画面を提供するパネルクラス
 * AbstractEngineerPanelの共通機能を活用し、新規登録に特化した機能を実装
 *
 * このクラスは、エンジニア人材管理システムにおいて新規エンジニア情報を
 * 登録するためのユーザーインターフェースを提供します。AbstractEngineerPanelを
 * 継承し、共通のフォーム作成機能とバリデーション機能を活用
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
        LogHandler.getInstance().log(Level.INFO, LogType.UI, "アドパネルを作成");
    }

    /**
     * メインコントローラーを設定
     * コントローラーを通じてイベント処理や画面遷移
     *
     * @param mainController メインコントローラー
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * パネルの初期化
     * UIコンポーネントの生成と配置
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

            LogHandler.getInstance().log(Level.INFO, LogType.UI, "アドパネルの初期化完了");
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.UI, "アドパネルの初期化中にエラーが発生", _e);
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
        backButton.addActionListener(_e -> {
            if (!processing) {
                goBack();
            }
        });
        addButton(backButton);

        // 登録ボタン
        addButton = new JButton("登録");
        addButton.addActionListener(_e -> {
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

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.UI, "エンジニア追加処理中にエラーが発生", _e);
            showErrorMessage("エンジニア情報の登録中にエラーが発生: " + _e.getMessage());
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
     * 既存のエンジニアIDセットを取得（重複チェック用）
     */
    @Override
    protected Set<String> getExistingEngineerIds() {
        try {
            if (mainController != null) {
                List<EngineerDTO> engineers = mainController.getEngineerController().loadEngineers();
                Set<String> ids = new HashSet<>();
                for (EngineerDTO engineer : engineers) {
                    ids.add(engineer.getId());
                }
                return ids;
            }
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "既存IDの取得中にエラーが発生", _e);
        }
        return new HashSet<>();
    }

    /**
     * エンジニア情報DTOを構築（バリデーション済みデータを使用）
     * 
     * @return 構築したEngineerDTOオブジェクト
     */
    private EngineerDTO buildEngineerDTO() {
        ValidationResult validationResult = getLastValidationResult();
        if (validationResult == null || !validationResult.isValid()) {
            throw new IllegalStateException("バリデーションが実行されていないか、失敗しています");
        }

        Map<String, String> processedValues = validationResult.getProcessedValues();
        EngineerBuilder builder = new EngineerBuilder();

        // 前処理済みの値を使用してDTOを構築
        builder.setId(processedValues.get("id"));
        builder.setName(processedValues.get("name"));
        builder.setNameKana(processedValues.get("nameKana"));

        // 日付の解析
        String birthDateStr = processedValues.get("birthDate");
        if (birthDateStr != null && !birthDateStr.isEmpty()) {
            builder.setBirthDate(LocalDate.parse(birthDateStr));
        }

        String joinDateStr = processedValues.get("joinDate");
        if (joinDateStr != null && !joinDateStr.isEmpty()) {
            builder.setJoinDate(LocalDate.parse(joinDateStr));
        }

        // エンジニア歴
        String careerStr = processedValues.get("career");
        if (careerStr != null && !careerStr.isEmpty()) {
            builder.setCareer(Integer.parseInt(careerStr));
        }

        // 扱える言語
        String languagesStr = processedValues.get("programmingLanguages");
        if (languagesStr != null && !languagesStr.isEmpty()) {
            builder.setProgrammingLanguages(Arrays.asList(languagesStr.split(";")));
        }

        // テキストフィールド
        builder.setCareerHistory(processedValues.get("careerHistory"));
        builder.setTrainingHistory(processedValues.get("trainingHistory"));
        builder.setNote(processedValues.get("note"));

        // スキル評価
        setProcessedSkillRating(builder, processedValues);

        // 新規登録時の登録日設定
        builder.setRegisteredDate(LocalDate.now());

        return builder.build();
    }

    /**
     * 前処理済みスキル評価をビルダーに設定
     */
    private void setProcessedSkillRating(EngineerBuilder builder, Map<String, String> processedValues) {
        String technicalSkill = processedValues.get("technicalSkill");
        if (technicalSkill != null && !technicalSkill.isEmpty()) {
            builder.setTechnicalSkill(Double.parseDouble(technicalSkill));
        }

        String learningAttitude = processedValues.get("learningAttitude");
        if (learningAttitude != null && !learningAttitude.isEmpty()) {
            builder.setLearningAttitude(Double.parseDouble(learningAttitude));
        }

        String communicationSkill = processedValues.get("communicationSkill");
        if (communicationSkill != null && !communicationSkill.isEmpty()) {
            builder.setCommunicationSkill(Double.parseDouble(communicationSkill));
        }

        String leadership = processedValues.get("leadership");
        if (leadership != null && !leadership.isEmpty()) {
            builder.setLeadership(Double.parseDouble(leadership));
        }
    }

    /**
     * 保存完了処理を実行
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
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "処理中状態を解除");

            // 登録完了ダイアログを表示し、次のアクションを取得
            String action = dialogManager.showRegisterCompletionDialog(engineer);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "選択されたアクション: " + action);

            // 選択されたアクションに応じた処理
            switch (action) {
                case "CONTINUE":
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "「続けて登録」が選択されました - フォームをクリア");
                    clearFields();
                    break;

                case "LIST":
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "「一覧に戻る」が選択されました - 一覧画面に遷移");
                    clearFields();
                    if (mainController != null) {
                        mainController.handleEvent("LOAD_DATA", null);
                        mainController.handleEvent("CHANGE_PANEL", "LIST");
                    } else {
                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                "メインコントローラが設定されていないため画面遷移できません");
                    }
                    break;

                case "DETAIL":
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "「詳細を表示」が選択されました - 詳細画面に遷移: ID=" + engineer.getId());
                    if (mainController != null) {
                        mainController.handleEvent("VIEW_DETAIL", engineer.getId());
                    } else {
                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                "メインコントローラが設定されていないため画面遷移できません");
                        clearFields();
                    }
                    break;

                default:
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "未知のアクションが選択されました: " + action + " - デフォルト処理を実行");
                    clearFields();
                    break;
            }

            // 処理成功フラグを設定
            handleSaveCompleteSuccess = true;
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "AddPanel.handleSaveComplete完了: ID=" + engineer.getId());

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "handleSaveComplete処理中にエラーが発生: " + engineer.getId(), _e);

            try {
                DialogManager.getInstance().showErrorDialog("エラー",
                        "登録完了処理中にエラーが発生: " + _e.getMessage());
            } catch (Exception dialogError) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "エラーダイアログの表示にも失敗", dialogError);
            }

            setProcessing(false);
            handleSaveCompleteSuccess = false;
        } finally {
            if (processing) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "処理中状態が解除されていません - 強制的に解除");
                setProcessing(false);
            }
        }
    }

    /**
     * 入力フィールドをクリア
     * すべての入力フィールドを初期状態にリセットし、エラー表示もクリア
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
     * コントローラーを通じて画面遷移
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
     * 処理中はUIコンポーネントを無効化し、プログレスインジケーターを表示
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
     * 完了処理の成功状態を取得
     *
     * @return 完了処理が成功した場合true、そうでなければfalse
     */
    public boolean isHandleSaveCompleteSuccess() {
        return handleSaveCompleteSuccess;
    }

    /**
     * 現在の処理中状態を取得
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