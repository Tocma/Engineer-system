package model;

import util.LogHandler;
import util.LogHandler.LogType;
import util.validator.IDValidator;
import util.ResourceManager;
import util.Constants.CSVConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * CSVファイルへのアクセスを実装するクラス
 * ファイル操作とデータの読み書きを担当し、同時アクセス制御とバリデーションを提供します
 *
 * <p>
 * このクラスは、CSVファイルへの読み書き操作を実装し、同時アクセス制御を提供します。
 * AccessThreadクラスを継承し、バックグラウンドでの非同期ファイルアクセスを実現します。
 * また、CSVファイル読み込み時のバリデーションと重複IDの検出処理も担当します。
 * </p>
 *
 * <p>
 * 主な責務：
 * <ul>
 * <li>CSVファイルの読み込み処理</li>
 * <li>CSVファイルへの書き込み処理</li>
 * <li>ReadWriteLockによる同時アクセス制御</li>
 * <li>CSV行のバリデーションと変換</li>
 * <li>重複IDの検出と管理</li>
 * <li>エラーデータの収集と結果オブジェクトへの格納</li>
 * </ul>
 * </p>
 *
 * <p>
 * CSVファイル読み込み時は、行ごとにバリデーションを実行し、エラーがある場合はエラーリストに追加します。
 * また、IDの重複チェックを行い、重複がある場合は重複IDリストに追加します。
 * 処理結果は、CSVAccessResultオブジェクトとして返却され、正常データ、エラーデータ、重複IDの情報が格納されます。
 * </p>
 *
 * <p>
 * CSVファイル書き込み時は、データをCSV形式に変換して出力し、結果をBoolean値として返却します。
 * </p>
 *
 * @author Nakano
 */
public class CSVAccess extends AccessThread {

    /** 操作種別（読み込み/書き込み） */
    private final String operation;

    /** 操作対象データ */
    private final Object data;

    /** CSVファイルパス */
    private final File csvFile;

    /** ファイルアクセス用ロック */
    private final ReadWriteLock lock;

    /** 処理結果 */
    private Object result;

    /** ResourceManager参照（リソース管理用） */
    private final ResourceManager resourceManager;

    /** リソース管理フラグ（ResourceManager経由で作成された場合はtrue） */
    private final boolean useResourceManager;

    /** CSVのヘッダー行 */

    /** 上書きモードフラグ（追記モードで書き込む場合はtrue） */
    private final boolean appendMode;

    /** 既存のIDマップ（重複チェック用） */
    private final Map<String, Integer> existingIds;

    /**
     * デフォルトコンストラクタ
     * ResourceManagerから標準のエンジニアCSVファイルを使用して初期化
     * 
     * @param operation 操作種別（"read"または"write"）
     * @param data      操作対象データ（書き込み時のみ使用）
     * @throws IllegalStateException ResourceManagerが初期化されていない場合
     */
    public CSVAccess(String operation, Object data) {
        this(operation, data, false);
    }

    /**
     * ResourceManager統合コンストラクタ（上書きモード指定）
     * ResourceManagerから標準のエンジニアCSVファイルパスを取得して初期化
     * 
     * @param operation  操作種別（"read"または"write"）
     * @param data       操作対象データ（書き込み時のみ使用）
     * @param appendMode 追記モードの場合はtrue
     * @throws IllegalStateException ResourceManagerが初期化されていない場合
     */
    public CSVAccess(String operation, Object data, boolean appendMode) {
        // ResourceManagerから標準CSVファイルパスを取得
        this.resourceManager = ResourceManager.getInstance();
        if (!resourceManager.isInitialized()) {
            throw new IllegalStateException("ResourceManagerが初期化されていません");
        }

        this.operation = operation;
        this.data = data;
        this.csvFile = resourceManager.getEngineerCsvPath().toFile();
        this.lock = new ReentrantReadWriteLock();
        this.appendMode = appendMode;
        this.existingIds = new HashMap<>();
        this.useResourceManager = true;

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "CSVAccess初期化完了（ResourceManager統合）: " + csvFile.getPath());
    }

    /**
     * CSVアクセス処理を実行
     * 指定された操作に基づいてファイル操作を実行
     */
    public void execute() {
        start();

        // スレッド完了を待機
        try {
            if (thread != null) {
                thread.join(); // スレッドの完了を待機
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVアクセス処理の待機中に割り込みが発生しました", e);
        }
    }

    /**
     * 処理結果を取得
     * 
     * @return 処理結果（読み込み時はCSVAccessResult、書き込み時はBoolean）
     */
    public Object getResult() {
        return result;
    }

    /**
     * スレッド処理の実行
     * 操作種別に応じた処理を実行します
     */
    @Override
    protected void processOperation() {
        try {
            if ("read".equalsIgnoreCase(operation)) {
                result = readCSV();
            } else if ("write".equalsIgnoreCase(operation)) {
                boolean success = writeCSV((List<String>) data);
                result = Boolean.valueOf(success);
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "未知の操作種別: " + operation);
                result = null;
            }
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVアクセス処理中にエラーが発生しました: " + operation, e);

            // エラー時は空の結果オブジェクトを返す
            if ("read".equalsIgnoreCase(operation)) {
                result = new CSVAccessResult(new ArrayList<>(), new ArrayList<>(), true, e.getMessage());
            } else {
                result = Boolean.FALSE;
            }
        }
    }

    /**
     * CSVファイルを読み込んでエンジニアデータを取得
     * ResourceManager統合により、エラーハンドリングとリソース管理を強化
     * 
     * @return CSV読み込み結果を格納したCSVAccessResultオブジェクト
     */
    private CSVAccessResult readCSV() {
        // 正常データリスト
        List<EngineerDTO> successData = new ArrayList<>();
        // エラーデータリスト
        List<EngineerDTO> errorData = new ArrayList<>();
        // 重複IDリスト
        List<String> duplicateIds = new ArrayList<>();

        // 読み込んだCSV行データ
        List<String[]> csvRows = new ArrayList<>();

        // ロックを取得
        lock.readLock().lock();

        // ResourceManagerを使用している場合のリソース登録
        String resourceKey = null;
        if (useResourceManager && resourceManager != null) {
            resourceKey = "csv_reader_" + System.currentTimeMillis();
        }

        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "CSVファイル読み込み開始: " + csvFile.getPath());

            // ファイルが存在しない場合の処理
            if (!csvFile.exists()) {
                if (useResourceManager) {
                    // ResourceManager使用時は、ファイルの自動作成を試行
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "CSVファイルが存在しないため、ResourceManager経由で作成を試行します");
                    // この場合は空のデータセットとして返す（ResourceManagerが適切にファイルを管理）
                    return new CSVAccessResult(successData, errorData, false, null);
                } else {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "CSVファイルが存在しません: " + csvFile.getPath());
                    return new CSVAccessResult(successData, errorData, false, "CSVファイルが存在しません");
                }
            }

            // ファイルを読み込む、try-with-resources文を使用してリソース管理を強化
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {

                // ResourceManagerにリソースを登録（リソースリーク防止）
                if (useResourceManager && resourceManager != null) {
                    resourceManager.registerResource(resourceKey, reader);
                }

                String line;
                int lineNumber = 0;

                // 全行を読み込む
                while ((line = reader.readLine()) != null) {
                    lineNumber++;

                    // 行を解析
                    String[] values = line.split(",", -1); // 空フィールドも保持

                    // ヘッダー行（1行目）はスキップ
                    if (lineNumber == 1) {
                        continue;
                    }

                    // 行データを追加
                    csvRows.add(values);
                }
            }

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "CSVファイル読み込み完了: " + csvFile.getPath() + ", " + csvRows.size() + "行");

        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVファイルの読み込みに失敗しました: " + csvFile.getPath(), e);
            return new CSVAccessResult(successData, errorData, true, "CSVファイルの読み込みに失敗しました: " + e.getMessage());
        } finally {
            // リソースのクリーンアップ
            if (useResourceManager && resourceManager != null && resourceKey != null) {
                resourceManager.releaseResource(resourceKey);
            }
            // ロックを解放
            lock.readLock().unlock();
        }

        // CSV行データをEngineerDTOに変換し、バリデーションを実行
        for (int i = 0; i < csvRows.size(); i++) {
            String[] row = csvRows.get(i);
            int lineNumber = i + 2; // ヘッダー行も含めた行番号

            try {
                // 行の最低カラム数チェック
                if (row.length < CSVConstants.CSV_HEADERS.length) {
                    EngineerDTO errorEngineer = createErrorEngineer(row, "カラム数が不足しています (行 " + lineNumber + ")");
                    errorData.add(errorEngineer);
                    continue;
                }

                // EngineerDTOに変換
                EngineerDTO engineer = convertToDTO(row);

                // nullの場合は変換エラー
                if (engineer == null) {
                    EngineerDTO errorEngineer = createErrorEngineer(row, "データ変換エラー (行 " + lineNumber + ")");
                    errorData.add(errorEngineer);
                    continue;
                }

                // IDの重複チェック
                String id = engineer.getId();
                if (id != null && !id.isEmpty()) {
                    // 標準形式に変換
                    String standardizedId = IDValidator.standardizeId(IDValidator.convertFullWidthToHalfWidth(id));

                    // 禁止ID（ID00000）チェック
                    if ("ID00000".equals(standardizedId)) {
                        EngineerDTO errorEngineer = createErrorEngineer(row, "ID00000は使用できません (行 " + lineNumber + ")");
                        errorData.add(errorEngineer);
                        continue;
                    }

                    // 既存のIDマップを確認
                    if (existingIds.containsKey(standardizedId)) {
                        // 重複ID発見
                        duplicateIds.add(standardizedId);
                    } else {
                        // IDを記録
                        existingIds.put(standardizedId, lineNumber);
                    }

                    // 標準化したIDをEngineerDTOに設定
                    engineer.setId(standardizedId);
                }

                // 成功リストに追加
                successData.add(engineer);

            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "CSV行の処理中にエラーが発生しました (行 " + lineNumber + ")", e);
                EngineerDTO errorEngineer = createErrorEngineer(row, "処理エラー (行 " + lineNumber + "): " + e.getMessage());
                errorData.add(errorEngineer);
            }
        }

        // 結果を返却
        return new CSVAccessResult(successData, errorData, duplicateIds, false, null);
    }

    /**
     * エラー情報を含むエンジニアDTOを作成
     * エラー発生時にエラーリストに追加するためのDTOを生成します
     * 
     * @param row          元のCSV行データ
     * @param errorMessage エラーメッセージ
     * @return エラー情報を含むEngineerDTO
     */
    private EngineerDTO createErrorEngineer(String[] row, String errorMessage) {
        EngineerDTO engineer = new EngineerDTO();

        // 利用可能なデータを設定
        if (row.length > 0 && row[0] != null && !row[0].isEmpty()) {
            engineer.setId(row[0]);
        } else {
            engineer.setId("ERROR");
        }

        if (row.length > 1 && row[1] != null && !row[1].isEmpty()) {
            engineer.setName(row[1]);
        } else {
            engineer.setName("不明");
        }

        // エラーメッセージを備考欄に設定
        engineer.setNote(errorMessage);

        return engineer;
    }

    /**
     * CSVファイルに書き込む
     * ResourceManager統合により、書き込み処理の安全性を向上
     * 
     * @param lines 書き込む行のリスト
     * @return 書き込み成功の場合はtrue
     */
    private boolean writeCSV(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "書き込むデータがありません");
            return false;
        }

        // 書き込みロックを取得
        lock.writeLock().lock();

        // ResourceManagerを使用している場合のリソース登録
        String resourceKey = null;
        if (useResourceManager && resourceManager != null) {
            resourceKey = "csv_writer_" + System.currentTimeMillis();
        }

        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "CSVファイル" + (appendMode ? "追記" : "書き込み") + "開始: " + csvFile.getPath());

            // 親ディレクトリが存在しない場合は作成
            File parentDir = csvFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created;
                if (useResourceManager && resourceManager != null) {
                    // ResourceManager経由でディレクトリを作成
                    try {
                        resourceManager.createDirectory(parentDir.getName());
                        created = true;
                        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                "ResourceManager経由でディレクトリを作成しました: " + parentDir.getPath());
                    } catch (IOException e) {
                        LogHandler.getInstance().logError(LogType.SYSTEM,
                                "ResourceManager経由でのディレクトリ作成に失敗しました", e);
                        created = false;
                    }
                } else {
                    // 従来方式でディレクトリを作成
                    created = parentDir.mkdirs();
                }

                if (!created) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "ディレクトリの作成に失敗しました: " + parentDir.getPath());
                }
            }

            // ファイルへの書き込み、try-with-resources文を使用してリソース管理を強化
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(csvFile, appendMode), StandardCharsets.UTF_8))) {

                // ResourceManagerにリソースを登録（リソースリーク防止）
                if (useResourceManager && resourceManager != null) {
                    resourceManager.registerResource(resourceKey, writer);
                }

                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "CSVファイル" + (appendMode ? "追記" : "書き込み") + "完了: " + csvFile.getPath() + ", " + lines.size() + "行");

            return true;

        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVファイルの書き込みに失敗しました: " + csvFile.getPath(), e);
            return false;
        } finally {
            // リソースのクリーンアップ
            if (useResourceManager && resourceManager != null && resourceKey != null) {
                resourceManager.releaseResource(resourceKey);
            }
            // 書き込みロックを解放
            lock.writeLock().unlock();
        }
    }

    /**
     * CSV行データをEngineerDTOに変換
     * CSV形式のデータをエンジニア情報オブジェクトに変換します
     * 
     * @param row CSV行データ
     * @return 変換されたEngineerDTOオブジェクト、変換エラー時はnull
     */
    private EngineerDTO convertToDTO(String[] row) {
        // EngineerBuilderを使用してDTOを構築
        try {
            // EngineerBuilderを使用してCSVからDTOを構築
            return new EngineerCSVDAO().convertToDTO(row);

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "EngineerDTOへの変換に失敗しました", e);
            return null;
        }
    }

    /**
     * 現在の処理の操作種別を取得
     * 
     * @return 操作種別（"read"または"write"）
     */
    public String getOperation() {
        return operation;
    }

    /**
     * CSVファイルのパスを取得
     * 
     * @return CSVファイルのパス
     */
    public File getCsvFile() {
        return csvFile;
    }

    /**
     * 上書きモードかどうかを取得
     * 
     * @return 上書きモードの場合はtrue
     */
    public boolean isAppendMode() {
        return appendMode;
    }

    /**
     * 既存IDのマップを取得
     * 
     * @return 既存IDと行番号のマップ
     */
    public Map<String, Integer> getExistingIds() {
        return new HashMap<>(existingIds);
    }

    /**
     * ResourceManager使用状態を取得
     * デバッグやテスト用途でResourceManager統合の状態を確認
     * 
     * @return ResourceManagerを使用している場合はtrue
     */
    public boolean isUsingResourceManager() {
        return useResourceManager;
    }
}