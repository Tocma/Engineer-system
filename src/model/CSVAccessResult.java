package model;

import java.util.ArrayList;
import java.util.List;

/**
 * CSVアクセス処理の結果を保持するクラス
 * 読み込んだデータとエラー情報を管理
 *
 * <p>
 * このクラスは、CSVファイル読み込み処理の結果を保持し、
 * 正常に読み込まれたデータとエラーが発生したデータを区別して管理します。
 * また、重複IDの検出と上書き確認のプロセスもサポートします。
 * </p>
 *
 * @author Nakano
 * @version 4.0.0
 * @since 2025-04-15
 */
public class CSVAccessResult {

    /** 正常に読み込まれたエンジニアデータ */
    private final List<EngineerDTO> successData;

    /** エラーが発生したエンジニアデータ */
    private final List<EngineerDTO> errorData;

    /** 致命的エラーフラグ */
    private final boolean fatalError;

    /** エラーメッセージ */
    private final String errorMessage;

    /** 重複IDのリスト */
    private final List<String> duplicateIds;

    /** 上書き確認済みフラグ */
    private boolean overwriteConfirmed;

    /**
     * 成功結果用コンストラクタ
     * 
     * @param successData 正常に読み込まれたデータ
     */
    public CSVAccessResult(List<EngineerDTO> successData) {
        this.successData = successData;
        this.errorData = new ArrayList<>();
        this.fatalError = false;
        this.errorMessage = null;
        this.duplicateIds = new ArrayList<>();
        this.overwriteConfirmed = false;
    }

    /**
     * エラー結果用コンストラクタ
     * 
     * @param successData  正常に読み込まれたデータ
     * @param errorData    エラーが発生したデータ
     * @param fatalError   致命的エラーフラグ
     * @param errorMessage エラーメッセージ
     */
    public CSVAccessResult(List<EngineerDTO> successData, List<EngineerDTO> errorData,
            boolean fatalError, String errorMessage) {
        this.successData = successData != null ? successData : new ArrayList<>();
        this.errorData = errorData != null ? errorData : new ArrayList<>();
        this.fatalError = fatalError;
        this.errorMessage = errorMessage;
        this.duplicateIds = new ArrayList<>();
        this.overwriteConfirmed = false;
    }

    /**
     * 重複ID情報を含む結果用コンストラクタ
     * 
     * @param successData  正常に読み込まれたデータ
     * @param errorData    エラーが発生したデータ
     * @param duplicateIds 重複IDのリスト
     * @param fatalError   致命的エラーフラグ
     * @param errorMessage エラーメッセージ
     */
    public CSVAccessResult(List<EngineerDTO> successData, List<EngineerDTO> errorData,
            List<String> duplicateIds, boolean fatalError, String errorMessage) {
        this.successData = successData != null ? successData : new ArrayList<>();
        this.errorData = errorData != null ? errorData : new ArrayList<>();
        this.duplicateIds = duplicateIds != null ? duplicateIds : new ArrayList<>();
        this.fatalError = fatalError;
        this.errorMessage = errorMessage;
        this.overwriteConfirmed = false;
    }

    /**
     * 正常に読み込まれたデータを取得
     * 
     * @return 正常に読み込まれたエンジニアデータのリスト
     */
    public List<EngineerDTO> getSuccessData() {
        return successData;
    }

    /**
     * エラーが発生したデータを取得
     * 
     * @return エラーが発生したエンジニアデータのリスト
     */
    public List<EngineerDTO> getErrorData() {
        return errorData;
    }

    /**
     * 致命的エラーが発生したかどうかを取得
     * 
     * @return 致命的エラーが発生した場合true
     */
    public boolean isFatalError() {
        return fatalError;
    }

    /**
     * エラーメッセージを取得
     * 
     * @return エラーメッセージ
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * エラーがあるかどうかを取得
     * 
     * @return エラーデータがある場合、または致命的エラーの場合true
     */
    public boolean hasError() {
        return fatalError || !errorData.isEmpty();
    }

    /**
     * 正常データの件数を取得
     * 
     * @return 正常データの件数
     */
    public int getSuccessCount() {
        return successData.size();
    }

    /**
     * エラーデータの件数を取得
     * 
     * @return エラーデータの件数
     */
    public int getErrorCount() {
        return errorData.size();
    }

    /**
     * 重複IDのリストを取得
     * 
     * @return 重複IDのリスト
     */
    public List<String> getDuplicateIds() {
        return duplicateIds;
    }

    /**
     * 重複IDを追加
     * 
     * @param id 追加する重複ID
     */
    public void addDuplicateId(String id) {
        if (id != null && !id.isEmpty() && !duplicateIds.contains(id)) {
            duplicateIds.add(id);
        }
    }

    /**
     * 複数の重複IDを追加
     * 
     * @param ids 追加する重複IDのリスト
     */
    public void addDuplicateIds(List<String> ids) {
        if (ids != null) {
            for (String id : ids) {
                addDuplicateId(id);
            }
        }
    }

    /**
     * 重複IDがあるかどうかを確認
     * 
     * @return 重複IDがある場合はtrue
     */
    public boolean hasDuplicateIds() {
        return !duplicateIds.isEmpty();
    }

    /**
     * 上書き確認済みかどうかを取得
     * 
     * @return 上書き確認済みの場合はtrue
     */
    public boolean isOverwriteConfirmed() {
        return overwriteConfirmed;
    }

    /**
     * 上書き確認済みフラグを設定
     * 
     * @param confirmed 上書き確認済みの場合はtrue
     */
    public void setOverwriteConfirmed(boolean confirmed) {
        this.overwriteConfirmed = confirmed;
    }

    /**
     * 重複ID数を取得
     * 
     * @return 重複IDの数
     */
    public int getDuplicateIdCount() {
        return duplicateIds.size();
    }

    /**
     * すべての問題（エラーおよび重複ID）があるかどうかを確認
     * 
     * @return エラーまたは重複IDがある場合はtrue
     */
    public boolean hasAnyIssue() {
        return hasError() || hasDuplicateIds();
    }

    /**
     * 処理結果の概要を文字列で取得
     * 
     * @return 処理結果の概要文字列
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CSVAccessResult{");
        sb.append("successCount=").append(getSuccessCount());
        sb.append(", errorCount=").append(getErrorCount());
        sb.append(", duplicateIdCount=").append(getDuplicateIdCount());
        sb.append(", fatalError=").append(fatalError);
        if (errorMessage != null) {
            sb.append(", errorMessage='").append(errorMessage).append('\'');
        }
        sb.append(", overwriteConfirmed=").append(overwriteConfirmed);
        sb.append('}');
        return sb.toString();
    }
}