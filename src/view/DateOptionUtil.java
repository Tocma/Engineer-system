package view;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 日付選択用コンボボックスの選択肢を生成するユーティリティクラス
 * 年、月、日、エンジニア歴、スキル評価の選択肢を統一的に提供
 * 
 * @author Nakano
 */
public final class DateOptionUtil {

    /** プライベートコンストラクタ（ユーティリティクラスのため） */
    private DateOptionUtil() {
        throw new AssertionError("ユーティリティクラスはインスタンス化できません");
    }

    /**
     * 年の選択肢を生成
     * 
     * @param startYear 開始年
     * @param endYear   終了年
     * @return 年の選択肢配列（先頭は空文字）
     */
    public static String[] getYearOptions(int startYear, int endYear) {
        List<String> years = new ArrayList<>();
        years.add(""); // 空の選択肢

        for (int year = startYear; year <= endYear; year++) {
            years.add(String.valueOf(year));
        }

        return years.toArray(new String[0]);
    }

    /**
     * 生年月日用の年選択肢を生成（1950年〜現在年）
     * 
     * @return 年の選択肢配列
     */
    public static String[] getBirthYearOptions() {
        return getYearOptions(1950, LocalDate.now().getYear());
    }

    /**
     * 入社年用の年選択肢を生成（1990年〜現在年）
     * 
     * @return 年の選択肢配列
     */
    public static String[] getJoinYearOptions() {
        return getYearOptions(1990, LocalDate.now().getYear());
    }

    /**
     * 検索用の年選択肢を生成（1940年〜現在年）
     * 
     * @return 年の選択肢配列
     */
    public static String[] getSearchYearOptions() {
        return getYearOptions(1940, LocalDate.now().getYear());
    }

    /**
     * 月の選択肢を生成
     * 
     * @return 月の選択肢配列（空文字 + 1-12月）
     */
    public static String[] getMonthOptions() {
        String[] months = new String[13]; // 空 + 1-12月
        months[0] = "";

        for (int i = 1; i <= 12; i++) {
            months[i] = String.valueOf(i);
        }

        return months;
    }

    /**
     * 日の選択肢を生成
     * 
     * @return 日の選択肢配列（空文字 + 1-31日）
     */
    public static String[] getDayOptions() {
        String[] days = new String[32]; // 空 + 1-31日
        days[0] = "";

        for (int i = 1; i <= 31; i++) {
            days[i] = String.valueOf(i);
        }

        return days;
    }

    /**
     * エンジニア歴の選択肢を生成
     * 
     * @return エンジニア歴の選択肢配列（空文字 + 0-50年）
     */
    public static String[] getCareerOptions() {
        String[] careers = new String[52]; // 空 + 0-50年
        careers[0] = "";

        for (int i = 0; i <= 50; i++) {
            careers[i + 1] = String.valueOf(i);
        }

        return careers;
    }

    /**
     * スキル評価の選択肢を生成（1.0-5.0、0.5刻み）
     * 
     * @return スキル評価の選択肢配列
     */
    public static String[] getSkillRatingOptions() {
        String[] ratings = new String[10]; // 空 + 1.0-5.0（0.5刻み）
        ratings[0] = "";

        double rating = 1.0;
        for (int i = 1; i < ratings.length; i++) {
            ratings[i] = String.valueOf(rating);
            rating += 0.5;
        }

        return ratings;
    }

    /**
     * 利用可能なプログラミング言語の配列を取得
     * 
     * @return プログラミング言語の配列
     */
    public static String[] getAvailableLanguages() {
        return new String[] {
                "C++", "C#", "Java", "Python", "JavaScript",
                "TypeScript", "PHP", "Ruby", "Go", "Swift",
                "Kotlin", "SQL", "HTML/CSS", "その他"
        };
    }
}