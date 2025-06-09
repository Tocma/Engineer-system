package util.validator;

import java.util.List;
import java.util.ArrayList;

/**
 * プログラミング言語選択検証用バリデータ
 * 少なくとも1つ以上の言語が選択されているかを検証します
 * 
 * @author Nakano
 */
public class ProgrammingLanguagesValidator extends AbstractValidator {

    /** 言語区切り文字 */
    private static final String LANGUAGE_DELIMITER = ";";

    /**
     * コンストラクタ
     * 
     * @param fieldName    フィールド名
     * @param errorMessage エラーメッセージ
     */
    public ProgrammingLanguagesValidator(String fieldName, String errorMessage) {
        super(fieldName, errorMessage);
    }

    /**
     * プログラミング言語リストの前処理を実行
     * 
     * @param value 入力値（セミコロン区切りの言語リスト）
     * @return 前処理済みの値
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

        // 各言語の前後の空白を除去
        List<String> languages = parseLanguages(trimmed);
        String normalized = String.join(LANGUAGE_DELIMITER, languages);

        logDebug("言語リスト前処理: " + value + " -> " + normalized);

        return normalized;
    }

    /**
     * プログラミング言語の検証を実行
     * 
     * @param value 検証対象の値（前処理済み）
     * @return 検証成功の場合true
     */
    @Override
    public boolean validate(String value) {
        // nullチェック
        if (value == null) {
            logWarning("言語検証失敗: null値");
            return false;
        }

        // 空リストチェック
        if (value.isEmpty()) {
            logWarning("言語検証失敗: 未選択");
            return false;
        }

        // 選択数チェック
        List<String> languages = parseLanguages(value);
        if (!checkSelectionCount(languages)) {
            logWarning("言語検証失敗: 選択数不足");
            return false;
        }

        logDebug("言語検証成功: " + value);
        return true;
    }

    /**
     * 言語リストを解析
     * 
     * @param value セミコロン区切りの言語リスト
     * @return 言語のリスト
     */
    private List<String> parseLanguages(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }

        String[] parts = value.split(LANGUAGE_DELIMITER);
        List<String> languages = new ArrayList<>();

        for (String lang : parts) {
            String trimmed = lang.trim();
            if (!trimmed.isEmpty()) {
                languages.add(trimmed);
            }
        }

        return languages;
    }

    /**
     * 選択数チェック
     * 
     * @param languages 言語リスト
     * @return 1つ以上選択されている場合true
     */
    private boolean checkSelectionCount(List<String> languages) {
        return !languages.isEmpty();
    }
}