package view;

import model.EngineerDTO;
import util.LogHandler;
import util.LogHandler.LogType;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import controller.MainController;
import java.awt.*;
import java.text.Collator;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * エンジニア一覧を表示するパネルクラス
 * ページング、ソート、検索、追加、取込、削除機能
 *
 * @author Bando
 * @version 4.1.0
 * @since 2025-04-16
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

    /** ページサイズ（1ページあたりの表示件数） */
    private final int pageSize = 100;

    /** 現在のページ番号 */
    private int currentPage = 1;

    /** 全エンジニアデータ */
    private List<EngineerDTO> allData;

    /** 検索用フィールド */
    private JTextField idField;
    private JTextField nameField;
    private JComboBox<String> yearBox;
    private JComboBox<String> monthBox;
    private JComboBox<String> dayBox;
    private JComboBox<String> careerBox;

    /** ソート関係 */
    private TableRowSorter<DefaultTableModel> sorter; // ← createTable() の sorter をフィールド化
    private boolean isAscending = true;
    private int lastSortedColumn = -1;
    private List<EngineerDTO> currentDisplayData = new ArrayList<>(); // ソート表示用

    /** メインコントローラー参照 */
    private MainController mainController;

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
        for (int i = 1; i < years.length; i++) {
            years[i] = df.format(startYear + (i - 1));
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
        for (int i = 1; i <= 12; i++) {
            months[i] = df.format(i); // 1, 2, ..., 12
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
        String[] careers = new String[22]; // 21要素 + 空の選択肢用の1要素
        // 最初の要素は空（選択なし）
        careers[0] = "";
        // DecimalFormatを使用して一貫した表示形式を保証
        DecimalFormat df = new DecimalFormat("0");
        // 0から20まで整数値を設定
        for (int i = 0; i <= 20; i++) {
            careers[i + 1] = df.format(i);
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
        addButton.addActionListener(e -> addNewEngineer());
        buttonPanel.add(addButton);

        JButton importButton = new JButton("取込");
        importButton.addActionListener(e -> importData());
        buttonPanel.add(importButton);

        JButton templateButton = new JButton("テンプレ");
        templateButton.addActionListener(e -> loadTemplate());
        buttonPanel.add(templateButton);

        JButton exportButton = new JButton("出力");
        exportButton.addActionListener(e -> exportData());
        buttonPanel.add(exportButton);

        JButton deleteButton = new JButton("削除");
        deleteButton.addActionListener(e -> deleteSelectedRow());
        buttonPanel.add(deleteButton);

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
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // ページ移動ボタン
        prevButton.setEnabled(false); // 初期状態では無効
        nextButton.setEnabled(false); // 初期状態では無効

        bottomPanel.add(prevButton);
        bottomPanel.add(pageLabel);
        bottomPanel.add(nextButton);

        return bottomPanel;
    }

    /**
     * ページネーションのイベント設定
     */
    private void setupPaginationEvents() {
        prevButton.addActionListener(e -> changePage(-1));
        nextButton.addActionListener(e -> changePage(1));
    }

    private void configureSorter() {
        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            sorter.setSortable(i, i != 4); // 「扱える言語」はソート対象外
        }

        table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int columnIndex = table.columnAtPoint(e.getPoint());
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
        Comparator<EngineerDTO> baseComparator;

        try {
            switch (columnIndex) {
                case 0 -> {
                    baseComparator = Comparator.comparing(e -> parseNumericId(e.getId()),
                            Comparator.nullsLast(Integer::compareTo));

                }
                case 1 ->
                    baseComparator = Comparator.comparing(
                            EngineerDTO::getNameKana,
                            getJapaneseKanaComparator());
                case 2 -> baseComparator = Comparator.comparing(EngineerDTO::getBirthDate,
                        Comparator.nullsLast(LocalDate::compareTo));
                case 3 -> baseComparator = Comparator.comparingDouble(EngineerDTO::getCareer);
                default -> {
                    return;
                }
            }

            Comparator<EngineerDTO> idComparator = Comparator
                    .comparing((EngineerDTO e) -> Integer.parseInt(e.getId().replaceAll("\\D+", "")));

            if (lastSortedColumn == columnIndex) {
                isAscending = !isAscending;
            } else {
                isAscending = true;
                lastSortedColumn = columnIndex;
            }

            // ログ出力：ソート対象と順序
            LogHandler.getInstance().log(
                    Level.INFO,
                    LogType.UI,
                    String.format("ソート実行: 列=%s, 順序=%s", COLUMN_NAMES[columnIndex], isAscending ? "昇順" : "降順"));

            Comparator<EngineerDTO> finalComparator = isAscending
                    ? baseComparator.thenComparing(idComparator)
                    : baseComparator.reversed().thenComparing(idComparator);

            // 「全件データ」をソートする
            List<EngineerDTO> sorted = new ArrayList<>(allData);
            sorted.sort(finalComparator);
            currentDisplayData = sorted;

            currentPage = 1;
            updateTableForCurrentPage();
            updatePageLabel();
            updatePaginationButtons();

            // 見た目の矢印は設定
            sorter.setSortKeys(List.of(new RowSorter.SortKey(columnIndex,
                    isAscending ? SortOrder.ASCENDING : SortOrder.DESCENDING)));

            // ↓ TableRowSorter による内部ソートを抑制（Comparatorを機能しないものにする）
            sorter.setComparator(columnIndex, (o1, o2) -> 0);

            LogHandler.getInstance().log(
                    Level.INFO,
                    LogType.SYSTEM,
                    String.format("ソート完了: ", currentDisplayData.size()));
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "ソート処理中にエラーが発生しました", e);
        }

    }

    //  IDの数値変換
    private int parseNumericId(String id) {
        try {
            return Integer.parseInt(id.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    // かな順Comparator
    private Comparator<String> getJapaneseKanaComparator() {
        Collator collator = Collator.getInstance(Locale.JAPANESE);
        return (s1, s2) -> {
            if (s1 == null && s2 == null)
                return 0;
            if (s1 == null)
                return -1;
            if (s2 == null)
                return 1;
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
        List<EngineerDTO> filteredData = new ArrayList<>();

        for (EngineerDTO engineer : allData) {
            boolean matches = true;

            // IDと名前での一致をmatchesSearchメソッドを使ってチェック
            if (!id.isEmpty() && !matchesSearch(engineer, id)) {
                matches = false;
            }

            if (!name.isEmpty() && !matchesSearch(engineer, name)) {
                matches = false;
            }

            // 生年月日チェック
            if (engineer.getBirthDate() != null) {
                String birthDateStr = engineer.getBirthDate().toString();

                if (!year.isEmpty() && !birthDateStr.startsWith(year)) {
                    matches = false;
                }

                if (!month.isEmpty()) {
                    // 月の部分を確認 (YYYY-MM-DD 形式の場合、5-7文字目)
                    String monthPart = month.length() == 1 ? "0" + month : month;
                    if (!birthDateStr.substring(5, 7).equals(monthPart)) {
                        matches = false;
                    }
                }

                if (!day.isEmpty()) {
                    // 日の部分を確認 (YYYY-MM-DD 形式の場合、8-10文字目)
                    String dayPart = day.length() == 1 ? "0" + day : day;
                    if (!birthDateStr.substring(8, 10).equals(dayPart)) {
                        matches = false;
                    }
                }
            } else if (!year.isEmpty() || !month.isEmpty() || !day.isEmpty()) {
                // 生年月日がnullで、かつ検索条件が指定されている場合
                matches = false;
            }

            // エンジニア歴チェック
            if (!career.isEmpty()) {
                // intに変更
                int careerValue = engineer.getCareer();
                try {
                    int searchCareer = Integer.parseInt(career);
                    if (careerValue != searchCareer) {
                        matches = false;
                    }
                } catch (NumberFormatException e) {
                    // 数値変換エラーは無視
                }
            }

            // 条件を満たす場合にリストに追加
            if (matches) {
                filteredData.add(engineer);
            }
        }

        // 現在のページを1にリセット
        currentPage = 1;

        // テーブルを更新
        updateTableForData(filteredData);
        updatePageLabel(filteredData.size());

        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                String.format("検索実行: %d件のデータがヒット", filteredData.size()));
    }

    /**
     * 検索文字列との一致を確認
     * 
     * @param engineer   エンジニア情報
     * @param searchText 検索文字列
     * @return 一致する場合はtrue
     */
    private boolean matchesSearch(EngineerDTO engineer, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }

        String searchLower = searchText.toLowerCase();

        // IDと名前での検索
        boolean idMatch = engineer.getId() != null &&
                engineer.getId().toLowerCase().contains(searchLower);

        boolean nameMatch = engineer.getName() != null &&
                engineer.getName().toLowerCase().contains(searchLower);

        return idMatch || nameMatch;
    }

    /**
     * ページを切り替え
     *
     * @param delta ページ変化量（前：-1、次：+1）
     */
    private void changePage(int delta) {
        int totalPages = calculateTotalPages();
        int newPage = currentPage + delta;

        // ページ範囲の検証
        if (newPage < 1 || newPage > totalPages) {
            return;
        }

        currentPage = newPage;
        updateTableForCurrentPage();
        updatePageLabel();
        updatePaginationButtons();

        LogHandler.getInstance().log(Level.INFO, LogType.UI,
                String.format("ページを切り替えました: %d / %d", currentPage, totalPages));
    }

    /**
     * 総ページ数を計算
     *
     * @return 総ページ数
     */
    private int calculateTotalPages() {
        return (int) Math.ceil((double) allData.size() / pageSize);
    }

    /**
     * ページラベルを更新
     */
    private void updatePageLabel() {
        int totalPages = calculateTotalPages();
        pageLabel.setText(String.format("ページ: %d / %d", currentPage, totalPages));
    }

    /**
     * 指定したデータ数のページラベルを更新
     * 
     * @param dataSize データ数
     */
    private void updatePageLabel(int dataSize) {
        int totalPages = (int) Math.ceil((double) dataSize / pageSize);
        pageLabel.setText(String.format("ページ: %d / %d", currentPage, Math.max(1, totalPages)));
    }

    /**
     * ページネーションボタンの状態を更新
     */
    private void updatePaginationButtons() {
        int totalPages = calculateTotalPages();
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
    }

    /**
     * 現在のページに対応するテーブルデータを更新
     */
    private void updateTableForCurrentPage() {
        // テーブルデータのクリア
        tableModel.setRowCount(0);
        List<EngineerDTO> sourceData = currentDisplayData.isEmpty() ? allData : currentDisplayData;

        // 表示範囲の計算
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, sourceData.size());

        // データの追加
        for (int i = startIndex; i < endIndex; i++) {
            addEngineerToTable(sourceData.get(i));
        }
    }

    /**
     * 指定されたデータでテーブルを更新
     * 
     * @param data 表示するデータリスト
     */
    private void updateTableForData(List<EngineerDTO> data) {
        // テーブルデータのクリア
        tableModel.setRowCount(0);

        // 表示範囲の計算
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, data.size());

        // データの追加
        for (int i = startIndex; i < endIndex; i++) {
            EngineerDTO engineer = data.get(i);
            addEngineerToTable(engineer);
        }

        // ページネーションボタンの更新
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < Math.ceil((double) data.size() / pageSize));
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

        // ページ情報の初期化
        currentPage = 1;

        // テーブルの更新
        updateTableForCurrentPage();
        updatePageLabel();
        updatePaginationButtons();

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

        allData.add(engineer);

        // 最終ページ表示中の場合は、テーブルを更新
        int totalPages = calculateTotalPages();
        if (currentPage == totalPages || totalPages == 1) {
            updateTableForCurrentPage();
        }

        // ページネーション情報の更新
        updatePageLabel();
        updatePaginationButtons();

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                String.format("エンジニアを追加しました: ID=%s, 氏名=%s",
                        engineer.getId(), engineer.getName()));
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

        // 選択行のモデル上のインデックスに変換
        int modelRow = table.convertRowIndexToModel(selectedRow);

        // 現在のページの先頭インデックス
        int startIndex = (currentPage - 1) * pageSize;

        // データリスト上のインデックス
        int dataIndex = startIndex + modelRow;

        // インデックスが有効範囲内かチェック
        if (dataIndex >= 0 && dataIndex < allData.size()) {
            return allData.get(dataIndex);
        }

        return null;
    }

    /**
     * 現在選択されているエンジニアリストを取得
     *
     * @return 選択されているエンジニアリスト、未選択時は空リスト
     */
    public List<EngineerDTO> getSelectedEngineers() {
        int[] selectedRows = table.getSelectedRows();
        List<EngineerDTO> selectedEngineers = new ArrayList<>();

        if (selectedRows.length == 0) {
            return selectedEngineers;
        }

        // 現在のページの先頭インデックス
        int startIndex = (currentPage - 1) * pageSize;

        for (int selectedRow : selectedRows) {
            // 選択行のモデル上のインデックスに変換
            int modelRow = table.convertRowIndexToModel(selectedRow);

            // データリスト上のインデックス
            int dataIndex = startIndex + modelRow;

            // インデックスが有効範囲内かチェック
            if (dataIndex >= 0 && dataIndex < allData.size()) {
                selectedEngineers.add(allData.get(dataIndex));
            }
        }

        return selectedEngineers;
    }

    /**
     * テーブルのデータを更新
     */
    public void refreshTable() {
        updateTableForCurrentPage();
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
     * 総ページ数を取得
     *
     * @return 総ページ数
     */
    public int getTotalPages() {
        return calculateTotalPages();
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
        // ここにデータ取込処理を実装する（TODO）
    }

    /**
     * テンプレボタンのイベントハンドラ
     */
    private void loadTemplate() {
        LogHandler.getInstance().log(Level.INFO, LogType.UI, "テンプレートボタンが押されました");
        // ここにテンプレート読み込み処理を実装する（TODO）
    }

    /**
     * 出力ボタンのイベントハンドラ
     */
    private void exportData() {
        LogHandler.getInstance().log(Level.INFO, LogType.UI, "出力ボタンが押されました");
        // ここにエクスポート処理を実装する（TODO）
    }

    /**
     * 削除ボタンのイベントハンドラ
     */
    private void deleteSelectedRow() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length > 0) {
            // 選択されたエンジニアリストを取得
            List<EngineerDTO> selectedEngineers = getSelectedEngineers();

            // ここに削除処理を実装する（TODO）
            // 現在はメッセージのみログに記録
            LogHandler.getInstance().log(Level.INFO, LogType.UI,
                    String.format("%d件の行が削除対象に選択されました", selectedRows.length));
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