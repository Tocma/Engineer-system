package model;

import java.time.LocalDate;
import java.util.List;
import util.IDValidator;

/**
 * EngineerDTOオブジェクトを生成するビルダークラス
 * Builderパターンを使用してEngineerDTOの段階的な構築を可能にする
 * 
 * <p>
 * このクラスは、必須項目の設定を強制し、任意項目は必要に応じて設定できるように
 * 設計されています。また、設定された値の簡易的な検証も行います。
 * </p>
 * 
 * <p>
 * 使用例：
 * </p>
 * 
 * <pre>
 * 
 * EngineerDTO engineer = new EngineerBuilder()
 *         .setId("ID00001")
 *         .setName("山田太郎")
 *         .setNameKana("ヤマダタロウ")
 *         .setBirthDate(LocalDate.of(1990, 1, 15))
 *         .setJoinDate(LocalDate.of(2020, 4, 1))
 *         .setCareer(5)
 *         .setProgrammingLanguages(Arrays.asList("Java", "Python"))
 *         .setTechnicalSkill(4.5)
 *         .build();
 * </pre>
 * 
 * @author Nakano
 * @version 4.0.0
 * @since 2025-04-15
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
            throw new IllegalStateException("エンジニア歴は0以上の値が必要です");
        }
        if (engineer.getProgrammingLanguages() == null || engineer.getProgrammingLanguages().isEmpty()) {
            throw new IllegalStateException("扱える言語は最低1つ設定する必要があります");
        }
    }

    /**
     * 社員IDを設定
     * 入力されたIDを標準形式に変換して設定します
     * 
     * @param id 社員ID
     */
    public EngineerBuilder setId(String id) {
        // IDを標準形式に変換
        String standardizedId = IDValidator.standardizeId(IDValidator.convertFullWidthToHalfWidth(id));
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
    public EngineerBuilder setTechnicalSkill(double skill) {
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
    public EngineerBuilder setLearningAttitude(double attitude) {
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
    public EngineerBuilder setCommunicationSkill(double skill) {
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
    public EngineerBuilder setLeadership(double leadership) {
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
