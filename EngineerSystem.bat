@echo off
:: Engineer Management System 起動スクリプト for Windows（重複起動対応改善版）
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

:: 重複起動チェック用のポート番号
set LOCK_PORT=54321

:: 既存プロセスのチェック（補助的）
echo 既存プロセスをチェック中...
tasklist /FI "IMAGENAME eq java.exe" | findstr /I "java.exe" >nul
if not errorlevel 1 (
    echo 注意: 他のJavaプロセスが実行中です
)

:: アプリケーション起動
echo アプリケーションを起動しています...
echo.

:: メモリ設定とともに起動
java -Xms512m -Xmx1024m -jar "%JAR_FILE%"

:: 終了コードの詳細確認
set EXIT_CODE=%errorlevel%

if %EXIT_CODE% equ 0 (
    echo.
    echo アプリケーションが正常に終了しました
    echo.
) else if %EXIT_CODE% equ 1 (
    echo.
    echo ========================================
    echo         重複起動が検出されました
    echo ========================================
    echo.
    echo 既にアプリケーションが起動しています。
    echo タスクバーまたはタスクマネージャーで
    echo 既存のアプリケーションを確認してください。
    echo.
    echo ポート番号: %LOCK_PORT%
    echo.
    echo 既存のアプリケーションを終了してから
    echo 再度起動してください。
    echo.
    pause
) else (
    echo.
    echo エラー: アプリケーションが異常終了しました
    echo 終了コード: %EXIT_CODE%
    echo.
    echo 詳細なエラー情報については、ログファイルを
    echo 確認してください。
    echo.
    pause
)

:: バッチファイル終了
exit /b %EXIT_CODE%