package view;

import javax.swing.*;

import util.LogHandler;
import util.LogHandler.LogType;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * ダイアログ表示を一元管理するシングルトンクラス
 * アプリケーション全体で一貫したダイアログ表示を提供します
 *
 * <p>
 * このクラスは、エンジニア管理システム内のすべてのダイアログ表示を管理し、
 * 視覚的に一貫したユーザーインターフェースを提供します。シングルトンパターンを
 * 採用しており、アプリケーション全体で単一のインスタンスを共有します。
 * </p>
 *
 * <p>
 * DialogManagerが提供する主な機能：
 * <ul>
 * <li>エラーダイアログの表示 - ユーザー入力エラーや処理エラーの通知</li>
 * <li>警告ダイアログの表示 - 潜在的な問題や注意事項の通知</li>
 * <li>情報ダイアログの表示 - 一般的な情報の通知</li>
 * <li>確認ダイアログの表示 - ユーザーの確認が必要な操作の前に表示</li>
 * <li>完了ダイアログの表示 - 処理の成功完了を通知</li>
 * <li>重複ID確認ダイアログの表示 - CSV読み込み時のID重複処理の確認</li>
 * </ul>
 * </p>
 *
 * <p>
 * このクラスは、Swingのダイアログコンポーネント（JOptionPane）をラップし、
 * アプリケーション固有のメッセージや振る舞いを統一的に提供します。
 * また、非同期処理でも適切にダイアログを表示できるよう、SwingUtilitiesと
 * CompletableFutureを組み合わせた実装を提供します。
 * </p>
 *
 * <p>
 * 使用例：
 * 
 * <pre>
 * // エラーダイアログの表示
 * DialogManager.getInstance().showErrorDialog("入力データが不正です");
 *
 * // 確認ダイアログの表示と結果の取得
 * boolean confirmed = DialogManager.getInstance().showConfirmDialog("保存してもよろしいですか？");
 * if (confirmed) {
 *     // 保存処理
 * }
 *
 * // 完了ダイアログの表示
 * DialogManager.getInstance().showCompletionDialog("データの保存が完了しました");
 * </pre>
 * </p>
 *
 * @author Nakano
 * @version 4.0.0
 * @since 2025-04-08
 */
public class DialogManager {

    /** シングルトンインスタンス */
    private static final DialogManager INSTANCE = new DialogManager();

    /** ダイアログのデフォルトタイトル */
    private static final String DEFAULT_ERROR_TITLE = "エラー";
    private static final String DEFAULT_WARNING_TITLE = "警告";
    private static final String DEFAULT_INFO_TITLE = "情報";
    private static final String DEFAULT_CONFIRM_TITLE = "確認";
    private static final String DEFAULT_COMPLETION_TITLE = "完了";

    /** ダイアログアイコン用のカラー設定 */
    private static final Color ERROR_COLOR = new Color(204, 0, 0);
    private static final Color WARNING_COLOR = new Color(255, 153, 0);
    private static final Color INFO_COLOR = new Color(0, 102, 204);
    private static final Color SUCCESS_COLOR = new Color(0, 153, 51);

    /**
     * プライベートコンストラクタ
     * シングルトンパターンを実現するため、外部からのインスタンス化を防止します
     */
    private DialogManager() {
        // シングルトンパターンのため空のコンストラクタ
    }

    /**
     * シングルトンインスタンスを取得します
     *
     * @return DialogManagerの唯一のインスタンス
     */
    public static DialogManager getInstance() {
        return INSTANCE;
    }

    /**
     * エラーダイアログを表示します
     * 入力エラーや処理エラーなど、エラー情報をユーザーに通知します
     *
     * @param message 表示するエラーメッセージ
     */
    public void showErrorDialog(String message) {
        showErrorDialog(DEFAULT_ERROR_TITLE, message);
    }

    /**
     * カスタムタイトルを指定してエラーダイアログを表示します
     *
     * @param title   ダイアログのタイトル
     * @param message 表示するエラーメッセージ
     */
    public void showErrorDialog(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    getActiveFrame(),
                    message,
                    title,
                    JOptionPane.ERROR_MESSAGE);
        });
    }

    /**
     * 警告ダイアログを表示します
     * 潜在的な問題や注意事項をユーザーに通知します
     *
     * @param message 表示する警告メッセージ
     */
    public void showWarningDialog(String message) {
        showWarningDialog(DEFAULT_WARNING_TITLE, message);
    }

    /**
     * カスタムタイトルを指定して警告ダイアログを表示します
     *
     * @param title   ダイアログのタイトル
     * @param message 表示する警告メッセージ
     */
    public void showWarningDialog(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    getActiveFrame(),
                    message,
                    title,
                    JOptionPane.WARNING_MESSAGE);
        });
    }

    /**
     * 情報ダイアログを表示します
     * 一般的な情報をユーザーに通知します
     *
     * @param message 表示する情報メッセージ
     */
    public void showInfoDialog(String message) {
        showInfoDialog(DEFAULT_INFO_TITLE, message);
    }

    /**
     * カスタムタイトルを指定して情報ダイアログを表示します
     *
     * @param title   ダイアログのタイトル
     * @param message 表示する情報メッセージ
     */
    public void showInfoDialog(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    getActiveFrame(),
                    message,
                    title,
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    /**
     * 完了ダイアログを表示します
     * 処理の成功完了をユーザーに通知します
     *
     * @param message 表示する完了メッセージ
     */
    public void showCompletionDialog(String message) {
        showCompletionDialog(DEFAULT_COMPLETION_TITLE, message);
    }

    /**
     * カスタムタイトルを指定して完了ダイアログを表示します
     *
     * @param title   ダイアログのタイトル
     * @param message 表示する完了メッセージ
     */
    public void showCompletionDialog(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            // カスタムの成功アイコンを使用したダイアログを表示
            JOptionPane.showMessageDialog(
                    getActiveFrame(),
                    message,
                    title,
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    /**
     * 確認ダイアログを表示します
     * ユーザーの確認が必要な操作の前に、確認を求めるダイアログを表示します
     *
     * @param message 表示する確認メッセージ
     * @return 「はい」が選択された場合はtrue、それ以外はfalse
     */
    public boolean showConfirmDialog(String message) {
        return showConfirmDialog(DEFAULT_CONFIRM_TITLE, message);
    }

    /**
     * カスタムタイトルを指定して確認ダイアログを表示します
     *
     * @param title   ダイアログのタイトル
     * @param message 表示する確認メッセージ
     * @return 「はい」が選択された場合はtrue、それ以外はfalse
     */
    public boolean showConfirmDialog(String title, String message) {
        try {
            // 非同期処理でダイアログを表示し、結果を待機
            CompletableFuture<Boolean> future = new CompletableFuture<>();

            SwingUtilities.invokeLater(() -> {
                int result = JOptionPane.showConfirmDialog(
                        getActiveFrame(),
                        message,
                        title,
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                future.complete(result == JOptionPane.YES_OPTION);
            });

            // 結果が利用可能になるまで待機
            return future.get();

        } catch (InterruptedException | ExecutionException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "確認ダイアログの表示中にエラーが発生しました", e);
            Thread.currentThread().interrupt(); // 割り込みステータスを復元
            return false;
        }
    }

    /**
     * ID重複確認ダイアログを表示します
     * CSV読み込み時に重複IDが検出された場合に、上書き確認のダイアログを表示します
     *
     * @param duplicateIds 重複しているID一覧
     * @return 上書きする場合はtrue、そうでなければfalse
     */
    public boolean showDuplicateIdConfirmDialog(List<String> duplicateIds) {
        try {
            // 非同期処理でダイアログを表示し、結果を待機
            CompletableFuture<Boolean> future = new CompletableFuture<>();

            SwingUtilities.invokeLater(() -> {
                // 重複IDの一覧を表示用に整形
                String idListText = duplicateIds.stream()
                        .limit(10) // 表示数を制限
                        .collect(Collectors.joining(", "));

                // 表示IDが10個を超える場合は、省略表記を追加
                if (duplicateIds.size() > 10) {
                    idListText += "... 他 " + (duplicateIds.size() - 10) + " 件";
                }

                // メッセージの構築
                String message = String.format(
                        "以下のIDが既に存在します。上書きしますか？\n\n%s\n\n" +
                                "「はい」：既存データを上書きします\n" +
                                "「いいえ」：既存データを保持し、新しいデータをスキップします",
                        idListText);

                int result = JOptionPane.showConfirmDialog(
                        getActiveFrame(),
                        message,
                        "ID重複確認",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                future.complete(result == JOptionPane.YES_OPTION);
            });

            // 結果が利用可能になるまで待機
            return future.get();

        } catch (InterruptedException | ExecutionException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "ID重複確認ダイアログの表示中にエラーが発生しました", e);
            Thread.currentThread().interrupt(); // 割り込みステータスを復元
            return false;
        }
    }

    /**
     * カスタムボタン付きの選択ダイアログを表示します
     * ユーザーに複数の選択肢を提示し、選択された結果を返します
     *
     * @param title         ダイアログのタイトル
     * @param message       表示するメッセージ
     * @param options       選択肢となるボタンラベルの配列
     * @param initialOption デフォルト選択されるオプション
     * @return 選択されたオプションのインデックス、キャンセルの場合は-1
     */
    public int showOptionDialog(String title, String message, String[] options, int initialOption) {
        try {
            // 非同期処理でダイアログを表示し、結果を待機
            CompletableFuture<Integer> future = new CompletableFuture<>();

            SwingUtilities.invokeLater(() -> {
                int result = JOptionPane.showOptionDialog(
                        getActiveFrame(),
                        message,
                        title,
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[initialOption]);

                future.complete(result);
            });

            // 結果が利用可能になるまで待機
            return future.get();

        } catch (InterruptedException | ExecutionException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "選択ダイアログの表示中にエラーが発生しました", e);
            Thread.currentThread().interrupt(); // 割り込みステータスを復元
            return -1;
        }
    }

    /**
     * 入力ダイアログを表示します
     * ユーザーからテキスト入力を受け付けるダイアログを表示します
     *
     * @param message      表示するメッセージ
     * @param initialValue 初期値
     * @return ユーザーが入力した文字列、キャンセルの場合はnull
     */
    public String showInputDialog(String message, String initialValue) {
        try {
            // 非同期処理でダイアログを表示し、結果を待機
            CompletableFuture<String> future = new CompletableFuture<>();

            SwingUtilities.invokeLater(() -> {
                String result = JOptionPane.showInputDialog(
                        getActiveFrame(),
                        message,
                        initialValue);

                future.complete(result);
            });

            // 結果が利用可能になるまで待機
            return future.get();

        } catch (InterruptedException | ExecutionException e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "入力ダイアログの表示中にエラーが発生しました", e);
            Thread.currentThread().interrupt(); // 割り込みステータスを復元
            return null;
        }
    }

    /**
     * バリデーションエラーダイアログを表示します
     * フォーム入力のバリデーションエラー時に特化したエラーダイアログを表示します
     * 
     * @param errorFields エラーのあるフィールド名のリスト
     */
    public void showValidationErrorDialog(List<String> errorFields) {
        StringBuilder messageBuilder = new StringBuilder("以下の入力項目に誤りがあります：\n\n");

        for (String field : errorFields) {
            messageBuilder.append("・").append(field).append("\n");
        }

        messageBuilder.append("\n入力内容を確認してください。");

        showErrorDialog("入力エラー", messageBuilder.toString());
    }

    /**
     * システムエラーダイアログを表示します
     * システム内部エラーをユーザーに通知します
     * 
     * @param errorMessage エラーメッセージ
     * @param exception    発生した例外（オプション）
     */
    public void showSystemErrorDialog(String errorMessage, Throwable exception) {
        StringBuilder messageBuilder = new StringBuilder(errorMessage);

        if (exception != null) {
            String exceptionMessage = exception.getMessage();
            if (exceptionMessage != null && !exceptionMessage.isEmpty()) {
                messageBuilder.append("\n\n詳細: ").append(exceptionMessage);
            }
        }

        showErrorDialog("システムエラー", messageBuilder.toString());
    }

    /**
     * アクティブなフレームを取得します
     * ダイアログの親コンポーネントとして使用するフレームを取得します
     *
     * @return アクティブなフレーム、存在しない場合はnull
     */
    private Frame getActiveFrame() {
        // アクティブなウィンドウを取得
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window.isActive() && window instanceof Frame) {
                return (Frame) window;
            }
        }

        // アクティブなウィンドウがない場合は、表示されているいずれかのフレームを使用
        for (Window window : windows) {
            if (window.isVisible() && window instanceof Frame) {
                return (Frame) window;
            }
        }

        return null;
    }
}