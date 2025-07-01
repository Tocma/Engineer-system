package util.validator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import view.DateOptionUtil;

/**
 * プログラミング言語選択検証用バリデータ
 * CSV取込時の入力はUIで選択可能な言語との完全一致のみ許可
 * 
 * @author Nakano
 */
public class ProgrammingLanguagesValidator extends AbstractValidator {

    /** 言語区切り文字 */
    private static final String LANGUAGE_DELIMITER = ";";

    /** UIで選択可能な言語一覧（DateOptionUtilから取得） */
    private static final Set<String> AVAILABLE_LANGUAGES = new HashSet<>(
            Arrays.asList(DateOptionUtil.getAvailableLanguages()));

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

        return normalized;
    }

    /**
     * プログラミング言語の検証を実行
     * UIで選択可能な言語との完全一致チェックを含む
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

        // 言語リストを解析
        List<String> languages = parseLanguages(value);

        // 選択数チェック
        if (!checkSelectionCount(languages)) {
            logWarning("言語検証失敗: 選択数不足");
            return false;
        }

        // 完全一致チェック（CSV取込時の要件）
        if (!validateLanguageNames(languages)) {
            logWarning("言語検証失敗: 無効な言語名が含まれています");
            return false;
        }

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

    /**
     * 言語名の完全一致チェック
     * CSV取込時にUIで選択可能な言語と完全一致するかを検証
     * 
     * @param languages 検証対象の言語リスト
     * @return すべての言語が有効な場合true
     */
    private boolean validateLanguageNames(List<String> languages) {
        for (String language : languages) {
            if (!AVAILABLE_LANGUAGES.contains(language)) {
                logWarning("無効な言語名: " + language +
                        " (利用可能な言語: " + String.join(", ", AVAILABLE_LANGUAGES) + ")");
                return false;
            }
        }
        return true;
    }

    /**
     * 利用可能な言語一覧を取得
     * デバッグ用のメソッド
     * 
     * @return 利用可能な言語のセット
     */
    public static Set<String> getAvailableLanguages() {
        return new HashSet<>(AVAILABLE_LANGUAGES);
    }
}