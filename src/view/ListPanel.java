package view;

import model.EngineerDTO;
import util.LogHandler;
import util.LogHandler.LogType;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.util.Iterator;
import controller.MainController;
import java.awt.*;
import java.text.Collator;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;

/**
 * エンジニア一覧を表示するパネルクラス
 * ページング、ソート、検索、追加、取込、削除機能
 *
 * @author Nakano
 * @version 4.9.7
 * @since 2025-05-29
 */
public class ListPanel extends JPanel {

    /** テーブルのカラム名 */
    private static final String[] COLUMN_NAMES = {
            "社員ID", "氏名", "生年月日", "エンジニア歴", "扱える言語"
    };

    /** テーブルコンポーネント */
    private final JTable table;

    /** テーブルモデル */
    private final DefaultTableModel tableModel;

    /** 検索フィールド */
    private final JTextField searchField;

    /** ページネーション関連コンポーネント */
    private final JLabel pageLabel;
    private final JButton prevButton;
    private final JButton nextButton;

    // ボタンのフィールド化
    private JButton importButton;
    private JButton templateButton;
    private JButton exportButton;
    private JButton deleteButton;

    // 処理中表示用ラベル
    private JLabel statusLabel;
    // 削除中状態フラグ
    private boolean deleting = false;

    // 一覧画面が次回表示時に再描画すべきかどうかのフラグ
    private static boolean needsRefresh = false;

    /** ページサイズ（1ページあたりの表示件数） */
    private final int pageSize = 100;

    /** 現在のページ番号 */
    private int currentPage = 1;

    /** 全エンジニアデータ */
    private List<EngineerDTO> allData;

    // currentDisplayDataフィールドを削除

    // 追加するフィールド - ソートとフィルタの状態のみ保持
    private String searchId = "";
    private String searchName = "";
    private String searchYear = "";
    private String searchMonth = "";
    private String searchDay = "";
    private String searchCareer = "";

    /** 検索用フィールド */
    private JTextField idField;
    private JTextField nameField;
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

    /** メインコントローラー参照 */
    private MainController mainController;

    /** ViewがControllerへの参照を持つためのセッター */
    public void setController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * コンストラクタ
     * パネルの初期化とUIコンポーネントの配置を行います
     */
    public ListPanel() {
        super(new BorderLayout());

        // データ初期化
        this.allData = new ArrayList<>();

        // テーブルモデルとテーブルの作成
        this.tableModel = createTableModel();
        this.table = createTable();

        // 検索フィールド
        this.searchField = new JTextField(20);

        // ページネーションコンポーネント
        this.pageLabel = new JLabel("ページ: 0 / 0");
        this.prevButton = new JButton("前へ");
        this.nextButton = new JButton("次へ");

        // 初期化処理を実行
        initialize();

    }

    /**
     * パネルを初期化
     * UIコンポーネントの配置と初期設定を行います
     */
    private void initialize() {
        // 上部パネル（タイトルと操作ボタン）
        add(createTopPanel(), BorderLayout.NORTH);

        // 中央部（テーブル）
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // 下部パネル（ページネーション）
        add(createBottomPanel(), BorderLayout.SOUTH);

        // ページネーションのイベント設定
        setupPaginationEvents();

        // ソート設定とイベント登録
        configureSorter();

        // テーブルのダブルクリックイベント設定を追加
        setupTableEvents();

        // ロギング
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "エンジニア一覧画面を初期化しました");
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
     * 年のコンボボックス用データを生成
     * 
     * @return 年のリスト
     */
    private String[] getYears() {
        String[] years = new String[87];
        years[0] = "";
        DecimalFormat df = new DecimalFormat("0");
        int startYear = 1940; // 1940年から2025年
        for (int yearIndex = 1; yearIndex < years.length; yearIndex++) {
            years[yearIndex] = df.format(startYear + (yearIndex - 1));
        }
        return years;
    }

    /**
     * 月のコンボボックス用データを生成
     * 
     * @return 月のリスト
     */
    private String[] getMonths() {
        String[] months = new String[13]; // 空白 + 1～12の月を追加
        months[0] = ""; // 最初の選択肢は空白
        DecimalFormat df = new DecimalFormat("0"); // 小数点なしで整数部分のみを表示
        for (int monthValue = 1; monthValue <= 12; monthValue++) {
            months[monthValue] = df.format(monthValue); // 1, 2, ..., 12
        }
        return months;
    }

    /**
     * 日のコンボボックス用データを生成
     * 
     * @return 日のリスト
     */
    private String[] getDays() {
        String[] days = new String[32]; // 空白 + 1～31の日付を追加
        days[0] = ""; // 最初の選択肢は空白
        // DecimalFormatを使って整数形式で設定
        DecimalFormat df = new DecimalFormat("0");
        for (int i = 1; i <= 31; i++) {
            days[i] = df.format(i); // 1, 2, ..., 31 を設定
        }
        return days;
    }

    /**
     * エンジニア歴のコンボボックス用データを生成
     * 
     * @return エンジニア歴のリスト
     */
    private String[] getCareerYears() {
        // 0から20までの整数値（計21要素）
        String[] careers = new String[51]; // 21要素 + 空の選択肢用の1要素
        // 最初の要素は空（選択なし）
        careers[0] = "";
        // DecimalFormatを使用して一貫した表示形式を保証
        DecimalFormat df = new DecimalFormat("0");
        // 0から20まで整数値を設定
        for (int i = 1; i <= 50; i++) {
            careers[i] = df.format(i);
        }
        return careers;
    }

    /**
     * 上部パネルを作成
     *
     * @return 上部パネル
     */
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new GridLayout(2, 1)); // 2行1列のレイアウト

        // ボタン群パネル（新規追加、取込、テンプレ、出力、削除ボタン）
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 各ボタンをボタンパネルに追加
        JButton addButton = new JButton("新規追加");
        addButton.addActionListener(_e -> addNewEngineer());
        buttonPanel.add(addButton);

        this.importButton = new JButton("取込");
        this.importButton.addActionListener(_e -> importData());
        buttonPanel.add(importButton);

        this.templateButton = new JButton("テンプレ");
        this.templateButton.addActionListener(_e -> loadTemplate());
        buttonPanel.add(templateButton);

        this.exportButton = new JButton("出力");
        this.exportButton.setEnabled(false); // 初期状態は無効
        this.exportButton.addActionListener(_e -> exportData());
        buttonPanel.add(this.exportButton);

        this.deleteButton = new JButton("削除");
        this.deleteButton.setEnabled(false); // 初期状態は無効
        this.deleteButton.addActionListener(_e -> deleteSelectedRow());
        buttonPanel.add(this.deleteButton);

        topPanel.add(buttonPanel); // ボタン群を追加

        // 検索パネル
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 社員ID（テキストボックス）ラベル
        searchPanel.add(new JLabel("社員ID:"));
        idField = new JTextField(10);
        searchPanel.add(idField);

        // 氏名（テキストボックス）
        searchPanel.add(new JLabel("氏名:"));
        nameField = new JTextField(10);
        searchPanel.add(nameField);

        // 生年月日（プルダウン 年・月・日）
        searchPanel.add(new JLabel("生年月日:"));
        yearBox = new JComboBox<>(getYears());
        monthBox = new JComboBox<>(getMonths());
        dayBox = new JComboBox<>(getDays());
        searchPanel.add(yearBox);
        searchPanel.add(new JLabel("年"));
        searchPanel.add(monthBox);
        searchPanel.add(new JLabel("月"));
        searchPanel.add(dayBox);
        searchPanel.add(new JLabel("日"));

        // エンジニア歴（プルダウン）
        searchPanel.add(new JLabel("エンジニア歴:"));
        careerBox = new JComboBox<>(getCareerYears());
        searchPanel.add(careerBox);

        // 検索ボタン
        JButton searchButton = new JButton("検索");
        searchButton.addActionListener(e -> search(
                idField.getText(),
                nameField.getText(),
                yearBox.getSelectedItem().toString(),
                monthBox.getSelectedItem().toString(),
                dayBox.getSelectedItem().toString(),
                careerBox.getSelectedItem().toString()));
        searchPanel.add(searchButton);

        topPanel.add(searchPanel); // 検索パネルを追加

        return topPanel;
    }

    /**
     * 下部パネル（ページネーション）を作成
     *
     * @return 下部パネル
     */
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        // 中央にボタン群
        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        navigationPanel.add(prevButton);
        navigationPanel.add(pageLabel);
        navigationPanel.add(nextButton);

        // 右にステータスラベル
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5)); // ← 横余白10ピクセル
        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusPanel.add(statusLabel);

        bottomPanel.add(navigationPanel, BorderLayout.CENTER);
        bottomPanel.add(statusPanel, BorderLayout.EAST);

        return bottomPanel;
    }

    /**
     * ページネーションのイベント設定
     */
    private void setupPaginationEvents() {
        prevButton.addActionListener(_e -> changePage(-1));
        nextButton.addActionListener(_e -> changePage(1));
    }

    /**
     * テーブルの各列に対するソートの有効/無効を設定し、
     * ヘッダークリックによるソートイベントを設定します。
     * 「扱える言語」列（インデックス4）はソート対象外。
     */
    private void configureSorter() {
        // ソート可能な列を設定
        for (int columnNumber = 0; columnNumber < COLUMN_NAMES.length; columnNumber++) {
            sorter.setSortable(columnNumber, columnNumber != COLUMN_INDEX_PROGRAMMING_LANGUAGES); // 「扱える言語」はソート対象外
        }

        // ソート可能な列のヘッダーにマウスリスナーを追加
        //// MouseAdapterのAPI仕様により、ここでメソッド（mouseClicked）をオーバーライドして実装しています。
        // このメソッドはMouseListenerインターフェースのコールバックとして必要です。
        this.table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int columnIndex = ListPanel.this.table.columnAtPoint(e.getPoint());
                sortByColumn(columnIndex);
            }
        });
    }

    /**
     * 指定された列インデックスに基づいてエンジニア情報のリストをソートします。
     *
     * @param columnIndex ソート対象の列インデックス（0:社員ID, 1:氏名かな, 2:生年月日, 3:経験年数）
     */

    private void sortByColumn(int columnIndex) {
        try {
            // ソート可能な列かチェック
            if (columnIndex < 0 || columnIndex >= COLUMN_NAMES.length
                    || columnIndex == COLUMN_INDEX_PROGRAMMING_LANGUAGES) {
                return; // 「扱える言語」(インデックス4)はソート対象外
            }

            // 選択状態をクリア
            this.selectedEngineerIds.clear();
            this.table.clearSelection();

            // 同じ列の場合は昇順/降順を切り替え、異なる列の場合は昇順に設定
            if (this.lastSortedColumn == columnIndex) {
                this.isAscending = !this.isAscending;
            } else {
                this.isAscending = true;
                this.lastSortedColumn = columnIndex;
            }

            // ログ出力：ソート対象と順序
            LogHandler.getInstance().log(
                    Level.INFO,
                    LogType.UI,
                    String.format("ソート実行: 列=%s, 順序=%s", COLUMN_NAMES[columnIndex], isAscending ? "昇順" : "降順"));

            // ソート条件を保存し、表示データを更新 修正
            List<EngineerDTO> displayData = getDisplayData();
            if (displayData == null || displayData.isEmpty()) {
                // データがない場合の処理
                updateTableData(new ArrayList<>()); // 空リストでテーブルをクリア
                updateButtonState();
                return;
            }

            // ページを1ページ目にリセット
            this.currentPage = 1;

            // テーブルを更新
            updateTableData(displayData);

            // ボタンの状態を更新
            updateButtonState();

            // UIのソートインジケータを設定（見た目の矢印）
            this.sorter.setSortKeys(List.of(new RowSorter.SortKey(columnIndex,
                    this.isAscending ? SortOrder.ASCENDING : SortOrder.DESCENDING)));

            // TableRowSorterによる内部ソートを無効化（Comparatorを常に0にすることで全て同じとみなす）
            this.sorter.setComparator(columnIndex, (o1, o2) -> 0);

            LogHandler.getInstance().log(
                    Level.INFO,
                    LogType.SYSTEM,
                    String.format("ソート完了: %d件", displayData.size()));
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "ソート処理中にエラーが発生しました", e);
        }
    }

    /**
     * 現在のソート条件に基づいてエンジニアリストをソートする
     * 
     * @param engineers   ソート対象のエンジニアリスト
     * @param columnIndex ソート対象の列インデックス
     * @param ascending   昇順でソートするか
     * @return ソート済みのエンジニアリスト
     * 
     */
    private List<EngineerDTO> sortEngineers(List<EngineerDTO> engineers, int columnIndex, boolean ascending) {
        Comparator<EngineerDTO> comparator = null;

        switch (columnIndex) {
            case COLUMN_INDEX_EMPLOYEE_ID: // ID
                comparator = Comparator.comparing(e -> parseNumericId(e.getId()),
                        Comparator.nullsLast(Integer::compareTo));
                break;
            case COLUMN_INDEX_NAME: // 氏名
                comparator = Comparator.comparing(EngineerDTO::getNameKana,
                        getJapaneseKanaComparator());
                break;
            case COLUMN_INDEX_BIRTHDATE: // 生年月日
                comparator = Comparator.comparing(EngineerDTO::getBirthDate,
                        Comparator.nullsLast(LocalDate::compareTo));
                break;
            case COLUMN_INDEX_CAREER: // エンジニア歴
                comparator = Comparator.comparingInt(EngineerDTO::getCareer);
                break;
            default:
                return new ArrayList<>(engineers);
        }

        // ID順を2次ソートに使用
        Comparator<EngineerDTO> idComparator = Comparator
                .comparing((EngineerDTO e) -> parseNumericId(e.getId()));

        Comparator<EngineerDTO> finalComparator = ascending
                ? comparator.thenComparing(idComparator)
                : comparator.reversed().thenComparing(idComparator);

        List<EngineerDTO> sorted = new ArrayList<>(engineers);
        sorted.sort(finalComparator);

        return sorted;
    }

    // IDの数値変換
    private int parseNumericId(String id) {
        try {
            return Integer.parseInt(id.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * 日本語のかな順（あいうえお順）で文字列を比較する Comparator を返します。
     *
     * @return 日本語のかな順に従う Comparator
     */
    private Comparator<String> getJapaneseKanaComparator() {
        Collator collator = Collator.getInstance(Locale.JAPANESE);
        return (s1, s2) -> {
            if (s1 == null && s2 == null)
                return 0;
            if (s1 == null)
                return 1; // nullは後ろ
            if (s2 == null)
                return -1; // nullは前
            return collator.compare(s1, s2);
        };
    }

    /**
     * 検索条件に基づいてデータをフィルタリング
     * 
     * @param id     社員ID
     * @param name   氏名
     * @param year   生年月日（年）
     * @param month  生年月日（月）
     * @param day    生年月日（日）
     * @param career エンジニア歴
     */
    private void search(String id, String name, String year, String month, String day, String career) {
        // 検索条件を保存
        this.searchId = id;
        this.searchName = name;
        this.searchYear = year;
        this.searchMonth = month;
        this.searchDay = day;
        this.searchCareer = career;

        // 現在のページを1にリセット
        currentPage = 1;

        // 検索条件を適用して表示データを取得
        List<EngineerDTO> filteredData = getDisplayData();

        // 選択状態をクリア
        selectedEngineerIds.clear(); // 追加
        table.clearSelection(); // 追加

        // テーブルを更新
        updateTableData(filteredData);

        // ボタンの状態を更新
        updateButtonState(); // 追加

        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                String.format("検索実行: %d件のデータがヒット", filteredData.size()));
    }

    /**
     * ページを切り替え
     *
     * @param delta ページ変化量（前：-1、次：+1）
     */
    private void changePage(int delta) {
        // 表示データを取得し、nullまたは空かチェック
        List<EngineerDTO> displayData = getDisplayData();
        if (displayData == null || displayData.isEmpty()) {
            // データがない場合はページ切り替えできないため早期リターン
            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    "データがないためページ切り替えをスキップします");
            updatePageLabel(0); // ページラベルを0件として更新
            updatePaginationButtons(0); // ページネーションボタンを無効化
            return;
        }

        // 総ページ数を計算
        int totalPages = (int) Math.ceil((double) displayData.size() / pageSize);
        int newPage = currentPage + delta;

        // ページ範囲の検証
        if (newPage < 1 || newPage > totalPages) {
            return;
        }

        currentPage = newPage;
        updateTableData(displayData);

        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                String.format("ページを切り替えました: %d / %d", currentPage, totalPages));
    }

    // 統合後のメソッド
    private void updatePageLabel(int dataSize) {
        int totalPages = (int) Math.ceil((double) dataSize / pageSize);
        totalPages = Math.max(1, totalPages);
        pageLabel.setText(String.format("ページ: %d / %d", currentPage, totalPages));
    }

    /**
     * テーブルに1エンジニアのデータを追加
     *
     * @param engineer 追加するエンジニアデータ
     */
    private void addEngineerToTable(EngineerDTO engineer) {
        if (engineer == null) {
            return;
        }

        // 言語リストを文字列に変換
        String languages = "";
        if (engineer.getProgrammingLanguages() != null) {
            languages = String.join(", ", engineer.getProgrammingLanguages());
        }

        // テーブルに行を追加
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
     * この方法でControllerからデータを受け取ります
     *
     * @param engineers エンジニア一覧データ
     */
    public void setEngineerData(List<EngineerDTO> engineers) {
        // データの設定
        this.allData = new ArrayList<>(engineers);

        // 現在のソート条件を保持
        List<EngineerDTO> displayData = getDisplayData();

        // ページ情報の初期化
        currentPage = 1;

        // テーブルとページネーションを更新
        updateTableData(displayData);

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                String.format("エンジニアデータを更新しました: %d件", allData.size()));
    }

    /**
     * エンジニアデータの追加
     * 既存データに新しいエンジニアを追加します
     *
     * @param engineer 追加するエンジニア
     */
    public void addEngineerData(EngineerDTO engineer) {
        if (engineer == null) {
            return;
        }

        // 既存IDチェック - 同じIDがあれば置き換え
        boolean replaced = false;
        for (int i = 0; i < allData.size(); i++) {
            if (allData.get(i).getId().equals(engineer.getId())) {
                allData.set(i, engineer);
                replaced = true;
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        String.format("エンジニア情報を更新しました: ID=%s, 氏名=%s",
                                engineer.getId(), engineer.getName()));
                break;
            }
        }

        // 新規追加の場合
        if (!replaced) {
            allData.add(engineer);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("エンジニアを追加しました: ID=%s, 氏名=%s",
                            engineer.getId(), engineer.getName()));
        }

        // 現在のソート・フィルタ条件を適用して表示を更新
        List<EngineerDTO> displayData = getDisplayData();
        updateTableData(displayData);
    }

    // 追加メソッド - 表示用データの取得（ソート・フィルタを適用）
    private List<EngineerDTO> getDisplayData() {
        // allDataがnullの場合は空のリストを返す
        if (allData == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "基本データがnullのため、空のリストを返します");
            return new ArrayList<>();
        }

        // 基本データのコピーを作成
        List<EngineerDTO> result = new ArrayList<>(allData);

        // 検索条件が設定されている場合はフィルタリング
        if (!searchId.isEmpty() || !searchName.isEmpty() || !searchYear.isEmpty() ||
                !searchMonth.isEmpty() || !searchDay.isEmpty() || !searchCareer.isEmpty()) {

            result = result.stream()
                    .filter(engineer -> filterEngineer(engineer))
                    .collect(Collectors.toList());
        }

        // ソート条件が設定されている場合はソート
        if (this.lastSortedColumn >= 0) {
            result = this.sortEngineers(result, this.lastSortedColumn, this.isAscending);
        }

        return result; // nullではなく少なくとも空のリストを返す
    }

    // 追加メソッド - エンジニアがフィルタ条件に一致するかチェック
    private boolean filterEngineer(EngineerDTO engineer) {
        // ID検索
        if (!searchId.isEmpty() && (engineer.getId() == null ||
                !engineer.getId().toLowerCase().contains(searchId.toLowerCase()))) {
            return false;
        }

        // 名前検索
        if (!searchName.isEmpty() && (engineer.getName() == null ||
                !engineer.getName().toLowerCase().contains(searchName.toLowerCase()))) {
            return false;
        }

        // 生年月日検索
        if (engineer.getBirthDate() != null) {
            String birthDateStr = engineer.getBirthDate().toString();

            // 年のチェック
            if (!searchYear.isEmpty() && !birthDateStr.startsWith(searchYear)) {
                return false;
            }

            // 月のチェック
            if (!searchMonth.isEmpty()) {
                String monthPart = searchMonth.length() == 1 ? "0" + searchMonth : searchMonth;
                if (!birthDateStr.substring(5, 7).equals(monthPart)) {
                    return false;
                }
            }

            // 日のチェック
            if (!searchDay.isEmpty()) {
                String dayPart = searchDay.length() == 1 ? "0" + searchDay : searchDay;
                if (!birthDateStr.substring(8, 10).equals(dayPart)) {
                    return false;
                }
            }
        } else if (!searchYear.isEmpty() || !searchMonth.isEmpty() || !searchDay.isEmpty()) {
            // 生年月日がnullで検索条件が指定されている場合
            return false;
        }

        // エンジニア歴検索
        if (!searchCareer.isEmpty()) {
            try {
                int careerValue = engineer.getCareer();
                int searchCareerValue = Integer.parseInt(searchCareer);
                if (careerValue != searchCareerValue) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // 数値変換エラーは無視
            }
        }

        return true;
    }

    /**
     * 現在選択されているエンジニアを取得
     *
     * @return 選択されているエンジニア、未選択時はnull
     */
    public EngineerDTO getSelectedEngineer() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            return null;
        }

        // 表示用のデータ（検索・ソート反映済み）
        List<EngineerDTO> displayData = getDisplayData();

        // データがnullまたは空の場合は早期リターン
        if (displayData == null || displayData.isEmpty()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.UI,
                    "表示データがないため選択エンジニアを取得できません");
            return null;
        }

        // 選択行のモデル上のインデックスに変換
        int modelRow = table.convertRowIndexToModel(selectedRow);

        // 現在のページの先頭インデックス
        int startIndex = (currentPage - 1) * pageSize;

        // データリスト上のインデックス
        int dataIndex = startIndex + modelRow;

        // インデックスが有効範囲内かチェック
        if (dataIndex >= 0 && dataIndex < displayData.size()) {
            return displayData.get(dataIndex);
        }

        return null;
    }

    // 追加メソッド - テーブルデータとページネーションを一括更新
    private void updateTableData(List<EngineerDTO> data) {
        // テーブルデータのクリア
        tableModel.setRowCount(0);

        // 表示範囲の計算
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, data.size());

        // データの追加
        for (int i = startIndex; i < endIndex; i++) {
            addEngineerToTable(data.get(i));
        }

        // ページネーション情報の更新
        updatePageLabel(data.size());
        updatePaginationButtons(data.size());
        // ページまたぎ選択復元
        restoreRowSelectionById();
    }

    // ページネーションボタンの状態を更新（オーバーロード）
    private void updatePaginationButtons(int dataSize) {
        int totalPages = (int) Math.ceil((double) dataSize / pageSize);
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
    }

    /**
     * 現在選択されているエンジニアリストを取得
     *
     * @return 選択されているエンジニアリスト、未選択時は空リスト
     */
    public List<EngineerDTO> getSelectedEngineers() {
        // 選択されたエンジニアを格納するリスト
        List<EngineerDTO> selectedEngineers = new ArrayList<>();

        // 表示データを取得
        List<EngineerDTO> displayData = getDisplayData();

        // データがnullまたは空の場合は空リストを返す
        if (displayData == null || displayData.isEmpty()) {
            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    "表示データがないため選択エンジニアリストは空です");
            return selectedEngineers; // 空リスト
        }

        // 重複追加防止用のIDセット
        Set<String> uniqueIds = new HashSet<>();

        // 表示中のデータから選択状態のエンジニアのみ抽出
        for (EngineerDTO engineerDTO : displayData) {
            // 選択IDセットに含まれていて、かつまだ追加していないIDのみリストに追加
            if (this.selectedEngineerIds.contains(engineerDTO.getId()) && uniqueIds.add(engineerDTO.getId())) {
                selectedEngineers.add(engineerDTO);
            }
        }

        // 選択されたエンジニアのリストを返す（未選択時は空リスト）
        return selectedEngineers;
    }

    /**
     * テーブルの選択状態を管理する
     * 
     * <p>
     * 以下の処理を行います：
     * 1. 現在のページで選択されている行のエンジニアIDを収集
     * 2. 通常クリック時は既存の選択をクリアし、新しい選択のみを反映
     * 3. Ctrl/Shiftキーによる複数選択時は、既存の選択に追加
     * 4. 選択件数が100件を超える場合は警告を表示
     * </p>
     * 
     * @param isCtrlDown  Ctrlキーが押されているか
     * @param isShiftDown Shiftキーが押されているか
     */
    private void updateSelectedEngineerIds(boolean isCtrlDown, boolean isShiftDown) {
        int startIndex = (this.currentPage - 1) * this.pageSize;
        List<EngineerDTO> displayData = getDisplayData();
        if (displayData == null || displayData.isEmpty()) {
            updateTableData(new ArrayList<>());
            updateButtonState();
            return;
        }
        int previousSelectionCount = this.selectedEngineerIds.size();

        // 選択された行のIDを収集
        Set<String> newlySelectedIds = collectSelectedIds(displayData, startIndex, isShiftDown);
        // 現在ページのIDを収集
        Set<String> currentPageIds = getCurrentPageIds(displayData, startIndex);
        // 現在ページで選択解除されたIDを削除
        removeUnselectedIds(currentPageIds, newlySelectedIds);

        // Ctrl/Shiftキーが押されていない場合は、既存の選択をクリア
        if (!isCtrlDown && !isShiftDown) {
            clearSelection();
            LogHandler.getInstance().log(
                    Level.INFO,
                    LogType.UI,
                    "通常クリックによる選択: 既存の選択はクリア");
        }
        // Ctrl/Shiftキーが押されている場合は、選択を追加
        this.selectedEngineerIds.addAll(newlySelectedIds);
        // 最後に選択した行を記憶
        // updateLastSelectedRowIndex();
        int currentSelectionCount = selectedEngineerIds.size();
        // 選択件数をログ出力
        if (currentSelectionCount != previousSelectionCount) {
            LogHandler.getInstance().log(
                    Level.INFO,
                    LogType.UI,
                    String.format("選択件数が変更されました: %d → %d件", previousSelectionCount, currentSelectionCount));
        }
        // 選択件数が上限を超えた場合の警告
        checkSelectionLimit(currentSelectionCount);
    }

    /**
     * 現在ページで選択された行のIDを収集する。
     * <p>
     * Shiftキーが押されている場合は範囲選択、そうでない場合は個別選択となる。
     * </p>
     *
     * @param displayData 表示中のエンジニアデータリスト
     * @param startIndex  現在ページの先頭インデックス
     * @param isShiftDown Shiftキーが押されているか（範囲選択の場合true）
     * @return 選択されたエンジニアIDのセット
     */
    private Set<String> collectSelectedIds(List<EngineerDTO> displayData, int startIndex, boolean isShiftDown) {
        Set<String> newlySelectedIds = new HashSet<>();
        int[] selectedRows = this.table.getSelectedRows();

        if (isShiftDown && selectedRows.length > 0) {
            // Shiftキーが押されている場合は、開始行と終了行の範囲をすべて選択対象とする
            int startRow = this.table.getSelectionModel().getAnchorSelectionIndex(); // 範囲選択の開始行
            int endRow = this.table.getSelectionModel().getLeadSelectionIndex(); // 範囲選択の終了行
            int fromRow = Math.min(startRow, endRow);
            int toRow = Math.max(startRow, endRow);
            // fromRow～toRow間の全行を選択対象IDに追加
            for (int viewRowIndex = fromRow; viewRowIndex <= toRow; viewRowIndex++) {
                int modelRow = this.table.convertRowIndexToModel(viewRowIndex);
                int dataIndex = startIndex + modelRow;
                // データ範囲外を除外
                if (dataIndex >= 0 && dataIndex < displayData.size()) {
                    newlySelectedIds.add(displayData.get(dataIndex).getId());
                }
            }
        } else {
            // 通常クリックまたはCtrlキーによる個別選択の場合
            for (int selectedRow : selectedRows) {
                int modelRow = this.table.convertRowIndexToModel(selectedRow);
                int dataIndex = startIndex + modelRow;
                // データ範囲外を除外
                if (dataIndex >= 0 && dataIndex < displayData.size()) {
                    newlySelectedIds.add(displayData.get(dataIndex).getId());
                }
            }
        }
        return newlySelectedIds;
    }

    /**
     * 現在ページに表示されているエンジニアIDのセットを作成する。
     *
     * @param displayData 表示中のエンジニアデータリスト
     * @param startIndex  現在ページの先頭インデックス
     * @return 現在ページに表示されているエンジニアIDのセット
     */
    private Set<String> getCurrentPageIds(List<EngineerDTO> displayData, int startIndex) {
        return displayData.stream()
                .skip(startIndex)
                .limit(this.pageSize)
                .map(EngineerDTO::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 現在のページで選択解除されたIDのみを選択IDセットから削除する。
     *
     * @param currentPageIds   現在ページに表示されているエンジニアIDのセット
     * @param newlySelectedIds 今回新たに選択されたエンジニアIDのセット
     */
    private void removeUnselectedIds(Set<String> currentPageIds, Set<String> newlySelectedIds) {
        Iterator<String> selectedIditerator = this.selectedEngineerIds.iterator();
        while (selectedIditerator.hasNext()) {
            // 選択されたIDを取得
            String id = selectedIditerator.next();
            // 現在ページに存在し、かつ新たに選択されていないIDを削除
            // 選択解除されたIDを削除します。
            if (currentPageIds.contains(id) && !newlySelectedIds.contains(id)) {
                selectedIditerator.remove();
            }
        }
    }

    // 選択状態をクリア
    private void clearSelection() {
        this.selectedEngineerIds.clear();
    }

    // 選択件数が上限を超えた場合の警告
    private void checkSelectionLimit(int currentSelectionCount) {
        if (currentSelectionCount > 100) {
            DialogManager.getInstance().showWarningDialog(
                    "選択数が上限を超えています",
                    String.format("%d件選択しています。一度に選択できるのは100件以下です。", currentSelectionCount));
            LogHandler.getInstance().log(
                    Level.WARNING,
                    LogType.UI,
                    String.format("選択件数が上限を超えています: %d件", currentSelectionCount));
        }
    }

    /**
     * ページ切り替えやテーブル再描画時に、選択IDに基づいて
     * テーブルの選択状態（青背景）を復元する。
     */
    private void restoreRowSelectionById() {
        // 現在ページの先頭インデックスを計算
        // 例: 1ページ目なら0、2ページ目なら100（pageSize=100の場合）となり、
        // 全体データリストのどこから表示を始めるかを決定する
        int startIndex = (this.currentPage - 1) * this.pageSize;
        // 検索・ソート・フィルタ済みの表示用データを取得
        List<EngineerDTO> displayData = getDisplayData();
        if (displayData == null || displayData.isEmpty()) {
            return; // データがない場合は何もしない
        }
        // 現在ページに表示されている行を順にチェック
        for (int rowIndex = 0; rowIndex < this.pageSize && (startIndex + rowIndex) < displayData.size(); rowIndex++) {
            // ページ内rowIndex番目のエンジニアDTOを取得
            EngineerDTO engineerDTO = displayData.get(startIndex + rowIndex);
            // 選択IDセットに含まれていれば、その行を選択状態にする
            if (this.selectedEngineerIds.contains(engineerDTO.getId())) {
                this.table.addRowSelectionInterval(rowIndex, rowIndex);
            }
        }
    }

    /**
     * データ件数を取得
     *
     * @return 読み込まれているエンジニアデータの総数
     */
    public int getDataCount() {
        return allData.size();
    }

    /**
     * 現在のページ番号を取得
     *
     * @return 現在表示中のページ番号
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * テーブルコンポーネントを取得
     *
     * @return テーブルコンポーネント
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
     * 取込ボタンのイベントハンドラ
     */
    private void importData() {
        LogHandler.getInstance().log(Level.INFO, LogType.UI, "取込ボタンが押されました");
        if (mainController != null) {
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
        // テンプレート出力処理
        if (mainController != null) {
            mainController.handleEvent("TEMPLATE", null); // ← 文字列イベント名で委譲
        }
    }

    /**
     * 出力ボタンのイベントハンドラ
     */
    private void exportData() {
        if (mainController != null) {
            LogHandler.getInstance().log(Level.INFO, LogType.UI, "出力ボタンが押されました");
            // CSV出力処理
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length > 0) {
                // 選択されたエンジニアリストを取得
                List<EngineerDTO> selectedEngineers = getSelectedEngineers();

                //ボタンの無効化
                setButtonsEnabled(false);
                mainController.getScreenController().setRegisterButtonEnabled(false); // 登録ボタンも無効化

                // 選択対象のリストを作成
                List<String> selectedTargets = selectedEngineers.stream()
                        .map(engineer -> engineer.getId() + " : " + engineer.getName())
                        .collect(Collectors.toList());

                LogHandler.getInstance().log(Level.INFO, LogType.UI,
                String.format("%d件の行が出力対象に選択されました", selectedRows.length));

                // 確認ダイアログを表示
                boolean confirmed = DialogManager.getInstance()
                        .showScrollableListDialog("CSV出力確認", "以下の項目を出力しますか？", selectedTargets);

                if (confirmed) {
                    //CSV出力項目許可のログ表示
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "CSV出力確認が承認されました");
                    // CSV出力実行
                    mainController.handleEvent("EXPORT_CSV", selectedEngineers);
                } else {
                    //CSV出力確認キャンセルのログ表示
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                "CSV出力確認がキャンセルされました");
                    // キャンセルされた場合、ボタンを再度有効化
                    setButtonsEnabled(true);
                    mainController.getScreenController().setRegisterButtonEnabled(true);
                }
            }
        }
    }

    // 削除中状態フラグ
    public boolean isDeleting() {
        return deleting;
    }

    /**
     * テーブルで選択された行を削除対象として確認ダイアログを表示し、
     * ユーザーが確認すれば削除処理を開始する。
     */
    private void deleteSelectedRow() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length > 0) {
            // 選択されたエンジニアリストを取得
            List<EngineerDTO> selectedEngineers = getSelectedEngineers();
            // ボタンをすぐ無効化
            deleting = true; // 削除中フラグセット
            updateButtonState();
            mainController.getScreenController().setRegisterButtonEnabled(false); // 登録ボタンも無効化

            // 削除対象のリストを作成
            List<String> deleteTargets = selectedEngineers.stream()
                    .map(engineer -> engineer.getId() + " : " + engineer.getName())
                    .collect(Collectors.toList());

            // 確認ダイアログを表示
            boolean confirmed = DialogManager.getInstance()
                    .showScrollableListDialog("削除確認", "以下の情報を削除します。よろしいですか？", deleteTargets);

            if (confirmed) {
                // 削除実行
                mainController.handleEvent("DELETE_ENGINEER", selectedEngineers);
            } else {
                deleting = false; // 削除中フラグをリセット
                // キャンセルされた場合、ボタンを再度有効化
                setButtonsEnabled(true);
                mainController.getScreenController().setRegisterButtonEnabled(true);

            }

            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    String.format("%d件の行が削除対象に選択されました", selectedRows.length));
        }
    }

    /**
     * テーブルイベントの設定
     * ダブルクリックによる詳細画面への遷移などを設定します
     */
    private void setupTableEvents() {
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isCtrlPressed = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
                isShiftPressed = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                updateSelectedEngineerIds(isCtrlPressed, isShiftPressed);
                updateButtonState();
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // ダブルクリック時の処理
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        openDetailView();
                    }
                }
            }
        });

        LogHandler.getInstance().log(Level.INFO, LogType.UI, "テーブルのダブルクリックイベントを設定しました");
    }

    /**
     * 詳細画面を開く
     * 選択されたエンジニア情報の詳細画面に遷移します
     */
    private void openDetailView() {
        EngineerDTO selectedEngineer = getSelectedEngineer();
        if (selectedEngineer != null && mainController != null) {
            // 詳細表示イベントを発行
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
     * ステータスラベルにメッセージを表示する。
     *
     * @param message 表示するステータスメッセージ
     */
    public void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    /**
     * ステータスラベルをクリアして空にする。
     */
    public void clearStatus() {
        if (statusLabel != null) {
            statusLabel.setText(""); // ステータスラベルを空にする
        }
    }

    /**
     * 削除やCSV操作に関するボタンを一括で有効/無効にする。
     *
     * @param enabled true で有効、false で無効
     */
    public void setButtonsEnabled(boolean enabled) {
        importButton.setEnabled(enabled);
        templateButton.setEnabled(enabled);
        exportButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
    }

    /**
     * テーブルの選択状態や削除中フラグに応じてボタンの状態を更新する。
     * 削除中は全ての操作ボタンを無効化する。
     */
    public void updateButtonState() {
        if (deleting) {
            setButtonsEnabled(false);
            return;
        }

        // 選択状態を正確に判定
        boolean hasSelection = !selectedEngineerIds.isEmpty();
        deleteButton.setEnabled(hasSelection);
        exportButton.setEnabled(hasSelection);

        // 他のボタンは常に有効
        importButton.setEnabled(true);
        templateButton.setEnabled(true);
    }

    /**
     * 削除処理完了後に呼び出され、削除中フラグを解除しボタン状態をリセットする。
     */
    public void onDeleteCompleted() {
        deleting = false;
        // 選択状態をクリア
        selectedEngineerIds.clear();
        table.clearSelection();
        updateButtonState();
        mainController.getScreenController().setRegisterButtonEnabled(true);
    }

    /**
     * 一覧画面が次回表示されたときにデータを再読み込みする必要があるかを示すフラグを設定。
     *
     * @param flag true の場合は再描画が必要、false の場合は不要
     */
    public static void setNeedsRefresh(boolean flag) {
        needsRefresh = flag;
    }

    /**
     * 一覧画面の再描画が必要かどうかを返します。
     *
     * @return true の場合は再描画が必要、false の場合は不要
     */
    public static boolean isRefreshNeeded() {
        return needsRefresh;
    }

    /**
     * 一覧画面が表示されたときに呼び出される処理。
     * 
     * <p>
     * 「再描画が必要」な場合は、最新のデータを Controller 経由で取得し、画面を更新します。<br>
     * その後、ページ情報やボタン状態もリセットされます。
     * </p>
     */
    public void onScreenShown() {
        if (needsRefresh && mainController != null) {
            try {
                List<EngineerDTO> latestData = mainController.getEngineerList(); // Controller経由で取得
                setEngineerData(latestData);
                needsRefresh = false;

                updatePageLabel(latestData.size());
                updatePaginationButtons(latestData.size());
                updateButtonState();
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "一覧画面を再描画しました（Controller経由）");
            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "一覧画面の再描画に失敗しました", e);
            }
        }

    }

    /**
     * メインコントローラーを設定
     *
     * @param mainController メインコントローラー
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}