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
 * </p>
 *
 * @author Nakano
 * @version 2.1.0
 * @since 2025-04-03
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
}
