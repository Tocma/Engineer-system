package util.validator;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import util.Constants.MessageEnum;

/**
 * バリデーション戦略を定義する列挙型クラス（新バリデーションシステム統合版）
 * エンジニア管理システムで使用される各種入力フィールドの検証ルールを定義し、
 * 新しい{@link FieldValidator}インターフェースの実装を通じて様々な検証方法
 * 
 * 
 * この列挙型は、Strategyパターンに基づいて実装されており、
 * 検証ロジックをカプセル化し、実行時に適切な検証戦略を選択できるようにします。
 * ValidationServiceとの統合を実現しています。
 * 
 * 各列挙値は特定のフィールドタイプに対応し、適切な{@link FieldValidator}実装
 * エラーメッセージは{@link MessageEnum}から取得し、一貫性のある表示を実現します。
 * 
 * @author Nakano
 */
public enum ValidatorEnum {

        /**
         * 氏名フィールドの検証
         * 制約：
         * 必須項目
         * 20文字以内
         * 日本語（漢字、ひらがな、カタカナ）のみ許可
         */
        NAME("name", () -> new NameValidator("name",
                        MessageEnum.VALIDATION_ERROR_NAME.getMessage())),

        /**
         * フリガナフィールドの検証
         * 制約：
         * 必須項目
         * 20文字以内
         * カタカナのみ許可
         */
        NAME_KANA("nameKana", () -> new NameKanaValidator("nameKana",
                        MessageEnum.VALIDATION_ERROR_NAME_KANA.getMessage())),

        /**
         * 社員IDフィールドの検証
         * 制約：
         * 必須項目
         * 5桁以内の数字
         * 既存IDとの重複不可
         * 注：このバリデーターは初期状態では空のIDセットで初期化され、使用前に
         * {@link #initializeIdValidator(Set)}メソッドで既存IDセットを設定する必要があります。
         * 
         */
        EMPLOYEE_ID("id", () -> new IDValidator("id",
                        MessageEnum.VALIDATION_ERROR_EMPLOYEE_ID.getMessage(), null)),

        /**
         * 生年月日フィールドの検証
         * 制約：
         * 必須項目
         * 1950年から現在までの有効な日付
         */
        BIRTH_DATE("birthDate", () -> new BirthDateValidator("birthDate",
                        MessageEnum.VALIDATION_ERROR_BIRTH_DATE.getMessage(),
                        LocalDate.of(1950, 1, 1), LocalDate.now())),

        /**
         * 入社年月フィールドの検証
         * 制約：
         * 必須項目
         * 1950年から現在までの有効な年月
         */
        JOIN_DATE("joinDate", () -> new JoinDateValidator("joinDate",
                        MessageEnum.VALIDATION_ERROR_JOIN_DATE.getMessage(),
                        LocalDate.of(1950, 1, 1), LocalDate.now())),

        /**
         * エンジニア歴フィールドの検証
         * 制約：
         * 必須項目
         * 0年目から50年目までの整数値
         */
        CAREER("career", () -> new CareerValidator("career",
                        MessageEnum.VALIDATION_ERROR_CAREER.getMessage(), 0, 50)),

        /**
         * プログラミング言語選択の検証
         * 制約：
         * 必須項目
         * 少なくとも1つ以上の言語を選択
         */
        PROGRAMMING_LANGUAGES("programmingLanguages", () -> new ProgrammingLanguagesValidator(
                        "programmingLanguages", MessageEnum.VALIDATION_ERROR_PROGRAMMING_LANGUAGES.getMessage())),

        /**
         * 経歴フィールドの検証
         * 制約：
         * 任意項目
         * 200文字以内
         */
        CAREER_HISTORY("careerHistory", () -> new TextValidator("careerHistory",
                        MessageEnum.VALIDATION_ERROR_CAREER_HISTORY.getMessage(), 200)),

        /**
         * 研修受講歴フィールドの検証
         * 制約：
         * 任意項目
         * 200文字以内
         */
        TRAINING_HISTORY("trainingHistory", () -> new TextValidator("trainingHistory",
                        MessageEnum.VALIDATION_ERROR_TRAINING_HISTORY.getMessage(), 200)),

        /**
         * 技術力フィールドの検証
         * 制約：
         * 1.0から5.0までの0.5刻みの値
         * 
         */
        TECHNICAL_SKILL("technicalSkill", () -> new SkillValidator("technicalSkill",
                        "技術力は1.0〜5.0の0.5刻みで選択してください")),

        /**
         * 受講態度フィールドの検証
         * 制約：
         * 1.0から5.0までの0.5刻みの値
         */
        LEARNING_ATTITUDE("learningAttitude", () -> new SkillValidator("learningAttitude",
                        "受講態度は1.0〜5.0の0.5刻みで選択してください")),

        /**
         * コミュニケーション能力フィールドの検証
         * 制約：
         * 1.0から5.0までの0.5刻みの値
         */
        COMMUNICATION_SKILL("communicationSkill", () -> new SkillValidator("communicationSkill",
                        "コミュニケーション能力は1.0〜5.0の0.5刻みで選択してください")),

        /**
         * リーダーシップフィールドの検証
         * 制約：
         * 1.0から5.0までの0.5刻みの値
         */
        LEADERSHIP("leadership", () -> new SkillValidator("leadership",
                        "リーダーシップは1.0〜5.0の0.5刻みで選択してください")),

        /**
         * 備考フィールドの検証
         * 制約：
         * 任意項目
         * 500文字以内
         */
        NOTE("note", () -> new TextValidator("note",
                        MessageEnum.VALIDATION_ERROR_NOTE.getMessage(), 500));

        /** フィールド名 */
        private final String fieldName;

        /** バリデーターファクトリー */
        private final ValidatorFactory validatorFactory;

        /** キャッシュされたバリデーターインスタンス */
        private FieldValidator cachedValidator;

        /** 既存IDセット（IDバリデーター用） */
        private static Set<String> existingIds = null;

        /**
         * バリデーターファクトリーインターフェース
         */
        @FunctionalInterface
        private interface ValidatorFactory {
                FieldValidator create();
        }

        /**
         * コンストラクタ
         * 
         * @param fieldName フィールド名
         * @param factory   バリデーターファクトリー
         */
        ValidatorEnum(String fieldName, ValidatorFactory factory) {
                this.fieldName = fieldName;
                this.validatorFactory = factory;
        }

        /**
         * この列挙値に関連付けられたバリデーターを取得
         * 
         * @return バリデーターインスタンス
         * @throws IllegalStateException IDバリデーターで既存IDセットが未設定の場合
         */
        public FieldValidator getValidator() {
                if (cachedValidator == null) {
                        // 特別処理：IDバリデーターの場合は既存IDセットをチェック
                        if (this == EMPLOYEE_ID && existingIds == null) {
                                throw new IllegalStateException(
                                                "IDバリデーターの既存IDセットが設定されていません。initializeIdValidator()メソッドを使用して初期化してください。");
                        }

                        cachedValidator = validatorFactory.create();

                        // IDバリデーターの場合は既存IDセットを設定
                        if (this == EMPLOYEE_ID && existingIds != null && cachedValidator instanceof IDValidator) {
                                cachedValidator = new IDValidator("id",
                                                MessageEnum.VALIDATION_ERROR_EMPLOYEE_ID.getMessage(), existingIds);
                        }
                }
                return cachedValidator;
        }

        /**
         * この列挙値に関連付けられたエラーメッセージを取得
         * 
         * @return エラーメッセージ
         */
        public String getErrorMessage() {
                return getValidator().getErrorMessage();
        }

        /**
         * フィールド名を取得
         * 
         * @return フィールド名
         */
        public String getFieldName() {
                return fieldName;
        }

        /**
         * バリデーターキャッシュをクリア
         * 設定変更後に新しいバリデーターインスタンスを生成したい場合に使用
         */
        public void clearCache() {
                this.cachedValidator = null;
        }

        /**
         * IDバリデーターを初期化するためのヘルパーメソッド。
         * 既存IDのセットを受け取り、適切な設定でIDValidatorを生成して設定
         * 
         * @param existingIdSet 既に使用されているIDのセット
         */
        public static void initializeIdValidator(Set<String> existingIdSet) {
                existingIds = existingIdSet;
                // IDバリデーターのキャッシュをクリア
                EMPLOYEE_ID.clearCache();
        }

        /**
         * 全バリデーターのキャッシュをクリア
         */
        public static void clearAllCaches() {
                for (ValidatorEnum validator : values()) {
                        validator.clearCache();
                }
        }

        /**
         * ValidatorFactoryと統合されたバリデーターマップを生成
         * 
         * @param existingIdSet 既存IDセット（重複チェック用）
         * @return フィールド名をキーとするバリデーターマップ
         */
        public static Map<String, FieldValidator> createValidatorMap(Set<String> existingIdSet) {
                // 既存IDセットを設定
                initializeIdValidator(existingIdSet);

                Map<String, FieldValidator> validatorMap = new HashMap<>();
                for (ValidatorEnum validator : values()) {
                        validatorMap.put(validator.getFieldName(), validator.getValidator());
                }
                return validatorMap;
        }

        /**
         * CSVバリデーション用のバリデーターマップを生成
         * UI用と異なり、既存IDチェックは後処理で行うため空のIDセットを使用
         * 
         * @return CSVバリデーション用のバリデーターマップ
         */
        public static Map<String, FieldValidator> createCSVValidatorMap() {
                return createValidatorMap(Set.of()); // 空のIDセット
        }

        /**
         * フィールド名から対応するValidatorEnumを取得
         * 
         * @param fieldName フィールド名
         * @return 対応するValidatorEnum、見つからない場合はnull
         */
        public static ValidatorEnum fromFieldName(String fieldName) {
                for (ValidatorEnum validator : values()) {
                        if (validator.getFieldName().equals(fieldName)) {
                                return validator;
                        }
                }
                return null;
        }

        /**
         * 全フィールドのバリデーションを実行
         * 
         * @param formData      フォームデータ（フィールド名 -> 値）
         * @param existingIdSet 既存IDセット
         * @return バリデーション結果
         */
        public static ValidationResult validateAllFields(Map<String, String> formData, Set<String> existingIdSet) {
                Map<String, FieldValidator> validators = createValidatorMap(existingIdSet);
                return ValidationService.getInstance().validateForm(formData, validators);
        }
}