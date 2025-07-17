package util.validator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import util.StringUtil;

/**
 * 日付検証用の基底バリデータ
 * 生年月日と入社年月の検証で共通利用される日付検証を実行
 * 
 * @author Nakano
 */
public abstract class DateValidator extends AbstractValidator {

    /** 日付フォーマット */
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 最小日付 */
    protected final LocalDate minDate;

    /** 最大日付 */
    protected final LocalDate maxDate;

    /**
     * コンストラクタ
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     * @param minDate      最小日付
     * @param maxDate      最大日付
     */
    protected DateValidator(String fieldName, String errorMessage,
            LocalDate minDate, LocalDate maxDate) {
        super(fieldName, errorMessage);
        this.minDate = minDate;
        this.maxDate = maxDate;
    }

    /**
     * 日付の前処理を実行
     * 日付文字列を標準フォーマット（YYYY-MM-DD）に正規化
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

        // 日付フォーマットの正規化を試行
        String normalized = normalizeDateFormat(noSpaces);

        return normalized;
    }

    /**
     * 日付フォーマットの正規化
     * 
     * @param value 日付文字列
     * @return 正規化された日付文字列
     */
    protected String normalizeDateFormat(String value) {
        // 既に正しいフォーマットの場合はそのまま返す
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return value;
        }

        // スラッシュ区切りをハイフン区切りに変換
        if (value.matches("\\d{4}/\\d{2}/\\d{2}")) {
            return value.replace('/', '-');
        }

        // その他のフォーマットは変換せずに返す
        return value;
    }

    /**
     * 日付の検証を実行
     * 
     * @param value 検証対象の値（前処理済み）
     * @return 検証成功の場合true
     */
    @Override
    public boolean validate(String value) {
        // nullチェック
        if (value == null) {
            logWarning("日付検証失敗: null値");
            return false;
        }

        // 空文字チェック
        if (value.isEmpty()) {
            logWarning("日付検証失敗: 空文字");
            return false;
        }

        // 日付形式チェックと変換
        LocalDate date;
        try {
            date = parseDate(value);
        } catch (DateTimeParseException _e) {
            logWarning("日付検証失敗: 無効な日付形式 - " + value);
            return false;
        }

        // 範囲チェック
        if (!checkDateRange(date)) {
            logWarning("日付検証失敗: 範囲外 - " + value);
            return false;
        }

        // 存在する日付かチェック
        if (!checkValidDate(date)) {
            logWarning("日付検証失敗: 存在しない日付 - " + value);
            return false;
        }

        return true;
    }

    /**
     * 日付文字列を解析
     * 
     * @param value 日付文字列
     * @return 解析されたLocalDate
     * @throws DateTimeParseException 解析失敗時
     */
    protected LocalDate parseDate(String value) throws DateTimeParseException {
        return LocalDate.parse(value, DATE_FORMATTER);
    }

    /**
     * 日付範囲チェック
     * 
     * @param date チェック対象の日付
     * @return 範囲内の場合true
     */
    protected boolean checkDateRange(LocalDate date) {
        if (minDate != null && date.isBefore(minDate)) {
            return false;
        }
        if (maxDate != null && date.isAfter(maxDate)) {
            return false;
        }
        return true;
    }

    /**
     * 存在する日付かチェック
     * 
     * @param date チェック対象の日付
     * @return 存在する日付の場合true
     */
    protected boolean checkValidDate(LocalDate date) {
        // LocalDateは存在しない日付を生成できないため、常にtrue
        return true;
    }
}