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
 * EngineerDAOインターフェースを実装し、CSVファイルを使用したデータ永続化を提供します
 * 
 * バージョン4.4.2での主な改善点：
 * - ResourceManagerとの統合によるファイルパス管理の一元化
 * - ファイル作成処理のResourceManagerへの委譲
 * - エラーハンドリングの統一とログ出力の改善
 * - リソースリーク防止機能の強化
 *
 * @author Nakano
 */
public class EngineerCSVDAO implements EngineerDAO {

    /** CSVファイルパス（ResourceManagerから取得） */
    private final String csvFilePath;

    /** ResourceManagerインスタンス（リソース管理の一元化） */
    private final ResourceManager resourceManager;

    /** 日付フォーマット */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /** CSVカラム定義 */

    /** DialogManagerインスタンス */
    private final DialogManager dialogManager;

    /**
     * デフォルトコンストラクタ
     * ResourceManagerから標準のCSVファイルパスを取得して初期化
     * 
     * この設計により、ファイルパスの管理がResourceManagerに一元化され、
     * 将来的な設定変更やディレクトリ構造の変更に柔軟に対応できます。
     */
    public EngineerCSVDAO() {
        // ResourceManagerのシングルトンインスタンスを取得
        this.resourceManager = ResourceManager.getInstance();

        try {
            // ResourceManagerが初期化されていない場合は初期化を実行
            if (!resourceManager.isInitialized()) {
                resourceManager.initialize();
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "ResourceManagerを初期化しました");
            }

            // ResourceManagerからエンジニアCSVファイルのパスを取得
            // これにより、ファイルパスの管理が一元化されます
            this.csvFilePath = resourceManager.getEngineerCsvPath().toString();

        } catch (IOException e) {
            // ResourceManagerの初期化に失敗した場合のフォールバック処理
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "ResourceManagerの初期化に失敗しました。フォールバックパスを使用します", e);
            throw new RuntimeException("ResourceManagerの初期化に失敗しました", e);
        }

        this.dialogManager = DialogManager.getInstance();

        // CSVファイルの存在確認と作成処理をResourceManagerに委譲
        // この設計により、ファイル管理ロジックの重複を排除できます
        ensureCsvFileExists();
    }

    /**
     * 指定されたCSVファイルパスを使用するコンストラクタ
     * テスト用途や特別な要件がある場合に使用
     * 
     * 注意：このコンストラクタを使用する場合、ResourceManagerとの
     * 統合メリットを一部失うことになります。通常はデフォルト
     * コンストラクタの使用を推奨します。
     * 
     * @param csvFilePath CSVファイルのパス
     */
    public EngineerCSVDAO(String csvFilePath) {
        this.resourceManager = ResourceManager.getInstance();
        this.csvFilePath = csvFilePath;
        this.dialogManager = DialogManager.getInstance();

        // 指定されたパスでもCSVファイルの存在確認を実行
        ensureCsvFileExists();
    }

    /**
     * CSVファイルの存在確認と作成処理
     * ResourceManagerの機能を活用してファイル管理を一元化
     * 
     * この実装により、以下のメリットが得られます：
     * - ファイル作成ロジックの重複排除
     * - 統一されたエラーハンドリング
     * - ResourceManagerによるリソース追跡
     */
    private void ensureCsvFileExists() {
        try {
            File csvFile = new File(csvFilePath);

            // ファイルが存在しない場合の処理
            if (!csvFile.exists()) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "CSVファイルが存在しないため、新規作成します: " + csvFilePath);

                // 親ディレクトリの確認と作成をResourceManagerに委譲
                File parentDir = csvFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    // ResourceManagerのcreateDirectoryメソッドを使用してディレクトリを作成
                    // この方法により、ディレクトリ作成のエラーハンドリングも統一されます
                    resourceManager.createDirectory(parentDir.getName());
                }

                // CSVファイルの作成処理
                createInitialCsvFile(csvFile);

                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "新規CSVファイルを作成しました: " + csvFilePath);
            } else {
            }

        } catch (IOException e) {
            // エラーハンドリングを統一し、適切なログ出力を行います
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "CSVファイルの確認・作成処理中にエラーが発生しました: " + csvFilePath, e);
            throw new RuntimeException("CSVファイルの初期化に失敗しました", e);
        }
    }

    /**
     * 初期CSVファイルの作成処理
     * ヘッダー行を含む新しいCSVファイルを作成
     * 
     * この処理では、リソースリークを防ぐためのtry-with-resources文を使用し、
     * 作成されたファイルストリームをResourceManagerに登録して適切な管理を行います。
     * 
     * @param csvFile 作成するCSVファイル
     * @throws IOException ファイル作成に失敗した場合
     */
    private void createInitialCsvFile(File csvFile) throws IOException {
        // try-with-resources文を使用してリソースリークを防止
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8))) {

            // ResourceManagerにファイルストリームを登録してリソース管理を委譲
            // この登録により、アプリケーション終了時に確実にリソースが解放されます
            String resourceKey = "csv_initial_writer_" + System.currentTimeMillis();
            resourceManager.registerResource(resourceKey, writer);

            // CSVヘッダーの書き込み
            writer.write(String.join(",", CSVConstants.CSV_HEADERS));
            writer.newLine();

            // 処理完了後にResourceManagerからリソースを解除
            // この操作により、適切なタイミングでリソースがクリーンアップされます
            resourceManager.releaseResource(resourceKey);

        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "初期CSVファイルの作成に失敗しました: " + csvFile.getPath(), e);
            throw e;
        }
    }

    @Override
    public List<EngineerDTO> findAll() {
        try {
            // CSVファイルの読み込み
            CSVAccessResult result = readCSV();

            // 致命的エラーがあれば空リストを返す
            if (result.isFatalError()) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "エンジニアデータの読み込みに失敗しました: " + result.getErrorMessage());
                return new ArrayList<>();
            }

            // 読み込んだデータの件数をチェック
            List<EngineerDTO> successData = result.getSuccessData();
            if (successData.size() > SystemConstants.MAX_ENGINEER_RECORDS) {
                // 1000件を超える場合はエラーログを出力
                LogHandler.getInstance().log(Level.SEVERE, LogType.SYSTEM,
                        "登録されているエンジニアデータが上限(" + SystemConstants.MAX_ENGINEER_RECORDS + "件)を超えています: "
                                + successData.size() + "件");

                // データ件数制限エラーをユーザーに通知する必要があるが、
                // このタイミングではUIスレッドでの操作ができないため、MainControllerを介して行う
                return successData; // エラーチェックは呼び出し元で行う
            }

            // 正常に読み込まれたデータを返す
            return successData;

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニアリストの取得に失敗しました", e);
            return new ArrayList<>();
        }
    }

    @Override
    public EngineerDTO findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        try {
            // すべてのエンジニアデータを取得
            List<EngineerDTO> engineers = findAll();

            // 指定されたIDと一致するエンジニアを検索
            for (EngineerDTO engineer : engineers) {
                if (id.equals(engineer.getId())) {
                    return engineer;
                }
            }

            return null;

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "IDによるエンジニア検索に失敗しました: " + id, e);
            return null;
        }
    }

    @Override
    public void save(EngineerDTO engineer) {
        if (engineer == null) {
            throw new IllegalArgumentException("エンジニア情報がnullです");
        }

        try {
            // 現在のCSVデータを読み込み
            CSVAccessResult currentData = readCSV();
            List<EngineerDTO> engineers = new ArrayList<>(currentData.getSuccessData());

            // 新しいエンジニアを追加
            engineers.add(engineer);

            // CSVに書き込み
            writeCSV(engineers);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("エンジニア情報を保存しました: ID=%s, 名前=%s", engineer.getId(), engineer.getName()));

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の保存に失敗しました", e);
            throw new RuntimeException("エンジニア情報の保存に失敗しました", e);
        }
    }

    @Override
    public void update(EngineerDTO engineer) {
        if (engineer == null) {
            throw new IllegalArgumentException("エンジニア情報がnullです");
        }

        try {
            // 現在のCSVデータを読み込み
            CSVAccessResult currentData = readCSV();
            List<EngineerDTO> engineers = new ArrayList<>(currentData.getSuccessData());
            boolean updated = false;

            // 更新対象のエンジニアを探して更新
            for (int engineerIndex = 0; engineerIndex < engineers.size(); engineerIndex++) {
                if (engineer.getId().equals(engineers.get(engineerIndex).getId())) {
                    engineers.set(engineerIndex, engineer);
                    updated = true;
                    break;
                }
            }

            // 更新対象が見つからない場合は追加
            if (!updated) {
                engineers.add(engineer);
            }

            // CSVに書き込み
            writeCSV(engineers);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("エンジニア情報を更新しました: ID=%s, 名前=%s", engineer.getId(), engineer.getName()));

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の更新に失敗しました", e);
            throw new RuntimeException("エンジニア情報の更新に失敗しました", e);
        }
    }

    @Override
    public void deleteAll(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("削除対象のIDリストが空です");
        }

        try {
            // メモリにあるデータはそのまま、まず削除後のリストを作る
            CSVAccessResult currentData = readCSV();
            List<EngineerDTO> original = new ArrayList<>(currentData.getSuccessData());

            // まず「CSVに書き込むための削除済みリスト」を生成
            List<EngineerDTO> filtered = original.stream()
                    .filter(dto -> !ids.contains(dto.getId()))
                    .toList();

            // CSVに先に書き込み
            writeCSV(filtered);

            // メモリ上のリスト（currentData）からも除外
            original.removeIf(dto -> ids.contains(dto.getId()));

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("CSV削除後、%d件のエンジニアをメモリ上からも削除しました", ids.size()));

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の一括削除に失敗しました", e);
            throw new RuntimeException("エンジニア情報の一括削除に失敗しました", e);
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
                        "エラーリストをCSVファイルに出力しました: " + filePath);
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "エラーリストのCSV出力に失敗しました: " + filePath);
            }

            return success;

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エラーリストのCSV出力中にエラーが発生しました", e);
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
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            writer.write(String.join(",", CSVConstants.CSV_HEADERS)); // ヘッダーだけを書き込む
            writer.newLine();

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "テンプレートCSVファイルを出力しました: " + filePath);
            return true;

        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "テンプレートCSV出力に失敗しました", e);
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
            // ResourceManagerから管理されているCSVファイルを使用
            File file = new File(csvFilePath);
            CSVAccess csvAccess = new CSVAccess("read", file);
            csvAccess.execute();

            // CSVAccessの結果を取得
            Object result = csvAccess.getResult();

            if (result instanceof CSVAccessResult) {
                CSVAccessResult accessResult = (CSVAccessResult) result;

                // 重複IDの処理
                if (accessResult.hasDuplicateIds() && !accessResult.isOverwriteConfirmed()) {
                    handleDuplicateIds(accessResult);
                }

                return accessResult;
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "CSV読み込み結果が不正な形式です: " + (result != null ? result.getClass().getName() : "null"));
                return new CSVAccessResult(new ArrayList<>(), new ArrayList<>(), true, "CSV読み込み結果が不正な形式です");
            }

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSV読み込み中にエラーが発生しました", e);
            return new CSVAccessResult(new ArrayList<>(), new ArrayList<>(), true,
                    "CSV読み込み中にエラーが発生しました: " + e.getMessage());
        }
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
            // ヘッダー行を含むCSV行リストを作成
            List<String> lines = new ArrayList<>();
            lines.add(String.join(",", CSVConstants.CSV_HEADERS));

            // エンジニアデータをCSV形式に変換
            for (EngineerDTO engineer : engineers) {
                String line = convertToCSV(engineer);
                lines.add(line);
            }

            CSVAccess csvAccess = new CSVAccess("write", lines);
            csvAccess.execute();

            // 結果を取得
            Object result = csvAccess.getResult();

            if (result instanceof Boolean) {
                return (Boolean) result;
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "CSV書き込み結果が不正な形式です: " + (result != null ? result.getClass().getName() : "null"));
                return false;
            }

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSV書き込み中にエラーが発生しました", e);
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
            // 重複ID確認ダイアログを表示
            boolean overwrite = dialogManager.showDuplicateIdConfirmDialog(result.getDuplicateIds());

            // 上書き確認結果を設定
            result.setOverwriteConfirmed(overwrite);

            if (overwrite) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "重複IDの上書きが確認されました: " + result.getDuplicateIds().size() + "件");
            } else {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "重複IDの保持が確認されました: " + result.getDuplicateIds().size() + "件");

                // 重複IDを持つデータを成功データから削除（上書き拒否の場合）
                List<EngineerDTO> filteredData = result.getSuccessData().stream()
                        .filter(engineer -> !result.getDuplicateIds().contains(engineer.getId()))
                        .collect(Collectors.toList());

                // 成功データを更新
                result.getSuccessData().clear();
                result.getSuccessData().addAll(filteredData);
            }

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "重複ID処理中にエラーが発生しました", e);

            // エラー時は上書きしない
            result.setOverwriteConfirmed(false);

            // 重複IDを持つデータを成功データから削除
            List<EngineerDTO> filteredData = result.getSuccessData().stream()
                    .filter(engineer -> !result.getDuplicateIds().contains(engineer.getId()))
                    .collect(Collectors.toList());

            // 成功データを更新
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
        StringBuilder csvLineBuilder = new StringBuilder();

        // id
        csvLineBuilder.append(nullToEmpty(engineer.getId())).append(",");

        // name
        csvLineBuilder.append(nullToEmpty(engineer.getName())).append(",");

        // nameKana
        csvLineBuilder.append(nullToEmpty(engineer.getNameKana())).append(",");

        // birthDate
        if (engineer.getBirthDate() != null) {
            csvLineBuilder.append(engineer.getBirthDate().format(DATE_FORMATTER));
        }
        csvLineBuilder.append(",");

        // joinDate
        if (engineer.getJoinDate() != null) {
            csvLineBuilder.append(engineer.getJoinDate().format(DATE_FORMATTER));
        }
        csvLineBuilder.append(",");

        // career
        csvLineBuilder.append(engineer.getCareer()).append(",");

        // programmingLanguages
        if (engineer.getProgrammingLanguages() != null && !engineer.getProgrammingLanguages().isEmpty()) {
            csvLineBuilder.append(String.join(";", engineer.getProgrammingLanguages()));
        }
        csvLineBuilder.append(",");

        // careerHistory
        csvLineBuilder.append(nullToEmpty(engineer.getCareerHistory())).append(",");

        // trainingHistory
        csvLineBuilder.append(nullToEmpty(engineer.getTrainingHistory())).append(",");

        // technicalSkill
        if (engineer.getTechnicalSkill() != null) {
            csvLineBuilder.append(engineer.getTechnicalSkill());
        }
        csvLineBuilder.append(",");

        // learningAttitude
        if (engineer.getLearningAttitude() != null) {
            csvLineBuilder.append(engineer.getLearningAttitude());
        }
        csvLineBuilder.append(",");

        // communicationSkill
        if (engineer.getCommunicationSkill() != null) {
            csvLineBuilder.append(engineer.getCommunicationSkill());
        }
        csvLineBuilder.append(",");

        // leadership
        if (engineer.getLeadership() != null) {
            csvLineBuilder.append(engineer.getLeadership());
        }
        csvLineBuilder.append(",");

        // note
        csvLineBuilder.append(nullToEmpty(engineer.getNote())).append(",");

        // registeredDate
        if (engineer.getRegisteredDate() != null) {
            csvLineBuilder.append(engineer.getRegisteredDate().format(DATE_FORMATTER));
        } else {
            csvLineBuilder.append(LocalDate.now().format(DATE_FORMATTER));
        }

        return csvLineBuilder.toString();
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
                            "生年月日の解析に失敗しました: " + line[3]);
                }
            }

            // 入社年月
            if (!line[4].isEmpty()) {
                try {
                    LocalDate joinDate = LocalDate.parse(line[4], DATE_FORMATTER);
                    builder.setJoinDate(joinDate);
                } catch (DateTimeParseException e) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "入社年月の解析に失敗しました: " + line[4]);
                }
            }

            // エンジニア歴
            if (!line[5].isEmpty()) {
                try {
                    int career = Integer.parseInt(line[5]);
                    builder.setCareer(career);
                } catch (NumberFormatException e) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "エンジニア歴の解析に失敗しました: " + line[5]);
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
                            "技術力の解析に失敗しました: " + line[9]);
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
                            "受講態度の解析に失敗しました: " + line[10]);
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
                            "コミュニケーション能力の解析に失敗しました: " + line[11]);
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
                            "リーダーシップの解析に失敗しました: " + line[12]);
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
                            "登録日時の解析に失敗しました: " + line[14]);
                }
            }

            return builder.build();

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "EngineerDTOへの変換に失敗しました", e);
            return null;
        }
    }

    /**
     * nullを空文字列に変換
     * CSV出力時にnullを安全に扱うためのユーティリティメソッド
     * 
     * @param value 変換する文字列
     * @return nullの場合は空文字列、それ以外は元の値
     */
    private String nullToEmpty(String value) {
        return value != null ? escapeComma(value) : "";
    }

    /**
     * カンマをエスケープ
     * CSV形式でカンマを含む文字列を適切に処理
     * 
     * @param value エスケープする文字列
     * @return エスケープされた文字列
     */
    private String escapeComma(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        // カンマが含まれている場合は二重引用符で囲む
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
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