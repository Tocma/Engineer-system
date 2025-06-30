package util.validator;

import util.LogHandler;
import util.LogHandler.LogType;
import java.util.Map;
import java.util.logging.Level;

/**
 * バリデーション実行を管理するサービスクラス
 * フォーム全体のバリデーション処理を統括
 * 
 * @author Nakano
 */
public class ValidationService {

    /** シングルトンインスタンス */
    private static final ValidationService INSTANCE = new ValidationService();

    /**
     * プライベートコンストラクタ
     */
    private ValidationService() {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "ValidationServiceを初期化完了");
    }

    /**
     * シングルトンインスタンスを取得
     * 
     * @return ValidationServiceのインスタンス
     */
    public static ValidationService getInstance() {
        return INSTANCE;
    }

    /**
     * フォーム全体のバリデーションを実行
     * 
     * @param formData   フォームデータ（フィールド名 -> 値）
     * @param validators バリデータマップ（フィールド名 -> バリデータ）
     * @return バリデーション結果
     */
    public ValidationResult validateForm(Map<String, String> formData,
            Map<String, FieldValidator> validators) {
        ValidationResult result = new ValidationResult();

        if (formData == null || validators == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "バリデーション実行時の引数がnullです");
            return result;
        }

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "フォームバリデーション開始: フィールド数=" + formData.size());

        // 各フィールドのバリデーションを実行
        for (Map.Entry<String, FieldValidator> entry : validators.entrySet()) {
            String fieldName = entry.getKey();
            FieldValidator validator = entry.getValue();

            // フォームデータから値を取得
            String rawValue = formData.get(fieldName);

            try {
                // バリデータが存在する場合のみ実行
                if (validator != null) {
                    validateField(fieldName, rawValue, validator, result);
                }
            } catch (Exception _e) {
                LogHandler.getInstance().logError(LogType.SYSTEM,
                        "フィールドバリデーション中にエラーが発生: " + fieldName, _e);
                result.addError(fieldName, "検証中にエラーが発生");
            }
        }

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                String.format("フォームバリデーション完了: 有効=%s, エラー数=%d",
                        result.isValid(), result.getErrorCount()));

        return result;
    }

    /**
     * 単一フィールドのバリデーションを実行
     * 
     * @param fieldName フィールド名
     * @param rawValue  入力値
     * @param validator バリデータ
     * @param result    結果格納先
     */
    private void validateField(String fieldName, String rawValue,
            FieldValidator validator, ValidationResult result) {
        // 前処理を実行
        String processedValue = validator.preprocess(rawValue);

        // バリデーションを実行
        boolean isValid = validator.validate(processedValue);

        if (isValid) {
            // 検証成功時は前処理済み値を保存
            result.addProcessedValue(fieldName, processedValue);
            LogHandler.getInstance().log(Level.FINE, LogType.SYSTEM,
                    String.format("フィールド検証成功: %s", fieldName));
        } else {
            // 検証失敗時はエラーメッセージを保存
            result.addError(fieldName, validator.getErrorMessage());
            LogHandler.getInstance().log(Level.FINE, LogType.SYSTEM,
                    String.format("フィールド検証失敗: %s - %s",
                            fieldName, validator.getErrorMessage()));
        }
    }

    /**
     * 単一フィールドのバリデーションを実行
     * 
     * @param value     検証対象の値
     * @param validator バリデータ
     * @return バリデーション結果
     */
    public ValidationResult validateSingleField(String value, FieldValidator validator) {
        ValidationResult result = new ValidationResult();

        if (validator == null) {
            return result;
        }

        validateField(validator.getFieldName(), value, validator, result);
        return result;
    }
}