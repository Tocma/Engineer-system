package util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * 文字列操作のユーティリティクラス（Unicode対応版）
 * サロゲートペア文字、絵文字、環境依存文字を適切に処理します
 * 
 * @author Nakano
 */
public final class StringUtil {

    /** 絵文字パターン（Java対応版） */
    private static final Pattern EMOJI_PATTERN = Pattern.compile(
            "[\\uD83D\\uDE00-\\uD83D\\uDE4F]|" + // 顔文字（サロゲートペア）
                    "[\\uD83C\\uDF00-\\uD83D\\uDDFF]|" + // その他のシンボル（サロゲートペア）
                    "[\\uD83D\\uDE80-\\uD83D\\uDEFF]|" + // 交通・地図（サロゲートペア）
                    "[\\u2600-\\u26FF]|" + // その他のシンボル（BMP）
                    "[\\u2700-\\u27BF]|" + // 装飾記号（BMP）
                    "[\\uD83E\\uDD00-\\uD83E\\uDDFF]|" + // 補助シンボル（サロゲートペア）
                    "[\\uD83C\\uDDE6-\\uD83C\\uDDFF]|" + // 国旗（地域表示文字）
                    "[\\uFE00-\\uFE0F]|" + // 異体字セレクタ（BMP）
                    "[\\uDB40\\uDD00-\\uDB40\\uDDEF]" // 異体字セレクタ補助（サロゲートペア）
    );

    /** 環境依存文字パターン */
    private static final Pattern ENVIRONMENT_DEPENDENT_PATTERN = Pattern.compile(
            "[\\uE000-\\uF8FF]|" + // 私用領域（BMP）
                    "[\\uDB80\\uDC00-\\uDBBF\\uDFFD]|" + // 私用領域A（サロゲートペア）
                    "[\\uDBC0\\uDC00-\\uDBFF\\uDFFD]" // 私用領域B（サロゲートペア）
    );

    /** 結合文字パターン */
    private static final Pattern COMBINING_PATTERN = Pattern.compile(
            "[\\u0300-\\u036F]|" + // 結合ダイアクリティカルマーク
                    "[\\u1AB0-\\u1AFF]|" + // 結合ダイアクリティカルマーク拡張
                    "[\\u1DC0-\\u1DFF]|" + // 結合ダイアクリティカルマーク補助
                    "[\\u20D0-\\u20FF]|" + // 結合記号
                    "[\\uFE20-\\uFE2F]" // 結合半角記号
    );

    /**
     * プライベートコンストラクタ
     */
    private StringUtil() {
        throw new AssertionError("ユーティリティクラスはインスタンス化できません");
    }

    /**
     * Unicode文字列の文字数をカウント（サロゲートペア対応）
     * 
     * @param input 入力文字列
     * @return 実際の文字数（サロゲートペアを1文字として計算）
     */
    public static int countCodePoints(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }
        return input.codePointCount(0, input.length());
    }

    /**
     * 文字列の書記素クラスター数をカウント
     * 結合文字や絵文字の合成を考慮した実際の表示文字数
     * 
     * @param input 入力文字列
     * @return 書記素クラスター数
     */
    public static int countGraphemeClusters(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }

        // 正規化を行い、結合文字を適切に処理
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFC);

        // 書記素クラスター境界で分割
        return normalized.split("(?<=\\p{L})(?=\\p{L})|(?<=\\p{So})(?=\\p{So})").length;
    }

    /**
     * 絵文字を含む文字列の安全な切り詰め
     * 
     * @param input     入力文字列
     * @param maxLength 最大文字数（コードポイント単位）
     * @return 切り詰められた文字列
     */
    public static String truncateAtCodePoint(String input, int maxLength) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        if (countCodePoints(input) <= maxLength) {
            return input;
        }

        int offset = input.offsetByCodePoints(0, maxLength);
        return input.substring(0, offset);
    }

    /**
     * 環境依存文字とサロゲートペアを安全に除去
     * 
     * @param input 入力文字列
     * @return 環境依存文字を除去した文字列
     */
    public static String removeEnvironmentDependentChars(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return ENVIRONMENT_DEPENDENT_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * 絵文字を除去
     * 
     * @param input 入力文字列
     * @return 絵文字を除去した文字列
     */
    public static String removeEmoji(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return EMOJI_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * 文字列の正規化とサニタイズ
     * NFCで正規化し、問題のある文字を除去
     * 
     * @param input 入力文字列
     * @return 正規化・サニタイズされた文字列
     */
    public static String normalizeAndSanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Unicode正規化（NFC）
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFC);

        // 制御文字を除去（改行・タブは保持）
        String sanitized = normalized.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        return sanitized;
    }

    /**
     * 文字列の安全性チェック
     * 環境依存文字や問題のある文字が含まれていないかチェック
     * 
     * @param input 入力文字列
     * @return 安全な文字列の場合true
     */
    public static boolean isSafeString(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }

        // 環境依存文字チェック
        if (ENVIRONMENT_DEPENDENT_PATTERN.matcher(input).find()) {
            return false;
        }

        // 制御文字チェック（改行・タブは除く）
        if (input.matches(".*[\\p{Cntrl}&&[^\r\n\t]].*")) {
            return false;
        }

        return true;
    }

    /**
     * 全角数字を半角数字に変換（サロゲートペア対応）
     * 
     * @param input 入力文字列
     * @return 変換後の文字列
     */
    public static String convertFullWidthToHalfWidth(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        int length = input.length();

        for (int i = 0; i < length; i++) {
            int codePoint = input.codePointAt(i);

            // サロゲートペアの場合は次の文字位置を調整
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++;
            }

            // 全角数字（U+FF10〜U+FF19）を半角（U+0030〜U+0039）に変換
            if (codePoint >= 0xFF10 && codePoint <= 0xFF19) {
                sb.append((char) (codePoint - 0xFF10 + 0x0030));
            } else {
                sb.appendCodePoint(codePoint);
            }
        }
        return sb.toString();
    }

    /**
     * 全角英数字を半角に変換（サロゲートペア対応）
     * 
     * @param input 入力文字列
     * @return 変換後の文字列
     */
    public static String convertFullWidthAlphanumericToHalfWidth(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        int length = input.length();

        for (int i = 0; i < length; i++) {
            int codePoint = input.codePointAt(i);

            // サロゲートペアの場合は次の文字位置を調整
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++;
            }

            // 全角数字
            if (codePoint >= 0xFF10 && codePoint <= 0xFF19) {
                sb.append((char) (codePoint - 0xFF10 + 0x0030));
            }
            // 全角大文字英字
            else if (codePoint >= 0xFF21 && codePoint <= 0xFF3A) {
                sb.append((char) (codePoint - 0xFF21 + 0x0041));
            }
            // 全角小文字英字
            else if (codePoint >= 0xFF41 && codePoint <= 0xFF5A) {
                sb.append((char) (codePoint - 0xFF41 + 0x0061));
            } else {
                sb.appendCodePoint(codePoint);
            }
        }
        return sb.toString();
    }

    /**
     * ひらがなをカタカナに変換（サロゲートペア対応）
     * 
     * @param input 入力文字列
     * @return 変換後の文字列
     */
    public static String convertHiraganaToKatakana(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        int length = input.length();

        for (int i = 0; i < length; i++) {
            int codePoint = input.codePointAt(i);

            // サロゲートペアの場合は次の文字位置を調整
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++;
            }

            // ひらがな（U+3041〜U+3096）をカタカナ（U+30A1〜U+30F6）に変換
            if (codePoint >= 0x3041 && codePoint <= 0x3096) {
                sb.appendCodePoint(codePoint + 0x0060);
            } else {
                sb.appendCodePoint(codePoint);
            }
        }
        return sb.toString();
    }

    /**
     * 半角カタカナを全角カタカナに変換（サロゲートペア対応）
     * 
     * @param input 入力文字列
     * @return 変換後の文字列
     */
    public static String convertHalfWidthKatakanaToFullWidth(String input) {
        if (input == null) {
            return null;
        }

        // 半角カタカナと全角カタカナの対応表
        String halfKana = "ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜｦﾝｧｨｩｪｫｬｭｮｯｰ｡｢｣､･";
        String fullKana = "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲンァィゥェォャュョッー。「」、・";

        // 濁点・半濁点対応文字の定義
        String dakutenBase = "ｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾊﾋﾌﾍﾎ";
        String dakutenFull = "ガギグゲゴザジズゼゾダヂヅデドバビブベボ";
        String handakutenBase = "ﾊﾋﾌﾍﾎ";
        String handakutenFull = "パピプペポ";

        StringBuilder sb = new StringBuilder();
        int length = input.length();

        for (int i = 0; i < length; i++) {
            int codePoint = input.codePointAt(i);

            // サロゲートペアの場合は次の文字位置を調整
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++;
                sb.appendCodePoint(codePoint);
                continue;
            }

            char c = (char) codePoint;

            // 濁点・半濁点の組み合わせ処理
            if (i + 1 < length) {
                char nextChar = input.charAt(i + 1);

                // 濁点の処理（基本文字 + ﾞ）
                if (nextChar == 'ﾞ') {
                    int dakutenIndex = dakutenBase.indexOf(c);
                    if (dakutenIndex >= 0) {
                        sb.append(dakutenFull.charAt(dakutenIndex));
                        i++; // 濁点文字をスキップ
                        continue;
                    }
                }

                // 半濁点の処理（基本文字 + ﾟ）
                if (nextChar == 'ﾟ') {
                    int handakutenIndex = handakutenBase.indexOf(c);
                    if (handakutenIndex >= 0) {
                        sb.append(handakutenFull.charAt(handakutenIndex));
                        i++; // 半濁点文字をスキップ
                        continue;
                    }
                }
            }

            // 単独の濁点・半濁点の処理
            if (c == 'ﾞ') {
                sb.append('゙'); // 全角濁点
                continue;
            }
            if (c == 'ﾟ') {
                sb.append('゚'); // 全角半濁点
                continue;
            }

            // 通常の半角カタカナ変換
            int index = halfKana.indexOf(c);
            if (index >= 0) {
                sb.append(fullKana.charAt(index));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * 空白文字を除去（サロゲートペア対応）
     * 
     * @param input 入力文字列
     * @return 空白除去後の文字列
     */
    public static String removeSpaces(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        int length = input.length();

        for (int i = 0; i < length; i++) {
            int codePoint = input.codePointAt(i);

            // サロゲートペアの場合は次の文字位置を調整
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++;
            }

            if (!Character.isWhitespace(codePoint)) {
                sb.appendCodePoint(codePoint);
            }
        }

        return sb.toString();
    }
}