# コードドキュメント

## 概要
`NetworkUtils` クラスは、Minecraft Mod におけるネットワーク通信処理を支援するユーティリティクラスです。インスタンス化不可とし、全てのメソッドを `static` として定義しています。主な機能は以下の通りです。

- ルータ間を接続するケーブルパスの探索と次のルータ位置の取得。
- ブロック判定（ケーブルブロックかどうか）や隣接座標の計算。
- TCP パケットおよび ACK パケットの組み立て。
- データチャンクの再構成や連続性の検証。
- バイト配列からデータチャンクを生成。

これにより、ブラウザ、サーバ、ルータブロック間の通信プロトコルを簡潔に実装することができます。

---

## グローバル変数一覧
`NetworkUtils` にはインスタンスフィールドや静的フィールドは存在せず、定数類も持ちません。すべての機能は `static` メソッドとして提供されます。

---

## 関数一覧

### `findCablePathBetweenRouters(Level level, BlockPos startRouter, BlockPos endRouter) -> List<BlockPos>`
- **概要**: 二つのルータ位置間をケーブルブロックのみを通って探索し、最短のケーブル経路を返す。
- **引数**:
    - `level` – ワールドコンテキスト。null の場合は空リストを返す。
    - `startRouter`, `endRouter` – 探索開始と終了位置のルータ座標。
- **処理内容**:
    1. 幅優先探索（BFS）を用いてケーブルブロックを辿りながら `endRouter` までの経路を探す。
    2. 探索中は訪問済みセットと前ノードマップを更新する。
    3. `endRouter` に到達した場合は前ノードマップを逆に辿り、ケーブルブロックのみをリストに追加して逆順にして返す。
- **戻り値**: ケーブルブロックの座標リスト。到達できない場合は空リスト。
- **呼び出し元**: `WifiRouterBlockEntity.findCablePathBetweenRouters()`（ラッパー）、ルータやケーブルの経路計算箇所。

### `isCableBlock(Block block) -> boolean`
- **概要**: 指定ブロックがケーブルブロック (`ExampleMod.CABLE_BLOCK`) と一致するか判定。
- **戻り値**: ケーブルブロックなら `true`、それ以外は `false`。
- **呼び出し元**: `findCablePathBetweenRouters()`, `NetworkUtils.getCablePathToNextRouter()`。

### `getNeighbors(BlockPos pos) -> BlockPos[]`
- **概要**: 与えられた座標の上下・南北・東西の6方向の隣接座標を返す。
- **呼び出し元**: 経路探索やブロック接続検査で使用。

### `@Nullable getNextRouterBlockPos(List<BlockPos> path, BlockPos currentPos) -> BlockPos?`
- **概要**: ルータ経路リストから、現在のルータの次に来るルータ位置を取得。
- **引数**:
    - `path` – ルータ経路のリスト（nullや空の場合は null を返す）。
    - `currentPos` – 現在のルータ位置。
- **戻り値**: 次のルータの座標。または null（位置が見つからない、最後のルータなど）。
- **呼び出し元**: `NetworkUtils.getCablePathToNextRouter()`, `WifiRouterBlockEntity` の経路計算。

### `getCablePathToNextRouter(Level level, List<BlockPos> path, BlockPos currentPos) -> List<BlockPos>`
- **概要**: 現在のルータから経路上の次のルータまでのケーブル経路を取得。
- **処理内容**: `getNextRouterBlockPos()` で次のルータ位置を求め、`findCablePathBetweenRouters()` を呼んでケーブル経路を返す。
- **戻り値**: ケーブルブロックのリスト。

### `byte[] createTcpPacket(int seqNumber, int ackNumber, boolean isLast, byte[] data)`
- **概要**: SEQ・ACK 番号、END フラグ、ペイロードから TCP パケットを生成。
- **処理内容**: `createTcpPacket(seqNumber, ackNumber, -1, isLast, data)` を呼び出すラッパーメソッド。
- **戻り値**: UTF-8 エンコードされたパケットバイト列。

### `byte[] createTcpPacket(int seqNumber, int ackNumber, int responseNumber, boolean isLast, byte[] data)`
- **概要**: レスポンス番号付きの TCP パケットを生成。
- **処理内容**:
    1. `headerMap` に `"SEQ"`, `"ACK"`, `"RESPONSE"` (条件付き), `"END"` を設定。
    2. `new Header(headerMap)`, `new Body(data)`, `new Message(header, body)` を生成。
    3. `Message.constructMessage().getBytes(StandardCharsets.UTF_8)` を返す。
- **呼び出し元**: `NetworkUtils.createTcpPacket()` の他オーバーロード、`fastRetransmit()`, `sendUrlData()`, `sendFileData()` など。

### `byte[] createAckPacket(int ackNumber)`
- **概要**: ACK パケットを生成。ヘッダ内に `"ACK"` のみを設定し、ボディは持たない。
- **処理内容**: `new Header(Map.of("ACK", ackNumber))` を生成し、そのヘッダ文字列の UTF-8 バイト列を返す。

### `String reconstructData(Map<Integer, Body> dataChunks)`
- **概要**: 受信したデータチャンクのマップをシーケンス番号順に連結し、元の文字列を再構築する。
- **処理内容**: `dataChunks.values().stream().map(Body::extractBody).reduce("", String::concat)` で連結。
- **戻り値**: 再構成された文字列。

### `boolean isDataContiguous(Map<Integer, Body> dataChunks)`
- **概要**: データチャンクがシーケンス番号に基づいて連続しているかどうか検証する。
- **処理内容**:
    1. キーをソートし、隣り合う SEQ 値とペイロード長から次の SEQ が正しく繋がるかチェック。
    2. 途中で不連続があれば `false` を返す。
- **戻り値**: 連続していれば `true`、欠落があれば `false`。

### `List<byte[]> createDataChunks(byte[] data, int chunkSize)`
- **概要**: 大きなデータを指定されたチャンクサイズで分割する。
- **処理内容**: `for` ループで `System.arraycopy()` を使い、`chunkSize` ごとに切り出したバイト配列をリストに追加。
- **戻り値**: 分割されたバイト配列のリスト。

---

## 呼び出し関係図（関数依存）
```
findCablePathBetweenRouters()
├─ getNeighbors()
├─ isCableBlock()
└─ reconstruct path via previous map

getNeighbors()
└─ returns six adjacent BlockPos

getNextRouterBlockPos()
└─ path.indexOf() and bounds checks

getCablePathToNextRouter()
├─ getNextRouterBlockPos()
└─ findCablePathBetweenRouters()

createTcpPacket(seq, ack, isLast, data)
└─ createTcpPacket(seq, ack, -1, isLast, data)

createTcpPacket(seq, ack, response, isLast, data)
├─ new Header(headerMap)
├─ new Body(data)
└─ new Message(header, body)

createAckPacket()
└─ new Header(Map.of(“ACK”, …))

reconstructData()
└─ Body.extractBody() for each chunk

isDataContiguous()
└─ Body.extractBody() for length calculation

createDataChunks()
└─ System.arraycopy() in a loop
```
---

## 外部依存関係
| クラス／ライブラリ | 用途 |
|------------------|----|
| **`ExampleMod.CABLE_BLOCK`** | ケーブルブロック判定に使用する定数。 |
| **`BlockPos`, `Level`, `BlockState`, `Block`, `BlockEntity`** | Minecraft API: 座標管理、ワールドアクセス、ブロック判定に使用。 |
| **`Header`, `Body`, `Message`** | TCP パケットや ACK パケットの組み立てに使用されるデータクラス。 |
| **`Queries`** | パケットの種類を区別する際に利用されるが、`createTcpPacket()` 内では設定されない（外部からデータパケットに付与される）。 |
| **`java.util` コレクション** (`List`, `ArrayList`, `Map`, `HashMap`, `Set`, `HashSet`, `Queue`, `LinkedList`, `PriorityQueue`) | 経路探索やデータチャンク処理、重み付き探索に使用。 |
| **`StandardCharsets`** | パケット文字列をバイト配列に変換する際に UTF-8 を指定するために使用。 |

---

## ファイル別概要
| ファイル名／クラス名 | 主な責務 | 主なメソッド |
|--------------------|-----------|--------------|
| **`NetworkUtils.java`** | ネットワークに関するユーティリティ関数をまとめ、経路探索、パケット生成、データ再構成、連続性検証、チャンク化を提供。 | `findCablePathBetweenRouters()`, `isCableBlock()`, `getNeighbors()`, `getNextRouterBlockPos()`, `getCablePathToNextRouter()`, `createTcpPacket()`, `createAckPacket()`, `reconstructData()`, `isDataContiguous()`, `createDataChunks()` |