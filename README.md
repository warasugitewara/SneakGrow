<div align="center">

# 🌱 SneakGrow

**しゃがむだけで、苗木・農作物・子供Mobの成長を早める Paper プラグイン / NeoForge MOD**

[![Build](https://github.com/warasugitewara/sneakgrow/actions/workflows/build.yml/badge.svg)](https://github.com/warasugitewara/sneakgrow/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/warasugitewara/sneakgrow?label=release)](https://github.com/warasugitewara/sneakgrow/releases/latest)
[![Paper](https://img.shields.io/badge/Paper-26.1.2-2C2C2C)](https://papermc.io/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.248%20%2F%201.21.1-F16436)](https://neoforged.net/)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

</div>

---

> [!NOTE]
> 本リポジトリは [Kamesuta/SneakGrow](https://github.com/Kamesuta/SneakGrow) の**フォーク**です。詳しくは [このフォークについて](#-このフォークについて) を参照してください。

## 目次

- [機能](#-機能)
- [このフォークについて](#-このフォークについて)
- [Paper 版](#-paper-版)
  - [導入方法](#-導入方法)
  - [設定 (config.yml)](#-設定-configyml)
  - [ビルド方法](#-ビルド方法)
- [NeoForge 版 (ATM10 / 1.21.1)](#-neoforge-版-atm10--1211)
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

## 🧩 Paper 版

### 📦 導入方法

1. [Releases](https://github.com/warasugitewara/sneakgrow/releases/latest) から最新の `sneakgrow-*.jar` をダウンロード
2. サーバーの `plugins` フォルダに配置
3. サーバーを起動 (または `/reload`) して `plugins/SneakGrow/config.yml` を必要に応じて編集

> [!IMPORTANT]
> 対応サーバーソフト: **Paper 26.1.2 以降**。Paper API を利用しているため Spigot 単体では動作しません。

### 🔧 設定 (config.yml)

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

### 🛠️ ビルド方法

JDK 25 と Maven が必要です。

```bash
mvn package
```

`target/sneakgrow-<version>.jar` にプラグインが出力されます。

## 🧱 NeoForge 版 (ATM10 / 1.21.1)

`neoforge/` に、**All the Mods 10 (Minecraft 1.21.1 / NeoForge 21.1.248)** 向けの移植版があります。

> [!IMPORTANT]
> **サーバーサイド完結**です。ブロック・アイテム・エンティティ・カスタムパケットを一切登録しないため、**サーバーの `mods` フォルダに入れるだけ**で動作します。クライアント側 (ATM10 インスタンス) への追加は不要で、MOD を持たないプレイヤーもそのまま接続できます。成長時のパーティクルもサーバーから送出します。

### 📦 導入方法

1. `sneakgrow-<version>.jar` をサーバーの `mods` フォルダに配置
2. サーバーを起動すると `config/sneakgrow-server.toml` が生成されます

依存はランタイムに **Kotlin for Forge 5.x** のみで、これは ATM10 に同梱済みです。

### 🔧 設定 (config/sneakgrow-server.toml)

ワールド個別に上書きしたい場合は `world/serverconfig/sneakgrow-server.toml` を使用します。

#### `[tweaks]`

| 設定項目 | デフォルト値 | 説明 |
|---|---|---|
| `cooldownTicks` | `5` | 成長判定を行う最短間隔 (tick) |
| `growSaplings` | `true` | 苗木 (`SaplingBlock`) を成長させるか |
| `growCrops` | `true` | 苗木以外の骨粉対象ブロック (農作物など) を成長させるか |
| `growMobs` | `true` | 子供Mob・繁殖クールダウン中のMobを成長させるか |
| `mobInstantGrow` | `true` | `true` で即時成体化 + 繁殖クールダウン解除 (Paper 版と同じ挙動) |
| `mobAgeStep` | `1200` | `mobInstantGrow=false` のとき1回で進める成長量 (tick) |
| `showParticles` | `true` | 成長時にパーティクルを表示するか |
| `blockRadius` | `5` | ブロックを判定する水平方向の範囲 |
| `blockHeight` | `2` | ブロックを判定する垂直方向の範囲 |
| `mobRadius` | `5` | Mobを判定する範囲 |
| `blockPercentage` | `0.1` | ブロック1つあたりの成長確率 |
| `mobPercentage` | `0.05` | Mob1体あたりの成長確率 |
| `maxBlocksPerTrigger` | `1024` | 1回のスニークで骨粉を適用する最大ブロック数 |
| `fireBonemealEvent` | `true` | `BonemealEvent` を発火し、土地保護MOD (FTB Chunks 等) に拒否権を与えるか |
| `requireGrowerType` | `true` | `BonemealableBlock.Type` が `GROWER` のものだけを対象にする (草ブロック等を除外) |

#### `[filter]`

| 設定項目 | デフォルト値 | 説明 |
|---|---|---|
| `mode` | `BLACKLIST` | `BLACKLIST` = 一覧を除外 / `WHITELIST` = 一覧のみ対象 |
| `blocks` | `[]` | 対象のブロックID (例: `"minecraft:wheat"`) |
| `namespaces` | `[]` | MOD単位で指定する名前空間 (例: `"mysticalagriculture"`) |

#### `[force]`

骨粉の成功判定 (`BonemealableBlock#isBonemealSuccess`) を無視する対象。MOD 側が骨粉を意図的に無効化している作物を成長させるための設定です。

| 設定項目 | デフォルト値 | 説明 |
|---|---|---|
| `blocks` | `[]` | 対象のブロックID |
| `namespaces` | `["mysticalagriculture"]` | MOD単位で指定する名前空間 |

> [!NOTE]
> `isValidBonemealTarget` は無視しないため、**MOD 独自の成長条件は守られます**。Mystical Agriculture であれば Crux ブロック・農地ティア・必要バイオームの判定はそのまま働きます。

#### `[botanypots]`

| 設定項目 | デフォルト値 | 説明 |
|---|---|---|
| `enabled` | `true` | Botany Pots のポットを成長対象にするか (`growCrops` も `true` である必要があります) |
| `minGrowthTicks` | `400` | 1回で進める成長量の下限 (tick) |
| `maxGrowthTicks` | `600` | 1回で進める成長量の上限 (tick) |
| `cooldownTicks` | `20` | 成長後にポット側へ設定するクールダウン (tick) |

デフォルト値は Botany Pots 本体の骨粉レシピ (`min_growth:400` / `max_growth:600` / `cooldown:20`) と同じです。本家と同様、**収穫までの最後の 20 tick は埋められない**ため、この機能だけでポットを収穫状態にすることはできません。

### ⌨️ コマンド

すべて OP レベル 2 以上が必要です。変更は即座に設定ファイルへ書き戻されます。

| コマンド | 説明 |
|---|---|
| `/sneakgrow` | 現在の設定を一覧表示 |
| `/sneakgrow reload` | 設定ファイルを再読み込み |
| `/sneakgrow set <key>` | 指定項目の現在値を表示 |
| `/sneakgrow set <key> <value>` | 指定項目を変更 (入力可能な範囲は設定定義から自動で反映) |
| `/sneakgrow filter` / `filter list` | フィルタの内容を表示 |
| `/sneakgrow filter mode <blacklist\|whitelist>` | フィルタの方式を切り替え |
| `/sneakgrow filter add <blockId>` | ブロックIDを一覧に追加 (補完あり) |
| `/sneakgrow filter remove <blockId>` | ブロックIDを一覧から削除 |
| `/sneakgrow filter addlook` | 視線先のブロックを一覧に追加 |
| `/sneakgrow filter addmod <namespace>` | MOD単位で一覧に追加 |
| `/sneakgrow filter removemod <namespace>` | MOD単位で一覧から削除 |
| `/sneakgrow filter clear` | フィルタを空にする |
| `/sneakgrow force` / `force list` | `[force]` の内容を表示 |
| `/sneakgrow force add\|remove <blockId>` | `[force]` にブロックIDを追加/削除 |
| `/sneakgrow force addlook` | 視線先のブロックを `[force]` に追加 |
| `/sneakgrow force addmod\|removemod <namespace>` | `[force]` にMOD単位で追加/削除 |
| `/sneakgrow force clear` | `[force]` を空にする |

未導入MODのブロックIDも登録できるよう、`add` はレジストリ上の存在を検証しません。

## 🧪 MOD 対応状況 (NeoForge 版)

| MOD | 状態 | 備考 |
|---|---|---|
| バニラの苗木・農作物 | ✅ | |
| 骨粉が効く modded 作物全般 | ✅ | `BonemealableBlock` を実装していれば自動対応 |
| **Mystical Agriculture** | ✅ | `MysticalCropBlock` は骨粉を意図的に無効化 (`isBonemealSuccess` が常に `false`) しているため、`[force]` に既定で登録して成功判定のみ回避します |
| **Botany Pots** | ✅ | ポットは `BonemealableBlock` ではなく BlockEntity 側で成長を管理するため、専用処理で対応します |

Botany Pots 連携はビルド時依存を持たず、サーバー起動時に API の有無を確認します。未導入の場合や API が想定と異なる場合はログに記録して連携のみ無効化し、他の成長処理には影響しません。連携状態は `/sneakgrow` の出力で確認できます。

### 🔄 Paper 版との違い

| | Paper 版 | NeoForge 版 |
|---|---|---|
| 起点 | `PlayerToggleSneakEvent` | `PlayerTickEvent.Post` でのスニーク状態ポーリング |
| 成長対象 | 苗木 + `age` プロパティを持つブロック | `BonemealableBlock` 全般 (modded 作物にも自動対応) |
| 骨粉適用 | `Block#applyBoneMeal(BlockFace)` | `BonemealableBlock` を直接呼び出し (同一手順) |
| 設定 | `config.yml` | `config/sneakgrow-server.toml` |
| 実行時変更 | なし | `/sneakgrow` コマンド |

> [!NOTE]
> Mob の成長について: Paper 版の `setAge(getAge() + 1)` は直後の `setBreed(true)` (CraftBukkit 実装では `setAge(0)`) に上書きされるため、実際の挙動は「段階的成長」ではなく「確率で即時成体化 + 繁殖クールダウン解除」でした。NeoForge 版はこれを既定として再現しつつ、`mobInstantGrow = false` で段階的成長に切り替えられます。

### 🛠️ ビルド方法

JDK 21 が必要です (Gradle Wrapper 同梱)。

```bash
cd neoforge
./gradlew build
```

`neoforge/build/libs/sneakgrow-<version>.jar` に MOD が出力されます。
`./gradlew runServer` で NeoForge 開発サーバーを起動して動作確認できます。

## 📄 ライセンス

[MIT License](LICENSE) — オリジナル作者: [Kamesuta](https://github.com/Kamesuta)
