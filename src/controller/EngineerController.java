// src/controller/EngineerController.java（修正版）
package controller;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import model.EngineerDTO;
import service.EngineerService;
import util.LogHandler;
import util.LogHandler.LogType;
import view.DialogManager;

/**
 * エンジニア情報に関する操作を制御するコントローラークラス
 * Service層を通じてビジネスロジックを実行
 *
 * @author Nakano
 */
public class EngineerController {

    /** エンジニアサービス */
    private final EngineerService engineerService;

    /**
     * コンストラクタ
     */
    public EngineerController() {
        this.engineerService = new EngineerService();
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "エンジニアコントローラーを初期化完了");
    }

    /**
     * 全エンジニア情報を取得
     *
     * @return エンジニア情報のリスト
     */
    public List<EngineerDTO> loadEngineers() {
        return engineerService.findAll();
    }

    /**
     * IDによりエンジニア情報を取得
     *
     * @param id 検索するエンジニアID
     * @return エンジニア情報（存在しない場合はnull）
     */
    public EngineerDTO getEngineerById(String id) {
        return engineerService.findById(id);
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
            if (validateEngineer(engineer)) {
                // 既存のエンジニアかチェック
                EngineerDTO existing = engineerService.findById(engineer.getId());
                if (existing != null) {
                    return engineerService.update(engineer);
                } else {
                    return engineerService.create(engineer);
                }
            }
            return false;
        } catch (EngineerService.TooManyRecordsException _e) {
            throw new TooManyRecordsException(_e.getMessage());
        }
    }

    /**
     * エンジニア情報を更新
     *
     * @param engineer 更新するエンジニア情報
     * @return 更新に成功した場合true
     */
    public boolean updateEngineer(EngineerDTO engineer) {
        if (validateEngineer(engineer)) {
            return engineerService.update(engineer);
        }
        return false;
    }

    /**
     * エンジニア情報を削除
     *
     * @param targetList 削除対象のエンジニア情報リスト
     * @param onFinish   削除完了後に実行する処理
     */
    public void deleteEngineers(List<EngineerDTO> targetList, Runnable onFinish) {
        try {

            List<String> ids = targetList.stream()
                    .map(EngineerDTO::getId)
                    .collect(Collectors.toList());

            boolean success = engineerService.delete(ids);

            // 削除に成功した場合はログ出力
            if (success) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "エンジニア情報を削除しました: ID=" + ids + "氏名=" +
                                targetList.stream()
                                        .map(EngineerDTO::getName)
                                        .collect(Collectors.joining(", ")));
            }

            if (!success) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "エンジニア情報を削除しました: ID=" + ids + "氏名=" +
                                targetList.stream()
                                        .map(EngineerDTO::getName)
                                        .collect(Collectors.joining(", ")));
                throw new RuntimeException("削除処理に失敗しました");

            }

        } catch (Exception _e) {
            DialogManager.getInstance().showSystemErrorDialog("削除中にエラーが発生。", _e);
        } finally {
            SwingUtilities.invokeLater(onFinish);
        }
    }

    /**
     * エンジニア情報を検証
     *
     * @param engineer 検証するエンジニア情報
     * @return 検証に成功した場合true
     */
    private boolean validateEngineer(EngineerDTO engineer) {
        if (engineer == null) {
            return false;
        }

        if (engineer.getId() == null || engineer.getId().trim().isEmpty()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "エンジニアIDが未設定です");
            return false;
        }

        if (engineer.getName() == null || engineer.getName().trim().isEmpty()) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "エンジニア名が未設定です");
            return false;
        }

        return true;
    }

    /**
     * 登録件数が上限を超える場合の例外クラス
     */
    public static class TooManyRecordsException extends Exception {
        public TooManyRecordsException(String message) {
            super(message);
        }
    }
}