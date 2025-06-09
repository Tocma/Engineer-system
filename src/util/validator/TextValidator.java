package util.validator;

import util.StringUtil;

/**
 * テキストフィールド検証用バリデータ
 * 経歴、研修の受講歴、備考の文字数検証を実行します
 * 
 * @author Nakano
 */
public class TextValidator extends AbstractValidator {

    /** 最大文字数 */
    private final int maxLength;

    /**
     * コンストラクタ
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     * @param maxLength    最大文字数
     */
    public TextValidator(String fieldName, String errorMessage, int maxLength) {
        super(fieldName, errorMessage);
        if (maxLength <= 0) {
            throw new IllegalArgumentException("最大文字数は正の値である必要があります");
        }
        this.maxLength = maxLength;
    }

    /**
     * テキストの前処理を実行
     * 全角英数字を半角に変換します
     * 
     * @param value 入力値
     * @return 前処理済みの値
     */
    @Override
    public String preprocess(String value) {
        if (value == null) {
            return null;
        }

        // 前後の空白を除去
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        // 全角英数字を半角に変換
        String converted = StringUtil.convertFullWidthAlphanumericToHalfWidth(trimmed);

        logDebug("テキスト前処理: " + value.length() + "文字 -> " + converted.length() + "文字");

        return converted;
    }

    /**
     * テキストの検証を実行
     * 
     * @param value 検証対象の値（前処理済み）
     * @return 検証成功の場合true
     */
    @Override
    public boolean validate(String value) {
        // nullチェック（任意項目のため、nullは許可）
        if (value == null) {
            return true;
        }

        // 空文字は許可（任意項目）
        if (value.isEmpty()) {
            return true;
        }

        // 文字数チェック
        if (!checkLength(value, maxLength)) {
            logWarning("テキスト検証失敗: 文字数超過 - " + value.length() + "文字（最大" + maxLength + "文字）");
            return false;
        }

        logDebug("テキスト検証成功: " + value.length() + "文字");
        return true;
    }

    /**
     * 最大文字数を取得
     * 
     * @return 最大文字数
     */
    public int getMaxLength() {
        return maxLength;
    }
}