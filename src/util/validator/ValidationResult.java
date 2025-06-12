package util.validator;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * バリデーション結果を保持するクラス
 * 検証エラーと前処理済み値を管理
 * 
 * @author Nakano
 */
public class ValidationResult {

    /** エラー情報（フィールド名 -> エラーメッセージ） */
    private final Map<String, String> errors;

    /** 前処理済み値（フィールド名 -> 処理済み値） */
    private final Map<String, String> processedValues;

    /**
     * コンストラクタ
     */
    public ValidationResult() {
        this.errors = new HashMap<>();
        this.processedValues = new HashMap<>();
    }

    /**
     * エラーを追加
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     */
    public void addError(String fieldName, String errorMessage) {
        if (fieldName != null && errorMessage != null) {
            errors.put(fieldName, errorMessage);
        }
    }

    /**
     * 前処理済み値を追加
     * 
     * @param fieldName      フィールド名
     * @param processedValue 前処理済み値
     */
    public void addProcessedValue(String fieldName, String processedValue) {
        if (fieldName != null && processedValue != null) {
            processedValues.put(fieldName, processedValue);
        }
    }

    /**
     * 検証が成功したかを判定
     * 
     * @return エラーがない場合true
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * エラーが存在するかを判定
     * 
     * @return エラーがある場合true
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 特定フィールドのエラーが存在するかを判定
     * 
     * @param fieldName フィールド名
     * @return エラーがある場合true
     */
    public boolean hasError(String fieldName) {
        return errors.containsKey(fieldName);
    }

    /**
     * エラー情報を取得
     * 
     * @return エラー情報のマップ
     */
    public Map<String, String> getErrors() {
        return Collections.unmodifiableMap(errors);
    }

    /**
     * 前処理済み値を取得
     * 
     * @return 前処理済み値のマップ
     */
    public Map<String, String> getProcessedValues() {
        return Collections.unmodifiableMap(processedValues);
    }

    /**
     * 特定フィールドのエラーメッセージを取得
     * 
     * @param fieldName フィールド名
     * @return エラーメッセージ（存在しない場合null）
     */
    public String getError(String fieldName) {
        return errors.get(fieldName);
    }

    /**
     * 特定フィールドの前処理済み値を取得
     * 
     * @param fieldName フィールド名
     * @return 前処理済み値（存在しない場合null）
     */
    public String getProcessedValue(String fieldName) {
        return processedValues.get(fieldName);
    }

    /**
     * エラー数を取得
     * 
     * @return エラー数
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * 結果をクリア
     */
    public void clear() {
        errors.clear();
        processedValues.clear();
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "isValid=" + isValid() +
                ", errorCount=" + getErrorCount() +
                ", errors=" + errors +
                '}';
    }
}