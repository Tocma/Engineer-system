package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import util.LogHandler;
import util.LogHandler.LogType;
import util.ResourceManager;
import util.Constants.CSVConstants;
import util.validator.FieldValidator;
import util.validator.ValidationResult;
import util.validator.ValidationService;
import util.validator.ValidatorFactory;

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

    /** 上書きモードフラグ（追記モードで書き込む場合はtrue） */
    private final boolean appendMode;

    /** 既存のIDマップ（重複チェック用） */
    private final Map<String, Integer> existingIds;

    /** バリデーションサービス */
    private final ValidationService validationService;

    /** CSVバリデータマップ */
    private Map<String, FieldValidator> csvValidators;

    /**
     * デフォルトコンストラクタ
     * ResourceManagerのデフォルトパスを使用
     */
    public CSVAccess(String operation, Object data) {
        this(operation, data, false, null);
    }

    /**
     * 追記モード指定コンストラクタ
     * ResourceManagerのデフォルトパスを使用し、追記モードを指定
     */
    public CSVAccess(String operation, Object data, boolean appendMode) {
        this(operation, data, appendMode, null);
    }

    /**
     * ファイル指定コンストラクタ
     * 特定のファイルを対象とする場合に使用
     */
    public CSVAccess(String operation, Object data, File targetFile) {
        this(operation, data, false, targetFile);
    }

    /**
     * 統合プライベートコンストラクタ
     * すべての初期化ロジックを一元管理
     * 
     * @param operation  操作種別（"read"または"write"）
     * @param data       操作対象データ
     * @param appendMode 追記モードフラグ
     * @param targetFile 対象ファイル（nullの場合はResourceManagerのデフォルトパスを使用）
     */
    private CSVAccess(String operation, Object data, boolean appendMode, File targetFile) {
        // ResourceManagerの初期化確認
        this.resourceManager = ResourceManager.getInstance();
        if (!resourceManager.isInitialized()) {
            throw new IllegalStateException("ResourceManagerが初期化されていません");
        }

        // 基本パラメータの設定
        this.operation = operation;
        this.data = data;
        this.appendMode = appendMode;

        // 共通オブジェクトの初期化
        this.lock = new ReentrantReadWriteLock();
        this.existingIds = new HashMap<>();
        this.validationService = ValidationService.getInstance();
        this.csvValidators = ValidatorFactory.createCSVValidators();

        // ターゲットファイルの設定
        if (targetFile != null) {
            // 外部ファイル指定の場合
            this.csvFile = targetFile;
            this.useResourceManager = false;
        } else {
            // ResourceManagerのデフォルトパスを使用
            this.csvFile = resourceManager.getEngineerCsvPath().toFile();
            this.useResourceManager = true;
        }

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

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "CSV行の変換中にエラーが発生（行 " + lineNumber + "）", _e);
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

        // CSVヘッダーの順序に従ってマッピング（必須項目）
        formData.put("id", row[CSVConstants.COLUMN_INDEX_ID]);
        formData.put("name", row[CSVConstants.COLUMN_INDEX_NAME]);
        formData.put("nameKana", row[CSVConstants.COLUMN_INDEX_NAME_KANA]);
        formData.put("birthDate", row[CSVConstants.COLUMN_INDEX_BIRTH_DATE]);
        formData.put("joinDate", row[CSVConstants.COLUMN_INDEX_JOIN_DATE]);
        formData.put("career", row[CSVConstants.COLUMN_INDEX_CAREER]);
        formData.put("programmingLanguages", row[CSVConstants.COLUMN_INDEX_LANGUAGES]);

        // 任意項目は空文字列をnullに変換
        formData.put("careerHistory", emptyToNull(row[CSVConstants.COLUMN_INDEX_CAREER_HISTORY]));
        formData.put("trainingHistory", emptyToNull(row[CSVConstants.COLUMN_INDEX_TRAINING_HISTORY]));
        formData.put("technicalSkill", emptyToNull(row[CSVConstants.COLUMN_INDEX_TECHNICAL_SKILL]));
        formData.put("learningAttitude", emptyToNull(row[CSVConstants.COLUMN_INDEX_LEARNING_ATTITUDE]));
        formData.put("communicationSkill", emptyToNull(row[CSVConstants.COLUMN_INDEX_COMMUNICATION_SKILL]));
        formData.put("leadership", emptyToNull(row[CSVConstants.COLUMN_INDEX_LEADERSHIP]));
        formData.put("note", emptyToNull(row[CSVConstants.COLUMN_INDEX_NOTE]));

        // 登録日（オプション）- 空欄やnullの場合は現在日付を設定
        String registeredDate = "";
        if (row.length > CSVConstants.COLUMN_INDEX_REGISTERED_DATE) {
            String csvRegisteredDate = row[CSVConstants.COLUMN_INDEX_REGISTERED_DATE];
            if (csvRegisteredDate != null && !csvRegisteredDate.trim().isEmpty()) {
                registeredDate = csvRegisteredDate.trim();
            } else {
                // 登録日が空欄やnullの場合は現在日付を設定
                registeredDate = LocalDate.now().format(
                        java.time.format.DateTimeFormatter.ofPattern(CSVConstants.DATE_FORMAT_PATTERN));
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "登録日が空欄のため現在日付を設定: " + registeredDate);
            }
        } else {
            // 登録日列が存在しない場合も現在日付を設定
            registeredDate = LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern(CSVConstants.DATE_FORMAT_PATTERN));
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "登録日列が存在しないため現在日付を設定: " + registeredDate);
        }

        formData.put("registeredDate", registeredDate);

        return formData;
    }

    /**
     * 空文字列をnullに変換するヘルパーメソッド
     */
    private String emptyToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value;
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
        } else {
            // CSV取り込み時に登録日が空白の場合は現在日付を設定
            builder.setRegisteredDate(LocalDate.now());
        }

        return builder.build();
    }

    /**
     * バリデーション済みスキル評価を設定
     */
    private void setValidatedSkillRatings(EngineerBuilder builder, Map<String, String> validatedData) {
        String technicalSkill = validatedData.get("technicalSkill");
        if (technicalSkill != null && !technicalSkill.trim().isEmpty()) {
            builder.setTechnicalSkill(Double.parseDouble(technicalSkill));
        } else {
            builder.setTechnicalSkill(null); // 明示的にnullを設定
        }

        String learningAttitude = validatedData.get("learningAttitude");
        if (learningAttitude != null && !learningAttitude.trim().isEmpty()) {
            builder.setLearningAttitude(Double.parseDouble(learningAttitude));
        } else {
            builder.setLearningAttitude(null); // 明示的にnullを設定
        }

        String communicationSkill = validatedData.get("communicationSkill");
        if (communicationSkill != null && !communicationSkill.trim().isEmpty()) {
            builder.setCommunicationSkill(Double.parseDouble(communicationSkill));
        } else {
            builder.setCommunicationSkill(null); // 明示的にnullを設定
        }

        String leadership = validatedData.get("leadership");
        if (leadership != null && !leadership.trim().isEmpty()) {
            builder.setLeadership(Double.parseDouble(leadership));
        } else {
            builder.setLeadership(null); // 明示的にnullを設定
        }
    }

    /**
     * CSVアクセス処理を実行
     * 指定された操作に基づいてファイル操作を実行
     */
    public void execute() {
        switch (operation) {
            case "read":
                result = readCSV();
                break;
            case "write":
                result = writeCSV((List<String>) data);
                break;
            default:
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "不明な操作: " + operation);
                result = null;
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
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVアクセス処理中にエラーが発生: " + operation, _e);

            // エラー時は空の結果オブジェクトを返す
            if ("read".equalsIgnoreCase(operation)) {
                result = new CSVAccessResult(new ArrayList<>(), new ArrayList<>(), true, _e.getMessage());
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
        // 正常データリスト
        List<EngineerDTO> successData = new ArrayList<>();
        // エラーデータリスト
        List<EngineerDTO> errorData = new ArrayList<>();
        // 重複IDリスト
        List<String> duplicateIds = new ArrayList<>();

        // 読み込んだCSV行データ
        List<String[]> csvRows = new ArrayList<>();

        lock.readLock().lock();

        String resourceKey = null;
        if (useResourceManager && resourceManager != null) {
            resourceKey = "csv_reader_" + System.currentTimeMillis();
        }

        try {
            //LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,"CSVファイル読み込み開始: " + csvFile.getPath());

            // ファイルが存在しない場合の処理
            if (!csvFile.exists()) {
                if (useResourceManager) {
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "CSVファイルが存在しないため、ResourceManager経由で作成を試行します");
                    return new CSVAccessResult(successData, errorData, false, null);
                } else {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "CSVファイルが存在しません: " + csvFile.getPath());
                    return new CSVAccessResult(successData, errorData, false, "CSVファイルが存在しません");
                }
            }

            // ファイル読み込み
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {

                if (useResourceManager && resourceManager != null) {
                    resourceManager.registerResource(resourceKey, reader);
                }

                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;

                    String[] values = parseCSVLine(line);

                    if (lineNumber == 1) {
                        continue; // ヘッダー行をスキップ
                    }

                    csvRows.add(values);
                }
            }

            //LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,"CSVファイル読み込み完了: " + csvFile.getPath() + ", " + csvRows.size() + "行");

        } catch (IOException _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "CSVファイルの読み込みに失敗: " + csvFile.getPath(), _e);
            return new CSVAccessResult(successData, errorData, true,
                    "CSVファイルの読み込みに失敗: " + _e.getMessage());
        } finally {
            if (useResourceManager && resourceManager != null && resourceKey != null) {
                resourceManager.releaseResource(resourceKey);
            }
            lock.readLock().unlock();
        }

        // CSV行データをEngineerDTOに変換
        for (int i = 0; i < csvRows.size(); i++) {
            String[] row = csvRows.get(i);
            int lineNumber = i + 2; // ヘッダー行も含めた行番号

            try {
                // 新しいバリデーションシステムを使用して変換
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

            } catch (Exception _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "CSV行の処理中にエラーが発生 (行 " + lineNumber + ")", _e);
                ValidationResult errorResult = new ValidationResult();
                errorResult.addError("error", "処理エラー (行 " + lineNumber + "): " + _e.getMessage());
                EngineerDTO errorEngineer = createErrorEngineer(row, lineNumber, errorResult);
                errorData.add(errorEngineer);
            }
        }

        return new CSVAccessResult(successData, errorData, duplicateIds, false, null);
    }

    /**
     * CSV行をパースしてカンマ区切りの値を抽出
     * 引用符で囲まれた値も正しく処理します
     * 
     * @param line CSV行データ
     * @return パースされた値の配列
     */
    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return new String[0];
        }
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        for (int charIndex = 0; charIndex < line.length(); charIndex++) {
            char currentChar = line.charAt(charIndex);
            if (currentChar == '"') {
                inQuotes = !inQuotes;
            } else if (currentChar == ',' && !inQuotes) {
                result.add(field.toString());
                field.setLength(0);
            } else {
                field.append(currentChar);
            }
        }
        result.add(field.toString());
        return result.toArray(new String[0]);
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
        if (useResourceManager && resourceManager != null) {
            resourceKey = "csv_writer_" + System.currentTimeMillis();
        }

        try {
            //LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,"CSVファイル" + (appendMode ? "追記" : "書き込み") + "開始: " + csvFile.getPath());

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
                                "ResourceManager経由でディレクトリを作成: " + parentDir.getPath());
                    } catch (IOException _e) {
                        LogHandler.getInstance().logError(LogType.SYSTEM,
                                "ResourceManager経由でのディレクトリ作成に失敗", _e);
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
                if (useResourceManager && resourceManager != null) {
                    resourceManager.registerResource(resourceKey, writer);
                }

                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            //LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,"CSVファイル" + (appendMode ? "追記" : "書き込み") + "完了: " + csvFile.getPath() + ", " + lines.size() + "行");

            return true;

        } catch (IOException _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVファイルの書き込みに失敗: " + csvFile.getPath(), _e);
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