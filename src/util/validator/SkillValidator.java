package util.validator;

import util.StringUtil;

/**
 * スキル評価検証用バリデータ
 * 1.0～5.0の0.5刻みの評価値の検証を実行
 * 技術力、受講態度、コミュニケーション能力、リーダーシップで共通利用されます
 * 
 * @author Nakano
 */
public class SkillValidator extends AbstractValidator {

    /** 最小値 */
    private static final double MIN_VALUE = 1.0;

    /** 最大値 */
    private static final double MAX_VALUE = 5.0;

    /** 刻み値 */
    private static final double STEP_VALUE = 0.5;

    /**
     * コンストラクタ
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     */
    public SkillValidator(String fieldName, String errorMessage) {
        super(fieldName, errorMessage);
    }

    /**
     * スキル評価値の前処理を実行
     * 数値形式の正規化（0.5刻み）を行います
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

        // 全角数字を半角に変換
        String converted = StringUtil.convertFullWidthToHalfWidth(trimmed);

        // 数値形式の正規化
        String normalized = normalizeRating(converted);

        logDebug("スキル評価前処理: " + value + " -> " + normalized);

        return normalized;
    }

    /**
     * 評価値の正規化
     * 
     * @param value 入力値
     * @return 正規化された評価値文字列
     */
    private String normalizeRating(String value) {
        try {
            double doubleValue = Double.parseDouble(value);
            // 0.5刻みに丸める
            double rounded = Math.round(doubleValue * 2) / 2.0;

            // 1.0 -> "1.0", 1.5 -> "1.5" の形式に統一
            if (rounded == Math.floor(rounded)) {
                return String.format("%.1f", rounded);
            } else {
                return String.valueOf(rounded);
            }
        } catch (NumberFormatException e) {
            // 数値でない場合はそのまま返す
            return value;
        }
    }

    /**
     * スキル評価値の検証を実行
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

        // 数値形式チェック
        double doubleValue;
        try {
            doubleValue = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logWarning("スキル評価検証失敗: 数値形式エラー - " + value);
            return false;
        }

        // 範囲チェック
        if (!checkRange(doubleValue)) {
            logWarning("スキル評価検証失敗: 範囲外 - " + doubleValue);
            return false;
        }

        // 刻み値チェック
        if (!checkStepValue(doubleValue)) {
            logWarning("スキル評価検証失敗: 0.5刻みでない - " + doubleValue);
            return false;
        }

        logDebug("スキル評価検証成功: " + value);
        return true;
    }

    /**
     * 範囲チェック
     * 
     * @param value チェック対象の値
     * @return 範囲内の場合true
     */
    private boolean checkRange(double value) {
        return value >= MIN_VALUE && value <= MAX_VALUE;
    }

    /**
     * 刻み値チェック
     * 
     * @param value チェック対象の値
     * @return 0.5刻みの場合true
     */
    private boolean checkStepValue(double value) {
        // 0.5で割った余りが0になるかチェック
        double remainder = value % STEP_VALUE;
        return Math.abs(remainder) < 0.001 || Math.abs(remainder - STEP_VALUE) < 0.001;
    }
}