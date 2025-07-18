#!/bin/bash
# Engineer System 起動スクリプト for macOS（権限問題対応版）

# スクリプトのディレクトリに移動
cd "$(dirname "$0")"

# ターミナルのタイトル設定
echo -ne "\033]0;エンジニア人材管理システム\007"

# 設定変数
JAR_FILE="target/EngineerSystem-jar-with-dependencies.jar"
LOCK_PORT=54321
APP_NAME="Engineer System"
MIN_JAVA_VERSION=17
NOTIFICATION_ENABLED=true
SCRIPT_NAME="$(basename "$0")"

# カラー出力用の定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# セルフチェック機能: 実行権限とセキュリティ属性の確認
perform_self_check() {
    echo -e "${CYAN}=== システム権限チェック ===${NC}"
    
    local script_path="$0"
    local issues_found=false
    
    # 1. 実行権限の確認
    if [ ! -x "$script_path" ]; then
        echo -e "${YELLOW}⚠ 実行権限が不足しています${NC}"
        echo "修復を試行中..."
        if chmod +x "$script_path" 2>/dev/null; then
            echo -e "${GREEN}✓ 実行権限を設定しました${NC}"
        else
            echo -e "${RED}✗ 実行権限の設定に失敗しました${NC}"
            echo "手動で以下のコマンドを実行してください:"
            echo "chmod +x '$script_path'"
            issues_found=true
        fi
    else
        echo -e "${GREEN}✓ 実行権限: 正常${NC}"
    fi
    
    # 2. 隔離属性（Quarantine）の確認
    if command -v xattr >/dev/null 2>&1; then
        local quarantine_attr=$(xattr -p com.apple.quarantine "$script_path" 2>/dev/null)
        if [ ! -z "$quarantine_attr" ]; then
            echo -e "${YELLOW}⚠ 隔離属性が設定されています${NC}"
            echo "修復を試行中..."
            if xattr -d com.apple.quarantine "$script_path" 2>/dev/null; then
                echo -e "${GREEN}✓ 隔離属性を除去しました${NC}"
            else
                echo -e "${RED}✗ 隔離属性の除去に失敗しました${NC}"
                echo "手動で以下のコマンドを実行してください:"
                echo "xattr -d com.apple.quarantine '$script_path'"
                issues_found=true
            fi
        else
            echo -e "${GREEN}✓ 隔離属性: なし${NC}"
        fi
    fi
    
    # 3. Gatekeeperの状態確認
    if command -v spctl >/dev/null 2>&1; then
        local gatekeeper_status=$(spctl --status 2>/dev/null)
        if [[ "$gatekeeper_status" == *"enabled"* ]]; then
            echo -e "${YELLOW}⚠ Gatekeeper: 有効${NC}"
            echo "未署名スクリプトの実行が制限される可能性があります"
        else
            echo -e "${GREEN}✓ Gatekeeper: 無効または制限なし${NC}"
        fi
    fi
    
    # 4. 問題が見つかった場合の対処法表示
    if [ "$issues_found" = true ]; then
        echo
        echo -e "${RED}=== 手動対処が必要です ===${NC}"
        echo "以下の手順を実行してから再度起動してください:"
        echo
        echo "1. ターミナルを開く"
        echo "2. 以下のコマンドを実行:"
        echo "   cd '$(pwd)'"
        echo "   chmod +x '$SCRIPT_NAME'"
        echo "   xattr -d com.apple.quarantine '$SCRIPT_NAME'"
        echo
        echo "3. システム環境設定でセキュリティ許可が必要な場合:"
        echo "   システム環境設定 → セキュリティとプライバシー"
        echo "   「このまま開く」ボタンをクリック"
        echo
        read -p "対処後、Enterキーを押して続行してください..."
        echo
    fi
    
    echo -e "${CYAN}=========================${NC}"
    echo
}

# macOS通知機能（エラーハンドリング強化）
send_notification() {
    local title="$1"
    local message="$2"
    local sound="${3:-default}"
    
    if [ "$NOTIFICATION_ENABLED" = true ] && command -v osascript &> /dev/null; then
        osascript -e "display notification \"$message\" with title \"$title\" sound name \"$sound\"" 2>/dev/null
    fi
}

# macOSダイアログ表示（エラーハンドリング強化）
show_dialog() {
    local title="$1"
    local message="$2"
    local button_text="${3:-OK}"
    
    if command -v osascript &> /dev/null; then
        osascript -e "tell application \"System Events\" to display dialog \"$message\" with title \"$title\" buttons {\"$button_text\"} default button \"$button_text\" with icon caution" 2>/dev/null
    else
        echo "Dialog: $title - $message"
    fi
}

# セキュリティ問題解決ガイドの表示
show_security_guide() {
    local issue_type="$1"
    
    echo
    echo -e "${BLUE}========================================${NC}"
    echo -e "${WHITE}  macOSセキュリティ問題解決ガイド${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo
    
    case "$issue_type" in
        "gatekeeper")
            echo -e "${YELLOW}Gatekeeperによる実行制限が検出されました${NC}"
            echo
            echo "解決方法:"
            echo "1. システム環境設定を開く"
            echo "2. 「セキュリティとプライバシー」をクリック"
            echo "3. 「一般」タブを選択"
            echo "4. 画面下部に表示される警告メッセージで「このまま開く」をクリック"
            echo "5. 管理者パスワードを入力"
            echo
            echo "または、ターミナルから以下のコマンドで実行:"
            echo "bash '$(pwd)/$SCRIPT_NAME'"
            ;;
        "permission")
            echo -e "${YELLOW}ファイル権限の問題が検出されました${NC}"
            echo
            echo "解決方法:"
            echo "1. ターミナルを開く"
            echo "2. 以下のコマンドを実行:"
            echo "   cd '$(pwd)'"
            echo "   chmod +x '$SCRIPT_NAME'"
            ;;
        "quarantine")
            echo -e "${YELLOW}隔離属性の問題が検出されました${NC}"
            echo
            echo "解決方法:"
            echo "1. ターミナルを開く"
            echo "2. 以下のコマンドを実行:"
            echo "   cd '$(pwd)'"
            echo "   xattr -d com.apple.quarantine '$SCRIPT_NAME'"
            ;;
    esac
    
    echo
    echo -e "${BLUE}========================================${NC}"
    echo
}

# アプリケーションをDockで探す
find_app_in_dock() {
    local app_name="$1"
    
    if command -v osascript &> /dev/null; then
        osascript -e "
        tell application \"System Events\"
            set foundApp to false
            tell dock preferences
                set appList to name of every application process
                repeat with appName in appList
                    if appName contains \"$app_name\" or appName contains \"Engineer\" or appName contains \"java\" then
                        tell application process (appName as string)
                            set frontmost to true
                        end tell
                        set foundApp to true
                        exit repeat
                    end if
                end repeat
            end tell
            return foundApp
        end tell" 2>/dev/null
    fi
}

# システム情報を取得
get_system_info() {
    echo "=== システム情報 ==="
    echo "macOS: $(sw_vers -productVersion) ($(sw_vers -buildVersion))"
    echo "アーキテクチャ: $(uname -m)"
    echo "CPU: $(sysctl -n machdep.cpu.brand_string 2>/dev/null || echo '不明')"
    echo "メモリ: $(echo "$(sysctl -n hw.memsize 2>/dev/null || echo 0) / 1024 / 1024 / 1024" | bc 2>/dev/null || echo '不明')GB"
    echo "Java: ${JAVA_VERSION:-不明}"
    echo "====================="
}

# プロセス詳細情報を取得
get_process_details() {
    local java_processes=$(pgrep -f "java.*EngineerSystem-jar-with-dependencies.jar" 2>/dev/null)
    
    if [ ! -z "$java_processes" ]; then
        echo -e "${CYAN}=== 実行中のプロセス詳細 ===${NC}"
        echo "$java_processes" | while read pid; do
            if [ ! -z "$pid" ]; then
                echo -e "${WHITE}PID: $pid${NC}"
                echo "コマンド: $(ps -p $pid -o command 2>/dev/null | tail -n +2)"
                echo "開始時刻: $(ps -p $pid -o lstart 2>/dev/null | tail -n +2)"
                echo "CPU使用率: $(ps -p $pid -o %cpu 2>/dev/null | tail -n +2)%"
                echo "メモリ使用量: $(ps -p $pid -o rss 2>/dev/null | tail -n +2 | awk '{printf "%.1f MB", $1/1024}' 2>/dev/null)"
                echo "使用ポート: $(lsof -p $pid -i 2>/dev/null | grep LISTEN || echo 'なし')"
                echo "---"
            fi
        done
        echo -e "${CYAN}=========================${NC}"
    fi
}

# メイン処理開始
echo
echo -e "${BLUE}========================================${NC}"
echo -e "${WHITE}  エンジニア人材管理システム v5.0.0${NC}"
echo -e "${BLUE}========================================${NC}"
echo

# セルフチェックの実行
perform_self_check

# 起動通知
send_notification "$APP_NAME" "アプリケーションの起動を開始します"

# JARファイル確認
echo -e "${CYAN}実行ファイルの確認中...${NC}"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}エラー: 実行ファイルが見つかりません${NC}"
    echo "ファイル: $JAR_FILE"
    echo
    echo "以下の手順でビルドしてください:"
    echo "1. ターミナルでプロジェクトディレクトリに移動"
    echo "2. 'mvn clean package' を実行"
    echo
    send_notification "$APP_NAME" "エラー: 実行ファイルが見つかりません" "Basso"
    show_dialog "ビルドエラー" "実行ファイルが見つかりません。\n\n以下のコマンドでビルドしてください:\nmvn clean package" "OK"
    exit 1
fi

echo -e "${GREEN}✓ 実行ファイルを確認${NC}"

# Java環境確認
echo -e "${CYAN}Java環境の確認中...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}エラー: Javaが見つかりません${NC}"
    echo
    echo "Java ${MIN_JAVA_VERSION}以上をインストールしてください"
    echo "ダウンロード: https://adoptium.net/"
    echo
    send_notification "$APP_NAME" "エラー: Javaが見つかりません" "Basso"
    show_dialog "Java未インストール" "Java ${MIN_JAVA_VERSION}以上をインストールしてください。\n\nダウンロード: https://adoptium.net/" "OK"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f 2 2>/dev/null || echo "不明")
echo -e "${GREEN}✓ Java: $JAVA_VERSION${NC}"

# 重複起動チェック（詳細版）
echo -e "${CYAN}重複起動チェック中...${NC}"

# 1. ポートチェック
PORT_USED=false
if lsof -i :$LOCK_PORT >/dev/null 2>&1; then
    PORT_USED=true
    echo -e "${YELLOW}⚠ ポート $LOCK_PORT は使用中です${NC}"
fi

# 2. プロセスチェック
JAVA_PROCESSES=$(pgrep -f "java.*EngineerSystem-jar-with-dependencies.jar" 2>/dev/null)
PROCESS_FOUND=false
if [ ! -z "$JAVA_PROCESSES" ]; then
    PROCESS_FOUND=true
    echo -e "${YELLOW}⚠ 既存のJavaプロセスを検出しました${NC}"
fi

# 3. 重複起動が予想される場合の事前対応
if [ "$PORT_USED" = true ] || [ "$PROCESS_FOUND" = true ]; then
    echo
    echo -e "${YELLOW}=== 既にアプリケーションが起動しています。 ===${NC}"
    
    get_process_details
fi
    
# アプリケーション起動
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}アプリケーションを起動しています...${NC}"
echo -e "${BLUE}========================================${NC}"
echo

send_notification "$APP_NAME" "アプリケーションを起動中..."

# Java起動（macOS最適化設定付き）
java -Xms512m -Xmx1024m \
     -Xdock:name="エンジニア管理" \
     -Xdock:icon="icon.icns" \
     -Dapple.awt.application.name="エンジニア管理" \
     -Dapple.laf.useScreenMenuBar=true \
     -Dcom.apple.mrj.application.apple.menu.about.name="エンジニア管理" \
     -Dfile.encoding=UTF-8 \
     -Djava.awt.headless=false \
     -jar "$JAR_FILE"

# 終了処理
EXIT_CODE=$?
echo
echo -e "${BLUE}========================================${NC}"

case $EXIT_CODE in
    0)
        echo -e "${GREEN}✓ アプリケーションが正常に終了しました${NC}"
        send_notification "$APP_NAME" "アプリケーションが正常に終了しました"
        ;;
    1)
        echo -e "${RED}⚠ 重複起動が検出されました${NC}"
        echo
        get_process_details
        
        show_dialog "エンジニア人材管理システム" "既にアプリケーションが起動しています。\n\n対処方法：\n\n1. タスクバーで既存アプリを確認\n\n2. タスクマネージャーでプロセス終了\n\nポート番号: $LOCK_PORT" "OK"
        
        send_notification "$APP_NAME" "重複起動エラー: 既にアプリケーションが起動しています" "Basso"
        ;;
    130)
        echo -e "${YELLOW}⚠ ユーザーによって中断されました (Ctrl+C)${NC}"
        send_notification "$APP_NAME" "アプリケーションが中断されました"
        ;;
    *)
        echo -e "${RED}✗ アプリケーションが異常終了しました (終了コード: $EXIT_CODE)${NC}"
        echo
        get_system_info
        
        show_dialog "アプリケーションエラー" "アプリケーションが異常終了しました。\n\n終了コード: $EXIT_CODE\n\nログファイルを確認してください。" "了解"
        
        send_notification "$APP_NAME" "アプリケーションエラーが発生しました" "Sosumi"
        ;;
esac

echo -e "${BLUE}========================================${NC}"
exit $EXIT_CODE