package controller;

import model.EngineerDTO;
import model.EngineerDAO;
import model.EngineerCSVDAO;
import util.LogHandler;
import util.LogHandler.LogType;
import view.DialogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

/**
 * エンジニア情報に関する操作を制御するコントローラークラス
 * データの取得、追加、更新、検証などを担当
 *
 * <p>
 * このクラスは、エンジニア情報の操作に関するビジネスロジックを実装し、
 * ModelとViewの間の橋渡し役を果たします。主な責務は以下の通りです：
 * <ul>
 * <li>エンジニア情報の取得と管理</li>
 * <li>データの検証</li>
 * <li>データアクセスオブジェクトとの連携</li>
 * </ul>
 * </p>
 *
 * @author Nakano
 * @version 4.4.1
 * @since 2025-05-08
 */
public class EngineerController {

    /** エンジニアデータアクセスオブジェクト */
    private final EngineerDAO engineerDAO;

    // 最大レコード数の定数
    private static final int MAX_RECORDS = 1000;

    /**
     * コンストラクタ
     * EngineerDAOの実装を初期化
     */
    public EngineerController() {
        // CSVファイルを使用するDAOを初期化
        this.engineerDAO = new EngineerCSVDAO();
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "エンジニアコントローラーを初期化しました");
    }

    /**
     * 全エンジニア情報を取得
     *
     * @return エンジニア情報のリスト
     */
    public List<EngineerDTO> loadEngineers() {
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "エンジニアデータの読み込みを開始します");
            List<EngineerDTO> engineers = engineerDAO.findAll();
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "Loaded " + engineers.size() + " engineers.");
            return engineers;
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニアデータの読み込みに失敗しました", e);
            return new ArrayList<>();
        }
    }

    /**
     * エンジニア情報を追加
     *
     * @param engineer 追加するエンジニア情報
     * @return 追加に成功した場合true
     * @throws TooManyRecordsException 追加後のレコード数が上限を超える場合
     */
    public boolean addEngineer(EngineerDTO engineer) throws TooManyRecordsException {
        try {
            // 現在のエンジニア情報を取得
            List<EngineerDTO> currentEngineers = loadEngineers();

            // 既存のエンジニアかどうかをチェック
            boolean isExisting = false;
            for (EngineerDTO existingEngineer : currentEngineers) {
                if (existingEngineer.getId().equals(engineer.getId())) {
                    isExisting = true;
                    break;
                }
            }

            // 新規追加の場合、登録後のレコード数をチェック
            if (!isExisting && currentEngineers.size() >= MAX_RECORDS) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "登録件数が上限(" + MAX_RECORDS + "件)に達しています。これ以上登録できません。");
                throw new TooManyRecordsException("登録件数が上限(" + MAX_RECORDS + "件)に達しています。これ以上登録できません。");
            }

            if (validateEngineer(engineer)) {
                engineerDAO.save(engineer);
                LogHandler.getInstance().log(LogType.SYSTEM,
                        String.format("Added engineer with ID %s and name %s", engineer.getId(), engineer.getName()));
                return true;
            }
            return false;
        } catch (TooManyRecordsException e) {
            // 上限エラーはそのまま再スロー
            throw e;
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の追加に失敗しました", e);
            return false;
        }
    }

    /**
     * 登録件数が上限を超える場合の例外クラス
     */
    public static class TooManyRecordsException extends Exception {
        public TooManyRecordsException(String message) {
            super(message);
        }
    }

    /**
     * エンジニア情報を更新
     *
     * @param engineer 更新するエンジニア情報
     * @return 更新に成功した場合true
     */
    public boolean updateEngineer(EngineerDTO engineer) {
        try {
            if (validateEngineer(engineer)) {
                engineerDAO.update(engineer);
                LogHandler.getInstance().log(LogType.SYSTEM,
                        String.format("Added engineer with ID %s and name %s", engineer.getId(), engineer.getName()));
                return true;
            }
            return false;
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の更新に失敗しました", e);
            return false;
        }
    }

    /**
     * エンジニア情報を一括削除
     *
     * @param targetList 削除対象のエンジニアDTOリスト
     * @param onFinish   完了後に実行する処理（UI更新など）
     */
    // public void deleteEngineers(List<EngineerDTO> targetList, Runnable onFinish)
    // {
    // try {

    // Thread.sleep(3000);

    // List<String> ids = targetList.stream()
    // .map(EngineerDTO::getId)
    // .collect(Collectors.toList());

    // engineerDAO.deleteAll(ids); // EngineerCSVDAOに実装したdeleteAll()を呼ぶ

    // LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
    // String.format("%d件のエンジニア情報を削除しました。", ids.size()));

    // } catch (Exception e) {
    // DialogManager.getInstance().showSystemErrorDialog("削除中にエラーが発生しました。", e);
    // } finally {
    // SwingUtilities.invokeLater(onFinish); // UIスレッドで後処理
    // }
    // }

    public void deleteEngineers(List<EngineerDTO> targetList, Runnable onFinish) {
        try {
            Thread.sleep(3000);

            List<String> ids = targetList.stream()
                    .map(EngineerDTO::getId)
                    .collect(Collectors.toList());

            engineerDAO.deleteAll(ids); // 削除実行
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("%d件のエンジニア情報を削除しました。", ids.size()));
        } catch (Exception e) {
            DialogManager.getInstance().showSystemErrorDialog("削除中にエラーが発生しました。", e);
        } finally {
            SwingUtilities.invokeLater(onFinish); // UIスレッドで後処理
        }
    }

    /**
     * エンジニア情報を検証
     *
     * @param engineer 検証するエンジニア情報
     * @return 検証に成功した場合true
     */
    public boolean validateEngineer(EngineerDTO engineer) {
        // 必須項目のチェック
        if (engineer == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "エンジニア情報がnullです");
            return false;
        }

        if (engineer.getId() == null || engineer.getId().trim().isEmpty()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "社員IDが未設定です");
            return false;
        }

        if (engineer.getName() == null || engineer.getName().trim().isEmpty()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "氏名が未設定です");
            return false;
        }

        if (engineer.getNameKana() == null || engineer.getNameKana().trim().isEmpty()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "フリガナが未設定です");
            return false;
        }

        if (engineer.getBirthDate() == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "生年月日が未設定です");
            return false;
        }

        if (engineer.getJoinDate() == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "入社年月が未設定です");
            return false;
        }

        if (engineer.getProgrammingLanguages() == null || engineer.getProgrammingLanguages().isEmpty()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "扱える言語が未設定です");
            return false;
        }

        return true;
    }

    /**
     * IDによりエンジニア情報を取得
     *
     * @param id 検索するエンジニアID
     * @return エンジニア情報（存在しない場合はnull）
     */
    public EngineerDTO getEngineerById(String id) {
        try {
            return engineerDAO.findById(id);
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の取得に失敗しました: ID=" + id, e);
            return null;
        }
    }
}
