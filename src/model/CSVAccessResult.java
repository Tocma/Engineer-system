package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import util.Constants.SystemConstants;

/**
 * CSVアクセス処理の結果を保持するクラス
 * 読み込んだデータとエラー情報を管理し、インポート分析機能を提供
 *
 * このクラスは、CSVファイル読み込み処理の結果を保持し、
 * 正常に読み込まれたデータとエラーが発生したデータを区別して管理
 *
 * @author Nakano
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

    // === 新機能: インポート分析関連のフィールド ===

    /** 現在のデータ件数（分析用） */
    private Integer currentDataCount;

    /** 新規追加されるデータ件数（分析結果） */
    private Integer calculatedNewDataCount;

    /** 上書きされるデータ件数（分析結果） */
    private Integer calculatedOverwriteDataCount;

    /** スキップされるデータ件数（分析結果） */
    private Integer calculatedSkipDataCount;

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
        // 分析関連フィールドの初期化
        initializeAnalysisFields();
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
        // 分析関連フィールドの初期化
        initializeAnalysisFields();
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
        // 分析関連フィールドの初期化
        initializeAnalysisFields();
    }

    /**
     * 分析関連フィールドの初期化
     * コンストラクタから呼び出される共通の初期化処理
     */
    private void initializeAnalysisFields() {
        this.currentDataCount = null;
        this.calculatedNewDataCount = null;
        this.calculatedOverwriteDataCount = null;
        this.calculatedSkipDataCount = null;
    }

    // === ゲッター・セッターメソッド群 ===

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
     * ファイル内重複IDチェック
     * 取り込みファイル内で同一IDが複数存在するかをチェック
     * 
     * @return ファイル内重複IDのリスト（重複がない場合は空のリスト）
     */
    public List<String> checkInternalDuplicateIds() {
        List<String> internalDuplicateIds = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (EngineerDTO engineer : successData) {
            String id = engineer.getId();
            if (id != null && !id.isEmpty()) {
                if (seenIds.contains(id)) {
                    if (!internalDuplicateIds.contains(id)) {
                        internalDuplicateIds.add(id);
                    }
                } else {
                    seenIds.add(id);
                }
            }
        }

        return internalDuplicateIds;
    }

    /**
     * ファイル内重複IDが存在するかを確認
     * 
     * @return ファイル内重複IDがある場合はtrue
     */
    public boolean hasInternalDuplicateIds() {
        return !checkInternalDuplicateIds().isEmpty();
    }

    /**
     * すべての問題（エラーおよび重複ID）があるかどうかを確認
     * 
     * @return エラーまたは重複IDがある場合はtrue
     */
    public boolean hasAnyIssue() {
        return hasError() || hasDuplicateIds();
    }

    // === インポート分析メソッド群 ===

    /**
     * 現在のデータと比較してインポート分析を実行
     * このメソッドが分析の中心的な処理
     * 
     * @param currentEngineers 現在システムに登録されているエンジニアデータ
     */
    public void performImportAnalysis(List<EngineerDTO> currentEngineers) {
        if (currentEngineers == null) {
            throw new IllegalArgumentException("現在のエンジニアデータがnullです");
        }

        // 現在のデータ件数を記録
        this.currentDataCount = currentEngineers.size();

        // 現在のデータのIDセットを作成（高速な重複チェックのため）
        Set<String> currentIds = currentEngineers.stream()
                .map(EngineerDTO::getId)
                .collect(Collectors.toSet());

        // 各カテゴリの件数を初期化
        int newDataCount = 0;
        int overwriteDataCount = 0;
        int skipDataCount = 0;

        // 取り込み予定の各データについて分析
        for (EngineerDTO imported : successData) {
            String importedId = imported.getId();

            if (currentIds.contains(importedId)) {
                // 重複IDが存在する場合の処理判定
                if (overwriteConfirmed) {
                    overwriteDataCount++; // 上書き処理（総件数に影響なし）
                } else {
                    skipDataCount++; // スキップ処理（総件数に影響なし）
                }
            } else {
                // 新規IDの場合
                newDataCount++; // 新規追加（総件数が増加）
            }
        }

        // 分析結果を保存
        this.calculatedNewDataCount = newDataCount;
        this.calculatedOverwriteDataCount = overwriteDataCount;
        this.calculatedSkipDataCount = skipDataCount;
    }

    /**
     * 分析が実行済みかどうかを確認
     * 
     * @return 分析が実行済みの場合はtrue
     */
    public boolean isAnalysisPerformed() {
        return currentDataCount != null && calculatedNewDataCount != null;
    }

    /**
     * 分析実行を強制確認するヘルパーメソッド
     * 分析が未実行の場合は例外を投げます
     */
    private void ensureAnalysisPerformed() {
        if (!isAnalysisPerformed()) {
            throw new IllegalStateException("インポート分析が実行されていません。performImportAnalysis()を先に呼び出してください。");
        }
    }

    /**
     * 現在のデータ件数を取得（分析結果）
     * 
     * @return 現在のデータ件数
     * @throws IllegalStateException 分析が未実行の場合
     */
    public int getCurrentDataCount() {
        ensureAnalysisPerformed();
        return currentDataCount;
    }

    /**
     * 新規追加されるデータ件数を取得（分析結果）
     * 
     * @return 新規追加データ件数
     * @throws IllegalStateException 分析が未実行の場合
     */
    public int getCalculatedNewDataCount() {
        ensureAnalysisPerformed();
        return calculatedNewDataCount;
    }

    /**
     * 上書きされるデータ件数を取得（分析結果）
     * 
     * @return 上書きデータ件数
     * @throws IllegalStateException 分析が未実行の場合
     */
    public int getCalculatedOverwriteDataCount() {
        ensureAnalysisPerformed();
        return calculatedOverwriteDataCount;
    }

    /**
     * スキップされるデータ件数を取得（分析結果）
     * 
     * @return スキップデータ件数
     * @throws IllegalStateException 分析が未実行の場合
     */
    public int getCalculatedSkipDataCount() {
        ensureAnalysisPerformed();
        return calculatedSkipDataCount;
    }

    /**
     * 処理後の総データ件数を計算して取得
     * 重要: 上書き処理は総件数に影響しないため、新規追加分のみを加算
     * 
     * @return 処理後の総データ件数
     * @throws IllegalStateException 分析が未実行の場合
     */
    public int getCalculatedFinalDataCount() {
        ensureAnalysisPerformed();
        return currentDataCount + calculatedNewDataCount;
    }

    /**
     * 処理後の総データ件数が上限を超えるかどうかを判定
     * 
     * @return 上限を超える場合はtrue
     * @throws IllegalStateException 分析が未実行の場合
     */
    public boolean willExceedLimit() {
        return getCalculatedFinalDataCount() > SystemConstants.MAX_ENGINEER_RECORDS;
    }

    /**
     * 上限を超過する件数を取得
     * 
     * @return 上限を超過する件数（0以下の場合は上限内）
     * @throws IllegalStateException 分析が未実行の場合
     */
    public int getExcessCount() {
        return getCalculatedFinalDataCount() - SystemConstants.MAX_ENGINEER_RECORDS;
    }

    /**
     * 分析結果の詳細情報を文字列で取得
     * ログ出力やデバッグ用の詳細情報を提供
     * 
     * @return 分析結果の詳細文字列
     * @throws IllegalStateException 分析が未実行の場合
     */
    public String getAnalysisDetailInfo() {
        ensureAnalysisPerformed();
        return String.format(
                "現在: %d件, 取込: %d件, 新規: %d件, 上書: %d件, スキップ: %d件, 処理後: %d件",
                currentDataCount, successData.size(), calculatedNewDataCount,
                calculatedOverwriteDataCount, calculatedSkipDataCount, getCalculatedFinalDataCount());
    }

    /**
     * ユーザー向けの詳細な上限エラーメッセージを構築
     * ユーザーが状況を理解しやすいメッセージを生成
     * 
     * @return 詳細なエラーメッセージ
     * @throws IllegalStateException 分析が未実行の場合
     */
    public String buildDetailedLimitErrorMessage() {
        ensureAnalysisPerformed();

        StringBuilder message = new StringBuilder();

        message.append("インポートすると登録件数の上限を超えます。\n\n");

        // 現状の詳細情報
        message.append("【現在の状況】\n");
        message.append(String.format("・現在登録済み: %,d件\n", currentDataCount));
        message.append(String.format("・取り込み予定: %,d件\n", successData.size()));
        message.append(String.format("・上限件数: %,d件\n\n", SystemConstants.MAX_ENGINEER_RECORDS));

        // 処理内容の詳細
        message.append("【処理内容の内訳】\n");
        message.append(String.format("・新規追加: %,d件\n", calculatedNewDataCount));
        if (calculatedOverwriteDataCount > 0) {
            message.append(String.format("・上書き更新: %,d件（件数に影響なし）\n", calculatedOverwriteDataCount));
        }
        if (calculatedSkipDataCount > 0) {
            message.append(String.format("・重複スキップ: %,d件（件数に影響なし）\n", calculatedSkipDataCount));
        }
        message.append(String.format("・処理後総件数: %,d件\n\n", getCalculatedFinalDataCount()));

        // 対処方法の提案
        int excessCount = getExcessCount();
        message.append("【対処方法】\n");
        message.append(String.format("・最低 %,d件のデータを削除してから再実行してください\n", excessCount));
        message.append("・または、取り込みデータを分割してインポートしてください");

        return message.toString();
    }

    /**
     * ユーザー向けの詳細なインポート完了メッセージを構築
     * 処理結果をユーザーが理解しやすい形で提供
     * 
     * @return 詳細な完了メッセージ
     * @throws IllegalStateException 分析が未実行の場合
     */
    public String buildDetailedCompletionMessage() {
        ensureAnalysisPerformed();

        StringBuilder message = new StringBuilder();
        message.append("CSVファイルのインポートが完了しました。\n\n");

        // 処理結果の詳細
        message.append("【処理結果】\n");
        message.append(String.format("・新規追加: %,d件\n", calculatedNewDataCount));

        if (calculatedOverwriteDataCount > 0) {
            message.append(String.format("・上書き更新: %,d件\n", calculatedOverwriteDataCount));
        }

        if (calculatedSkipDataCount > 0) {
            message.append(String.format("・重複スキップ: %,d件\n", calculatedSkipDataCount));
        }

        if (getErrorCount() > 0) {
            message.append(String.format("・エラー: %,d件\n", getErrorCount()));
        }

        message.append(String.format("\n・総データ件数: %,d件", getCalculatedFinalDataCount()));

        return message.toString();
    }

    /**
     * 処理結果の概要を文字列で取得
     * 分析結果も含めた包括的な情報を提供
     * 
     * @return 処理結果の概要文字列
     */
    @Override
    public String toString() {
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("CSVAccessResult{");
        resultBuilder.append("successCount=").append(getSuccessCount());
        resultBuilder.append(", errorCount=").append(getErrorCount());
        resultBuilder.append(", duplicateIdCount=").append(getDuplicateIdCount());
        resultBuilder.append(", fatalError=").append(fatalError);
        if (errorMessage != null) {
            resultBuilder.append(", errorMessage='").append(errorMessage).append('\'');
        }
        resultBuilder.append(", overwriteConfirmed=").append(overwriteConfirmed);

        // 分析結果が利用可能な場合は追加
        if (isAnalysisPerformed()) {
            resultBuilder.append(", analysisInfo='").append(getAnalysisDetailInfo()).append('\'');
        }

        resultBuilder.append('}');
        return resultBuilder.toString();
    }
}