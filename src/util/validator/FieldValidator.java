package util.validator;

/**
 * フィールドバリデータのインターフェース
 * 各フィールドの検証処理を定義します
 * 
 * @author Nakano
 */
public interface FieldValidator {

    /**
     * 入力値の前処理を実行
     * 全角→半角変換、スペース除去などの正規化処理を行います
     * 
     * @param value 入力値
     * @return 前処理済みの値
     */
    String preprocess(String value);

    /**
     * 値の検証を実行
     * 
     * @param value 検証対象の値（前処理済み）
     * @return 検証成功の場合true
     */
    boolean validate(String value);

    /**
     * フィールド名を取得
     * 
     * @return フィールド名
     */
    String getFieldName();

    /**
     * エラーメッセージを取得
     * 
     * @return エラーメッセージ
     */
    String getErrorMessage();
}