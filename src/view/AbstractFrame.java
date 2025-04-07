package view;

import javax.swing.*;
import java.awt.*;
import util.LogHandler;
import util.LogHandler.LogType;

/**
 * フレームの基本機能を提供する抽象基底クラス
 * 共通のフレーム初期化処理とサイズ設定機能を実装
 *
 * <p>
 * 主な機能：
 * <ul>
 * <li>基本フレームの初期化</li>
 * <li>フレームサイズの設定</li>
 * <li>共通のフレームプロパティ設定</li>
 * </ul>
 * </p>
 *
 * @author Nakano
 * @version 3.1.0
 * @since 2025-04-04
 */
public abstract class AbstractFrame {

    /** メインフレーム */
    protected final JFrame frame;

    /** デフォルトのフレームサイズ */
    protected static final int DEFAULT_WIDTH = 1000;
    protected static final int DEFAULT_HEIGHT = 800;

    /**
     * コンストラクタ
     * フレームの基本初期化を実行
     */
    protected AbstractFrame() {
        this.frame = new JFrame();
        initialize();
    }

    /**
     * フレームの初期化処理を実行
     * templatemethodドパターンを使用
     */
    protected void initialize() {
        try {
            initializeFrame();
            customizeFrame();
        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "フレームの初期化に失敗しました", e);
            throw new RuntimeException("フレームの初期化に失敗しました", e);
        }
    }

    /**
     * 基本的なフレーム設定を実行
     */
    protected void initializeFrame() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setFrameSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        frame.setLocationRelativeTo(null);
    }

    /**
     * フレームサイズを設定
     *
     * @param width  フレームの幅
     * @param height フレームの高さ
     */
    protected void setFrameSize(int width, int height) {
        frame.setSize(width, height);
        frame.setMinimumSize(new Dimension(width, height));
    }

    /**
     * フレームの表示状態を設定
     *
     * @param visible 表示する場合はtrue
     */
    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    /**
     * フレームインスタンスを取得
     *
     * @return JFrameインスタンス
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * サブクラスで実装する必要のあるフレームカスタマイズメソッド
     * 各画面固有の設定を行う
     */
    protected abstract void customizeFrame();
}