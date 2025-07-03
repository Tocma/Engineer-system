#!/bin/bash
# Engineer Management System 起動スクリプト for macOS

# スクリプトのディレクトリに移動
cd "$(dirname "$0")"

# ターミナルのタイトル設定
echo -ne "\033]0;エンジニア人材管理システム\007"

# カラー出力用の定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo
echo "========================================"
echo "  エンジニア人材管理システム v5.0.0"
echo "========================================"
echo

# JARファイルの存在確認
JAR_FILE="target/EngineerSystem.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}エラー: 実行ファイルが見つかりません${NC}"
    echo
    echo "以下のコマンドでビルドしてください:"
    echo -e "${YELLOW}  mvn clean package${NC}"
    echo
    echo "ファイル: $JAR_FILE"
    echo
    echo "Press any key to exit..."
    read -n 1 -s
    exit 1
fi

# Java実行環境の確認
echo "Javaバージョンを確認中..."
if ! command -v java &> /dev/null; then
    echo
    echo -e "${RED}エラー: Javaが見つかりません${NC}"
    echo
    echo "Java 17以上をインストールしてください"
    echo "Homebrewを使用する場合:"
    echo -e "${YELLOW}  brew install openjdk@17${NC}"
    echo
    echo "または以下からダウンロード:"
    echo "  https://adoptium.net/"
    echo
    echo "Press any key to exit..."
    read -n 1 -s
    exit 1
fi

# Javaバージョンの表示
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f 2)
echo "Javaバージョン: $JAVA_VERSION"

# バージョンチェック（Java 17以上）
JAVA_MAJOR_VERSION=$(echo $JAVA_VERSION | cut -d'.' -f1)
if [ "$JAVA_MAJOR_VERSION" -lt 17 ]; then
    echo
    echo -e "${YELLOW}警告: Java 17以上を推奨します${NC}"
    echo "現在のバージョン: $JAVA_VERSION"
    echo
fi

# アプリケーション起動
echo
echo -e "${GREEN}アプリケーションを起動しています...${NC}"
echo

# メモリ設定とともに起動
java -Xms512m -Xmx1024m \
     -Xdock:name="エンジニア管理" \
     -Dapple.awt.application.name="エンジニア管理" \
     -jar "$JAR_FILE"

# 終了コードの確認
EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
    echo
    echo -e "${RED}エラー: アプリケーションが異常終了しました${NC}"
    echo "終了コード: $EXIT_CODE"
    echo
    echo "Press any key to exit..."
    read -n 1 -s
fi

# 正常終了
exit $EXIT_CODE