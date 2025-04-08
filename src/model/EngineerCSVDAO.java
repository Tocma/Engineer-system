package model;

import util.LogHandler;
import util.LogHandler.LogType;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * CSVファイルによるエンジニア情報のデータアクセスを実装するクラス
 * EngineerDAOインターフェースを実装し、CSVファイルを使用したデータ永続化を提供
 *
 * <p>
 * このクラスは、CSVファイルを使用してエンジニア情報のCRUD操作を実現します：
 * <ul>
 * <li>ファイル読み書きの処理</li>
 * <li>CSVデータとDTOオブジェクトの相互変換</li>
 * <li>同時アクセス制御</li>
 * </ul>
 * </p>
 *
 * @author Nakano
 * @version 2.1.0
 * @since 2025-04-03
 */
public class EngineerCSVDAO implements EngineerDAO {

    /** CSVファイルパス */
    private final String csvFilePath;

    /** 同時アクセス制御用のロック */
    private final ReadWriteLock lock;

    /** 日付フォーマット */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /** CSVカラム定義 */
    private static final String[] CSV_HEADERS = {
            "id", "name", "nameKana", "birthDate", "joinDate", "career",
            "programmingLanguages", "careerHistory", "trainingHistory",
            "technicalSkill", "learningAttitude", "communicationSkill",
            "leadership", "note", "registeredDate"
    };

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
        this.lock = new ReentrantReadWriteLock();

        // CSVファイルの存在確認、なければ作成
        checkAndCreateCsvFile();
    }

    /**
     * CSVファイルの存在確認と作成
     */
    private void checkAndCreateCsvFile() {
        Path path = Paths.get(csvFilePath);
        if (!Files.exists(path)) {
            try {
                // 親ディレクトリがなければ作成
                Files.createDirectories(path.getParent());

                // ヘッダー行を書き込む
                try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    writer.write(String.join(",", CSV_HEADERS));
                    writer.newLine();
                }

                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, csvFilePath);
            } catch (IOException e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "CSVファイルの作成に失敗しました", e);
            }
        }
    }

    @Override
    public List<EngineerDTO> findAll() {
        List<EngineerDTO> engineers = new ArrayList<>();
        List<String[]> csvData = readCSV();

        // ヘッダー行をスキップ（最初の行）
        for (int i = 1; i < csvData.size(); i++) {
            String[] line = csvData.get(i);
            try {
                EngineerDTO engineer = convertToDTO(line);
                if (engineer != null) {
                    engineers.add(engineer);
                }
            } catch (Exception e) {
                LogHandler.getInstance().logError(LogType.SYSTEM, "CSV行の変換に失敗しました: 行=" + i, e);
            }
        }

        return engineers;
    }

    @Override
    public EngineerDTO findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        List<String[]> csvData = readCSV();

        // ヘッダー行をスキップしてデータを検索
        for (int i = 1; i < csvData.size(); i++) {
            String[] line = csvData.get(i);
            if (line.length > 0 && id.equals(line[0])) {
                try {
                    return convertToDTO(line);
                } catch (Exception e) {
                    LogHandler.getInstance().logError(LogType.SYSTEM, "CSV行の変換に失敗しました: ID=" + id, e);
                    return null;
                }
            }
        }

        return null;
    }

    @Override
    public void save(EngineerDTO engineer) {
        if (engineer == null) {
            throw new IllegalArgumentException("エンジニア情報がnullです");
        }

        List<String[]> csvData = readCSV();

        // 新しい行を追加
        csvData.add(convertToCSV(engineer));

        // CSVファイルに書き込み
        writeCSV(csvData);
    }

    @Override
    public void update(EngineerDTO engineer) {
        if (engineer == null) {
            throw new IllegalArgumentException("エンジニア情報がnullです");
        }

        List<String[]> csvData = readCSV();
        boolean updated = false;

        // IDに一致する行を探して更新
        for (int i = 1; i < csvData.size(); i++) {
            String[] line = csvData.get(i);
            if (line.length > 0 && engineer.getId().equals(line[0])) {
                csvData.set(i, convertToCSV(engineer));
                updated = true;
                break;
            }
        }

        // 一致する行がなければ追加
        if (!updated) {
            csvData.add(convertToCSV(engineer));
        }

        // CSVファイルに書き込み
        writeCSV(csvData);
    }

    @Override
    public void delete(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("IDがnullまたは空です");
        }

        List<String[]> csvData = readCSV();

        // IDに一致する行を削除
        csvData.removeIf(line -> line.length > 0 && id.equals(line[0]));

        // CSVファイルに書き込み
        writeCSV(csvData);
    }

    /**
     * CSVファイルからデータを読み込む
     * 
     * @return CSV行データのリスト
     */
    private List<String[]> readCSV() {
        List<String[]> data = new ArrayList<>();

        lock.readLock().lock();
        try {
            Path path = Paths.get(csvFilePath);
            if (!Files.exists(path)) {
                return data;
            }

            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                // カンマでスプリットする際に、空フィールドも保持
                String[] values = line.split(",", -1);
                data.add(values);
            }
        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVファイルの読み込みに失敗しました", e);
        } finally {
            lock.readLock().unlock();
        }

        return data;
    }

    /**
     * CSVファイルにデータを書き込む
     * 
     * @param data CSV行データのリスト
     */
    private void writeCSV(List<String[]> data) {
        lock.writeLock().lock();
        try {
            Path path = Paths.get(csvFilePath);
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                for (String[] line : data) {
                    writer.write(String.join(",", line));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVファイルの書き込みに失敗しました", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * CSV行データをEngineerDTOに変換
     * 
     * @param line CSV行データ
     * @return 変換されたEngineerDTOオブジェクト
     */
    private EngineerDTO convertToDTO(String[] line) {
        if (line == null || line.length < CSV_HEADERS.length) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "CSV行のカラム数が不足しています");
            return null;
        }

        EngineerBuilder builder = new EngineerBuilder();

        try {
            // 必須フィールドの設定
            builder.setId(line[0]);
            builder.setName(line[1]);
            builder.setNameKana(line[2]);

            // 日付の変換
            if (!line[3].isEmpty()) {
                builder.setBirthDate(LocalDate.parse(line[3], DATE_FORMATTER));
            }

            if (!line[4].isEmpty()) {
                builder.setJoinDate(LocalDate.parse(line[4], DATE_FORMATTER));
            }

            // 数値の変換
            if (!line[5].isEmpty()) {
                builder.setCareer(Integer.parseInt(line[5]));
            }

            // リストの変換（カンマ区切り文字列をリストに）
            if (!line[6].isEmpty()) {
                List<String> languages = Arrays.asList(line[6].split(";"));
                builder.setProgrammingLanguages(languages);
            } else {
                // 空の場合でも最低1つは必要なので、デフォルト値を設定
                builder.setProgrammingLanguages(Arrays.asList("未設定"));
            }

            // 任意フィールドの設定
            if (!line[7].isEmpty())
                builder.setCareerHistory(line[7]);
            if (!line[8].isEmpty())
                builder.setTrainingHistory(line[8]);

            // 数値評価の変換
            if (!line[9].isEmpty())
                builder.setTechnicalSkill(Double.parseDouble(line[9]));
            if (!line[10].isEmpty())
                builder.setLearningAttitude(Double.parseDouble(line[10]));
            if (!line[11].isEmpty())
                builder.setCommunicationSkill(Double.parseDouble(line[11]));
            if (!line[12].isEmpty())
                builder.setLeadership(Double.parseDouble(line[12]));

            // その他のフィールド
            if (!line[13].isEmpty())
                builder.setNote(line[13]);

            // 登録日時
            if (!line[14].isEmpty()) {
                builder.setRegisteredDate(LocalDate.parse(line[14], DATE_FORMATTER));
            }

            return builder.build();

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "DTOへの変換中にエラーが発生しました: ID=" + line[0], e);
            return null;
        }
    }

    /**
     * EngineerDTOをCSV行データに変換
     * 
     * @param engineer 変換するEngineerDTOオブジェクト
     * @return 変換されたCSV行データ
     */
    private String[] convertToCSV(EngineerDTO engineer) {
        String[] line = new String[CSV_HEADERS.length];

        // 必須フィールド
        line[0] = engineer.getId();
        line[1] = engineer.getName();
        line[2] = engineer.getNameKana();
        line[3] = engineer.getBirthDate() != null ? engineer.getBirthDate().format(DATE_FORMATTER) : "";
        line[4] = engineer.getJoinDate() != null ? engineer.getJoinDate().format(DATE_FORMATTER) : "";
        line[5] = String.valueOf(engineer.getCareer());

        // プログラミング言語リストをセミコロン区切りに変換
        if (engineer.getProgrammingLanguages() != null && !engineer.getProgrammingLanguages().isEmpty()) {
            line[6] = String.join(";", engineer.getProgrammingLanguages());
        } else {
            line[6] = "";
        }

        // 任意フィールド
        line[7] = engineer.getCareerHistory() != null ? engineer.getCareerHistory() : "";
        line[8] = engineer.getTrainingHistory() != null ? engineer.getTrainingHistory() : "";
        line[9] = String.valueOf(engineer.getTechnicalSkill());
        line[10] = String.valueOf(engineer.getLearningAttitude());
        line[11] = String.valueOf(engineer.getCommunicationSkill());
        line[12] = String.valueOf(engineer.getLeadership());
        line[13] = engineer.getNote() != null ? engineer.getNote() : "";

        // 登録日時
        line[14] = engineer.getRegisteredDate() != null ? engineer.getRegisteredDate().format(DATE_FORMATTER)
                : LocalDate.now().format(DATE_FORMATTER);

        return line;
    }
}
