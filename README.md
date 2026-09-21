# PluginTemplateKt

morinoparty の Minecraft プラグインテンプレートです。Paper / Velocity 両対応のマルチモジュール構成になっています。

## モジュール構成

| モジュール | 説明 |
|-----------|------|
| `common` | Paper/Velocity 共通コード |
| `paper` | Paper (Bukkit) プラグイン |
| `velocity` | Velocity プロキシプラグイン |
| `api` | 外部プラグイン向け公開 API |

## 技術スタック

- **Kotlin** - 言語
- **Paper API** 26.2 - Minecraft サーバー API
- **Velocity API** 4.1 - Minecraft プロキシ API
- **Cloud** - コマンドフレームワーク (Incendo)
- **Koin** - 依存性注入
- **MCCoroutine** - Kotlin Coroutines の Minecraft 統合
- **ShadowJar** - Fat JAR 生成
- **Fumadocs** - ドキュメントサイト (Next.js)

## 必要環境

- **Java** 25 (Temurin 推奨)
- **Gradle** 9.x (Wrapper 同梱)
- **Node.js** 26+ / **pnpm** 10+ (ドキュメントビルド用)
- **[Task](https://taskfile.dev/)** (タスクランナー、任意)

## セットアップ

### 1. テンプレートからリポジトリを作成

GitHub の "Use this template" ボタンからリポジトリを作成するか、クローンしてください。

### 2. プロジェクト名の変更

テンプレートのプレースホルダーを自分のプラグイン名に一括置換します。

```bash
# 例: "PluginName" を "MyPlugin" に変更する場合

# 1. settings.gradle.kts のルートプロジェクト名
#    rootProject.name = "MyPlugin"

# 2. パッケージ名の変更
#    party.morino.pluginname → party.morino.myplugin
#    ディレクトリ名も合わせてリネーム

# 3. クラス名の変更
#    PluginName → MyPlugin
#    PluginNameAPI → MyPluginAPI
#    PluginNameCommon → MyPluginCommon

# 4. 設定ファイルの更新
#    - paper/build.gradle.kts: main クラスのパス、website URL
#    - velocity PluginName.kt: @Plugin アノテーションの id, name
#    - CLAUDE.md: プロジェクト名、リポジトリ URL
#    - docs/app/layout.tsx: メタデータ
#    - docs/app/layout.config.tsx: タイトル、GitHub URL
#    - .github/workflows/preview.yml: PROJECT_NAME
#    - .github/workflows/upload.yml: JAR ファイル名
```

### 3. ビルド

```bash
# Gradle ビルド
./gradlew build -x test

# または Task を使用
task build
```

### 4. 開発サーバー起動

```bash
# Paper テストサーバー
./gradlew :paper:runServer

# または Task を使用
task run
```

### 5. ドキュメント開発

```bash
cd docs
pnpm install
pnpm dev

# または Task を使用
task docs
```

## 初期化チェックリスト

テンプレートから新しいプラグインを作成する際に行うべき作業の一覧です。

- [ ] リポジトリ名を変更
- [ ] `settings.gradle.kts` の `rootProject.name` を変更
- [ ] パッケージ名 `party.morino.pluginname` を変更
- [ ] ソースディレクトリ名をパッケージ名に合わせてリネーム
- [ ] クラス名 (`PluginName`, `PluginNameAPI`, `PluginNameCommon`) を変更
- [ ] `paper/build.gradle.kts` のメインクラスパス・website を変更
- [ ] `velocity/.../PluginName.kt` の `@Plugin` アノテーションを変更
- [ ] `CLAUDE.md` のプロジェクト説明とリポジトリ URL を変更
- [ ] `docs/app/layout.tsx` のメタデータを変更
- [ ] `docs/app/layout.config.tsx` のタイトルと GitHub URL を変更
- [ ] `docs/app/llms.txt/route.ts` のプラグイン名を変更
- [ ] `docs/package.json` の `name` を変更
- [ ] `.github/workflows/preview.yml` の `PROJECT_NAME` を変更
- [ ] `.github/workflows/upload.yml` の JAR ファイル名を変更
- [ ] GitHub リポジトリの Settings で GitHub Pages を有効化
- [ ] GitHub リポジトリの Secrets に S3 認証情報を設定 (プレビュー機能を使う場合)
- [ ] 不要な初期サンプルコード (`ExampleCommand`) を削除
- [ ] `config/spotless/license-header.kt` の著者名を変更し、`task license` でヘッダーを付け直す

## Task コマンド一覧

| コマンド | 説明 |
|---------|------|
| `task build` | 全モジュールをビルド |
| `task run` | Paper 開発サーバーを起動 |
| `task docs` | ドキュメント開発サーバーを起動 |
| `task check` | フォーマット + ビルド |
| `task clear` | session.lock ファイルを削除 |
| `task license` | ライセンスヘッダーを付与・更新 (`spotlessApply`) |
| `task license:check` | ライセンスヘッダーを検証 (`spotlessCheck`) |

## GitHub Actions

| ワークフロー | トリガー | 説明 |
|-------------|---------|------|
| `check_pull_request.yml` | Pull Request | ビルドチェック |
| `preview.yml` | Pull Request | プレビュービルド・S3 アップロード・PR コメント |
| `upload.yml` | Release published | GitHub Release にJAR をアップロード |
| `release.yml` | Push to master | Release Drafter でドラフトリリース作成 |
| `deploy_docs.yml` | Push to master (docs/) | GitHub Pages にドキュメントデプロイ |
| `dependabot_auto_merge.yml` | Dependabot PR | 自動マージ |
| `sync-label.yml` | labels.json 変更 | GitHub ラベル同期 |

## ライセンス

CC0-1.0

### ライセンスヘッダー

Kotlin ソースと `*.gradle.kts` には [Spotless](https://github.com/diffplug/spotless) でライセンスヘッダーを付与します。
ヘッダー本文は `config/spotless/license-header.kt` に一元管理されており、`$YEAR` が年に置き換わります (既存ファイルは `開始年-現在年` に更新されます)。

```bash
# ヘッダーを付与・更新
task license        # = ./gradlew spotlessApply

# ヘッダーの検証のみ
task license:check  # = ./gradlew spotlessCheck
```

`build` / `check` は失敗しない設定 (`isEnforceCheck = false`) なので、任意のタイミングで実行してください。
