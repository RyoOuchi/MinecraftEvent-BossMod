# コードドキュメント

## 概要
`DataPacket` クラスは、ネットワーク通信のパケットを表現するための不変オブジェクトです。パケットは、ルータの経路、送信データ本体、通信クエリの種類（DNS、TCP ハンドシェイク、接続済み、切断など）、ケーブル経路、送信者と受信者のブロック位置、エラーコード、クライアント側ブロック位置といった情報を内包します。クラスはこれらの情報を保持し、更新や経路反転、送受信者の入れ替えといった操作を行う際は新しいインスタンスを返す設計となっています。

---

## グローバル変数一覧
`DataPacket` は不変オブジェクトであり、クラスレベルのグローバル変数は持ちません。各インスタンスが以下のフィールドを持ち、コンストラクタで初期化されます。

| フィールド名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------------|----|--------|----------|------|
| `routerPath` | `List<BlockPos>` | コンストラクタ引数 | `getRouterPath()`, `updateRouterPath()`, `invertRouterPath()`, `addNewRouterPathToOldPath()` | パケットが通過するルータブロックの順序を保持。 |
| `data` | `byte[]` | コンストラクタ引数 | `getData()`, `updateData()` | パケットのペイロード（URL 文字列やファイルデータ、ACK など）。 |
| `queryType` | `Queries` | コンストラクタ引数 | `getQueryType()`, `updateQueryType()` | 通信の種類を示す列挙型（DNS、TCP_HANDSHAKE など）。 |
| `cablePath` | `List<BlockPos>` | コンストラクタ引数 | `getCablePath()`, `updateCablePath()` | ルータ間のケーブルブロック経路を保持。 |
| `senderBlockPos` | `BlockPos` | コンストラクタ引数 | `getSenderBlockPos()`, `updateSenderBlockPos()`, `swapSenderAndReceiver()` | パケット送信元のブロック位置。 |
| `sendToBlockPos` | `BlockPos` | コンストラクタ引数 | `getSendToBlockPos()`, `updateSendToBlockPos()`, `swapSenderAndReceiver()` | 次の送信先ブロック位置。 |
| `errorCode` | `ErrorCodes` (`@Nullable`) | コンストラクタ引数または `null` | `getErrorCode()`, `updateErrorCode()` | パケットに関連するエラー状態を示す（NOERROR, FORMERR, NXDOMAIN など）。 |
| `clientBlockPos` | `BlockPos` | コンストラクタ引数 | `getClientBlockPos()` | クライアントブロック（ブラウザ）の位置。応答の送信先を特定する。 |

---

## 関数一覧

### コンストラクタ
- **`DataPacket(List<BlockPos> routerPath, byte[] data, Queries queryType, List<BlockPos> cablePath, BlockPos senderBlockPos, BlockPos sendToBlockPos, BlockPos clientBlockPos)`**  
  エラーコードを持たないパケットを初期化します。`errorCode` は `null` に設定されます。
- **`DataPacket(List<BlockPos> routerPath, byte[] data, Queries queryType, List<BlockPos> cablePath, BlockPos senderBlockPos, BlockPos sendToBlockPos, ErrorCodes errorCode, BlockPos clientBlockPos)`**  
  エラーコード付きのパケットを初期化します。応答パケットを生成する際に使用されます。

### アクセサメソッド
- **`getRouterPath()`**, **`getData()`**, **`getQueryType()`**, **`getCablePath()`**, **`getSenderBlockPos()`**, **`getSendToBlockPos()`**, **`getClientBlockPos()`**  
  各フィールドの値を返す getter。
- **`getErrorCode()`**  
  エラーコードを返します。存在しない場合は `null` を返すため、呼び出し側は null チェックが必要です。

### 更新系メソッド
これらのメソッドは不変性を保つため、新しい `DataPacket` インスタンスを返します。

- **`updateCablePath(List<BlockPos> newCablePath)`**  
  ケーブル経路を `newCablePath` で置き換えた新しいパケットを返します。
- **`updateRouterPath(List<BlockPos> newRouterPath)`**  
  ルータ経路を `newRouterPath` で置き換えた新しいパケットを返します。
- **`updateData(byte[] newData)`**  
  ペイロードを新しいデータで置き換えた新しいパケットを返します。
- **`updateErrorCode(ErrorCodes newErrorCode)`**  
  エラーコードを更新した新しいパケットを返します。
- **`updateQueryType(Queries newQueryType)`**  
  クエリタイプを更新した新しいパケットを返します。
- **`updateSenderBlockPos(BlockPos newSenderBlockPos)`**, **`updateSendToBlockPos(BlockPos newSendToBlockPos)`**  
  送信元または送信先ブロック位置を更新した新しいパケットを返します。

### ルータ経路操作メソッド
- **`invertRouterPath()`**  
  ルータ経路を反転させ、送信方向を逆にした新しいパケットを返します。ルータ経路が空の場合は元のインスタンスを返します。
- **`swapSenderAndReceiver()`**  
  送信者 (`senderBlockPos`) と受信者 (`sendToBlockPos`) を入れ替えた新しいパケットを返します。応答パケット生成時に利用。
- **`addNewRouterPathToOldPath(List<BlockPos> oldPath, List<BlockPos> newPath)`**  
  `oldPath` を破壊的に更新しつつ `newPath` を連結し、新しい経路を保持したパケットを返します。呼び出し側は副作用に注意する必要があります。

---

## 呼び出し関係図（関数依存）
```
DataPacket constructors
└─ initializes fields (routerPath, data, queryType, cablePath, senderBlockPos, sendToBlockPos, [errorCode], clientBlockPos)

updateCablePath()
└─ return new DataPacket(… newCablePath …)

updateRouterPath()
└─ return new DataPacket(… newRouterPath …)

updateData()
└─ return new DataPacket(… newData …)

updateErrorCode()
└─ return new DataPacket(… newErrorCode …)

updateQueryType()
└─ return new DataPacket(… newQueryType …)

updateSenderBlockPos()
└─ return new DataPacket(… newSenderBlockPos …)

updateSendToBlockPos()
└─ return new DataPacket(… newSendToBlockPos …)

invertRouterPath()
├─ if routerPath empty return this
├─ reverse copy of routerPath
└─ updateRouterPath(invertedPath)

swapSenderAndReceiver()
├─ updateSendToBlockPos(senderBlockPos)
└─ updateSenderBlockPos(sendToBlockPos)

addNewRouterPathToOldPath()
├─ oldPath.addAll(newPath)
└─ updateRouterPath(oldPath)
```
---

## 外部依存関係
| クラス／ライブラリ | 用途 |
|------------------|-----|
| **`BlockPos` (Minecraft API)** | ブロックの座標を保持し、ルータやケーブル、送信元・送信先の位置を表す。 |
| **`Queries` (Enum)** | 通信の種類を識別する列挙型。DNS、TCP_HANDSHAKE、TCP_ESTABLISHED、TCP_DISCONNECT など。 |
| **`ErrorCodes` (Enum)** | 通信エラーの種類を示す列挙型。NOERROR、FORMERR、NXDOMAIN などが含まれる。 |
| **`java.util.List`, `ArrayList`, `Collections`** | 経路やデータの保持、反転、連結に使用。`Collections.reverse()` は経路反転時に利用される。 |
| **`javax.annotation.Nullable`** | `getErrorCode()` の戻り値が null になり得ることを示すアノテーション。 |

---

## ファイル別概要
| ファイル名／クラス名 | 主な責務 | 主なメソッド |
|--------------------|-----------|---------------|
| **`DataPacket.java`** | ネットワークパケットを不変オブジェクトとして表現し、経路やデータ、クエリタイプ、エラーコードを保持・更新する。 | コンストラクタ群、更新メソッド (`updateCablePath()`, `updateRouterPath()`, `updateData()`, `updateErrorCode()`, `updateQueryType()`, `updateSenderBlockPos()`, `updateSendToBlockPos()`)、経路操作 (`invertRouterPath()`, `swapSenderAndReceiver()`, `addNewRouterPathToOldPath()`)、アクセサメソッド等 |

---

## 💬 総評
`DataPacket` クラスは、ネットワーク通信のさまざまな状態を一つのオブジェクトで表すために設計された不変クラスです。データ更新を伴う操作はすべて新しいインスタンスを返すため、スレッドセーフに利用できます。経路反転や送受信者の入れ替えといった操作がメソッドとして用意されており、パケットの送受方向を簡潔に切り替えられる点が特徴です。唯一の注意点は `addNewRouterPathToOldPath()` が渡されたリストを破壊的に変更する点であり、この副作用を理解した上で利用する必要があります。全体として、拡張性と安全性に優れたネットワークパケット表現として機能します。