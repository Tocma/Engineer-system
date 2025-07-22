package service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import controller.MainController.SearchCriteria;
import controller.MainController.SearchResult;
import model.EngineerDAO;
import model.EngineerDTO;
import util.LogHandler;
import util.LogHandler.LogType;
import util.validator.SearchValidationService;
import util.validator.SearchValidationService.SearchValidationResult;

/**
 * エンジニア検索サービス
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
            Thread.sleep(5000); // 5秒待機
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "検索処理中にエラーが発生", _e);
            return new SearchResult(new ArrayList<>(),
                    List.of("検索処理中にエラーが発生: " + _e.getMessage()));
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
            } catch (NumberFormatException _e) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "エンジニア歴の数値変換に失敗: " + searchCareer);
                return false;
            }
        }

        return true;
    }

    /**
     * 生年月日の検索条件があるかチェック（AND検索対応）
     * 年・月・日のいずれかが入力されていれば検索対象とする
     */
    private boolean hasDateCriteria(Map<String, String> processedCriteria) {
        return !processedCriteria.get("year").isEmpty() ||
                !processedCriteria.get("month").isEmpty() ||
                !processedCriteria.get("day").isEmpty();
    }

    /**
     * 生年月日のマッチング処理（AND検索対応）
     * 入力された項目すべてがマッチした場合のみtrueを返す
     * 
     * @param engineer          エンジニアDTO
     * @param processedCriteria 前処理済み検索条件
     * @return すべての入力項目がマッチした場合true
     */
    private boolean matchesBirthDate(EngineerDTO engineer, Map<String, String> processedCriteria) {
        try {
            LocalDate engineerBirthDate = engineer.getBirthDate();

            // 年の検索条件チェック
            String searchYear = processedCriteria.get("year");
            if (!searchYear.isEmpty()) {
                int targetYear = Integer.parseInt(searchYear);
                if (engineerBirthDate.getYear() != targetYear) {
                    return false;
                }
            }

            // 月の検索条件チェック
            String searchMonth = processedCriteria.get("month");
            if (!searchMonth.isEmpty()) {
                int targetMonth = Integer.parseInt(searchMonth);
                if (engineerBirthDate.getMonthValue() != targetMonth) {
                    return false;
                }
            }

            // 日の検索条件チェック
            String searchDay = processedCriteria.get("day");
            if (!searchDay.isEmpty()) {
                int targetDay = Integer.parseInt(searchDay);
                if (engineerBirthDate.getDayOfMonth() != targetDay) {
                    return false;
                }
            }

            return true;

        } catch (NumberFormatException _e) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "生年月日の数値変換に失敗: " + _e.getMessage());
            return false;
        } catch (Exception _e) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "生年月日の比較処理でエラーが発生: " + _e.getMessage());
            return false;
        }
    }

    /**
     * 検索可能な条件があるかチェック（AND検索対応）
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
                (hasNonEmptyValue(searchCriteria.getYear()) ||
                        hasNonEmptyValue(searchCriteria.getMonth()) ||
                        hasNonEmptyValue(searchCriteria.getDay()));
    }

    /**
     * 空でない値があるかチェック
     */
    private boolean hasNonEmptyValue(String value) {
        return value != null && !value.trim().isEmpty() && !"未選択".equals(value);
    }

    /**
     * 検索条件の要約を取得（ログ出力用）（AND検索対応）
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

        // 生年月日の個別要素をチェック（AND検索対応）
        List<String> dateConditions = new ArrayList<>();
        if (hasNonEmptyValue(searchCriteria.getYear())) {
            dateConditions.add(searchCriteria.getYear() + "年");
        }
        if (hasNonEmptyValue(searchCriteria.getMonth())) {
            dateConditions.add(searchCriteria.getMonth() + "月");
        }
        if (hasNonEmptyValue(searchCriteria.getDay())) {
            dateConditions.add(searchCriteria.getDay() + "日");
        }
        if (!dateConditions.isEmpty()) {
            conditions.add("生年月日: " + String.join("", dateConditions));
        }

        if (hasNonEmptyValue(searchCriteria.getCareer())) {
            conditions.add("エンジニア歴: " + searchCriteria.getCareer() + "年");
        }

        return conditions.isEmpty() ? "条件なし" : String.join(", ", conditions);
    }

}