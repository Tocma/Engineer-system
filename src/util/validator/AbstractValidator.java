package util.validator;

import util.LogHandler;
import util.LogHandler.LogType;
import java.util.logging.Level;

/**
 * フィールドバリデータの抽象基底クラス
 * 共通のバリデーション処理とユーティリティメソッドを提供します
 * 
 * @author Nakano
 */
public abstract class AbstractValidator implements FieldValidator {

    /** フィールド名 */
    protected final String fieldName;

    /** エラーメッセージ */
    protected final String errorMessage;

    /**
     * コンストラクタ
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     */
    protected AbstractValidator(String fieldName, String errorMessage) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("フィールド名はnullまたは空にできません");
        }
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("エラーメッセージはnullまたは空にできません");
        }

        this.fieldName = fieldName;
        this.errorMessage = errorMessage;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * デフォルトの前処理実装
     * 前後の空白を除去します
     * 
     * @param value 入力値
     * @return トリムされた値
     */
    @Override
    public String preprocess(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    /**
     * 値がnullまたは空文字かを判定
     * 
     * @param value 検証対象の値
     * @return nullまたは空文字の場合true
     */
    protected boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    /**
     * 文字列の長さをチェック
     * 
     * @param value     検証対象の値
     * @param maxLength 最大文字数
     * @return 最大文字数以下の場合true
     */
    protected boolean checkLength(String value, int maxLength) {
        if (value == null) {
            return true;
        }
        return value.length() <= maxLength;
    }

    /**
     * 正規表現パターンマッチングをチェック
     * 
     * @param value   検証対象の値
     * @param pattern 正規表現パターン
     * @return パターンに一致する場合true
     */
    protected boolean checkPattern(String value, String pattern) {
        if (value == null || pattern == null) {
            return false;
        }
        return value.matches(pattern);
    }

    /**
     * デバッグログを出力
     * 
     * @param message ログメッセージ
     */
    protected void logDebug(String message) {
        LogHandler.getInstance().log(Level.FINE, LogType.SYSTEM,
                String.format("[%s] %s", this.getClass().getSimpleName(), message));
    }

    /**
     * 警告ログを出力
     * 
     * @param message ログメッセージ
     */
    protected void logWarning(String message) {
        LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                String.format("[%s] %s", this.getClass().getSimpleName(), message));
    }
}