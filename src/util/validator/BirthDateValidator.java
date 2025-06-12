package util.validator;

import java.time.LocalDate;

/**
 * 生年月日検証用バリデータ
 * 生年月日の形式と範囲（1950年～現在）の検証を実行
 * 
 * @author Nakano
 */
public class BirthDateValidator extends DateValidator {

    /**
     * コンストラクタ
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     * @param minDate      最小日付
     * @param maxDate      最大日付
     */
    public BirthDateValidator(String fieldName, String errorMessage,
            LocalDate minDate, LocalDate maxDate) {
        super(fieldName, errorMessage, minDate, maxDate);
    }
}