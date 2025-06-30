package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
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
 * アプリケーション全体で一貫したダイアログ表示
 *
 * このクラスは、Swingのダイアログコンポーネント（JOptionPane）をラップし、
 * アプリケーション固有のメッセージや振る舞いを統一的に提供します。
 * また、非同期処理でも適切にダイアログを表示できるよう、SwingUtilitiesと
 * CompletableFutureを組み合わせた実装
 *
 * @author Nakano
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
     * シングルトンパターンを実現するため、外部からのインスタンス化を防止
     */
    private DialogManager() {
        // シングルトンパターンのため空のコンストラクタ
    }

    /**
     * シングルトンインスタンスを取得
     *
     * @return DialogManagerの唯一のインスタンス
     */
    public static DialogManager getInstance() {
        return INSTANCE;
    }

    /**
     * エラーダイアログを表示
     * 入力エラーや処理エラーなど、エラー情報をユーザーに通知
     *
     * @param message 表示するエラーメッセージ
     */
    public void showErrorDialog(String message) {
        showErrorDialog(DEFAULT_ERROR_TITLE, message);
    }

    /**
     * カスタムタイトルを指定してエラーダイアログを表示
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
     * 警告ダイアログを表示
     * 潜在的な問題や注意事項をユーザーに通知
     *
     * @param message 表示する警告メッセージ
     */
    public void showWarningDialog(String message) {
        showWarningDialog(DEFAULT_WARNING_TITLE, message);
    }

    /**
     * カスタムタイトルを指定して警告ダイアログを表示
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
     * 情報ダイアログを表示
     * 一般的な情報をユーザーに通知
     *
     * @param message 表示する情報メッセージ
     */
    public void showInfoDialog(String message) {
        showInfoDialog(DEFAULT_INFO_TITLE, message);
    }

    /**
     * カスタムタイトルを指定して情報ダイアログを表示
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
     * 完了ダイアログを表示
     * 処理の成功完了をユーザーに通知
     *
     * @param message 表示する完了メッセージ
     */
    public void showCompletionDialog(String message) {
        showCompletionDialog(DEFAULT_COMPLETION_TITLE, message);
    }

    /**
     * カスタムタイトルを指定して完了ダイアログを表示
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
     * 完了メッセージを表示するダイアログを表示し、ユーザーが閉じた後に任意の後続処理を実行
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
     * 大量データをスクロール表示できる確認ダイアログを表示
     *
     * @param title         ダイアログのタイトル
     * @param headerMessage 上部に表示する簡単なメッセージ（null可）
     * @param lines         表示する文字列リスト
     * @return 「はい」が選択された場合はtrue、それ以外はfalse
     */
    public boolean showScrollableListDialog(String title, String headerMessage, List<String> lines) {
        try {
            StringBuilder messageBuilder = new StringBuilder();
            if (headerMessage != null && !headerMessage.isEmpty()) {
                messageBuilder.append(headerMessage).append("\n\n");
            }
            for (String line : lines) {
                messageBuilder.append(line).append("\n");
            }

            JTextArea textArea = new JTextArea(messageBuilder.toString());
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

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "スクロール可能リストダイアログの表示中にエラーが発生", _e);
            return false;
        }
    }

    /**
     * 確認ダイアログを表示
     * ユーザーの確認が必要な操作の前に、確認を求めるダイアログを表示
     *
     * @param message 表示する確認メッセージ
     * @return 「はい」が選択された場合はtrue、それ以外はfalse
     */
    public boolean showConfirmDialog(String message) {
        return showConfirmDialog(DEFAULT_CONFIRM_TITLE, message);
    }

    /**
     * カスタムタイトルを指定して確認ダイアログを表示
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

        } catch (InterruptedException | ExecutionException _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "確認ダイアログの表示中にエラーが発生", _e);
            Thread.currentThread().interrupt(); // 割り込みステータスを復元
            return false;
        }
    }

    /**
     * ID重複確認ダイアログを表示
     * CSV読み込み時に重複IDが検出された場合に、上書き確認のダイアログを表示
     *
     * @param duplicateIds 重複しているID一覧
     * @return 上書きする場合はtrue、そうでなければfalse
     */
    public boolean showDuplicateIdConfirmDialog(List<String> duplicateIds) {
        // デバッグログの追加
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "重複確認ダイアログの表示を開始: 重複ID数=" + duplicateIds.size());

        try {
            // 非同期処理でダイアログを表示し、結果を待機
            CompletableFuture<Boolean> future = new CompletableFuture<>();

            SwingUtilities.invokeLater(() -> {
                try {
                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "重複確認ダイアログをEDTで表示中");

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

                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "重複確認ダイアログメッセージ: " + message.substring(0, Math.min(100, message.length())) + "...");

                    int result = JOptionPane.showConfirmDialog(
                            getActiveFrame(),
                            message,
                            "ID重複確認",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);

                    boolean userChoice = (result == JOptionPane.YES_OPTION);

                    LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                            "重複確認ダイアログの結果: " + (userChoice ? "上書き" : "スキップ"));

                    future.complete(userChoice);

                } catch (Exception _e) {
                    LogHandler.getInstance().logError(LogType.SYSTEM,
                            "重複確認ダイアログ内でエラーが発生", _e);
                    future.complete(false); // エラー時はスキップを選択
                }
            });

            // 結果が利用可能になるまで待機
            boolean result = future.get();

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "重複確認ダイアログ処理完了: 結果=" + (result ? "上書き" : "スキップ"));

            return result;

        } catch (InterruptedException | ExecutionException _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "ID重複確認ダイアログの表示中にエラーが発生", _e);
            Thread.currentThread().interrupt(); // 割り込みステータスを復元
            return false;
        }
    }

    /**
     * カスタムボタン付きの選択ダイアログを表示
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

        } catch (InterruptedException | ExecutionException _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "選択ダイアログの表示中にエラーが発生", _e);
            Thread.currentThread().interrupt(); // 割り込みステータスを復元
            return -1;
        }
    }

    /**
     * 入力ダイアログを表示
     * ユーザーからテキスト入力を受け付けるダイアログを表示
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

        } catch (InterruptedException | ExecutionException _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM, "入力ダイアログの表示中にエラーが発生", _e);
            Thread.currentThread().interrupt(); // 割り込みステータスを復元
            return null;
        }
    }

    /**
     * エンジニア登録完了時の選択ダイアログを表示します
     * <p>
     * このメソッドは、エンジニア情報の登録が成功した後にユーザーに表示される
     * ダイアログを生成・表示します。ユーザーは登録完了後の次のアクションとして
     * 3つの選択肢から選ぶことができます：
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
     * EDT上で直接登録完了ダイアログを表示（内部メソッド）
     * 
     * このメソッドは、イベントディスパッチスレッド(EDT)上から呼び出される場合に使用され、
     * ダイアログを直接表示して結果を返します。CompletableFutureのようなスレッド間通信
     * メカニズムを使用せず、同期的に処理を行うため、EDTブロックのリスクを回避
     * 
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

        } catch (Exception _e) {
            // ダイアログ表示中のエラー
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "直接ダイアログ表示中にエラーが発生しました: ID=" + engineer.getId(), _e);

            // エラー時はデフォルトアクションを返す
            return "CONTINUE";
        }
    }

    /**
     * 非EDT上から登録完了ダイアログを非同期的に表示（内部メソッド）
     * 
     * このメソッドは、イベントディスパッチスレッド(EDT)以外のスレッドから呼び出される場合に使用され、
     * CompletableFutureを使用してEDT上でダイアログを表示し、その結果を安全に待機
     * EDTをブロックすることなく、ダイアログの結果を非同期に取得することができます。
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

                } catch (Exception _e) {
                    // ダイアログ表示中のエラー
                    LogHandler.getInstance().logError(LogType.SYSTEM,
                            "ダイアログ表示中にエラーが発生しました: ID=" + engineer.getId(), _e);

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

        } catch (InterruptedException _e) {
            // 割り込み発生時
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "非同期ダイアログの待機中に割り込みが発生: ID=" + engineer.getId(), _e);
            Thread.currentThread().interrupt(); // 割り込みステータスを復元
            return "CONTINUE"; // エラー時はデフォルト動作

        } catch (ExecutionException _e) {
            // 実行時エラー
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "非同期ダイアログの実行中にエラーが発生: ID=" + engineer.getId(), _e);
            return "CONTINUE"; // エラー時はデフォルト動作

        } catch (Exception _e) {
            // その他の予期しないエラー
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "非同期ダイアログ処理中に予期しないエラーが発生: ID=" + engineer.getId(), _e);
            return "CONTINUE"; // エラー時はデフォルト動作
        }
    }

    /**
     * バリデーションエラーダイアログを表示
     * フォーム入力のバリデーションエラー時に特化したエラーダイアログを表示
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
     * システムエラーダイアログを表示
     * システム内部エラーをユーザーに通知
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
     * アクティブなフレームを取得
     * ダイアログの親コンポーネントとして使用するフレームを取得
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

    /**
     * インポートエラーの詳細表示ダイアログ
     * エラーになったデータの詳細情報を構造化して表示
     * 
     * @param errorEngineers エラーになったエンジニアデータのリスト
     * @return ユーザーが確認した場合はtrue
     */
    public boolean showImportErrorDetailDialog(List<EngineerDTO> errorEngineers) {
        if (errorEngineers == null || errorEngineers.isEmpty()) {
            return true;
        }

        try {
            // エラー詳細パネルの作成
            JPanel errorPanel = new JPanel(new BorderLayout());

            // ヘッダーメッセージ
            JLabel headerLabel = new JLabel(
                    String.format("インポート時に %d 件のエラーが発生：", errorEngineers.size()));
            headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            errorPanel.add(headerLabel, BorderLayout.NORTH);

            // エラーテーブルの作成
            String[] columnNames = { "行", "社員ID", "氏名", "エラー内容" };
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            for (EngineerDTO errorEngineer : errorEngineers) {
                String id = errorEngineer.getId() != null ? errorEngineer.getId() : "未設定";
                String name = errorEngineer.getName() != null ? errorEngineer.getName() : "未設定";
                String error = errorEngineer.getNote() != null ? errorEngineer.getNote() : "不明なエラー";

                // エラーメッセージから行番号を抽出（例：「バリデーションエラー（行 5）:」）
                String rowInfo = "不明";
                if (error.contains("行 ")) {
                    int startIndex = error.indexOf("行 ") + 2;
                    int endIndex = error.indexOf("）", startIndex);
                    if (endIndex > startIndex) {
                        rowInfo = error.substring(startIndex, endIndex);
                    }
                }

                tableModel.addRow(new Object[] { rowInfo, id, name, error });
            }

            JTable errorTable = new JTable(tableModel);
            errorTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            errorTable.getColumnModel().getColumn(0).setPreferredWidth(50); // 行
            errorTable.getColumnModel().getColumn(1).setPreferredWidth(100); // ID
            errorTable.getColumnModel().getColumn(2).setPreferredWidth(150); // 氏名
            errorTable.getColumnModel().getColumn(3).setPreferredWidth(400); // エラー

            JScrollPane scrollPane = new JScrollPane(errorTable);
            scrollPane.setPreferredSize(new Dimension(700, 300));
            errorPanel.add(scrollPane, BorderLayout.CENTER);

            // フッターメッセージ
            JLabel footerLabel = new JLabel(
                    "これらのデータはインポートされませんでした。修正後、再度インポートしてください。");
            footerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            errorPanel.add(footerLabel, BorderLayout.SOUTH);

            // ダイアログ表示
            int result = JOptionPane.showConfirmDialog(
                    getActiveFrame(),
                    errorPanel,
                    "インポートエラー詳細",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            return result == JOptionPane.OK_OPTION;

        } catch (Exception _e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "エラー詳細ダイアログの表示中にエラーが発生しました", _e);

            // フォールバック：シンプルなリスト表示
            return showScrollableListDialog(
                    "インポートエラー",
                    "以下のデータにエラーがありました：",
                    errorEngineers.stream()
                            .map(eng -> String.format("ID: %s, 氏名: %s - %s",
                                    eng.getId() != null ? eng.getId() : "未設定",
                                    eng.getName() != null ? eng.getName() : "未設定",
                                    eng.getNote() != null ? eng.getNote() : "エラー"))
                            .collect(Collectors.toList()));
        }
    }
}