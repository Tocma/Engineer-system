package model;

import util.LogHandler;
import util.LogHandler.LogType;
import util.validator.*;
import util.ResourceManager;
import util.Constants.CSVConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * CSVファイルへのアクセスを実装するクラス
 * ファイル操作とデータの読み書きを担当し、同時アクセス制御とバリデーションを提供します
 *
 * @author Nakano
 */
public class CSVAccess extends AccessThread {

    /** 操作種別（読み込み/書き込み） */
    private final String operation;

    /** 操作対象データ */
    private final Object data;

    /** CSVファイルパス（書き込み可能な一時ファイル） */
    private final File csvFile;

    /** ファイルアクセス用ロック */
    private final ReadWriteLock lock;

    /** 処理結果 */
    private Object result;

    /** ResourceManager参照（クラスパスベース） */
    private final ResourceManager resourceManager;

    /** 上書きモードフラグ */
    private final boolean appendMode;

    /** 既存のIDマップ（重複チェック用） */
    private final Map<String, Integer> existingIds;

    /** バリデーションサービス */
    private final ValidationService validationService;

    /** CSVバリデータマップ */
    private Map<String, FieldValidator> csvValidators;

    /**
     * デフォルトコンストラクタ（更新版）
     */
    public CSVAccess(String operation, Object data) {
        this(operation, data, false);
    }

    /**
     * ResourceManager統合コンストラクタ（更新版）
     */
    public CSVAccess(String operation, Object data, boolean appendMode) {
        // ResourceManagerはクラスパスベースで初期化済み
        this.resourceManager = ResourceManager.getInstance();
        if (!resourceManager.isInitialized()) {
            throw new IllegalStateException("ResourceManagerが初期化されていません");
        }

        this.operation = operation;
        this.data = data;

        // 重要な変更点：書き込み可能な一時ファイルパスを取得
        this.csvFile = resourceManager.getEngineerCsvPath().toFile();
        this.lock = new ReentrantReadWriteLock();
        this.appendMode = appendMode;
        this.existingIds = new HashMap<>();

        // バリデーションシステムの初期化
        this.validationService = ValidationService.getInstance();
        this.csvValidators = ValidatorFactory.createCSVValidators();

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "クラスパスベースCSVAccessを初期化完了: " + csvFile.getPath());
    }

    // CSVAccessクラスに、ファイルを指定できるコンストラクタを追加
    public CSVAccess(String operation, Object data, File targetFile) {
        this.resourceManager = ResourceManager.getInstance();
        this.operation = operation;
        this.data = data;

        if (targetFile != null) {
            this.csvFile = targetFile;
        } else {
            // デフォルトは一時ディレクトリ内のCSVファイル
            this.csvFile = resourceManager.getEngineerCsvPath().toFile();
        }

        this.lock = new ReentrantReadWriteLock();
        this.appendMode = false;
        this.existingIds = new HashMap<>();
        this.validationService = ValidationService.getInstance();
        this.csvValidators = ValidatorFactory.createCSVValidators();
    }

    /**
     * CSV行データをEngineerDTOに変換
     * 
     * @param row        CSV行データ
     * @param lineNumber 行番号（エラー報告用）
     * @return 変換されたEngineerDTOオブジェクト、変換エラー時はnull
     */
    private EngineerDTO convertToDTOWithValidation(String[] row, int lineNumber) {
        if (row == null || row.length < CSVConstants.CSV_HEADERS.length) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "CSV行データが不正です（行 " + lineNumber + "）: カラム数不足");
            return null;
        }

        try {
            // CSV行データをフィールドマップに変換
            Map<String, String> formData = createFormDataFromCSVRow(row);

            // バリデーション実行
            ValidationResult validationResult = validationService.validateForm(formData, csvValidators);

            if (!validationResult.isValid()) {
                // エラーログ出力
                logCSVValidationErrors(lineNumber, validationResult);
                return createErrorEngineer(row, lineNumber, validationResult);
            }

            // バリデーション成功：前処理済みデータでDTOを構築
            return buildEngineerFromValidatedData(validationResult.getProcessedValues());

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "CSV行の変換中にエラーが発生（行 " + lineNumber + "）", e);
            return null;
        }
    }

    /**
     * CSVバリデーションエラーをログ出力
     */
    private void logCSVValidationErrors(int lineNumber, ValidationResult result) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("CSV行のバリデーションエラー（行 ").append(lineNumber).append("）: ");

        for (Map.Entry<String, String> error : result.getErrors().entrySet()) {
            errorMessage.append("\n  - ").append(error.getKey()).append(": ").append(error.getValue());
        }

        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, errorMessage.toString());
    }

    /**
     * CSV行データからフォームデータマップを作成
     */
    private Map<String, String> createFormDataFromCSVRow(String[] row) {
        Map<String, String> formData = new HashMap<>();

        // CSVヘッダーの順序に従ってマッピング
        formData.put("id", row[CSVConstants.COLUMN_INDEX_ID]);
        formData.put("name", row[CSVConstants.COLUMN_INDEX_NAME]);
        formData.put("nameKana", row[CSVConstants.COLUMN_INDEX_NAME_KANA]);
        formData.put("birthDate", row[CSVConstants.COLUMN_INDEX_BIRTH_DATE]);
        formData.put("joinDate", row[CSVConstants.COLUMN_INDEX_JOIN_DATE]);
        formData.put("career", row[CSVConstants.COLUMN_INDEX_CAREER]);
        formData.put("programmingLanguages", row[CSVConstants.COLUMN_INDEX_LANGUAGES]);
        formData.put("careerHistory", row[CSVConstants.COLUMN_INDEX_CAREER_HISTORY]);
        formData.put("trainingHistory", row[CSVConstants.COLUMN_INDEX_TRAINING_HISTORY]);
        formData.put("technicalSkill", row[CSVConstants.COLUMN_INDEX_TECHNICAL_SKILL]);
        formData.put("learningAttitude", row[CSVConstants.COLUMN_INDEX_LEARNING_ATTITUDE]);
        formData.put("communicationSkill", row[CSVConstants.COLUMN_INDEX_COMMUNICATION_SKILL]);
        formData.put("leadership", row[CSVConstants.COLUMN_INDEX_LEADERSHIP]);
        formData.put("note", row[CSVConstants.COLUMN_INDEX_NOTE]);

        // 登録日（オプション）
        if (row.length > CSVConstants.COLUMN_INDEX_REGISTERED_DATE) {
            formData.put("registeredDate", row[CSVConstants.COLUMN_INDEX_REGISTERED_DATE]);
        }

        return formData;
    }

    /**
     * バリデーション済みデータからEngineerDTOを構築
     */
    private EngineerDTO buildEngineerFromValidatedData(Map<String, String> validatedData) {
        EngineerBuilder builder = new EngineerBuilder();

        // 基本情報
        builder.setId(validatedData.get("id"));
        builder.setName(validatedData.get("name"));
        builder.setNameKana(validatedData.get("nameKana"));

        // 日付
        String birthDate = validatedData.get("birthDate");
        if (birthDate != null && !birthDate.isEmpty()) {
            builder.setBirthDate(LocalDate.parse(birthDate));
        }

        String joinDate = validatedData.get("joinDate");
        if (joinDate != null && !joinDate.isEmpty()) {
            builder.setJoinDate(LocalDate.parse(joinDate));
        }

        // エンジニア歴
        String career = validatedData.get("career");
        if (career != null && !career.isEmpty()) {
            builder.setCareer(Integer.parseInt(career));
        }

        // プログラミング言語
        String languages = validatedData.get("programmingLanguages");
        if (languages != null && !languages.isEmpty()) {
            builder.setProgrammingLanguages(Arrays.asList(languages.split(";")));
        }

        // テキストフィールド
        builder.setCareerHistory(validatedData.get("careerHistory"));
        builder.setTrainingHistory(validatedData.get("trainingHistory"));
        builder.setNote(validatedData.get("note"));

        // スキル評価
        setValidatedSkillRatings(builder, validatedData);

        // 登録日
        String registeredDate = validatedData.get("registeredDate");
        if (registeredDate != null && !registeredDate.isEmpty()) {
            builder.setRegisteredDate(LocalDate.parse(registeredDate));
        }

        return builder.build();
    }

    /**
     * バリデーション済みスキル評価を設定
     */
    private void setValidatedSkillRatings(EngineerBuilder builder, Map<String, String> validatedData) {
        String technicalSkill = validatedData.get("technicalSkill");
        if (technicalSkill != null && !technicalSkill.isEmpty()) {
            builder.setTechnicalSkill(Double.parseDouble(technicalSkill));
        }

        String learningAttitude = validatedData.get("learningAttitude");
        if (learningAttitude != null && !learningAttitude.isEmpty()) {
            builder.setLearningAttitude(Double.parseDouble(learningAttitude));
        }

        String communicationSkill = validatedData.get("communicationSkill");
        if (communicationSkill != null && !communicationSkill.isEmpty()) {
            builder.setCommunicationSkill(Double.parseDouble(communicationSkill));
        }

        String leadership = validatedData.get("leadership");
        if (leadership != null && !leadership.isEmpty()) {
            builder.setLeadership(Double.parseDouble(leadership));
        }
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
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVアクセス処理の待機中に割り込みが発生", e);
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
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVアクセス処理中にエラーが発生: " + operation, e);

            // エラー時は空の結果オブジェクトを返す
            if ("read".equalsIgnoreCase(operation)) {
                result = new CSVAccessResult(new ArrayList<>(), new ArrayList<>(), true, e.getMessage());
            } else {
                result = Boolean.FALSE;
            }
        }
    }

    /**
     * CSVファイルを読み込み（更新版）
     * 新しいバリデーションシステムを使用
     */
    public CSVAccessResult readCSV() {
        List<EngineerDTO> successData = new ArrayList<>();
        List<EngineerDTO> errorData = new ArrayList<>();
        List<String> duplicateIds = new ArrayList<>();
        List<String[]> csvRows = new ArrayList<>();

        lock.readLock().lock();
        String resourceKey = "csv_reader_" + System.currentTimeMillis();

        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "クラスパスベースCSV読み込み開始: " + csvFile.getPath());

            // 段階的なリソースアクセス戦略
            boolean dataLoaded = false;

            // ステップ1: 一時ファイルが存在するかチェック
            if (csvFile.exists()) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "一時ファイルからCSVを読み込み: " + csvFile.getPath());
                dataLoaded = loadFromFile(csvRows, resourceKey);
            }

            // ステップ2: 一時ファイルが存在しない場合、クラスパスリソースを確認
            if (!dataLoaded) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "クラスパスリソースからCSVを読み込み試行");
                dataLoaded = loadFromClasspathResource(csvRows);
            }

            // ステップ3: どちらも存在しない場合、初期化ファイルを作成
            if (!dataLoaded) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "初期CSVファイルを作成");
                initializeDefaultCsvFile();
                // 空のデータとして処理を継続
            }

            // CSV行データをEngineerDTOに変換
            processCSVRows(csvRows, successData, errorData, duplicateIds);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "CSV読み込み完了: 成功=" + successData.size() + "件, エラー=" + errorData.size() + "件");

        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSV読み込み中にエラーが発生", e);
            return new CSVAccessResult(successData, errorData, true,
                    "CSV読み込み中にエラーが発生: " + e.getMessage());
        } finally {
            if (resourceManager != null && resourceKey != null) {
                resourceManager.releaseResource(resourceKey);
            }
            lock.readLock().unlock();
        }

        return new CSVAccessResult(successData, errorData, duplicateIds, false, null);
    }

    /**
     * ファイルシステムからCSVを読み込み
     */
    private boolean loadFromFile(List<String[]> csvRows, String resourceKey) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {

            resourceManager.registerResource(resourceKey, reader);

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) {
                    continue; // ヘッダー行をスキップ
                }
                String[] values = line.split(",", -1);
                csvRows.add(values);
            }

            return true;
        }
    }

    /**
     * クラスパスリソースからCSVを読み込み
     * 新機能：クラスパス内の初期データを読み込む
     */
    private boolean loadFromClasspathResource(List<String[]> csvRows) {
        String csvResourcePath = "data/" + util.Constants.FileConstants.DEFAULT_ENGINEER_CSV;

        try (InputStream resourceStream = resourceManager.getResourceAsStream(csvResourcePath)) {
            if (resourceStream == null) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "クラスパス内にCSVリソースが見つかりません: " + csvResourcePath);
                return false;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {

                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber == 1) {
                        continue; // ヘッダー行をスキップ
                    }
                    String[] values = line.split(",", -1);
                    csvRows.add(values);
                }

                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "クラスパスリソースから読み込み完了: " + csvRows.size() + "行");

                // クラスパスから読み込んだデータを一時ファイルにも保存
                copyResourceToTempFile(csvResourcePath);

                return true;
            }
        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "クラスパスリソース読み込み中にエラーが発生", e);
            return false;
        }
    }

    /**
     * クラスパスリソースを一時ファイルにコピー
     */
    private void copyResourceToTempFile(String resourcePath) {
        try (InputStream resourceStream = resourceManager.getResourceAsStream(resourcePath)) {
            if (resourceStream == null) {
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }

                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "クラスパスリソースを一時ファイルにコピー: " + csvFile.getPath());
            }
        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "リソースコピー中にエラーが発生", e);
        }
    }

    /**
     * 初期CSVファイルの作成
     */
    private void initializeDefaultCsvFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8))) {

            writer.write(String.join(",", CSVConstants.CSV_HEADERS));
            writer.newLine();

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "初期CSVファイルを作成: " + csvFile.getPath());
        }
    }

    /**
     * CSV行データの処理
     * バリデーション処理は既存のロジックを維持
     */
    private void processCSVRows(List<String[]> csvRows,
            List<EngineerDTO> successData,
            List<EngineerDTO> errorData,
            List<String> duplicateIds) {

        for (int i = 0; i < csvRows.size(); i++) {
            String[] row = csvRows.get(i);
            int lineNumber = i + 2; // ヘッダー行も含めた行番号

            try {
                EngineerDTO engineer = convertToDTOWithValidation(row, lineNumber);

                if (engineer == null) {
                    continue;
                }

                // エラーチェック（備考欄にエラーメッセージが含まれる場合）
                if (engineer.getNote() != null && engineer.getNote().startsWith("バリデーションエラー")) {
                    errorData.add(engineer);
                    continue;
                }

                // ID重複チェック
                String id = engineer.getId();
                if (id != null && !id.isEmpty() && existingIds.containsKey(id)) {
                    duplicateIds.add(id);
                } else if (id != null && !id.isEmpty()) {
                    existingIds.put(id, lineNumber);
                }

                successData.add(engineer);

            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "CSV行の処理中にエラーが発生 (行 " + lineNumber + ")", e);

                // エラー用のEngineerDTOを作成
                ValidationResult errorResult = new ValidationResult();
                errorResult.addError("error", "処理エラー (行 " + lineNumber + "): " + e.getMessage());
                EngineerDTO errorEngineer = createErrorEngineer(row, lineNumber, errorResult);
                errorData.add(errorEngineer);
            }
        }
    }

    /**
     * エラー情報を含むエンジニアDTOを作成
     */
    private EngineerDTO createErrorEngineer(String[] row, int lineNumber, ValidationResult result) {
        EngineerDTO engineer = new EngineerDTO();

        // 利用可能なデータを設定
        if (row.length > 0 && row[0] != null && !row[0].isEmpty()) {
            engineer.setId(row[0]);
        } else {
            engineer.setId("ERROR_LINE_" + lineNumber);
        }

        if (row.length > 1 && row[1] != null && !row[1].isEmpty()) {
            engineer.setName(row[1]);
        } else {
            engineer.setName("エラー（行 " + lineNumber + "）");
        }

        // エラーメッセージを備考欄に設定
        StringBuilder errorSummary = new StringBuilder("バリデーションエラー（行 " + lineNumber + "）:");
        for (String error : result.getErrors().values()) {
            errorSummary.append(" ").append(error).append(";");
        }
        engineer.setNote(errorSummary.toString());

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
        if (isUsingResourceManager() && resourceManager != null) {
            resourceKey = "csv_writer_" + System.currentTimeMillis();
        }

        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "CSVファイル" + (appendMode ? "追記" : "書き込み") + "開始: " + csvFile.getPath());

            // 親ディレクトリが存在しない場合は作成
            File parentDir = csvFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created;
                if (isUsingResourceManager() && resourceManager != null) {
                    // ResourceManager経由でディレクトリを作成
                    try {
                        resourceManager.createDirectory(parentDir.getName());
                        created = true;
                        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                "ResourceManager経由でディレクトリを作成: " + parentDir.getPath());
                    } catch (IOException e) {
                        LogHandler.getInstance().logError(LogType.SYSTEM,
                                "ResourceManager経由でのディレクトリ作成に失敗", e);
                        created = false;
                    }
                } else {
                    // 従来方式でディレクトリを作成
                    created = parentDir.mkdirs();
                }

                if (!created) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "ディレクトリの作成に失敗: " + parentDir.getPath());
                }
            }

            // ファイルへの書き込み、try-with-resources文を使用してリソース管理を強化
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(csvFile, appendMode), StandardCharsets.UTF_8))) {

                // ResourceManagerにリソースを登録（リソースリーク防止）
                if (isUsingResourceManager() && resourceManager != null) {
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
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVファイルの書き込みに失敗: " + csvFile.getPath(), e);
            return false;
        } finally {
            // リソースのクリーンアップ
            if (isUsingResourceManager() && resourceManager != null && resourceKey != null) {
                resourceManager.releaseResource(resourceKey);
            }
            // 書き込みロックを解放
            lock.writeLock().unlock();
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
        return resourceManager != null && resourceManager.isInitialized();
    }
}