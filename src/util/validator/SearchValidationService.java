package util.validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import controller.MainController.SearchCriteria;
import util.LogHandler;
import util.LogHandler.LogType;
import util.StringUtil;

/**
 * 検索条件の前処理・バリデーション専用サービスクラス
 * 新規登録時と検索時で共通の検証ロジックを提供
 * 
 * @author Nakano
 */
public class SearchValidationService {

    /** シングルトンインスタンス */
    private static final SearchValidationService INSTANCE = new SearchValidationService();

    /** バリデーションサービス */
    private final ValidationService validationService;

    /** 検索用バリデータマップ */
    private final Map<String, FieldValidator> searchValidators;

    /** 社員ID検証パターン */
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{1,5}$");

    /** 氏名検証パターン（日本語文字） */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}ー　\\s]*$");

    /** 年月日検証パターン */
    private static final Pattern YEAR_PATTERN = Pattern.compile("^(19[5-9]\\d|20[0-2]\\d)$");
    private static final Pattern MONTH_PATTERN = Pattern.compile("^(0?[1-9]|1[0-2])$");
    private static final Pattern DAY_PATTERN = Pattern.compile("^(0?[1-9]|[12]\\d|3[01])$");

    /** エンジニア歴検証パターン */
    private static final Pattern CAREER_PATTERN = Pattern.compile("^(\\d{1,2})$");

    /**
     * プライベートコンストラクタ
     */
    private SearchValidationService() {
        this.validationService = ValidationService.getInstance();
        this.searchValidators = createSearchValidators();
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "SearchValidationServiceを初期化完了");
    }

    /**
     * シングルトンインスタンスを取得
     * 
     * @return SearchValidationServiceのインスタンス
     */
    public static SearchValidationService getInstance() {
        return INSTANCE;
    }

    /**
     * 検索条件の包括的な前処理・バリデーションを実行
     * 
     * @param searchCriteria 検索条件
     * @return バリデーション結果とエラーメッセージのリスト
     */
    public SearchValidationResult validateAndPreprocessSearchCriteria(SearchCriteria searchCriteria) {
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "検索条件の前処理・バリデーション開始");

            // 入力値の前処理を実行
            Map<String, String> preprocessedData = preprocessSearchInputs(searchCriteria);

            // バリデーション実行
            ValidationResult validationResult = validationService.validateForm(
                    preprocessedData, searchValidators);

            List<String> errorMessages = new ArrayList<>();

            if (!validationResult.isValid()) {
                // エラーメッセージの構築
                for (Map.Entry<String, String> error : validationResult.getErrors().entrySet()) {
                    String fieldName = getFieldDisplayName(error.getKey());
                    errorMessages.add(fieldName + ": " + error.getValue());
                }
            }

            // 追加の論理チェック（日付の妥当性など）
            if (validationResult.isValid()) {
                List<String> logicalErrors = performLogicalValidation(
                        validationResult.getProcessedValues());
                errorMessages.addAll(logicalErrors);
            }

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("検索条件バリデーション完了: 有効=%s, エラー数=%d",
                            errorMessages.isEmpty(), errorMessages.size()));

            return new SearchValidationResult(
                    errorMessages.isEmpty(),
                    errorMessages,
                    validationResult.getProcessedValues());

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "検索条件バリデーション中にエラーが発生", e);
            return new SearchValidationResult(false,
                    List.of("検索条件の検証中にエラーが発生: " + e.getMessage()),
                    new HashMap<>());
        }
    }

    /**
     * 検索入力値の前処理を実行
     * 
     * @param searchCriteria 検索条件
     * @return 前処理済みデータマップ
     */
    private Map<String, String> preprocessSearchInputs(SearchCriteria searchCriteria) {
        Map<String, String> preprocessedData = new HashMap<>();

        // 社員ID: 全角数字→半角、前後空白除去
        String id = preprocessId(searchCriteria.getId());
        preprocessedData.put("id", id);

        // 氏名: 前後空白除去、全角スペース→半角スペース
        String name = preprocessName(searchCriteria.getName());
        preprocessedData.put("name", name);

        // 生年月日の各要素
        preprocessedData.put("year", preprocessNumeric(searchCriteria.getYear()));
        preprocessedData.put("month", preprocessNumeric(searchCriteria.getMonth()));
        preprocessedData.put("day", preprocessNumeric(searchCriteria.getDay()));

        // エンジニア歴
        String career = preprocessNumeric(searchCriteria.getCareer());
        preprocessedData.put("career", career);

        return preprocessedData;
    }

    /**
     * 社員IDの前処理
     */
    private String preprocessId(String id) {
        if (id == null)
            return "";

        // 全ての半角・全角スペースを除去
        String noSpaces = StringUtil.removeSpaces(id);

        Pattern pattern = Pattern.compile("[０-９]");
        Matcher matcher = pattern.matcher(noSpaces);

        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            char c = (char) (matcher.group().charAt(0) - '０' + '0');
            matcher.appendReplacement(buffer, String.valueOf(c));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    /**
     * 氏名の前処理
     */
    private String preprocessName(String name) {
        if (name == null)
            return "";

        // 全ての半角・全角スペースを除去
        return StringUtil.removeSpaces(name);
    }

    /**
     * 数値項目の前処理
     */
    private String preprocessNumeric(String value) {
        if (value == null || "未選択".equals(value))
            return "";

        // 全ての半角・全角スペースを除去
        String noSpaces = StringUtil.removeSpaces(value);

        Pattern pattern = Pattern.compile("[０-９]");
        Matcher matcher = pattern.matcher(noSpaces);

        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            char c = (char) (matcher.group().charAt(0) - '０' + '0');
            matcher.appendReplacement(buffer, String.valueOf(c));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    /**
     * 論理的なバリデーションを実行
     * 
     * @param processedValues 前処理済み値マップ
     * @return エラーメッセージのリスト
     */
    private List<String> performLogicalValidation(Map<String, String> processedValues) {
        List<String> errors = new ArrayList<>();

        // 生年月日の妥当性チェック（AND検索対応）
        String year = processedValues.get("year");
        String month = processedValues.get("month");
        String day = processedValues.get("day");

        // 年・月・日がすべて入力されている場合のみ日付妥当性をチェック
        if (isAllNotEmpty(year, month, day)) {
            try {
                LocalDate birthDate = LocalDate.of(
                        Integer.parseInt(year),
                        Integer.parseInt(month),
                        Integer.parseInt(day));

                // 未来日チェック
                if (birthDate.isAfter(LocalDate.now())) {
                    errors.add("生年月日に未来の日付は指定できません");
                }

                // 年齢チェック（100歳以上は警告）
                if (birthDate.isBefore(LocalDate.now().minusYears(100))) {
                    errors.add("生年月日が100年以上前に設定されています。正しい日付か確認してください");
                }

            } catch (Exception e) {
                errors.add("無効な生年月日が指定されています");
            }
        }
        // AND検索では個別項目の妥当性のみチェック（組み合わせは要求しない）
        else {
            // 年のみの妥当性チェック
            if (!year.isEmpty()) {
                try {
                    int yearValue = Integer.parseInt(year);
                    if (yearValue < 1950 || yearValue > LocalDate.now().getYear()) {
                        errors.add("年は1950年から現在年まで入力してください");
                    }
                } catch (NumberFormatException e) {
                    errors.add("年は数値で入力してください");
                }
            }

            // 月のみの妥当性チェック
            if (!month.isEmpty()) {
                try {
                    int monthValue = Integer.parseInt(month);
                    if (monthValue < 1 || monthValue > 12) {
                        errors.add("月は1から12の範囲で入力してください");
                    }
                } catch (NumberFormatException e) {
                    errors.add("月は数値で入力してください");
                }
            }

            // 日のみの妥当性チェック
            if (!day.isEmpty()) {
                try {
                    int dayValue = Integer.parseInt(day);
                    if (dayValue < 1 || dayValue > 31) {
                        errors.add("日は1から31の範囲で入力してください");
                    }
                } catch (NumberFormatException e) {
                    errors.add("日は数値で入力してください");
                }
            }
        }

        // エンジニア歴の論理チェック
        String career = processedValues.get("career");
        if (!career.isEmpty()) {
            try {
                int careerYears = Integer.parseInt(career);
                if (careerYears > 50) {
                    errors.add("エンジニア歴は50年以下で指定してください");
                }
            } catch (NumberFormatException e) {
                errors.add("エンジニア歴は数値で指定してください");
            }
        }

        return errors;
    }

    /**
     * 検索用バリデータマップを作成
     */
    private Map<String, FieldValidator> createSearchValidators() {
        Map<String, FieldValidator> validators = new HashMap<>();

        // 社員IDバリデータ（検索用：空文字許可、形式チェックのみ）
        validators.put("id", new SearchFieldValidator("id") {
            @Override
            public boolean validate(String value) {
                if (value.isEmpty())
                    return true;
                return ID_PATTERN.matcher(value).matches();
            }

            @Override
            public String getErrorMessage() {
                return "社員IDは5桁以内の数字で入力してください";
            }
        });

        // 氏名バリデータ（検索用：空文字許可、日本語文字チェック）
        validators.put("name", new SearchFieldValidator("name") {
            @Override
            public boolean validate(String value) {
                if (value.isEmpty())
                    return true;
                return value.length() <= 20 && NAME_PATTERN.matcher(value).matches();
            }

            @Override
            public String getErrorMessage() {
                return "氏名は20文字以内の日本語文字で入力してください";
            }
        });

        // 年バリデータ
        validators.put("year", new SearchFieldValidator("year") {
            @Override
            public boolean validate(String value) {
                if (value.isEmpty())
                    return true;
                return YEAR_PATTERN.matcher(value).matches();
            }

            @Override
            public String getErrorMessage() {
                return "年は1950年から現在年まで入力可能です";
            }
        });

        // 月バリデータ
        validators.put("month", new SearchFieldValidator("month") {
            @Override
            public boolean validate(String value) {
                if (value.isEmpty())
                    return true;
                return MONTH_PATTERN.matcher(value).matches();
            }

            @Override
            public String getErrorMessage() {
                return "月は1から12の範囲で入力してください";
            }
        });

        // 日バリデータ
        validators.put("day", new SearchFieldValidator("day") {
            @Override
            public boolean validate(String value) {
                if (value.isEmpty())
                    return true;
                return DAY_PATTERN.matcher(value).matches();
            }

            @Override
            public String getErrorMessage() {
                return "日は1から31の範囲で入力してください";
            }
        });

        // エンジニア歴バリデータ
        validators.put("career", new SearchFieldValidator("career") {
            @Override
            public boolean validate(String value) {
                if (value.isEmpty())
                    return true;
                return CAREER_PATTERN.matcher(value).matches();
            }

            @Override
            public String getErrorMessage() {
                return "エンジニア歴は数字で入力してください";
            }
        });

        return validators;
    }

    /**
     * フィールド表示名を取得
     */
    private String getFieldDisplayName(String fieldName) {
        switch (fieldName) {
            case "id":
                return "社員ID";
            case "name":
                return "氏名";
            case "year":
                return "生年月日（年）";
            case "month":
                return "生年月日（月）";
            case "day":
                return "生年月日（日）";
            case "career":
                return "エンジニア歴";
            default:
                return fieldName;
        }
    }

    /**
     * すべてが空でないかチェック
     */
    private boolean isAllNotEmpty(String... values) {
        for (String value : values) {
            if (value == null || value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 検索用フィールドバリデータの抽象クラス
     */
    private abstract static class SearchFieldValidator implements FieldValidator {
        private final String fieldName;

        public SearchFieldValidator(String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public String preprocess(String value) {
            return value != null ? value.trim() : "";
        }

        @Override
        public String getFieldName() {
            return fieldName;
        }
    }

    /**
     * 検索バリデーション結果クラス
     */
    public static class SearchValidationResult {
        private final boolean valid;
        private final List<String> errorMessages;
        private final Map<String, String> processedValues;

        public SearchValidationResult(boolean valid, List<String> errorMessages,
                Map<String, String> processedValues) {
            this.valid = valid;
            this.errorMessages = errorMessages;
            this.processedValues = processedValues;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrorMessages() {
            return errorMessages;
        }

        public Map<String, String> getProcessedValues() {
            return processedValues;
        }
    }
}