package util.validator;

import util.Constants.MessageEnum;

/**
 * 入力データの検証を行うためのインターフェース。
 * <p>
 * このインターフェースは、Strategy パターンに基づいて設計されており、
 * 様々な種類の入力検証ロジックを統一的なインターフェースで提供します。
 * エンジニア管理システム内の各種入力フィールドの検証に使用され、
 * {@link ValidationEnum} と組み合わせて利用されます。
 * </p>
 * 
 * <p>
 * Validator インターフェースを実装するクラスは、以下の責務を持ちます：
 * <ol>
 * <li>特定の規則に基づいて入力値を検証する</li>
 * <li>検証に失敗した場合のエラーメッセージを提供する</li>
 * </ol>
 * </p>
 * 
 * <p>
 * このインターフェースを実装する主な具象クラスには以下のものがあります：
 * <ul>
 * <li>{@link TextValidator} - テキスト入力の長さや形式を検証</li>
 * <li>{@link DateValidator} - 日付入力の有効性や範囲を検証</li>
 * <li>{@link IDValidator} - ID形式と一意性を検証</li>
 * </ul>
 * </p>
 * 
 * <p>
 * 使用例：
 * 
 * <pre>
 * // TextValidator の実装例
 * Validator nameValidator = new TextValidator(
 *         20, // 最大長
 *         "^[\\p{InHiragana}\\p{InKatakana}\\p{InCJKUnifiedIdeographs}]+$", // 正規表現パターン
 *         MessageEnum.VALIDATION_ERROR_NAME.getMessage() // エラーメッセージ
 * );
 * 
 * // 検証実行
 * boolean isValid = nameValidator.validate("山田太郎");
 * 
 * // 検証失敗時の処理
 * if (!isValid) {
 *     String errorMessage = nameValidator.getErrorMessage();
 *     // エラーメッセージの表示など
 * }
 * </pre>
 * </p>
 * 
 * <p>
 * 新しい種類の検証ロジックが必要な場合は、このインターフェースを実装した
 * 新しいクラスを作成することで、既存のコードに変更を加えることなく
 * システムを拡張することができます（Open-Closed Principle）。
 * </p>
 * 
 * @author Nakano
 * @version 4.0.0
 * @since 2025-04-15
 * @see ValidationEnum
 * @see TextValidator
 * @see DateValidator
 * @see IDValidator
 * @see MessageEnum
 */
public interface Validator {

    /**
     * 指定された値が有効かどうかを検証します。
     * <p>
     * このメソッドは、実装クラスで定義された検証ルールに基づいて
     * 入力値の有効性を判断します。検証ルールは実装クラスによって異なり、
     * 文字列の長さ、形式（正規表現）、日付範囲、一意性などの
     * 様々な条件を含むことができます。
     * </p>
     * 
     * <p>
     * 一般的な実装では、null値や空文字列の処理、値の型変換、
     * パターンマッチングなどを適切に行い、検証結果を真偽値で返します。
     * </p>
     * 
     * @param value 検証する値（通常は文字列形式）
     * @return 値が有効な場合はtrue、無効な場合はfalse
     */
    boolean validate(String value);

    /**
     * 検証エラー時に表示するエラーメッセージを取得します。
     * <p>
     * このメソッドは、{@link #validate(String)}メソッドが
     * falseを返した場合に、エラーの理由を説明するメッセージを提供します。
     * エラーメッセージは通常、ユーザーインターフェースに表示されたり、
     * ログに記録されたりします。
     * </p>
     * 
     * <p>
     * メッセージは、{@link MessageEnum}から取得することで
     * システム全体でのメッセージの一貫性を確保することを推奨します。
     * </p>
     * 
     * @return 検証エラーを説明するメッセージ
     */
    String getErrorMessage();
}
