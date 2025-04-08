package util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * テキスト入力を検証するための {@link Validator} インターフェース実装クラス。
 * <p>
 * このクラスは、テキスト入力に対して以下の2種類の検証を実行します。
 * <ol>
 * <li>最大文字数の検証：指定された文字数を超えていないか</li>
 * <li>文字パターンの検証：指定された正規表現パターンに一致するか（オプション）</li>
 * </ol>
 * </p>
 * 
 * <p>
 * 文字パターンの検証は任意であり、コンストラクタでパターンとして null を指定した場合は
 * 文字数のみのチェックが行われます。これにより、同じバリデータークラスで様々な厳密さの
 * 検証を行うことができます。
 * </p>
 * 
 * <p>
 * このクラスは、{@link ValidationEnum} と組み合わせて使用されることを想定しており、
 * エラーメッセージは {@link MessageEnum} から取得することを推奨します。
 * </p>
 * 
 * <p>
 * 使用例：
 * 
 * <pre>
 * // 氏名フィールド（20文字以内、日本語のみ）の検証
 * TextValidator nameValidator = new TextValidator(
 *         20, // 最大長
 *         "^[\\p{InHiragana}\\p{InKatakana}\\p{InCJKUnifiedIdeographs}]+$", // 日本語のみ
 *         MessageEnum.VALIDATION_ERROR_NAME.getMessage() // エラーメッセージ
 * );
 * 
 * boolean isValid = nameValidator.validate("山田太郎");
 * if (!isValid) {
 *     String errorMessage = nameValidator.getErrorMessage();
 *     // エラー処理...
 * }
 * 
 * // 備考フィールド（500文字以内、パターンなし）の検証
 * TextValidator noteValidator = new TextValidator(
 *         500, // 最大長
 *         null, // パターン制限なし
 *         MessageEnum.VALIDATION_ERROR_NOTE.getMessage() // エラーメッセージ
 * );
 * </pre>
 * </p>
 *
 * @author [著者名]
 * @version 1.0
 * @see Validator
 * @see ValidationEnum
 * @see MessageEnum
 */
public class TextValidator implements Validator {

    /**
     * 許容される最大文字数
     */
    private final int maxLength;

    /**
     * 文字パターン検証用の正規表現パターン
     * null の場合は文字パターン検証をスキップ
     */
    private final Pattern pattern;

    /**
     * 検証失敗時のエラーメッセージ
     */
    private final String errorMessage;

    /**
     * TextValidator のコンストラクタ。
     * <p>
     * 最大文字数、正規表現パターン、エラーメッセージを指定して
     * バリデーターを初期化します。パターンとして null を指定した場合は、
     * 文字数のみの検証が行われ、文字パターンの検証はスキップされます。
     * </p>
     * 
     * @param maxLength    許容される最大文字数
     * @param regex        正規表現パターン文字列（文字パターン検証が不要な場合は null）
     * @param errorMessage 検証失敗時のエラーメッセージ
     * @throws IllegalArgumentException maxLength が0以下の場合
     */
    public TextValidator(int maxLength, String regex, String errorMessage) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be greater than 0");
        }

        this.maxLength = maxLength;
        this.pattern = (regex != null) ? Pattern.compile(regex) : null;
        this.errorMessage = errorMessage;
    }

    /**
     * 指定されたテキスト値を検証します。
     * <p>
     * 以下の条件をすべて満たす場合に true を返します。
     * <ol>
     * <li>値が null でない</li>
     * <li>値の長さが maxLength 以下である</li>
     * <li>パターンが指定されている場合、値がそのパターンに一致する</li>
     * </ol>
     * </p>
     * 
     * <p>
     * 空文字列（""）については、パターンが指定されていればそれに従い、
     * パターンが null の場合は常に有効と判断します（長さは0なので maxLength 以下）。
     * 必須項目のチェックは、このクラスの責務ではなく、呼び出し側で別途対応することを想定しています。
     * </p>
     * 
     * @param value 検証するテキスト値
     * @return 値が条件を満たす場合は true、そうでなければ false
     */
    @Override
    public boolean validate(String value) {
        // null 値のチェック
        if (value == null) {
            return false;
        }

        // 長さのチェック
        if (!checkLength(value)) {
            return false;
        }

        // パターンのチェック（パターンがnullでなければ）
        if (pattern != null && !checkPattern(value)) {
            return false;
        }

        return true;
    }

    /**
     * 検証失敗時のエラーメッセージを返します。
     * このメッセージは通常、コンストラクタで指定された {@link MessageEnum} からの
     * エラーメッセージです。
     * 
     * @return エラーメッセージ
     */
    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * テキストの長さが最大文字数以下であるかをチェックします。
     * 
     * @param value チェック対象のテキスト
     * @return 長さが maxLength 以下の場合は true、そうでなければ false
     */
    private boolean checkLength(String value) {
        return value.length() <= this.maxLength;
    }

    /**
     * テキストが指定されたパターンに一致するかをチェックします。
     * 
     * @param value チェック対象のテキスト
     * @return パターンに一致する場合は true、そうでなければ false
     */
    private boolean checkPattern(String value) {
        Matcher matcher = this.pattern.matcher(value);
        return matcher.matches();
    }

    /**
     * このバリデーターの最大文字数を取得します。
     * テスト時やデバッグ時に有用です。
     * 
     * @return 設定されている最大文字数
     */
    public int getMaxLength() {
        return this.maxLength;
    }

    /**
     * このバリデーターの正規表現パターンを文字列として取得します。
     * パターンが設定されていない場合は null を返します。
     * テスト時やデバッグ時に有用です。
     * 
     * @return 正規表現パターン文字列、またはパターンが設定されていない場合は null
     */
    public String getPatternString() {
        return (this.pattern != null) ? this.pattern.pattern() : null;
    }
}
