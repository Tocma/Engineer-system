package model;

import view.DialogManager;
import util.LogHandler;
import util.LogHandler.LogType;
import util.validator.IDValidator;
import util.ResourceManager;
import util.Constants.CSVConstants;
import util.Constants.SystemConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * CSVファイルを使用したエンジニア情報のデータアクセスを実装するクラス
 * EngineerDAOインターフェースを実装し、CSVファイルを使用したデータ永続化を提供
 *
 * @author Nakano
 */
public class EngineerCSVDAO implements EngineerDAO {

    /** 日付フォーマット */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /** ResourceManagerインスタンス（クラスパスベース対応） */
    private final ResourceManager resourceManager;

    /** DialogManagerインスタンス */
    private final DialogManager dialogManager;

    /** 書き込み用CSVファイルパス（一時ディレクトリ内） */
    private final String csvFilePath;

    /** インポート用の外部ファイル参照 */
    private File importFile = null;

    /** クラスパスリソースのベースパス */
    private static final String CSV_RESOURCE_PATH = "data/" + util.Constants.FileConstants.DEFAULT_ENGINEER_CSV;
    private static final String TEMPLATE_RESOURCE_PATH = "templates/engineer_template.csv";

    /**
     * デフォルトコンストラクタ
     * ResourceManagerから標準のCSVファイルパスを取得して初期化
     * 
     */
    public EngineerCSVDAO() {
        this.resourceManager = ResourceManager.getInstance();
        this.dialogManager = DialogManager.getInstance();

        try {
            // ResourceManagerの初期化確認
            if (!resourceManager.isInitialized()) {
                resourceManager.initialize();
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "クラスパスベースResourceManagerを初期化完了");
            }

            // 書き込み用ファイルパス取得（一時ディレクトリ内）
            this.csvFilePath = resourceManager.getEngineerCsvPath().toString();

            // 初期データの準備（多段階アプローチ）
            initializeDataWithMultiStageApproach();

        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "EngineerCSVDAOの初期化に失敗", e);
            throw new RuntimeException("EngineerCSVDAOの初期化に失敗", e);
        }
    }

    /**
     * 指定されたCSVファイルパスを使用するコンストラクタ
     * テスト用途や特別な要件がある場合に使用
     * 
     * @param csvFilePath CSVファイルのパス
     */
    public EngineerCSVDAO(String csvFilePath) {
        this.resourceManager = ResourceManager.getInstance();
        this.dialogManager = DialogManager.getInstance();
        this.csvFilePath = csvFilePath;

        // インポート用ファイルとして設定
        this.importFile = new File(csvFilePath);

        if (!this.importFile.exists()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "指定されたインポートファイルが存在しません: " + csvFilePath);
        }
    }

    /**
     * 多段階アプローチによるデータ初期化
     * クラスパスリソース → 一時ファイル → 新規作成の順で試行
     */
    private void initializeDataWithMultiStageApproach() throws IOException {
        File csvFile = new File(csvFilePath);

        // ステップ1: 一時ファイルが既に存在するかチェック
        if (csvFile.exists()) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "既存の一時CSVファイルを使用: " + csvFilePath);
            return;
        }

        // ステップ2: クラスパスからの初期データ読み込みを試行
        if (loadInitialDataFromClasspath(csvFile)) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "クラスパスリソースから初期データを読み込み完了");
            return;
        }

        // ステップ3: 初期ファイルの新規作成
        createInitialCsvFile(csvFile);
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "新規初期CSVファイルを作成: " + csvFilePath);
    }

    /**
     * クラスパスから初期データを読み込み
     * 
     * @param targetFile 書き込み先の一時ファイル
     * @return 読み込み成功の場合true
     */
    private boolean loadInitialDataFromClasspath(File targetFile) {
        try (InputStream resourceStream = resourceManager.getResourceAsStream(CSV_RESOURCE_PATH)) {
            if (resourceStream == null) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "クラスパス内に初期CSVリソースが見つかりません: " + CSV_RESOURCE_PATH);
                return false;
            }

            // クラスパスリソースを一時ファイルにコピー
            copyStreamToFile(resourceStream, targetFile);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "クラスパスリソースを一時ファイルにコピー: " + targetFile.getPath());
            return true;

        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "クラスパスリソースの読み込みに失敗", e);
            return false;
        }
    }

    /**
     * ストリームをファイルにコピーするヘルパーメソッド
     */
    private void copyStreamToFile(InputStream inputStream, File targetFile) throws IOException {
        // 親ディレクトリの確認と作成
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    /**
     * 初期CSVファイルの作成
     * ヘッダー行のみを含む空のCSVファイルを作成
     */
    private void createInitialCsvFile(File csvFile) throws IOException {
        File parentDir = csvFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8))) {

            writer.write(String.join(",", CSVConstants.CSV_HEADERS));
            writer.newLine();
        }
    }

    @Override
    public List<EngineerDTO> findAll() {
        try {
            CSVAccessResult result = readCSV();

            if (result.isFatalError()) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "エンジニアデータの読み込みに失敗: " + result.getErrorMessage());
                return new ArrayList<>();
            }

            List<EngineerDTO> successData = result.getSuccessData();
            if (successData.size() > SystemConstants.MAX_ENGINEER_RECORDS) {
                LogHandler.getInstance().log(Level.SEVERE, LogType.SYSTEM,
                        "データ件数が上限を超過: " + successData.size() + "件");
                return successData;
            }

            return successData;

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "エンジニアリストの取得に失敗", e);
            return new ArrayList<>();
        }
    }

    @Override
    public EngineerDTO findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        try {
            List<EngineerDTO> engineers = findAll();
            return engineers.stream()
                    .filter(engineer -> id.equals(engineer.getId()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "IDによるエンジニア検索に失敗: " + id, e);
            return null;
        }
    }

    @Override
    public void save(EngineerDTO engineer) {
        if (engineer == null) {
            throw new IllegalArgumentException("エンジニア情報がnullです");
        }

        try {
            CSVAccessResult currentData = readCSV();
            List<EngineerDTO> engineers = new ArrayList<>(currentData.getSuccessData());
            engineers.add(engineer);

            writeCSV(engineers);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("エンジニア情報を保存: ID=%s, 名前=%s",
                            engineer.getId(), engineer.getName()));

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "エンジニア情報の保存に失敗", e);
            throw new RuntimeException("エンジニア情報の保存に失敗", e);
        }
    }

    @Override
    public void update(EngineerDTO engineer) {
        if (engineer == null) {
            throw new IllegalArgumentException("エンジニア情報がnullです");
        }

        try {
            CSVAccessResult currentData = readCSV();
            List<EngineerDTO> engineers = new ArrayList<>(currentData.getSuccessData());
            boolean updated = false;

            for (int i = 0; i < engineers.size(); i++) {
                if (engineer.getId().equals(engineers.get(i).getId())) {
                    engineers.set(i, engineer);
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                engineers.add(engineer);
            }

            writeCSV(engineers);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("エンジニア情報を更新: ID=%s, 名前=%s",
                            engineer.getId(), engineer.getName()));

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "エンジニア情報の更新に失敗", e);
            throw new RuntimeException("エンジニア情報の更新に失敗", e);
        }
    }

    @Override
    public void deleteAll(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("削除対象のIDリストが空です");
        }

        try {
            CSVAccessResult currentData = readCSV();
            List<EngineerDTO> filtered = currentData.getSuccessData().stream()
                    .filter(dto -> !ids.contains(dto.getId()))
                    .collect(Collectors.toList());

            writeCSV(filtered);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("エンジニア情報を一括削除: %d件", ids.size()));

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "エンジニア情報の一括削除に失敗", e);
            throw new RuntimeException("エンジニア情報の一括削除に失敗", e);
        }
    }

    /**
     * エラーリストをCSVファイルにエクスポート
     * 読み込み時に発生したエラーリストを別CSVファイルに出力
     * 
     * @param errorList エクスポートするエラーデータリスト
     * @param filePath  出力するCSVファイルパス
     * @return エクスポート成功の場合はtrue
     */
    public boolean exportErrorList(List<EngineerDTO> errorList, String filePath) {
        if (errorList == null || errorList.isEmpty()) {
            return false;
        }

        try {
            // ヘッダー行の追加
            List<String> csvLines = new ArrayList<>();
            csvLines.add(String.join(",", CSVConstants.CSV_HEADERS));

            // エラーリストをCSV形式に変換
            for (EngineerDTO engineer : errorList) {
                csvLines.add(convertToCSV(engineer));
            }

            CSVAccess csvAccess = new CSVAccess("write", csvLines);
            csvAccess.execute();

            // 結果を取得
            Object result = csvAccess.getResult();
            boolean success = result instanceof Boolean && (Boolean) result;

            if (success) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "エラーリストをCSVファイルに出力: " + filePath);
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "エラーリストのCSV出力に失敗: " + filePath);
            }

            return success;

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エラーリストのCSV出力中にエラーが発生", e);
            return false;
        }
    }

    /**
     * CSVファイルにエンジニアデータをエクスポート
     * エンジニアリストをCSVファイルに出力
     * 
     * @param engineerList エクスポートするエンジニアデータリスト
     * @param filePath     出力するCSVファイルパス
     * @return エクスポート成功の場合はtrue
     */
    public boolean exportCSV(List<EngineerDTO> engineerList, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            // ヘッダー書き込み
            writer.write(String.join(",", CSVConstants.CSV_HEADERS));
            writer.newLine();

            // データ書き込み
            for (EngineerDTO engineer : engineerList) {
                writer.write(convertToCSV(engineer));
                writer.newLine();
            }

            return true;
        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSV出力エラー", e);
            return false;
        }
    }

    /** テンプレート出力機能 */
    public boolean exportTemplate(String filePath) {
        try {
            // まずクラスパス内のテンプレートリソースを確認
            InputStream templateStream = resourceManager.getResourceAsStream(TEMPLATE_RESOURCE_PATH);

            if (templateStream != null) {
                // クラスパス内のテンプレートを使用
                return exportTemplateFromResource(templateStream, filePath);
            } else {
                // フォールバック: 基本的なヘッダーのみのテンプレートを作成
                return exportBasicTemplate(filePath);
            }

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "テンプレートCSV出力に失敗", e);
            return false;
        }
    }

    /**
     * クラスパスリソースからテンプレートを出力
     */
    private boolean exportTemplateFromResource(InputStream templateStream, String filePath) {
        try (templateStream;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(templateStream, StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "クラスパステンプレートから出力: " + filePath);
            return true;

        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "テンプレートリソース出力に失敗", e);
            return false;
        }
    }

    /**
     * 基本テンプレートの出力（フォールバック）
     */
    private boolean exportBasicTemplate(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            writer.write(String.join(",", CSVConstants.CSV_HEADERS));
            writer.newLine();

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "基本テンプレートを出力: " + filePath);
            return true;

        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "基本テンプレート出力に失敗", e);
            return false;
        }
    }

    /**
     * CSVファイルを読み込み
     * ResourceManagerと統合されたCSVAccessを使用してデータを読み込む
     * 
     * この実装では、CSVAccessクラスにResourceManagerから取得した
     * ファイルパスを渡すことで、リソース管理を一元化しています。
     * 
     * @return CSV読み込み結果
     */
    public CSVAccessResult readCSV() {
        try {
            CSVAccess csvAccess;

            if (importFile != null && importFile.exists()) {
                // インポートファイルが指定されている場合
                csvAccess = new CSVAccess("read", null, importFile);
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "外部インポートファイルから読み込み: " + importFile.getPath());
            } else {
                // 標準的な読み込み（クラスパスベース対応CSVAccess使用）
                csvAccess = new CSVAccess("read", null, true); // appendModeはfalse
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "クラスパスベース読み込みを開始");
            }

            csvAccess.execute();
            Object result = csvAccess.getResult();

            if (result instanceof CSVAccessResult accessResult) {
                // 重複ID処理
                if (accessResult.hasDuplicateIds() && !accessResult.isOverwriteConfirmed()) {
                    handleDuplicateIds(accessResult);
                }
                return accessResult;
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "CSV読み込み結果が不正な形式です");
                return new CSVAccessResult(new ArrayList<>(), new ArrayList<>(),
                        true, "CSV読み込み結果が不正な形式です");
            }

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "CSV読み込み中にエラーが発生", e);
            return new CSVAccessResult(new ArrayList<>(), new ArrayList<>(),
                    true, "CSV読み込み中にエラーが発生: " + e.getMessage());
        }
    }

    /**
     * 設定情報の読み込み（新機能）
     * クラスパス内の設定ファイルから各種設定を読み込み
     * 
     * @param configPath 設定ファイルのクラスパス
     * @return 設定情報のProperties
     */
    public Properties loadConfigurationFromClasspath(String configPath) {
        Properties config = new Properties();

        try (InputStream configStream = resourceManager.getResourceAsStream(configPath)) {
            if (configStream != null) {
                config.load(configStream);
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "設定ファイルを読み込み: " + configPath);
            } else {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "設定ファイルが見つかりません: " + configPath);
            }
        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "設定ファイル読み込みエラー: " + configPath, e);
        }

        return config;
    }

    /**
     * CSVファイルに書き込み
     * ResourceManagerと統合されたCSVAccessを使用してデータを書き込む
     * 
     * @param engineers 書き込むエンジニアデータリスト
     * @return 書き込み成功の場合はtrue
     */
    private boolean writeCSV(List<EngineerDTO> engineers) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add(String.join(",", CSVConstants.CSV_HEADERS));

            for (EngineerDTO engineer : engineers) {
                String line = convertToCSV(engineer);
                lines.add(line);
            }

            CSVAccess csvAccess = new CSVAccess("write", lines, false);
            csvAccess.execute();

            Object result = csvAccess.getResult();
            return result instanceof Boolean && (Boolean) result;

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "CSV書き込み中にエラーが発生", e);
            return false;
        }
    }

    /**
     * 重複ID処理
     * 重複IDが検出された場合の処理を行う
     * 
     * @param result CSVアクセス結果
     */
    private void handleDuplicateIds(CSVAccessResult result) {
        if (!result.hasDuplicateIds()) {
            return;
        }

        try {
            boolean overwrite = dialogManager.showDuplicateIdConfirmDialog(result.getDuplicateIds());
            result.setOverwriteConfirmed(overwrite);

            if (overwrite) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "重複IDの上書きが確認されました: " + result.getDuplicateIds().size() + "件");
            } else {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "重複IDの保持が確認されました: " + result.getDuplicateIds().size() + "件");

                List<EngineerDTO> filteredData = result.getSuccessData().stream()
                        .filter(engineer -> !result.getDuplicateIds().contains(engineer.getId()))
                        .collect(Collectors.toList());

                result.getSuccessData().clear();
                result.getSuccessData().addAll(filteredData);
            }

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "重複ID処理中にエラーが発生", e);
            result.setOverwriteConfirmed(false);

            List<EngineerDTO> filteredData = result.getSuccessData().stream()
                    .filter(engineer -> !result.getDuplicateIds().contains(engineer.getId()))
                    .collect(Collectors.toList());

            result.getSuccessData().clear();
            result.getSuccessData().addAll(filteredData);
        }
    }

    /**
     * EngineerDTOをCSV行に変換
     * エンジニア情報をCSV形式の文字列に変換
     * 
     * @param engineer 変換するエンジニア情報
     * @return CSV形式の文字列
     */
    public String convertToCSV(EngineerDTO engineer) {
        // 既存の実装を保持
        StringBuilder csvLineBuilder = new StringBuilder();

        csvLineBuilder.append(nullToEmpty(engineer.getId())).append(",");
        csvLineBuilder.append(nullToEmpty(engineer.getName())).append(",");
        csvLineBuilder.append(nullToEmpty(engineer.getNameKana())).append(",");

        if (engineer.getBirthDate() != null) {
            csvLineBuilder.append(engineer.getBirthDate().format(DATE_FORMATTER));
        }
        csvLineBuilder.append(",");

        if (engineer.getJoinDate() != null) {
            csvLineBuilder.append(engineer.getJoinDate().format(DATE_FORMATTER));
        }
        csvLineBuilder.append(",");

        csvLineBuilder.append(engineer.getCareer()).append(",");

        if (engineer.getProgrammingLanguages() != null && !engineer.getProgrammingLanguages().isEmpty()) {
            csvLineBuilder.append(String.join(";", engineer.getProgrammingLanguages()));
        }
        csvLineBuilder.append(",");

        csvLineBuilder.append(nullToEmpty(engineer.getCareerHistory())).append(",");
        csvLineBuilder.append(nullToEmpty(engineer.getTrainingHistory())).append(",");

        if (engineer.getTechnicalSkill() != null) {
            csvLineBuilder.append(engineer.getTechnicalSkill());
        }
        csvLineBuilder.append(",");

        if (engineer.getLearningAttitude() != null) {
            csvLineBuilder.append(engineer.getLearningAttitude());
        }
        csvLineBuilder.append(",");

        if (engineer.getCommunicationSkill() != null) {
            csvLineBuilder.append(engineer.getCommunicationSkill());
        }
        csvLineBuilder.append(",");

        if (engineer.getLeadership() != null) {
            csvLineBuilder.append(engineer.getLeadership());
        }
        csvLineBuilder.append(",");

        csvLineBuilder.append(nullToEmpty(engineer.getNote())).append(",");

        if (engineer.getRegisteredDate() != null) {
            csvLineBuilder.append(engineer.getRegisteredDate().format(DATE_FORMATTER));
        } else {
            csvLineBuilder.append(LocalDate.now().format(DATE_FORMATTER));
        }

        return csvLineBuilder.toString();
    }

    private String nullToEmpty(String value) {
        return value != null ? escapeComma(value) : "";
    }

    private String escapeComma(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    /**
     * CSV行をEngineerDTOに変換
     * CSV形式のデータをエンジニア情報オブジェクトに変換
     * 
     * @param line CSV行データ
     * @return 変換されたEngineerDTOオブジェクト
     */
    public EngineerDTO convertToDTO(String[] line) {
        if (line == null || line.length < CSVConstants.CSV_HEADERS.length) {
            return null;
        }

        try {
            EngineerBuilder builder = new EngineerBuilder();

            // ID - 全角数字を半角に変換し、標準形式（ID00000）に変換
            if (!line[0].isEmpty()) {
                String idValue = IDValidator.convertFullWidthToHalfWidth(line[0]);

                // 禁止ID（ID00000）チェック - CSVデータでも禁止
                if (IDValidator.isForbiddenId(idValue)) {
                    int i = 0;
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "禁止されている社員ID(ID00000)が含まれています: 行 " + (i + 2));
                    // 無効なIDとしてマーク（空文字列またはエラーフラグ）
                    builder.setId("ERROR_FORBIDDEN_ID");
                } else {
                    String standardizedId = IDValidator.standardizeId(idValue);
                    builder.setId(standardizedId);
                }
            }

            // 氏名
            if (!line[1].isEmpty()) {
                builder.setName(line[1]);
            }

            // フリガナ
            if (!line[2].isEmpty()) {
                builder.setNameKana(line[2]);
            }

            // 生年月日
            if (!line[3].isEmpty()) {
                try {
                    LocalDate birthDate = LocalDate.parse(line[3], DATE_FORMATTER);
                    builder.setBirthDate(birthDate);
                } catch (DateTimeParseException e) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "生年月日の解析に失敗: " + line[3]);
                }
            }

            // 入社年月
            if (!line[4].isEmpty()) {
                try {
                    LocalDate joinDate = LocalDate.parse(line[4], DATE_FORMATTER);
                    builder.setJoinDate(joinDate);
                } catch (DateTimeParseException e) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "入社年月の解析に失敗: " + line[4]);
                }
            }

            // エンジニア歴
            if (!line[5].isEmpty()) {
                try {
                    int career = Integer.parseInt(line[5]);
                    builder.setCareer(career);
                } catch (NumberFormatException e) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "エンジニア歴の解析に失敗: " + line[5]);
                }
            }

            // プログラミング言語
            if (!line[6].isEmpty()) {
                List<String> languages = Arrays.asList(line[6].split(";"));
                builder.setProgrammingLanguages(languages);
            }

            // 経歴
            if (!line[7].isEmpty()) {
                builder.setCareerHistory(line[7]);
            }

            // 研修の受講歴
            if (!line[8].isEmpty()) {
                builder.setTrainingHistory(line[8]);
            }

            // 技術力
            if (!line[9].isEmpty()) {
                try {
                    double technicalSkill = Double.parseDouble(line[9]);
                    builder.setTechnicalSkill(technicalSkill);
                } catch (NumberFormatException e) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "技術力の解析に失敗: " + line[9]);
                    // nullを設定（未評価）
                    builder.setTechnicalSkill(null);
                }
            } else {
                // 空の場合もnullを設定
                builder.setTechnicalSkill(null);
            }

            // 受講態度
            if (!line[10].isEmpty()) {
                try {
                    double learningAttitude = Double.parseDouble(line[10]);
                    builder.setLearningAttitude(learningAttitude);
                } catch (NumberFormatException e) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "受講態度の解析に失敗: " + line[10]);
                    // nullを設定（未評価）
                    builder.setLearningAttitude(null);
                }
            } else {
                // 空の場合もnullを設定
                builder.setLearningAttitude(null);
            }

            // コミュニケーション能力
            if (!line[11].isEmpty()) {
                try {
                    double communicationSkill = Double.parseDouble(line[11]);
                    builder.setCommunicationSkill(communicationSkill);
                } catch (NumberFormatException e) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "コミュニケーション能力の解析に失敗: " + line[11]);
                    // nullを設定（未評価）
                    builder.setCommunicationSkill(null);
                }
            } else {
                // 空の場合もnullを設定
                builder.setCommunicationSkill(null);
            }

            // リーダーシップ
            if (!line[12].isEmpty()) {
                try {
                    double leadership = Double.parseDouble(line[12]);
                    builder.setLeadership(leadership);
                } catch (NumberFormatException e) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "リーダーシップの解析に失敗: " + line[12]);
                    // nullを設定（未評価）
                    builder.setLeadership(null);
                }
            } else {
                // 空の場合もnullを設定
                builder.setLeadership(null);
            }

            // 備考
            if (!line[13].isEmpty()) {
                builder.setNote(line[13]);
            }

            // 登録日時
            if (line.length > 14 && !line[14].isEmpty()) {
                try {
                    LocalDate registeredDate = LocalDate.parse(line[14], DATE_FORMATTER);
                    builder.setRegisteredDate(registeredDate);
                } catch (DateTimeParseException e) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "登録日時の解析に失敗: " + line[14]);
                }
            }

            return builder.build();

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "EngineerDTOへの変換に失敗", e);
            return null;
        }
    }

    /**
     * CSVファイルパスを取得
     * ResourceManagerから管理されているパスを返す
     * 
     * @return CSVファイルパス
     */
    public String getCsvFilePath() {
        return csvFilePath;
    }

    /**
     * ResourceManagerインスタンスを取得
     * テスト用途や外部からのリソース管理が必要な場合に使用
     * 
     * @return ResourceManagerインスタンス
     */
    public ResourceManager getResourceManager() {
        return resourceManager;
    }
}