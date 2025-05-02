package controller;

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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * アプリケーションのメインコントローラー
 * 画面遷移、イベント処理、スレッド管理を統括するクラス
 *
 * <p>
 * このクラスは、エンジニア人材管理システムのMVCアーキテクチャにおける中心的なコントローラーとして機能し、
 * ビュー（画面）とモデル（データ）を連携させる役割を担います。アプリケーション全体の状態管理から
 * 個別の操作イベント処理まで、ビジネスロジックの多くの側面を統括的に制御します。
 * </p>
 *
 * <p>
 * 主な責務：
 * <ul>
 * <li>画面遷移の制御とビュー間の調整</li>
 * <li>ユーザーインターフェースイベントの処理</li>
 * <li>非同期処理とスレッド管理</li>
 * <li>アプリケーション終了処理の調整</li>
 * <li>モデル操作の統合管理</li>
 * <li>データの保存と読み込み処理</li>
 * <li>エラー処理と回復機能</li>
 * </ul>
 * </p>
 *
 * <p>
 * このコントローラーは「コマンドパターン」に近い設計で実装されており、
 * イベント名と関連データをパラメーターとして受け取り、適切なハンドラーメソッドに
 * ディスパッチする仕組みを持っています。これにより、新しいイベント処理の追加が容易になり、
 * コードの保守性と拡張性が向上しています。
 * </p>
 *
 * <p>
 * スレッド管理機能により、長時間実行される処理（データの読み込み/保存など）を
 * 非同期的に実行し、UIのブロッキングを防止します。また、アプリケーション終了時には
 * すべての実行中スレッドを適切に終了させ、データの整合性を保ちつつ安全な終了を実現します。
 * </p>
 *
 * <p>
 * このコントローラーは以下のイベントを処理します：
 * <ul>
 * <li>REFRESH_VIEW - 現在の画面を更新</li>
 * <li>CHANGE_PANEL - 指定された画面に遷移</li>
 * <li>SAVE_DATA - エンジニア情報の保存</li>
 * <li>LOAD_DATA - エンジニア情報の読み込み</li>
 * <li>SHUTDOWN - アプリケーション終了</li>
 * </ul>
 * </p>
 *
 * @author Nagai
 * @version 4.3.0
 * @since 2025-05-02
 */
public class MainController {

    /** 画面遷移コントローラー */
    private final ScreenTransitionController screenController;

    /** エンジニアデータコントローラー */
    private EngineerController engineerController;

    /** リソースマネージャー */
    private ResourceManager resourceManager;

    /** メインフレーム */
    private final MainFrame mainFrame;

    /** ダイアログマネージャー */
    private final DialogManager dialogManager;

    /** 実行中非同期タスクの追跡マップ */
    private final ConcurrentMap<String, Thread> runningTasks;

    /** シャットダウン中フラグ */
    private final AtomicBoolean isShuttingDown;

    /**
     * コンストラクタ
     * 必要な初期化を行いますが、完全な初期化はinitializeメソッドで行います
     *
     * @param mainFrame メインフレーム
     */
    public MainController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.screenController = new ScreenTransitionController(mainFrame);
        this.runningTasks = new ConcurrentHashMap<>();
        this.isShuttingDown = new AtomicBoolean(false);
        this.dialogManager = DialogManager.getInstance();

        // 画面遷移コントローラーにメインコントローラーへの参照を設定
        this.screenController.setMainController(this);

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "メインコントローラーを初期化しました");
    }

    /**
     * アプリケーションの初期化
     * すべてのコンポーネントを初期化し、初期画面を表示
     */
    public void initialize() {
        try {
            // リソースマネージャーの取得
            resourceManager = new ResourceManager();

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
     * イベントを処理
     * アプリケーション全体のイベントをディスパッチします
     *
     * @param event イベント種別（"REFRESH_VIEW", "CHANGE_PANEL", "SAVE_DATA", "LOAD_DATA",
     *              "SHUTDOWN"）
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
                case "TEMPLATE":
                    handleTemplateExport();
                    break;
                /**
                 * 実装途中です
                 * case "EXPORT_CSV":
                 * handleExportCSV();
                 * break;
                 */
                case "SHUTDOWN":
                    initiateShutdown();
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
                            success = engineerController.addEngineer(engineer);
                            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                    "エンジニア情報の新規追加処理を実行: ID=" + engineer.getId());
                        }

                        if (success) {
                            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                    "エンジニア情報の保存に成功しました: ID=" + engineer.getId());

                            // UI更新はSwingのEDTで実行
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                try {
                                    // ListPanelを取得してデータを更新
                                    JPanel currentPanel = screenController.getCurrentPanel();
                                    if (currentPanel instanceof ListPanel) {
                                        ((ListPanel) currentPanel).addEngineerData(engineer);
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

                // UI更新はSwingのEDTで実行
                javax.swing.SwingUtilities.invokeLater(() -> {
                    // ListPanelを取得してエンジニアデータを設定
                    JPanel currentPanel = screenController.getCurrentPanel();
                    if (currentPanel instanceof ListPanel) {
                        ((ListPanel) currentPanel).setEngineerData(engineers);
                    }
                    screenController.refreshView();
                });

            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "データ読み込みに失敗しました", e);
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
        try {
            // エンジニア情報を取得
            EngineerDTO engineer = engineerController.getEngineerById(engineerId);

            if (engineer != null) {
                // 詳細画面に遷移し、完了後にエンジニア情報を設定するコールバックを指定
                screenController.showPanelWithCallback("DETAIL", () -> {
                    JPanel currentPanel = screenController.getCurrentPanel();
                    if (currentPanel instanceof DetailPanel) {
                        // エンジニア情報を設定
                        ((DetailPanel) currentPanel).setEngineerData(engineer);
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

    /* テンプレート出力機能 */
    public void handleTemplateExport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("テンプレートCSVの保存先を選択してください");
        fileChooser.setSelectedFile(new File("エンジニア情報テンプレート.csv"));

        boolean fileSaved = false;

        while (!fileSaved) {
            int userSelection = fileChooser.showSaveDialog(null);

            if (userSelection != JFileChooser.APPROVE_OPTION) {
                return; // キャンセル → 処理終了
            }

            File fileToSave = fileChooser.getSelectedFile();

            if (fileToSave.exists()) {
                int overwriteConfirm = JOptionPane.showConfirmDialog(
                        null,
                        "同じ名前のファイルが既に存在します。上書きしますか？",
                        "上書き確認",
                        JOptionPane.YES_NO_CANCEL_OPTION);

                if (overwriteConfirm == JOptionPane.YES_OPTION) {
                    // 上書きを許可された → 保存してループ終了
                    if (saveTemplate(fileToSave)) {
                        fileSaved = true;
                    } else {
                        break; // エラー → 終了
                    }
                } else if (overwriteConfirm == JOptionPane.NO_OPTION) {
                    continue; // 別名保存を促す → 再度ループ
                } else {
                    return; // キャンセル → 終了
                }
            } else {
                // 新規ファイル → 保存
                if (saveTemplate(fileToSave)) {
                    fileSaved = true;
                } else {
                    break; // エラー → 終了
                }
            }
        }
    }

    /** CSV出力機能 */
    /**
     * 実装途中です
     * public void handleExportCSV() {
     * JFileChooser fileChooser = new JFileChooser();
     * fileChooser.setDialogTitle("保存先を選択");
     * fileChooser.setSelectedFile(new File("エンジニア情報一覧.csv"));
     * 
     * while (true) {
     * int result = fileChooser.showSaveDialog(null);
     * if (result != JFileChooser.APPROVE_OPTION) return;
     * 
     * File file = fileChooser.getSelectedFile();
     * if (file.exists()) {
     * int overwrite = JOptionPane.showConfirmDialog(
     * null,
     * "既にファイルがあります。上書きしますか？",
     * "上書き確認",
     * JOptionPane.YES_NO_OPTION
     * );
     * 
     * if (overwrite != JOptionPane.YES_OPTION) continue;
     * }
     * 
     * List<EngineerDTO> data = mainFrame.getListPanel().getSelectedEngineers();
     * boolean success = new EngineerCSVDAO().exportCSV(data, file.getPath());
     * 
     * if (success) {
     * DialogManager.getInstance().showInfoDialog("完了", "CSVを出力しました");
     * } else {
     * DialogManager.getInstance().showErrorDialog("失敗", "CSV出力に失敗しました");
     * }
     * 
     * break;
     * }
     * }
     */

    // ヘルパーメソッド：保存処理を共通化
    private boolean saveTemplate(File file) {
        boolean result = new EngineerCSVDAO().exportTemplate(file.getPath());
        if (result) {
            DialogManager.getInstance().showInfoDialog("出力完了", "CSVテンプレートを保存しました。");
        } else {
            DialogManager.getInstance().showErrorDialog("出力エラー", "テンプレート出力に失敗しました。");
        }
        return result;
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
     * シャットダウン処理の開始
     * アプリケーションの安全な終了処理を開始します
     */
    public void initiateShutdown() {
        // 既にシャットダウン中なら処理しない
        if (isShuttingDown.getAndSet(true)) {
            return;
        }

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "アプリケーションのシャットダウンを開始します");

        // 実行中のタスクをすべて終了
        terminateRunningTasks();

        // メインフレームに終了を通知
        mainFrame.performShutdown();
    }

    /**
     * 実行中のタスクを終了
     */
    private void terminateRunningTasks() {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "実行中の非同期タスクを終了します: " + runningTasks.size() + "件");

        // すべての実行中タスクを中断
        for (Thread thread : runningTasks.values()) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }

        // マップをクリア
        runningTasks.clear();
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