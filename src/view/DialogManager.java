package view;

import javax.swing.*;
import model.EngineerDTO;
import util.LogHandler;
import util.LogHandler.LogType;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
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
 * @author Bando
 * @version 4.1.0
 * @since 2025-05-07
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
     * 完了メッセージを表示するダイアログを表示し、ユーザーが閉じた後に任意の後続処理を実行します。
     *
     * @param message  表示する完了メッセージ
     * @param onClosed ダイアログが閉じられた後に実行される処理（null 可）
     */
    public void showCompletionDialog(String message, Runnable onClosed) {

        JOptionPane.showMessageDialog(
                getActiveFrame(),
                message,
                "メッセージ",
                JOptionPane.INFORMATION_MESSAGE);
        if (onClosed != null) {
            onClosed.run(); // ダイアログ閉じたら後続処理実行
        }

    }

    /**
     * 大量データをスクロール表示できる確認ダイアログを表示します
     *
     * @param title         ダイアログのタイトル
     * @param headerMessage 上部に表示する簡単なメッセージ（null可）
     * @param lines         表示する文字列リスト
     * @return 「はい」が選択された場合はtrue、それ以外はfalse
     */
    public boolean showScrollableListDialog(String title, String headerMessage, List<String> lines) {
        try {
            StringBuilder sb = new StringBuilder();
            if (headerMessage != null && !headerMessage.isEmpty()) {
                sb.append(headerMessage).append("\n\n");
            }
            for (String line : lines) {
                sb.append(line).append("\n");
            }

            JTextArea textArea = new JTextArea(sb.toString());
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300)); // 必要に応じて調整

            int result = JOptionPane.showConfirmDialog(
                    getActiveFrame(),
                    scrollPane,
                    title,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            return result == JOptionPane.YES_OPTION;

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "スクロール可能リストダイアログの表示中にエラーが発生しました", e);
            return false;
        }
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
        if (SwingUtilities.isEventDispatchThread()) {
            int result = JOptionPane.showConfirmDialog(
                    getActiveFrame(),
                    message,
                    title,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            return result == JOptionPane.YES_OPTION;
        }

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

    // public boolean showConfirmDialog(String title, String message) {
    // try {
    // // 非同期処理でダイアログを表示し、結果を待機
    // CompletableFuture<Boolean> future = new CompletableFuture<>();

    // SwingUtilities.invokeLater(() -> {
    // int result = JOptionPane.showConfirmDialog(
    // getActiveFrame(),
    // message,
    // title,
    // JOptionPane.YES_NO_OPTION,
    // JOptionPane.QUESTION_MESSAGE);

    // future.complete(result == JOptionPane.YES_OPTION);
    // });

    // // 結果が利用可能になるまで待機
    // return future.get();

    // } catch (InterruptedException | ExecutionException e) {
    // LogHandler.getInstance().logError(LogType.SYSTEM, "確認ダイアログの表示中にエラーが発生しました",
    // e);
    // Thread.currentThread().interrupt(); // 割り込みステータスを復元
    // return false;
    // }
    // }

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
     * エンジニア登録完了時の選択ダイアログを表示します
     * <p>
     * このメソッドは、エンジニア情報の登録が成功した後にユーザーに表示される
     * ダイアログを生成・表示します。ユーザーは登録完了後の次のアクションとして
     * 以下の3つの選択肢から選ぶことができます：
     * </p>
     * 
     * <ul>
     * <li><b>続けて登録</b>: 入力フォームをクリアして新たな登録を続行</li>
     * <li><b>一覧に戻る</b>: エンジニア一覧画面に戻る</li>
     * <li><b>詳細を表示</b>: 登録したエンジニアの詳細画面を表示</li>
     * </ul>
     * 
     * <p>
     * このメソッドは、呼び出し元のスレッドがイベントディスパッチスレッド(EDT)かどうかを
     * 自動的に検出し、適切な方法でダイアログ表示を行います：
     * </p>
     * 
     * <ul>
     * <li>EDT上から呼び出された場合：直接ダイアログを表示し、結果を同期的に返します</li>
     * <li>非EDT上から呼び出された場合：CompletableFutureを使用して、EDTでダイアログを表示し、
     * 結果が得られるまで安全に待機します</li>
     * </ul>
     * 
     * <p>
     * この実装による主な利点：
     * </p>
     * 
     * <ul>
     * <li>EDTでのデッドロック防止: EDT上での呼び出し時にCompletableFuture.get()によるEDTブロックを回避</li>
     * <li>一貫した使用方法: 呼び出し元は、どのスレッドから呼び出してもAPIの使い方を変更する必要がない</li>
     * <li>堅牢なエラーハンドリング: 例外発生時も確実に結果を返し、EDTブロックを防止</li>
     * </ul>
     * 
     * <p>
     * エラーが発生した場合でも適切にハンドリングし、ログに記録した上でデフォルトの
     * アクション（"CONTINUE"）を返却します。また、スレッドの割り込みが発生した場合は
     * 割り込みステータスを適切に復元します。
     * </p>
     * 
     * <p>
     * このメソッドは、処理の各ステップでロギングを行い、問題が発生した場合の
     * トラブルシューティングを容易にしています。
     * </p>
     *
     * @param engineer 登録されたエンジニア情報（{@link EngineerDTO}オブジェクト）
     * @return 選択されたアクション（"CONTINUE", "LIST", "DETAIL"のいずれか）
     */
    public String showRegisterCompletionDialog(EngineerDTO engineer) {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "登録完了ダイアログ表示処理を開始: ID=" + engineer.getId());

        // イベントディスパッチスレッド（EDT）上で実行されているかを確認
        if (SwingUtilities.isEventDispatchThread()) {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "EDT上での直接ダイアログ表示を実行: ID=" + engineer.getId());
            // EDT上で直接ダイアログを表示
            return showCompletionDialogDirectly(engineer);
        } else {
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "非EDT上での非同期ダイアログ表示を実行: ID=" + engineer.getId());
            // 非EDT上からの呼び出し - CompletableFutureを使用
            return showCompletionDialogAsync(engineer);
        }
    }

    /**
     * EDT上で直接登録完了ダイアログを表示します（内部メソッド）
     * <p>
     * このメソッドは、イベントディスパッチスレッド(EDT)上から呼び出される場合に使用され、
     * ダイアログを直接表示して結果を返します。CompletableFutureのようなスレッド間通信
     * メカニズムを使用せず、同期的に処理を行うため、EDTブロックのリスクを回避します。
     * </p>
     * 
     * @param engineer 登録されたエンジニア情報
     * @return 選択されたアクション
     */
    private String showCompletionDialogDirectly(EngineerDTO engineer) {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "直接ダイアログ表示を開始: ID=" + engineer.getId());

        try {
            // メッセージの構築
            String message = String.format(
                    "エンジニア情報を登録しました\nID: %s\n氏名: %s\n\n次のアクションを選択してください",
                    engineer.getId(), engineer.getName());

            // 選択肢ボタンの定義
            String[] options = {
                    "続けて登録", "一覧に戻る", "詳細を表示"
            };

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "ダイアログを表示します: ID=" + engineer.getId());

            // ダイアログを表示し、選択結果を取得
            int result = JOptionPane.showOptionDialog(
                    getActiveFrame(),
                    message,
                    "登録完了",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    options[0]);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "ダイアログの選択結果: " + result + ", ID=" + engineer.getId());

            // 選択結果をアクション文字列に変換
            String action;
            switch (result) {
                case 0:
                    action = "CONTINUE";
                    break;
                case 1:
                    action = "LIST";
                    break;
                case 2:
                    action = "DETAIL";
                    break;
                case JOptionPane.CLOSED_OPTION: // ダイアログが閉じられた場合
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "ダイアログが閉じられました - デフォルトアクションを使用: ID=" + engineer.getId());
                    action = "CONTINUE"; // デフォルト
                    break;
                default:
                    LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                            "予期しない選択結果: " + result + " - デフォルトアクションを使用: ID=" + engineer.getId());
                    action = "CONTINUE"; // デフォルト
                    break;
            }

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "直接ダイアログ処理完了 - 選択アクション: " + action + ", ID=" + engineer.getId());

            return action;

        } catch (Exception e) {
            // ダイアログ表示中のエラー
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "直接ダイアログ表示中にエラーが発生しました: ID=" + engineer.getId(), e);

            // エラー時はデフォルトアクションを返す
            return "CONTINUE";
        }
    }

    /**
     * 非EDT上から登録完了ダイアログを非同期的に表示します（内部メソッド）
     * <p>
     * このメソッドは、イベントディスパッチスレッド(EDT)以外のスレッドから呼び出される場合に使用され、
     * CompletableFutureを使用してEDT上でダイアログを表示し、その結果を安全に待機します。
     * EDTをブロックすることなく、ダイアログの結果を非同期に取得することができます。
     * </p>
     * 
     * @param engineer 登録されたエンジニア情報
     * @return 選択されたアクション
     */
    private String showCompletionDialogAsync(EngineerDTO engineer) {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "非同期ダイアログ表示を開始: ID=" + engineer.getId());

        try {
            // 非同期処理でダイアログを表示し、結果を待機するためのFuture
            CompletableFuture<String> future = new CompletableFuture<>();

            SwingUtilities.invokeLater(() -> {
                try {
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "EDTでダイアログの表示を準備中: ID=" + engineer.getId());

                    // メッセージの構築
                    String message = String.format(
                            "エンジニア情報を登録しました\nID: %s\n氏名: %s\n\n次のアクションを選択してください",
                            engineer.getId(), engineer.getName());

                    // 選択肢ボタンの定義
                    String[] options = {
                            "続けて登録", "一覧に戻る", "詳細を表示"
                    };

                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "ダイアログを表示します: ID=" + engineer.getId());

                    // ダイアログを表示し、選択結果を取得
                    int result = JOptionPane.showOptionDialog(
                            getActiveFrame(),
                            message,
                            "登録完了",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            options,
                            options[0]);

                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "ダイアログの選択結果: " + result + ", ID=" + engineer.getId());

                    // 選択結果をアクション文字列に変換
                    String action;
                    switch (result) {
                        case 0:
                            action = "CONTINUE";
                            break;
                        case 1:
                            action = "LIST";
                            break;
                        case 2:
                            action = "DETAIL";
                            break;
                        case JOptionPane.CLOSED_OPTION: // ダイアログが閉じられた場合
                            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                                    "ダイアログが閉じられました - デフォルトアクションを使用: ID=" + engineer.getId());
                            action = "CONTINUE"; // デフォルト
                            break;
                        default:
                            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                                    "予期しない選択結果: " + result + " - デフォルトアクションを使用: ID=" + engineer.getId());
                            action = "CONTINUE"; // デフォルト
                            break;
                    }

                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "選択アクション: " + action + ", ID=" + engineer.getId());

                    // 結果をCompletableFutureに設定
                    future.complete(action);

                } catch (Exception e) {
                    // ダイアログ表示中のエラー
                    LogHandler.getInstance().logError(LogType.SYSTEM,
                            "ダイアログ表示中にエラーが発生しました: ID=" + engineer.getId(), e);

                    // エラー時はデフォルトアクションを返す
                    future.complete("CONTINUE");
                }
            });

            // 結果が利用可能になるまで待機
            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "ダイアログの結果を待機中: ID=" + engineer.getId());

            String result = future.get();

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "非同期ダイアログ処理完了 - アクション=" + result + ", ID=" + engineer.getId());

            return result;

        } catch (InterruptedException e) {
            // 割り込み発生時
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "非同期ダイアログの待機中に割り込みが発生しました: ID=" + engineer.getId(), e);
            Thread.currentThread().interrupt(); // 割り込みステータスを復元
            return "CONTINUE"; // エラー時はデフォルト動作

        } catch (ExecutionException e) {
            // 実行時エラー
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "非同期ダイアログの実行中にエラーが発生しました: ID=" + engineer.getId(), e);
            return "CONTINUE"; // エラー時はデフォルト動作

        } catch (Exception e) {
            // その他の予期しないエラー
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "非同期ダイアログ処理中に予期しないエラーが発生しました: ID=" + engineer.getId(), e);
            return "CONTINUE"; // エラー時はデフォルト動作
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