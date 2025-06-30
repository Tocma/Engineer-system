package model;

import util.LogHandler;
import util.LogHandler.LogType;
import java.util.logging.Level;

/**
 * 非同期データアクセスの基底クラス
 * バックグラウンドでのデータアクセス処理を抽象化
 *
 * このクラスは、データアクセス処理を非同期的に実行するための基底クラス
 * スレッド制御と処理状態の管理を担当し、サブクラスで具体的な処理を実装
 *
 * @author Nakano
 */
public abstract class AccessThread implements Runnable {

    /** スレッド実行状態フラグ */
    protected volatile boolean running;

    /** 作業スレッド */
    protected Thread thread;

    /**
     * コンストラクタ
     */
    protected AccessThread() {
        this.running = false;
    }

    /**
     * スレッド処理の実行
     * Template Methodパターンによりサブクラスで具体的な処理を実装
     */
    @Override
    public void run() {
        try {
            // 処理の実行
            processOperation();
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "スレッド処理中にエラーが発生", _e);
        } finally {
            running = false;
        }
    }

    /**
     * スレッドの開始
     */
    public void start() {
        if (!running) {
            running = true;
            thread = new Thread(this);
            thread.start();
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "アクセススレッドを開始");
        }
    }

    /**
     * スレッドの停止
     */
    public void stop() {
        running = false;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM, "アクセススレッドを停止");
        }
    }

    /**
     * 処理の実行
     * サブクラスで実装する必要あり
     */
    protected abstract void processOperation();
}
