# Version Control

- ブランチを切る際は、mainブランチから切り、プルリクエストは必ず mainブランチに対して行うこと
- ブランチを切ってから、作業を始める前に、mainブランチの最新の状態を取り込み、ブランチを切って作業をすること
- また、pushやprを作成する前に確認すること
- 別の作業があったとしても、できるだけすべてのファイルをステージングの対象とすること

## Repository
- [moripa-prometheus-exporter](https://github.com/morinoparty/moripa-prometheus-exporter)

## コミットメッセージ
- コミットメッセージは英語で書き、以下のような形式で書く。

```
emoji コミットの概要
```

例:
```
🎨 Add new method to get fish
```

## Issueについて

- 新しい機能を追加する場合は、Issueを作成してください。
- Issueは英語で書き、適切なラベルを追加してください。
- 現状存在しないラベルについては、勝手に作成しないでください
- どうしても必要である場合は、.github/labels.jsonに追加してください
