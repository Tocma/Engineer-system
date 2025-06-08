package util;

import util.LogHandler.LogType;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * リスナー管理を統合的に行うシングルトンクラス
 * 
 * このクラスは、アプリケーション全体のマウスリスナー、クリックリスナー、
 * その他のイベントリスナーを一元管理し、以下の機能を提供します：
 * 
 * - リスナーの登録・削除の統一管理
 * - リスナーの重複登録防止
 * - リソースリークの防止
 * - デバッグ情報の提供
 * - 自動クリーンアップ機能
 * 
 * @author Nakano
 */
public class ListenerManager {

    /** シングルトンインスタンス */
    private static final ListenerManager INSTANCE = new ListenerManager();

    /** リスナーIDの自動生成用カウンター */
    private final AtomicInteger listenerIdCounter = new AtomicInteger(0);

    /** コンポーネント別リスナー管理マップ */
    private final Map<Component, ComponentListenerInfo> componentListeners = new ConcurrentHashMap<>();

    /** リスナーID別管理マップ（逆引き用） */
    private final Map<String, ListenerRegistration> listenerRegistry = new ConcurrentHashMap<>();

    /**
     * リスナー登録情報を保持するクラス
     */
    public static class ListenerRegistration {
        private final String listenerId;
        private final Component component;
        private final EventListener listener;
        private final ListenerType type;
        private final String description;
        private final long registeredTime;

        public ListenerRegistration(String listenerId, Component component,
                EventListener listener, ListenerType type, String description) {
            this.listenerId = listenerId;
            this.component = component;
            this.listener = listener;
            this.type = type;
            this.description = description;
            this.registeredTime = System.currentTimeMillis();
        }

        // ゲッターメソッド
        public String getListenerId() {
            return listenerId;
        }

        public Component getComponent() {
            return component;
        }

        public EventListener getListener() {
            return listener;
        }

        public ListenerType getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public long getRegisteredTime() {
            return registeredTime;
        }
    }

    /**
     * コンポーネント別のリスナー情報を管理するクラス
     */
    private static class ComponentListenerInfo {
        private final Map<ListenerType, Set<String>> listenersByType = new EnumMap<>(ListenerType.class);
        private final Set<String> allListeners = new HashSet<>();

        public ComponentListenerInfo(Component component) {
            // 各リスナータイプのセットを初期化
            for (ListenerType type : ListenerType.values()) {
                listenersByType.put(type, new HashSet<>());
            }
        }

        public void addListener(String listenerId, ListenerType type) {
            listenersByType.get(type).add(listenerId);
            allListeners.add(listenerId);
        }

        public boolean removeListener(String listenerId, ListenerType type) {
            boolean removed = listenersByType.get(type).remove(listenerId);
            allListeners.remove(listenerId);
            return removed;
        }

        public Set<String> getAllListeners() {
            return new HashSet<>(allListeners);
        }

        public int getListenerCount() {
            return allListeners.size();
        }

        public int getListenerCount(ListenerType type) {
            return listenersByType.get(type).size();
        }
    }

    /**
     * サポートするリスナータイプの列挙型
     */
    public enum ListenerType {
        MOUSE_LISTENER("MouseListener", "マウスイベント（押下・離上・クリック・入入・退出）"),
        MOUSE_MOTION_LISTENER("MouseMotionListener", "マウス移動イベント（ドラッグ・移動）"),
        ACTION_LISTENER("ActionListener", "アクションイベント（ボタンクリックなど）"),
        KEY_LISTENER("KeyListener", "キーボードイベント（押下・離上・タイプ）"),
        FOCUS_LISTENER("FocusListener", "フォーカスイベント（取得・失失）"),
        COMPONENT_LISTENER("ComponentListener", "コンポーネントイベント（リサイズ・移動・表示・非表示）"),
        WINDOW_LISTENER("WindowListener", "ウィンドウイベント（開く・閉じる・アクティブ化など）");

        private final String typeName;
        private final String description;

        ListenerType(String typeName, String description) {
            this.typeName = typeName;
            this.description = description;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * プライベートコンストラクタ（シングルトンパターン）
     */
    private ListenerManager() {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "ListenerManagerを初期化しました");
    }

    /**
     * シングルトンインスタンスを取得
     * 
     * @return ListenerManagerの唯一のインスタンス
     */
    public static ListenerManager getInstance() {
        return INSTANCE;
    }

    /**
     * マウスリスナーを登録
     * 
     * @param component   対象コンポーネント
     * @param listener    マウスリスナー
     * @param description リスナーの説明（デバッグ用）
     * @return 登録されたリスナーのID
     */
    public String addMouseListener(Component component, MouseListener listener, String description) {
        return addListener(component, listener, ListenerType.MOUSE_LISTENER, description,
                () -> component.addMouseListener(listener),
                () -> component.removeMouseListener(listener));
    }

    /**
     * アクションリスナーを登録
     * 
     * @param component   対象コンポーネント（AbstractButtonまたはJTextField）
     * @param listener    アクションリスナー
     * @param description リスナーの説明（デバッグ用）
     * @return 登録されたリスナーのID
     */
    public String addActionListener(Component component, ActionListener listener, String description) {
        return addListener(component, listener, ListenerType.ACTION_LISTENER, description,
                () -> {
                    if (component instanceof AbstractButton) {
                        ((AbstractButton) component).addActionListener(listener);
                    } else if (component instanceof JTextField) {
                        ((JTextField) component).addActionListener(listener);
                    } else {
                        throw new IllegalArgumentException("コンポーネントはActionListenerをサポートしていません: "
                                + component.getClass().getSimpleName());
                    }
                },
                () -> {
                    if (component instanceof AbstractButton) {
                        ((AbstractButton) component).removeActionListener(listener);
                    } else if (component instanceof JTextField) {
                        ((JTextField) component).removeActionListener(listener);
                    }
                });
    }

    /**
     * キーリスナーを登録
     * 
     * @param component   対象コンポーネント
     * @param listener    キーリスナー
     * @param description リスナーの説明（デバッグ用）
     * @return 登録されたリスナーのID
     */
    public String addKeyListener(Component component, KeyListener listener, String description) {
        return addListener(component, listener, ListenerType.KEY_LISTENER, description,
                () -> component.addKeyListener(listener),
                () -> component.removeKeyListener(listener));
    }

    /**
     * 汎用リスナー登録メソッド
     * 
     * @param component    対象コンポーネント
     * @param listener     リスナー
     * @param type         リスナータイプ
     * @param description  説明
     * @param addAction    追加処理
     * @param removeAction 削除処理
     * @return リスナーID
     */
    private String addListener(Component component, EventListener listener, ListenerType type,
            String description, Runnable addAction, Runnable removeAction) {

        if (component == null || listener == null) {
            throw new IllegalArgumentException("コンポーネントとリスナーはnullにできません");
        }

        // リスナーIDを生成
        String listenerId = generateListenerId(type);

        try {
            // 実際にリスナーを追加
            addAction.run();

            // 管理情報に登録
            ListenerRegistration registration = new ListenerRegistration(
                    listenerId, component, listener, type, description);
            listenerRegistry.put(listenerId, registration);

            // コンポーネント別情報を更新
            ComponentListenerInfo componentInfo = componentListeners.computeIfAbsent(
                    component, ComponentListenerInfo::new);
            componentInfo.addListener(listenerId, type);

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    String.format("リスナーを登録しました: ID=%s, Type=%s, Component=%s, Description=%s",
                            listenerId, type.getTypeName(), component.getClass().getSimpleName(), description));

            return listenerId;

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "リスナーの登録に失敗しました: " + description, e);
            throw new RuntimeException("リスナー登録エラー", e);
        }
    }

    /**
     * リスナーを削除
     * 
     * @param listenerId 削除するリスナーのID
     * @return 削除に成功した場合true
     */
    public boolean removeListener(String listenerId) {
        if (listenerId == null || listenerId.trim().isEmpty()) {
            return false;
        }

        ListenerRegistration registration = listenerRegistry.get(listenerId);
        if (registration == null) {
            LogHandler.getInstance().log(Level.WARNING, LogType.SYSTEM,
                    "削除対象のリスナーが見つかりません: " + listenerId);
            return false;
        }

        try {
            // 実際にリスナーを削除
            removeListenerFromComponent(registration);

            // 管理情報から削除
            listenerRegistry.remove(listenerId);

            ComponentListenerInfo componentInfo = componentListeners.get(registration.getComponent());
            if (componentInfo != null) {
                componentInfo.removeListener(listenerId, registration.getType());

                // コンポーネントにリスナーが残っていない場合は情報を削除
                if (componentInfo.getListenerCount() == 0) {
                    componentListeners.remove(registration.getComponent());
                }
            }

            LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                    "リスナーを削除しました: " + listenerId);

            return true;

        } catch (Exception e) {
            LogHandler.getInstance().logError(LogType.SYSTEM,
                    "リスナーの削除に失敗しました: " + listenerId, e);
            return false;
        }
    }

    /**
     * コンポーネントからリスナーを実際に削除
     */
    private void removeListenerFromComponent(ListenerRegistration registration) {
        Component component = registration.getComponent();
        EventListener listener = registration.getListener();
        ListenerType type = registration.getType();

        switch (type) {
            case MOUSE_LISTENER:
                component.removeMouseListener((MouseListener) listener);
                break;
            case MOUSE_MOTION_LISTENER:
                component.removeMouseMotionListener((MouseMotionListener) listener);
                break;
            case ACTION_LISTENER:
                if (component instanceof AbstractButton) {
                    ((AbstractButton) component).removeActionListener((ActionListener) listener);
                } else if (component instanceof JTextField) {
                    ((JTextField) component).removeActionListener((ActionListener) listener);
                }
                break;
            case KEY_LISTENER:
                component.removeKeyListener((KeyListener) listener);
                break;
            case FOCUS_LISTENER:
                component.removeFocusListener((FocusListener) listener);
                break;
            case COMPONENT_LISTENER:
                component.removeComponentListener((ComponentListener) listener);
                break;
            case WINDOW_LISTENER:
                if (component instanceof Window) {
                    ((Window) component).removeWindowListener((WindowListener) listener);
                }
                break;
        }
    }

    /**
     * 指定されたコンポーネントのすべてのリスナーを削除
     * 
     * @param component 対象コンポーネント
     * @return 削除されたリスナー数
     */
    public int removeAllListeners(Component component) {
        if (component == null) {
            return 0;
        }

        ComponentListenerInfo componentInfo = componentListeners.get(component);
        if (componentInfo == null) {
            return 0;
        }

        Set<String> listenerIds = new HashSet<>(componentInfo.getAllListeners());
        int removedCount = 0;

        for (String listenerId : listenerIds) {
            if (removeListener(listenerId)) {
                removedCount++;
            }
        }

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                String.format("コンポーネントのリスナーをすべて削除しました: %s, 削除数=%d",
                        component.getClass().getSimpleName(), removedCount));

        return removedCount;
    }

    /**
     * すべてのリスナーを削除（クリーンアップ用）
     * 
     * @return 削除されたリスナー数
     */
    public int removeAllListeners() {
        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "全リスナーのクリーンアップを開始します: 総数=" + listenerRegistry.size());

        Set<String> allListenerIds = new HashSet<>(listenerRegistry.keySet());
        int removedCount = 0;

        for (String listenerId : allListenerIds) {
            if (removeListener(listenerId)) {
                removedCount++;
            }
        }

        // 管理用データ構造もクリア
        componentListeners.clear();
        listenerRegistry.clear();

        LogHandler.getInstance().log(Level.INFO, LogType.SYSTEM,
                "全リスナーのクリーンアップが完了しました: 削除数=" + removedCount);

        return removedCount;
    }

    /**
     * リスナーIDを生成
     */
    private String generateListenerId(ListenerType type) {
        int id = listenerIdCounter.incrementAndGet();
        return String.format("LISTENER_%s_%04d", type.name(), id);
    }

    /**
     * 管理状況のデバッグ情報を取得
     * 
     * @return デバッグ情報の文字列
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== ListenerManager Debug Info ===\n");
        info.append("総リスナー数: ").append(listenerRegistry.size()).append("\n");
        info.append("管理中コンポーネント数: ").append(componentListeners.size()).append("\n\n");

        // リスナータイプ別の統計
        Map<ListenerType, Integer> typeStats = new EnumMap<>(ListenerType.class);
        for (ListenerRegistration registration : listenerRegistry.values()) {
            typeStats.merge(registration.getType(), 1, Integer::sum);
        }

        info.append("リスナータイプ別統計:\n");
        for (Map.Entry<ListenerType, Integer> entry : typeStats.entrySet()) {
            info.append("  ").append(entry.getKey().getTypeName())
                    .append(": ").append(entry.getValue()).append("個\n");
        }

        info.append("\nコンポーネント別詳細:\n");
        for (Map.Entry<Component, ComponentListenerInfo> entry : componentListeners.entrySet()) {
            Component component = entry.getKey();
            ComponentListenerInfo componentInfo = entry.getValue();

            info.append("  ").append(component.getClass().getSimpleName())
                    .append(" (").append(System.identityHashCode(component)).append(")")
                    .append(": ").append(componentInfo.getListenerCount()).append("個\n");

            for (ListenerType type : ListenerType.values()) {
                int count = componentInfo.getListenerCount(type);
                if (count > 0) {
                    info.append("    ").append(type.getTypeName())
                            .append(": ").append(count).append("個\n");
                }
            }
        }

        return info.toString();
    }

    /**
     * 指定されたコンポーネントのリスナー情報を取得
     * 
     * @param component 対象コンポーネント
     * @return リスナー情報のリスト
     */
    public java.util.List<ListenerRegistration> getListenerInfo(Component component) {
        if (component == null) {
            return new ArrayList<>();
        }

        ComponentListenerInfo componentInfo = componentListeners.get(component);
        if (componentInfo == null) {
            return new ArrayList<>();
        }

        java.util.List<ListenerRegistration> result = new ArrayList<>();
        for (String listenerId : componentInfo.getAllListeners()) {
            ListenerRegistration registration = listenerRegistry.get(listenerId);
            if (registration != null) {
                result.add(registration);
            }
        }

        return result;
    }

    /**
     * 登録されているリスナー数を取得
     * 
     * @return 総リスナー数
     */
    public int getTotalListenerCount() {
        return listenerRegistry.size();
    }

    /**
     * 管理中のコンポーネント数を取得
     * 
     * @return コンポーネント数
     */
    public int getManagedComponentCount() {
        return componentListeners.size();
    }
}