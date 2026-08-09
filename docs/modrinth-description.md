# SneakGrow

**Sneak to grow nearby saplings, crops, and baby mobs.**
しゃがむだけで、周囲の苗木・農作物・子供Mobの成長を早めます。

> This is a fork of [Kamesuta/SneakGrow](https://github.com/Kamesuta/SneakGrow), released under the MIT License.
> 本プロジェクトは [Kamesuta/SneakGrow](https://github.com/Kamesuta/SneakGrow) のフォークです (MIT License)。

---

## ✨ Features / 機能

- 🌾 Sneaking has a chance to grow nearby saplings and crops
  スニークするたびに、周囲の苗木・農作物を一定確率で成長させます
- 🐄 Sneaking has a chance to grow nearby baby mobs (and reset their breeding cooldown)
  周囲の子供Mobを一定確率で成長させ、繁殖可能状態にします
- ✨ Particle effects on growth (can be disabled)
  成長時のパーティクルエフェクト (無効化可能)
- ⚙️ Cooldown, radius, and probability are all configurable
  クールダウン・範囲・確率を細かく調整可能

---

## 📦 Editions / エディション

### Paper plugin

For **Minecraft 26.1.2**, requires **Java 25**.

Drop the jar into your `plugins` folder. Settings live in `plugins/SneakGrow/config.yml`.
`plugins` フォルダに入れるだけです。設定は `plugins/SneakGrow/config.yml` にあります。

Unlike the original, this fork uses the public Bukkit API (`Block#applyBoneMeal`) instead of NMS
reflection, so it no longer breaks on every server update.
オリジナルは NMS をリフレクション経由で呼んでいましたが、このフォークは公開 API を使うため、
サーバー更新のたびに壊れることがありません。

### NeoForge mod

For **Minecraft 1.21.1** (NeoForge 21.1.248), requires **Java 21**.

**Server-side only.** No blocks, items, entities, or custom packets are registered, so you only need
to place the jar in the server's `mods` folder. Clients do not need the mod installed, and vanilla
players can connect normally.
**サーバーサイド完結**です。サーバーの `mods` フォルダに入れるだけで動作し、クライアント側への
導入は不要です。MODを持たないプレイヤーもそのまま接続できます。

Additional features over the Paper edition:
Paper 版にはない機能:

- Works with **modded crops** automatically (any `BonemealableBlock`)
  Modded 作物にも自動対応
- Fires `BonemealEvent`, so land-protection mods are respected
  `BonemealEvent` を発火するため、土地保護MODと共存できます
- Whitelist / blacklist filtering, editable in-game via `/sneakgrow`
  `/sneakgrow` コマンドによるホワイトリスト・ブラックリスト管理
- Settings in `config/sneakgrow-server.toml` (per-world overrides supported)

Runtime dependency: **Kotlin For Forge 5.x** (already bundled with All the Mods 10).
ランタイム依存は **Kotlin For Forge 5.x** のみで、ATM10 には同梱済みです。

---

## 📄 License / ライセンス

[MIT License](https://github.com/warasugitewara/SneakGrow/blob/main/LICENSE) — original author:
[Kamesuta](https://github.com/Kamesuta)
