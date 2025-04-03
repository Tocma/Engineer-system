package view;

import model.EngineerDTO;
import util.LogHandler;
import util.LogHandler.LogType;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * エンジニア一覧を表示するパネルクラス
 * CSVファイルから読み込んだエンジニア情報をテーブル形式で表示
 *
 * <p>
 * このクラスは、エンジニア人材管理システムの中心的な画面として機能し、
 * 全エンジニアの一覧をテーブル形式で表示します。主な機能は以下の通りです：
 * <ul>
 * <li>エンジニア情報の一覧表示</li>
 * <li>ページネーション機能（大量データの効率的な表示）</li>
 * <li>基本的なテーブルのソート機能</li>
 * </ul>
 * </p>
 *
 * <p>
 * 現在の実装では、基本的な一覧表示とページネーション機能のみを提供しています。
 * 将来的な拡張として以下の機能が予定されています：
 * <ul>
 * <li>検索機能</li>
 * <li>CSVエクスポート機能</li>
 * <li>詳細表示機能</li>
 * <li>新規追加機能</li>
 * </ul>
 * </p>
 *
 * <p>
 * このパネルはMVCアーキテクチャのView部分を担当し、
 * ユーザーインターフェースの提供と基本的なユーザー操作の受け付けを行います。
 * データの操作や保存はControllerを介してModelに委譲されます。
 * </p>
 *
 * @author Nakano
 * @version 2.1.0
 * @since 2025-04-03
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

        // ロギング
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "エンジニア一覧パネルを初期化しました");
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
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        newTable.setRowSorter(sorter);

        // 列幅設定
        newTable.getColumnModel().getColumn(0).setPreferredWidth(100); // 社員ID
        newTable.getColumnModel().getColumn(1).setPreferredWidth(150); // 氏名
        newTable.getColumnModel().getColumn(2).setPreferredWidth(120); // 生年月日
        newTable.getColumnModel().getColumn(3).setPreferredWidth(100); // エンジニア歴
        newTable.getColumnModel().getColumn(4).setPreferredWidth(300); // 扱える言語

        // 選択モード設定
        newTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 行の高さ
        newTable.setRowHeight(25);

        return newTable;
    }

    /**
     * 上部パネルを作成
     *
     * @return 上部パネル
     */
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());

        // タイトルラベル
        JLabel titleLabel = new JLabel("エンジニア一覧", JLabel.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        topPanel.add(titleLabel, BorderLayout.CENTER);

        // 将来的に検索フィールドや操作ボタンを追加する場所

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

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
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

        // 表示範囲の計算
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allData.size());

        // データの追加
        for (int i = startIndex; i < endIndex; i++) {
            EngineerDTO engineer = allData.get(i);
            addEngineerToTable(engineer);
        }
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
     * 既存データに新しいエンジニアを追加
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
}