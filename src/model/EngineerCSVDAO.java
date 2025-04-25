package model;

import view.DialogManager;
import util.LogHandler;
import util.LogHandler.LogType;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import util.IDValidator;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * CSVファイルを使用したエンジニア情報のデータアクセスを実装するクラス
 * EngineerDAOインターフェースを実装し、CSVファイルを使用したデータ永続化を提供します
 *
 * <p>
 * このクラスは、CSVファイルを使用してエンジニア情報のCRUD操作を実現します。
 * 主要な責務として以下があります：
 * <ul>
 * <li>エンジニア情報の取得 (findAll, findById)</li>
 * <li>エンジニア情報の保存 (save)</li>
 * <li>エンジニア情報の更新 (update)</li>
 * <li>エンジニア情報の削除 (delete)</li>
 * <li>CSVファイルとDTOオブジェクト間の相互変換</li>
 * <li>CSVファイル読み込み時の重複ID処理</li>
 * <li>バリデーションエラーの処理と結果管理</li>
 * </ul>
 * </p>
 *
 * <p>
 * CSVファイル読み込み時に重複IDが検出された場合は、ユーザーに上書き確認ダイアログを表示し、
 * ユーザーの選択に応じて上書き処理または保持処理を行います。また、バリデーションエラーが
 * 発生した場合は、エラー情報を収集して適切なフィードバックを提供します。
 * </p>
 *
 * <p>
 * この実装では、CSVFileAccessクラスを使用してCSVファイルの読み書き操作を行い、
 * それを元にエンジニア情報のデータアクセス機能を提供します。非同期処理と同時アクセス制御も
 * 備えており、複数のスレッドからの安全なアクセスをサポートします。
 * </p>
 *
 * @author Nagai
 * @version 4.2.2
 * @since 2025-04-25
 */
public class EngineerCSVDAO implements EngineerDAO {

    /** CSVファイルパス */
    private final String csvFilePath;

    /** 日付フォーマット */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /** CSVカラム定義 */
    private static final String[] CSV_HEADERS = {
            "社員ID(必須)", "氏名(必須)", "フリガナ(必須)", "生年月日(必須)",
            "入社年月(必須)", "エンジニア歴(必須)", "扱える言語(必須)", "経歴,研修の受講歴",
            "技術力", "受講態度", "コミュニケーション能力", "リーダーシップ", "備考", "登録日"
    };

    /** DialogManagerインスタンス */
    private final DialogManager dialogManager;

    /**
     * コンストラクタ
     * デフォルトのCSVファイルパスを使用
     */
    public EngineerCSVDAO() {
        this("src/data/engineers.csv");
    }

    /**
     * コンストラクタ
     * 
     * @param csvFilePath CSVファイルのパス
     */
    public EngineerCSVDAO(String csvFilePath) {
        this.csvFilePath = csvFilePath;
        this.dialogManager = DialogManager.getInstance();

        // CSVファイルの存在確認、なければ作成
        checkAndCreateCsvFile();
    }

    /**
     * CSVファイルの存在確認と作成
     * ファイルが存在しない場合は、ヘッダー行を含む新しいファイルを作成
     */
    private void checkAndCreateCsvFile() {
        File file = new File(csvFilePath);

        if (!file.exists()) {
            File parentDir = file.getParentFile();

            // 親ディレクトリが存在しない場合は作成
            if (parentDir != null && !parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs();
                if (!dirCreated) {
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "CSVファイルの親ディレクトリを作成できませんでした: " + parentDir.getPath());
                }
            }

            // ヘッダー行のみを持つCSVファイルを作成
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write(String.join(",", CSV_HEADERS));
                writer.newLine();

                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "CSVファイルを新規作成しました: " + file.getPath());
            } catch (IOException e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "CSVファイルの作成に失敗しました", e);
            }
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

            // 正常に読み込まれたデータを返す
            return result.getSuccessData();

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
            for (int i = 0; i < engineers.size(); i++) {
                if (engineer.getId().equals(engineers.get(i).getId())) {
                    engineers.set(i, engineer);
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
    public void delete(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("IDがnullまたは空です");
        }

        try {
            // 現在のCSVデータを読み込み
            CSVAccessResult currentData = readCSV();
            List<EngineerDTO> engineers = new ArrayList<>(currentData.getSuccessData());

            // 削除対象のエンジニアを探して削除
            engineers.removeIf(engineer -> id.equals(engineer.getId()));

            // CSVに書き込み
            writeCSV(engineers);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "エンジニア情報を削除しました: ID=" + id);

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の削除に失敗しました: ID=" + id, e);
            throw new RuntimeException("エンジニア情報の削除に失敗しました", e);
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
            csvLines.add(String.join(",", CSV_HEADERS));

            // エラーリストをCSV形式に変換
            for (EngineerDTO engineer : errorList) {
                csvLines.add(convertToCSV(engineer));
            }

            // CSVファイルに書き込み
            File file = new File(filePath);
            CSVAccess csvAccess = new CSVAccess("write", csvLines, file);
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
        if (engineerList == null || engineerList.isEmpty()) {
            return false;
        }

        try {
            // ヘッダー行の追加
            List<String> csvLines = new ArrayList<>();
            csvLines.add(String.join(",", CSV_HEADERS));

            // エンジニアリストをCSV形式に変換
            for (EngineerDTO engineer : engineerList) {
                csvLines.add(convertToCSV(engineer));
            }

            // CSVファイルに書き込み
            File file = new File(filePath);
            CSVAccess csvAccess = new CSVAccess("write", csvLines, file);
            csvAccess.execute();

            // 結果を取得
            Object result = csvAccess.getResult();
            boolean success = result instanceof Boolean && (Boolean) result;

            if (success) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "エンジニアデータをCSVファイルに出力しました: " + filePath);
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "エンジニアデータのCSV出力に失敗しました: " + filePath);
            }

            return success;

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニアデータのCSV出力中にエラーが発生しました", e);
            return false;
        }
    }

    /** テンプレート出力機能 */
    public boolean exportTemplate(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            writer.write(String.join(",", CSV_HEADERS)); // ヘッダーだけを書き込む
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
     * CSVファイルからエンジニアデータを読み込み、重複ID処理を行う
     * 
     * @return CSV読み込み結果
     */
    public CSVAccessResult readCSV() {
        try {
            // CSVファイルを読み込み
            File file = new File(csvFilePath);
            CSVAccess csvAccess = new CSVAccess("read", null, file);
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
     * エンジニアデータをCSV形式に変換してファイルに書き込む
     * 
     * @param engineers 書き込むエンジニアデータリスト
     * @return 書き込み成功の場合はtrue
     */
    private boolean writeCSV(List<EngineerDTO> engineers) {
        try {
            // ヘッダー行を含むCSV行リストを作成
            List<String> lines = new ArrayList<>();
            lines.add(String.join(",", CSV_HEADERS));

            // エンジニアデータをCSV形式に変換
            for (EngineerDTO engineer : engineers) {
                String line = convertToCSV(engineer);
                lines.add(line);
            }

            // CSVファイルに書き込み
            File file = new File(csvFilePath);
            CSVAccess csvAccess = new CSVAccess("write", lines, file);
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
        StringBuilder sb = new StringBuilder();

        // id
        sb.append(nullToEmpty(engineer.getId())).append(",");

        // name
        sb.append(nullToEmpty(engineer.getName())).append(",");

        // nameKana
        sb.append(nullToEmpty(engineer.getNameKana())).append(",");

        // birthDate
        if (engineer.getBirthDate() != null) {
            sb.append(engineer.getBirthDate().format(DATE_FORMATTER));
        }
        sb.append(",");

        // joinDate
        if (engineer.getJoinDate() != null) {
            sb.append(engineer.getJoinDate().format(DATE_FORMATTER));
        }
        sb.append(",");

        // career
        sb.append(engineer.getCareer()).append(",");

        // programmingLanguages
        if (engineer.getProgrammingLanguages() != null && !engineer.getProgrammingLanguages().isEmpty()) {
            sb.append(String.join(";", engineer.getProgrammingLanguages()));
        }
        sb.append(",");

        // careerHistory
        sb.append(nullToEmpty(engineer.getCareerHistory())).append(",");

        // trainingHistory
        sb.append(nullToEmpty(engineer.getTrainingHistory())).append(",");

        // technicalSkill
        if (engineer.getTechnicalSkill() != null) {
            sb.append(engineer.getTechnicalSkill());
        }
        sb.append(",");

        // learningAttitude
        if (engineer.getLearningAttitude() != null) {
            sb.append(engineer.getLearningAttitude());
        }
        sb.append(",");

        // communicationSkill
        if (engineer.getCommunicationSkill() != null) {
            sb.append(engineer.getCommunicationSkill());
        }
        sb.append(",");

        // leadership
        if (engineer.getLeadership() != null) {
            sb.append(engineer.getLeadership());
        }
        sb.append(",");

        // note
        sb.append(nullToEmpty(engineer.getNote())).append(",");

        // registeredDate
        if (engineer.getRegisteredDate() != null) {
            sb.append(engineer.getRegisteredDate().format(DATE_FORMATTER));
        } else {
            sb.append(LocalDate.now().format(DATE_FORMATTER));
        }

        return sb.toString();
    }

    /**
     * CSV行をEngineerDTOに変換
     * CSV形式のデータをエンジニア情報オブジェクトに変換
     * 
     * @param line CSV行データ
     * @return 変換されたEngineerDTOオブジェクト
     */
    public EngineerDTO convertToDTO(String[] line) {
        if (line == null || line.length < CSV_HEADERS.length) {
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
     * 
     * @return CSVファイルパス
     */
    public String getCsvFilePath() {
        return csvFilePath;
    }
}