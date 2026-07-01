<div align="center">

# 🌱 SneakGrow

**しゃがむだけで、苗木・農作物・子供Mobの成長を早める Paper プラグイン**

[![Build](https://github.com/warasugitewara/sneakgrow/actions/workflows/build.yml/badge.svg)](https://github.com/warasugitewara/sneakgrow/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/warasugitewara/sneakgrow?label=release)](https://github.com/warasugitewara/sneakgrow/releases/latest)
[![Paper](https://img.shields.io/badge/Paper-26.1.2-2C2C2C)](https://papermc.io/)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

</div>

---

> [!NOTE]
> 本リポジトリは [Kamesuta/SneakGrow](https://github.com/Kamesuta/SneakGrow) の**フォーク**です。詳しくは [このフォークについて](#-このフォークについて) を参照してください。

## 目次

- [機能](#-機能)
- [このフォークについて](#-このフォークについて)
- [導入方法](#-導入方法)
- [設定 (config.yml)](#-設定-configyml)
- [ビルド方法](#-ビルド方法)
- [ライセンス](#-ライセンス)

## ✨ 機能

- 🌾 スニークするたびに、周囲の苗木・農作物 (成長途中のブロック) を一定確率で成長させる
- 🐄 スニークするたびに、周囲の子供Mobを一定確率で成長させる (繁殖可能状態にもなる)
- ✨ 成長時にパーティクルエフェクトを表示 (無効化可能)
- ⚙️ クールダウン・範囲・確率などを `config.yml` から細かく調整可能

## 🍴 このフォークについて

オリジナルの [Kamesuta/SneakGrow](https://github.com/Kamesuta/SneakGrow) は非常にシンプルで便利なプラグインですが、内部で Minecraft サーバーの内部実装 (NMS) をリフレクションで直接呼び出しており、対応する Minecraft/Paper のバージョンが上がるたびに動かなくなる作りになっていました。

このフォークの目的は、**オリジナルの機能・設定・挙動をそのまま維持しつつ、最新の Paper でも動作するように追従させ、継続的にビルド・リリースできる状態を保つこと** です。機能追加や仕様変更は行っていません。

| | オリジナル | このフォーク |
|---|---|---|
| 対応 Paper バージョン | 1.21.8 | **26.1.2** |
| 骨粉の適用処理 | バージョン依存の NMS リフレクション (`ReflectionUtil`) | Bukkit 公式 API [`Block#applyBoneMeal(BlockFace)`](https://jd.papermc.io/paper/26.1.2/org/bukkit/block/Block.html) |
| ビルド用 Java | 8 | 25 (Paper 26.1.2 の要件) |
| CI/CD | なし | GitHub Actions によるビルド確認・リリース自動化 |

## 📦 導入方法

1. [Releases](https://github.com/warasugitewara/sneakgrow/releases/latest) から最新の `sneakgrow-*.jar` をダウンロード
2. サーバーの `plugins` フォルダに配置
3. サーバーを起動 (または `/reload`) して `plugins/SneakGrow/config.yml` を必要に応じて編集

> [!IMPORTANT]
> 対応サーバーソフト: **Paper 26.1.2 以降**。Paper API を利用しているため Spigot 単体では動作しません。

## 🔧 設定 (config.yml)

| 設定項目 | デフォルト値 | 説明 |
|---|---|---|
| `cooldownTicks` | `5` | 成長判定を行う間隔 (tick) |
| `growSaplings` | `true` | スニーク時に苗木を成長させるか |
| `growCrops` | `true` | スニーク時に農作物を成長させるか |
| `showParticles` | `true` | 成長時にパーティクルエフェクトを表示するか |
| `blockRadius` | `5` | 苗木・農作物を判定する範囲 (ブロック) |
| `mobRadius` | `5` | 子供Mobを判定する範囲 (ブロック) |
| `blockPercentage` | `0.1` | 苗木・農作物が成長する確率 |
| `mobPercentage` | `0.05` | 子供Mobが成長する確率 |

<details>
<summary>config.yml の実際の内容</summary>

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

</details>

## 🛠️ ビルド方法

JDK 25 と Maven が必要です。

```bash
mvn package
```

`target/sneakgrow-<version>.jar` にプラグインが出力されます。

## 📄 ライセンス

[MIT License](LICENSE) — オリジナル作者: [Kamesuta](https://github.com/Kamesuta)
