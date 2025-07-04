package util.validator;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import util.Constants.MessageEnum;

/**
 * バリデータファクトリクラス（Unicode対応版）
 * サロゲートペア文字、絵文字、環境依存文字を適切に処理するバリデータを生成
 * 
 * @author Nakano
 */
public final class ValidatorFactory {

        /** 最小日付 */
        private static final LocalDate MIN_DATE = LocalDate.of(1950, 1, 1);

        /** 最大日付 */
        private static final LocalDate MAX_DATE = LocalDate.now();

        /**
         * プライベートコンストラクタ
         */
        private ValidatorFactory() {
                throw new AssertionError("ファクトリクラスはインスタンス化できません");
        }

        /**
         * UI用のバリデータマップを生成（Unicode対応版）
         * 
         * @param existingIds 既存ID一覧
         * @return バリデータマップ（フィールド名 -> バリデータ）
         */
        public static Map<String, FieldValidator> createEngineerValidators(Set<String> existingIds) {
                Map<String, FieldValidator> validators = new HashMap<>();

                validators.put("id", new IDValidator("id",
                                MessageEnum.VALIDATION_ERROR_EMPLOYEE_ID.getMessage(), existingIds));

                validators.put("name", new NameValidator("name",
                                MessageEnum.VALIDATION_ERROR_NAME.getMessage()));

                validators.put("nameKana", new NameKanaValidator("nameKana",
                                MessageEnum.VALIDATION_ERROR_NAME_KANA.getMessage()));

                validators.put("birthDate", new BirthDateValidator("birthDate",
                                MessageEnum.VALIDATION_ERROR_BIRTH_DATE.getMessage(), MIN_DATE, MAX_DATE));

                validators.put("joinDate", new JoinDateValidator("joinDate",
                                MessageEnum.VALIDATION_ERROR_JOIN_DATE.getMessage(), MIN_DATE, MAX_DATE));

                validators.put("career", new CareerValidator("career",
                                MessageEnum.VALIDATION_ERROR_CAREER.getMessage(), 0, 50));

                validators.put("programmingLanguages", new ProgrammingLanguagesValidator("programmingLanguages",
                                MessageEnum.VALIDATION_ERROR_PROGRAMMING_LANGUAGES.getMessage()));

                // Unicode対応TextValidator（絵文字許可、環境依存文字不許可）
                validators.put("careerHistory", new TextValidator("careerHistory",
                                MessageEnum.VALIDATION_ERROR_CAREER_HISTORY.getMessage(), 200, true, false));

                validators.put("trainingHistory", new TextValidator("trainingHistory",
                                MessageEnum.VALIDATION_ERROR_TRAINING_HISTORY.getMessage(), 200, true, false));

                validators.put("technicalSkill", new SkillValidator("technicalSkill",
                                "技術力は1.0〜5.0の0.5刻みで選択してください"));

                validators.put("learningAttitude", new SkillValidator("learningAttitude",
                                "受講態度は1.0〜5.0の0.5刻みで選択してください"));

                validators.put("communicationSkill", new SkillValidator("communicationSkill",
                                "コミュニケーション能力は1.0〜5.0の0.5刻みで選択してください"));

                validators.put("leadership", new SkillValidator("leadership",
                                "リーダーシップは1.0〜5.0の0.5刻みで選択してください"));

                // Unicode対応TextValidator（絵文字許可、環境依存文字不許可）
                validators.put("note", new TextValidator("note",
                                MessageEnum.VALIDATION_ERROR_NOTE.getMessage(), 500, true, false));

                return validators;
        }

        /**
         * CSV読み込み用のバリデータマップを生成（Unicode対応版）
         * UI用と異なり、既存IDチェックは行わない
         * 
         * @return バリデータマップ（フィールド名 -> バリデータ）
         */
        public static Map<String, FieldValidator> createCSVValidators() {
                Map<String, FieldValidator> validators = new HashMap<>();

                // CSV読み込み用のバリデータを生成
                // 既存IDチェックは後処理で行うため、nullを渡す
                validators.put("id", new IDValidator("id",
                                MessageEnum.VALIDATION_ERROR_EMPLOYEE_ID.getMessage(), null));

                validators.put("name", new NameValidator("name",
                                MessageEnum.VALIDATION_ERROR_NAME.getMessage()));

                validators.put("nameKana", new NameKanaValidator("nameKana",
                                MessageEnum.VALIDATION_ERROR_NAME_KANA.getMessage()));

                validators.put("birthDate", new BirthDateValidator("birthDate",
                                MessageEnum.VALIDATION_ERROR_BIRTH_DATE.getMessage(), MIN_DATE, MAX_DATE));

                validators.put("joinDate", new JoinDateValidator("joinDate",
                                MessageEnum.VALIDATION_ERROR_JOIN_DATE.getMessage(), MIN_DATE, MAX_DATE));

                validators.put("career", new CareerValidator("career",
                                MessageEnum.VALIDATION_ERROR_CAREER.getMessage(), 0, 50));

                validators.put("programmingLanguages", new ProgrammingLanguagesValidator("programmingLanguages",
                                MessageEnum.VALIDATION_ERROR_PROGRAMMING_LANGUAGES.getMessage()));

                // CSV用TextValidator（絵文字不許可、環境依存文字不許可）
                validators.put("careerHistory", new TextValidator("careerHistory",
                                MessageEnum.VALIDATION_ERROR_CAREER_HISTORY.getMessage(), 200, false, false));

                validators.put("trainingHistory", new TextValidator("trainingHistory",
                                MessageEnum.VALIDATION_ERROR_TRAINING_HISTORY.getMessage(), 200, false, false));

                validators.put("technicalSkill", new SkillValidator("technicalSkill",
                                "技術力は1.0〜5.0の0.5刻みで選択してください"));

                validators.put("learningAttitude", new SkillValidator("learningAttitude",
                                "受講態度は1.0〜5.0の0.5刻みで選択してください"));

                validators.put("communicationSkill", new SkillValidator("communicationSkill",
                                "コミュニケーション能力は1.0〜5.0の0.5刻みで選択してください"));

                validators.put("leadership", new SkillValidator("leadership",
                                "リーダーシップは1.0〜5.0の0.5刻みで選択してください"));

                // CSV用TextValidator（絵文字不許可、環境依存文字不許可）
                validators.put("note", new TextValidator("note",
                                MessageEnum.VALIDATION_ERROR_NOTE.getMessage(), 500, false, false));

                validators.put("registeredDate", new RegisteredDateValidator("registeredDate",
                                "登録日の形式が不正です"));

                return validators;
        }

        /**
         * 厳格なバリデータマップを生成（特殊用途向け）
         * 絵文字と環境依存文字を完全に禁止
         * 
         * @param existingIds 既存ID一覧
         * @return バリデータマップ（フィールド名 -> バリデータ）
         */
        public static Map<String, FieldValidator> createStrictValidators(Set<String> existingIds) {
                Map<String, FieldValidator> validators = new HashMap<>();

                validators.put("id", new IDValidator("id",
                                MessageEnum.VALIDATION_ERROR_EMPLOYEE_ID.getMessage(), existingIds));

                validators.put("name", new NameValidator("name",
                                MessageEnum.VALIDATION_ERROR_NAME.getMessage()));

                validators.put("nameKana", new NameKanaValidator("nameKana",
                                MessageEnum.VALIDATION_ERROR_NAME_KANA.getMessage()));

                validators.put("birthDate", new BirthDateValidator("birthDate",
                                MessageEnum.VALIDATION_ERROR_BIRTH_DATE.getMessage(), MIN_DATE, MAX_DATE));

                validators.put("joinDate", new JoinDateValidator("joinDate",
                                MessageEnum.VALIDATION_ERROR_JOIN_DATE.getMessage(), MIN_DATE, MAX_DATE));

                validators.put("career", new CareerValidator("career",
                                MessageEnum.VALIDATION_ERROR_CAREER.getMessage(), 0, 50));

                validators.put("programmingLanguages", new ProgrammingLanguagesValidator("programmingLanguages",
                                MessageEnum.VALIDATION_ERROR_PROGRAMMING_LANGUAGES.getMessage()));

                // 厳格なTextValidator（絵文字・環境依存文字共に不許可）
                validators.put("careerHistory", new TextValidator("careerHistory",
                                MessageEnum.VALIDATION_ERROR_CAREER_HISTORY.getMessage(), 200, false, false));

                validators.put("trainingHistory", new TextValidator("trainingHistory",
                                MessageEnum.VALIDATION_ERROR_TRAINING_HISTORY.getMessage(), 200, false, false));

                validators.put("technicalSkill", new SkillValidator("technicalSkill",
                                "技術力は1.0〜5.0の0.5刻みで選択してください"));

                validators.put("learningAttitude", new SkillValidator("learningAttitude",
                                "受講態度は1.0〜5.0の0.5刻みで選択してください"));

                validators.put("communicationSkill", new SkillValidator("communicationSkill",
                                "コミュニケーション能力は1.0〜5.0の0.5刻みで選択してください"));

                validators.put("leadership", new SkillValidator("leadership",
                                "リーダーシップは1.0〜5.0の0.5刻みで選択してください"));

                // 厳格なTextValidator（絵文字・環境依存文字共に不許可）
                validators.put("note", new TextValidator("note",
                                MessageEnum.VALIDATION_ERROR_NOTE.getMessage(), 500, false, false));

                validators.put("registeredDate", new RegisteredDateValidator("registeredDate",
                                "登録日の形式が不正です"));

                return validators;
        }
}