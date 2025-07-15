package service;

import model.EngineerDTO;
import model.EngineerDAO;
import util.LogHandler;
import util.LogHandler.LogType;
import util.validator.SearchValidationService;
import util.validator.SearchValidationService.SearchValidationResult;
import controller.MainController.SearchCriteria;
import controller.MainController.SearchResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.time.LocalDate;

/**
 * エンジニア検索サービス（強化版）
 * 検索に関するビジネスロジックを担当
 * 共通の前処理・バリデーション機能を統合
 * 
 * @author Nakano
 */
public class EngineerSearchService {

    private final EngineerDAO engineerDAO;
    private final SearchValidationService searchValidationService;

    public EngineerSearchService(EngineerDAO engineerDAO) {
        this.engineerDAO = engineerDAO;
        this.searchValidationService = SearchValidationService.getInstance();
    }

    /**
     * エンジニア検索処理
     * 共通の前処理・バリデーション機能を使用
     * 
     * @param searchCriteria 検索条件
     * @return 検索結果
     */
    public SearchResult searchEngineers(SearchCriteria searchCriteria) {
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "エンジニア検索処理を開始します");

            // 共通の前処理・バリデーション実行
            SearchValidationResult validationResult = searchValidationService
                    .validateAndPreprocessSearchCriteria(searchCriteria);

            if (!validationResult.isValid()) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "検索条件の検証でエラーが発生: " +
                                String.join(", ", validationResult.getErrorMessages()));
                return new SearchResult(new ArrayList<>(), validationResult.getErrorMessages());
            }

            // データ取得と検索実行
            List<EngineerDTO> allEngineers = engineerDAO.findAll();
            List<EngineerDTO> filteredEngineers = filterEngineersByCriteria(allEngineers,
                    validationResult.getProcessedValues());

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("検索実行完了: %d件のデータがヒット（全%d件中）",
                            filteredEngineers.size(), allEngineers.size()));

            return new SearchResult(filteredEngineers, new ArrayList<>());

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "検索処理中にエラーが発生", e);
            return new SearchResult(new ArrayList<>(),
                    List.of("検索処理中にエラーが発生: " + e.getMessage()));
        }
    }

    /**
     * エンジニアデータのフィルタリング処理（前処理済み値を使用）
     * 
     * @param allEngineers      全エンジニアデータ
     * @param processedCriteria 前処理済み検索条件
     * @return フィルタリング済みエンジニアリスト
     */
    private List<EngineerDTO> filterEngineersByCriteria(List<EngineerDTO> allEngineers,
            Map<String, String> processedCriteria) {

        return allEngineers.stream()
                .filter(engineer -> matchesCriteria(engineer, processedCriteria))
                .collect(Collectors.toList());
    }

    /**
     * エンジニアが検索条件にマッチするかチェック
     * 
     * @param engineer          エンジニアDTO
     * @param processedCriteria 前処理済み検索条件
     * @return マッチした場合true
     */
    private boolean matchesCriteria(EngineerDTO engineer, Map<String, String> processedCriteria) {
        // 社員IDでの検索
        String searchId = processedCriteria.get("id");
        if (!searchId.isEmpty()) {
            if (!engineer.getId().contains(searchId)) {
                return false;
            }
        }

        // 氏名での検索（部分一致、大文字小文字区別なし）
        String searchName = processedCriteria.get("name");
        if (!searchName.isEmpty()) {
            String engineerName = engineer.getName().toLowerCase();
            String searchNameLower = searchName.toLowerCase();
            if (!engineerName.contains(searchNameLower)) {
                return false;
            }
        }

        // 生年月日での検索（完全一致）
        if (hasDateCriteria(processedCriteria)) {
            if (!matchesBirthDate(engineer, processedCriteria)) {
                return false;
            }
        }

        // エンジニア歴での検索（完全一致）
        String searchCareer = processedCriteria.get("career");
        if (!searchCareer.isEmpty()) {
            try {
                int targetCareer = Integer.parseInt(searchCareer);
                if (engineer.getCareer() != targetCareer) {
                    return false;
                }
            } catch (NumberFormatException e) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "エンジニア歴の数値変換に失敗: " + searchCareer);
                return false;
            }
        }

        return true;
    }

    /**
     * 生年月日の検索条件があるかチェック
     */
    private boolean hasDateCriteria(Map<String, String> processedCriteria) {
        return !processedCriteria.get("year").isEmpty() &&
                !processedCriteria.get("month").isEmpty() &&
                !processedCriteria.get("day").isEmpty();
    }

    /**
     * 生年月日のマッチング処理
     * 
     * @param engineer          エンジニアDTO
     * @param processedCriteria 前処理済み検索条件
     * @return マッチした場合true
     */
    private boolean matchesBirthDate(EngineerDTO engineer, Map<String, String> processedCriteria) {
        try {
            LocalDate searchDate = LocalDate.of(
                    Integer.parseInt(processedCriteria.get("year")),
                    Integer.parseInt(processedCriteria.get("month")),
                    Integer.parseInt(processedCriteria.get("day")));

            return engineer.getBirthDate().equals(searchDate);

        } catch (Exception e) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "生年月日の比較処理でエラーが発生");
            return false;
        }
    }

    /**
     * 検索可能な条件があるかチェック
     * 
     * @param searchCriteria 検索条件
     * @return 検索可能な条件がある場合true
     */
    public boolean hasSearchableConditions(SearchCriteria searchCriteria) {
        if (searchCriteria == null) {
            return false;
        }

        return hasNonEmptyValue(searchCriteria.getId()) ||
                hasNonEmptyValue(searchCriteria.getName()) ||
                hasNonEmptyValue(searchCriteria.getCareer()) ||
                (hasNonEmptyValue(searchCriteria.getYear()) &&
                        hasNonEmptyValue(searchCriteria.getMonth()) &&
                        hasNonEmptyValue(searchCriteria.getDay()));
    }

    /**
     * 空でない値があるかチェック
     */
    private boolean hasNonEmptyValue(String value) {
        return value != null && !value.trim().isEmpty() && !"未選択".equals(value);
    }

    /**
     * 検索条件の要約を取得（ログ出力用）
     * 
     * @param searchCriteria 検索条件
     * @return 検索条件の要約文字列
     */
    public String getSummary(SearchCriteria searchCriteria) {
        List<String> conditions = new ArrayList<>();

        if (hasNonEmptyValue(searchCriteria.getId())) {
            conditions.add("社員ID: " + searchCriteria.getId());
        }
        if (hasNonEmptyValue(searchCriteria.getName())) {
            conditions.add("氏名: " + searchCriteria.getName());
        }
        if (hasNonEmptyValue(searchCriteria.getYear()) &&
                hasNonEmptyValue(searchCriteria.getMonth()) &&
                hasNonEmptyValue(searchCriteria.getDay())) {
            conditions.add(String.format("生年月日: %s年%s月%s日",
                    searchCriteria.getYear(),
                    searchCriteria.getMonth(),
                    searchCriteria.getDay()));
        }
        if (hasNonEmptyValue(searchCriteria.getCareer())) {
            conditions.add("エンジニア歴: " + searchCriteria.getCareer() + "年");
        }

        return conditions.isEmpty() ? "条件なし" : String.join(", ", conditions);
    }
}