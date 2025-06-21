// src/service/EngineerSearchService.java
package service;

import model.EngineerDTO;
import model.EngineerDAO;
import util.LogHandler;
import util.LogHandler.LogType;
import controller.MainController.SearchCriteria;
import controller.MainController.SearchResult;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * エンジニア検索サービス
 * 検索に関するビジネスロジックを担当
 * 
 * @author Nakano
 */
public class EngineerSearchService {

    private final EngineerDAO engineerDAO;

    public EngineerSearchService(EngineerDAO engineerDAO) {
        this.engineerDAO = engineerDAO;
    }

    /**
     * エンジニア検索処理
     * 
     * @param searchCriteria 検索条件
     * @return 検索結果
     */
    public SearchResult searchEngineers(SearchCriteria searchCriteria) {
        try {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "エンジニア検索処理を開始します");

            // 入力検証の実行
            List<String> validationErrors = validateSearchCriteria(searchCriteria);
            if (!validationErrors.isEmpty()) {
                LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                        "検索条件の検証でエラーが発生: " + String.join(", ", validationErrors));
                return new SearchResult(new ArrayList<>(), validationErrors);
            }

            // データ取得と検索実行
            List<EngineerDTO> allEngineers = engineerDAO.findAll();
            List<EngineerDTO> filteredEngineers = filterEngineersByCriteria(allEngineers, searchCriteria);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("検索実行完了: %d件のデータがヒット（全%d件中）",
                            filteredEngineers.size(), allEngineers.size()));

            return new SearchResult(filteredEngineers, new ArrayList<>());

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "検索処理中にエラーが発生", e);
            return new SearchResult(new ArrayList<>(), List.of("検索処理中にエラーが発生: " + e.getMessage()));
        }
    }

    /**
     * 検索条件の妥当性検証
     */
    private List<String> validateSearchCriteria(SearchCriteria criteria) {
        List<String> errors = new ArrayList<>();

        // 社員ID検証
        if (criteria.getId() != null && !criteria.getId().trim().isEmpty()) {
            String id = criteria.getId().trim();
            String convertedId = id
                    .replace("０", "0").replace("１", "1").replace("２", "2")
                    .replace("３", "3").replace("４", "4").replace("５", "5")
                    .replace("６", "6").replace("７", "7").replace("８", "8")
                    .replace("９", "9");
            if (!convertedId.matches("^[0-9]{1,5}$")) {
                errors.add("社員IDは5桁以内の数字で入力してください");
            }
        }

        // 氏名検証
        if (criteria.getName() != null && !criteria.getName().trim().isEmpty()) {
            String name = criteria.getName().trim().replaceAll("\\s{2,}", " ");
            if (name.length() > 20) {
                errors.add("氏名は20文字以内で入力してください");
            }
            if (!name.matches("[ぁ-んァ-ヶ一-龯々〆〤ー\\s]*")) {
                errors.add("氏名は日本語のみで入力してください");
            }
        }

        // 生年月日検証
        boolean hasYear = criteria.getYear() != null && !criteria.getYear().isEmpty();
        boolean hasMonth = criteria.getMonth() != null && !criteria.getMonth().isEmpty();
        boolean hasDay = criteria.getDay() != null && !criteria.getDay().isEmpty();

        if ((hasYear || hasMonth || hasDay) && !(hasYear && hasMonth && hasDay)) {
            errors.add("生年月日は年・月・日すべてを選択してください");
        }

        // エンジニア歴検証
        if (criteria.getCareer() != null && !criteria.getCareer().trim().isEmpty()) {
            try {
                int careerValue = Integer.parseInt(criteria.getCareer());
                if (careerValue < 0 || careerValue > 50) {
                    errors.add("エンジニア歴は0年から50年の範囲で入力してください");
                }
            } catch (NumberFormatException e) {
                errors.add("エンジニア歴は数値で入力してください");
            }
        }

        return errors;
    }

    /**
     * 検索条件によるフィルタリング
     */
    private List<EngineerDTO> filterEngineersByCriteria(List<EngineerDTO> engineers, SearchCriteria criteria) {
        return engineers.stream()
                .filter(engineer -> matchesCriteria(engineer, criteria))
                .collect(Collectors.toList());
    }

    /**
     * エンジニアが検索条件にマッチするかチェック
     */
    private boolean matchesCriteria(EngineerDTO engineer, SearchCriteria criteria) {
        // 社員ID検索
        if (criteria.getId() != null && !criteria.getId().trim().isEmpty()) {
            if (engineer.getId() == null ||
                    !engineer.getId().toLowerCase().contains(criteria.getId().toLowerCase())) {
                return false;
            }
        }

        // 氏名検索
        if (criteria.getName() != null && !criteria.getName().trim().isEmpty()) {
            if (engineer.getName() == null ||
                    !engineer.getName().toLowerCase().contains(criteria.getName().toLowerCase())) {
                return false;
            }
        }

        // 生年月日検索
        if (engineer.getBirthDate() != null && criteria.hasDateCriteria()) {
            String birthDateStr = engineer.getBirthDate().toString();

            if (!criteria.getYear().isEmpty() && !birthDateStr.startsWith(criteria.getYear())) {
                return false;
            }

            if (!criteria.getMonth().isEmpty()) {
                String monthPart = criteria.getMonth().length() == 1 ? "0" + criteria.getMonth() : criteria.getMonth();
                if (!birthDateStr.substring(5, 7).equals(monthPart)) {
                    return false;
                }
            }

            if (!criteria.getDay().isEmpty()) {
                String dayPart = criteria.getDay().length() == 1 ? "0" + criteria.getDay() : criteria.getDay();
                if (!birthDateStr.substring(8, 10).equals(dayPart)) {
                    return false;
                }
            }
        }

        // エンジニア歴検索
        if (criteria.getCareer() != null && !criteria.getCareer().trim().isEmpty()) {
            try {
                int searchCareer = Integer.parseInt(criteria.getCareer());
                if (engineer.getCareer() != searchCareer) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }
}