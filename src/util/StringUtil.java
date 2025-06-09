package util;

/**
 * 文字列操作のユーティリティクラス
 * バリデータで使用する文字列変換処理を提供します
 * 
 * @author Nakano
 */
public final class StringUtil {

    /**
     * プライベートコンストラクタ
     */
    private StringUtil() {
        throw new AssertionError("ユーティリティクラスはインスタンス化できません");
    }

    /**
     * 全角数字を半角数字に変換
     * 
     * @param input 入力文字列
     * @return 変換後の文字列
     */
    public static String convertFullWidthToHalfWidth(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // 全角数字（U+FF10〜U+FF19）を半角（U+0030〜U+0039）に変換
            if (c >= '０' && c <= '９') {
                sb.append((char) (c - '０' + '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 全角英数字を半角に変換
     * 
     * @param input 入力文字列
     * @return 変換後の文字列
     */
    public static String convertFullWidthAlphanumericToHalfWidth(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // 全角数字
            if (c >= '０' && c <= '９') {
                sb.append((char) (c - '０' + '0'));
            }
            // 全角大文字英字
            else if (c >= 'Ａ' && c <= 'Ｚ') {
                sb.append((char) (c - 'Ａ' + 'A'));
            }
            // 全角小文字英字
            else if (c >= 'ａ' && c <= 'ｚ') {
                sb.append((char) (c - 'ａ' + 'a'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * ひらがなをカタカナに変換
     * 
     * @param input 入力文字列
     * @return 変換後の文字列
     */
    public static String convertHiraganaToKatakana(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // ひらがな（U+3041〜U+3096）をカタカナ（U+30A1〜U+30F6）に変換
            if (c >= 'ぁ' && c <= 'ゖ') {
                sb.append((char) (c - 'ぁ' + 'ァ'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 半角カタカナを全角カタカナに変換
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

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
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
     * 空白文字を除去
     * 
     * @param input 入力文字列
     * @return 空白除去後の文字列
     */
    public static String removeSpaces(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("\\s+", "");
    }
}