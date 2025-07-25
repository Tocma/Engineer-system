package model;

import java.time.LocalDate;
import java.util.List;

import util.validator.IDValidator;

/**
 * EngineerDTOオブジェクトを生成するビルダークラス
 * Builderパターンを使用してEngineerDTOの段階的な構築
 * 
 * 
 * @author Nakano
 */
public class EngineerBuilder {
    // 構築中のエンジニアDTOオブジェクト
    private final EngineerDTO engineer;

    /**
     * コンストラクタ
     * 新しいEngineerDTOインスタンスを初期化
     */
    public EngineerBuilder() {
        this.engineer = new EngineerDTO();
    }

    /**
     * 構築したEngineerDTOを返す
     * 必須フィールドのバリデーションを実行
     * 
     * @return 構築されたEngineerDTOオブジェクト
     * @throws IllegalStateException 必須フィールドが設定されていない場合
     */
    public EngineerDTO build() {
        validateRequiredFields();
        return engineer;
    }

    /**
     * 必須フィールドの検証
     * 必須フィールドが全て設定されていることを確認
     * 
     * @throws IllegalStateException 必須フィールドが設定されていない場合
     */
    private void validateRequiredFields() {
        if (engineer.getId() == null || engineer.getId().trim().isEmpty()) {
            throw new IllegalStateException("社員IDは必須です");
        }
        if (engineer.getName() == null || engineer.getName().trim().isEmpty()) {
            throw new IllegalStateException("氏名は必須です");
        }
        if (engineer.getNameKana() == null || engineer.getNameKana().trim().isEmpty()) {
            throw new IllegalStateException("フリガナは必須です");
        }
        if (engineer.getBirthDate() == null) {
            throw new IllegalStateException("生年月日は必須です");
        }
        if (engineer.getJoinDate() == null) {
            throw new IllegalStateException("入社年月は必須です");
        }
        if (engineer.getCareer() < 0) {
            throw new IllegalStateException("エンジニア歴は1以上の値が必要です");
        }
        if (engineer.getProgrammingLanguages() == null || engineer.getProgrammingLanguages().isEmpty()) {
            throw new IllegalStateException("扱える言語は最低1つ設定する必要があります");
        }
    }

    /**
     * 社員IDを設定
     * 入力されたIDの空白除去、全角→半角変換、標準形式変換を実行
     * 
     * @param id 社員ID
     */
    public EngineerBuilder setId(String id) {
        // 空白除去 → 全角半角変換 → 標準形式変換
        String cleanedId = IDValidator.removeAllWhitespace(id);
        String convertedId = IDValidator.convertFullWidthToHalfWidth(cleanedId);
        String standardizedId = IDValidator.standardizeId(convertedId);
        engineer.setId(standardizedId);
        return this;
    }

    /**
     * 氏名を設定
     * 
     * @param name 氏名（必須）
     * @return このビルダーインスタンス
     */
    public EngineerBuilder setName(String name) {
        engineer.setName(name);
        return this;
    }

    /**
     * フリガナを設定
     * 
     * @param nameKana フリガナ（必須）
     * @return このビルダーインスタンス
     */
    public EngineerBuilder setNameKana(String nameKana) {
        engineer.setNameKana(nameKana);
        return this;
    }

    /**
     * 生年月日を設定
     * 
     * @param birthDate 生年月日（必須）
     * @return このビルダーインスタンス
     */
    public EngineerBuilder setBirthDate(LocalDate birthDate) {
        engineer.setBirthDate(birthDate);
        return this;
    }

    /**
     * 入社年月を設定
     * 
     * @param joinDate 入社年月（必須）
     * @return このビルダーインスタンス
     */
    public EngineerBuilder setJoinDate(LocalDate joinDate) {
        engineer.setJoinDate(joinDate);
        return this;
    }

    /**
     * エンジニア歴を設定
     * 
     * @param career エンジニア歴（必須）
     * @return このビルダーインスタンス
     */
    public EngineerBuilder setCareer(int career) {
        engineer.setCareer(career);
        return this;
    }

    /**
     * プログラミング言語リストを設定
     * 
     * @param languages プログラミング言語のリスト（必須）
     * @return このビルダーインスタンス
     */
    public EngineerBuilder setProgrammingLanguages(List<String> languages) {
        engineer.setProgrammingLanguages(languages);
        return this;
    }

    /**
     * 経歴を設定
     * 
     * @param history 経歴（任意）
     * @return このビルダーインスタンス
     */
    public EngineerBuilder setCareerHistory(String history) {
        engineer.setCareerHistory(history);
        return this;
    }

    /**
     * 研修の受講歴を設定
     * 
     * @param history 研修の受講歴（任意）
     * @return このビルダーインスタンス
     */
    public EngineerBuilder setTrainingHistory(String history) {
        engineer.setTrainingHistory(history);
        return this;
    }

    /**
     * 技術力を設定
     * 
     * @param skill 技術力（1.0-5.0の評価、任意）
     * @return このビルダーインスタンス
     * @throws IllegalArgumentException 値が範囲外の場合
     */
    public EngineerBuilder setTechnicalSkill(Double skill) {
        // nullの場合はそのまま設定
        if (skill == null) {
            engineer.setTechnicalSkill(null);
            return this;
        }

        // 値がある場合は範囲チェック
        if (skill < 1.0 || skill > 5.0) {
            throw new IllegalArgumentException("技術力は1.0から5.0の範囲で設定してください");
        }
        engineer.setTechnicalSkill(skill);
        return this;
    }

    /**
     * 受講態度を設定
     * 
     * @param attitude 受講態度（1.0-5.0の評価、任意）
     * @return このビルダーインスタンス
     * @throws IllegalArgumentException 値が範囲外の場合
     */
    public EngineerBuilder setLearningAttitude(Double attitude) {
        // nullの場合はそのまま設定
        if (attitude == null) {
            engineer.setLearningAttitude(null);
            return this;
        }

        // 値がある場合は範囲チェック
        if (attitude < 1.0 || attitude > 5.0) {
            throw new IllegalArgumentException("受講態度は1.0から5.0の範囲で設定してください");
        }
        engineer.setLearningAttitude(attitude);
        return this;
    }

    /**
     * コミュニケーション能力を設定
     * 
     * @param skill コミュニケーション能力（1.0-5.0の評価、任意）
     * @return このビルダーインスタンス
     * @throws IllegalArgumentException 値が範囲外の場合
     */
    public EngineerBuilder setCommunicationSkill(Double skill) {
        // nullの場合はそのまま設定
        if (skill == null) {
            engineer.setCommunicationSkill(null);
            return this;
        }

        // 値がある場合は範囲チェック
        if (skill < 1.0 || skill > 5.0) {
            throw new IllegalArgumentException("コミュニケーション能力は1.0から5.0の範囲で設定してください");
        }
        engineer.setCommunicationSkill(skill);
        return this;
    }

    /**
     * リーダーシップを設定
     * 
     * @param leadership リーダーシップ（1.0-5.0の評価、任意）
     * @return このビルダーインスタンス
     * @throws IllegalArgumentException 値が範囲外の場合
     */
    public EngineerBuilder setLeadership(Double leadership) {
        // nullの場合はそのまま設定
        if (leadership == null) {
            engineer.setLeadership(null);
            return this;
        }

        // 値がある場合は範囲チェック
        if (leadership < 1.0 || leadership > 5.0) {
            throw new IllegalArgumentException("リーダーシップは1.0から5.0の範囲で設定してください");
        }
        engineer.setLeadership(leadership);
        return this;
    }

    /**
     * 備考を設定
     * 
     * @param note 備考（任意）
     * @return このビルダーインスタンス
     */
    public EngineerBuilder setNote(String note) {
        engineer.setNote(note);
        return this;
    }

    /**
     * 登録日時を設定
     * 通常、システムが自動的に現在日付を設定するため、このメソッドは主にテストやインポート用
     * 
     * @param date 登録日時
     * @return このビルダーインスタンス
     */
    public EngineerBuilder setRegisteredDate(LocalDate date) {
        engineer.setRegisteredDate(date);
        return this;
    }
}
