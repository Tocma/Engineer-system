package util.validator;

import util.StringUtil;

/**
 * 氏名検証用バリデータ
 * 氏名の文字数と文字種（日本語のみ）の検証を実行
 * 
 * @author Nakano
 */
public class NameValidator extends AbstractValidator {

    /** 最大文字数 */
    private static final int MAX_LENGTH = 20;

    /** 日本語文字パターン（ひらがな、カタカナ、漢字） */
    private static final String JAPANESE_PATTERN = "^[\\p{InHiragana}\\p{InKatakana}\\p{InCJKUnifiedIdeographs}]+$";

    /**
     * コンストラクタ
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     */
    public NameValidator(String fieldName, String errorMessage) {
        super(fieldName, errorMessage);
    }

    /**
     * 氏名の前処理を実行
     * スペースを除去し、半角カタカナを全角に変換
     * 
     * @param value 入力値
     * @return 前処理済みの氏名
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

        // スペースを除去
        String noSpaces = StringUtil.removeSpaces(trimmed);

        // 半角カタカナを全角に変換
        String converted = StringUtil.convertHalfWidthKatakanaToFullWidth(noSpaces);

        logDebug("氏名前処理: " + value + " -> " + converted);

        return converted;
    }

    /**
     * 氏名の検証を実行
     * 
     * @param value 検証対象の値（前処理済み）
     * @return 検証成功の場合true
     */
    @Override
    public boolean validate(String value) {
        // nullチェック
        if (value == null) {
            logWarning("氏名検証失敗: null値");
            return false;
        }

        // 空文字チェック
        if (value.isEmpty()) {
            logWarning("氏名検証失敗: 空文字");
            return false;
        }

        // 文字数チェック
        if (!checkLength(value, MAX_LENGTH)) {
            logWarning("氏名検証失敗: 文字数超過 - " + value.length() + "文字");
            return false;
        }

        // 文字種チェック（日本語のみ）
        if (!checkPattern(value, JAPANESE_PATTERN)) {
            logWarning("氏名検証失敗: 日本語以外の文字を含む - " + value);
            return false;
        }

        logDebug("氏名検証成功: " + value);
        return true;
    }
}