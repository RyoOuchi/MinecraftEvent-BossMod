# コードドキュメント

## 概要
このクラス `ServerSavedData` は、Minecraft サーバーワールドに存在する **サーバーブロックの位置情報を永続的に管理・保存するためのデータクラス** です。  
`SavedData` を継承し、NBT（Named Binary Tag）を用いてサーバー名と位置座標 (`BlockPos`) のマッピングを行います。  
本クラスは、サーバーブロックの登録・削除・保存・読み込みを一元的に扱う役割を持ちます。

---

## グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `servers` | `Map<String, BlockPos>` | `new HashMap<>()` | クラス全体 | サーバー名（キー）と座標 (`BlockPos`)（値）のペアを保持する。NBTデータとして永続化される。 |

---

## 関数一覧

### `ServerSavedData()`
- **概要**: コンストラクタ。空のサーバーデータマップを初期化。
- **引数**: なし
- **呼び出し元**: 新規データ生成時 (`get()` 内部など)。

---

### `static ServerSavedData get(ServerLevel level)`
- **概要**: 現在のサーバーワールドから `ServerSavedData` を取得する。存在しない場合は新規生成する。
- **引数**:
    - `level`: サーバーレベル (`ServerLevel`)
- **戻り値**: `ServerSavedData`（永続データインスタンス）
- **主な処理**:
    1. `level.getDataStorage()` からデータストレージを取得。
    2. `computeIfAbsent()` により、既存データがなければ `load()` もしくは `new ServerSavedData()` で生成。
    3. ストレージキー `"server_data"` で管理。
- **呼び出し元**: サーバーワールド初期化、またはサーバーブロック配置イベント時。
- **呼び出し先**: `load(CompoundTag)`。

---

### `static ServerSavedData load(CompoundTag tag)`
- **概要**: NBTデータから `ServerSavedData` を再構築。
- **引数**:
    - `tag`: 永続化されている NBT タグデータ
- **戻り値**: `ServerSavedData`
- **主な処理**:
    1. 新しい `ServerSavedData` インスタンスを作成。
    2. `tag.getList("servers", Tag.TAG_COMPOUND)` で保存済みサーバーデータを取得。
    3. 各 `CompoundTag` から `"name"`・`"x"`・`"y"`・`"z"` を読み取り、`BlockPos` を生成。
    4. `servers` マップに `put(name, pos)` で登録。
- **呼び出し元**: `get(ServerLevel)` 内の `computeIfAbsent()`。

---

### `CompoundTag save(CompoundTag tag)`
- **概要**: 現在保持しているサーバーデータをNBT形式で保存。
- **引数**:
    - `tag`: 書き込み先の `CompoundTag`
- **戻り値**: `CompoundTag`（書き込み後のデータ）
- **主な処理**:
    1. 新しい `ListTag` を作成。
    2. `servers.entrySet()` をループし、各サーバー名と座標を `CompoundTag` に格納。
    3. それを `ListTag` に追加後、最終的に `"servers"` キーで `tag` に追加。
- **呼び出し元**: Minecraftのデータ保存サイクル。

---

### `void addServer(String name, BlockPos pos)`
- **概要**: 新しいサーバーをデータに追加。
- **引数**:
    - `name`: サーバー名
    - `pos`: サーバーの座標 (`BlockPos`)
- **処理内容**:
    1. 既に同名のサーバーが登録されていない場合のみ追加。
    2. 追加後、`setDirty()` を呼び出してデータ変更を通知。
- **呼び出し元**: サーバーブロック設置処理など。

---

### `boolean removeServer(BlockPos blockPos)`
- **概要**: 指定された座標を持つサーバーを削除。
- **引数**:
    - `blockPos`: 削除対象のサーバー座標
- **戻り値**: `true`（削除成功）／`false`（削除対象なし）
- **主な処理**:
    1. `servers.entrySet()` をループし、座標が一致するエントリを検索。
    2. 一致するキー（サーバー名）を特定後、マップから削除。
    3. 削除が成功した場合 `setDirty()` を呼び出し、データを更新。
- **呼び出し元**: サーバーブロック破壊・リセット時。

---

### `Map<String, BlockPos> getServers()`
- **概要**: 登録済みの全サーバーデータを取得。
- **戻り値**: `Map<String, BlockPos>`
- **用途**: デバッグ出力、ネットワーク構築、サーバー探索処理などで利用。

---

## 呼び出し関係図（関数依存）
```
get(ServerLevel)
└─ computeIfAbsent()
    ├─ load(CompoundTag)
    └─ new ServerSavedData()
save(CompoundTag)
  └─ NBTへサーバーデータを書き込み
addServer()
  └─ setDirty()
removeServer()
  └─ setDirty()
getServers()
```

---

## 外部依存関係

| クラス／パッケージ | 用途 |
|------------------|------|
| **`net.minecraft.server.level.ServerLevel`** | サーバーワールドのデータストレージを取得するために使用。 |
| **`net.minecraft.nbt.CompoundTag`, `ListTag`, `Tag`** | MinecraftのNBT形式を使用してサーバーデータをシリアライズ／デシリアライズ。 |
| **`net.minecraft.core.BlockPos`** | サーバーのブロック座標を保持。 |
| **`net.minecraft.world.level.saveddata.SavedData`** | 永続データ管理のための基底クラス。`save()` や `setDirty()` を利用。 |

---

## ファイル別概要

| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| **`ServerSavedData.java`** | サーバーブロック位置データの保存・管理。NBTへの書き込みと読み込み機能を提供。 | `get()`, `load()`, `save()`, `addServer()`, `removeServer()`, `getServers()` |

---

## 総評
このクラスは、`ServerLevel` における**サーバーブロックの状態管理**を担う中核クラスです。  
NBTベースのデータ永続化機構を通じて、サーバー名と座標の対応関係をシンプルに保持しています。  
`setDirty()` の呼び出しによって自動保存が行われる仕組みを採用しており、  
Modのサーバーネットワーク管理（例：DNSサーバーやブラウザブロックとの連携）の基盤として利用されます。

この構成により、サーバー情報の追加・削除・取得が統一的なAPIで実装されており、  
将来的な拡張（例：サーバー属性やステータス情報の追加）にも容易に対応できる設計となっています。