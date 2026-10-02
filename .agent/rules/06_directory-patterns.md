## ディレクトリ配置規則

- docs/content/ にドキュメントを配置します。
- common/src/main/kotlin/party/morino/moripautils/common/ に共通コードを配置します。
- paper/src/main/kotlin/party/morino/moripautils/paper/ にPaperプラグインのコードを配置します。
- velocity/src/main/kotlin/party/morino/moripautils/velocity/ にVelocityプラグインのコードを配置します。
- api/src/main/kotlin/party/morino/moripautils/api/ にAPIのインターフェースを配置します。
- 機能ごとのコードは、各モジュール配下の機能名のパッケージ (例: `observability`, `ticket`) に配置します。
- data classはそれぞれのmodelに配置します。
