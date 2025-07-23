package util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import util.LogHandler.LogType;

/**
 * テキストフィールド・テキストエリアの文字数制限を行うDocumentFilter
 * Unicode文字（サロゲートペア、絵文字）を考慮した正確な文字数カウントを実装
 * JTextField、JTextArea、JTextPaneなど全てのテキストコンポーネントで利用可能
 * 
 * @author Nakano
 */
public class TextLengthFilter extends DocumentFilter {
    
    /** 最大文字数 */
    private final int maxLength;
    
    /** フィールド名（ログ用） */
    private final String fieldName;
    
    /**
     * コンストラクタ
     * 
     * @param maxLength 最大文字数
     * @param fieldName フィールド名（ログ用）
     */
    public TextLengthFilter(int maxLength, String fieldName) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("最大文字数は正の値である必要があります: " + maxLength);
        }
        this.maxLength = maxLength;
        this.fieldName = fieldName != null ? fieldName : "不明";
    }
    
    /**
     * テキスト挿入時の制限チェック
     */
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) 
            throws BadLocationException {
        
        if (string == null) {
            return;
        }
        
        // 現在のドキュメント内容を取得
        String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
        
        // 挿入後の文字列を構築
        String newText = new StringBuilder(currentText)
            .insert(offset, string)
            .toString();
        
        // Unicode対応の文字数カウント
        int newLength = StringUtil.countCodePoints(newText);
        
        if (newLength <= maxLength) {
            // 制限内の場合は挿入を許可
            super.insertString(fb, offset, string, attr);
        } else {
            // 制限を超える場合は部分的に挿入
            int currentLength = StringUtil.countCodePoints(currentText);
            int allowedLength = maxLength - currentLength;
            
            if (allowedLength > 0) {
                // 挿入可能な文字数分のみ抽出
                String allowedString = StringUtil.truncateAtCodePoint(string, allowedLength);
                super.insertString(fb, offset, allowedString, attr);
                
                LogHandler.getInstance().log(
                    java.util.logging.Level.INFO, 
                    LogType.UI, 
                    fieldName + "フィールド: 文字数制限により部分挿入 - " + 
                    allowedLength + "文字のみ挿入（最大" + maxLength + "文字）"
                );
            } else {
                LogHandler.getInstance().log(
                    java.util.logging.Level.INFO, 
                    LogType.UI, 
                    fieldName + "フィールド: 文字数制限により挿入拒否 - " + 
                    "現在" + currentLength + "文字（最大" + maxLength + "文字）"
                );
            }
        }
    }
    
    /**
     * テキスト置換時の制限チェック
     */
    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) 
            throws BadLocationException {
        
        if (text == null) {
            text = "";
        }
        
        // 現在のドキュメント内容を取得
        String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
        
        // 置換後の文字列を構築
        String newText = new StringBuilder(currentText)
            .replace(offset, offset + length, text)
            .toString();
        
        // Unicode対応の文字数カウント
        int newLength = StringUtil.countCodePoints(newText);
        
        if (newLength <= maxLength) {
            // 制限内の場合は置換を許可
            super.replace(fb, offset, length, text, attrs);
        } else {
            // 制限を超える場合は部分的に置換
            int currentLength = StringUtil.countCodePoints(currentText);
            int removedLength = StringUtil.countCodePoints(
                currentText.substring(offset, offset + length)
            );
            int availableLength = maxLength - (currentLength - removedLength);
            
            if (availableLength > 0) {
                // 置換可能な文字数分のみ抽出
                String allowedText = StringUtil.truncateAtCodePoint(text, availableLength);
                super.replace(fb, offset, length, allowedText, attrs);
                
                LogHandler.getInstance().log(
                    java.util.logging.Level.INFO, 
                    LogType.UI, 
                    fieldName + "フィールド: 文字数制限により部分置換 - " + 
                    availableLength + "文字のみ置換（最大" + maxLength + "文字）"
                );
            } else {
                // 削除のみ実行（置換文字列は空文字）
                super.replace(fb, offset, length, "", attrs);
                
                LogHandler.getInstance().log(
                    java.util.logging.Level.INFO, 
                    LogType.UI, 
                    fieldName + "フィールド: 文字数制限により削除のみ実行 - " + 
                    "現在" + (currentLength - removedLength) + "文字（最大" + maxLength + "文字）"
                );
            }
        }
    }
    
    /**
     * 現在の設定情報を取得
     * 
     * @return 設定情報文字列
     */
    public String getFilterInfo() {
        return String.format("TextLengthFilter[フィールド=%s, 最大文字数=%d]", 
                            fieldName, maxLength);
    }
    
    /**
     * 最大文字数を取得
     * 
     * @return 最大文字数
     */
    public int getMaxLength() {
        return maxLength;
    }
    
    /**
     * フィールド名を取得
     * 
     * @return フィールド名
     */
    public String getFieldName() {
        return fieldName;
    }
}