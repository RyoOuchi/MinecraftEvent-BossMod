# コードドキュメント

## 概要
このクラス `DnsServerSavedData` は、Minecraft のサーバーサイド環境における **DNSサーバーブロックの位置情報を永続化（保存・読み込み）するためのデータ管理クラス** です。  
`SavedData` を継承しており、Minecraft のワールドデータ (`ServerLevel`) に結び付けられた NBT ストレージに情報を記録します。  
主な処理の流れは以下の通りです。

- ワールドから `DnsServerSavedData` のインスタンスを取得 (`get()`)
- DNSサーバーの座標をリストに追加または削除 (`addDnsServer()`, `removeDnsServer()`)
- ワールド保存時にNBTへ書き込み (`save()`)
- ワールド読み込み時にNBTから復元 (`load()`)

---

## グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `dnsServers` | `List<BlockPos>` | `new ArrayList<>()` | クラス全体 | 登録されたDNSサーバーブロックの位置（`x, y, z`）を保持するリスト。NBTに保存・読み込みされる主要データ。 |

---

## 関数一覧

### `DnsServerSavedData()`
- **概要**: デフォルトコンストラクタ。新規のDNSサーバーデータを初期化します。
- **引数**: なし
- **処理内容**: 空の `dnsServers` リストを生成。

---

### `static DnsServerSavedData get(ServerLevel level)`
- **概要**: 現在のサーバーレベルから永続データを取得します。存在しない場合は新しく生成します。
- **引数**:
    - `level`: サーバーレベルインスタンス (`ServerLevel`)。
- **戻り値**: `DnsServerSavedData`
- **処理内容**:
    1. `ServerLevel#getDataStorage()` を呼び出してデータストレージを取得。
    2. `computeIfAbsent()` を用いて既存データがなければ `load()` または `new DnsServerSavedData()` で生成。
    3. ストレージキー `"dns_server_data"` で保存領域を管理。
- **呼び出し元**: 主に `DNSServerBlockEntity` またはサーバー起動時の初期化処理。

---

### `static DnsServerSavedData load(CompoundTag tag)`
- **概要**: NBTデータから `DnsServerSavedData` を再構築します。
- **引数**:
    - `tag`: NBTタグオブジェクト (`CompoundTag`)。
- **戻り値**: `DnsServerSavedData`
- **主な処理**:
    1. 新規インスタンスを生成。
    2. `tag.getList("dnsServers", Tag.TAG_COMPOUND)` でDNSサーバー情報リストを取得。
    3. 各 `CompoundTag` から `x, y, z` 座標を読み取り、`BlockPos` を生成しリストに追加。
- **呼び出し元**: `computeIfAbsent()` 内部から。

---

### `CompoundTag save(CompoundTag pCompoundTag)`
- **概要**: 現在のDNSサーバーデータをNBT形式で保存します。
- **引数**:
    - `pCompoundTag`: 保存先のNBTタグ。
- **戻り値**: `CompoundTag`
- **主な処理**:
    1. `dnsServers` リスト内の全 `BlockPos` をループ。
    2. 各位置を `CompoundTag` に変換 (`x, y, z` を格納)。
    3. `ListTag` に追加後、最終的に `pCompoundTag` に `"dnsServers"` キーで登録。
- **呼び出し元**: Minecraft の内部 `SavedData` 保存サイクル。

---

### `void addDnsServer(BlockPos pos)`
- **概要**: 新しいDNSサーバー座標をリストに追加します。
- **引数**:
    - `pos`: DNSサーバーの位置 (`BlockPos`)。
- **処理内容**:
    1. `dnsServers` に同一座標が存在しない場合のみ追加。
    2. `setDirty()` を呼び出し、データが変更されたことをMinecraftに通知。
- **呼び出し元**: `DNSServerBlockEntity` の配置イベントなど。

---

### `void removeDnsServer(BlockPos pos)`
- **概要**: 指定されたDNSサーバーをリストから削除します。
- **引数**:
    - `pos`: 削除対象の座標 (`BlockPos`)。
- **処理内容**:
    1. リストから削除成功した場合のみ `setDirty()` を呼び出す。
    2. データストレージ更新を促す。

---

### `List<BlockPos> getDnsServers()`
- **概要**: 現在登録されているすべてのDNSサーバー座標を取得。
- **戻り値**: `List<BlockPos>`
- **用途**: ブロックレンダリングやネットワークマッピングなどの表示処理。

---

### `boolean hasDnsServers()`
- **概要**: 登録済みDNSサーバーが存在するかを判定。
- **戻り値**: `true`（リストが空でない場合）／`false`（空の場合）。

---

## 呼び出し関係図（関数依存）
```
get(ServerLevel)
├─ computeIfAbsent()
    ├─ load(CompoundTag)
    └─ new DnsServerSavedData()
save(CompoundTag)
  └─ ループで dnsServers の内容を保存
addDnsServer()
  └─ setDirty()
removeDnsServer()
  └─ setDirty()
getDnsServers(), hasDnsServers()
```
---

## 外部依存関係
| クラス／パッケージ | 用途 |
|----------------|------|
| **`net.minecraft.server.level.ServerLevel`** | サーバー側ワールドインスタンス。データストレージ (`getDataStorage()`) を取得するために使用。 |
| **`net.minecraft.nbt.CompoundTag`, `ListTag`, `Tag`** | MinecraftのNBT（Named Binary Tag）フォーマット操作。データのシリアライズ／デシリアライズを実装。 |
| **`net.minecraft.core.BlockPos`** | ブロック位置の座標を表す3次元整数ベクトル。 |
| **`SavedData`** | 永続データをワールドに関連付けて保存するための基底クラス。`save()` や `setDirty()` を提供。 |

---

## ファイル別概要
| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| **`DnsServerSavedData.java`** | DNSサーバーブロック位置の永続化管理。NBTの読み書き処理を実装。 | `get()`, `load()`, `save()`, `addDnsServer()`, `removeDnsServer()`, `getDnsServers()`, `hasDnsServers()` |

---

## 総評
このクラスはMinecraft Forgeの `SavedData` API を正しく活用した**サーバー状態永続化クラス**であり、  
ワールド内のDNSサーバーブロックの位置情報を一貫して追跡します。  
NBT構造をシンプルに保ちながらも、`computeIfAbsent()` により読み込みと生成を自動的に切り替える設計がされており、  
Mod開発におけるデータ永続化の基本モデルとして非常に安定した構成になっています。