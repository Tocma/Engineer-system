package util.validator;

import util.Constants.MessageEnum;

/**
 * 入力データの検証を行うためのインターフェース。
 * 
 * 
 * @author Nakano
 */
public interface Validator {

    /**
     * 指定された値が有効かどうかを検証
     * 
     * @param value 検証する値（通常は文字列形式）
     * @return 値が有効な場合はtrue、無効な場合はfalse
     */
    boolean validate(String value);

    /**
     * 検証エラー時に表示するエラーメッセージを取得
     * 
     * このメソッドは、{@link #validate(String)}メソッドが
     * falseを返した場合に、エラーの理由を説明するメッセージを提供
     * エラーメッセージは通常、ユーザーインターフェースに表示されたり、
     * ログに記録されたりします。
     * 
     * 
     * 
     * メッセージは、{@link MessageEnum}から取得することで
     * システム全体でのメッセージの一貫性を確保することを推奨
     * 
     * 
     * @return 検証エラーを説明するメッセージ
     */
    String getErrorMessage();
}
