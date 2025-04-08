package util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日付入力を検証するための {@link Validator} インターフェース実装クラス。
 * <p>
 * このクラスは、文字列として入力された日付情報に対して以下の検証を行います：
 * <ol>
 * <li>指定された形式（フォーマット）に従って日付に変換可能かどうか</li>
 * <li>指定された期間（最小日付から最大日付まで）の範囲内にあるかどうか</li>
 * </ol>
 * </p>
 * 
 * <p>
 * 日付フォーマットには、{@link SimpleDateFormat} で定義されるパターン文字列を
 * 使用します。例えば、「yyyy/MM/dd」（年/月/日）や「yyyy/MM」（年/月）などが
 * 一般的に使用されます。
 * </p>
 * 
 * <p>
 * 最小日付または最大日付として null を指定した場合、その方向への制限は適用されません。
 * つまり、最小日付に null を指定すると過去のどの日付も有効と判断され、
 * 最大日付に null を指定すると未来のどの日付も有効と判断されます。
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
 * // 生年月日フィールド（1950年1月1日から現在まで）の検証
 * Calendar cal = Calendar.getInstance();
 * cal.set(1950, 0, 1, 0, 0, 0);
 * Date minDate = cal.getTime();
 * Date maxDate = new Date(); // 現在日時
 * 
 * DateValidator birthDateValidator = new DateValidator(
 *         minDate, // 最小日付：1950/01/01
 *         maxDate, // 最大日付：現在
 *         "yyyy/MM/dd", // 日付フォーマット
 *         MessageEnum.VALIDATION_ERROR_BIRTH_DATE.getMessage() // エラーメッセージ
 * );
 * 
 * boolean isValid = birthDateValidator.validate("2000/01/01");
 * if (!isValid) {
 *     String errorMessage = birthDateValidator.getErrorMessage();
 *     // エラー処理...
 * }
 * 
 * // 入社年月フィールド（年月のみ）の検証
 * DateValidator joinDateValidator = new DateValidator(
 *         minDate, // 最小日付：1950/01/01
 *         maxDate, // 最大日付：現在
 *         "yyyy/MM", // 年月のフォーマット
 *         MessageEnum.VALIDATION_ERROR_JOIN_DATE.getMessage() // エラーメッセージ
 * );
 * </pre>
 * </p>
 *
 * @author [著者名]
 * @version 1.0
 * @see Validator
 * @see ValidationEnum
 * @see MessageEnum
 * @see SimpleDateFormat
 * @see Date
 */
public class DateValidator implements Validator {

    /**
     * 許容される最小日付（この日付以降が有効）
     */
    private final Date minDate;

    /**
     * 許容される最大日付（この日付以前が有効）
     */
    private final Date maxDate;

    /**
     * 日付フォーマット（SimpleDateFormatで使用するパターン文字列）
     */
    private final SimpleDateFormat dateFormat;

    /**
     * 検証失敗時のエラーメッセージ
     */
    private final String errorMessage;

    /**
     * DateValidator のコンストラクタ。
     * <p>
     * 最小日付、最大日付、日付フォーマット、エラーメッセージを指定して
     * バリデーターを初期化します。最小日付または最大日付に null を指定すると、
     * その方向の制限は適用されません。
     * </p>
     * 
     * @param minDate      許容される最小日付（null可）
     * @param maxDate      許容される最大日付（null可）
     * @param format       日付フォーマットパターン（SimpleDateFormatで使用する形式）
     * @param errorMessage 検証失敗時のエラーメッセージ
     * @throws IllegalArgumentException formatがnullまたは不正なフォーマットパターンの場合
     */
    public DateValidator(Date minDate, Date maxDate, String format, String errorMessage) {
        if (format == null) {
            throw new IllegalArgumentException("日付フォーマットにnullは指定できません");
        }

        this.minDate = minDate;
        this.maxDate = maxDate;
        this.dateFormat = new SimpleDateFormat(format);
        this.dateFormat.setLenient(false); // 厳密な日付解釈を設定（例：2月30日などの無効な日付をエラーとする）
        this.errorMessage = errorMessage;
    }

    /**
     * 指定された日付文字列を検証します。
     * <p>
     * 以下の条件をすべて満たす場合に true を返します。
     * <ol>
     * <li>値が null でなく、空文字列でもない</li>
     * <li>値が指定されたフォーマットに従って有効な日付に変換できる</li>
     * <li>変換された日付が、最小日付以降である（最小日付が null の場合は制限なし）</li>
     * <li>変換された日付が、最大日付以前である（最大日付が null の場合は制限なし）</li>
     * </ol>
     * </p>
     * 
     * @param value 検証する日付文字列
     * @return 値が条件を満たす場合は true、そうでなければ false
     */
    @Override
    public boolean validate(String value) {
        // null または空文字列のチェック
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        try {
            // 文字列を日付に変換
            Date date = parseDate(value);

            // 最小日付のチェック
            if (minDate != null && date.before(minDate)) {
                return false;
            }

            // 最大日付のチェック
            if (maxDate != null && date.after(maxDate)) {
                return false;
            }

            return true;
        } catch (ParseException e) {
            // 日付変換に失敗した場合
            return false;
        }
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
     * 文字列を日付に変換します。
     * 
     * @param value 変換対象の文字列
     * @return 変換された日付オブジェクト
     * @throws ParseException 文字列が指定されたフォーマットに従った有効な日付でない場合
     */
    private Date parseDate(String value) throws ParseException {
        // SimpleDateFormat は同期化されていないため、このメソッド内でのみ使用する
        return this.dateFormat.parse(value);
    }

    /**
     * このバリデーターの最小日付を取得します。
     * テスト時やデバッグ時に有用です。
     * 
     * @return 設定されている最小日付、または null（制限なしの場合）
     */
    public Date getMinDate() {
        return this.minDate;
    }

    /**
     * このバリデーターの最大日付を取得します。
     * テスト時やデバッグ時に有用です。
     * 
     * @return 設定されている最大日付、または null（制限なしの場合）
     */
    public Date getMaxDate() {
        return this.maxDate;
    }

    /**
     * このバリデーターの日付フォーマットパターンを取得します。
     * テスト時やデバッグ時に有用です。
     * 
     * @return 日付フォーマットパターン
     */
    public String getDateFormatPattern() {
        return this.dateFormat.toPattern();
    }
}
