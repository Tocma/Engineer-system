package util;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import util.Constants.UIConstants;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * フォーム関連のカスタムコンポーネントを提供するユーティリティクラス
 * 複数選択可能なコンボボックスとその関連クラスを定義
 * 
 * @author Nakano
 */
public final class FormComponentUtil {

    /** プライベートコンストラクタ */
    private FormComponentUtil() {
        throw new AssertionError("ユーティリティクラスはインスタンス化できません");
    }

    /**
     * プログラミング言語選択用のMultiSelectComboBoxを作成
     * 
     * @return 設定済みのMultiSelectComboBox
     */
    public static MultiSelectComboBox createLanguageComboBox() {
        String[] languages = DateOptionUtil.getAvailableLanguages();
        CheckableItem[] items = new CheckableItem[languages.length];

        for (int i = 0; i < languages.length; i++) {
            items[i] = new CheckableItem(languages[i]);
        }

        MultiSelectComboBox comboBox = new MultiSelectComboBox(items);
        // 定数を使用してサイズを統一管理
        comboBox.setPreferredSize(UIConstants.LANGUAGE_COMBO_SIZE);
        comboBox.setMinimumSize(UIConstants.LANGUAGE_COMBO_SIZE);
        comboBox.setMaximumSize(UIConstants.LANGUAGE_COMBO_SIZE);
        return comboBox;
    }

    /**
     * チェック可能なアイテムを表すクラス
     * コンボボックス内でチェックボックス機能を提供
     */
    public static class CheckableItem {
        private final String label;
        private boolean selected;

        public CheckableItem(String label) {
            this.label = label;
            this.selected = false;
        }

        public String getLabel() {
            return label;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * チェックボックス機能付きのレンダラークラス
     */
    public static class CheckBoxRenderer extends JCheckBox implements ListCellRenderer<CheckableItem> {
        @Override
        public Component getListCellRendererComponent(JList<? extends CheckableItem> list,
                CheckableItem value, int index, boolean isSelected, boolean cellHasFocus) {

            if (value == null) {
                setText("");
                setSelected(false);
                return this;
            }

            setText(value.getLabel());
            setSelected(value.isSelected());
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            return this;
        }
    }

    /**
     * 複数選択可能なコンボボックスクラス
     * チェックボックス機能を内蔵し、複数項目の選択状態を管理
     */
    public static class MultiSelectComboBox extends JComboBox<CheckableItem> {

        public MultiSelectComboBox(CheckableItem[] items) {
            super(items);

            // 表示レンダラー（カンマ区切り表示）
            this.setRenderer(new BasicComboBoxRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value,
                        int index, boolean isSelected, boolean cellHasFocus) {

                    if (index == -1) {
                        // コンボボックス上部の表示部分
                        return new JLabel(getSelectedLabels());
                    }
                    // ドロップダウンリスト内の表示
                    return new CheckBoxRenderer().getListCellRendererComponent(
                            list, (CheckableItem) value, index, isSelected, cellHasFocus);
                }
            });

            // 選択時の処理（選択された項目のチェック状態をトグル）
            addActionListener(_e -> {
                Object selected = getSelectedItem();
                if (selected instanceof CheckableItem item) {
                    item.setSelected(!item.isSelected());
                    repaint(); // 上部表示更新
                }
            });
        }

        /**
         * 選択された項目をカンマ区切りで返す
         * 
         * @return 選択項目の文字列表現
         */
        public String getSelectedLabels() {
            List<String> selected = new ArrayList<>();
            for (int i = 0; i < getModel().getSize(); i++) {
                CheckableItem item = getModel().getElementAt(i);
                if (item.isSelected()) {
                    selected.add(item.getLabel());
                }
            }
            return selected.isEmpty() ? "" : String.join(", ", selected);
        }

        /**
         * 選択された項目のリストを取得
         * 
         * @return 選択項目のリスト
         */
        public List<String> getSelectedItems() {
            List<String> selected = new ArrayList<>();
            for (int i = 0; i < getModel().getSize(); i++) {
                CheckableItem item = getModel().getElementAt(i);
                if (item.isSelected()) {
                    selected.add(item.getLabel());
                }
            }
            return selected;
        }

        /**
         * 指定した項目リストを選択状態に設定
         * 
         * @param selectedItems 選択する項目のリスト
         */
        public void setSelectedItems(List<String> selectedItems) {
            if (selectedItems == null) {
                return;
            }

            // すべての項目の選択状態をリセット
            for (int i = 0; i < getModel().getSize(); i++) {
                CheckableItem item = getModel().getElementAt(i);
                item.setSelected(false);
            }

            // 指定された項目を選択状態に設定
            for (int i = 0; i < getModel().getSize(); i++) {
                CheckableItem item = getModel().getElementAt(i);
                if (selectedItems.contains(item.getLabel())) {
                    item.setSelected(true);
                }
            }

            repaint();
        }

        /**
         * すべての選択状態をクリア
         */
        public void clearSelection() {
            for (int i = 0; i < getModel().getSize(); i++) {
                CheckableItem item = getModel().getElementAt(i);
                item.setSelected(false);
            }
            repaint();
        }
    }
}