#!/bin/bash
# Engineer Management System 起動スクリプト for macOS（macOS機能強化版）

# スクリプトのディレクトリに移動
cd "$(dirname "$0")"

# ターミナルのタイトル設定
echo -ne "\033]0;エンジニア人材管理システム\007"

# 設定変数
JAR_FILE="target/engineer-system-5.0.0.jar"
LOCK_PORT=54321
APP_NAME="Engineer Management System"
MIN_JAVA_VERSION=17
NOTIFICATION_ENABLED=true

# カラー出力用の定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# macOS通知機能
send_notification() {
    local title="$1"
    local message="$2"
    local sound="${3:-default}"
    
    if [ "$NOTIFICATION_ENABLED" = true ] && command -v osascript &> /dev/null; then
        osascript -e "display notification \"$message\" with title \"$title\" sound name \"$sound\""
    fi
}

# macOSダイアログ表示
show_dialog() {
    local title="$1"
    local message="$2"
    local button_text="${3:-OK}"
    
    if command -v osascript &> /dev/null; then
        osascript -e "tell application \"System Events\" to display dialog \"$message\" with title \"$title\" buttons {\"$button_text\"} default button \"$button_text\" with icon caution"
    fi
}

# macOS選択ダイアログ
show_choice_dialog() {
    local title="$1"
    local message="$2"
    local choices="$3"
    
    if command -v osascript &> /dev/null; then
        local result=$(osascript -e "tell application \"System Events\" to display dialog \"$message\" with title \"$title\" buttons {$choices} default button 1")
        echo "$result"
    fi
}

# アプリケーションをDockで探す
find_app_in_dock() {
    local app_name="$1"
    
    # Dockでアプリケーションを探してフロントに移動
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
    echo "CPU: $(sysctl -n machdep.cpu.brand_string)"
    echo "メモリ: $(echo "$(sysctl -n hw.memsize) / 1024 / 1024 / 1024" | bc)GB"
    echo "Java: $JAVA_VERSION"
    echo "====================="
}

# プロセス詳細情報を取得
get_process_details() {
    local java_processes=$(pgrep -f "java.*EngineerSystem.jar")
    
    if [ ! -z "$java_processes" ]; then
        echo -e "${CYAN}=== 実行中のプロセス詳細 ===${NC}"
        echo "$java_processes" | while read pid; do
            if [ ! -z "$pid" ]; then
                echo -e "${WHITE}PID: $pid${NC}"
                echo "コマンド: $(ps -p $pid -o command | tail -n +2)"
                echo "開始時刻: $(ps -p $pid -o lstart | tail -n +2)"
                echo "CPU使用率: $(ps -p $pid -o %cpu | tail -n +2)%"
                echo "メモリ使用量: $(ps -p $pid -o rss | tail -n +2 | awk '{printf "%.1f MB", $1/1024}')"
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

# 起動通知
send_notification "$APP_NAME" "アプリケーションの起動を開始します"

# JARファイル確認
echo -e "${CYAN}実行ファイルの確認中...${NC}"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}エラー: 実行ファイルが見つかりません${NC}"
    send_notification "$APP_NAME" "エラー: 実行ファイルが見つかりません" "Basso"
    show_dialog "ビルドエラー" "実行ファイルが見つかりません。\n\n以下のコマンドでビルドしてください:\nmvn clean package" "OK"
    exit 1
fi

echo -e "${GREEN}✓ 実行ファイルを確認${NC}"

# Java環境確認
echo -e "${CYAN}Java環境の確認中...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}エラー: Javaが見つかりません${NC}"
    send_notification "$APP_NAME" "エラー: Javaが見つかりません" "Basso"
    show_dialog "Java未インストール" "Java ${MIN_JAVA_VERSION}以上をインストールしてください。\n\nダウンロード: https://adoptium.net/" "OK"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f 2)
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
JAVA_PROCESSES=$(pgrep -f "java.*EngineerSystem.jar")
PROCESS_FOUND=false
if [ ! -z "$JAVA_PROCESSES" ]; then
    PROCESS_FOUND=true
    echo -e "${YELLOW}⚠ 既存のJavaプロセスを検出しました${NC}"
fi

# 3. 重複起動が予想される場合の事前対応
if [ "$PORT_USED" = true ] || [ "$PROCESS_FOUND" = true ]; then
    echo
    echo -e "${YELLOW}=== 重複起動の可能性があります ===${NC}"
    
    get_process_details
    
    # ユーザーに選択肢を提示
    echo -e "${CYAN}対応方法を選択してください:${NC}"
    echo "1) 既存のアプリケーションを前面に表示"
    echo "2) 既存のアプリケーションを終了して新規起動"
    echo "3) そのまま起動を試行"
    echo "4) 起動をキャンセル"
    echo
    echo -n "選択 (1-4): "
    read -n 1 choice
    echo
    
    case $choice in
        1)
            echo -e "${CYAN}既存のアプリケーションを探しています...${NC}"
            if find_app_in_dock "Engineer"; then
                echo -e "${GREEN}✓ アプリケーションを前面に表示しました${NC}"
                send_notification "$APP_NAME" "既存のアプリケーションを前面に表示しました"
            else
                echo -e "${YELLOW}Dockでアプリケーションが見つかりませんでした${NC}"
                echo "手動でアプリケーションを探してください"
            fi
            exit 0
            ;;
        2)
            echo -e "${CYAN}既存のプロセスを終了しています...${NC}"
            if [ ! -z "$JAVA_PROCESSES" ]; then
                echo "$JAVA_PROCESSES" | xargs kill -TERM 2>/dev/null
                sleep 2
                # 強制終了が必要な場合
                REMAINING=$(pgrep -f "java.*EngineerSystem.jar")
                if [ ! -z "$REMAINING" ]; then
                    echo "$REMAINING" | xargs kill -KILL 2>/dev/null
                fi
                echo -e "${GREEN}✓ 既存のプロセスを終了しました${NC}"
            fi
            ;;
        3)
            echo -e "${YELLOW}そのまま起動を試行します...${NC}"
            ;;
        4)
            echo -e "${CYAN}起動をキャンセルしました${NC}"
            send_notification "$APP_NAME" "起動をキャンセルしました"
            exit 0
            ;;
        *)
            echo -e "${YELLOW}無効な選択です。そのまま起動を試行します...${NC}"
            ;;
    esac
    echo
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
        
        # 重複起動エラーの詳細ダイアログ
        show_dialog "重複起動エラー" "既にアプリケーションが起動しています。\n\nDockまたはアクティビティモニタで既存のアプリケーションを確認してください。" "了解"
        
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