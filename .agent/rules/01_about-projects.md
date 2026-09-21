## このアプリケーションの概要
「MoripaPrometheusExporter」という、Minecraftのプラグインです。
リポジトリ名は `moripa-prometheus-exporter` です。

## 目的
Minecraft サーバー (Paper) およびプロキシ (Velocity) の稼働状況を Prometheus 形式で公開し、Grafana で可視化するための exporter です。
主に以下のようなメトリクスを公開します。

- オンラインプレイヤー数 (Paper / Velocity)
- サーバーの TPS / MSPT (Paper)
- JVM のメモリ使用量などのランタイム情報

メトリクスは Prometheus からスクレイプされることを前提に設計してください。
メトリクス名は Prometheus の命名規約 (snake_case、単位を末尾に付ける、例: `minecraft_players_online`, `minecraft_tps`) に従ってください。

## 主な技術スタック
- Kotlin (言語)
- Koin (依存性注入)
- PaperMC (MinecraftプラグインAPI)
- Velocity (MinecraftプロキシAPI)
- Cloud (コマンドフレームワーク)
- MCCoroutine (非同期処理)
- Prometheus (メトリクス公開先。Grafana で可視化する)

## モジュール構成
- `common` - Paper/Velocity共通コード
- `paper` - Paper (Bukkit) プラグイン
- `velocity` - Velocity プロキシプラグイン
- `api` - 外部プラグイン向け公開API
