package util;

import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * 社員IDを検証するための {@link Validator} インターフェース実装クラス。
 * <p>
 * このクラスは、エンジニア管理システムにおける社員IDの検証を担当し、
 * 以下の2種類の検証を実行します：
 * <ol>
 * <li>ID形式の検証：規定のフォーマットに従っているか（数字5桁以内であるか）</li>
 * <li>ID重複の検証：既に登録されているIDと重複していないか</li>
 * </ol>
 * </p>
 * 
 * <p>
 * 社員IDは内部的に「ID」接頭辞と5桁の数字（先頭を0埋めした形式）の組み合わせとして
 * 標準化されます。例えば、入力値が「12345」の場合は「ID12345」に、「123」の場合は
 * 「ID00123」に標準化されます。また、入力値が既に「ID」接頭辞を持っている場合は、
 * 数字部分のみが抽出され、5桁に正規化されます。
 * </p>
 * 
 * <p>
 * 重複チェックは、標準化されたID形式を使用して行われます。これにより、
 * 「ID00123」と「123」、「00123」などの実質的に同じIDが重複して
 * 登録されることを防ぎます。
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
 * // 既存の社員IDリスト
 * Set&lt;String&gt; existingIds = new HashSet&lt;&gt;();
 * existingIds.add("ID00001");
 * existingIds.add("ID00002");
 * existingIds.add("ID00003");
 * 
 * // 社員IDバリデーターの初期化
 * IDValidator idValidator = new IDValidator(
 *         existingIds, // 既存ID一覧
 *         MessageEnum.VALIDATION_ERROR_EMPLOYEE_ID.getMessage() // エラーメッセージ
 * );
 * 
 * // 形式チェックと重複チェック
 * boolean isValid1 = idValidator.validate("12345"); // true (新規ID)
 * boolean isValid2 = idValidator.validate("00001"); // false (ID00001と重複)
 * boolean isValid3 = idValidator.validate("ID00003"); // false (ID00003と重複)
 * boolean isValid4 = idValidator.validate("123ABC"); // false (数字以外を含む)
 * 
 * if (!isValid2) {
 *     String errorMessage = idValidator.getErrorMessage();
 *     // エラー処理...
 * }
 * </pre>
 * </p>
 *
 * @author [著者名]
 * @version 1.0
 * @see Validator
 * @see ValidationEnum
 * @see MessageEnum
 */
public class IDValidator implements Validator {

    /**
     * ID形式を検証するための正規表現パターン。
     * 1〜5桁の数字を表します。
     */
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{1,5}$");

    /**
     * 既に使用されているIDのセット。
     * 重複チェックに使用されます。
     */
    private final Set<String> usedIds;

    /**
     * 検証失敗時のエラーメッセージ。
     */
    private final String errorMessage;

    /**
     * IDValidator のコンストラクタ。
     * <p>
     * 既に使用されているIDのセットとエラーメッセージを指定して
     * バリデーターを初期化します。
     * </p>
     * 
     * @param usedIds      既に使用されているIDのセット（標準形式「ID00000」のもの）
     * @param errorMessage 検証失敗時のエラーメッセージ
     */
    public IDValidator(Set<String> usedIds, String errorMessage) {
        this.usedIds = usedIds != null ? usedIds : new HashSet<>();
        this.errorMessage = errorMessage;
    }

    /**
     * 指定されたID値を検証します。
     * <p>
     * 以下の条件をすべて満たす場合に true を返します：
     * <ol>
     * <li>値が null でなく、空文字列でもない</li>
     * <li>値が規定の形式に従っている（「ID」接頭辞の有無にかかわらず、数字部分が1〜5桁の数字のみで構成されている）</li>
     * <li>値（標準形式に変換後）が既存のIDと重複していない</li>
     * </ol>
     * </p>
     * 
     * @param value 検証するID値
     * @return 値が条件を満たす場合は true、そうでなければ false
     */
    @Override
    public boolean validate(String value) {
        // null または空文字列のチェック
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        // ID形式のチェック
        if (!checkFormat(value)) {
            return false;
        }

        // ID00000の登録を禁止する
        String standardizedId = standardizeId(value);
        if ("ID00000".equals(standardizedId)) {
            return false;
        }

        // 重複チェック
        if (!checkUnique(value)) {
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
     * ID値が規定の形式に従っているかをチェックします。
     * <p>
     * 「ID」接頭辞の有無にかかわらず、数字部分が1〜5桁の数字のみで
     * 構成されていることを確認します。
     * </p>
     * 
     * @param value チェック対象のID値
     * @return 形式が正しい場合は true、そうでなければ false
     */
    private boolean checkFormat(String value) {
        // 「ID」接頭辞がある場合は取り除く
        String numericPart = extractNumericPart(value);

        // 数字部分が1〜5桁の数字のみで構成されているかチェック
        return ID_PATTERN.matcher(numericPart).matches();
    }

    /**
     * ID値が既存のIDと重複していないかをチェックします。
     * <p>
     * 入力値を標準形式（「ID」接頭辞 + 5桁の0埋め数字）に変換した上で、
     * 既存IDリストとの重複をチェックします。
     * </p>
     * 
     * @param value チェック対象のID値
     * @return 重複がない場合は true、重複がある場合は false
     */
    private boolean checkUnique(String value) {
        // 標準形式に変換
        String standardizedId = standardizeId(value);

        // 既存IDリストとの重複チェック
        return !usedIds.contains(standardizedId);
    }

    /**
     * ID値から数字部分を抽出します。
     * <p>
     * 「ID」接頭辞がある場合は除去し、数字部分のみを返します。
     * 接頭辞がない場合は入力値をそのまま返します。
     * </p>
     * 
     * @param value 対象のID値
     * @return 数字部分の文字列
     */
    private String extractNumericPart(String value) {
        if (value.toUpperCase().startsWith("ID")) {
            return value.substring(2);
        }
        return value;
    }

    /**
     * ID値を標準形式に変換します。
     * <p>
     * 標準形式は「ID」接頭辞 + 5桁の0埋め数字 です。
     * 例えば、「123」は「ID00123」に、「12345」は「ID12345」に変換されます。
     * 既に「ID」接頭辞がある場合でも、数字部分のみが抽出され、
     * 5桁に正規化されます。
     * </p>
     * 
     * @param value 対象のID値
     * @return 標準形式に変換されたID
     * @throws NumberFormatException 数字部分が数値に変換できない場合
     */
    private String standardizeId(String value) {
        // 数字部分を抽出
        String numericPart = extractNumericPart(value);

        // 数値に変換（数字以外の文字が含まれている場合はNumberFormatExceptionが発生）
        int numericValue = Integer.parseInt(numericPart);

        // 5桁の0埋め形式に変換
        String paddedNumeric = String.format("%05d", numericValue);

        // 「ID」接頭辞を付与
        return "ID" + paddedNumeric;
    }

    /**
     * 既に使用されているIDのセットを取得します。
     * テストやデバッグ、または新しいIDの追加時に有用です。
     * 
     * @return 使用中のIDセット
     */
    public Set<String> getUsedIds() {
        // 防御的コピーを返す
        return new HashSet<>(this.usedIds);
    }

    /**
     * 新しい使用済みIDをセットに追加します。
     * 新しいエンジニアが登録された後に呼び出されることを想定しています。
     * 
     * @param id 追加するID（標準形式でなくても自動的に変換されます）
     */
    public void addUsedId(String id) {
        if (id != null && !id.trim().isEmpty()) {
            try {
                // 標準形式に変換して追加
                this.usedIds.add(standardizeId(id));
            } catch (NumberFormatException e) {
                // 数値変換エラーの場合は追加しない
            }
        }
    }

    /**
     * 入力されたIDを標準形式に変換して返します。
     * <p>
     * このメソッドは、入力検証とは別に、任意のIDを標準形式に変換したい
     * 場合に使用できます。
     * </p>
     * 
     * @param id 変換するID
     * @return 標準形式に変換されたID
     * @throws IllegalArgumentException IDが無効な形式の場合
     */
    public String convertToStandardForm(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("IDはnullまたは空であってはなりません");
        }

        if (!checkFormat(id)) {
            throw new IllegalArgumentException("無効なID形式です: " + id);
        }

        return standardizeId(id);
    }
}
