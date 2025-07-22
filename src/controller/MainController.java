package controller;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import model.CSVAccessResult;
import model.EngineerCSVDAO;
import model.EngineerDTO;
import service.CSVExportService;
import service.EngineerSearchService;
import util.FileUtils;
import util.LogHandler;
import util.LogHandler.LogType;
import util.PerformanceMonitor;
import util.ResourceManager;
import util.Constants.EventType;
import util.Constants.PanelType;
import util.Constants.SystemConstants;
import view.AddPanel;
import view.DetailPanel;
import view.DialogManager;
import view.ListPanel;
import view.MainFrame;

/**
 * アプリケーションのメインコントローラー
 * 画面遷移、イベント処理、スレッド管理、エンジニア検索機能を統括するクラス
 *
 * このクラスは、アプリケーション全体の制御を担当：
 * * 画面遷移の管理
 * エンジニア情報の検索とフィルタリング
 * 非同期タスクの管理
 * イベント処理のディスパッチ
 * リソースの管理と初期化
 * シャットダウン処理の制御
 * *
 * 
 * @author Nakano
 */
public class MainController {

    /** 画面遷移コントローラー */
    private final ScreenTransitionController screenController;

    /** エンジニアデータコントローラー */
    private EngineerController engineerController;

    /** エンジニア検索サービス */
    private EngineerSearchService searchService;

    /** CSV出力サービス */
    private CSVExportService exportService;

    /** リソースマネージャー - ファイル操作の中心的な管理クラス */
    private ResourceManager resourceManager;

    /** メインフレーム */
    private final MainFrame mainFrame;

    /** リストパネル */
    private final ListPanel listPanel;

    /** 実行中非同期タスクの追跡マップ */
    private final ConcurrentMap<String, Thread> runningTasks;

    /** シャットダウン中フラグ */
    private final AtomicBoolean isShuttingDown;

    /** シャットダウン完了フラグ */
    private final AtomicBoolean isShutdownCompleted = new AtomicBoolean(false);

    /** 削除中のエンジニアIDのセット */
    private final Set<String> deletingIds = ConcurrentHashMap.newKeySet();

    /**
     * JFileChooser内のファイル名テキストフィールドを無効化する
     * 
     * @param container
     */
    private void disableFileNameTextField(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JTextField) {
                ((JTextField) c).setEditable(false);
                return;
            } else if (c instanceof Container) {
                disableFileNameTextField((Container) c);
            }
        }
    }

    /**
     * エンジニア検索条件を保持するクラス
     * 検索フォームから入力された各種条件を格納し、検索処理に使用
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
         * * @param id 社員ID検索条件
         * 
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
         * 生年月日に関する検索条件が設定されているかを判定（AND検索対応）
         * * @return 年、月、日のいずれかが設定されている場合はtrue
         */
        public boolean hasDateCriteria() {
            return (year != null && !year.isEmpty() && !"未選択".equals(year)) ||
                    (month != null && !month.isEmpty() && !"未選択".equals(month)) ||
                    (day != null && !day.isEmpty() && !"未選択".equals(day));
        }

        /** ゲッターメソッド群 */
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
     * 検索処理の結果とエラー情報を格納し、呼び出し元に結果を返却
     */
    public static class SearchResult {
        private final List<EngineerDTO> results;
        private final List<String> errors;

        /**
         * 検索結果を初期化するコンストラクタ
         * * @param results 検索にヒットしたエンジニアのリスト
         * 
         * @param errors 検索処理中に発生したエラーメッセージのリスト
         */
        public SearchResult(List<EngineerDTO> results, List<String> errors) {
            this.results = results != null ? results : new ArrayList<>();
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        /**
         * 検索処理でエラーが発生したかを判定
         * * @return エラーが存在する場合はtrue
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
        DialogManager.getInstance();

        // 画面遷移コントローラーにメインコントローラーへの参照を設定
        this.screenController.setMainController(this);

        // MainFrameにMainControllerへの参照を設定（循環参照だが制御された形で）
        this.mainFrame.setMainController(this);

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "メインコントローラを初期化完了");
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
                        "ResourceManager を初期化完了");
            }

            // エンジニアコントローラーの初期化
            engineerController = new EngineerController();

            searchService = new EngineerSearchService(new EngineerCSVDAO());
            exportService = new CSVExportService(new EngineerCSVDAO());

            // 初期画面の表示
            screenController.showPanel("LIST");

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "アプリケーションを初期化完了");

            // ListPanelにMainControllerを設定
            JPanel panel = screenController.getCurrentPanel();
            if (panel instanceof ListPanel) {
                ((ListPanel) panel).setMainController(this);
            }

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "アプリケーションの初期化に失敗", _e);
            handleFatalError(_e);
        }
    }

    /**
     * エンジニア検索処理
     * 検索条件に基づいてエンジニア情報を検索し、結果を返却
     * * @param searchCriteria 検索条件オブジェクト
     * 
     * @return 検索結果オブジェクト（結果リストとエラー情報を含む）
     */
    public SearchResult searchEngineers(SearchCriteria searchCriteria) {
        return searchService.searchEngineers(searchCriteria);
    }

    /**
     * イベントを処理
     * アプリケーション全体のイベントをディスパッチします
     *
     * @param event イベント種別（EventType列挙型または文字列）
     * @param data  イベントデータ（イベント種別に応じたデータ）
     */
    public void handleEvent(EventType eventType, Object data) {
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();

        monitor.startMeasurement("MainController.handleEvent_" + eventType);
        try {
            // シャットダウン中は新しいイベントを処理しない
            if (isShuttingDown.get()) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "シャットダウン中のためイベントを無視します: " + eventType.getEventName());
                return;
            }

            switch (eventType) {
                case REFRESH_VIEW:
                    screenController.refreshView();
                    break;

                case CHANGE_PANEL:
                    // データが文字列の場合はPanelTypeに変換
                    if (data instanceof String) {
                        PanelType panelType = PanelType.fromId((String) data);
                        if (panelType != null) {
                            screenController.showPanel(panelType);
                        } else {
                            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                    "未定義のパネルタイプ: " + data);
                        }
                    } else if (data instanceof PanelType) {
                        screenController.showPanel((PanelType) data);
                    }
                    break;

                case SAVE_DATA:
                    handleSaveData(data);
                    break;

                case LOAD_DATA:
                    handleLoadData();
                    // データ読み込み処理完了後に統計出力
                    monitor.logPerformanceStatistics();
                    break;

                case VIEW_DETAIL:
                    handleViewDetail((String) data);
                    break;

                case SEARCH_ENGINEERS:
                    handleSearchEngineers(data);
                    break;

                case TEMPLATE:
                    handleTemplateExport();
                    break;

                case EXPORT_CSV:
                    handleExportCSV(data);
                    break;

                case IMPORT_CSV:
                    handleImportData();
                    break;

                case DELETE_ENGINEER:
                    handleDeleteEngineer(data);
                    break;

                case SHUTDOWN:
                    initiateShutdown();
                    break;

                default:
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "未処理のイベントタイプ: " + eventType.getEventName());
                    break;
            }

            LogHandler.getInstance().log(
                    Level.INFO, LogType.SYSTEM,
                    String.format("イベントを処理: %s (%s)", eventType.getEventName(), eventType.getDescription()));

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "イベント処理に失敗: " + eventType.getEventName(), _e);
            handleError(_e);
        } finally {
            monitor.endMeasurement("MainController.handleEvent_" + eventType);
        }
    }

    /**
     * 文字列イベント名でイベントを処理
     *
     * @param eventName イベント名文字列
     * @param data      イベントデータ
     */
    public void handleEvent(String eventName, Object data) {
        EventType eventType = EventType.fromEventName(eventName);

        if (eventType != null) {
            handleEvent(eventType, data);
        } else {
            // 未定義のイベントの場合
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "未定義のイベント名: " + eventName);
            handleUnknownEvent(eventName, data);
        }
    }

    /**
     * エンジニア検索処理のイベントハンドラ
     * 検索要求を受け取り、非同期で検索処理を実行
     * * @param data 検索条件オブジェクト（SearchCriteria型）
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
                    } catch (Exception _e) {
                        LogHandler.getInstance().logError(LogType.SYSTEM,
                                "検索結果のUI更新中にエラーが発生", _e);
                    }
                });

            } catch (Exception _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "非同期検索処理中にエラーが発生", _e);

                // エラー時のUI更新
                SwingUtilities.invokeLater(() -> {
                    DialogManager.getInstance().showErrorDialog(
                            "検索エラー", "検索処理中にエラーが発生: " + _e.getMessage());
                });
            }
        });
    }

    /**
     * ListPanel向けの検索結果処理
     * 検索結果をListPanelに適切に反映
     * * @param listPanel 更新対象のListPanel
     * 
     * @param result 検索結果
     */
    private void handleSearchResultForListPanel(ListPanel listPanel, SearchResult result) {
        try {
            if (result.hasErrors()) {
                // 【変更点】バリデーションエラーを統一形式に変更
                DialogManager.getInstance().showInfoDialog("検索結果", "該当するエンジニアは見つかりませんでした。");
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "検索条件にエラーがあります: " + String.join(", ", result.getErrors()));
            } else {
                // 検索結果が正常な場合
                List<EngineerDTO> searchResults = result.getResults();

                if (searchResults.isEmpty()) {
                    // 検索結果が空の場合は通知（既存と同じ）
                    DialogManager.getInstance().showInfoDialog("検索結果", "該当するエンジニアは見つかりませんでした。");
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "検索結果が0件でした");
                } else {
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            String.format("検索結果を表示します: %d件", searchResults.size()));
                }

                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "検索結果をListPanelに反映しました");
            }
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "検索結果の処理中にエラーが発生", _e);
            DialogManager.getInstance().showErrorDialog(
                    "表示エラー", "検索結果の表示中にエラーが発生");
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
            } catch (Exception _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "非同期タスクの実行中にエラーが発生: " + taskId, _e);
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
     * エンジニア情報の削除処理を非同期で実行
     * * @param data 削除対象のエンジニアDTOリスト
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

                            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "削除後のデータを再読み込み");

                        } catch (Exception _e) {
                            LogHandler.getInstance().logError(LogType.SYSTEM, "削除後の再読み込みに失敗", _e);
                        }

                    });

                });
            });
        });
    }

    /**
     * 最新のエンジニア一覧を取得
     *
     * @return エンジニアDTOのリスト
     */
    public List<EngineerDTO> getEngineerList() {
        if (engineerController == null) {
            engineerController = new EngineerController();
        }
        return engineerController.loadEngineers();
    }

    /**
     * 最新のエンジニア一覧を取得
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
     * * このメソッドはエンジニア情報の保存処理を非同期的に実行します。単一エンジニアの追加と
     * 複数エンジニアの一括更新に対応しています。保存処理の成功後は、UIを適切に更新し、
     * 必要に応じてダイアログ表示や画面遷移を行います。
     * * 主な処理の流れ：
     * * データ型の判定（単一エンジニアまたはエンジニアリスト）
     * コンテキスト情報（元画面の参照）の取得と保持
     * 非同期タスクとしての保存処理実行
     * 成功時のUI更新（リスト更新、ダイアログ表示など）
     * エラー時の例外処理と状態復元
     * * このメソッドは特に次の点に注意して実装されています：
     * * @param data
     * 保存するデータ（{@link EngineerDTO}または{@link List}&lt;{@link EngineerDTO}&gt;）
     */
    private void handleSaveData(Object data) {
        // シャットダウン中は処理しない
        if (isShuttingDown.get()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "シャットダウン中のため保存処理をスキップ");
            return;
        }

        // エンジニア情報の追加処理（単一オブジェクト）
        if (data instanceof EngineerDTO) {
            try {
                EngineerDTO engineer = (EngineerDTO) data;
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "エンジニア情報の保存を開始: ID=" + engineer.getId());

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
                            } catch (EngineerController.TooManyRecordsException _e) {
                                // 登録上限エラーの処理
                                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                        "登録件数の上限に達したため、エンジニア情報の追加に失敗: " + _e.getMessage());

                                // UI更新はSwingのEDTで実行
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    // エラーダイアログ表示
                                    DialogManager.getInstance().showErrorDialog(
                                            "登録制限エラー",
                                            "登録件数の上限(" + SystemConstants.MAX_ENGINEER_RECORDS
                                                    + "件)に達しています。これ以上登録できません。\n" +
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
                                                "リストパネルにエンジニアデータを追加: " + engineer.getId());
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
                                                "対応するパネルが見つからないため完了処理をスキップ: " +
                                                        "sourcePanel="
                                                        + (sourcePanel != null ? sourcePanel.getClass().getName()
                                                                : "null")
                                                        +
                                                        ", sourcePanelType=" + sourcePanelType);

                                        // 代替手段としてダイアログを直接表示
                                        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                                "代替手段としてダイアログを直接表示");
                                        DialogManager.getInstance().showCompletionDialog(
                                                "処理完了",
                                                "エンジニア情報を保存: ID=" + engineer.getId() + ", 名前="
                                                        + engineer.getName());
                                    }

                                    // 画面更新
                                    screenController.refreshView();
                                } catch (Exception _e) {
                                    LogHandler.getInstance().logError(LogType.SYSTEM,
                                            "保存完了後の処理中にエラーが発生: " + engineer.getId(), _e);

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
                                                "エラー処理中にも例外が発生", ex);
                                    }
                                }
                            });
                        } else {
                            // 保存失敗時の処理
                            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                    "エンジニア情報の保存に失敗: ID=" + engineer.getId());

                            javax.swing.SwingUtilities.invokeLater(() -> {
                                if (finalAddPanel != null) {
                                    finalAddPanel.setProcessing(false);
                                    DialogManager.getInstance().showErrorDialog(
                                            "保存エラー", "エンジニア情報の保存に失敗");
                                }
                                if (finalDetailPanel != null) {
                                    finalDetailPanel.setProcessing(false);
                                    DialogManager.getInstance().showErrorDialog(
                                            "更新エラー", "エンジニア情報の更新に失敗");
                                }
                            });
                        }
                    } catch (Exception _e) {
                        LogHandler.getInstance().logError(LogType.SYSTEM,
                                "エンジニア情報の保存処理中にエラーが発生: " + engineer.getId(), _e);

                        // エラー時に処理中状態を解除
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            if (finalAddPanel != null) {
                                finalAddPanel.setProcessing(false);
                                DialogManager.getInstance().showErrorDialog(
                                        "保存エラー", "エンジニア情報の保存中にエラーが発生: " + _e.getMessage());
                            }
                            if (finalDetailPanel != null) {
                                finalDetailPanel.setProcessing(false);
                                DialogManager.getInstance().showErrorDialog(
                                        "更新エラー", "エンジニア情報の更新中にエラーが発生: " + _e.getMessage());
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
                                            "AddPanelの処理状態をリセット");
                                }
                                if (finalDetailPanel != null) {
                                    // 処理状態の強制リセット（念のため）
                                    finalDetailPanel.setProcessing(false);
                                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                            "DetailPanelの処理状態をリセット");
                                }
                            } catch (Exception _e) {
                                LogHandler.getInstance().logError(LogType.SYSTEM,
                                        "処理状態リセット中にエラーが発生", _e);
                            }
                        });
                    }
                });

            } catch (ClassCastException _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "保存データの型が不正です", _e);
            } catch (Exception _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の保存前処理中にエラーが発生", _e);
            }
        }
        // エンジニア情報の一括保存処理（リスト）
        else if (data instanceof List<?>) {
            try {
                @SuppressWarnings("unchecked")
                List<EngineerDTO> engineers = (List<EngineerDTO>) data;
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "エンジニア情報の一括保存を開始: " + engineers.size() + "件");

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
                                    "エンジニア情報の一括保存に成功: " + engineers.size() + "件");

                            // UI更新はSwingのEDTで実行
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                try {
                                    // 画面更新
                                    screenController.refreshView();

                                    // 保存完了ダイアログを表示
                                    DialogManager.getInstance().showCompletionDialog(
                                            "保存完了", engineers.size() + "件のエンジニア情報を保存");
                                } catch (Exception _e) {
                                    LogHandler.getInstance().logError(LogType.SYSTEM,
                                            "一括保存後の処理中にエラーが発生", _e);
                                }
                            });
                        } else {
                            // 保存失敗時の処理
                            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                    "エンジニア情報の一括保存に失敗");

                            javax.swing.SwingUtilities.invokeLater(() -> {
                                DialogManager.getInstance().showErrorDialog(
                                        "保存エラー", "エンジニア情報の一括保存に失敗");
                            });
                        }
                    } catch (Exception _e) {
                        LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の一括保存処理中にエラーが発生", _e);

                        javax.swing.SwingUtilities.invokeLater(() -> {
                            DialogManager.getInstance().showErrorDialog(
                                    "保存エラー", "エンジニア情報の一括保存中にエラーが発生: " + _e.getMessage());
                        });
                    }
                });

            } catch (ClassCastException _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "保存データの型が不正です", _e);
            } catch (Exception _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の一括保存前処理中にエラーが発生", _e);
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
                if (engineers.size() > SystemConstants.MAX_ENGINEER_RECORDS) {
                    LogHandler.getInstance().log(Level.SEVERE, LogType.SYSTEM,
                            "登録されているエンジニアデータが上限(" + SystemConstants.MAX_ENGINEER_RECORDS + "件)を超えています: "
                                    + engineers.size() + "件");

                    // UI更新はSwingのEDTで実行
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        // エラーダイアログを表示
                        DialogManager.getInstance().showErrorDialog(
                                "データ制限エラー",
                                "登録されているエンジニアデータが上限(" + SystemConstants.MAX_ENGINEER_RECORDS + "件)を超えています: "
                                        + engineers.size() + "件\n" +
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

            } catch (Exception _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "データ読み込みに失敗", _e);

                // エラーメッセージ表示
                javax.swing.SwingUtilities.invokeLater(() -> {
                    DialogManager.getInstance().showErrorDialog(
                            "読み込みエラー",
                            "データ読み込みに失敗: " + _e.getMessage());
                });
            }
        });
    }

    /**
     * 詳細表示処理
     * エンジニアIDを指定して詳細画面に遷移
     * * @param engineerId 表示するエンジニアID
     */
    private void handleViewDetail(String engineerId) {
        // 削除中のIDは遷移禁止
        if (deletingIds.contains(engineerId)) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "削除中のため詳細画面に遷移しません: ID=" + engineerId);
            return;
        }
        // ListPanelから削除中フラグを取得（この行だけ追加）
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

                        // 更新ボタンを削除中なら無効化（この行だけ追加）
                        detailPanel.setUpdateButtonEnabled(!isCurrentlyDeleting);

                        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                "エンジニア詳細を表示: ID=" + engineerId);
                    } else {
                        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                "DetailPanelへの参照取得に失敗");
                    }
                });
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "指定されたIDのエンジニアが見つかりません: " + engineerId);

                // エラーダイアログを表示
                DialogManager.getInstance().showErrorDialog(
                        "エラー", "指定されたIDのエンジニアが見つかりません: " + engineerId);
            }
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "エンジニア詳細表示中にエラーが発生: ID=" + engineerId, _e);

            // エラーダイアログを表示
            DialogManager.getInstance().showErrorDialog(
                    "エラー", "エンジニア詳細表示中にエラーが発生: " + _e.getMessage());
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
            File selectedFile = exportTemplateFileWithResourceManager("エンジニア情報テンプレート.csv");
            if (selectedFile == null) {
                return;
            }

            // ビジネスロジック：テンプレート出力処理の実行
            executeTemplateExportWithResourceManager(selectedFile);
        });
    }

    /**
     * テンプレート出力確認処理
     * * @return 出力許可の場合true
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
     * 重複ファイル名の自動回避機能を追加
     * * @param defaultFileName デフォルトファイル名
     * 
     * @return 選択されたファイル、キャンセル時はnull
     */
    private File exportTemplateFileWithResourceManager(String defaultFileName) {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        disableFileNameTextField(fileChooser);
        fileChooser.setDialogTitle("テンプレートCSV出力先");
        fileChooser.setSelectedFile(new File(defaultFileName));

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "出力先選択画面を表示");
        int userSelection = fileChooser.showSaveDialog(listPanel);

        if (userSelection != JFileChooser.APPROVE_OPTION) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "テンプレート出力がキャンセルされました");
            return null;
        }

        File selectedFile = fileChooser.getSelectedFile();

        // 拡張子の自動付加
        if (!selectedFile.getName().toLowerCase().endsWith(".csv")) {
            selectedFile = new File(selectedFile.getAbsolutePath() + ".csv");
        }

        // ResourceManagerを活用したファイル検証
        FileValidationResult validation = validateFileSelectionWithResourceManager(selectedFile);
        if (!validation.isValid()) {
            DialogManager.getInstance().showErrorDialog("エラー", validation.getErrorMessage());
            return null;
        }

        // 重複ファイル名の自動回避処理
        File finalFile = FileUtils.generateUniqueFileName(selectedFile);

        if (!finalFile.equals(selectedFile)) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "重複ファイルを検出し、自動的に名前を変更: " + selectedFile.getName() + " → " + finalFile.getName());
        }

        return finalFile;
    }

    /**
     * ResourceManagerを活用したテンプレート出力処理の実行
     * リソース管理の統合とエラーハンドリングの改善
     * * @param outputFile 出力先ファイル
     */
    private void executeTemplateExportWithResourceManager(File selectedFile) {
        String resourceKey = "template_export_" + System.currentTimeMillis();

        try (FileOutputStream fos = new FileOutputStream(selectedFile)) {
            resourceManager.registerResource(resourceKey, fos);

            boolean success = exportService.exportTemplate(selectedFile);

            SwingUtilities.invokeLater(() -> {
                if (success) {
                    DialogManager.getInstance().showCompletionDialog(
                            "テンプレート出力が完了しました。",
                            () -> LogHandler.getInstance().log(Level.INFO, LogType.UI,
                                    "テンプレート出力完了ダイアログを閉じました"));
                } else {
                    DialogManager.getInstance().showErrorDialog("出力エラー",
                            "テンプレート出力に失敗しました。");
                }
            });

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "テンプレート出力実行中にエラー発生", _e);
            SwingUtilities.invokeLater(() -> {
                DialogManager.getInstance().showErrorDialog("システムエラー",
                        "出力中にエラーが発生しました。");
            });
        } finally {
            resourceManager.releaseResource(resourceKey);
        }
    }

    /**
     * エンジニア情報のCSV出力を非同期で実行
     * ResourceManagerを活用したファイル操作の統合版
     * * @param data CSV出力対象のエンジニアDTOリスト
     */
    @SuppressWarnings("unchecked")
    public void handleExportCSV(Object data) {
        List<EngineerDTO> targetList = (List<EngineerDTO>) data;

        startAsyncTask("export_engineers", () -> {
            // ResourceManagerを活用したファイル選択とバリデーション
            File selectedFile = exportFileWithResourceManager("エンジニア情報-" + LocalDate.now() + ".csv");
            if (selectedFile == null) {
                return; // ユーザーがキャンセルした場合
            }

            // ビジネスロジック：CSV出力処理の実行
            executeCSVExportWithResourceManager(targetList, selectedFile);
        });
    }

    /**
     * ResourceManagerを活用したCSV出力用ファイル選択処理
     * 重複ファイル名の自動回避機能を追加
     * * @param defaultFileName デフォルトファイル名
     * 
     * @return 選択されたファイル、キャンセル時はnull
     */
    private File exportFileWithResourceManager(String defaultFileName) {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        disableFileNameTextField(fileChooser);
        fileChooser.setDialogTitle("CSVファイルの保存先");
        fileChooser.setSelectedFile(new File(defaultFileName));

        int result = fileChooser.showSaveDialog(listPanel);
        if (result != JFileChooser.APPROVE_OPTION) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "CSV出力がキャンセルされました");
            return null;
        }

        File selectedFile = fileChooser.getSelectedFile();

        // 拡張子の自動付加
        if (!selectedFile.getName().toLowerCase().endsWith(".csv")) {
            selectedFile = new File(selectedFile.getAbsolutePath() + ".csv");
        }

        // ResourceManagerを活用したファイル名妥当性検証
        FileValidationResult validation = validateFileSelectionWithResourceManager(selectedFile);
        if (!validation.isValid()) {
            DialogManager.getInstance().showErrorDialog("エラー", validation.getErrorMessage());
            return null;
        }

        // 重複ファイル名の自動回避処理
        File finalFile = FileUtils.generateUniqueFileName(selectedFile);

        if (!finalFile.equals(selectedFile)) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "重複ファイルを検出し、自動的に名前を変更: " + selectedFile.getName() + " → " + finalFile.getName());
        }

        return finalFile;
    }

    /**
     * ResourceManagerを活用したファイル検証
     * 統一されたバリデーションロジックとエラーハンドリング
     * * @param file 検証対象ファイル
     * 
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
            // データディレクトリの存在確認と作成
            if (dataDir != null && !dataDir.toFile().exists()) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "データディレクトリが存在しないため、作成を試みます: " + dataDir);
                // ResourceManager経由でディレクトリ作成を試行できます
            }

            return new FileValidationResult(true, null);

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "ファイル検証中にエラーが発生", _e);
            return new FileValidationResult(false,
                    "ファイル検証中にエラーが発生: " + _e.getMessage());
        }
    }

    /**
     * ResourceManagerを活用したCSV出力処理の実行
     * リソース管理とエラーハンドリングの統合
     * * @param targetList 出力対象データ
     * 
     * @param outputFile 出力先ファイル
     */
    private void executeCSVExportWithResourceManager(List<EngineerDTO> targetList, File selectedFile) {
        JPanel panel = screenController.getCurrentPanel();
        String resourceKey = "csv_export_" + System.currentTimeMillis();

        try (FileOutputStream fos = new FileOutputStream(selectedFile)) {
            resourceManager.registerResource(resourceKey, fos);

            // 修正: exportTemplate() から exportCSV() に変更
            boolean success = exportService.exportCSV(targetList, selectedFile);

            SwingUtilities.invokeLater(() -> {
                if (success) {
                    DialogManager.getInstance().showCompletionDialog(
                            "CSV出力が完了しました。",
                            () -> LogHandler.getInstance().log(Level.INFO, LogType.UI,
                                    "CSV出力完了ダイアログを閉じました"));

                    // 出力完了後にボタン状態を復元
                    clearExportStatus(panel);
                } else {
                    clearExportStatus(panel);
                    DialogManager.getInstance().showErrorDialog("出力エラー",
                            "CSV出力に失敗しました。");
                }
            });

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "CSV出力実行中にエラー発生", _e);
            SwingUtilities.invokeLater(() -> {
                DialogManager.getInstance().showErrorDialog("システムエラー",
                        "出力中にエラーが発生しました。");
                clearExportStatus(panel);
            });
        } finally {
            resourceManager.releaseResource(resourceKey);
        }
    }

    /**
     * エクスポートステータスのクリア
     * * @param panel 対象パネル
     */
    private void clearExportStatus(JPanel panel) {
        SwingUtilities.invokeLater(() -> {
            if (panel instanceof ListPanel listPanel) {
                listPanel.setStatus("");
                // ボタン状態を有効化
                listPanel.setButtonsEnabled(true);
            }
            // 登録ボタンも有効化
            screenController.setRegisterButtonEnabled(true);
        });
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

    /**
     * キャッシュからListPanelを取得するヘルパーメソッド
     * * @return ListPanelインスタンス（見つからない場合はnull）
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
        if (isShuttingDown.get()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "シャットダウン中のためインポート処理をスキップ");
            return;
        }

        // 現在のパネルを取得（状態管理用）
        final JPanel currentPanel = screenController.getCurrentPanel();

        // ListPanelの場合、インポート処理中状態を設定
        if (currentPanel instanceof ListPanel) {
            ((ListPanel) currentPanel).setImportProcessing(true);
        }

        // ファイル選択ダイアログの表示
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        fileChooser.setDialogTitle("インポートするCSVファイルを選択");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSVファイル (*.csv)", "csv"));

        int result = fileChooser.showOpenDialog(mainFrame.getJFrame());
        if (result != JFileChooser.APPROVE_OPTION) {
            // キャンセル時は状態をリセット
            if (currentPanel instanceof ListPanel) {
                ((ListPanel) currentPanel).setImportProcessing(false);
            }
            return;
        }

        final File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile == null || !selectedFile.exists()) {
            DialogManager.getInstance().showErrorDialog("エラー", "ファイルが見つかりません。");
            if (currentPanel instanceof ListPanel) {
                ((ListPanel) currentPanel).setImportProcessing(false);
            }
            return;
        }

        FileValidationResult validation = validateImportFileWithResourceManager(selectedFile);
        if (!validation.isValid()) {
            DialogManager.getInstance().showErrorDialog("ファイル検証エラー", validation.getErrorMessage());
            if (currentPanel instanceof ListPanel) {
                ((ListPanel) currentPanel).setImportProcessing(false);
            }
            return;
        }

        // ファイル名確認ダイアログの表示
        String fileName = selectedFile.getName();
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "ファイル選択完了: " + fileName + " - 読み込み確認ダイアログを表示");

        int confirmImport = JOptionPane.showConfirmDialog(
                mainFrame.getJFrame(),
                "選択したファイル「" + fileName + "」を読み込みますか？",
                "ファイル読み込み確認",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirmImport != JOptionPane.YES_OPTION) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "ユーザーがファイル読み込みをキャンセルしました: " + fileName);
            // 処理中状態をリセット
            if (currentPanel instanceof ListPanel) {
                ((ListPanel) currentPanel).setImportProcessing(false);
            }
            return;
        }

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "ファイル読み込みが確認されました: " + fileName);

        // 全パネルの初期化を確認
        screenController.ensureAllPanelsInitialized();

        // 取込処理開始時にAddPanelとDetailPanelのボタンを無効化
        screenController.setRegisterButtonEnabled(false);

        // 非同期処理でCSVインポートを実行
        executeCSVImportWithResourceManager(selectedFile, currentPanel);
    }

    /**
     * ResourceManagerを活用したCSVインポート処理の実行
     * * @param selectedFile インポート対象ファイル
     * 
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

            } catch (Exception _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "CSVファイルのインポート中にエラーが発生", _e);

                // UI更新はSwingのEDTで実行
                SwingUtilities.invokeLater(() -> {
                    DialogManager.getInstance().showErrorDialog(
                            "インポートエラー",
                            "CSVファイルのインポート中にエラーが発生：" + _e.getMessage());

                    if (currentPanel instanceof ListPanel) {
                        ((ListPanel) currentPanel).clearStatus();
                        ((ListPanel) currentPanel).setImportProcessing(false);
                    }

                    // エラー時もボタンを有効化
                    screenController.setRegisterButtonEnabled(true);
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "取込エラー：AddPanel/DetailPanelのボタンを有効化");
                });
            } finally {
                // ResourceManagerを通じたリソースクリーンアップ
                try {
                    resourceManager.releaseResource(resourceKey);
                } catch (Exception cleanupError) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "インポート処理のリソースクリーンアップ中にエラーが発生: " + cleanupError.getMessage());
                }
            }
        });
    }

    /**
     * ResourceManagerを活用したインポート結果の処理
     * * * @param importResult インポート結果
     * 
     * @param currentEngineers 現在のエンジニアリスト
     * @param currentPanel     ステータス表示用パネル
     */
    private void processImportResultWithResourceManager(CSVAccessResult importResult,
            List<EngineerDTO> currentEngineers, JPanel currentPanel) {

        List<EngineerDTO> importedEngineers = importResult.getSuccessData();
        List<EngineerDTO> errorEngineers = importResult.getErrorData();

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                String.format("インポート処理開始: 取込データ=%d件, 現在データ=%d件, エラーデータ=%d件",
                        importedEngineers.size(), currentEngineers.size(), errorEngineers.size()));

        // エラーデータがある場合の詳細表示
        if (!errorEngineers.isEmpty()) {
            showImportErrorDetails(errorEngineers, currentPanel);
        }

        // 致命的エラーの場合は処理を中断
        if (importResult.isFatalError()) {
            SwingUtilities.invokeLater(() -> {
                DialogManager.getInstance().showErrorDialog(
                        "インポートエラー",
                        "CSV読み込み中に致命的なエラーが発生:\n" + importResult.getErrorMessage());
                if (currentPanel instanceof ListPanel) {
                    ((ListPanel) currentPanel).setImportProcessing(false);
                }
                // 致命的エラー時もボタンを有効化
                screenController.setRegisterButtonEnabled(true);
            });
            return;
        }

        // 【修正】ファイル内重複IDチェックを最初に行う
        List<String> internalDuplicateIds = importResult.checkInternalDuplicateIds();
        if (!internalDuplicateIds.isEmpty()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "ファイル内重複IDを検出: " + internalDuplicateIds.size() + "件");

            SwingUtilities.invokeLater(() -> {
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("エラー：取り込みファイル内に重複したIDが存在します。\n\n");
                errorMessage.append("重複しているID:\n");
                for (String duplicateId : internalDuplicateIds) {
                    errorMessage.append("  - ").append(duplicateId).append("\n");
                }
                errorMessage.append("\nファイルを修正してから、再度インポートしてください。");

                DialogManager.getInstance().showErrorDialog(
                        "ファイル内重複IDエラー",
                        errorMessage.toString());

                if (currentPanel instanceof ListPanel) {
                    ((ListPanel) currentPanel).setImportProcessing(false);
                }
                // 重複エラー時もボタンを有効化
                screenController.setRegisterButtonEnabled(true);
            });
            return; // 処理を中断
        }

        // 既存データとの重複チェックを実行
        performDuplicateCheckWithExistingData(importResult, currentEngineers);

        // ユーザーの選択を先に行う
        // 重複IDがある場合は、先にユーザーに上書きするかどうかを確認
        if (importResult.hasDuplicateIds()) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "重複IDが検出されました: " + importResult.getDuplicateIds().size() + "件");
            handleDuplicateIds(importResult);
        } else {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "重複IDは検出されませんでした");
        }

        // ユーザーの選択後に、再度分析処理を実行
        try {
            // ユーザーの選択（上書き or スキップ）が反映された状態で分析を実行
            importResult.performImportAnalysis(currentEngineers);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "インポート分析完了: " + importResult.getAnalysisDetailInfo());

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "インポート分析中にエラーが発生", _e);
            SwingUtilities.invokeLater(() -> {
                DialogManager.getInstance().showErrorDialog(
                        "分析エラー", "インポートデータの分析中にエラーが発生: " + _e.getMessage());
                if (currentPanel instanceof ListPanel) {
                    ((ListPanel) currentPanel).setImportProcessing(false);
                }
            });
            return;
        }

        // 上限チェック (分析後に行う)
        if (importResult.willExceedLimit()) {
            // ...
            return;
        }

        // データ更新処理 (分析結果に基づいて実行)
        try {
            performDataUpdateWithAnalysis(importResult, importedEngineers, currentEngineers);

            SwingUtilities.invokeLater(() -> {
                try {
                    updateUIAfterImportWithAnalysis(importResult, currentPanel);
                } catch (Exception _e) {
                    LogHandler.getInstance().logError(LogType.SYSTEM,
                            "インポート完了後のUI更新中にエラーが発生", _e);
                    handleImportError(_e, currentPanel);
                }
            });

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "インポートデータの処理中にエラーが発生", _e);
            SwingUtilities.invokeLater(() -> handleImportError(_e, currentPanel));
        }
    }

    /**
     * 既存データとの重複チェックを実行
     * インポートデータと既存エンジニアデータを比較し、重複IDを検出
     * * @param importResult インポート結果オブジェクト
     * 
     * @param currentEngineers 現在の既存エンジニアデータ
     */
    private void performDuplicateCheckWithExistingData(CSVAccessResult importResult,
            List<EngineerDTO> currentEngineers) {

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "既存データとの重複チェックを開始");

        // 既存エンジニアのIDセットを作成（高速検索のため）
        Set<String> existingIds = currentEngineers.stream()
                .map(EngineerDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "既存エンジニアID数: " + existingIds.size());

        // インポートデータから重複IDを検出
        List<String> duplicateIds = new ArrayList<>();
        List<EngineerDTO> successData = importResult.getSuccessData();

        for (EngineerDTO engineer : successData) {
            String engineerId = engineer.getId();
            if (engineerId != null && existingIds.contains(engineerId)) {
                if (!duplicateIds.contains(engineerId)) { // 重複排除
                    duplicateIds.add(engineerId);
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "重複ID検出: " + engineerId);
                }
            }
        }

        // 重複IDをCSVAccessResultに設定
        if (!duplicateIds.isEmpty()) {
            importResult.addDuplicateIds(duplicateIds);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "重複ID検出完了: " + duplicateIds.size() + "件 - " + duplicateIds);
        } else {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "重複IDは検出されませんでした");
        }
    }

    /**
     * エラーデータの詳細表示
     * エラーになったエンジニアデータの詳細情報を表示
     */
    private void showImportErrorDetails(List<EngineerDTO> errorEngineers, JPanel currentPanel) {
        SwingUtilities.invokeLater(() -> {
            List<String> errorDetails = new ArrayList<>();
            errorDetails.add("以下のデータに問題が見つかりました：");
            errorDetails.add("");

            for (EngineerDTO errorEngineer : errorEngineers) {
                StringBuilder detail = new StringBuilder();

                // エラー行の基本情報
                detail.append("【");
                if (errorEngineer.getId() != null && !errorEngineer.getId().isEmpty()) {
                    detail.append("ID: ").append(errorEngineer.getId());
                } else {
                    detail.append("ID: 未設定");
                }

                if (errorEngineer.getName() != null && !errorEngineer.getName().isEmpty()) {
                    detail.append(", 氏名: ").append(errorEngineer.getName());
                }
                detail.append("】");

                // エラー内容（備考欄に格納されている）
                if (errorEngineer.getNote() != null) {
                    detail.append("\n  エラー: ").append(errorEngineer.getNote());
                }

                errorDetails.add(detail.toString());
                errorDetails.add(""); // 空行
            }

            errorDetails.add("これらのデータはスキップされました。");
            errorDetails.add("修正後、再度インポートしてください。");

            // スクロール可能なダイアログで表示
            boolean understood = DialogManager.getInstance().showScrollableListDialog(
                    "インポートエラー詳細",
                    "一部のデータに問題がありました",
                    errorDetails);

            if (understood) {
                LogHandler.getInstance().log(Level.INFO, LogType.UI,
                        "ユーザーがエラー詳細を確認しました");
            }
        });
    }

    /**
     * 分析結果に基づくデータ更新処理
     * CSVAccessResultの分析機能を活用して効率的なデータ更新を実行
     * * @param importResult 拡張されたCSVAccessResult
     * 
     * @param importedEngineers 取り込み予定のエンジニアデータ
     * @param currentEngineers  現在のエンジニアデータ
     */
    private void performDataUpdateWithAnalysis(CSVAccessResult importResult,
            List<EngineerDTO> importedEngineers,
            List<EngineerDTO> currentEngineers) {

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "データ更新処理開始: " + importResult.getAnalysisDetailInfo());

        // 現在のデータIDセットを作成（高速な重複チェックのため）
        Set<String> currentIds = currentEngineers.stream()
                .map(EngineerDTO::getId)
                .collect(Collectors.toSet());

        // 実際の処理件数をカウント（検証用）
        int actualUpdatedCount = 0;
        int actualAddedCount = 0;
        int actualSkippedCount = 0;

        // 各インポートデータを分析結果に基づいて処理
        for (EngineerDTO engineer : importedEngineers) {
            String engineerId = engineer.getId();

            try {
                if (currentIds.contains(engineerId)) {
                    // 重複IDが存在する場合の処理
                    if (importResult.isOverwriteConfirmed()) {
                        // 上書き処理: 既存データを更新
                        engineerController.updateEngineer(engineer);
                        actualUpdatedCount++;
                        LogHandler.getInstance().log(Level.FINE, LogType.SYSTEM,
                                "データ上書き完了: " + engineerId);
                    } else {
                        // スキップ処理: 何もしない
                        actualSkippedCount++;
                        LogHandler.getInstance().log(Level.FINE, LogType.SYSTEM,
                                "データスキップ: " + engineerId);
                    }
                } else {
                    // 新規ID: 新しいデータとして追加
                    engineerController.addEngineer(engineer);
                    actualAddedCount++;
                    LogHandler.getInstance().log(Level.FINE, LogType.SYSTEM,
                            "データ新規追加完了: " + engineerId);
                }
            } catch (Exception _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "個別データ更新エラー: " + engineerId, _e);
                // 個別のエラーは記録するが、全体の処理は継続
                // この設計により、一部のデータに問題があっても全体の処理が停止しない
            }
        }

        // 実際の処理結果と分析結果の整合性を検証
        validateProcessingResults(importResult, actualAddedCount, actualUpdatedCount, actualSkippedCount);

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                String.format("データ更新完了: 追加=%d件, 更新=%d件, スキップ=%d件",
                        actualAddedCount, actualUpdatedCount, actualSkippedCount));
    }

    /**
     * 処理結果と分析結果の整合性検証
     * 予想された結果と実際の結果が一致することを確認
     * * @param importResult 分析済みのCSVAccessResult
     * 
     * @param actualAddedCount   実際に追加されたデータ件数
     * @param actualUpdatedCount 実際に更新されたデータ件数
     * @param actualSkippedCount 実際にスキップされたデータ件数
     */
    private void validateProcessingResults(CSVAccessResult importResult,
            int actualAddedCount, int actualUpdatedCount, int actualSkippedCount) {

        // 分析結果と実際の結果を比較
        boolean isConsistent = true;
        StringBuilder inconsistencyReport = new StringBuilder();

        if (actualAddedCount != importResult.getCalculatedNewDataCount()) {
            isConsistent = false;
            inconsistencyReport.append(String.format(
                    "新規追加件数の不一致: 予想=%d, 実際=%d; ",
                    importResult.getCalculatedNewDataCount(), actualAddedCount));
        }

        if (actualUpdatedCount != importResult.getCalculatedOverwriteDataCount()) {
            isConsistent = false;
            inconsistencyReport.append(String.format(
                    "更新件数の不一致: 予想=%d, 実際=%d; ",
                    importResult.getCalculatedOverwriteDataCount(), actualUpdatedCount));
        }

        if (actualSkippedCount != importResult.getCalculatedSkipDataCount()) {
            isConsistent = false;
            inconsistencyReport.append(String.format(
                    "スキップ件数の不一致: 予想=%d, 実際=%d; ",
                    importResult.getCalculatedSkipDataCount(), actualSkippedCount));
        }

        if (!isConsistent) {
            // 不整合がある場合は警告ログを出力
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "処理結果と分析結果に不整合があります: " + inconsistencyReport.toString());
        } else {
            // 整合性が確認された場合
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "処理結果と分析結果の整合性を確認しました");
        }
    }

    /**
     * インポート完了後のUI更新
     * CSVAccessResultの拡張機能を使用して、詳細な結果表示を実行
     * * @param importResult 分析済みのCSVAccessResult
     * 
     * @param currentPanel ステータス表示用パネル
     */
    private void updateUIAfterImportWithAnalysis(CSVAccessResult importResult, JPanel currentPanel) {
        // 最新データでListPanelを更新
        List<EngineerDTO> updatedEngineers = engineerController.loadEngineers();

        if (currentPanel instanceof ListPanel) {
            ListPanel listPanel = (ListPanel) currentPanel;
            listPanel.setEngineerData(updatedEngineers);

            // インポート処理完了状態をリセット
            listPanel.setImportProcessing(false);
        }

        // 全パネルの初期化を確認
        screenController.ensureAllPanelsInitialized();

        // 取込処理完了時にAddPanelとDetailPanelのボタンを有効化
        screenController.setRegisterButtonEnabled(true);

        // 詳細な完了メッセージを表示
        showDetailedImportResultWithAnalysis(importResult);

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "インポート完了後のUI更新が正常に完了しました");
    }

    /**
     * 分析機能を活用した詳細なインポート結果表示
     * CSVAccessResultの拡張機能により、ユーザーフレンドリーな結果表示を実現
     * * @param importResult 分析済みのCSVAccessResult
     */
    private void showDetailedImportResultWithAnalysis(CSVAccessResult importResult) {
        try {
            // CSVAccessResultの新機能を使用して詳細なメッセージを生成
            String completionMessage = importResult.buildDetailedCompletionMessage();

            DialogManager.getInstance().showCompletionDialog("インポート完了", completionMessage);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "インポート結果ダイアログを表示: " + importResult.getAnalysisDetailInfo());

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "インポート結果ダイアログの表示中にエラーが発生", _e);

            // フォールバック: 基本的な完了メッセージを表示
            DialogManager.getInstance().showCompletionDialog("インポート完了",
                    "CSVファイルのインポートが完了しました。\n" +
                            "詳細な結果表示中にエラーが発生しましたが、データの処理は正常に完了しています。");
        }
    }

    /**
     * インポートエラーの処理（既存メソッドを保持）
     * エラー時の統一的な処理を提供
     * * @param _e 発生した例外
     * 
     * @param currentPanel ステータス表示用パネル
     */
    private void handleImportError(Exception _e, JPanel currentPanel) {
        DialogManager.getInstance().showErrorDialog(
                "エラー",
                "インポート処理中にエラーが発生：" + _e.getMessage());

        if (currentPanel instanceof ListPanel) {
            ((ListPanel) currentPanel).setImportProcessing(false);
        }
    }

    /**
     * 重複ID処理（新規メソッド）
     * CSVAccessResultの重複ID確認を実行
     */
    private void handleDuplicateIds(CSVAccessResult importResult) {
        boolean overwrite = DialogManager.getInstance().showDuplicateIdConfirmDialog(
                importResult.getDuplicateIds());

        importResult.setOverwriteConfirmed(overwrite);

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                String.format("重複ID処理: %s, 件数=%d件",
                        (overwrite ? "上書き" : "スキップ"),
                        importResult.getDuplicateIds().size()));
    }

    /**
     * 未知のイベント処理
     * 未定義のイベントを処理します
     * * @param event イベント種別
     * 
     * @param data イベントデータ
     */
    private void handleUnknownEvent(String event, Object data) {
        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                "未定義のイベントを検出: " + event + ", データ: " + (data != null ? data.toString() : "null"));

        // 必要に応じてエラーダイアログを表示
        DialogManager.getInstance().showWarningDialog(
                "未定義のイベント",
                "システムが認識できないイベントが発生: " + event);
    }

    /**
     * ResourceManagerを活用したインポートファイルの検証
     * * @param file 検証対象ファイル
     * 
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

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "インポートファイル検証中にエラーが発生", _e);
            return new FileValidationResult(false,
                    "ファイル検証中にエラーが発生: " + _e.getMessage());
        }
    }

    /**
     * エラー処理
     * 通常のエラーを処理します
     * * @param _e 発生した例外
     */
    private void handleError(Exception _e) {
        // 通常のエラー処理
        LogHandler.getInstance().logError(LogType.SYSTEM, "エラーが発生", _e);
        // UI更新はSwingのEDTで実行
        javax.swing.SwingUtilities.invokeLater(() -> {
            // エラーダイアログを表示
            DialogManager.getInstance().showErrorDialog("処理中にエラーが発生", _e.getMessage());
        });
    }

    /**
     * 致命的なエラー処理
     * アプリケーションを安全に終了します
     * * @param _e 発生した例外
     */
    private void handleFatalError(Exception _e) {
        LogHandler.getInstance().logError(LogType.SYSTEM, "致命的なエラー", _e);
        initiateShutdown();
    }

    /**
     * シャットダウン処理の開始
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

        } catch (Exception _e) {
            // エラー時は緊急終了処理を実行
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "統合シャットダウン処理中にエラーが発生", _e);
            performEmergencyShutdown(_e);
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
                    "ビジネスロジック層のシャットダウンが完了");
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "ビジネスロジック層の終了処理中にエラーが発生", _e);
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
                        "UI層のリソース解放が完了");
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "MainFrameへの参照がnullのため、UI層の終了処理をスキップ");
            }
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "UI層の終了処理中にエラーが発生", _e);
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
                "リソース層のシャットダウンが完了");
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
                            "一部のResourceManagerリソース解放に失敗しましたが、処理を続行");
                }
            } else {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "ResourceManagerは未初期化のため、クリーンアップをスキップ");
            }
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "ResourceManagerクリーンアップ中にエラーが発生", _e);
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
            // 先にログシステムのクリーンアップを実行する
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "統合シャットダウン処理が正常に完了");
            LogHandler.getInstance().cleanup();

            // ウィンドウを閉じる
            if (mainFrame != null) {
                mainFrame.getJFrame().dispose();
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "メインウィンドウを閉じました");
            }

            // 終了処理完了フラグを設定
            isShutdownCompleted.set(true);

            // JVMの正常終了
            System.exit(0);

        } catch (Exception _e) {
            // 最終段階でのエラーは緊急終了で対応
            System.err.println("最終終了処理中にエラーが発生: " + _e.getMessage());
            performEmergencyShutdown(_e);
        }
    }

    /**
     * 緊急終了処理
     * 通常の終了処理でエラーが発生した場合の最後の手段
     */
    private void performEmergencyShutdown(Exception originalError) {
        try {
            // 最小限のクリーンアップを試行
            System.err.println("緊急終了処理を実行。元のエラー: " + originalError.getMessage());

            // ログシステムが生きていれば最後のログを記録
            try {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "緊急終了処理を実行", originalError);
                LogHandler.getInstance().cleanup();
            } catch (Exception logError) {
                System.err.println("ログ記録も失敗しました: " + logError.getMessage());
            }

            // 強制終了
            System.exit(1);

        } catch (Exception _e) {
            // 最後の最後の手段
            System.err.println("緊急終了処理中にも例外が発生: " + _e.getMessage());
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
                "実行中の非同期タスクを終了: " + taskCount + "件");

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
                } catch (Exception _e) {
                    LogHandler.getInstance().logError(LogType.SYSTEM,
                            "タスクの中断処理中にエラーが発生しました: " + taskId, _e);
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
     * * @return 実行中のタスク数
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }

    /**
     * シャットダウン状態の確認
     * * @return シャットダウン中の場合はtrue
     */
    public boolean isShuttingDown() {
        return isShuttingDown.get();
    }

    /**
     * エンジニアコントローラーを取得
     * * @return エンジニアコントローラー
     */
    public EngineerController getEngineerController() {
        return engineerController;
    }

    /**
     * 画面遷移コントローラーを取得
     * * @return 画面遷移コントローラー
     */
    public ScreenTransitionController getScreenController() {
        return screenController;
    }
}