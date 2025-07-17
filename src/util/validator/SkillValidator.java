package util.validator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import util.StringUtil;

/**
 * スキル評価検証用バリデータ
 * 1.0～5.0の0.5刻みの評価値の検証を実行
 * 技術力、受講態度、コミュニケーション能力、リーダーシップで共通利用されます
 * * @author Nakano
 */
public class SkillValidator extends AbstractValidator {

    /** 有効な評価値の文字列表現のセット */
    private static final Set<String> VALID_RATINGS = new HashSet<>(Arrays.asList(
            "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0", "4.5", "5.0"));

    /**
     * コンストラクタ
     * * @param fieldName フィールド名
     * 
     * @param errorMessage エラーメッセージ
     */
    public SkillValidator(String fieldName, String errorMessage) {
        super(fieldName, errorMessage);
    }

    /**
     * スキル評価値の前処理を実行
     * 全角数字を半角に変換し、スペースを除去します。
     * * @param value 入力値
     * 
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

        // 全角数字や小数点を半角に変換
        return StringUtil.convertFullWidthToHalfWidth(noSpaces);
    }

    /**
     * スキル評価値の検証を実行
     * * @param value 検証対象の値（前処理済み）
     * 
     * @return 検証成功の場合true
     */
    @Override
    public boolean validate(String value) {
        // nullまたは空文字は任意項目のため許可
        if (value == null || value.isEmpty()) {
            return true;
        }

        // 有効な評価値リストに完全一致するかをチェック
        if (!VALID_RATINGS.contains(value)) {
            logWarning("スキル評価検証失敗: 無効な値です - " + value);
            return false;
        }

        return true;
    }
}