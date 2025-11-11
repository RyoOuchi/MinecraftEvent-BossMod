# 📘 コードドキュメント

## 概要
`OnMinecraftInstanceLaunched` クラスは、Minecraftのワールドが読み込まれた際に実行されるイベントハンドラを提供します。このイベントはサーバワールドの起動時に発火し、保存データに記録されているサーバブロックの位置を検証して、既に存在しない不正なエントリを削除します。主な処理の流れは以下の通りです。

1. ワールド読み込みイベント (`WorldEvent.Load`) を受け取る。
2. レベルがサーバサイドであることを確認し、サーバセーブデータ (`ServerSavedData`) を取得。
3. 登録されているサーバ位置をチェックし、既に正規のサーバブロック (`ExampleMod.SERVER_BLOCK`) が無い場所をリストアップ。
4. 不正なサーバエントリを削除し、削除した数をログに出力。

この処理により、ワールドロード時にセーブデータの整合性を保ち、不要なデータをクリーンアップします。

---

## 🌐 グローバル変数一覧
このクラスにはインスタンスフィールドや静的なグローバル変数はありません。全ての状態はメソッド内のローカル変数で管理されています。

---

## 🧩 関数一覧

### `@SubscribeEvent static void onWorldLoad(WorldEvent.Load event)`
- **概要**: ワールドが読み込まれたときに呼び出されるイベントハンドラ。サーバワールドの場合、登録されているサーバブロックの位置を検証し、不正なものを削除します。
- **引数**: `event` – ワールド読み込みイベント (`WorldEvent.Load`)。イベントからワールド (`LevelAccessor`) を取得可能。
- **処理内容**:
    1. 「World instance launched!」というログを出力してイベント発火を確認。
    2. `event.getWorld()` から `Level` オブジェクトを取得し、サーバ側 (`!level.isClientSide()`) かどうかを確認。
    3. サーバワールド (`ServerLevel` にキャスト) から `ServerSavedData` を取得。
    4. `getBlockPos(level, data)` を呼び出して、登録されているサーバ位置から不正なものを収集。
    5. 収集した位置について `data.removeServer()` を呼び、セーブデータからサーバ情報を削除。
    6. 削除件数が 0 でない場合、「🧹 Cleaned up X invalid servers from saved data.」とログに出力。
- **呼び出し元**: Forge のイベントバス。`@Mod.EventBusSubscriber` と `@SubscribeEvent` により自動的に登録されます。
- **呼び出し先**: `getBlockPos()`、`ServerSavedData.get()`。

### `private static List<BlockPos> getBlockPos(Level level, ServerSavedData data)`
- **概要**: セーブデータに登録されているサーバブロックの位置を検証し、実際のワールドに存在するブロックが正しいサーバブロック (`ExampleMod.SERVER_BLOCK`) でない場合、その位置を返します。
- **引数**:
    - `level` – 現在のサーバワールド。
    - `data` – サーバセーブデータ (`ServerSavedData`)。登録されているサーバ名と位置を保持する。
- **戻り値**: 不正なサーバ位置 (`BlockPos`) のリスト。
- **処理内容**:
    1. `data.getServers()` からサーバ名とその位置のマップを取得。
    2. 各サーバ位置について `level.getBlockState(serverBlockPos).getBlock()` を呼び出し、現在のブロックを取得。
    3. 取得したブロックが `ExampleMod.SERVER_BLOCK` と一致する場合は有効なサーバとしてログ出力。そうでなければ無効と判断しログ出力後、削除対象リストに追加。
    4. すべてのサーバに対してチェックを行い、削除対象リストを返す。
- **呼び出し元**: `onWorldLoad()`。
- **備考**: 削除対象リストに追加する際、ConcurrentModificationExceptionを避けるために `forEach` ループ内では実際の削除を行わず、後でまとめて削除します。

---

## 🔁 呼び出し関係図（関数依存）
```
WorldEvent.Load event
└─ onWorldLoad()
├─ event.getWorld() → LevelAccessor
├─ ServerSavedData.get((ServerLevel) level)
├─ getBlockPos(level, data)
├─ toRemove.forEach(data::removeServer)
└─ log cleanup

getBlockPos(level, data)
├─ data.getServers()
├─ level.getBlockState(serverBlockPos).getBlock()
└─ checks ExampleMod.SERVER_BLOCK
```
---

## ⚙️ 外部依存関係
| クラス／ライブラリ | 用途 |
|------------------|-----|
| **`WorldEvent.Load` (Forge)** | ワールド読み込み時に発火するイベントクラス。`@SubscribeEvent` でハンドラ登録に使用。 |
| **`Mod.EventBusSubscriber` (Forge)** | イベントバスに登録するためのアノテーション。このクラスを自動的にイベントリスナーとして認識させる。 |
| **`ServerSavedData`** | Mod 独自のセーブデータ管理クラス。サーバ名とブロック位置のマッピングを永続的に保存し、取得・追加・削除を行う。 |
| **`ExampleMod.SERVER_BLOCK`** | 正しいサーバブロックを表す定数。ブロックの型チェックに使用。 |
| **`Level`, `ServerLevel`, `BlockPos`, `BlockState`** | Minecraft のワールド・座標・ブロック状態を扱うクラス。 |
| **Java 標準ライブラリ (`List`, `ArrayList`, `Map`)** | サーバ位置のリストアップやマップ操作に使用。 |

---

## 📄 ファイル別概要
| ファイル名／クラス名 | 主な責務 | 主なメソッド |
|--------------------|-----------|--------------|
| **`OnMinecraftInstanceLaunched.java`** | ワールド読み込みイベントに応じてサーバセーブデータを検証し、不整合なサーバ位置を削除する。 | `onWorldLoad()`, `getBlockPos()` |

---

## 💬 総評
`OnMinecraftInstanceLaunched` クラスは、ワールドロード時にサーバ保存データの整合性を保つためのメンテナンス処理を実装しています。イベント駆動で自動的に実行されるため、ユーザーや他のシステムが介在することなく不要なデータをクリーンアップできます。コードはシンプルで読みやすく、`getBlockPos()` で削除対象を別リストに集めてから一括削除することで `ConcurrentModificationException` を回避するなど、細かな配慮も見られます。改善点としては、ログ出力における `ExampleMod.SERVER_BLOCK` の存在確認をより詳細に記録する、または削除後のサーバ数などを報告するなど、管理者向け情報の充実が考えられますが、基本的な機能としては十分です。