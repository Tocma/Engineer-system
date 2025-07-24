package util.validator;

import util.StringUtil;

/**
 * テキストフィールド検証用バリデータ（Unicode対応版）
 * 経歴、研修の受講歴、備考の文字数検証を実行
 * サロゲートペア文字、絵文字、環境依存文字を適切に処理
 * 
 * @author Nakano
 */
public class TextValidator extends AbstractValidator {

    /** 最大文字数 */
    private final int maxLength;

    /** 絵文字許可フラグ */
    private final boolean allowEmoji;

    /** 環境依存文字許可フラグ */
    private final boolean allowEnvironmentDependentChars;

    /**
     * コンストラクタ（既存互換用）
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     * @param maxLength    最大文字数
     */
    public TextValidator(String fieldName, String errorMessage, int maxLength) {
        this(fieldName, errorMessage, maxLength, true, false);
    }

    /**
     * コンストラクタ（拡張版）
     * 
     * @param fieldName                      フィールド名
     * @param errorMessage                   エラーメッセージ
     * @param maxLength                      最大文字数
     * @param allowEmoji                     絵文字許可フラグ
     * @param allowEnvironmentDependentChars 環境依存文字許可フラグ
     */
    public TextValidator(String fieldName, String errorMessage, int maxLength,
            boolean allowEmoji, boolean allowEnvironmentDependentChars) {
        super(fieldName, errorMessage);
        if (maxLength <= 0) {
            throw new IllegalArgumentException("最大文字数は正の値である必要があります");
        }
        this.maxLength = maxLength;
        this.allowEmoji = allowEmoji;
        this.allowEnvironmentDependentChars = allowEnvironmentDependentChars;
    }

    /**
     * テキストの前処理を実行
     * 全角英数字を半角に変換し、Unicode正規化を行う
     * 
     * @param value 入力値
     * @return 前処理済みの値
     */
    @Override
    public String preprocess(String value) {
        if (value == null) {
            return null;
        }

        // 全ての半角・全角スペースを除去
        String noSpaces = StringUtil.removeSpaces(value).replace(",", "");
        if (noSpaces.isEmpty()) {
            return "";
        }

        // Unicode正規化とサニタイズ
        String normalized = StringUtil.normalizeAndSanitize(noSpaces);

        // 全角英数字を半角に変換
        String converted = StringUtil.convertFullWidthAlphanumericToHalfWidth(normalized);

        // 絵文字除去（許可されていない場合）
        if (!allowEmoji) {
            converted = StringUtil.removeEmoji(converted);
        }

        // 環境依存文字除去（許可されていない場合）
        if (!allowEnvironmentDependentChars) {
            converted = StringUtil.removeEnvironmentDependentChars(converted);
        }

        return converted;
    }

    /**
     * テキストの検証を実行
     * サロゲートペア文字を考慮した文字数チェック
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

        // 文字列の安全性チェック
        if (!StringUtil.isSafeString(value)) {
            logWarning("テキスト検証失敗: 不正な文字が含まれています");
            return false;
        }

        // サロゲートペア対応文字数チェック
        int actualLength = StringUtil.countCodePoints(value);
        if (actualLength > maxLength) {
            logWarning("テキスト検証失敗: 文字数超過 - " + actualLength + "文字（最大" + maxLength + "文字）");
            return false;
        }

        // 絵文字チェック（許可されていない場合）
        if (!allowEmoji && !value.equals(StringUtil.removeEmoji(value))) {
            logWarning("テキスト検証失敗: 絵文字が含まれています");
            return false;
        }

        // 環境依存文字チェック（許可されていない場合）
        if (!allowEnvironmentDependentChars && !value.equals(StringUtil.removeEnvironmentDependentChars(value))) {
            logWarning("テキスト検証失敗: 環境依存文字が含まれています");
            return false;
        }

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

    /**
     * 絵文字許可フラグを取得
     * 
     * @return 絵文字許可フラグ
     */
    public boolean isAllowEmoji() {
        return allowEmoji;
    }

    /**
     * 環境依存文字許可フラグを取得
     * 
     * @return 環境依存文字許可フラグ
     */
    public boolean isAllowEnvironmentDependentChars() {
        return allowEnvironmentDependentChars;
    }
}