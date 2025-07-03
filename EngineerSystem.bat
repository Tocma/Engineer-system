@echo off
:: Engineer Management System 起動スクリプト for Windows
:: 文字エンコーディングをUTF-8に設定
chcp 65001 >nul 2>&1

:: タイトル設定
title エンジニア人材管理システム

:: 実行ディレクトリをバッチファイルの場所に設定
cd /d "%~dp0"

echo.
echo ========================================
echo   エンジニア人材管理システム v5.0.0
echo ========================================
echo.

:: JARファイルの存在確認
set JAR_FILE=target\EngineerSystem.jar
if not exist "%JAR_FILE%" (
    echo エラー: 実行ファイルが見つかりません
    echo.
    echo 以下のコマンドでビルドしてください:
    echo   mvn clean package
    echo.
    echo ファイル: %JAR_FILE%
    echo.
    pause
    exit /b 1
)

:: Java実行環境の確認
echo Javaバージョンを確認中...
java -version >nul 2>&1
if errorlevel 1 (
    echo.
    echo エラー: Javaが見つかりません
    echo.
    echo Java 17以上をインストールしてください
    echo ダウンロード: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

:: Java バージョンチェック（簡易版）
for /f tokens^=3^ delims^=^" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%i
echo Javaバージョン: %JAVA_VERSION%
echo.

:: アプリケーション起動
echo アプリケーションを起動しています...
echo.

:: メモリ設定とともに起動
java -Xms512m -Xmx1024m -jar "%JAR_FILE%"

:: 終了コードの確認
if errorlevel 1 (
    echo.
    echo エラー: アプリケーションが異常終了しました
    echo 終了コード: %errorlevel%
    echo.
    pause
)

:: 正常終了
exit /b 0