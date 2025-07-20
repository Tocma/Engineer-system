package view;

import java.awt.Dimension;

import javax.swing.JFrame;

import util.LogHandler;
import util.LogHandler.LogType;
import util.Constants.UIConstants;

/**
 * フレームの基本機能を提供する抽象基底クラス
 * 共通のフレーム初期化処理とサイズ設定機能を実装
 *
 *
 * @author Nakano
 */
public abstract class AbstractFrame {

    /** メインフレーム */
    protected final JFrame frame;

    /**
     * コンストラクタ
     * フレームの基本初期化を実行
     */
    protected AbstractFrame() {
        this.frame = new JFrame();
        this.initialize();
    }

    /**
     * フレームの初期化処理を実行
     * templatemethodパターンを使用
     */
    protected void initialize() {
        try {
            this.initializeFrame();
            this.customizeFrame();
        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "フレームの初期化に失敗", _e);
            throw new RuntimeException("フレームの初期化に失敗", _e);
        }
    }

    /**
     * 基本的なフレーム設定を実行
     */
    protected void initializeFrame() {
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setFrameSize(UIConstants.DEFAULT_WINDOW_WIDTH, UIConstants.DEFAULT_WINDOW_HEIGHT);
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
     * 各画面固有の設定
     */
    protected abstract void customizeFrame();
}