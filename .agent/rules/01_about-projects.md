## このアプリケーションの概要
「PluginName」という、Minecraftのプラグインです。

## 主な技術スタック
- Kotlin (言語)
- Koin (依存性注入)
- PaperMC (MinecraftプラグインAPI)
- Velocity (MinecraftプロキシAPI)
- Cloud (コマンドフレームワーク)
- MCCoroutine (非同期処理)

## モジュール構成
- `common` - Paper/Velocity共通コード
- `paper` - Paper (Bukkit) プラグイン
- `velocity` - Velocity プロキシプラグイン
- `api` - 外部プラグイン向け公開API
