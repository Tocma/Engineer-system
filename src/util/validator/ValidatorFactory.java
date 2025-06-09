package util.validator;

import util.Constants.MessageEnum;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * バリデータインスタンスを生成するファクトリクラス
 * 各種バリデータの生成と設定を一元管理します
 * 
 * @author Nakano
 */
public class ValidatorFactory {

    /** 日付範囲の最小値（1950年1月1日） */
    private static final LocalDate MIN_DATE = LocalDate.of(1950, 1, 1);

    /** 日付範囲の最大値（現在日付） */
    private static final LocalDate MAX_DATE = LocalDate.now();

    /**
     * エンジニア登録フォーム用のバリデータマップを生成
     * 
     * @param existingIds 既存のIDセット（重複チェック用）
     * @return バリデータマップ（フィールド名 -> バリデータ）
     */
    public static Map<String, FieldValidator> createEngineerValidators(Set<String> existingIds) {
        Map<String, FieldValidator> validators = new HashMap<>();

        // 各フィールドのバリデータを生成
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

        validators.put("careerHistory", new TextValidator("careerHistory",
                MessageEnum.VALIDATION_ERROR_CAREER_HISTORY.getMessage(), 200));

        validators.put("trainingHistory", new TextValidator("trainingHistory",
                MessageEnum.VALIDATION_ERROR_TRAINING_HISTORY.getMessage(), 200));

        validators.put("technicalSkill", new SkillValidator("technicalSkill",
                "技術力は1.0〜5.0の0.5刻みで選択してください"));

        validators.put("learningAttitude", new SkillValidator("learningAttitude",
                "受講態度は1.0〜5.0の0.5刻みで選択してください"));

        validators.put("communicationSkill", new SkillValidator("communicationSkill",
                "コミュニケーション能力は1.0〜5.0の0.5刻みで選択してください"));

        validators.put("leadership", new SkillValidator("leadership",
                "リーダーシップは1.0〜5.0の0.5刻みで選択してください"));

        validators.put("note", new TextValidator("note",
                MessageEnum.VALIDATION_ERROR_NOTE.getMessage(), 500));

        return validators;
    }

    /**
     * CSV読み込み用のバリデータマップを生成
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

        validators.put("careerHistory", new TextValidator("careerHistory",
                MessageEnum.VALIDATION_ERROR_CAREER_HISTORY.getMessage(), 200));

        validators.put("trainingHistory", new TextValidator("trainingHistory",
                MessageEnum.VALIDATION_ERROR_TRAINING_HISTORY.getMessage(), 200));

        validators.put("technicalSkill", new SkillValidator("technicalSkill",
                "技術力は1.0〜5.0の0.5刻みで選択してください"));

        validators.put("learningAttitude", new SkillValidator("learningAttitude",
                "受講態度は1.0〜5.0の0.5刻みで選択してください"));

        validators.put("communicationSkill", new SkillValidator("communicationSkill",
                "コミュニケーション能力は1.0〜5.0の0.5刻みで選択してください"));

        validators.put("leadership", new SkillValidator("leadership",
                "リーダーシップは1.0〜5.0の0.5刻みで選択してください"));

        validators.put("note", new TextValidator("note",
                MessageEnum.VALIDATION_ERROR_NOTE.getMessage(), 500));

        validators.put("registeredDate", new RegisteredDateValidator("registeredDate",
                "登録日の形式が不正です"));

        return validators;
    }
}