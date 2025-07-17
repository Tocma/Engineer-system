package util.validator;

import util.StringUtil;

/**
 * エンジニア歴検証用バリデータ
 * エンジニア歴の数値形式と範囲（0～50年）の検証を実行
 * 
 * @author Nakano
 */
public class CareerValidator extends AbstractValidator {

    /** 最小値 */
    private final int minValue;

    /** 最大値 */
    private final int maxValue;

    /**
     * コンストラクタ
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     * @param minValue     最小値
     * @param maxValue     最大値
     */
    public CareerValidator(String fieldName, String errorMessage,
            int minValue, int maxValue) {
        super(fieldName, errorMessage);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * エンジニア歴の前処理を実行
     * 数値形式の正規化を行います
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
        String noSpaces = StringUtil.removeSpaces(value);
        if (noSpaces.isEmpty()) {
            return "";
        }

        // 全角数字を半角に変換
        String converted = StringUtil.convertFullWidthToHalfWidth(noSpaces);

        // 数値以外の文字を除去
        String normalized = normalizeNumeric(converted);

        return normalized;
    }

    /**
     * 数値形式の正規化
     * 
     * @param value 入力値
     * @return 正規化された数値文字列
     */
    private String normalizeNumeric(String value) {
        // 数字以外を除去
        return value.replaceAll("[^0-9]", "");
    }

    /**
     * エンジニア歴の検証を実行
     * 
     * @param value 検証対象の値（前処理済み）
     * @return 検証成功の場合true
     */
    @Override
    public boolean validate(String value) {
        // nullチェック
        if (value == null) {
            logWarning("エンジニア歴検証失敗: null値");
            return false;
        }

        // 空文字チェック
        if (value.isEmpty()) {
            logWarning("エンジニア歴検証失敗: 空文字");
            return false;
        }

        // 数値形式チェック
        int intValue;
        try {
            intValue = Integer.parseInt(value);
        } catch (NumberFormatException _e) {
            logWarning("エンジニア歴検証失敗: 数値形式エラー - " + value);
            return false;
        }

        // 範囲チェック
        if (!checkRange(intValue)) {
            logWarning("エンジニア歴検証失敗: 範囲外 - " + intValue);
            return false;
        }

        return true;
    }

    /**
     * 範囲チェック
     * 
     * @param value チェック対象の値
     * @return 範囲内の場合true
     */
    private boolean checkRange(int value) {
        return value >= minValue && value <= maxValue;
    }
}