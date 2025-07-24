package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Collator;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AbstractDocument;

import controller.MainController;
import model.EngineerDTO;
import util.DateOptionUtil;
import util.ListenerManager;
import util.LogHandler;
import util.LogHandler.LogType;
import util.PropertiesManager;
import util.TextLengthFilter;
import util.Constants.SystemConstants;

/**
 * エンジニア一覧を表示するパネルクラス（ListenerManager統合版）
 * ページング、ソート、検索、追加、取込、削除機能
 * * ListenerManagerによる統合リスナー管理の導入
 * リスナーのライフサイクル管理の明確化
 *
 * @author Nakano
 */
public class ListPanel extends JPanel {

    /** テーブルのカラム名 */
    private static final String[] COLUMN_NAMES = {
            "社員ID", "氏名", "生年月日", "エンジニア歴", "扱える言語"
    };

    /** テーブルコンポーネント */
    private final JTable table;

    /** スクロールパネル */
    private final JScrollPane scrollPane;

    /** テーブルモデル */
    private final DefaultTableModel tableModel;

    /** ページネーション関連コンポーネント */
    private final JLabel pageLabel;
    private final JButton prevButton;
    private final JButton nextButton;

    /** ボタンのフィールド化 */
    private JButton addButton;
    private JButton importButton;
    private JButton templateButton;
    private JButton exportButton;
    private JButton deleteButton;

    /** 処理中表示用ラベル */
    private JLabel statusLabel;

    /** 削除中状態フラグ */
    private boolean deleting = false;

    /** ファイル処理中状態フラグ (追加) */
    private boolean isFileProcessing = false;

    /** 一覧画面が次回表示時に再描画すべきかどうかのフラグ */
    private static boolean needsRefresh = false;

    /** 現在のページ番号 */
    private int currentPage = 1;

    /** 全エンジニアデータ */
    private List<EngineerDTO> allData;

    /** 現在表示中のデータ（検索結果またはallData） */
    private List<EngineerDTO> currentDisplayData;

    /** 検索モードフラグ */
    private boolean isSearchMode = false;

    /** 検索中フラグ */
    private boolean isSearching = false;

    /** 検索用フィールド */
    private PlaceholderTextField idField;
    private PlaceholderTextField nameField;
    private JComboBox<String> yearBox;
    private JComboBox<String> monthBox;
    private JComboBox<String> dayBox;
    private JComboBox<String> careerBox;

    /** ソート関係 */
    private static final int COLUMN_INDEX_EMPLOYEE_ID = 0;
    private static final int COLUMN_INDEX_NAME = 1;
    private static final int COLUMN_INDEX_BIRTHDATE = 2;
    private static final int COLUMN_INDEX_CAREER = 3;
    private static final int COLUMN_INDEX_PROGRAMMING_LANGUAGES = 4;
    private TableRowSorter<DefaultTableModel> sorter;
    private boolean isAscending = true;
    private int lastSortedColumn = -1;

    /** 選択されたエンジニアのIDを保持するセット */
    private final Set<String> selectedEngineerIds = new HashSet<>();
    private boolean isCtrlPressed = false;
    private boolean isShiftPressed = false;

    /** 検索機能のUI */
    private JButton searchButton;
    private JButton endSearchButton;

    /** メインコントローラー参照 */
    private MainController mainController;

    // ListenerManager統合による新機能

    /** リスナー管理システムのインスタンス */
    private final ListenerManager listenerManager;

    /** 登録されたリスナーIDを管理するリスト */
    private final List<String> registeredListenerIds;

    /** リスナー管理の状態フラグ */
    private boolean listenersInitialized = false;

    /**
     * プレースホルダー機能付きテキストフィールド
     * ユーザーに入力ヒントを提供するテキストフィールド
     */
    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        public PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            setColumns(10);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(Color.GRAY);
                g2d.setFont(getFont().deriveFont(Font.ITALIC));

                FontMetrics fm = g2d.getFontMetrics();
                int x = getInsets().left;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

                g2d.drawString(placeholder, x, y);
                g2d.dispose();
            }
        }
    }

    /**
     * コンストラクタ
     * パネルの初期化とUIコンポーネントの配置
     */
    public ListPanel() {
        super(new BorderLayout());

        // ListenerManagerの初期化
        this.listenerManager = ListenerManager.getInstance();
        this.registeredListenerIds = new ArrayList<>();

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "リストパネル初期化開始");

        // データ初期化
        this.allData = new ArrayList<>();
        this.currentDisplayData = new ArrayList<>();

        // テーブルモデルとテーブルの作成
        this.tableModel = createTableModel();
        this.table = createTable();
        this.scrollPane = new JScrollPane(table);

        new JTextField(20);

        // ページネーションコンポーネント
        this.pageLabel = new JLabel("ページ: 0 / 0");
        this.prevButton = new JButton("前へ");
        this.nextButton = new JButton("次へ");

        // 初期化処理を実行
        this.initialize();

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "リストパネル初期化完了：登録リスナー数=" + registeredListenerIds.size());
    }

    /**
     * パネルを初期化
     * UIコンポーネントの配置と初期設定
     */
    public void initialize() {
        try {
            // 上部パネル（タイトルと操作ボタン）
            this.add(createTopPanel(), BorderLayout.NORTH);

            // 中央部（テーブル）
            scrollPane.getViewport().setBackground(Color.WHITE);
            this.add(scrollPane, BorderLayout.CENTER);

            // 下部パネル（ページネーション）
            this.add(createBottomPanel(), BorderLayout.SOUTH);

            // ページネーションのイベント設定（ListenerManager経由）
            this.setupPaginationEventsWithManager();

            // ソート設定とイベント登録（ListenerManager経由）
            this.configureSorterWithManager();

            // テーブルのイベント設定（ListenerManager経由）
            this.setupTableEventsWithManager();

            // リスナー初期化完了フラグを設定
            this.listenersInitialized = true;

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "エンジニア一覧画面を初期化完了");

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "リストパネル初期化中にエラーが発生", _e);
            throw new RuntimeException("リストパネル初期化失敗", _e);
        }
    }

    /**
     * ページネーションのイベント設定（ListenerManager統合版）
     */
    private void setupPaginationEventsWithManager() {
        // 前へボタンのリスナー登録
        String prevListenerId = listenerManager.addActionListener(
                prevButton,
                _e -> changePage(-1),
                "ページネーション：前へボタン");
        registeredListenerIds.add(prevListenerId);

        // 次へボタンのリスナー登録
        String nextListenerId = listenerManager.addActionListener(
                nextButton,
                _e -> changePage(1),
                "ページネーション：次へボタン");
        registeredListenerIds.add(nextListenerId);

    }

    /**
     * ソート機能のイベント設定（ListenerManager統合版）
     */
    private void configureSorterWithManager() {
        // ソート可能な列を設定
        for (int columnNumber = 0; columnNumber < COLUMN_NAMES.length; columnNumber++) {
            sorter.setSortable(columnNumber, columnNumber != COLUMN_INDEX_PROGRAMMING_LANGUAGES);
        }

        // テーブルヘッダーのマウスリスナーをListenerManager経由で登録
        String headerListenerId = listenerManager.addMouseListener(
                table.getTableHeader(),
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent _e) {
                        if (isSearching)
                            return; // 検索中は処理をスキップ
                        // ソート処理の実行：クリック位置から列を特定
                        int columnIndex = table.columnAtPoint(_e.getPoint());
                        sortByColumn(columnIndex);

                        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                                "テーブルヘッダークリック：列=" + columnIndex + ", ソート=" +
                                        (isAscending ? "昇順" : "降順"));
                    }
                },
                "テーブルヘッダー：ソート機能");
        registeredListenerIds.add(headerListenerId);
    }

    /**
     * テーブルのイベント設定（ListenerManager統合版）
     * * 1. リスナーの目的が明確（説明文付き）
     * 2. デバッグ時にリスナーの動作を追跡可能
     * 3. 必要に応じて個別のリスナーを無効化可能
     * 4. メモリリーク防止のための確実な削除
     */
    private void setupTableEventsWithManager() {
        // テーブルのマウスイベント処理：複雑な選択ロジックを含む
        String tableMouseListenerId = listenerManager.addMouseListener(
                table,
                new MouseAdapter() {
                    /**
                     * マウス押下時の処理：修飾キーの状態を記録
                     * Ctrl/Shiftキーの状態を正確に把握することで、
                     * 複数選択の動作を制御します。
                     */
                    @Override
                    public void mousePressed(MouseEvent _e) {
                        if (isSearching || deleting || isFileProcessing)
                            return; // 検索中は処理をスキップ
                        // 修飾キーの状態を記録（複数選択制御用）
                        isCtrlPressed = (_e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
                        isShiftPressed = (_e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;

                        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                                "テーブルマウス押下：Ctrl=" + isCtrlPressed + ", Shift=" + isShiftPressed);
                    }

                    /**
                     * マウス離上時の処理：選択状態の更新
                     * 実際の選択処理はここで実行されます。押下時ではなく離上時に
                     * 処理することで、ドラッグ操作との区別を明確にしています。
                     */
                    @Override
                    public void mouseReleased(MouseEvent _e) {
                        if (isSearching || deleting || isFileProcessing)
                            return; // 検索中は処理をスキップ
                        try {
                            // 選択されたエンジニアIDを更新
                            updateSelectedEngineerIds(isCtrlPressed, isShiftPressed);

                            // ボタン状態を更新（選択状態に応じて削除・出力ボタンを有効化）
                            updateButtonState();

                            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                                    "選択状態更新完了：選択数=" + selectedEngineerIds.size());

                        } catch (Exception ex) {
                            LogHandler.getInstance().logError(LogType.UI,
                                    "マウス離上処理中にエラーが発生", ex);
                        }
                    }

                    /**
                     * ダブルクリック処理：詳細画面への遷移
                     * ダブルクリックによる詳細表示は、多くのユーザーが期待する
                     * 直感的な操作です。エラーハンドリングを含めて安全に実装しています。
                     */
                    @Override
                    public void mouseClicked(MouseEvent _e) {
                        if (isSearching || deleting || isFileProcessing)
                            return; // 検索中は処理をスキップ
                        if (_e.getClickCount() == 2) { // ダブルクリックの検出
                            try {
                                int row = table.rowAtPoint(_e.getPoint());
                                if (row >= 0) {
                                    // ダブルクリックされた行を選択状態にする
                                    table.setRowSelectionInterval(row, row);

                                    // 詳細画面を開く
                                    openDetailView();

                                    LogHandler.getInstance().log(Level.INFO, LogType.UI,
                                            "テーブルダブルクリック：行=" + row + "の詳細表示を開始");
                                }
                            } catch (Exception ex) {
                                LogHandler.getInstance().logError(LogType.UI,
                                        "ダブルクリック処理中にエラーが発生", ex);
                            }
                        }
                    }
                },
                "テーブル：マウス操作（選択・ダブルクリック）");
        registeredListenerIds.add(tableMouseListenerId);
    }

    /**
     * すべてのリスナーを削除（クリーンアップ用）
     * * このメソッドは、パネルが破棄される際やアプリケーション終了時に
     * 呼び出されます。すべてのリスナーを確実に削除することで、
     * メモリリークを防止し、システムリソースを適切に解放します。
     * * @return 削除されたリスナーの数
     */
    public int removeAllListeners() {
        if (!listenersInitialized) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "リスナーが初期化されていないため、クリーンアップをスキップ");
            return 0;
        }

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "リストパネルのリスナークリーンアップを開始：対象=" + registeredListenerIds.size() + "個");

        int removedCount = 0;
        Iterator<String> iterator = registeredListenerIds.iterator();

        while (iterator.hasNext()) {
            String listenerId = iterator.next();
            try {
                if (listenerManager.removeListener(listenerId)) {
                    removedCount++;
                    iterator.remove(); // 安全な削除

                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "リスナー削除成功：" + listenerId);
                } else {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "リスナー削除失敗：" + listenerId);
                }
            } catch (Exception _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "リスナー削除中にエラーが発生：" + listenerId, _e);
            }
        }

        // 初期化フラグをリセット
        listenersInitialized = false;

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "ListPanelリスナークリーンアップ完了：削除数=" + removedCount);

        return removedCount;
    }

    /**
     * リスナー管理の詳細情報を取得（デバッグ用）
     * * 開発時やトラブルシューティング時に、現在登録されている
     * リスナーの状況を詳しく確認できる
     * * @return リスナー管理情報の詳細な文字列
     */
    public String getListenerDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== ListPanel リスナー管理情報 ===\n");
        info.append("初期化状態: ").append(listenersInitialized ? "完了" : "未完了").append("\n");
        info.append("登録済みリスナー数: ").append(registeredListenerIds.size()).append("\n");
        info.append("削除中状態: ").append(deleting ? "はい" : "いいえ").append("\n\n");

        info.append("登録リスナー一覧:\n");
        for (int i = 0; i < registeredListenerIds.size(); i++) {
            String listenerId = registeredListenerIds.get(i);
            info.append(String.format("  %d. %s\n", i + 1, listenerId));
        }

        // ListenerManagerから詳細情報を取得
        info.append("\n").append(listenerManager.getDebugInfo());

        return info.toString();
    }

    /**
     * テーブルモデルを作成
     *
     * @return 初期化されたDefaultTableModel
     */
    private DefaultTableModel createTableModel() {
        return new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // セル編集を無効化
            }
        };
    }

    /**
     * テーブルを作成
     *
     * @return 初期化されたJTable
     */
    private JTable createTable() {
        JTable newTable = new JTable(tableModel);

        // ソーター設定
        sorter = new TableRowSorter<>(tableModel);
        newTable.setRowSorter(sorter);

        // カラムのドラッグ＆ドロップによる並び替えを禁止
        newTable.getTableHeader().setReorderingAllowed(false);

        // 列幅設定
        newTable.getColumnModel().getColumn(0).setPreferredWidth(100); // 社員ID
        newTable.getColumnModel().getColumn(1).setPreferredWidth(150); // 氏名
        newTable.getColumnModel().getColumn(2).setPreferredWidth(120); // 生年月日
        newTable.getColumnModel().getColumn(3).setPreferredWidth(100); // エンジニア歴
        newTable.getColumnModel().getColumn(4).setPreferredWidth(500); // 扱える言語

        // 選択モード設定 - 複数選択モードに変更
        newTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 行の高さ
        newTable.setRowHeight(25);

        return newTable;
    }

    /**
     * 月のコンボボックス用データを生成
     */
    private String[] getMonths() {
        String[] months = new String[13];
        months[0] = "";
        DecimalFormat df = new DecimalFormat("0");
        for (int monthValue = 1; monthValue <= 12; monthValue++) {
            months[monthValue] = df.format(monthValue);
        }
        return months;
    }

    /**
     * 日のコンボボックス用データを生成
     */
    private String[] getDays() {
        String[] days = new String[32];
        days[0] = "";
        DecimalFormat df = new DecimalFormat("0");
        for (int i = 1; i <= 31; i++) {
            days[i] = df.format(i);
        }
        return days;
    }

    /**
     * エンジニア歴のコンボボックス用データを生成
     */
    private String[] getCareerYears() {
        String[] careers = new String[51];
        careers[0] = "";
        DecimalFormat df = new DecimalFormat("0");
        for (int i = 1; i <= 50; i++) {
            careers[i] = df.format(i);
        }
        return careers;
    }

    /**
     * 上部パネルを作成
     * ボタン群と検索パネルを含む2段構成
     */
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new GridLayout(2, 1));

        // ボタン群パネル（ActionListenerもListenerManager経由）
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        this.addButton = new JButton("新規追加");
        String addListenerId = listenerManager.addActionListener(
                addButton, _e -> addNewEngineer(), "新規追加ボタン");
        registeredListenerIds.add(addListenerId);
        buttonPanel.add(addButton);

        this.importButton = new JButton("取込");
        String importListenerId = listenerManager.addActionListener(
                importButton, _e -> importData(), "取込ボタン");
        registeredListenerIds.add(importListenerId);
        buttonPanel.add(importButton);

        this.templateButton = new JButton("テンプレ");
        String templateListenerId = listenerManager.addActionListener(
                templateButton, _e -> loadTemplate(), "テンプレートボタン");
        registeredListenerIds.add(templateListenerId);
        buttonPanel.add(templateButton);

        this.exportButton = new JButton("出力");
        this.exportButton.setEnabled(false);
        String exportListenerId = listenerManager.addActionListener(
                exportButton, _e -> exportData(), "出力ボタン");
        registeredListenerIds.add(exportListenerId);
        buttonPanel.add(this.exportButton);

        this.deleteButton = new JButton("削除");
        this.deleteButton.setEnabled(false);
        String deleteListenerId = listenerManager.addActionListener(
                deleteButton, _e -> deleteSelectedRow(), "削除ボタン");
        registeredListenerIds.add(deleteListenerId);
        buttonPanel.add(this.deleteButton);

        topPanel.add(buttonPanel);

        // 検索パネル
        topPanel.add(createSearchPanel());

        return topPanel;
    }

    /**
     * 検索パネルを作成
     */
    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 社員ID - 10文字制限適用
        searchPanel.add(new JLabel("社員ID:"));
        idField = new PlaceholderTextField("5桁の数値");
        idField.setColumns(10);
        applySearchFieldLengthFilter(idField, getSearchEmployeeIdMaxLength(), "社員ID検索");
        searchPanel.add(idField);

        // 氏名 - 20文字制限適用
        searchPanel.add(new JLabel("氏名:"));
        nameField = new PlaceholderTextField("20文字以内");
        nameField.setColumns(10);
        applySearchFieldLengthFilter(nameField, SystemConstants.MAX_NAME_LENGTH, "氏名検索");
        searchPanel.add(nameField);

        // 生年月日
        searchPanel.add(new JLabel("生年月日:"));
        yearBox = new JComboBox<>(DateOptionUtil.getSearchYearOptions());
        monthBox = new JComboBox<>(getMonths());
        dayBox = new JComboBox<>(getDays());
        searchPanel.add(yearBox);
        searchPanel.add(new JLabel("年"));
        searchPanel.add(monthBox);
        searchPanel.add(new JLabel("月"));
        searchPanel.add(dayBox);
        searchPanel.add(new JLabel("日"));

        // 年月コンボボックスにリスナーを追加
        ActionListener dateUpdateListener = _e -> updateSearchDayOptions();
        yearBox.addActionListener(dateUpdateListener);
        monthBox.addActionListener(dateUpdateListener);

        // エンジニア歴
        searchPanel.add(new JLabel("エンジニア歴:"));
        careerBox = new JComboBox<>(getCareerYears());
        searchPanel.add(careerBox);

        // 検索ボタン（ListenerManager経由）
        searchButton = new JButton("検索");
        String searchListenerId = listenerManager.addActionListener(
                searchButton, _e -> handleSearchButton(), "検索ボタン");
        registeredListenerIds.add(searchListenerId);
        searchPanel.add(searchButton);

        // 検索終了ボタン（ListenerManager経由）
        endSearchButton = new JButton("検索終了");
        endSearchButton.setVisible(false);
        String endSearchListenerId = listenerManager.addActionListener(
                endSearchButton, _e -> handleEndSearchButton(), "検索終了ボタン");
        registeredListenerIds.add(endSearchListenerId);
        searchPanel.add(endSearchButton);

        return searchPanel;
    }

    /**
     * 検索パネルの日の選択肢を動的に更新する
     */
    private void updateSearchDayOptions() {
        String selectedYear = (String) yearBox.getSelectedItem();
        String selectedMonth = (String) monthBox.getSelectedItem();
        String currentDay = (String) dayBox.getSelectedItem();

        // 新しい日の選択肢を取得
        String[] newDayOptions = DateOptionUtil.getDayOptions(selectedYear, selectedMonth);

        // 日のコンボボックスのモデルを更新
        dayBox.setModel(new DefaultComboBoxModel<>(newDayOptions));

        // 以前の選択値が新しい選択肢に存在すれば再選択
        if (currentDay != null) {
            for (String option : newDayOptions) {
                if (currentDay.equals(option)) {
                    dayBox.setSelectedItem(currentDay);
                    return;
                }
            }
        }
        // 存在しない場合は先頭（空欄）を選択
        dayBox.setSelectedIndex(0);
    }

    /**
     * 検索フィールド専用の文字数制限適用メソッド
     * PlaceholderTextFieldに対応したDocumentFilter適用処理
     * * @param textField 対象のPlaceholderTextField
     *
     * @param maxLength 最大文字数
     * @param fieldName フィールド名（ログ用）
     */
    private void applySearchFieldLengthFilter(PlaceholderTextField textField, int maxLength, String fieldName) {
        try {
            if (textField == null) {
                LogHandler.getInstance().logError(LogType.UI,
                        fieldName + "フィールドがnullのため、文字数制限を適用できません", null);
                return;
            }

            if (maxLength <= 0) {
                LogHandler.getInstance().logError(LogType.UI,
                        fieldName + "フィールドの最大文字数が不正です: " + maxLength, null);
                return;
            }

            // PlaceholderTextFieldにDocumentFilterを適用
            TextLengthFilter filter = new TextLengthFilter(maxLength, fieldName);
            ((AbstractDocument) textField.getDocument()).setDocumentFilter(filter);

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.UI,
                    fieldName + "フィールドへの文字数制限適用中にエラーが発生", _e);
        }
    }

    /**
     * 検索用社員IDの最大文字数を取得
     * 通常の登録用とは異なり、検索では柔軟な入力を許可するため10文字制限
     * * @return 検索用社員IDの最大文字数
     */
    private int getSearchEmployeeIdMaxLength() {
        try {
            // 検索用は登録用より柔軟な制限を適用
            return PropertiesManager.getInstance().getInt("validation.employee.id.max.length", 10);
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "検索用社員ID最大文字数の取得に失敗、デフォルト値を使用", _e);
            return 10;
        }
    }

    /**
     * 検索フィールドのクリア処理（文字数制限対応版）
     * 検索終了時に実行される処理
     */
    private void clearSearchFields() {
        try {
            if (idField != null) {
                idField.setText("");
                LogHandler.getInstance().log(Level.INFO, LogType.UI, "社員ID検索フィールドをクリア");
            }

            if (nameField != null) {
                nameField.setText("");
                LogHandler.getInstance().log(Level.INFO, LogType.UI, "氏名検索フィールドをクリア");
            }

            // コンボボックスの初期化
            if (yearBox != null)
                yearBox.setSelectedIndex(0);
            if (monthBox != null)
                monthBox.setSelectedIndex(0);
            if (dayBox != null)
                dayBox.setSelectedIndex(0);
            if (careerBox != null)
                careerBox.setSelectedIndex(0);

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.UI, "検索フィールドのクリア中にエラーが発生", _e);
        }
    }

    /**
     * 検索ボタンのクリック処理
     */
    private void handleSearchButton() {
        if (mainController == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.UI,
                    "メインコントローラが設定されていないため検索できません");
            return;
        }

        MainController.SearchCriteria criteria = new MainController.SearchCriteria(
                idField.getText(),
                nameField.getText(),
                yearBox.getSelectedItem().toString(),
                monthBox.getSelectedItem().toString(),
                dayBox.getSelectedItem().toString(),
                careerBox.getSelectedItem().toString());

        // UIの無効化処理を追加
        isSearching = true;
        statusLabel.setText("検索中...");
        setUIComponentsEnabled(false);
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);
        table.setEnabled(false);
        table.getTableHeader().setEnabled(false);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        SwingWorker<MainController.SearchResult, Void> worker = new SwingWorker<>() {
            @Override
            protected MainController.SearchResult doInBackground() throws Exception {
                return mainController.searchEngineers(criteria);
            }

            @Override
            protected void done() {
                try {
                    MainController.SearchResult result = get();
                    if (result.hasErrors()) {
                        DialogManager.getInstance().showInfoDialog("検索結果", "該当するエンジニアは見つかりませんでした。");
                    } else {
                        List<EngineerDTO> searchResults = result.getResults();
                        if (searchResults.isEmpty()) {
                            DialogManager.getInstance().showInfoDialog("検索結果", "該当するエンジニアは見つかりませんでした。");
                        }
                        updateSearchResults(searchResults);
                    }
                } catch (Exception _e) {
                    LogHandler.getInstance().logError(LogType.UI, "検索処理中にエラーが発生", _e);
                    DialogManager.getInstance().showErrorDialog("検索エラー", "検索処理中にエラーが発生しました。");
                } finally {
                    // UIの有効化処理を追加
                    isSearching = false;
                    statusLabel.setText("");
                    setUIComponentsEnabled(true);
                    updatePaginationButtons(currentDisplayData.size());
                    table.setEnabled(true);
                    table.getTableHeader().setEnabled(true);
                    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                    endSearchButton.setVisible(true);
                }
            }
        };

        worker.execute();
    }

    /**
     * 検索結果をテーブルに反映
     */
    private void updateSearchResults(List<EngineerDTO> results) {
        isSearchMode = true;
        currentDisplayData = new ArrayList<>(results);
        currentPage = 1;
        selectedEngineerIds.clear();
        table.clearSelection();
        updateTableData(getDisplayData());
        updateButtonState();

        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                String.format("検索実行: %d件のデータがヒット", results.size()));
    }

    /**
     * 検索終了ボタンのクリック処理
     */
    private void handleEndSearchButton() {
        SwingWorker<Void, Void> searchWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                SwingUtilities.invokeLater(() -> {
                    clearSearchFields();
                });

                resetSearchAndShowAllData();
                return null;
            }

            @Override
            protected void done() {
                statusLabel.setText("");
                endSearchButton.setVisible(false);
                setUIComponentsEnabled(true);

                LogHandler.getInstance().log(Level.INFO, LogType.UI, "検索終了処理が完了しました");
            }
        };
        searchWorker.execute();
    }

    /**
     * UI コンポーネントの有効/無効を切り替える
     */
    public void setUIComponentsEnabled(boolean enabled) {
        idField.setEnabled(enabled);
        nameField.setEnabled(enabled);
        yearBox.setEnabled(enabled);
        monthBox.setEnabled(enabled);
        dayBox.setEnabled(enabled);
        careerBox.setEnabled(enabled);
        searchButton.setEnabled(enabled);
        addButton.setEnabled(enabled);
        importButton.setEnabled(enabled);
        templateButton.setEnabled(enabled);
        if (enabled) {
            updateButtonState();
        } else {
            exportButton.setEnabled(false);
            deleteButton.setEnabled(false);
        }
    }

    /**
     * 検索状態をリセットして全データを表示
     */
    private void resetSearchAndShowAllData() {
        isSearchMode = false;
        currentDisplayData = new ArrayList<>(allData);
        currentPage = 1;

        SwingUtilities.invokeLater(() -> {
            List<EngineerDTO> displayData = getDisplayData();
            updateTableData(displayData);
            updateButtonState();
        });
    }

    /**
     * 下部パネル（ページネーション）を作成
     */
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        navigationPanel.add(prevButton);
        navigationPanel.add(pageLabel);
        navigationPanel.add(nextButton);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusPanel.add(statusLabel);

        bottomPanel.add(navigationPanel, BorderLayout.CENTER);
        bottomPanel.add(statusPanel, BorderLayout.EAST);

        return bottomPanel;
    }

    /**
     * 指定された列インデックスに基づいてエンジニア情報のリストをソートします
     */
    private void sortByColumn(int columnIndex) {
        try {
            if (columnIndex < 0 || columnIndex >= COLUMN_NAMES.length
                    || columnIndex == COLUMN_INDEX_PROGRAMMING_LANGUAGES) {
                return;
            }

            selectedEngineerIds.clear();
            table.clearSelection();

            if (lastSortedColumn == columnIndex) {
                isAscending = !isAscending;
            } else {
                isAscending = true;
                lastSortedColumn = columnIndex;
            }

            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    String.format("ソート実行: 列=%s, 順序=%s", COLUMN_NAMES[columnIndex],
                            isAscending ? "昇順" : "降順"));

            currentDisplayData = sortEngineers(currentDisplayData, columnIndex, isAscending);
            currentPage = 1;
            updateTableData(getDisplayData());
            updateButtonState();

            sorter.setSortKeys(List.of(new RowSorter.SortKey(columnIndex,
                    isAscending ? SortOrder.ASCENDING : SortOrder.DESCENDING)));
            sorter.setComparator(columnIndex, (o1, o2) -> 0);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("ソート完了: %d件", currentDisplayData.size()));
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "ソート処理中にエラーが発生", _e);
        }
    }

    /**
     * 現在のソート条件に基づいてエンジニアリストをソートする
     */
    private List<EngineerDTO> sortEngineers(List<EngineerDTO> engineers, int columnIndex, boolean ascending) {
        Comparator<EngineerDTO> comparator = null;

        switch (columnIndex) {
            case COLUMN_INDEX_EMPLOYEE_ID:
                comparator = Comparator.comparing(_e -> parseNumericId(_e.getId()),
                        Comparator.nullsLast(Integer::compareTo));
                break;
            case COLUMN_INDEX_NAME:
                comparator = Comparator.comparing(EngineerDTO::getNameKana,
                        getJapaneseKanaComparator());
                break;
            case COLUMN_INDEX_BIRTHDATE:
                comparator = Comparator.comparing(EngineerDTO::getBirthDate,
                        Comparator.nullsLast(LocalDate::compareTo));
                break;
            case COLUMN_INDEX_CAREER:
                comparator = Comparator.comparingInt(EngineerDTO::getCareer);
                break;
            default:
                return new ArrayList<>(engineers);
        }

        Comparator<EngineerDTO> idComparator = Comparator
                .comparing((EngineerDTO _e) -> parseNumericId(_e.getId()));

        Comparator<EngineerDTO> finalComparator = ascending
                ? comparator.thenComparing(idComparator)
                : comparator.reversed().thenComparing(idComparator);

        List<EngineerDTO> sorted = new ArrayList<>(engineers);
        sorted.sort(finalComparator);

        return sorted;
    }

    /**
     * IDの数値変換
     */
    private int parseNumericId(String id) {
        try {
            return Integer.parseInt(id.replaceAll("\\D+", ""));
        } catch (NumberFormatException _e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * 日本語のかな順で文字列を比較するComparatorを返します
     */
    private Comparator<String> getJapaneseKanaComparator() {
        Collator collator = Collator.getInstance(Locale.JAPANESE);
        return (s1, s2) -> {
            if (s1 == null && s2 == null)
                return 0;
            if (s1 == null)
                return 1;
            if (s2 == null)
                return -1;
            return collator.compare(s1, s2);
        };
    }

    /**
     * ページを切り替え
     */
    private void changePage(int delta) {
        List<EngineerDTO> displayData = getDisplayData();
        if (displayData == null || displayData.isEmpty()) {
            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    "データがないためページ切り替えをスキップします");
            updatePageLabel(0);
            updatePaginationButtons(0);
            return;
        }

        int totalPages = (int) Math.ceil((double) displayData.size() / SystemConstants.PAGE_SIZE);
        int newPage = currentPage + delta;

        if (newPage < 1 || newPage > totalPages) {
            return;
        }

        currentPage = newPage;
        updateTableData(displayData);

        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                String.format("ページを切り替えました: %d / %d", currentPage, totalPages));
    }

    /**
     * ページラベルを更新
     */
    private void updatePageLabel(int dataSize) {
        int totalPages = (int) Math.ceil((double) dataSize / SystemConstants.PAGE_SIZE);
        totalPages = Math.max(1, totalPages);
        pageLabel.setText(String.format("ページ: %d / %d", currentPage, totalPages));
    }

    /**
     * テーブルに1エンジニアのデータを追加
     */
    private void addEngineerToTable(EngineerDTO engineer) {
        if (engineer == null) {
            return;
        }

        String languages = "";
        if (engineer.getProgrammingLanguages() != null) {
            languages = String.join(", ", engineer.getProgrammingLanguages());
        }

        tableModel.addRow(new Object[] {
                engineer.getId(),
                engineer.getName(),
                engineer.getBirthDate(),
                engineer.getCareer(),
                languages
        });
    }

    /**
     * エンジニアデータを設定
     */
    public void setEngineerData(List<EngineerDTO> engineers) {
        this.allData = new ArrayList<>(engineers);

        if (!isSearchMode) {
            this.currentDisplayData = new ArrayList<>(engineers);
        }

        currentPage = 1;
        updateTableData(getDisplayData());

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                String.format("エンジニアデータを更新: %d件", allData.size()));
    }

    /**
     * エンジニアデータの追加
     */
    public void addEngineerData(EngineerDTO engineer) {
        if (engineer == null) {
            return;
        }

        boolean replaced = false;
        for (int i = 0; i < allData.size(); i++) {
            if (allData.get(i).getId().equals(engineer.getId())) {
                allData.set(i, engineer);
                replaced = true;
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        String.format("エンジニア情報を更新: ID=%s, 氏名=%s",
                                engineer.getId(), engineer.getName()));
                break;
            }
        }

        if (!replaced) {
            allData.add(engineer);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("エンジニアを追加: ID=%s, 氏名=%s",
                            engineer.getId(), engineer.getName()));
        }

        if (!isSearchMode) {
            currentDisplayData = new ArrayList<>(allData);
        }

        List<EngineerDTO> displayData = getDisplayData();
        updateTableData(displayData);
    }

    /**
     * 表示用データの取得（ソートを適用）
     */
    private List<EngineerDTO> getDisplayData() {
        if (currentDisplayData == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "表示データがnullのため、空のリストを返します");
            return new ArrayList<>();
        }

        List<EngineerDTO> result = new ArrayList<>(currentDisplayData);

        if (lastSortedColumn >= 0) {
            result = sortEngineers(result, lastSortedColumn, isAscending);
        }

        return result;
    }

    /**
     * 現在選択されているエンジニアを取得
     */
    public EngineerDTO getSelectedEngineer() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            return null;
        }

        List<EngineerDTO> displayData = getDisplayData();

        if (displayData == null || displayData.isEmpty()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.UI,
                    "表示データがないため選択エンジニアを取得できません");
            return null;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        int startIndex = (currentPage - 1) * SystemConstants.PAGE_SIZE;
        int dataIndex = startIndex + modelRow;

        if (dataIndex >= 0 && dataIndex < displayData.size()) {
            return displayData.get(dataIndex);
        }

        return null;
    }

    /**
     * テーブルデータとページネーションを一括更新
     */
    private void updateTableData(List<EngineerDTO> data) {
        tableModel.setRowCount(0);

        int startIndex = (currentPage - 1) * SystemConstants.PAGE_SIZE;
        int endIndex = Math.min(startIndex + SystemConstants.PAGE_SIZE, data.size());

        for (int i = startIndex; i < endIndex; i++) {
            addEngineerToTable(data.get(i));
        }

        updatePageLabel(data.size());
        updatePaginationButtons(data.size());
        restoreRowSelectionById();
    }

    /**
     * ページネーションボタンの状態を更新
     */
    private void updatePaginationButtons(int dataSize) {
        int totalPages = (int) Math.ceil((double) dataSize / SystemConstants.PAGE_SIZE);
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
    }

    /**
     * 現在選択されているエンジニアリストを取得
     */
    public List<EngineerDTO> getSelectedEngineers() {
        List<EngineerDTO> selectedEngineers = new ArrayList<>();
        List<EngineerDTO> displayData = getDisplayData();

        if (displayData == null || displayData.isEmpty()) {
            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    "表示データがないため選択エンジニアリストは空です");
            return selectedEngineers;
        }

        Set<String> uniqueIds = new HashSet<>();

        for (EngineerDTO engineerDTO : displayData) {
            if (selectedEngineerIds.contains(engineerDTO.getId()) && uniqueIds.add(engineerDTO.getId())) {
                selectedEngineers.add(engineerDTO);
            }
        }

        return selectedEngineers;
    }

    /**
     * テーブルの選択状態を管理する
     */
    private void updateSelectedEngineerIds(boolean isCtrlDown, boolean isShiftDown) {
        int startIndex = (currentPage - 1) * SystemConstants.PAGE_SIZE;
        List<EngineerDTO> displayData = getDisplayData();
        if (displayData == null || displayData.isEmpty()) {
            updateTableData(new ArrayList<>());
            updateButtonState();
            return;
        }
        int previousSelectionCount = selectedEngineerIds.size();

        Set<String> newlySelectedIds = collectSelectedIds(displayData, startIndex, isShiftDown);
        Set<String> currentPageIds = getCurrentPageIds(displayData, startIndex);
        removeUnselectedIds(currentPageIds, newlySelectedIds);

        if (!isCtrlDown && !isShiftDown) {
            clearSelection();
            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    "通常クリックによる選択: 既存の選択はクリア");
        }

        selectedEngineerIds.addAll(newlySelectedIds);

        int currentSelectionCount = selectedEngineerIds.size();

        if (currentSelectionCount != previousSelectionCount) {
            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    String.format("選択件数が変更されました: %d → %d件", previousSelectionCount, currentSelectionCount));
        }
    }

    /**
     * 現在ページで選択された行のIDを収集する
     */
    private Set<String> collectSelectedIds(List<EngineerDTO> displayData, int startIndex, boolean isShiftDown) {
        Set<String> newlySelectedIds = new HashSet<>();
        int[] selectedRows = table.getSelectedRows();

        if (isShiftDown && selectedRows.length > 0) {
            int startRow = table.getSelectionModel().getAnchorSelectionIndex();
            int endRow = table.getSelectionModel().getLeadSelectionIndex();
            int fromRow = Math.min(startRow, endRow);
            int toRow = Math.max(startRow, endRow);

            for (int viewRowIndex = fromRow; viewRowIndex <= toRow; viewRowIndex++) {
                int modelRow = table.convertRowIndexToModel(viewRowIndex);
                int dataIndex = startIndex + modelRow;
                if (dataIndex >= 0 && dataIndex < displayData.size()) {
                    newlySelectedIds.add(displayData.get(dataIndex).getId());
                }
            }
        } else {
            for (int selectedRow : selectedRows) {
                int modelRow = table.convertRowIndexToModel(selectedRow);
                int dataIndex = startIndex + modelRow;
                if (dataIndex >= 0 && dataIndex < displayData.size()) {
                    newlySelectedIds.add(displayData.get(dataIndex).getId());
                }
            }
        }
        return newlySelectedIds;
    }

    /**
     * 現在ページに表示されているエンジニアIDのセットを作成する
     */
    private Set<String> getCurrentPageIds(List<EngineerDTO> displayData, int startIndex) {
        return displayData.stream()
                .skip(startIndex)
                .limit(SystemConstants.PAGE_SIZE)
                .map(EngineerDTO::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 現在のページで選択解除されたIDのみを選択IDセットから削除する
     */
    private void removeUnselectedIds(Set<String> currentPageIds, Set<String> newlySelectedIds) {
        Iterator<String> selectedIditerator = selectedEngineerIds.iterator();
        while (selectedIditerator.hasNext()) {
            String id = selectedIditerator.next();
            if (currentPageIds.contains(id) && !newlySelectedIds.contains(id)) {
                selectedIditerator.remove();
            }
        }
    }

    /**
     * 選択状態をクリア
     */
    private void clearSelection() {
        selectedEngineerIds.clear();
    }

    /**
     * ページ切り替えやテーブル再描画時に、選択IDに基づいて
     * テーブルの選択状態を復元する
     */
    private void restoreRowSelectionById() {
        int startIndex = (currentPage - 1) * SystemConstants.PAGE_SIZE;
        List<EngineerDTO> displayData = getDisplayData();
        if (displayData == null || displayData.isEmpty()) {
            return;
        }

        for (int rowIndex = 0; rowIndex < SystemConstants.PAGE_SIZE
                && (startIndex + rowIndex) < displayData.size(); rowIndex++) {
            EngineerDTO engineerDTO = displayData.get(startIndex + rowIndex);
            if (selectedEngineerIds.contains(engineerDTO.getId())) {
                table.addRowSelectionInterval(rowIndex, rowIndex);
            }
        }
    }

    /**
     * データ件数を取得
     */
    public int getDataCount() {
        return allData.size();
    }

    /**
     * 現在のページ番号を取得
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * テーブルコンポーネントを取得
     */
    public JTable getTable() {
        return table;
    }

    /**
     * 新規追加ボタンのイベントハンドラ
     */
    private void addNewEngineer() {
        LogHandler.getInstance().log(Level.INFO, LogType.UI, "新規追加ボタンが押されました");
        if (mainController != null) {
            mainController.handleEvent("CHANGE_PANEL", "ADD");
        } else {
            LogHandler.getInstance().log(Level.WARNING, LogType.UI,
                    "MainControllerが設定されていないため画面遷移できません");
        }
    }

    /**
     * インポート処理開始時の状態設定
     * ボタンを無効化し、処理中であることを示す
     */
    public void setImportProcessing(boolean processing) {
        if (processing) {
            // インポート処理中はボタンを無効化
            setButtonsEnabled(false);
            setFileProcessing(true, "CSV処理中...");

            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    "インポート処理開始：UIコンポーネントを無効化");
        } else {
            // インポート処理完了後はボタンを有効化
            setButtonsEnabled(true);
            clearStatus();

            // ボタン状態を再評価（選択状態に応じて）
            updateButtonState();

            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    "インポート処理完了：UIコンポーネントを有効化");
        }
    }

    /**
     * ファイル処理中の状態を設定する
     * 
     * @param processing    処理中の場合はtrue
     * @param statusMessage 表示するステータスメッセージ
     */
    public void setFileProcessing(boolean processing, String statusMessage) {
        this.isFileProcessing = processing;
        if (processing) {
            setButtonsEnabled(false);
            setStatus(statusMessage);
            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    statusMessage + "開始：UIコンポーネントを無効化");
        } else {
            setButtonsEnabled(true);
            clearStatus();
            updateButtonState();
            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    "ファイル処理完了：UIコンポーネントを有効化");
        }
    }

    /**
     * 取込ボタンのイベントハンドラ
     */
    private void importData() {
        LogHandler.getInstance().log(Level.INFO, LogType.UI, "取込ボタンが押されました");
        if (mainController != null) {
            // MainControllerに処理を移譲するのみに変更
            mainController.handleImportData();
        } else {
            LogHandler.getInstance().log(Level.WARNING, LogType.UI,
                    "MainControllerが設定されていないためCSVインポートできません");
        }
    }

    /**
     * テンプレボタンのイベントハンドラ
     */
    private void loadTemplate() {
        LogHandler.getInstance().log(Level.INFO, LogType.UI, "テンプレートボタンが押されました");
        if (mainController != null) {
            mainController.handleEvent("TEMPLATE", null);
        }
    }

    /**
     * 出力ボタンのイベントハンドラ
     */
    private void exportData() {
        if (mainController != null) {
            LogHandler.getInstance().log(Level.INFO, LogType.UI, "出力ボタンが押されました");

            if (!selectedEngineerIds.isEmpty()) {
                List<EngineerDTO> selectedEngineers = getSelectedEngineers();

                List<String> selectedTargets = selectedEngineers.stream()
                        .map(engineer -> engineer.getId() + " : " + engineer.getName())
                        .collect(Collectors.toList());

                LogHandler.getInstance().log(Level.INFO, LogType.UI,
                        String.format("%d件の項目が出力対象に選択されました", selectedEngineers.size()));

                boolean confirmed = DialogManager.getInstance()
                        .showScrollableListDialog("CSV出力確認", "以下の項目を出力しますか？", selectedTargets);

                if (confirmed) {
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "CSV出力確認が承認されました");

                    mainController.handleEvent("EXPORT_CSV", selectedEngineers);
                } else {
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "CSV出力確認がキャンセルされました");
                }
            }
        }
    }

    /**
     * 削除中状態フラグを取得
     */
    public boolean isDeleting() {
        return deleting;
    }

    /**
     * テーブルで選択された行を削除対象として確認ダイアログを表示し、
     * ユーザーが確認すれば削除処理を開始する
     */
    private void deleteSelectedRow() {
        if (!selectedEngineerIds.isEmpty()) {
            List<EngineerDTO> selectedEngineers = getSelectedEngineers();

            deleting = true;
            updateButtonState();
            mainController.getScreenController().setRegisterButtonEnabled(false);

            List<String> deleteTargets = selectedEngineers.stream()
                    .map(engineer -> engineer.getId() + " : " + engineer.getName())
                    .collect(Collectors.toList());

            boolean confirmed = DialogManager.getInstance()
                    .showScrollableListDialog("削除確認", "以下の情報を削除します。よろしいですか？", deleteTargets);

            if (confirmed) {
                mainController.handleEvent("DELETE_ENGINEER", selectedEngineers);
            } else {
                deleting = false;
                setButtonsEnabled(true);
                mainController.getScreenController().setRegisterButtonEnabled(true);
            }

            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    String.format("%d件の項目が削除対象に選択されました", selectedEngineers.size()));
        }
    }

    /**
     * 詳細画面を開く
     */
    private void openDetailView() {
        EngineerDTO selectedEngineer = getSelectedEngineer();
        if (selectedEngineer != null && mainController != null) {
            mainController.handleEvent("VIEW_DETAIL", selectedEngineer.getId());
            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    String.format("エンジニア詳細表示: ID=%s, 氏名=%s",
                            selectedEngineer.getId(), selectedEngineer.getName()));
        } else {
            LogHandler.getInstance().log(Level.WARNING, LogType.UI,
                    "詳細表示に失敗: エンジニアが選択されていないか、コントローラーが設定されていません");
        }
    }

    /**
     * ステータスラベルにメッセージを表示する
     */
    public void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    /**
     * ステータスラベルをクリアして空にする
     */
    public void clearStatus() {
        if (statusLabel != null) {
            statusLabel.setText("");
        }
    }

    /**
     * 削除やCSV操作に関するボタンを一括で有効/無効にする
     */
    public void setButtonsEnabled(boolean enabled) {
        importButton.setEnabled(enabled);
        templateButton.setEnabled(enabled);
        exportButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
    }

    /**
     * テーブルの選択状態や削除中フラグに応じてボタンの状態を更新する
     */
    public void updateButtonState() {
        if (deleting) {
            setButtonsEnabled(false);
            return;
        }

        boolean hasSelection = !selectedEngineerIds.isEmpty();
        deleteButton.setEnabled(hasSelection);
        exportButton.setEnabled(hasSelection);

        importButton.setEnabled(true);
        templateButton.setEnabled(true);
    }

    /**
     * 削除処理完了後に呼び出され、削除中フラグを解除しボタン状態をリセットする
     */
    public void onDeleteCompleted() {
        deleting = false;
        selectedEngineerIds.clear();
        table.clearSelection();
        updateButtonState();
        mainController.getScreenController().setRegisterButtonEnabled(true);
    }

    /**
     * 一覧画面が次回表示されたときにデータを再読み込みする必要があるかを示すフラグを設定
     */
    public static void setNeedsRefresh(boolean flag) {
        needsRefresh = flag;
    }

    /**
     * 一覧画面の再描画が必要かどうかを返します
     */
    public static boolean isRefreshNeeded() {
        return needsRefresh;
    }

    /**
     * 一覧画面が表示されたときに呼び出される処理
     */
    public void onScreenShown() {
        if (needsRefresh && mainController != null) {
            try {
                List<EngineerDTO> latestData = mainController.getEngineerList();
                setEngineerData(latestData);
                needsRefresh = false;

                updatePageLabel(latestData.size());
                updatePaginationButtons(latestData.size());
                updateButtonState();
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "一覧画面を再描画（Controller経由）");
            } catch (Exception _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "一覧画面の再描画に失敗", _e);
            }
        }
    }

    /**
     * メインコントローラーを設定
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}