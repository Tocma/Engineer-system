package util.validator;

import java.util.Set;

import util.Constants.MessageEnum;

/**
 * バリデーション戦略を定義する列挙型クラス。
 * エンジニア管理システムで使用される各種入力フィールドの検証ルールを定義し、
 * {@link Validator}インターフェースの実装を通じて様々な検証方法を提供します。
 * 
 * <p>
 * この列挙型は、Strategyパターンに基づいて実装されており、
 * 検証ロジックをカプセル化し、実行時に適切な検証戦略を選択できるようにします。
 * </p>
 * 
 * <p>
 * 各列挙値は特定のフィールドタイプに対応し、適切な{@link Validator}実装を提供します。
 * エラーメッセージは{@link MessageEnum}から取得し、一貫性のある表示を実現します。
 * </p>
 * 
 * <p>
 * 使用例：
 * </p>
 * 
 * <pre>
 * // 氏名のバリデーション
 * Validator nameValidator = ValidationEnum.NAME.getValidator();
 * boolean isValid = nameValidator.validate("山田太郎");
 * if (!isValid) {
 *         String errorMessage = ValidationEnum.NAME.getErrorMessage();
 *         // エラー処理...
 * }
 * </pre>
 * 
 * @author Nakano
 * @version 4.0.0
 * @since 2025-04-15
 * @see Validator
 * @see TextValidator
 * @see DateValidator
 * @see IDValidator
 * @see MessageEnum
 */
public enum ValidatorEnum {

        /**
         * 氏名フィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>必須項目</li>
         * <li>20文字以内</li>
         * <li>日本語（漢字、ひらがな、カタカナ）のみ許可</li>
         * </ul>
         */
        NAME(new TextValidator(
                        20,
                        "^[\\p{InHiragana}\\p{InKatakana}\\p{InCJKUnifiedIdeographs}]+$",
                        MessageEnum.VALIDATION_ERROR_NAME.getMessage())),

        /**
         * フリガナフィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>必須項目</li>
         * <li>20文字以内</li>
         * <li>カタカナのみ許可</li>
         * </ul>
         */
        NAME_KANA(new TextValidator(
                        20,
                        "^[\\p{InKatakana}]+$",
                        MessageEnum.VALIDATION_ERROR_NAME_KANA.getMessage())),

        /**
         * 社員IDフィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>必須項目</li>
         * <li>5桁以内の数字</li>
         * <li>既存IDとの重複不可</li>
         * </ul>
         * <p>
         * 注：このバリデーターは初期状態ではnullであり、使用前に
         * {@link #setValidator(Validator)}メソッドで初期化する必要があります。
         * これは、既存IDのリストが実行時に動的に変わるためです。
         * </p>
         */
        EMPLOYEE_ID(null),

        /**
         * 生年月日フィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>必須項目</li>
         * <li>1950年から現在までの有効な日付</li>
         * </ul>
         * <p>
         * 注：このバリデーターは初期状態ではnullであり、使用前に
         * {@link #setValidator(Validator)}メソッドで初期化する必要があります。
         * これは、「現在の日付」が実行時によって変わるためです。
         * </p>
         */
        BIRTH_DATE(null),

        /**
         * 入社年月フィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>必須項目</li>
         * <li>1950年から現在までの有効な年月</li>
         * </ul>
         * <p>
         * 注：このバリデーターは初期状態ではnullであり、使用前に
         * {@link #setValidator(Validator)}メソッドで初期化する必要があります。
         * これは、「現在の日付」が実行時によって変わるためです。
         * </p>
         */
        JOIN_DATE(null),

        /**
         * エンジニア歴フィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>必須項目</li>
         * <li>1年目から50年目までの整数値</li>
         * </ul>
         */
        CAREER(new TextValidator(
                        2,
                        "^([1-9]|[1-4][0-9]|50)$",
                        MessageEnum.VALIDATION_ERROR_CAREER.getMessage())),

        /**
         * プログラミング言語選択の検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>必須項目</li>
         * <li>少なくとも1つ以上の言語を選択</li>
         * </ul>
         * <p>
         * 注：このバリデーターは、選択された言語のリスト（空でないこと）を
         * 検証するカスタムバリデーターを使用します。
         * </p>
         */
        PROGRAMMING_LANGUAGES(new Validator() {
                @Override
                public boolean validate(String value) {
                        // カンマ区切りの言語リストを想定
                        if (value == null || value.trim().isEmpty()) {
                                return false;
                        }
                        // 少なくとも1つの言語が選択されていることを確認
                        return value.split(",").length > 0;
                }

                @Override
                public String getErrorMessage() {
                        return MessageEnum.VALIDATION_ERROR_PROGRAMMING_LANGUAGES.getMessage();
                }
        }),

        /**
         * 経歴フィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>任意項目</li>
         * <li>200文字以内</li>
         * </ul>
         */
        CAREER_HISTORY(new TextValidator(
                        200,
                        null, // パターン制約なし
                        MessageEnum.VALIDATION_ERROR_CAREER_HISTORY.getMessage())),

        /**
         * 研修受講歴フィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>任意項目</li>
         * <li>200文字以内</li>
         * </ul>
         */
        TRAINING_HISTORY(new TextValidator(
                        200,
                        null, // パターン制約なし
                        MessageEnum.VALIDATION_ERROR_TRAINING_HISTORY.getMessage())),

        /**
         * 技術力フィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>1.0から5.0までの0.5刻みの値</li>
         * </ul>
         */
        TECHNICAL_SKILL(new TextValidator(
                        3,
                        "^[1-5](\\.0|\\.5)?$",
                        "技術力は1.0〜5.0の0.5刻みで選択してください" // 専用メッセージ
        )),

        /**
         * 受講態度フィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>1.0から5.0までの0.5刻みの値</li>
         * </ul>
         */
        LEARNING_ATTITUDE(new TextValidator(
                        3,
                        "^[1-5](\\.0|\\.5)?$",
                        "受講態度は1.0〜5.0の0.5刻みで選択してください" // 専用メッセージ
        )),

        /**
         * コミュニケーション能力フィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>1.0から5.0までの0.5刻みの値</li>
         * </ul>
         */
        COMMUNICATION_SKILL(new TextValidator(
                        3,
                        "^[1-5](\\.0|\\.5)?$",
                        "コミュニケーション能力は1.0〜5.0の0.5刻みで選択してください" // 専用メッセージ
        )),

        /**
         * リーダーシップフィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>1.0から5.0までの0.5刻みの値</li>
         * </ul>
         */
        LEADERSHIP(new TextValidator(
                        3,
                        "^[1-5](\\.0|\\.5)?$",
                        "リーダーシップは1.0〜5.0の0.5刻みで選択してください" // 専用メッセージ
        )),

        /**
         * 備考フィールドの検証
         * <p>
         * 制約：
         * </p>
         * <ul>
         * <li>任意項目</li>
         * <li>500文字以内</li>
         * </ul>
         */
        NOTE(new TextValidator(
                        500,
                        null, // パターン制約なし
                        MessageEnum.VALIDATION_ERROR_NOTE.getMessage()));

        /**
         * バリデーターインスタンス
         */
        private Validator validator;

        /**
         * コンストラクタ
         * 
         * @param validator 使用するバリデーターインスタンス
         */
        ValidatorEnum(Validator validator) {
                this.validator = validator;
        }

        /**
         * この列挙値に関連付けられたバリデーターを取得します。
         * 
         * @return バリデーターインスタンス
         * @throws IllegalStateException バリデーターがnullで初期化されていない場合
         */
        public Validator getValidator() {
                if (this.validator == null) {
                        throw new IllegalStateException(
                                        "バリデーターが初期化されていません。setValidator()メソッドを使用して初期化してください。: " + this.name());
                }
                return this.validator;
        }

        /**
         * この列挙値に関連付けられたエラーメッセージを取得します。
         * 
         * @return エラーメッセージ
         * @throws IllegalStateException バリデーターがnullで初期化されていない場合
         */
        public String getErrorMessage() {
                return getValidator().getErrorMessage();
        }

        /**
         * この列挙値に関連付けられたバリデーターを設定します。
         * 動的に初期化が必要な列挙値（EMPLOYEE_ID, BIRTH_DATE, JOIN_DATEなど）で使用します。
         * 
         * <p>
         * 使用例：
         * </p>
         * 
         * <pre>
         * // 社員IDバリデーターの初期化
         * Set&lt;String&gt; existingIds = fetchExistingIds(); // 既存IDの取得
         * IDValidator idValidator = new IDValidator(
         *                 existingIds,
         *                 MessageEnum.VALIDATION_ERROR_EMPLOYEE_ID.getMessage());
         * ValidationEnum.EMPLOYEE_ID.setValidator(idValidator);
         * </pre>
         * 
         * @param validator 設定するバリデーターインスタンス
         */
        public void setValidator(Validator validator) {
                this.validator = validator;
        }

        /**
         * IDバリデーターを初期化するためのヘルパーメソッド。
         * 既存IDのセットを受け取り、適切な設定でIDValidatorを生成して設定します。
         * 
         * @param existingIds 既に使用されているIDのセット
         */
        public static void initializeIdValidator(Set<String> existingIds) {
                EMPLOYEE_ID.setValidator(new IDValidator(
                                existingIds,
                                MessageEnum.VALIDATION_ERROR_EMPLOYEE_ID.getMessage()));
        }

        /**
         * 日付バリデーターを初期化するためのヘルパーメソッド。
         * 最小日付と最大日付を受け取り、適切な設定でDateValidatorを生成して
         * BIRTH_DATEとJOIN_DATEの両方を初期化します。
         * 
         * @param minDate 最小許容日付（例：1950年1月1日）
         * @param maxDate 最大許容日付（通常は現在日付）
         */
        public static void initializeDateValidators(java.util.Date minDate, java.util.Date maxDate) {
                BIRTH_DATE.setValidator(new DateValidator(
                                minDate,
                                maxDate,
                                "yyyy/MM/dd",
                                MessageEnum.VALIDATION_ERROR_BIRTH_DATE.getMessage()));

                JOIN_DATE.setValidator(new DateValidator(
                                minDate,
                                maxDate,
                                "yyyy/MM",
                                MessageEnum.VALIDATION_ERROR_JOIN_DATE.getMessage()));
        }
}
