package controller;

import model.CSVAccessResult;
import model.EngineerCSVDAO;
import model.EngineerDTO;
import util.LogHandler;
import util.LogHandler.LogType;
import util.ResourceManager;
import view.AddPanel;
import view.DetailPanel;
import view.DialogManager;
import view.ListPanel;
import view.MainFrame;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * アプリケーションのメインコントローラー
 * 画面遷移、イベント処理、スレッド管理、エンジニア検索機能を統括するクラス
 *
 * <p>
 * このクラスは、エンジニア人材管理システムのMVCアーキテクチャにおける中心的なコントローラーとして機能し、
 * ビュー（画面）とモデル（データ）を連携させる役割を担います。アプリケーション全体の状態管理から
 * 個別の操作イベント処理まで、ビジネスロジックの多くの側面を統括的に制御します。
 * </p>
 *
 * <p>
 * バージョン4.12.8での主な改善点：
 * <ul>
 * <li>エンジニア検索機能の責任分離とMainControllerへの統合</li>
 * <li>検索条件の検証ロジックの一元化</li>
 * <li>検索結果処理の非同期化対応</li>
 * <li>MVCパターンに準拠した適切な責任分散</li>
 * </ul>
 * </p>
 *
 * <p>
 * 主な責務：
 * <ul>
 * <li>画面遷移の制御とビュー間の調整</li>
 * <li>ユーザーインターフェースイベントの処理</li>
 * <li>エンジニア検索機能のビジネスロジック制御</li>
 * <li>非同期処理とスレッド管理</li>
 * <li>アプリケーション終了処理の調整</li>
 * <li>モデル操作の統合管理</li>
 * <li>データの保存と読み込み処理</li>
 * <li>エラー処理と回復機能</li>
 * </ul>
 * </p>
 *
 * @author Nakano
 * @version 4.12.10
 * @since 2025-06-02
 */
public class MainController {

    /** 画面遷移コントローラー */
    private final ScreenTransitionController screenController;

    /** エンジニアデータコントローラー */
    private EngineerController engineerController;

    /** リソースマネージャー - ファイル操作の中心的な管理クラス */
    private ResourceManager resourceManager;

    /** メインフレーム */
    private final MainFrame mainFrame;

    /** リストパネル */
    private final ListPanel listPanel;

    /** ダイアログマネージャー */
    private final DialogManager dialogManager;

    /** 実行中非同期タスクの追跡マップ */
    private final ConcurrentMap<String, Thread> runningTasks;

    /** シャットダウン中フラグ */
    private final AtomicBoolean isShuttingDown;

    /** シャットダウン完了フラグ */
    private final AtomicBoolean isShutdownCompleted = new AtomicBoolean(false);

    private final Set<String> deletingIds = ConcurrentHashMap.newKeySet();

    // 最大レコード数の定数
    private static final int MAX_RECORDS = 1000;

    /**
     * エンジニア検索条件を保持するクラス
     * 検索フォームから入力された各種条件を格納し、検索処理に使用します
     */
    public static class SearchCriteria {
        private final String id;
        private final String name;
        private final String year;
        private final String month;
        private final String day;
        private final String career;

        /**
         * 検索条件を初期化するコンストラクタ
         * 
         * @param id     社員ID検索条件
         * @param name   氏名検索条件
         * @param year   生年月日（年）検索条件
         * @param month  生年月日（月）検索条件
         * @param day    生年月日（日）検索条件
         * @param career エンジニア歴検索条件
         */
        public SearchCriteria(String id, String name, String year, String month, String day, String career) {
            this.id = id;
            this.name = name;
            this.year = year;
            this.month = month;
            this.day = day;
            this.career = career;
        }

        /**
         * 生年月日に関する検索条件が設定されているかを判定
         * 
         * @return 年、月、日のいずれかが設定されている場合はtrue
         */
        public boolean hasDateCriteria() {
            return (year != null && !year.isEmpty()) ||
                    (month != null && !month.isEmpty()) ||
                    (day != null && !day.isEmpty());
        }

        // ゲッターメソッド群
        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getYear() {
            return year;
        }

        public String getMonth() {
            return month;
        }

        public String getDay() {
            return day;
        }

        public String getCareer() {
            return career;
        }
    }

    /**
     * エンジニア検索結果を保持するクラス
     * 検索処理の結果とエラー情報を格納し、呼び出し元に結果を返却します
     */
    public static class SearchResult {
        private final List<EngineerDTO> results;
        private final List<String> errors;

        /**
         * 検索結果を初期化するコンストラクタ
         * 
         * @param results 検索にヒットしたエンジニアのリスト
         * @param errors  検索処理中に発生したエラーメッセージのリスト
         */
        public SearchResult(List<EngineerDTO> results, List<String> errors) {
            this.results = results != null ? results : new ArrayList<>();
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        /**
         * 検索処理でエラーが発生したかを判定
         * 
         * @return エラーが存在する場合はtrue
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        // ゲッターメソッド群
        public List<EngineerDTO> getResults() {
            return results;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    /**
     * コンストラクタ
     * 必要な初期化を行いますが、完全な初期化はinitializeメソッドで行います
     *
     * @param mainFrame メインフレーム
     */
    public MainController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.listPanel = mainFrame.getListPanel();
        this.screenController = new ScreenTransitionController(mainFrame);
        this.runningTasks = new ConcurrentHashMap<>();
        this.isShuttingDown = new AtomicBoolean(false);
        this.dialogManager = DialogManager.getInstance();

        // 画面遷移コントローラーにメインコントローラーへの参照を設定
        this.screenController.setMainController(this);

        // MainFrameにMainControllerへの参照を設定（循環参照だが制御された形で）
        this.mainFrame.setMainController(this);

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "メインコントローラーを初期化しました");
    }

    /**
     * アプリケーションの初期化
     * ResourceManagerの適切な初期化と活用を含む
     */
    public void initialize() {
        try {
            // ResourceManagerの取得と初期化確認
            // これにより、ファイルパス管理が一元化されます
            resourceManager = ResourceManager.getInstance();
            if (!resourceManager.isInitialized()) {
                resourceManager.initialize();
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "ResourceManager を初期化しました");
            }

            // エンジニアコントローラーの初期化
            engineerController = new EngineerController();

            // 初期画面の表示
            screenController.showPanel("LIST");

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "アプリケーションを初期化しました");

            // ListPanelにMainControllerを設定
            JPanel panel = screenController.getCurrentPanel();
            if (panel instanceof ListPanel) {
                ((ListPanel) panel).setMainController(this);
            }

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "アプリケーションの初期化に失敗しました", e);
            handleFatalError(e);
        }
    }

    /**
     * エンジニア検索処理
     * 検索条件に基づいてエンジニア情報を検索し、結果を返却します
     * 
     * @param searchCriteria 検索条件オブジェクト
     * @return 検索結果オブジェクト（結果リストとエラー情報を含む）
     */
    public SearchResult searchEngineers(SearchCriteria searchCriteria) {
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "エンジニア検索処理を開始します");

            // 入力検証の実行
            List<String> validationErrors = validateSearchCriteria(searchCriteria);
            if (!validationErrors.isEmpty()) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "検索条件の検証でエラーが発生しました: " + String.join(", ", validationErrors));
                return new SearchResult(new ArrayList<>(), validationErrors);
            }

            // データ取得と検索実行
            List<EngineerDTO> allEngineers = engineerController.loadEngineers();
            List<EngineerDTO> filteredEngineers = filterEngineersByCriteria(allEngineers, searchCriteria);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("検索実行完了: %d件のデータがヒット（全%d件中）",
                            filteredEngineers.size(), allEngineers.size()));

            return new SearchResult(filteredEngineers, new ArrayList<>());

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "検索処理中にエラーが発生しました", e);
            return new SearchResult(new ArrayList<>(), List.of("検索処理中にエラーが発生しました: " + e.getMessage()));
        }
    }

    /**
     * 検索条件の妥当性検証
     * 各検索条件フィールドに対して適切な検証を実行します
     * 
     * @param criteria 検証対象の検索条件
     * @return 検証エラーメッセージのリスト（エラーがない場合は空リスト）
     */
    private List<String> validateSearchCriteria(SearchCriteria criteria) {
        List<String> errors = new ArrayList<>();

        // 社員ID検証
        if (criteria.getId() != null && !criteria.getId().trim().isEmpty()) {
            String id = criteria.getId().trim();
            // 全角数字を半角に変換してから検証
            String convertedId = id
                    .replace("0", "0")
                    .replace("1", "1")
                    .replace("2", "2")
                    .replace("3", "3")
                    .replace("4", "4")
                    .replace("5", "5")
                    .replace("6", "6")
                    .replace("7", "7")
                    .replace("8", "8")
                    .replace("9", "9");
            if (!convertedId.matches("^[0-9]{1,5}$")) {
                errors.add("社員IDは5桁以内の数字で入力してください");
            }
        }

        // 氏名検証
        if (criteria.getName() != null && !criteria.getName().trim().isEmpty()) {
            String name = criteria.getName().trim().replaceAll("\\s{2,}", " ");
            if (name.length() > 20) {
                errors.add("氏名は20文字以内で入力してください");
            }
            if (!name.matches("[ぁ-んァ-ヶ一-龯々〆〤ー\\s]*")) {
                errors.add("氏名は日本語のみで入力してください");
            }
        }

        // 生年月日検証（年月日の組み合わせチェック）
        boolean hasYear = criteria.getYear() != null && !criteria.getYear().isEmpty();
        boolean hasMonth = criteria.getMonth() != null && !criteria.getMonth().isEmpty();
        boolean hasDay = criteria.getDay() != null && !criteria.getDay().isEmpty();

        if ((hasYear || hasMonth || hasDay) && !(hasYear && hasMonth && hasDay)) {
            errors.add("生年月日は年・月・日すべてを選択してください");
        }

        // エンジニア歴検証
        if (criteria.getCareer() != null && !criteria.getCareer().trim().isEmpty()) {
            try {
                int careerValue = Integer.parseInt(criteria.getCareer());
                if (careerValue < 0 || careerValue > 50) {
                    errors.add("エンジニア歴は0年から50年の範囲で入力してください");
                }
            } catch (NumberFormatException e) {
                errors.add("エンジニア歴は数値で入力してください");
            }
        }

        return errors;
    }

    /**
     * 検索条件によるエンジニアリストのフィルタリング
     * 指定された検索条件に一致するエンジニアのみを抽出します
     * 
     * @param engineers 全エンジニアのリスト
     * @param criteria  検索条件
     * @return フィルタリングされたエンジニアのリスト
     */
    private List<EngineerDTO> filterEngineersByCriteria(List<EngineerDTO> engineers, SearchCriteria criteria) {
        return engineers.stream()
                .filter(engineer -> matchesCriteria(engineer, criteria))
                .collect(Collectors.toList());
    }

    /**
     * エンジニアが検索条件にマッチするかチェック
     * 各検索条件項目について個別に適合性を判定します
     * 
     * @param engineer 判定対象のエンジニア
     * @param criteria 検索条件
     * @return すべての条件にマッチする場合はtrue
     */
    private boolean matchesCriteria(EngineerDTO engineer, SearchCriteria criteria) {
        // 社員ID検索（部分一致、大文字小文字を区別しない）
        if (criteria.getId() != null && !criteria.getId().trim().isEmpty()) {
            if (engineer.getId() == null ||
                    !engineer.getId().toLowerCase().contains(criteria.getId().toLowerCase())) {
                return false;
            }
        }

        // 氏名検索（部分一致、大文字小文字を区別しない）
        if (criteria.getName() != null && !criteria.getName().trim().isEmpty()) {
            if (engineer.getName() == null ||
                    !engineer.getName().toLowerCase().contains(criteria.getName().toLowerCase())) {
                return false;
            }
        }

        // 生年月日検索（完全一致）
        if (engineer.getBirthDate() != null && criteria.hasDateCriteria()) {
            String birthDateStr = engineer.getBirthDate().toString();

            // 年の検証
            if (!criteria.getYear().isEmpty() && !birthDateStr.startsWith(criteria.getYear())) {
                return false;
            }

            // 月の検証（ゼロパディング対応）
            if (!criteria.getMonth().isEmpty()) {
                String monthPart = criteria.getMonth().length() == 1 ? "0" + criteria.getMonth() : criteria.getMonth();
                if (!birthDateStr.substring(5, 7).equals(monthPart)) {
                    return false;
                }
            }

            // 日の検証（ゼロパディング対応）
            if (!criteria.getDay().isEmpty()) {
                String dayPart = criteria.getDay().length() == 1 ? "0" + criteria.getDay() : criteria.getDay();
                if (!birthDateStr.substring(8, 10).equals(dayPart)) {
                    return false;
                }
            }
        } else if (criteria.hasDateCriteria()) {
            // 生年月日がnullで検索条件が指定されている場合は不一致
            return false;
        }

        // エンジニア歴検索（完全一致）
        if (criteria.getCareer() != null && !criteria.getCareer().trim().isEmpty()) {
            try {
                int searchCareerValue = Integer.parseInt(criteria.getCareer());
                if (engineer.getCareer() != searchCareerValue) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * イベントを処理
     * アプリケーション全体のイベントをディスパッチします
     *
     * @param event イベント種別（"REFRESH_VIEW", "CHANGE_PANEL", "SAVE_DATA", "LOAD_DATA",
     *              "SEARCH_ENGINEERS", "SHUTDOWN"）
     * @param data  イベントデータ（イベント種別に応じたデータ）
     */
    public void handleEvent(String event, Object data) {
        try {
            // シャットダウン中は新しいイベントを処理しない
            if (isShuttingDown.get()) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "シャットダウン中のためイベントを無視します: " + event);
                return;
            }

            switch (event) {
                case "REFRESH_VIEW":
                    screenController.refreshView();
                    break;
                case "CHANGE_PANEL":
                    screenController.showPanel((String) data);
                    break;
                case "SAVE_DATA":
                    handleSaveData(data);
                    break;
                case "LOAD_DATA":
                    handleLoadData();
                    break;
                case "VIEW_DETAIL":
                    handleViewDetail((String) data);
                    break;
                case "SEARCH_ENGINEERS":
                    handleSearchEngineers(data);
                    break;
                case "TEMPLATE":
                    handleTemplateExport();
                    break;
                case "EXPORT_CSV":
                    handleExportCSV(data);
                    break;
                case "IMPORT_CSV":
                    handleImportData();
                    break;
                case "SHUTDOWN":
                    initiateShutdown();
                    break;
                case "DELETE_ENGINEER":
                    handleDeleteEngineer(data);
                    break;
                default:
                    handleUnknownEvent(event, data);
                    break;
            }

            LogHandler.getInstance().log(
                    Level.INFO, LogType.SYSTEM,
                    String.format("イベントを処理しました: %s", event));

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "イベント処理に失敗しました: " + event, e);
            handleError(e);
        }
    }

    /**
     * エンジニア検索処理のイベントハンドラ
     * 検索要求を受け取り、非同期で検索処理を実行します
     * 
     * @param data 検索条件オブジェクト（SearchCriteria型）
     */
    private void handleSearchEngineers(Object data) {
        if (!(data instanceof SearchCriteria)) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "検索条件の型が不正です: " + (data != null ? data.getClass().getName() : "null"));
            return;
        }

        SearchCriteria criteria = (SearchCriteria) data;

        // 非同期で検索処理を実行
        startAsyncTask("SearchEngineers", () -> {
            try {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "非同期検索処理を開始します");

                SearchResult result = searchEngineers(criteria);

                // UI更新はSwingのEDTで実行
                SwingUtilities.invokeLater(() -> {
                    try {
                        JPanel currentPanel = screenController.getCurrentPanel();
                        if (currentPanel instanceof ListPanel) {
                            // ListPanelに検索結果を通知するメソッドを呼び出し
                            handleSearchResultForListPanel((ListPanel) currentPanel, result);
                        } else {
                            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                    "現在のパネルがListPanelではないため検索結果を表示できません");
                        }
                    } catch (Exception e) {
                        LogHandler.getInstance().logError(LogType.SYSTEM,
                                "検索結果のUI更新中にエラーが発生しました", e);
                    }
                });

            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "非同期検索処理中にエラーが発生しました", e);

                // エラー時のUI更新
                SwingUtilities.invokeLater(() -> {
                    DialogManager.getInstance().showErrorDialog(
                            "検索エラー", "検索処理中にエラーが発生しました: " + e.getMessage());
                });
            }
        });
    }

    /**
     * ListPanel向けの検索結果処理
     * 検索結果をListPanelに適切に反映します
     * 
     * @param listPanel 更新対象のListPanel
     * @param result    検索結果
     */
    private void handleSearchResultForListPanel(ListPanel listPanel, SearchResult result) {
        try {
            if (result.hasErrors()) {
                // 検証エラーがある場合はエラーダイアログを表示
                DialogManager.getInstance().showValidationErrorDialog(result.getErrors());
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "検索条件にエラーがあります: " + String.join(", ", result.getErrors()));
            } else {
                // 検索結果が正常な場合
                List<EngineerDTO> searchResults = result.getResults();

                if (searchResults.isEmpty()) {
                    // 検索結果が空の場合は通知
                    DialogManager.getInstance().showInfoDialog("検索結果", "該当するエンジニアは見つかりませんでした。");
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "検索結果が0件でした");
                } else {
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            String.format("検索結果を表示します: %d件", searchResults.size()));
                }

                // 検索結果をListPanelに通知（具体的な実装はListPanel側で対応）
                // 注意: この部分はListPanel側に対応するメソッドを追加する必要があります
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "検索結果をListPanelに反映しました");
            }
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "検索結果の処理中にエラーが発生しました", e);
            DialogManager.getInstance().showErrorDialog(
                    "表示エラー", "検索結果の表示中にエラーが発生しました");
        }
    }

    /**
     * 非同期タスクを開始
     * タスクをバックグラウンドで実行し、完了時のコールバックを設定
     *
     * @param taskId タスク識別子
     * @param task   実行するRunnable
     */
    public void startAsyncTask(String taskId, Runnable task) {
        // シャットダウン中は新しいタスクを開始しない
        if (isShuttingDown.get()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "シャットダウン中のため非同期タスクを開始できません: " + taskId);
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "非同期タスクを開始: " + taskId);
                task.run();
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "非同期タスクが完了: " + taskId);
            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "非同期タスクの実行中にエラーが発生: " + taskId, e);
            } finally {
                runningTasks.remove(taskId);
            }
        }, "AsyncTask-" + taskId);

        // 実行中タスクマップに追加
        runningTasks.put(taskId, thread);

        // スレッドを登録して開始
        mainFrame.registerThread(thread);
        thread.start();
    }

    /**
     * エンジニア情報の削除処理を非同期で実行します。
     * 
     * @param data 削除対象のエンジニアDTOリスト
     */
    @SuppressWarnings("unchecked")
    private void handleDeleteEngineer(Object data) {
        List<EngineerDTO> targetList = (List<EngineerDTO>) data;
        JPanel panel = screenController.getCurrentPanel();

        if (panel instanceof ListPanel listPanel) {
            listPanel.setStatus("削除中...   ");
        }

        // 削除中フラグがtrueであれば、登録ボタンを無効化
        screenController.setRegisterButtonEnabled(false);

        // 削除対象IDを記録
        targetList.forEach(dto -> deletingIds.add(dto.getId()));

        startAsyncTask("delete_engineers", () -> {
            EngineerController controller = new EngineerController();
            controller.deleteEngineers(targetList, () -> {
                SwingUtilities.invokeLater(() -> {
                    if (panel instanceof ListPanel listPanel) {
                        listPanel.clearStatus();

                    }

                    DialogManager.getInstance().showCompletionDialog("削除が完了しました。", () -> {
                        try {
                            // 削除完了後に対象IDを解除
                            targetList.forEach(dto -> deletingIds.remove(dto.getId()));
                            // CSV から最新データを再取得
                            List<EngineerDTO> updatedList = engineerController.loadEngineers();
                            // ListPanel に再設定（再描画される）
                            JPanel refreshedPanel = screenController.getCurrentPanel();
                            if (refreshedPanel instanceof ListPanel listPanel) {
                                listPanel.setEngineerData(updatedList);
                            }
                            // 登録ボタン再有効化
                            screenController.setRegisterButtonEnabled(true);
                            if (panel instanceof ListPanel listPanel) {
                                listPanel.onDeleteCompleted(); // ←ここで削除中フラグ解除

                            }

                            ListPanel.setNeedsRefresh(true); // ← 一覧更新フラグON

                            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "削除後のデータを再読み込みしました");

                        } catch (Exception e) {
                            LogHandler.getInstance().logError(LogType.SYSTEM, "削除後の再読み込みに失敗しました", e);
                        }

                    });

                });
            });
        });
    }

    /**
     * 最新のエンジニア一覧を取得します。
     *
     * @return エンジニアDTOのリスト
     */
    public List<EngineerDTO> getEngineerList() {
        if (engineerController == null) {
            engineerController = new EngineerController(); // 念のため
        }
        return engineerController.loadEngineers();
    }

    /**
     * 最新のエンジニア一覧を取得します。
     *
     * @return エンジニアDTOのリスト
     */
    public ListPanel getListPanel() {
        JPanel panel = screenController.getPanelByType("LIST");
        if (panel instanceof ListPanel listPanel) {
            return listPanel;
        }
        return null;
    }

    /**
     * データ保存処理を実行します
     * <p>
     * このメソッドはエンジニア情報の保存処理を非同期的に実行します。単一エンジニアの追加と
     * 複数エンジニアの一括更新に対応しています。保存処理の成功後は、UIを適切に更新し、
     * 必要に応じてダイアログ表示や画面遷移を行います。
     * </p>
     * 
     * <p>
     * 主な処理の流れ：
     * <ol>
     * <li>データ型の判定（単一エンジニアまたはエンジニアリスト）</li>
     * <li>コンテキスト情報（元画面の参照）の取得と保持</li>
     * <li>非同期タスクとしての保存処理実行</li>
     * <li>成功時のUI更新（リスト更新、ダイアログ表示など）</li>
     * <li>エラー時の例外処理と状態復元</li>
     * </ol>
     * </p>
     * 
     * <p>
     * このメソッドは特に次の点に注意して実装されています：
     * <ul>
     * <li>スレッド間の安全な連携（バックグラウンド処理とEDTの連携）</li>
     * <li>コンテキスト情報の確実な保持と伝達</li>
     * <li>例外発生時の適切な処理とロギング</li>
     * <li>処理状態の確実なリセット（処理中フラグの管理）</li>
     * </ul>
     * </p>
     * 
     * @param data 保存するデータ（{@link EngineerDTO}または{@link List}&lt;{@link EngineerDTO}&gt;）
     */
    private void handleSaveData(Object data) {
        // シャットダウン中は処理しない
        if (isShuttingDown.get()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "シャットダウン中のため保存処理をスキップします");
            return;
        }

        // エンジニア情報の追加処理（単一オブジェクト）
        if (data instanceof EngineerDTO) {
            try {
                EngineerDTO engineer = (EngineerDTO) data;
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "エンジニア情報の保存を開始します: ID=" + engineer.getId());

                // パネル情報を取得（直接参照として）
                JPanel sourcePanel = screenController.getCurrentPanel();
                String sourcePanelType = screenController.getCurrentPanelType();
                AddPanel addPanel = null;
                DetailPanel detailPanel = null;

                // パネルタイプを判定して適切な参照を取得
                if (sourcePanel instanceof AddPanel) {
                    addPanel = (AddPanel) sourcePanel;
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "保存処理の元パネルを記録: AddPanel");
                } else if (sourcePanel instanceof DetailPanel) {
                    detailPanel = (DetailPanel) sourcePanel;
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "保存処理の元パネルを記録: DetailPanel");
                } else {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "保存処理の元パネルが特定できません: " +
                                    (sourcePanel != null ? sourcePanel.getClass().getName() : "null"));
                }

                // final変数として保持（ラムダ式内で使用するため）
                final AddPanel finalAddPanel = addPanel;
                final DetailPanel finalDetailPanel = detailPanel;

                // 非同期タスクとして保存処理を実行
                startAsyncTask("SaveEngineer", () -> {
                    try {
                        // エンジニア情報を保存（新規追加または更新）
                        boolean success;

                        // IDを検証して既存のエンジニアかどうかを判断
                        EngineerDTO existingEngineer = engineerController.getEngineerById(engineer.getId());
                        if (existingEngineer != null) {
                            // 既存エンジニアの場合は更新処理
                            success = engineerController.updateEngineer(engineer);
                            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                    "エンジニア情報の更新処理を実行: ID=" + engineer.getId());
                        } else {
                            // 新規エンジニアの場合は追加処理
                            try {
                                success = engineerController.addEngineer(engineer);
                                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                        "エンジニア情報の新規追加処理を実行: ID=" + engineer.getId());
                            } catch (EngineerController.TooManyRecordsException e) {
                                // 登録上限エラーの処理
                                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                        "登録件数の上限に達したため、エンジニア情報の追加に失敗しました: " + e.getMessage());

                                // UI更新はSwingのEDTで実行
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    // エラーダイアログ表示
                                    DialogManager.getInstance().showErrorDialog(
                                            "登録制限エラー",
                                            "登録件数の上限(" + MAX_RECORDS + "件)に達しています。これ以上登録できません。\n" +
                                                    "不要なデータを削除してから再試行してください。");

                                    // 処理中状態を解除
                                    if (finalAddPanel != null) {
                                        finalAddPanel.setProcessing(false);
                                    }
                                    if (finalDetailPanel != null) {
                                        finalDetailPanel.setProcessing(false);
                                    }
                                });
                                return;
                            }
                        }

                        if (success) {
                            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                    "エンジニア情報の保存に成功しました: ID=" + engineer.getId());

                            // UI更新はSwingのEDTで実行
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                try {
                                    // **重要な変更点**: ListPanelを常に更新（現在表示中かどうかにかかわらず）
                                    JPanel listPanel = getListPanelFromCache();
                                    if (listPanel instanceof ListPanel) {
                                        ((ListPanel) listPanel).addEngineerData(engineer);
                                        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                                "ListPanelにエンジニアデータを追加しました: " + engineer.getId());
                                    }

                                    // 保存元のパネルタイプに応じた完了処理を呼び出し
                                    if (finalDetailPanel != null) {
                                        // DetailPanelからの保存（更新）の場合
                                        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                                "DetailPanelの完了処理を呼び出します: " + engineer.getId());
                                        finalDetailPanel.handleUpdateComplete(engineer);
                                    } else if (finalAddPanel != null) {
                                        // AddPanelからの保存（新規追加）の場合
                                        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                                "AddPanelの完了処理を呼び出します: " + engineer.getId());
                                        finalAddPanel.handleSaveComplete(engineer);
                                    } else if (sourcePanel instanceof DetailPanel && "DETAIL".equals(sourcePanelType)) {
                                        // 元パネルがDetailPanelだが直接参照が取得できなかった場合
                                        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                                "DetailPanelの完了処理を呼び出します（間接参照）: " + engineer.getId());
                                        ((DetailPanel) sourcePanel).handleUpdateComplete(engineer);
                                    } else if (sourcePanel instanceof AddPanel && "ADD".equals(sourcePanelType)) {
                                        // 元パネルがAddPanelだが直接参照が取得できなかった場合
                                        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                                "AddPanelの完了処理を呼び出します（間接参照）: " + engineer.getId());
                                        ((AddPanel) sourcePanel).handleSaveComplete(engineer);
                                    } else {
                                        // 対応するパネルが見つからない場合
                                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                                "対応するパネルが見つからないため完了処理をスキップします: " +
                                                        "sourcePanel="
                                                        + (sourcePanel != null ? sourcePanel.getClass().getName()
                                                                : "null")
                                                        +
                                                        ", sourcePanelType=" + sourcePanelType);

                                        // 代替手段としてダイアログを直接表示
                                        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                                "代替手段としてダイアログを直接表示します");
                                        DialogManager.getInstance().showCompletionDialog(
                                                "処理完了",
                                                "エンジニア情報を保存しました: ID=" + engineer.getId() + ", 名前="
                                                        + engineer.getName());
                                    }

                                    // 画面更新
                                    screenController.refreshView();
                                } catch (Exception e) {
                                    LogHandler.getInstance().logError(LogType.SYSTEM,
                                            "保存完了後の処理中にエラーが発生しました: " + engineer.getId(), e);

                                    // エラー時もパネルの処理中状態は解除
                                    try {
                                        if (finalAddPanel != null) {
                                            finalAddPanel.setProcessing(false);
                                        }
                                        if (finalDetailPanel != null) {
                                            finalDetailPanel.setProcessing(false);
                                        }
                                    } catch (Exception ex) {
                                        LogHandler.getInstance().logError(LogType.SYSTEM,
                                                "エラー処理中にも例外が発生しました", ex);
                                    }
                                }
                            });
                        } else {
                            // 保存失敗時の処理
                            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                    "エンジニア情報の保存に失敗しました: ID=" + engineer.getId());

                            javax.swing.SwingUtilities.invokeLater(() -> {
                                if (finalAddPanel != null) {
                                    finalAddPanel.setProcessing(false);
                                    DialogManager.getInstance().showErrorDialog(
                                            "保存エラー", "エンジニア情報の保存に失敗しました");
                                }
                                if (finalDetailPanel != null) {
                                    finalDetailPanel.setProcessing(false);
                                    DialogManager.getInstance().showErrorDialog(
                                            "更新エラー", "エンジニア情報の更新に失敗しました");
                                }
                            });
                        }
                    } catch (Exception e) {
                        LogHandler.getInstance().logError(LogType.SYSTEM,
                                "エンジニア情報の保存処理中にエラーが発生しました: " + engineer.getId(), e);

                        // エラー時に処理中状態を解除
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            if (finalAddPanel != null) {
                                finalAddPanel.setProcessing(false);
                                DialogManager.getInstance().showErrorDialog(
                                        "保存エラー", "エンジニア情報の保存中にエラーが発生しました: " + e.getMessage());
                            }
                            if (finalDetailPanel != null) {
                                finalDetailPanel.setProcessing(false);
                                DialogManager.getInstance().showErrorDialog(
                                        "更新エラー", "エンジニア情報の更新中にエラーが発生しました: " + e.getMessage());
                            }
                        });
                    } finally {
                        // 最終的に処理中状態を解除する追加の保険
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            try {
                                if (finalAddPanel != null) {
                                    // 処理状態の強制リセット（念のため）
                                    finalAddPanel.setProcessing(false);
                                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                            "AddPanelの処理状態をリセットしました");
                                }
                                if (finalDetailPanel != null) {
                                    // 処理状態の強制リセット（念のため）
                                    finalDetailPanel.setProcessing(false);
                                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                            "DetailPanelの処理状態をリセットしました");
                                }
                            } catch (Exception e) {
                                LogHandler.getInstance().logError(LogType.SYSTEM,
                                        "処理状態リセット中にエラーが発生しました", e);
                            }
                        });
                    }
                });

            } catch (ClassCastException e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "保存データの型が不正です", e);
            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の保存前処理中にエラーが発生しました", e);
            }
        }
        // エンジニア情報の一括保存処理（リスト）
        else if (data instanceof List<?>) {
            try {
                @SuppressWarnings("unchecked")
                List<EngineerDTO> engineers = (List<EngineerDTO>) data;
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "エンジニア情報の一括保存を開始します: " + engineers.size() + "件");

                // 非同期タスクとして保存処理を実行
                startAsyncTask("SaveEngineerList", () -> {
                    try {
                        // 各エンジニア情報を更新
                        boolean success = true;
                        for (EngineerDTO engineer : engineers) {
                            success &= engineerController.updateEngineer(engineer);
                        }

                        if (success) {
                            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                    "エンジニア情報の一括保存に成功しました: " + engineers.size() + "件");

                            // UI更新はSwingのEDTで実行
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                try {
                                    // 画面更新
                                    screenController.refreshView();

                                    // 保存完了ダイアログを表示
                                    DialogManager.getInstance().showCompletionDialog(
                                            "保存完了", engineers.size() + "件のエンジニア情報を保存しました");
                                } catch (Exception e) {
                                    LogHandler.getInstance().logError(LogType.SYSTEM,
                                            "一括保存後の処理中にエラーが発生しました", e);
                                }
                            });
                        } else {
                            // 保存失敗時の処理
                            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                    "エンジニア情報の一括保存に失敗しました");

                            javax.swing.SwingUtilities.invokeLater(() -> {
                                DialogManager.getInstance().showErrorDialog(
                                        "保存エラー", "エンジニア情報の一括保存に失敗しました");
                            });
                        }
                    } catch (Exception e) {
                        LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の一括保存処理中にエラーが発生しました", e);

                        javax.swing.SwingUtilities.invokeLater(() -> {
                            DialogManager.getInstance().showErrorDialog(
                                    "保存エラー", "エンジニア情報の一括保存中にエラーが発生しました: " + e.getMessage());
                        });
                    }
                });

            } catch (ClassCastException e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "保存データの型が不正です", e);
            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の一括保存前処理中にエラーが発生しました", e);
            }
        }
        // その他のデータ型（非対応）
        else {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "未対応のデータ型が保存要求されました: " + (data != null ? data.getClass().getName() : "null"));
        }
    }

    /**
     * データ読み込み処理
     * エンジニア情報の読み込みを行います
     */
    private void handleLoadData() {
        // 非同期タスクとして読み込み処理を実行
        startAsyncTask("LoadData", () -> {
            try {
                List<EngineerDTO> engineers = engineerController.loadEngineers();

                // データ件数が上限を超えているかチェック
                if (engineers.size() > MAX_RECORDS) {
                    LogHandler.getInstance().log(Level.SEVERE, LogType.SYSTEM,
                            "登録されているエンジニアデータが上限(" + MAX_RECORDS + "件)を超えています: " + engineers.size() + "件");

                    // UI更新はSwingのEDTで実行
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        // エラーダイアログを表示
                        DialogManager.getInstance().showErrorDialog(
                                "データ制限エラー",
                                "登録されているエンジニアデータが上限(" + MAX_RECORDS + "件)を超えています: " + engineers.size() + "件\n" +
                                        "アプリケーションを終了します。");

                        // アプリケーションの安全な終了処理を実行
                        // すでに実装されているinitiateShutdownメソッドを使用
                        initiateShutdown();
                    });
                    return;
                }

                // UI更新はSwingのEDTで実行
                javax.swing.SwingUtilities.invokeLater(() -> {
                    // ListPanelを取得してエンジニアデータを設定
                    JPanel currentPanel = screenController.getCurrentPanel();
                    if (currentPanel instanceof ListPanel) {
                        ((ListPanel) currentPanel).setEngineerData(engineers);
                    }
                    screenController.refreshView();

                    // データ件数の表示 (オプション)
                    if (currentPanel instanceof ListPanel) {
                        ((ListPanel) currentPanel).setStatus("読込完了: " + engineers.size() + "件");
                    }
                });

            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "データ読み込みに失敗しました", e);

                // エラーメッセージ表示
                javax.swing.SwingUtilities.invokeLater(() -> {
                    DialogManager.getInstance().showErrorDialog(
                            "読み込みエラー",
                            "データ読み込みに失敗しました: " + e.getMessage());
                });
            }
        });
    }

    /**
     * 詳細表示処理
     * エンジニアIDを指定して詳細画面に遷移します
     * 
     * @param engineerId 表示するエンジニアID
     */
    /**
     * 詳細表示処理
     * エンジニアIDを指定して詳細画面に遷移します
     * 
     * @param engineerId 表示するエンジニアID
     */
    private void handleViewDetail(String engineerId) {
        // 削除中のIDは遷移禁止
        if (deletingIds.contains(engineerId)) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "削除中のため詳細画面に遷移しません: ID=" + engineerId);
            return;
        }
        // ★ListPanelから削除中フラグを取得（この行だけ追加）
        final boolean isCurrentlyDeleting;
        JPanel listPanelRaw = screenController.getPanelByType("LIST");
        if (listPanelRaw instanceof ListPanel listPanel) {
            isCurrentlyDeleting = listPanel.isDeleting();
        } else {
            isCurrentlyDeleting = false;
        }
        try {
            // エンジニア情報を取得
            EngineerDTO engineer = engineerController.getEngineerById(engineerId);

            if (engineer != null) {
                // 詳細画面に遷移し、完了後にエンジニア情報を設定するコールバックを指定
                screenController.showPanelWithCallback("DETAIL", () -> {
                    JPanel currentPanel = screenController.getCurrentPanel();
                    if (currentPanel instanceof DetailPanel detailPanel) {
                        // エンジニア情報を設定
                        detailPanel.setEngineerData(engineer);

                        // ★更新ボタンを削除中なら無効化（この行だけ追加）
                        detailPanel.setUpdateButtonEnabled(!isCurrentlyDeleting);

                        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                "エンジニア詳細を表示: ID=" + engineerId);
                    } else {
                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                "DetailPanelへの参照取得に失敗しました");
                    }
                });
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "指定されたIDのエンジニアが見つかりません: " + engineerId);

                // エラーダイアログを表示
                DialogManager.getInstance().showErrorDialog(
                        "エラー", "指定されたIDのエンジニアが見つかりません: " + engineerId);
            }
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "エンジニア詳細表示中にエラーが発生しました: ID=" + engineerId, e);

            // エラーダイアログを表示
            DialogManager.getInstance().showErrorDialog(
                    "エラー", "エンジニア詳細表示中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * テンプレートCSV出力機能
     * ResourceManagerを活用したファイル操作の統合版
     */
    public void handleTemplateExport() {
        startAsyncTask("TemplateCSV", () -> {
            // UI操作：出力確認
            if (!confirmTemplateExport()) {
                return;
            }

            // ResourceManagerを活用したファイル選択処理
            // デフォルトのデータディレクトリを初期表示ディレクトリとして使用
            File selectedFile = selectTemplateFileWithResourceManager("エンジニア情報テンプレート.csv");
            if (selectedFile == null) {
                return;
            }

            // ビジネスロジック：テンプレート出力処理の実行
            executeTemplateExportWithResourceManager(selectedFile);
        });
    }

    /**
     * テンプレート出力確認処理
     * 
     * @return 出力許可の場合true
     */
    private boolean confirmTemplateExport() {
        int confirm = JOptionPane.showConfirmDialog(
                null,
                "テンプレートを出力しますか？",
                "テンプレート出力確認",
                JOptionPane.YES_NO_OPTION);

        boolean confirmed = (confirm == JOptionPane.YES_OPTION);
        if (!confirmed) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "テンプレート出力がユーザーによりキャンセルされました");
        }

        return confirmed;
    }

    /**
     * ResourceManagerを活用したテンプレート出力用ファイル選択処理
     * ユーザビリティの向上と一貫したファイル操作を実現
     * 
     * @param defaultFileName デフォルトファイル名
     * @return 選択されたファイル、キャンセル時はnull
     */
    private File selectTemplateFileWithResourceManager(String defaultFileName) {
        File selectedFile = null;

        while (true) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("テンプレートCSV出力先");

            // ResourceManagerからデータディレクトリを取得して初期ディレクトリに設定
            // これにより、ユーザーは適切なディレクトリから開始できます
            try {
                Path dataDir = resourceManager.getDataDirectoryPath();
                if (dataDir != null && dataDir.toFile().exists()) {
                    fileChooser.setCurrentDirectory(dataDir.toFile());
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "ファイル選択ダイアログの初期ディレクトリを設定: " + dataDir);
                }
            } catch (Exception e) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "初期ディレクトリの設定に失敗しましたが、処理を続行します: " + e.getMessage());
            }

            fileChooser.setSelectedFile(new File(defaultFileName));

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "出力先選択画面を表示をしました");
            int userSelection = fileChooser.showSaveDialog(listPanel);

            if (userSelection != JFileChooser.APPROVE_OPTION) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "テンプレート出力がキャンセルされました");
                return null;
            }

            selectedFile = fileChooser.getSelectedFile();

            // 拡張子の自動付加
            if (!selectedFile.getName().toLowerCase().endsWith(".csv")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".csv");
            }

            // ResourceManagerを活用したファイル検証
            // 統一されたバリデーションロジックを使用
            FileValidationResult validation = validateFileSelectionWithResourceManager(selectedFile);
            if (!validation.isValid()) {
                DialogManager.getInstance().showErrorDialog("エラー", validation.getErrorMessage());
                continue;
            }

            // 上書き確認処理
            if (selectedFile.exists()) {
                if (!confirmTemplateOverwrite(selectedFile)) {
                    continue;
                }
            }

            break;
        }

        return selectedFile;
    }

    /**
     * テンプレートファイル上書き確認処理
     * 
     * @param file 上書き対象ファイル
     * @return 上書き許可の場合true
     */
    private boolean confirmTemplateOverwrite(File file) {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "保存場所に同一名ファイルが存在したため、上書き確認ダイアログを表示しました");

        int overwriteConfirm = JOptionPane.showConfirmDialog(
                null,
                "ファイル" + file.getName() + "は既に存在します。上書きしますか？",
                "上書き確認",
                JOptionPane.YES_NO_OPTION);

        boolean confirmed = (overwriteConfirm == JOptionPane.YES_OPTION);
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                confirmed ? "ファイルの上書きが許可されました" : "上書き保存が許可されませんでした");

        return confirmed;
    }

    /**
     * ResourceManagerを活用したテンプレート出力処理の実行
     * リソース管理の統合とエラーハンドリングの改善
     * 
     * @param outputFile 出力先ファイル
     */
    private void executeTemplateExportWithResourceManager(File outputFile) {
        JPanel panel = screenController.getCurrentPanel();

        // ステータス表示更新
        updateExportStatus(panel, "テンプレート出力中...   ");

        // ResourceManagerにファイルリソースを登録してリソース管理を委譲
        String resourceKey = "template_export_" + System.currentTimeMillis();

        try {
            // テンプレート出力の実行
            boolean success = new EngineerCSVDAO().exportTemplate(outputFile.getPath());

            if (success) {
                // 成功時のリソース登録（必要に応じて）
                // resourceManager.registerResource(resourceKey, appropriateCloseable);

                SwingUtilities.invokeLater(() -> {
                    DialogManager.getInstance().showInfoDialog("出力完了",
                            "テンプレートCSVを保存しました。\n保存先: " + outputFile.getAbsolutePath());
                    clearExportStatus(panel);
                });

                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "テンプレートファイルの出力が成功しました: " + outputFile.getAbsolutePath());
            } else {
                throw new RuntimeException("テンプレート出力処理が失敗しました");
            }

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "テンプレート出力処理中に例外が発生しました", e);

            SwingUtilities.invokeLater(() -> {
                DialogManager.getInstance().showErrorDialog("エラー",
                        "テンプレート出力中にエラーが発生しました。\n" +
                                "保存先のフォルダにアクセスできない可能性があります。\n" +
                                "詳細: " + e.getMessage());
                clearExportStatus(panel);
            });
        } finally {
            // ResourceManagerを通じたリソースのクリーンアップ
            try {
                resourceManager.releaseResource(resourceKey);
            } catch (Exception cleanupError) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "リソースクリーンアップ中にエラーが発生しました: " + cleanupError.getMessage());
            }
        }
    }

    /**
     * エンジニア情報のCSV出力を非同期で実行
     * ResourceManagerを活用したファイル操作の統合版
     * 
     * @param data CSV出力対象のエンジニアDTOリスト
     */
    @SuppressWarnings("unchecked")
    public void handleExportCSV(Object data) {
        List<EngineerDTO> targetList = (List<EngineerDTO>) data;

        startAsyncTask("export_engineers", () -> {
            // ResourceManagerを活用したファイル選択とバリデーション
            File selectedFile = selectExportFileWithResourceManager("エンジニア情報-" + LocalDate.now() + ".csv");
            if (selectedFile == null) {
                return; // ユーザーがキャンセルした場合
            }

            // ビジネスロジック：CSV出力処理の実行
            executeCSVExportWithResourceManager(targetList, selectedFile);
        });
    }

    /**
     * ResourceManagerを活用したCSV出力用ファイル選択処理
     * 一貫したユーザーエクスペリエンスとエラーハンドリング
     * 
     * @param defaultFileName デフォルトファイル名
     * @return 選択されたファイル、キャンセル時はnull
     */
    private File selectExportFileWithResourceManager(String defaultFileName) {
        File selectedFile = null;

        while (true) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("CSVファイルの保存先");

            // ResourceManagerからデータディレクトリを初期ディレクトリに設定
            try {
                Path dataDir = resourceManager.getDataDirectoryPath();
                if (dataDir != null && dataDir.toFile().exists()) {
                    fileChooser.setCurrentDirectory(dataDir.toFile());
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "CSVエクスポートダイアログの初期ディレクトリを設定: " + dataDir);
                }
            } catch (Exception e) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "初期ディレクトリの設定に失敗しましたが、処理を続行します: " + e.getMessage());
            }

            fileChooser.setSelectedFile(new File(defaultFileName));

            int result = fileChooser.showSaveDialog(listPanel);
            if (result != JFileChooser.APPROVE_OPTION) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "CSV出力がキャンセルされました");
                return null;
            }

            selectedFile = fileChooser.getSelectedFile();

            // 拡張子の自動付加
            if (!selectedFile.getName().toLowerCase().endsWith(".csv")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".csv");
            }

            // ResourceManagerを活用したファイル名妥当性検証
            FileValidationResult validation = validateFileSelectionWithResourceManager(selectedFile);
            if (!validation.isValid()) {
                DialogManager.getInstance().showErrorDialog("エラー", validation.getErrorMessage());
                continue;
            }

            // 上書き確認処理
            if (selectedFile.exists()) {
                if (!confirmFileOverwrite(selectedFile)) {
                    continue; // 上書き拒否の場合は再選択
                }
            }

            break; // 全ての検証をクリアした場合
        }

        return selectedFile;
    }

    /**
     * ResourceManagerを活用したファイル検証
     * 統一されたバリデーションロジックとエラーハンドリング
     * 
     * @param file 検証対象ファイル
     * @return 検証結果
     */
    private FileValidationResult validateFileSelectionWithResourceManager(File file) {
        try {
            // 不正文字チェック（既存のロジック）
            if (file.getName().matches(".*[\\\\/:*?\"<>|].*")) {
                return new FileValidationResult(false,
                        "ファイル名に使用できない文字が含まれています。\n使用できない文字: \\ / : * ? \" < > |");
            }

            // 親ディレクトリの書き込み権限チェック
            File parentDir = file.getParentFile();
            if (!parentDir.canWrite()) {
                return new FileValidationResult(false,
                        "指定された保存先に書き込む権限がありません。\n別の場所を選択してください。");
            }

            // ResourceManagerを通じた追加的な検証
            // 例：データディレクトリ配下であることの確認など
            Path dataDir = resourceManager.getDataDirectoryPath();
            Path filePath = file.toPath().toAbsolutePath();

            // データディレクトリの存在確認と作成
            if (dataDir != null && !dataDir.toFile().exists()) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "データディレクトリが存在しないため、作成を試みます: " + dataDir);
                // ResourceManager経由でディレクトリ作成を試行できます
            }

            return new FileValidationResult(true, null);

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "ファイル検証中にエラーが発生しました", e);
            return new FileValidationResult(false,
                    "ファイル検証中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * ファイル上書き確認処理
     * 
     * @param file 上書き対象ファイル
     * @return 上書き許可の場合true
     */
    private boolean confirmFileOverwrite(File file) {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "保存場所に同一名ファイルが存在したため、上書き確認ダイアログを表示しました");

        int overwrite = JOptionPane.showConfirmDialog(
                listPanel,
                "ファイル " + file.getName() + " は既に存在します。上書きしますか？",
                "上書き確認",
                JOptionPane.YES_NO_OPTION);

        boolean confirmed = (overwrite == JOptionPane.YES_OPTION);
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                confirmed ? "CSV出力ファイルの上書きが許可されました" : "上書き保存が許可されませんでした");

        return confirmed;
    }

    /**
     * ResourceManagerを活用したCSV出力処理の実行
     * リソース管理とエラーハンドリングの統合
     * 
     * @param targetList 出力対象データ
     * @param outputFile 出力先ファイル
     */
    private void executeCSVExportWithResourceManager(List<EngineerDTO> targetList, File outputFile) {
        JPanel panel = screenController.getCurrentPanel();

        // ステータス表示更新
        updateExportStatus(panel, "CSV出力中...   ");

        // ResourceManagerにリソースを登録してライフサイクル管理
        String resourceKey = "csv_export_" + System.currentTimeMillis();

        try {
            EngineerCSVDAO csvDAO = new EngineerCSVDAO();
            boolean success = csvDAO.exportCSV(targetList, outputFile.getAbsolutePath());

            // 結果表示
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    showExportResult(true, "CSV出力");
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "CSV出力が成功しました: " + outputFile.getAbsolutePath() +
                                    ", 件数: " + targetList.size());
                } else {
                    showExportResult(false, "CSV出力");
                }
                clearExportStatus(panel);
            });

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "CSV出力処理中に例外が発生しました", e);

            SwingUtilities.invokeLater(() -> {
                showExportError("CSV出力", e);
                clearExportStatus(panel);
            });
        } finally {
            // ResourceManagerを通じたリソースクリーンアップ
            try {
                resourceManager.releaseResource(resourceKey);
            } catch (Exception cleanupError) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "CSV出力リソースクリーンアップ中にエラーが発生しました: " + cleanupError.getMessage());
            }
        }
    }

    /**
     * エクスポートステータスの更新
     * 
     * @param panel  対象パネル
     * @param status ステータスメッセージ
     */
    private void updateExportStatus(JPanel panel, String status) {
        SwingUtilities.invokeLater(() -> {
            if (panel instanceof ListPanel listPanel) {
                listPanel.setStatus(status);
            }
        });
    }

    /**
     * エクスポートステータスのクリア
     * 
     * @param panel 対象パネル
     */
    private void clearExportStatus(JPanel panel) {
        SwingUtilities.invokeLater(() -> {
            if (panel instanceof ListPanel listPanel) {
                listPanel.setStatus("");
            }
        });
    }

    /**
     * エクスポート結果の表示
     * 
     * @param success   成功フラグ
     * @param operation 操作名
     */
    private void showExportResult(boolean success, String operation) {
        if (success) {
            JOptionPane.showMessageDialog(listPanel, "出力に成功しました。", "完了", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(listPanel, "出力に失敗しました。アクセス権限などを確認してください。",
                    "エラー", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * エクスポートエラーの表示
     * 
     * @param operation 操作名
     * @param exception 発生した例外
     */
    private void showExportError(String operation, Exception exception) {
        JOptionPane.showMessageDialog(listPanel,
                operation + "中にエラーが発生しました:\n" + exception.getMessage(),
                "エラー", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * ファイル検証結果を保持するクラス
     */
    private static class FileValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public FileValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    // 追加するヘルパーメソッド
    /**
     * キャッシュからListPanelを取得するヘルパーメソッド
     * 
     * @return ListPanelインスタンス（見つからない場合はnull）
     */
    private JPanel getListPanelFromCache() {
        // ScreenTransitionControllerからパネルを取得
        if (screenController != null) {
            // 現在表示中のパネルがListPanelならそれを返す
            JPanel currentPanel = screenController.getCurrentPanel();
            if (currentPanel instanceof ListPanel) {
                return currentPanel;
            }

            // キャッシュからListPanelを検索
            if (screenController.hasPanelInCache("LIST")) {
                return screenController.getPanelFromCache("LIST");
            }
        }
        return null;
    }

    /**
     * CSVファイルのインポート処理
     * ResourceManagerを活用したファイル操作の統合版
     */
    public void handleImportData() {
        // シャットダウン中は処理しない
        if (isShuttingDown.get()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "シャットダウン中のためインポート処理をスキップします");
            return;
        }

        // ResourceManagerを活用したファイル選択ダイアログの表示
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("インポートするCSVファイルを選択");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSVファイル (*.csv)", "csv"));

        // ResourceManagerからデータディレクトリを初期ディレクトリに設定
        try {
            Path dataDir = resourceManager.getDataDirectoryPath();
            if (dataDir != null && dataDir.toFile().exists()) {
                fileChooser.setCurrentDirectory(dataDir.toFile());
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "CSVインポートダイアログの初期ディレクトリを設定: " + dataDir);
            }
        } catch (Exception e) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "初期ディレクトリの設定に失敗しましたが、処理を続行します: " + e.getMessage());
        }

        int result = fileChooser.showOpenDialog(mainFrame.getJFrame());
        if (result != JFileChooser.APPROVE_OPTION) {
            return; // キャンセル
        }

        // 選択されたファイル
        final File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile == null || !selectedFile.exists()) {
            DialogManager.getInstance().showErrorDialog("エラー", "ファイルが見つかりません。");
            return;
        }

        // ResourceManagerを活用したファイル検証
        FileValidationResult validation = validateImportFileWithResourceManager(selectedFile);
        if (!validation.isValid()) {
            DialogManager.getInstance().showErrorDialog("ファイル検証エラー", validation.getErrorMessage());
            return;
        }

        // 現在のパネルを取得（ステータス表示用）
        final JPanel currentPanel = screenController.getCurrentPanel();
        if (currentPanel instanceof ListPanel) {
            ((ListPanel) currentPanel).setStatus("インポート中...");
        }

        // 非同期処理でCSVインポートを実行
        executeCSVImportWithResourceManager(selectedFile, currentPanel);
    }

    /**
     * ResourceManagerを活用したCSVインポート処理の実行
     * 
     * @param selectedFile インポート対象ファイル
     * @param currentPanel ステータス表示用パネル
     */
    private void executeCSVImportWithResourceManager(File selectedFile, JPanel currentPanel) {
        String resourceKey = "csv_import_" + System.currentTimeMillis();

        startAsyncTask("ImportCSV", () -> {
            try {
                // ResourceManagerにリソースを登録
                // 実際のCloseableリソースがある場合はここで登録
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "CSVインポート処理を開始: " + selectedFile.getAbsolutePath());

                // 現在のデータを取得
                List<EngineerDTO> currentEngineers = engineerController.loadEngineers();

                // CSVファイルからデータをインポート
                EngineerCSVDAO csvDao = new EngineerCSVDAO(selectedFile.getAbsolutePath());
                CSVAccessResult importResult = csvDao.readCSV();

                // インポート結果の処理（既存のロジック）
                processImportResultWithResourceManager(importResult, currentEngineers, currentPanel);

            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "CSVファイルのインポート中にエラーが発生しました", e);

                // UI更新はSwingのEDTで実行
                SwingUtilities.invokeLater(() -> {
                    DialogManager.getInstance().showErrorDialog(
                            "インポートエラー",
                            "CSVファイルのインポート中にエラーが発生しました：" + e.getMessage());

                    if (currentPanel instanceof ListPanel) {
                        ((ListPanel) currentPanel).clearStatus();
                    }
                });
            } finally {
                // ResourceManagerを通じたリソースクリーンアップ
                try {
                    resourceManager.releaseResource(resourceKey);
                } catch (Exception cleanupError) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "インポート処理のリソースクリーンアップ中にエラーが発生しました: " + cleanupError.getMessage());
                }
            }
        });
    }

    /**
     * ResourceManagerを活用したインポート結果の処理
     * 
     * @param importResult     インポート結果
     * @param currentEngineers 現在のエンジニアリスト
     * @param currentPanel     ステータス表示用パネル
     */
    private void processImportResultWithResourceManager(CSVAccessResult importResult,
            List<EngineerDTO> currentEngineers, JPanel currentPanel) {

        // インポート結果の取得
        List<EngineerDTO> importedEngineers = importResult.getSuccessData();
        List<EngineerDTO> errorEngineers = importResult.getErrorData();

        // インポート後の総件数が上限を超えるかチェック
        final int MAX_RECORDS = 1000;
        if (!importResult.isOverwriteConfirmed() &&
                importedEngineers.size() + currentEngineers.size() > MAX_RECORDS) {

            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "インポートすると登録件数の上限(" + MAX_RECORDS + "件)を超えます。" +
                            "現在: " + currentEngineers.size() + "件, インポート: " + importedEngineers.size() + "件");

            SwingUtilities.invokeLater(() -> {
                DialogManager.getInstance().showErrorDialog(
                        "インポート制限エラー",
                        "インポートすると登録件数の上限(" + MAX_RECORDS + "件)を超えます。\n" +
                                "現在: " + currentEngineers.size() + "件, インポート: " + importedEngineers.size()
                                + "件\n" +
                                "不要なデータを削除してから再試行してください。");

                if (currentPanel instanceof ListPanel) {
                    ((ListPanel) currentPanel).clearStatus();
                }
            });
            return;
        }

        // 重複IDの処理
        try {
            if (importResult.hasDuplicateIds() && importResult.isOverwriteConfirmed()) {
                for (EngineerDTO engineer : importedEngineers) {
                    engineerController.updateEngineer(engineer);
                }
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "重複IDのエンジニア情報を上書きしました: " + importResult.getDuplicateIds().size() + "件");
            } else {
                for (EngineerDTO engineer : importedEngineers) {
                    if (!currentEngineers.stream().anyMatch(e -> e.getId().equals(engineer.getId()))) {
                        engineerController.addEngineer(engineer);
                    }
                }
            }

            // UI更新の実行
            SwingUtilities.invokeLater(() -> {
                try {
                    List<EngineerDTO> updatedEngineers = engineerController.loadEngineers();

                    if (currentPanel instanceof ListPanel) {
                        ((ListPanel) currentPanel).setEngineerData(updatedEngineers);
                        ((ListPanel) currentPanel).clearStatus();
                    }

                    // 結果ダイアログの表示
                    showImportResultDialog(importedEngineers.size(), errorEngineers.size(), importResult);

                } catch (Exception e) {
                    LogHandler.getInstance().logError(LogType.SYSTEM,
                            "インポート完了後の処理中にエラーが発生しました", e);
                    handleImportError(e, currentPanel);
                }
            });

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "インポートデータの処理中にエラーが発生しました", e);
            SwingUtilities.invokeLater(() -> handleImportError(e, currentPanel));
        }
    }

    /**
     * インポート結果ダイアログの表示
     */
    private void showImportResultDialog(int successCount, int errorCount, CSVAccessResult importResult) {
        StringBuilder message = new StringBuilder();
        message.append("CSVファイルのインポートが完了しました：\n");
        message.append("・インポート成功：").append(successCount).append("件\n");

        if (errorCount > 0) {
            message.append("・エラー：").append(errorCount).append("件\n");
        }

        if (importResult.hasDuplicateIds()) {
            message.append("・重複ID：").append(importResult.getDuplicateIds().size()).append("件\n");
            if (importResult.isOverwriteConfirmed()) {
                message.append("  （上書き済み）");
            } else {
                message.append("  （保持）");
            }
        }

        DialogManager.getInstance().showCompletionDialog("インポート完了", message.toString());
    }

    /**
     * インポートエラーの処理
     */
    private void handleImportError(Exception e, JPanel currentPanel) {
        DialogManager.getInstance().showErrorDialog(
                "エラー",
                "インポート完了後の処理中にエラーが発生しました：" + e.getMessage());

        if (currentPanel instanceof ListPanel) {
            ((ListPanel) currentPanel).clearStatus();
        }
    }

    /**
     * 未知のイベント処理
     * 未定義のイベントを処理します
     * 
     * @param event イベント種別
     * @param data  イベントデータ
     */
    private void handleUnknownEvent(String event, Object data) {
        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "未定義のイベントを検出: " + event);
    }

    /**
     * ResourceManagerを活用したインポートファイルの検証
     * 
     * @param file 検証対象ファイル
     * @return 検証結果
     */
    private FileValidationResult validateImportFileWithResourceManager(File file) {
        try {
            // ファイルの基本的な検証
            if (!file.canRead()) {
                return new FileValidationResult(false, "ファイルを読み込む権限がありません。");
            }

            // CSVファイルの形式チェック（拡張子）
            String fileName = file.getName().toLowerCase();
            if (!fileName.endsWith(".csv")) {
                return new FileValidationResult(false, "CSVファイル（.csv）を選択してください。");
            }

            return new FileValidationResult(true, null);

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "インポートファイル検証中にエラーが発生しました", e);
            return new FileValidationResult(false,
                    "ファイル検証中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * エラー処理
     * 通常のエラーを処理します
     * 
     * @param e 発生した例外
     */
    private void handleError(Exception e) {
        // 通常のエラー処理
        LogHandler.getInstance().logError(LogType.SYSTEM, "エラーが発生しました", e);
        // UI更新はSwingのEDTで実行
        javax.swing.SwingUtilities.invokeLater(() -> {
            // エラーダイアログを表示
            DialogManager.getInstance().showErrorDialog("処理中にエラーが発生しました", e.getMessage());
        });
    }

    /**
     * 致命的なエラー処理
     * アプリケーションを安全に終了します
     * 
     * @param e 発生した例外
     */
    private void handleFatalError(Exception e) {
        LogHandler.getInstance().logError(LogType.SYSTEM, "致命的なエラー", e);
        initiateShutdown();
    }

    /**
     * シャットダウン処理の開始（修正版）
     * AtomicBooleanによる排他制御を活用し、システム全体を統制する
     */
    public void initiateShutdown() {
        // 既にシャットダウン中なら処理しない（原子的操作による排他制御）
        if (isShuttingDown.getAndSet(true)) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "シャットダウンは既に進行中です。重複処理をスキップします");
            return;
        }

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "MainController主導で統合シャットダウンを開始します");

        try {
            // ステップ1: ビジネスロジック層の終了
            performBusinessLayerShutdown();

            // ステップ2: UI層の終了（MainFrameに部分的に委譲）
            performUILayerShutdown();

            // ステップ3: リソース層の統合終了
            performResourceLayerShutdown();

            // ステップ4: 最終終了処理
            performFinalShutdown();

        } catch (Exception e) {
            // エラー時は緊急終了処理を実行
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "統合シャットダウン処理中にエラーが発生しました", e);
            performEmergencyShutdown(e);
        }
    }

    /**
     * ステップ1: ビジネスロジック層の終了処理
     * 実行中のタスクを安全に終了させる
     */
    private void performBusinessLayerShutdown() {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "ステップ1: ビジネスロジック層のシャットダウンを開始");

        try {
            terminateRunningTasks();
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "ビジネスロジック層のシャットダウンが完了しました");
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "ビジネスロジック層の終了処理中にエラーが発生しました", e);
            // エラーがあっても次のステップに進む
        }
    }

    /**
     * ステップ2: UI層の終了処理
     * MainFrameに最小限の処理のみ委譲し、制御権は保持
     */
    private void performUILayerShutdown() {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "ステップ2: UI層のシャットダウンを開始");

        try {
            if (mainFrame != null) {
                // MainFrameには純粋にUI関連のリソース解放のみを委譲
                mainFrame.releaseUIResources();
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "UI層のリソース解放が完了しました");
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "MainFrameへの参照がnullのため、UI層の終了処理をスキップします");
            }
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "UI層の終了処理中にエラーが発生しました", e);
            // エラーがあっても次のステップに進む
        }
    }

    /**
     * ステップ3: リソース層の統合終了処理
     * ResourceManagerとログシステムの統合クリーンアップ
     */
    private void performResourceLayerShutdown() {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "ステップ3: リソース層のシャットダウンを開始");

        // ResourceManagerのクリーンアップ（一度だけ実行）
        performResourceManagerCleanup();

        // 注意: ログシステムのクリーンアップは最後のステップで実行
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "リソース層のシャットダウンが完了しました");
    }

    /**
     * ResourceManagerの統合クリーンアップ
     * 重複実行を防ぎ、一度だけ確実に実行
     */
    private void performResourceManagerCleanup() {
        try {
            if (resourceManager != null && resourceManager.isInitialized()) {
                boolean allResourcesReleased = resourceManager.releaseAllResources();
                if (allResourcesReleased) {
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "ResourceManagerのリソースが正常に解放されました");
                } else {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "一部のResourceManagerリソース解放に失敗しましたが、処理を続行します");
                }
            } else {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "ResourceManagerは未初期化のため、クリーンアップをスキップします");
            }
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "ResourceManagerクリーンアップ中にエラーが発生しました", e);
            // エラーがあっても処理を続行
        }
    }

    /**
     * ステップ4: 最終終了処理
     * ログシステムのクリーンアップとJVM終了
     */
    private void performFinalShutdown() {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "ステップ4: 最終終了処理を開始");

        try {
            // ウィンドウを閉じる（MainFrameが存在する場合）
            if (mainFrame != null) {
                mainFrame.getJFrame().dispose();
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "メインウィンドウを閉じました");
            }

            // 終了処理完了フラグを設定
            isShutdownCompleted.set(true);

            // ログシステムの最終クリーンアップ（一度だけ実行）
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "統合シャットダウン処理が正常に完了しました");
            LogHandler.getInstance().cleanup();

            // JVMの正常終了
            System.exit(0);

        } catch (Exception e) {
            // 最終段階でのエラーは緊急終了で対応
            System.err.println("最終終了処理中にエラーが発生しました: " + e.getMessage());
            performEmergencyShutdown(e);
        }
    }

    /**
     * 緊急終了処理
     * 通常の終了処理でエラーが発生した場合の最後の手段
     */
    private void performEmergencyShutdown(Exception originalError) {
        try {
            // 最小限のクリーンアップを試行
            System.err.println("緊急終了処理を実行します。元のエラー: " + originalError.getMessage());

            // ログシステムが生きていれば最後のログを記録
            try {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "緊急終了処理を実行します", originalError);
                LogHandler.getInstance().cleanup();
            } catch (Exception logError) {
                System.err.println("ログ記録も失敗しました: " + logError.getMessage());
            }

            // 強制終了
            System.exit(1);

        } catch (Exception e) {
            // 最後の最後の手段
            System.err.println("緊急終了処理中にも例外が発生しました: " + e.getMessage());
            Runtime.getRuntime().halt(1); // 即座に強制終了
        }
    }

    /**
     * 実行中のタスクを終了（既存メソッドの改良）
     * より詳細なログ出力と例外処理を追加
     */
    private void terminateRunningTasks() {
        int taskCount = runningTasks.size();
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "実行中の非同期タスクを終了します: " + taskCount + "件");

        if (taskCount == 0) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "終了対象のタスクはありません");
            return;
        }

        // すべての実行中タスクを中断
        for (Map.Entry<String, Thread> entry : runningTasks.entrySet()) {
            String taskId = entry.getKey();
            Thread thread = entry.getValue();

            if (thread != null && thread.isAlive()) {
                try {
                    thread.interrupt();
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "タスクに中断信号を送信しました: " + taskId);
                } catch (Exception e) {
                    LogHandler.getInstance().logError(LogType.SYSTEM,
                            "タスクの中断処理中にエラーが発生しました: " + taskId, e);
                }
            }
        }

        // マップをクリア
        runningTasks.clear();
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "すべてのタスク管理情報をクリアしました");
    }

    /**
     * 実行中タスク数を取得
     * 
     * @return 実行中のタスク数
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }

    /**
     * シャットダウン状態の確認
     * 
     * @return シャットダウン中の場合はtrue
     */
    public boolean isShuttingDown() {
        return isShuttingDown.get();
    }

    /**
     * エンジニアコントローラーを取得
     * 
     * @return エンジニアコントローラー
     */
    public EngineerController getEngineerController() {
        return engineerController;
    }

    /**
     * 画面遷移コントローラーを取得
     * 
     * @return 画面遷移コントローラー
     */
    public ScreenTransitionController getScreenController() {
        return screenController;
    }
}