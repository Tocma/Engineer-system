package model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * エンジニア情報を保持するデータ転送オブジェクト（DTO）
 * エンジニアの基本情報、スキル情報、経歴情報などを管理
 * 
 * <p>
 * 主要フィールド：
 * <ul>
 * <li>id - 社員ID（必須）</li>
 * <li>name - 氏名（必須）</li>
 * <li>nameKana - フリガナ（必須）</li>
 * <li>birthDate - 生年月日（必須）</li>
 * <li>joinDate - 入社年月（必須）</li>
 * <li>career - エンジニア歴（必須）</li>
 * <li>programmingLanguages - 扱える言語（必須）</li>
 * <li>careerHistory - 経歴</li>
 * <li>trainingHistory - 研修の受講歴</li>
 * <li>technicalSkill - 技術力</li>
 * <li>learningAttitude - 受講態度</li>
 * <li>communicationSkill - コミュニケーション能力</li>
 * <li>leadership - リーダーシップ</li>
 * <li>note - 備考</li>
 * <li>registeredDate - 登録日時</li>
 * </ul>
 * </p>
 * 
 * @author Nakano
 * @version 4.2.2
 * @since 2025-04-25
 */
public class EngineerDTO {
    // 必須フィールド
    private String id; // 社員ID
    private String name; // 氏名
    private String nameKana; // フリガナ
    private LocalDate birthDate; // 生年月日
    private LocalDate joinDate; // 入社年月
    private int career; // エンジニア歴をintに変更
    private List<String> programmingLanguages; // 扱える言語

    // 任意フィールド
    private String careerHistory; // 経歴
    private String trainingHistory; // 研修の受講歴
    private Double technicalSkill; // 技術力
    private Double learningAttitude; // 受講態度
    private Double communicationSkill; // コミュニケーション能力
    private Double leadership; // リーダーシップ
    private String note; // 備考

    // システム管理用フィールド
    private LocalDate registeredDate; // 登録日時

    /**
     * デフォルトコンストラクタ
     * 登録日時は現在日付で初期化
     */
    public EngineerDTO() {
        this.registeredDate = LocalDate.now();
    }

    // ゲッターとセッター（各フィールドに対して実装）

    /**
     * 社員IDを取得
     * 
     * @return 社員ID
     */
    public String getId() {
        return id;
    }

    /**
     * 社員IDを設定
     * 
     * @param id 社員ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 氏名を取得
     * 
     * @return 氏名
     */
    public String getName() {
        return name;
    }

    /**
     * 氏名を設定
     * 
     * @param name 氏名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * フリガナを取得
     * 
     * @return フリガナ
     */
    public String getNameKana() {
        return nameKana;
    }

    /**
     * フリガナを設定
     * 
     * @param nameKana フリガナ
     */
    public void setNameKana(String nameKana) {
        this.nameKana = nameKana;
    }

    /**
     * 生年月日を取得
     * 
     * @return 生年月日
     */
    public LocalDate getBirthDate() {
        // LocalDateはイミュータブルなので、防御的コピーは不要
        return birthDate;
    }

    /**
     * 生年月日を設定
     * 
     * @param birthDate 生年月日
     */
    public void setBirthDate(LocalDate birthDate) {
        // LocalDateはイミュータブルなので、防御的コピーは不要
        this.birthDate = birthDate;
    }

    /**
     * 入社年月を取得
     * 
     * @return 入社年月
     */
    public LocalDate getJoinDate() {
        return joinDate;
    }

    /**
     * 入社年月を設定
     * 
     * @param joinDate 入社年月
     */
    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    /**
     * エンジニア歴を取得
     * 
     * @return エンジニア歴（年数）
     */
    public int getCareer() {
        return career;
    }

    /**
     * エンジニア歴を設定
     * 
     * @param career エンジニア歴（年数）
     */
    public void setCareer(int career) {
        this.career = career;
    }

    /**
     * プログラミング言語リストを取得
     * 
     * @return プログラミング言語のリスト
     */
    public List<String> getProgrammingLanguages() {
        return programmingLanguages;
    }

    /**
     * プログラミング言語リストを設定
     * 
     * @param programmingLanguages プログラミング言語のリスト
     */
    public void setProgrammingLanguages(List<String> programmingLanguages) {
        this.programmingLanguages = programmingLanguages;
    }

    /**
     * 経歴を取得
     * 
     * @return 経歴
     */
    public String getCareerHistory() {
        return careerHistory;
    }

    /**
     * 経歴を設定
     * 
     * @param careerHistory 経歴
     */
    public void setCareerHistory(String careerHistory) {
        this.careerHistory = careerHistory;
    }

    /**
     * 研修の受講歴を取得
     * 
     * @return 研修の受講歴
     */
    public String getTrainingHistory() {
        return trainingHistory;
    }

    /**
     * 研修の受講歴を設定
     * 
     * @param trainingHistory 研修の受講歴
     */
    public void setTrainingHistory(String trainingHistory) {
        this.trainingHistory = trainingHistory;
    }

    /**
     * 技術力を取得
     * 
     * @return 技術力（1.0-5.0の評価）
     */
    public Double getTechnicalSkill() {
        return technicalSkill;
    }

    /**
     * 技術力を設定
     * 
     * @param technicalSkill 技術力（1.0-5.0の評価）
     */
    public void setTechnicalSkill(Double technicalSkill) {
        this.technicalSkill = technicalSkill;
    }

    /**
     * 受講態度を取得
     * 
     * @return 受講態度（1.0-5.0の評価）
     */
    public Double getLearningAttitude() {
        return learningAttitude;
    }

    /**
     * 受講態度を設定
     * 
     * @param learningAttitude 受講態度（1.0-5.0の評価）
     */
    public void setLearningAttitude(Double learningAttitude) {
        this.learningAttitude = learningAttitude;
    }

    /**
     * コミュニケーション能力を取得
     * 
     * @return コミュニケーション能力（1.0-5.0の評価）
     */
    public Double getCommunicationSkill() {
        return communicationSkill;
    }

    /**
     * コミュニケーション能力を設定
     * 
     * @param communicationSkill コミュニケーション能力（1.0-5.0の評価）
     */
    public void setCommunicationSkill(Double communicationSkill) {
        this.communicationSkill = communicationSkill;
    }

    /**
     * リーダーシップを取得
     * 
     * @return リーダーシップ（1.0-5.0の評価）
     */
    public Double getLeadership() {
        return leadership;
    }

    /**
     * リーダーシップを設定
     * 
     * @param leadership リーダーシップ（1.0-5.0の評価）
     */
    public void setLeadership(Double leadership) {
        this.leadership = leadership;
    }

    /**
     * 備考を取得
     * 
     * @return 備考
     */
    public String getNote() {
        return note;
    }

    /**
     * 備考を設定
     * 
     * @param note 備考
     */
    public void setNote(String note) {
        this.note = note;
    }

    /**
     * 登録日時を取得
     * 
     * @return 登録日時
     */
    public LocalDate getRegisteredDate() {
        return registeredDate;
    }

    /**
     * 登録日時を設定
     * 
     * @param registeredDate 登録日時
     */
    public void setRegisteredDate(LocalDate registeredDate) {
        this.registeredDate = registeredDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EngineerDTO that = (EngineerDTO) o;
        return career == that.career &&
                Double.compare(that.technicalSkill, technicalSkill) == 0 &&
                Double.compare(that.learningAttitude, learningAttitude) == 0 &&
                Double.compare(that.communicationSkill, communicationSkill) == 0 &&
                Double.compare(that.leadership, leadership) == 0 &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(nameKana, that.nameKana) &&
                Objects.equals(birthDate, that.birthDate) &&
                Objects.equals(joinDate, that.joinDate) &&
                Objects.equals(programmingLanguages, that.programmingLanguages) &&
                Objects.equals(careerHistory, that.careerHistory) &&
                Objects.equals(trainingHistory, that.trainingHistory) &&
                Objects.equals(note, that.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, nameKana, birthDate, joinDate, career, programmingLanguages,
                careerHistory, trainingHistory, technicalSkill, learningAttitude,
                communicationSkill, leadership, note);
    }

    @Override
    public String toString() {
        return "EngineerDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", nameKana='" + nameKana + '\'' +
                ", birthDate=" + birthDate +
                ", joinDate=" + joinDate +
                ", career=" + career +
                ", programmingLanguages=" + programmingLanguages +
                ", careerHistory='" + careerHistory + '\'' +
                ", trainingHistory='" + trainingHistory + '\'' +
                ", technicalSkill=" + technicalSkill +
                ", learningAttitude=" + learningAttitude +
                ", communicationSkill=" + communicationSkill +
                ", leadership=" + leadership +
                ", note='" + note + '\'' +
                ", registeredDate=" + registeredDate +
                '}';
    }
}
