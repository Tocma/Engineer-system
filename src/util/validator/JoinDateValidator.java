package util.validator;

import java.time.LocalDate;

import util.StringUtil;

/**
 * 入社年月検証用バリデータ
 * 入社年月の形式と範囲（1950年～現在）の検証を実行
 * 
 * @author Nakano
 */
public class JoinDateValidator extends DateValidator {

    /**
     * コンストラクタ
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     * @param minDate      最小日付
     * @param maxDate      最大日付
     */
    public JoinDateValidator(String fieldName, String errorMessage,
            LocalDate minDate, LocalDate maxDate) {
        super(fieldName, errorMessage, minDate, maxDate);
    }

    /**
     * 入社年月の前処理を実行
     * 年月のみの場合は日を01に補完
     * 
     * @param value 入力値
     * @return 前処理済みの日付文字列
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

        // 年月形式（YYYY-MM）の場合は日を補完
        String withDay = noSpaces;
        if (withDay.matches("\\d{4}-\\d{2}")) {
            withDay = withDay + "-01";
        }

        // 親クラスの正規化処理を実行
        return super.normalizeDateFormat(withDay);
    }
}