package view;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 日付選択用コンボボックスの選択肢を生成するユーティリティクラス
 * 年、月、日、エンジニア歴、スキル評価の選択肢を統一的に提供
 * うるう年を考慮した動的な日付選択肢生成機能を追加
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
     * 入社年用の年選択肢を生成（1950年〜現在年）
     * 
     * @return 年の選択肢配列
     */
    public static String[] getJoinYearOptions() {
        return getYearOptions(1950, LocalDate.now().getYear());
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
     * 日の選択肢を生成（従来版：固定1-31日）
     * 互換性のため残存
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
     * 指定された年月に基づく日の選択肢を生成（うるう年対応）
     * 年と月が指定された場合、その月の実際の日数に基づいて選択肢を生成
     * 
     * @param year  年（null または 空文字列の場合は最大31日を返す）
     * @param month 月（null または 空文字列の場合は最大31日を返す）
     * @return 日の選択肢配列（空文字 + 1-実際の日数）
     */
    public static String[] getDayOptions(String year, String month) {
        // 年または月が未選択の場合は最大日数を返す
        if (year == null || year.trim().isEmpty() ||
                month == null || month.trim().isEmpty()) {
            return getDayOptions();
        }

        try {
            int yearInt = Integer.parseInt(year.trim());
            int monthInt = Integer.parseInt(month.trim());

            // 指定された年月の日数を取得
            YearMonth yearMonth = YearMonth.of(yearInt, monthInt);
            int daysInMonth = yearMonth.lengthOfMonth();

            // 選択肢配列を生成
            String[] days = new String[daysInMonth + 1]; // 空 + 1-実際の日数
            days[0] = "";

            for (int i = 1; i <= daysInMonth; i++) {
                days[i] = String.valueOf(i);
            }

            return days;

        } catch (NumberFormatException _e) {
            // 数値変換エラーの場合は最大日数を返す
            return getDayOptions();
        }
    }

    /**
     * 指定された年がうるう年かどうかを判定
     * 
     * @param year 判定対象の年
     * @return うるう年の場合true
     */
    public static boolean isLeapYear(int year) {
        return Year.isLeap(year);
    }

    /**
     * 指定された年がうるう年かどうかを判定（文字列版）
     * 
     * @param yearStr 判定対象の年（文字列）
     * @return うるう年の場合true、変換エラーの場合false
     */
    public static boolean isLeapYear(String yearStr) {
        if (yearStr == null || yearStr.trim().isEmpty()) {
            return false;
        }

        try {
            int year = Integer.parseInt(yearStr.trim());
            return isLeapYear(year);
        } catch (NumberFormatException _e) {
            return false;
        }
    }

    /**
     * 指定された年月日が有効な日付かどうかを検証
     * うるう年を考慮した日付の存在チェック
     * 
     * @param year  年
     * @param month 月
     * @param day   日
     * @return 有効な日付の場合true
     */
    public static boolean isValidDate(String year, String month, String day) {
        if (year == null || year.trim().isEmpty() ||
                month == null || month.trim().isEmpty() ||
                day == null || day.trim().isEmpty()) {
            return false;
        }

        try {
            int yearInt = Integer.parseInt(year.trim());
            int monthInt = Integer.parseInt(month.trim());
            int dayInt = Integer.parseInt(day.trim());

            // LocalDateで日付生成を試行（例外発生で無効と判定）
            LocalDate.of(yearInt, monthInt, dayInt);
            return true;

        } catch (Exception _e) {
            return false;
        }
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