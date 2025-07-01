package util.validator;

import util.StringUtil;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * 社員ID検証用バリデータ
 * ID形式の検証、禁止ID（ID00000）のチェック、重複チェックを実行
 * 
 * @author Nakano
 */
public class IDValidator extends AbstractValidator {

    /** ID形式を検証するための正規表現パターン（1～5桁の数字） */
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{1,5}$");

    /** 禁止ID */
    private static final String FORBIDDEN_ID = "ID00000";

    /** 既存IDのセット（重複チェック用） */
    private final Set<String> existingIds;

    /**
     * コンストラクタ
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     * @param existingIds  既存IDのセット（nullの場合は空セットとして扱う）
     */
    public IDValidator(String fieldName, String errorMessage, Set<String> existingIds) {
        super(fieldName, errorMessage);
        this.existingIds = existingIds != null ? new HashSet<>(existingIds) : new HashSet<>();
    }

    /**
     * IDの前処理を実行
     * 全角数字を半角に変換し、標準形式（ID00000）に変換
     * 
     * @param value 入力値
     * @return 前処理済みのID
     */
    @Override
    public String preprocess(String value) {
        if (value == null) {
            return null;
        }

        // 前後の空白を除去
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        // 全角数字を半角に変換
        String converted = convertFullWidthToHalfWidth(trimmed);

        // ID標準形式への変換
        String standardized = standardizeId(converted);

        return standardized;
    }

    /**
     * IDの検証を実行
     * 
     * @param value 検証対象の値（前処理済み）
     * @return 検証成功の場合true
     */
    @Override
    public boolean validate(String value) {
        // nullチェック
        if (value == null) {
            logWarning("ID検証失敗: null値");
            return false;
        }

        // 空文字チェック
        if (value.trim().isEmpty()) {
            logWarning("ID検証失敗: 空文字");
            return false;
        }

        // ID形式チェック
        if (!checkFormat(value)) {
            logWarning("ID検証失敗: 形式エラー - " + value);
            return false;
        }

        // 禁止IDチェック
        if (isForbiddenId(value)) {
            logWarning("ID検証失敗: 禁止ID - " + value);
            return false;
        }

        // 重複IDチェック
        if (!checkUnique(value)) {
            logWarning("ID検証失敗: 重複ID - " + value);
            return false;
        }

        return true;
    }

    /**
     * ID形式のチェック
     * 
     * @param value チェック対象のID値
     * @return 形式が正しい場合true
     */
    private boolean checkFormat(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        // 標準形式（ID00000）のチェック
        if (!value.matches("^ID\\d{5}$")) {
            return false;
        }

        return true;
    }

    /**
     * ID重複チェック
     * 
     * @param value チェック対象のID値
     * @return 重複がない場合true
     */
    private boolean checkUnique(String value) {
        return !existingIds.contains(value);
    }

    /**
     * 既存IDを追加（動的な重複チェック用）
     * 
     * @param id 追加するID
     */
    public void addExistingId(String id) {
        if (id != null && !id.trim().isEmpty()) {
            existingIds.add(standardizeId(id));
        }
    }

    // ========== 静的ユーティリティメソッド ==========

    /**
     * 全角数字を半角に変換（静的メソッド）
     * 
     * @param value 変換対象の文字列
     * @return 変換後の文字列
     */
    public static String convertFullWidthToHalfWidth(String value) {
        if (value == null) {
            return null;
        }
        return StringUtil.convertFullWidthToHalfWidth(value);
    }

    /**
     * 社員IDを標準形式（ID00000）に変換（静的メソッド）
     * 
     * @param idValue 元の社員ID
     * @return 標準化されたID
     */
    public static String standardizeId(String idValue) {
        if (idValue == null || idValue.trim().isEmpty()) {
            return "";
        }

        try {
            // IDプレフィックス有無の確認と数値部分の抽出
            String numericPart = extractNumericPart(idValue);

            // 数値部分のみかチェック
            if (!ID_PATTERN.matcher(numericPart).matches()) {
                return idValue; // 変換に失敗した場合は元の値を返す
            }

            // 数値部分を左部0埋めして5桁に
            String paddedId = String.format("%05d", Integer.parseInt(numericPart));

            // IDプレフィックスを付加
            return "ID" + paddedId;

        } catch (NumberFormatException _e) {
            return idValue;
        }
    }

    /**
     * 禁止IDのチェック（静的メソッド）
     * 
     * @param value チェック対象のID値
     * @return 禁止IDの場合true
     */
    public static boolean isForbiddenId(String value) {
        return FORBIDDEN_ID.equals(value);
    }

    /**
     * ID値から数字部分を抽出（静的メソッド）
     * 
     * @param value 対象のID値
     * @return 数字部分の文字列
     */
    private static String extractNumericPart(String value) {
        if (value == null) {
            return "";
        }

        if (value.toUpperCase().startsWith("ID")) {
            return value.substring(2);
        }
        return value;
    }

    /**
     * ID形式が正しいかチェック（静的メソッド）
     * 数字のみで5桁以内かを確認
     * 
     * @param idValue チェック対象の値
     * @return 形式が正しい場合true
     */
    public static boolean checkIdFormat(String idValue) {
        if (idValue == null || idValue.isEmpty()) {
            return false;
        }

        // IDプレフィックスを除去して数字部分のみチェック
        String numericPart = extractNumericPart(idValue);
        return ID_PATTERN.matcher(numericPart).matches();
    }
}