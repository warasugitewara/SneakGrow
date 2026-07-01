# SneakGrow

[![Build](https://github.com/warasugitewara/sneakgrow/actions/workflows/build.yml/badge.svg)](https://github.com/warasugitewara/sneakgrow/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

しゃがみ (スニーク) しているだけで、周囲の苗木・農作物の成長を早めたり、周囲の子供Mobを成長させたりする Paper/Spigot 用プラグインです。

## これは何のフォークか

本リポジトリは [Kamesuta/SneakGrow](https://github.com/Kamesuta/SneakGrow) のフォークです。
オリジナルのプラグインは非常にシンプルで便利ですが、内部で Minecraft サーバーの内部実装 (NMS) をリフレクションで直接呼び出しており、対応する Minecraft/Paper のバージョンが上がるたびに動かなくなる作りになっていました。

このフォークの目的は、**オリジナルの機能・設定・挙動はそのまま維持しつつ、最新の Paper (現時点で 26.1.2) でも動作するように追従させ、継続的にビルド・リリースできる状態を保つこと** です。機能追加や仕様変更は行っていません。

## オリジナルからの主な変更点

- 対応バージョンを Paper **26.1.2** に更新 (`pom.xml` の `paper-api` 依存関係、`plugin.yml` の `api-version`)
- ブロックへの骨粉(ボーンミール)適用処理を、バージョンごとに壊れやすい NMS リフレクション (`ReflectionUtil`) から、Bukkit 公式 API である [`Block#applyBoneMeal(BlockFace)`](https://jd.papermc.io/paper/26.1.2/org/bukkit/block/Block.html) を使う実装に置き換え、将来のバージョンアップへの追従を容易化
- ビルド環境を Paper 26.1.2 が要求する Java 25 に更新
- GitHub Actions によるビルド・リリース自動化を追加

## 機能

- スニークするたびに、周囲の苗木・農作物 (成長途中のブロック) を一定確率で成長させます
- スニークするたびに、周囲の子供Mobを一定確率で成長させます (繁殖可能状態にもなります)
- 成長時にパーティクルエフェクトを表示します (無効化可能)
- クールダウン・範囲・確率などは `config.yml` から調整できます

## 導入方法

1. [Releases](../../releases) から最新の `sneakgrow-*.jar` をダウンロード
2. サーバーの `plugins` フォルダに配置
3. サーバーを起動 (または `/reload`) して `plugins/SneakGrow/config.yml` を必要に応じて編集

対応サーバーソフト: Paper 26.1.2 以降 (Paper API を利用しているため Spigot 単体では動作しません)

## 設定 (config.yml)

```yaml
Tweaks:
  # 成長判定を行う間隔 (tick)
  cooldownTicks: 5
  # スニーク時に苗木を成長させるか
  growSaplings: true
  # スニーク時に農作物を成長させるか
  growCrops: true
  # 成長時にパーティクルエフェクトを表示するか
  showParticles: true
  # 苗木・農作物を判定する範囲 (ブロック)
  blockRadius: 5
  # 子供Mobを判定する範囲 (ブロック)
  mobRadius: 5
  # 苗木・農作物が成長する確率
  blockPercentage: 0.1
  # 子供Mobが成長する確率
  mobPercentage: 0.05
```

## ビルド方法

JDK 25 と Maven が必要です。

```bash
mvn package
```

`target/sneakgrow-<version>.jar` にプラグインが出力されます。

## ライセンス

[MIT License](LICENSE) — オリジナル作者: [Kamesuta](https://github.com/Kamesuta)
