// src/service/CSVExportService.java
package service;

import model.EngineerDTO;
import model.EngineerCSVDAO;
import util.LogHandler;
import util.LogHandler.LogType;
import util.ResourceManager;
import java.io.File;
import java.util.List;
import java.util.logging.Level;

/**
 * CSV出力サービス
 * CSV出力・テンプレート出力に関するビジネスロジックを担当
 * 
 * @author Nakano
 */
public class CSVExportService {

    private final EngineerCSVDAO engineerDAO;

    public CSVExportService(EngineerCSVDAO engineerDAO) {
        this.engineerDAO = engineerDAO;
        ResourceManager.getInstance();
    }

    /**
     * テンプレートCSV出力
     * 
     * @param selectedFile 出力先ファイル
     * @return 出力成功の場合true
     */
    public boolean exportTemplate(File selectedFile) {
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "テンプレートCSV出力を開始: " + selectedFile.getPath());

            boolean success = engineerDAO.exportTemplate(selectedFile.getPath());

            if (success) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "テンプレートCSV出力完了: " + selectedFile.getPath());
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "テンプレートCSV出力失敗: " + selectedFile.getPath());
            }

            return success;

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "テンプレート出力中にエラー発生", e);
            return false;
        }
    }

    /**
     * エンジニア情報CSV出力
     * 
     * @param targetList   出力対象リスト
     * @param selectedFile 出力先ファイル
     * @return 出力成功の場合true
     */
    public boolean exportCSV(List<EngineerDTO> targetList, File selectedFile) {
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("CSV出力を開始: %d件のデータを %s に出力",
                            targetList.size(), selectedFile.getPath()));

            boolean success = engineerDAO.exportCSV(targetList, selectedFile.getPath());

            if (success) {
                LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                        "CSV出力完了: " + selectedFile.getPath());
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "CSV出力失敗: " + selectedFile.getPath());
            }

            return success;

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "CSV出力中にエラー発生", e);
            return false;
        }
    }

    /**
     * ファイル書き込み権限チェック
     * 
     * @param file チェック対象ファイル
     * @return 書き込み可能な場合true
     */
    public boolean canWriteFile(File file) {
        try {
            if (file.exists()) {
                return file.canWrite();
            } else {
                File parent = file.getParentFile();
                return parent != null && parent.canWrite();
            }
        } catch (SecurityException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "ファイル権限チェックエラー", e);
            return false;
        }
    }
}