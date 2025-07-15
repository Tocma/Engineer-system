package util;

import java.io.File;
import java.util.logging.Level;

/**
 * ファイル操作に関するユーティリティクラス
 * 重複ファイル名の自動回避機能を提供
 * 
 * @author Nakano
 */
public class FileUtils {

    /**
     * 重複しないファイル名を生成する
     * ファイルが既に存在する場合、ファイル名に括弧と番号を追加
     * 
     * @param originalFile 元のファイル
     * @return 重複しないファイル
     */
    public static File generateUniqueFileName(File originalFile) {
        if (!originalFile.exists()) {
            return originalFile;
        }

        String parentPath = originalFile.getParent();
        String fileName = originalFile.getName();
        String baseName;
        String extension;

        // ファイル名と拡張子を分離
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            baseName = fileName.substring(0, lastDotIndex);
            extension = fileName.substring(lastDotIndex);
        } else {
            baseName = fileName;
            extension = "";
        }

        // 重複しないファイル名を生成
        int counter = 1;
        File uniqueFile;
        do {
            String uniqueName = baseName + "(" + counter + ")" + extension;
            uniqueFile = new File(parentPath, uniqueName);
            counter++;
        } while (uniqueFile.exists());

        LogHandler.getInstance().log(Level.INFO, LogHandler.LogType.SYSTEM,
                "重複ファイル名を回避: " + originalFile.getName() + " → " + uniqueFile.getName());

        return uniqueFile;
    }
}