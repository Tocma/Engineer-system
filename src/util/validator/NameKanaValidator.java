package util.validator;

import util.StringUtil;

/**
 * フリガナ検証用バリデータ
 * フリガナの文字数と文字種（カタカナのみ）の検証を実行
 * 
 * @author Nakano
 */
public class NameKanaValidator extends AbstractValidator {

    /** 最大文字数 */
    private static final int MAX_LENGTH = 20;

    /** カタカナ文字パターン */
    private static final String KATAKANA_PATTERN = "^[\\p{InKatakana}]+$";

    /**
     * コンストラクタ
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     */
    public NameKanaValidator(String fieldName, String errorMessage) {
        super(fieldName, errorMessage);
    }

    /**
     * フリガナの前処理を実行
     * スペース除去、ひらがな→カタカナ変換、半角カタカナ→全角変換
     * 
     * @param value 入力値
     * @return 前処理済みのフリガナ
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

        // ひらがなをカタカナに変換
        String katakana = StringUtil.convertHiraganaToKatakana(noSpaces);

        // 半角カタカナを全角に変換
        String fullWidth = StringUtil.convertHalfWidthKatakanaToFullWidth(katakana);

        return fullWidth;
    }

    /**
     * フリガナの検証を実行
     * 
     * @param value 検証対象の値（前処理済み）
     * @return 検証成功の場合true
     */
    @Override
    public boolean validate(String value) {
        // nullチェック
        if (value == null) {
            logWarning("フリガナ検証失敗: null値");
            return false;
        }

        // 空文字チェック
        if (value.isEmpty()) {
            logWarning("フリガナ検証失敗: 空文字");
            return false;
        }

        // 文字数チェック
        if (!checkLength(value, MAX_LENGTH)) {
            logWarning("フリガナ検証失敗: 文字数超過 - " + value.length() + "文字");
            return false;
        }

        // 文字種チェック（カタカナのみ）
        if (!checkPattern(value, KATAKANA_PATTERN)) {
            logWarning("フリガナ検証失敗: カタカナ以外の文字を含む - " + value);
            return false;
        }

        return true;
    }
}