#!/bin/bash
# Engineer Management System 起動スクリプト for macOS（重複起動対応改善版）

# UTF-8エンコーディングを明示的に設定
export LANG=ja_JP.UTF-8
export LC_ALL=ja_JP.UTF-8

# スクリプトのディレクトリに移動
cd "$(dirname "$0")"

# ターミナルのタイトル設定
echo -ne "\033]0;エンジニア人材管理システム\007"

# カラー出力用の定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# エラー処理用の関数
print_error() {
    echo -e "${RED}エラー: $1${NC}" >&2
}

print_warning() {
    echo -e "${YELLOW}警告: $1${NC}"
}

print_success() {
    echo -e "${GREEN}$1${NC}"
}

print_info() {
    echo -e "${CYAN}$1${NC}"
}

# 区切り線を出力する関数
print_separator() {
    echo -e "${BLUE}========================================${NC}"
}

# ヘッダー表示
echo
print_separator
echo -e "${WHITE}  エンジニア人材管理システム v5.0.0${NC}"
print_separator
echo

# 設定変数
JAR_FILE="target/EngineerSystem.jar"
LOCK_PORT=54321
APP_NAME="Engineer Management System"
MIN_JAVA_VERSION=17

# JARファイルの存在確認
print_info "実行ファイルの確認中..."
if [ ! -f "$JAR_FILE" ]; then
    print_error "実行ファイルが見つかりません"
    echo
    echo -e "${YELLOW}以下のコマンドでビルドしてください:${NC}"
    echo -e "${WHITE}  mvn clean package${NC}"
    echo
    echo "ファイル: $JAR_FILE"
    echo
    echo -e "${CYAN}Press any key to exit...${NC}"
    read -n 1 -s
    exit 1
fi

print_success "✓ 実行ファイルを確認しました"

# Java実行環境の確認
print_info "Javaバージョンを確認中..."
if ! command -v java &> /dev/null; then
    echo
    print_error "Javaが見つかりません"
    echo
    echo "Java ${MIN_JAVA_VERSION}以上をインストールしてください"
    echo
    echo -e "${YELLOW}Homebrewを使用する場合:${NC}"
    echo -e "${WHITE}  brew install openjdk@${MIN_JAVA_VERSION}${NC}"
    echo
    echo -e "${YELLOW}または以下からダウンロード:${NC}"
    echo "  https://adoptium.net/"
    echo
    echo -e "${CYAN}Press any key to exit...${NC}"
    read -n 1 -s
    exit 1
fi

# Javaバージョンの表示と確認
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f 2)
print_success "✓ Javaバージョン: $JAVA_VERSION"

# バージョンチェック（Java 17以上）
JAVA_MAJOR_VERSION=$(echo $JAVA_VERSION | cut -d'.' -f1)
if [ "$JAVA_MAJOR_VERSION" -lt "$MIN_JAVA_VERSION" ]; then
    echo
    print_warning "Java ${MIN_JAVA_VERSION}以上を推奨します"
    echo "現在のバージョン: $JAVA_VERSION"
    echo
    echo -e "${CYAN}続行しますか？ (y/N): ${NC}"
    read -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "起動をキャンセルしました"
        exit 0
    fi
fi

# 既存プロセスのチェック（補助的）
print_info "既存プロセスをチェック中..."
EXISTING_JAVA_PROCESSES=$(pgrep -f "java.*EngineerSystem.jar" | wc -l | tr -d ' ')

if [ "$EXISTING_JAVA_PROCESSES" -gt 0 ]; then
    print_warning "既存のアプリケーションプロセスが検出されました (${EXISTING_JAVA_PROCESSES}個)"
    echo "プロセス一覧:"
    ps aux | grep "java.*EngineerSystem.jar" | grep -v grep | while read line; do
        echo "  $line"
    done
    echo
fi

# ポート使用状況の確認
print_info "ポート ${LOCK_PORT} の使用状況を確認中..."
if lsof -i :$LOCK_PORT >/dev/null 2>&1; then
    print_warning "ポート ${LOCK_PORT} は既に使用されています"
    echo "詳細:"
    lsof -i :$LOCK_PORT | head -5
    echo
fi

# アプリケーション起動前の最終確認
echo
print_separator
print_success "アプリケーションを起動しています..."
print_separator
echo

# メモリ設定とmacOS固有設定を含む起動コマンド
java -Xms512m -Xmx1024m \
     -Xdock:name="エンジニア管理" \
     -Dapple.awt.application.name="エンジニア管理" \
     -Dfile.encoding=UTF-8 \
     -jar "$JAR_FILE"

# 終了コードの詳細確認
EXIT_CODE=$?

echo
print_separator

if [ $EXIT_CODE -eq 0 ]; then
    print_success "アプリケーションが正常に終了しました"
    echo
elif [ $EXIT_CODE -eq 1 ]; then
    # 重複起動エラーの処理
    echo
    print_error "重複起動が検出されました"
    print_separator
    echo
    echo -e "${YELLOW}既にアプリケーションが起動しています。${NC}"
    echo
    echo -e "${CYAN}確認方法:${NC}"
    echo "• Dockでアプリケーションアイコンを探す"
    echo "• Cmd+Tab でアプリケーション切り替えを確認"
    echo "• アクティビティモニタでプロセスを確認"
    echo
    echo -e "${CYAN}対処方法:${NC}"
    echo "1. 既存のアプリケーションを見つけて前面に表示"
    echo "2. 既存のアプリケーションを終了してから再起動"
    echo
    echo -e "${WHITE}ポート番号: ${LOCK_PORT}${NC}"
    echo
    
    # 既存プロセスの詳細表示
    CURRENT_PROCESSES=$(pgrep -f "java.*EngineerSystem.jar")
    if [ ! -z "$CURRENT_PROCESSES" ]; then
        echo -e "${CYAN}現在実行中のプロセス:${NC}"
        echo "$CURRENT_PROCESSES" | while read pid; do
            if [ ! -z "$pid" ]; then
                ps -p $pid -o pid,ppid,command | tail -n +2
            fi
        done
        echo
        echo -e "${YELLOW}プロセスを強制終了する場合:${NC}"
        echo "  kill $CURRENT_PROCESSES"
        echo
    fi
    
    echo -e "${CYAN}Press any key to exit...${NC}"
    read -n 1 -s
elif [ $EXIT_CODE -eq 130 ]; then
    # Ctrl+C による中断
    echo
    print_warning "ユーザーによって中断されました (Ctrl+C)"
    echo
else
    # その他のエラー
    echo
    print_error "アプリケーションが異常終了しました"
    echo -e "${WHITE}終了コード: ${EXIT_CODE}${NC}"
    echo
    echo -e "${CYAN}トラブルシューティング:${NC}"
    echo "• ログファイルを確認してください"
    echo "• Java のバージョンが適切かを確認してください"
    echo "• 十分なメモリが利用可能かを確認してください"
    echo
    
    # システム情報の表示
    echo -e "${CYAN}システム情報:${NC}"
    echo "• Java バージョン: $JAVA_VERSION"
    echo "• 利用可能メモリ: $(vm_stat | grep 'Pages free' | awk '{print $3}' | sed 's/\.//' | awk '{printf "%.1f MB", $1 * 4096 / 1024 / 1024}')"
    echo "• macOS バージョン: $(sw_vers -productVersion)"
    echo
    
    echo -e "${CYAN}Press any key to exit...${NC}"
    read -n 1 -s
fi

print_separator

# スクリプト終了
exit $EXIT_CODE