// src/service/EngineerService.java
package service;

import model.EngineerDTO;
import model.EngineerDAO;
import model.EngineerCSVDAO;
import util.LogHandler;
import util.LogHandler.LogType;
import util.Constants.SystemConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * エンジニア情報のCRUD操作を提供するサービスクラス
 * ビジネスロジックとデータアクセス層の仲介を行う
 *
 * @author Nakano
 */
public class EngineerService {

    /** エンジニアデータアクセスオブジェクト */
    private final EngineerDAO engineerDAO;

    /**
     * コンストラクタ
     */
    public EngineerService() {
        this.engineerDAO = new EngineerCSVDAO();
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "エンジニアサービスを初期化完了");
    }

    /**
     * 全エンジニア情報を取得
     *
     * @return エンジニア情報のリスト
     */
    public List<EngineerDTO> findAll() {
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "エンジニアデータの読み込みを開始");
            List<EngineerDTO> engineers = engineerDAO.findAll();
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "Loaded " + engineers.size() + " engineers");
            return engineers;
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニアデータの読み込みに失敗", e);
            return new ArrayList<>();
        }
    }

    /**
     * IDによりエンジニア情報を取得
     *
     * @param id 検索するエンジニアID
     * @return エンジニア情報（存在しない場合はnull）
     */
    public EngineerDTO findById(String id) {
        try {
            return engineerDAO.findById(id);
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "IDによるエンジニア検索に失敗: " + id, e);
            return null;
        }
    }

    /**
     * エンジニア情報を作成
     *
     * @param engineer 作成するエンジニア情報
     * @return 作成に成功した場合true
     * @throws TooManyRecordsException 登録件数が上限を超える場合
     */
    public boolean create(EngineerDTO engineer) throws TooManyRecordsException {
        try {
            // 現在のエンジニア数をチェック
            List<EngineerDTO> currentEngineers = findAll();

            if (currentEngineers.size() >= SystemConstants.MAX_ENGINEER_RECORDS) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "登録件数が上限(" + SystemConstants.MAX_ENGINEER_RECORDS + "件)に達しています");
                throw new TooManyRecordsException(
                        "登録件数が上限(" + SystemConstants.MAX_ENGINEER_RECORDS + "件)に達しています");
            }

            engineerDAO.save(engineer);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("エンジニア情報を作成: ID=%s, 名前=%s",
                            engineer.getId(), engineer.getName()));
            return true;

        } catch (TooManyRecordsException e) {
            throw e;
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の作成に失敗", e);
            return false;
        }
    }

    /**
     * エンジニア情報を更新
     *
     * @param engineer 更新するエンジニア情報
     * @return 更新に成功した場合true
     */
    public boolean update(EngineerDTO engineer) {
        try {
            engineerDAO.update(engineer);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("エンジニア情報を更新: ID=%s, 名前=%s",
                            engineer.getId(), engineer.getName()));
            return true;
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の更新に失敗", e);
            return false;
        }
    }

    /**
     * エンジニア情報を削除
     *
     * @param ids 削除するエンジニアIDのリスト
     * @return 削除に成功した場合true
     */
    public boolean delete(List<String> ids) {
        try {
            engineerDAO.deleteAll(ids);
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("%d件のエンジニア情報を削除", ids.size()));
            return true;
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "エンジニア情報の削除に失敗", e);
            return false;
        }
    }

    /**
     * 登録件数上限例外クラス
     */
    public static class TooManyRecordsException extends Exception {
        public TooManyRecordsException(String message) {
            super(message);
        }
    }
}