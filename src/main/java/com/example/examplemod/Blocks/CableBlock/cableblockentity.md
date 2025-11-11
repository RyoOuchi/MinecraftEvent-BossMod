# 📘 コードドキュメント

## 概要
ケーブルは、ネットワークデータパケット (`DataPacket`) をブロックチェーン上で受け渡し、隣接するケーブルまたはルータ (`WifiRouterBlockEntity`) へ転送する役割を持つ。実際のネットワークケーブルと同様に、順序を保ちながらパケットを送信し、キューによるバッファリングとホッピング（中継）を実装している。

処理の概略は以下のとおり：

1. 他ブロックからのデータパケットを `enqueuePacket()` でキューに追加。
2. サーバティック毎に `tickServer()` が呼び出され、`processNextPacket()` によって未処理パケットがロードされる。
3. パケットのケーブル経路 (`cablePath`) に沿って現在ブロックの位置を確認し、次のブロックがケーブルならそこへハンドオフ（`enqueuePacket()` を呼ぶ）する。
4. 経路の終点に到達した場合は隣接するルータへデータを送信し、現在処理中のパケットをクリア。
5. 状態確認 (`hasDataPacket()`) やキューサイズ取得 (`getQueueSize()`) など補助メソッドを提供している。

---

## 🌐 グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|-------|----|---------|-----------|------|
| `packetQueue` | `Queue<PacketFrame>` | `new LinkedList<>()` | `enqueuePacket()`, `processNextPacket()`, `tickServer()`, `getQueueSize()` | 送信待機中のパケットを格納する FIFO キュー。各 `PacketFrame` はパケットと送信者をセットで保持する。 |
| `currentFrame` | `PacketFrame` | `null` | `processNextPacket()`, `tickServer()`, `clearCurrentPacket()`, `hasDataPacket()` | 現在転送中のパケットフレーム。処理中のパケットが無い場合は `null`。|
| `worldPosition` | `BlockPos` | 基底クラスから継承 | ログ出力など | ブロックエンティティの位置座標。|
| `level` | `Level` | 基底クラスから継承 | `tickServer()`, `getCableEntityAtPosition()`, `sendPacketToRouterEntity()` | ワールドオブジェクト。クライアント／サーバ判定やエンティティ取得に使用する。|

> **備考**: `PacketFrame` は `private static final` クラスで、`packet` (`DataPacket`) と `sender` (`Senders` 列挙型) の2つのフィールドを保持する軽量ラッパである。メタデータをパケットと共にキューに保存するために用いる。

---

## 🧩 関数一覧

### `enqueuePacket(DataPacket packet, Senders sender)`
- **概要**: 新しいデータパケットとその送信者をキューへ追加する。パケット単位で送信者メタデータを保存し、キューに待機させる。
- **引数**:
    - `packet`: `DataPacket` – ケーブルを通って送信するパケット。`null` の場合は処理しない。
    - `sender`: `Senders` – パケットの発信者（CLIENT, SERVER, CABLE など）。
- **戻り値**: なし。
- **処理内容**:
    1. 引数のいずれかが `null` ならそのまま return。
    2. 新しい `PacketFrame` インスタンスを作成し、`packetQueue.offer()` でキューに追加。
    3. `safeDataString()` でパケットデータを安全に文字列化し、ログ出力でキュー状態や内容を表示。
- **呼び出し元**: 他ケーブル (`CableBlockEntity`) やルータ (`WifiRouterBlockEntity`) がケーブルへパケットを送るときに使用する。
- **呼び出し先**: `safeDataString()`。

---

### `processNextPacket()`
- **概要**: 現在処理中のパケットがない (`currentFrame == null`) 場合、キューから次の `PacketFrame` を取り出して `currentFrame` に設定する。次のパケットが無い場合は何もせず終了する。
- **引数**: なし。
- **戻り値**: なし。
- **処理内容**:
    1. `currentFrame` が `null` かつ `packetQueue` が空でない場合のみ実行。
    2. 先頭の `PacketFrame` を `poll()` で取り出し `currentFrame` に設定。
    3. ログに現在のキュー状態と選択されたパケットを出力。
- **呼び出し元**: `tickServer()`。
- **呼び出し先**: `safeDataString()`。

---

### `tickServer(final Level level, final BlockPos pos)`
- **概要**: サーバ側の各ティックで呼び出され、ケーブルが現在保持しているパケットを処理し、適切な次のブロックへ転送するメインループ。
- **引数**:
    - `level`: `Level` – 現在のワールド。クライアントサイドかどうかを判定し、エンティティ取得に使用する。
    - `pos`: `BlockPos` – 現在のケーブルブロックの座標。
- **戻り値**: なし。
- **主な処理**:
    1. `level.isClientSide` が `true` ならクライアント側のため何もしない。
    2. `processNextPacket()` を呼び、`currentFrame` が `null` であれば処理を終了。
    3. `currentFrame.packet` と `currentFrame.sender` を参照しログ出力。
    4. パケットの `cablePath` を取得。`null` または空の場合は `clearCurrentPacket()` を呼んで終了。
    5. `cablePath` から現在のブロックのインデックスを得られなければ（`indexOf(pos) == -1`）`clearCurrentPacket()` を実行し終了。
    6. `currentIndex` が `cablePath` の終端であれば、`sendPacketToRouterEntity()` へハンドオフし、`clearCurrentPacket()` でクリア。
    7. そうでない場合は `getCableEntityAtPosition(nextPos)` で次のケーブルブロックエンティティを取得し、存在すれば `enqueuePacket()` でそのケーブルへバッファリング。送信者を `Senders.CABLE` に設定し、`clearCurrentPacket()` で現在のパケットをクリア。
- **呼び出し元**: サーバのティック処理 (`ServerTickEvent` 等)。
- **呼び出し先**: `processNextPacket()`, `safeDataString()`, `clearCurrentPacket()`, `getCableEntityAtPosition()`, `sendPacketToRouterEntity()`。
- **エラー処理**: 途中で不正な状態（`cablePath` が null/空、位置が経路外など）が発生した場合は現在のフレームをクリアし処理を終了。

---

### `clearCurrentPacket()`
- **概要**: 現在処理中の `currentFrame` を `null` にする。次のパケット処理の準備を整える。
- **引数／戻り値**: なし。
- **呼び出し元**: `tickServer()` の各処理分岐や `sendPacketToRouterEntity()` 内。

---

### `getCableEntityAtPosition(final BlockPos pos) -> @Nullable CableBlockEntity`
- **概要**: 指定された位置に存在するブロックがケーブルブロックエンティティであればそのインスタンスを返し、そうでなければ `null` を返す。
- **引数**: `pos` – 取得対象の座標。`null` や `level` が `null` の場合は `null` を返す。
- **戻り値**: `CableBlockEntity` インスタンスまたは `null`。
- **処理内容**:
    1. `level.getBlockEntity(pos)` でその位置の `BlockEntity` を取得。
    2. `instanceof CableBlockEntity` を判定し、真ならキャストして返す。偽なら `null` を返す。
- **呼び出し元**: `tickServer()`。

---

### `sendPacketToRouterEntity(final BlockPos currentPos, final DataPacket packet)`
- **概要**: ケーブルの終端に到達したパケットを隣接するルータブロックに送信する。
- **引数**:
    - `currentPos`: 現在のケーブルの座標。
    - `packet`: 転送するデータパケット。
- **戻り値**: なし。
- **処理内容**:
    1. パケットが保持する `routerPath` を取得。これが `null` または空の場合は何もしない。
    2. `routerPath` に含まれる各ルータ座標について、現在位置とのマンハッタン距離が 1 以下であれば隣接とみなす。
    3. 隣接ルータのブロックエンティティが `WifiRouterBlockEntity` なら、その `receiveTransmittedPacket(packet, this)` を呼び出してパケットを渡し、ログ出力を行い処理を終了。
- **呼び出し先**: `WifiRouterBlockEntity.receiveTransmittedPacket()`（隣接ルータへの実際の送信処理）。
- **呼び出し元**: `tickServer()`。

---

### `safeDataString(DataPacket p) -> String`
- **概要**: `DataPacket` のデータ部を安全に文字列化するユーティリティメソッド。データがバイト配列であり直接表示できない場合も考慮する。
- **引数**: `p` – 文字列化したいデータパケット。
- **戻り値**: データのUTF-8文字列、または `<binary N bytes>` 形式。
- **処理内容**:
    1. `p.getData()` を `new String()` で直接文字列化を試みる。
    2. 例外が発生した場合や `p.getData()` が `null` の場合は `<binary N bytes>` の形式で文字列を返す（N はバイト配列長）。
- **呼び出し元**: パケットデータのログ出力箇所（`enqueuePacket()`, `processNextPacket()`, `tickServer()`, `sendPacketToRouterEntity()`）。

---

### `hasDataPacket() -> boolean`
- **概要**: ケーブルが現在パケットを処理中かどうかを確認する。
- **戻り値**: `true` if `currentFrame != null`, otherwise `false`。
- **呼び出し元**: 外部モジュールがケーブルにパケットが存在するかどうかをチェックしたいとき。

---

### `setSender(Senders sender)` / `setSenderToNull()` (Deprecated)
- **概要**: 古い API 互換性のために残されているメソッド。現在は送信者が `PacketFrame` に保持されているため、何もしないノープ処理。
- **引数**: `setSender()` は送信者、`setSenderToNull()` はなし。
- **戻り値**: なし。
- **呼び出し元**: 他モジュールの旧コード。将来的には削除される予定。

---

### `getQueueSize() -> int`
- **概要**: 現在のパケットキューのサイズを返す。
- **戻り値**: `packetQueue.size()` – キューに溜まっているパケットの数。
- **呼び出し元**: 外部 GUI などでバッファサイズを表示する場合やデバッグで使用。

---

## 🔁 呼び出し関係図（関数依存）
```plaintext
enqueuePacket()
└─ safeDataString()
processNextPacket()
└─ safeDataString()

tickServer()
├─ processNextPacket()
├─ safeDataString()
├─ clearCurrentPacket()
├─ getCableEntityAtPosition()
│    └─ level.getBlockEntity()
├─ sendPacketToRouterEntity()
│    ├─ level.getBlockEntity()
│    └─ WifiRouterBlockEntity.receiveTransmittedPacket()
└─ enqueuePacket()   ← nextCable が存在する場合のみ

clearCurrentPacket()      ← tickServer()から呼ばれる

getCableEntityAtPosition()  ← tickServer()から呼ばれる

sendPacketToRouterEntity()  ← tickServer()から呼ばれる

safeDataString() ← enqueuePacket(), processNextPacket(), tickServer(), sendPacketToRouterEntity()

hasDataPacket() ← 外部モジュール

setSender(), setSenderToNull() ← 非推奨、旧コード用

getQueueSize() ← 外部モジュール
```
---

## ⚙️ 外部依存関係

| クラス／ライブラリ | 用途 |
|------------------|-----|
| **`ExampleMod.CABLE_BLOCK_ENTITY`** | ブロックエンティティの登録。`CableBlockEntity` のインスタンス識別用 ID を提供する。 |
| **`DataPacket`** | ネットワーク層のデータを表すクラス。`getCablePath()` と `getRouterPath()` により、ケーブル経路とルータ経路を取得する。 |
| **`WifiRouterBlockEntity`** | ケーブル終端のルータブロックを表すクラス。`receiveTransmittedPacket()` でケーブルからのパケットを受け取り、ルーティング処理を担当する。 |
| **`Senders`** | 列挙型で、パケットの送信元を `CLIENT`, `SERVER`, `CABLE` などとして識別する。`PacketFrame` で使用。 |
| **`Level`, `BlockPos`, `BlockEntity`** | Minecraft のゲームエンジンの基礎クラス。世界や座標を管理し、ブロックエンティティ取得やサーバ・クライアント判定に利用する。 |

---

## 📄 ファイル別概要

| ファイル名／クラス名 | 主な責務 | 主なメソッド |
|----------------|-----------------------------|---------------------------|
| **`CableBlockEntity.java`** | ケーブルブロックエンティティの実装。パケットの受け付け、キュー管理、ケーブル経路追跡、隣接ケーブル・ルータへのハンドオフを実装する。 | `enqueuePacket()`, `processNextPacket()`, `tickServer()`, `getCableEntityAtPosition()`, `sendPacketToRouterEntity()` |
| **`WifiRouterBlockEntity.java`** | ルータエンティティの実装。ケーブルから送られてきたパケットを受け取りサーバへ中継する。 | `receiveTransmittedPacket()` 他 |
| **`DataPacket.java`** | ネットワークパケットのデータ構造を提供。ケーブル経路 (`cablePath`) とルータ経路 (`routerPath`) を保持し、転送先を決定するために使用。 | `getCablePath()`, `getRouterPath()`, `getData()` |
| **`ExampleMod.java`** | MOD 全体の初期化を行い、各種ブロックやアイテム、エンティティを登録。 | MOD 初期化メソッド全般 |

---

## 💬 総評
`CableBlockEntity` は、Minecraft の世界内でネットワークケーブルとして振る舞うブロックエンティティを実装している。シンプルながら重要な機能として、**パケットの順序保持・経路追跡・ルータとの連携**を提供している。キューを用いたバッファリングとパケット転送のステータス管理 (`currentFrame`) により、複数のパケットが流れる状況でも安定した送信が可能になっている。ログ出力も豊富でデバッグが容易である。改善点としては、キューサイズが大きくなった場合のスロットリングや、クライアント側での進捗表示などユーザビリティ向上が考えられる。また、現在は単純な FIFO キューだが、優先度や帯域制御を行う機能を追加することも将来的には検討できるだろう。