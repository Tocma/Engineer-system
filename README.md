# Engineer Management System

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Swing](https://img.shields.io/badge/GUI-Java%20Swing-blue.svg)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)]()
[![Version](https://img.shields.io/badge/Version-v4.15.11-brightgreen.svg)]()

エンジニア人材情報を効率的に管理するためのデスクトップアプリケーションです。直感的なGUIと豊富な機能を提供し、企業や組織でのエンジニア情報管理を強力にサポートします。

##  主な特徴

### 包括的なエンジニア情報管理
- **基本情報管理**: 社員ID、氏名、フリガナ、生年月日、入社年月
- **技術情報管理**: エンジニア歴、プログラミング言語、経歴、研修受講歴
- **評価情報管理**: 技術力、受講態度、コミュニケーション能力、リーダーシップ
- **カスタム情報**: 自由記述の備考欄

### 高機能な検索・フィルタリング
- **複合検索**: 複数条件を組み合わせた詳細検索
- **リアルタイム検索**: 入力と同時に結果を表示
- **ソート機能**: 任意の項目でのデータ並び替え
- **ページネーション**: 大量データの効率的な表示

### データ交換・連携機能
- **CSVインポート**: 既存データの一括取り込み
- **CSVエクスポート**: 選択データの外部出力
- **テンプレート生成**: 標準フォーマットのCSVテンプレート
- **重複データ処理**: インポート時の重複ID自動検出・上書き確認

### 堅牢な設計・品質保証
- **MVCアーキテクチャ**: 保守性の高い分離設計
- **包括的バリデーション**: 入力データの厳密な検証
- **詳細ログ管理**: 操作履歴の完全記録
- **エラーハンドリング**: 例外状況での安全な動作保証
- **ListenerManager統合**: 統合リスナー管理によるメモリ効率化

##  ユーザーインターフェース

### メイン画面構成
```
┌─────────────────────────────────────────────────┐
│    [新規追加] [取込] [テンプレ] [出力] [削除]　　　│
├─────────────────────────────────────────────────┤
│ 検索: [ID] [氏名] [生年月日] [エンジニア歴] [検索] │
├─────────────────────────────────────────────────┤
│   ┌─────┬─────────┬──────────┬─────┬─────────┐  │
│   │ ID  │   氏名  │ 生年月日 │歴年数│言語      │  │
│   ├─────┼─────────┼──────────┼─────┼─────────┤  │
│   │ID001│ 山田太郎 │1990-01-15│  5  │Java, 　 │  │
│   │ID002│ 佐藤花子 │1985-03-20│ 10  │C#,    　│  │
│   └─────┴─────────┴──────────┴─────┴─────────┘  │
├─────────────────────────────────────────────────┤
│            ページ ： 1 / 5 [前へ] [次へ] 　　　 　│
└─────────────────────────────────────────────────┘
```

### 機能別画面
- **エンジニア一覧画面**: 登録済みエンジニアの一覧表示・検索・操作
- **新規登録画面**: 新しいエンジニア情報の入力・登録
- **詳細・編集画面**: 既存情報の表示・編集・更新
- **インポート機能**: CSVファイルからの一括データ取り込み

##  クイックスタート

### 必要環境
- **Java**: JDK 17以上
- **OS**: Windows 10+, macOS 10.14+,
- **メモリ**: 最小512MB（推奨1GB以上）
- **ストレージ**: 1GB以上の空き容量

### インストール手順

1. **リポジトリのクローン**
```bash
git clone https://github.com/yourusername/engineer-system.git
cd engineer-system
```

2. **プロジェクトのビルド**
```bash
# Windows
javac -d bin -sourcepath src src/main/Main.java

# macOS/Linux  
javac -d bin -sourcepath src src/main/Main.java
```

3. **アプリケーションの実行**
```bash
# Windows
java -cp bin main.Main

# macOS/Linux
java -cp bin main.Main
```

##  使用方法

### 基本操作の流れ

#### 新規エンジニア登録
1. メイン画面で「**新規追加**」ボタンをクリック
2. 必須項目（社員ID、氏名、フリガナ、生年月日、入社年月、エンジニア歴、言語）を入力
3. 任意項目（経歴、研修歴、スキル評価、備考）を必要に応じて入力
4. 「**登録**」ボタンで保存
5. 完了ダイアログで次のアクション（続けて登録/一覧に戻る/詳細表示）を選択

#### エンジニア情報の検索
1. 検索バーに条件を入力（部分一致検索対応）
   - **社員ID**: 5桁以内の数値
   - **氏名**: 20文字以内の日本語
   - **生年月日**: 年・月・日の組み合わせ
   - **エンジニア歴**: 0-50年の範囲
2. 「**検索**」ボタンで結果表示
3. 「**検索終了**」で全データ表示に戻る

#### CSVデータの活用
1. **テンプレート作成**: 「**テンプレ**」→保存場所選択→空のCSVファイル生成
2. **データインポート**: 「**取込**」→CSVファイル選択→重複確認→一括登録
3. **データエクスポート**: エンジニア選択→「**出力**」→保存場所選択→CSV出力

### 高度な機能

#### 複数選択とバッチ操作
- **Ctrl+クリック**: 個別選択の追加・除外
- **Shift+クリック**: 範囲選択
- **選択後操作**: 一括削除、一括CSV出力

#### データ検証とエラー処理
- **リアルタイム検証**: 入力と同時にエラーチェック
- **詳細エラー表示**: 具体的な修正指示
- **安全な削除**: 確認ダイアログでの誤操作防止

##  アーキテクチャ

### システム設計思想
このシステムは**Model-View-Controller (MVC)パターン**を基盤とし、保守性と拡張性を重視した設計となっています。

```
┌─────────────────────────────────────────────────┐
│                   View Layer                    │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │
│ │  ListPanel  │ │  AddPanel   │ │ DetailPanel │ │
│ │             │ │             │ │             │ │
│ │ Abstract    │ │   ← extends │ │   ← extends │ │
│ │EngineerPanel│ │             │ │             │ │
│ └─────────────┘ └─────────────┘ └─────────────┘ │
└─────────────────────┬───────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────┐
│                Controller Layer                 │
│  ┌─────────────────┐ ┌───────────────────────┐  │
│  │ MainController  │ │ScreenTransitionCtrl   │  │
│  │                 │ │                       │  │
│  │ EngineerCtrl    │ │   ListenerManager     │  │
│  └─────────────────┘ └───────────────────────┘  │
└─────────────────────┬───────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────┐
│                 Model Layer                     │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │
│ │EngineerDTO  │ │EngineerDAO  │ │ CSVAccess   │ │
│ │             │ │             │ │             │ │
│ │EngineerBlder│ │ResourceMgr  │ │LogHandler   │ │
│ └─────────────┘ └─────────────┘ └─────────────┘ │
└─────────────────────────────────────────────────┘
```

### コア技術コンポーネント

#### バリデーションシステム
- **Strategy Pattern**: 項目別の検証ロジック分離
- **Chain of Responsibility**: 複数検証ルールの連鎖実行
- **国際化対応**: 日本語特有の文字種・形式検証

#### リソース管理
- **Singleton Pattern**: システム全体でのリソース一元管理
- **自動リソース解放**: アプリケーション終了時の安全なクリーンアップ
- **ファイルシステム統合**: データディレクトリの自動生成・管理

#### 非同期処理
- **SwingWorker**: UIブロッキング回避の背景処理
- **CompletableFuture**: 複雑な非同期フロー制御
- **Thread Safety**: マルチスレッド環境での安全なデータアクセス

#### ListenerManager統合
- **統合リスナー管理**: メモリリーク防止とパフォーマンス最適化
- **ライフサイクル管理**: 適切なリスナー登録・解除タイミング

### データ永続化戦略
```
Application Data
├── EngineerSystem/         # ホームディレクトリ配下
│   ├── data/              # CSVデータファイル
│   │   └── engineers.csv
│   └── logs/              # システムログ
│       └── system-2024-06-24.log
```

## 🔧 技術仕様

### 開発技術スタック
| カテゴリ | 技術 | 用途 |
|---------|------|------|
| **言語** | Java 17+ | アプリケーション開発 |
| **GUI** | Java Swing | ユーザーインターフェース |
| **アーキテクチャ** | MVC Pattern | 設計パターン |
| **データ形式** | CSV | データ交換・永続化 |
| **ログ管理** | java.util.logging | システム監視・デバッグ |
| **文字エンコーディング** | UTF-8 | 日本語対応 |

### パフォーマンス特性
- **データ容量**: 最大1,000件のエンジニア情報

### 対応データ形式

#### CSVファイル仕様
```csv
社員ID(必須),氏名(必須),フリガナ(必須),生年月日(必須),入社年月(必須),エンジニア歴(必須),扱える言語(必須),経歴,研修の受講歴,技術力,受講態度,コミュニケーション能力,リーダーシップ,備考
ID00001,山田太郎,ヤマダタロウ,1990-01-15,2020-04-01,5,Java;Python,Web開発,Spring研修,4.5,4.0,4.5,3.5,優秀なエンジニア
```

#### 入力検証ルール
- **社員ID**: ID + 5桁数字（例：ID00001）
- **氏名・フリガナ**: 20文字以内の日本語
- **日付**: YYYY-MM-DD形式、1950年以降
- **スキル評価**: 1.0-5.0の0.5刻み
- **プログラミング言語**: セミコロン区切りリスト

##  テスト・品質保証

### 自動テスト実行
```bash
# 統合テストモードでの実行
java -cp bin main.Main --test=core
```

### 手動テストシナリオ
1. **基本機能テスト**: CRUD操作の正常動作確認
2. **バリデーションテスト**: 異常入力での適切なエラー表示
3. **データ整合性テスト**: CSV入出力でのデータ保持
4. **ユーザビリティテスト**: 直感的な操作フロー

### ログ監視
```bash
# リアルタイムログ監視
tail -f ~/EngineerSystem/logs/system-$(date +%Y-%m-%d).log
```

##  貢献方法

### 開発参加の流れ
1. **Issue確認**: [Issues](https://github.com/yourusername/engineer-system/issues)で作業対象を選択
2. **開発・テスト**: 変更実装とテスト実行

### コーディング規約
- **Java Naming Conventions**: Oracle標準に準拠
- **コメント**: Javadoc形式での詳細記述
- **インデント**: 4スペース統一
- **文字エンコーディング**: UTF-8必須

### 推奨改善領域
- **データベース連携**: CSV → SQLite/H2 Database移行
- **Web UI**: Swing → Spring Boot + Thymeleaf
- **API化**: REST API提供でのシステム間連携

##  ライセンス

```
MIT License

Copyright (c) 2024 Engineer Management System

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## サポート・コミュニティ

### ヘルプとサポート
- **GitHub Issues**: [バグ報告・機能要望](https://github.com/yourusername/engineer-system/issues)
- **GitHub Discussions**: [質問・アイデア共有](https://github.com/yourusername/engineer-system/discussions)

### 更新履歴
- **v4.15.11** (最新): 新バリデーションシステム統合、検索機能強化
- **v4.13.0**: ListenerManager統合、リソース管理改善
- **v4.4.2**: ResourceManager統合、ファイル管理一元化

---

**Engineer System**は、現代の開発現場で求められるエンジニア情報管理の課題を解決するために設計されました。シンプルな操作性と堅牢な設計により、あらゆる規模の組織でご活用いただけます。