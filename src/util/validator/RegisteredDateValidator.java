package util.validator;

import java.time.LocalDate;

/**
 * 登録日検証用バリデータ
 * 登録日の形式の検証を実行（CSV読み込み時に使用）
 * 
 * @author Nakano
 */
public class RegisteredDateValidator extends DateValidator {

    /**
     * コンストラクタ
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     */
    public RegisteredDateValidator(String fieldName, String errorMessage) {
        // 登録日は過去から現在までの日付を許可
        super(fieldName, errorMessage, LocalDate.of(1900, 1, 1), LocalDate.now());
    }
}