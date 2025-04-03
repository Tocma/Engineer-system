package model;

import util.LogHandler;
import util.LogHandler.LogType;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * CSVファイルへのアクセスを実装するクラス
 * ファイル操作とデータの読み書きを担当
 *
 * <p>
 * このクラスは、CSVファイルへの読み書き操作を実装し、同時アクセス制御を提供します。
 * AccessThreadクラスを継承し、バックグラウンドでの非同期ファイルアクセスを実現します。
 * </p>
 *
 * @author Nakano
 * @version 2.1.0
 * @since 2025-04-03
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

    /**
     * コンストラクタ
     * 
     * @param operation 操作種別（"read"または"write"）
     * @param data      操作対象データ（書き込み時のみ使用）
     * @param csvFile   CSVファイルパス
     */
    public CSVAccess(String operation, Object data, File csvFile) {
        this.operation = operation;
        this.data = data;
        this.csvFile = csvFile;
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * CSVアクセス処理を実行
     * 指定された操作に基づいてファイル操作を実行
     */
    public void execute() {
        start();
    }

    /**
     * 処理結果を取得
     * 
     * @return 処理結果（読み込み時はList<String>、書き込み時はBoolean）
     */
    public Object getResult() {
        return result;
    }

    @Override
    protected void processOperation() {
        try {
            if ("read".equalsIgnoreCase(operation)) {
                result = read();
            } else if ("write".equalsIgnoreCase(operation)) {
                write((List<String>) data);
                result = Boolean.TRUE;
            } else {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "未知の操作種別: " + operation);
                result = null;
            }
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVアクセス処理中にエラーが発生しました: " + operation, e);
            result = null;
        }
    }

    /**
     * CSVファイルを読み込む
     * 
     * @return 読み込んだ行のリスト
     */
    private List<String> read() {
        List<String> lines = new ArrayList<>();

        lock.readLock().lock();
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "CSVファイル読み込み開始: " + csvFile.getPath());

            if (!csvFile.exists()) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "CSVファイルが存在しません: " + csvFile.getPath());
                return lines;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(csvFile), "UTF_8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "CSVファイル読み込み完了: " + csvFile.getPath() + ", " + lines.size() + "行");

        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVファイルの読み込みに失敗しました: " + csvFile.getPath(), e);
        } finally {
            lock.readLock().unlock();
        }

        return lines;
    }

    /**
     * CSVファイルに書き込む
     * 
     * @param lines 書き込む行のリスト
     */
    private void write(List<String> lines) {
        if (lines == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM, "書き込むデータがnullです");
            return;
        }

        lock.writeLock().lock();
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "CSVファイル書き込み開始: " + csvFile.getPath());

            // 親ディレクトリが存在しない場合は作成
            File parentDir = csvFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(csvFile), "UTF_8"))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "CSVファイル書き込み完了: " + csvFile.getPath() + ", " + lines.size() + "行");

        } catch (IOException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "CSVファイルの書き込みに失敗しました: " + csvFile.getPath(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
