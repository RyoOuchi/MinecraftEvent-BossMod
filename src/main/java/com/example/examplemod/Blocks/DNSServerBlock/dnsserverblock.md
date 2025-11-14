# 📘 コードドキュメント

## 概要
この `DNSServerBlockEntity` クラスは、Minecraft Mod における DNS サーバブロックの動作を定義しています。ブラウザブロックからのドメイン名解決要求に応答し、
ドメイン名をサーバブロックの位置 (`BlockPos`) に解決して返す役割を担います。
具体的な処理の流れは以下のようになっています：

1. ルータブロック (`WifiRouterBlockEntity`) から DNS リクエスト (`DataPacket`) を受け取り、`receiveDataPacket()` で処理を開始。
2. サーバー保存データ (`ServerSavedData`) からドメイン→IP マッピングを読み込み、要求された URL からドメインを抽出。
3. ドメインが登録されているかどうかを `validateDataPacket()` で検証し、結果に応じてエラーコードを決定。
4. `buildResponsePacket()` で DNS 応答パケットを生成し、パケット内のルータ経路を反転・ケーブル経路を構築した上で応答データを設定。
5. 応答パケットが準備できたら `packetToTransmit` に保持し、`tickServer()` で隣接ルータへ転送する。
6. 送信後は状態をリセットし次のリクエストに備える。

---

## 🌐 グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|------|-----------|-----------|--------------------------|------|
| `dataPacket` | `DataPacket` | `null` | `receiveDataPacket()`, `validateDataPacket()` | 現在処理中の DNS 要求パケット。`receiveDataPacket()` で設定され、応答生成後に `null` へ戻される。 |
| `currentPos` | `BlockPos` | コンストラクタ引数の `pWorldPosition` | 様々なログ出力、ケーブル経路探索 (`findCablePathBetweenRouters()`)、ルータ探索 (`getNextRouterBlockPos()`)、隣接ルータ取得 (`tickServer()`) | この DNS サーバブロックの位置。固定値。 |
| `dnsDomainToIPMap` | `Map<String, BlockPos>` | 空の `HashMap` | `populateDnsMap()`, `receiveDataPacket()`, `validateDataPacket()` | ドメイン名をサーバ IP（`BlockPos`）に対応付けるマップ。`populateDnsMap()` で更新される。 |
| `readyToTransmitData` | `boolean` | `false` | `receiveDataPacket()`, `tickServer()` | 応答パケットが送信準備完了かどうかのフラグ。DNS 応答生成後に `true` に設定され、送信後に `false` に戻される。 |
| `packetToTransmit` | `DataPacket` | `null` | `receiveDataPacket()`, `tickServer()` | 送信予定の DNS 応答パケット。`tickServer()` でルータへ転送後に `null` にリセットされる。 |
| `level` | `Level` | 基底クラス `BlockEntity` より | `receiveDataPacket()`, `buildResponsePacket()`, `tickServer()`, `populateDnsMap()` | ワールドコンテキスト。クライアント／サーバ判定やルータ／ケーブルブロックエンティティ取得に使用。 |
| `worldPosition` | `BlockPos` | 基底クラスより | `validateDataPacket()` | ブロックのワールド座標。ログ出力やバリデーションに利用。 |

---

## 🧩 関数一覧

### コンストラクタ
#### `DNSServerBlockEntity(BlockPos pWorldPosition, BlockState pBlockState)`
- **概要**: DNS サーバブロックエンティティを初期化する。
- **引数**:
    - `pWorldPosition` – このブロックの位置。
    - `pBlockState` – ブロックの状態。
- **処理**: 親クラス `BlockEntity` のコンストラクタを呼び、`currentPos` に位置を設定する。その他の状態変数は初期値のまま。
- **呼び出し元**: ブロックがワールドに配置されたときに自動的に呼ばれる。

### `void receiveDataPacket(DataPacket packet)`
- **概要**: DNS リクエスト用の `DataPacket` を受信し、ドメインの解決と応答パケットの生成を行う。
- **引数**: `packet` – クライアントから送られた DNS リクエストパケット。null の場合は処理を中止。
- **処理の流れ**:
    1. `packet` が null であれば警告を出して終了。
    2. `this.dataPacket` に受信したパケットを保存。
    3. `populateDnsMap()` を呼び、サーバ保存データから `dnsDomainToIPMap` を更新。
    4. 受信パケットのルータ経路をログ出力し、視覚的なデバッグとしてサーバ上部に黒曜石ブロックを置く。
    5. `packet.getData()` を UTF‑8 文字列化し `extractDomainFromUrl()` によりドメイン名を抽出。
    6. `validateDataPacket()` で要求ドメインの状態を検証し、`ErrorCodes` を決定。`dnsDomainToIPMap` から対応 IP を取得。
    7. `buildResponsePacket()` を呼んで DNS 応答パケットを生成。
    8. 応答パケットを `packetToTransmit` に保存し、`readyToTransmitData = true` に設定。処理中パケット `dataPacket` を `null` に戻す。
- **呼び出し元**: ルータ (`WifiRouterBlockEntity`) が DNS リクエストをサーバに届けたときに呼び出す。
- **呼び出し先**: `populateDnsMap()`, `extractDomainFromUrl()`, `validateDataPacket()`, `buildResponsePacket()`, `Block.setBlockAndUpdate()`。

### `private DataPacket buildResponsePacket(DataPacket packet, BlockPos resolvedIP, ErrorCodes errorCode)`
- **概要**: DNS 応答パケットを構築し、ルータ経路・ケーブル経路・エラーコードを設定する。
- **引数**:
    - `packet` – 元の DNS リクエストパケット。
    - `resolvedIP` – ドメイン解決結果のサーバ座標。null の場合は "0:0:0" として扱う。
    - `errorCode` – 解決結果に応じたエラーコード（`NOERROR`, `NXDOMAIN`, `FORMERR`）。
- **戻り値**: 構築された `DataPacket`（応答パケット）。`packet` が null の場合は null。
- **処理の流れ**:
    1. IP 座標を `x:y:z` 形式の文字列に変換し UTF‑8 バイト配列化。
    2. `packet.updateData(ipBytes)` によりパケットのデータ部を IP に更新。
    3. `invertRouterPath()` でルータ経路を反転し、`swapSenderAndReceiver()` で送信者／受信者ブロック座標を入れ替え、返送方向へ。
    4. 返送用ルータ経路 (`getRouterPath()`) から `getNextRouterBlockPos()` で次のルータ座標を決定。見つからない場合は現在位置を使用。
    5. スタティックメソッド `findCablePathBetweenRouters(level, currentPos.below(), nextRouterPos)` を用いて現在の下方向ケーブルから次ルータまでのケーブル経路を取得し、`updateCablePath()` で設定。
    6. `ResponseHandler.handleResponse(errorCode, updatedPacket)` を呼び、エラーコードに応じたヘッダーやフラグを付与した最終パケットを取得。
    7. ログ出力後、最終パケットを返す。
- **呼び出し元**: `receiveDataPacket()`。
- **呼び出し先**: `getNextRouterBlockPos()`, `findCablePathBetweenRouters()`, `ResponseHandler.handleResponse()`。

### `@Nullable BlockPos getNextRouterBlockPos(List<BlockPos> path)`
- **概要**: 返送用ルータ経路において現在のルータの次に来るルータ座標を求める。
- **引数**: `path` – ルータ経路のリスト。null または空リストなら null を返す。
- **戻り値**: 次ルータの `BlockPos`。存在しない場合は null。
- **処理の流れ**:
    1. `path.indexOf(currentPos.below())` でこのサーバのルータ位置（1 ブロック下）を経路から検索。見つからなければ警告ログを出して null を返す。
    2. インデックスがリストの末尾の場合は終点と見なし、終了ログを出して null を返す。
    3. それ以外の場合は `path.get(currentIndex + 1)` を次ルータ座標として返し、ログに出力する。
- **呼び出し元**: `buildResponsePacket()`。

### `void tickServer()`
- **概要**: サーバ側で毎ティック呼び出され、DNS 応答パケットの送信を行う。
- **処理の流れ**:
    1. `readyToTransmitData` が true かつ `packetToTransmit` が null でない場合にのみ処理を実行。
    2. `level.getBlockEntity(currentPos.below())` で下方のブロックエンティティを取得し、`WifiRouterBlockEntity` であるか確認。また、そのルータが DNS サーバ専用 (`isDNSServerRouter()`) であるか検証。
    3. 条件を満たせばログを出力し、`wifiRouterBlockEntity.transmitPacket(packetToTransmit)` で応答を送信。
    4. 送信後、`readyToTransmitData` と `packetToTransmit` をリセットする。
- **呼び出し元**: ワールドのサーバ側ティック処理。DNS 応答が生成された後、応答送信に利用される。

### `ErrorCodes validateDataPacket(String extractedDomain)`
- **概要**: 受信パケットおよび抽出したドメイン名を検証し、適切なエラーコードを返す。
- **引数**: `extractedDomain` – URL から抽出したドメイン名。
- **戻り値**: `ErrorCodes.NOERROR`, `NXDOMAIN`, `FORMERR` のいずれか。
- **処理の流れ**:
    1. `dataPacket` が null なら警告ログを出し `FORMERR` を返す。
    2. `dataPacket.getData()` が null または空であれば `FORMERR` を返す。
    3. `dnsDomainToIPMap` が空、もしくは `extractedDomain` が含まれていなければ `NXDOMAIN` を返す。
    4. `dnsDomainToIPMap.get(extractedDomain)` が null なら警告ログを出し `NXDOMAIN` を返す。
    5. それ以外は `NOERROR` を返す。
- **呼び出し元**: `receiveDataPacket()`。

### `String extractDomainFromUrl(String url)`
- **概要**: URL 文字列からドメイン部を切り出す。
- **引数**: `url` – 完全な URL。null または空の場合は空文字列を返す。
- **戻り値**: ドメイン文字列（スラッシュ `/` より前の部分）。スラッシュが無ければ URL 全体を返す。
- **呼び出し元**: `receiveDataPacket()`。

### `void populateDnsMap()`
- **概要**: サーバ保存データから DNS マッピング (`dnsDomainToIPMap`) を構築する。
- **処理の流れ**:
    1. `dnsDomainToIPMap.clear()` で既存のマップをクリア。
    2. `level` が `ServerLevel` のインスタンスであることを確認し、そうでない場合は何もしない。
    3. `ServerSavedData.get(serverLevel).getServers()` で全サーバドメインのマップを取得し `dnsDomainToIPMap.putAll(servers)` で登録。
- **呼び出し元**: `receiveDataPacket()`。

---

## 🔁 呼び出し関係図（関数依存）
```
receiveDataPacket()
├─ populateDnsMap()
├─ extractDomainFromUrl()
├─ validateDataPacket()
├─ buildResponsePacket()
│    ├─ getNextRouterBlockPos()
│    ├─ findCablePathBetweenRouters() [static import]
│    └─ ResponseHandler.handleResponse()
└─ level.setBlockAndUpdate()  (デバッグ用)

buildResponsePacket()
├─ updateData(), invertRouterPath(), swapSenderAndReceiver()  [DataPacketのメソッド]
├─ getNextRouterBlockPos()
├─ findCablePathBetweenRouters()
└─ ResponseHandler.handleResponse()

getNextRouterBlockPos()
(単独で他を呼ばない)

validateDataPacket()
(依存関係なし)

extractDomainFromUrl()
(依存関係なし)

populateDnsMap()
└─ ServerSavedData.get()

tickServer()
└─ wifiRouterBlockEntity.transmitPacket() （条件成立時）
```

---

## ⚙️ 外部依存関係
| クラス／ライブラリ | 用途と理由 |
|---|---|
| **`ExampleMod.DNSSERVER_BLOCK_ENTITY`** | Forge のブロックエンティティ登録 ID。コンストラクタ呼び出しでこのエンティティを登録する。 |
| **`WifiRouterBlockEntity`** | ルータブロックの実装。DNS 応答を受信する側で、`receiveTransmittedPacket()` や `transmitPacket()` を提供する。 |
| **`DataPacket`** | ネットワークデータを表現するクラス。データ部更新 (`updateData()`)、ルータ経路反転 (`invertRouterPath()`)、送受信者入替 (`swapSenderAndReceiver()`)、ケーブル経路設定 (`updateCablePath()`) などのメソッドを持つ。 |
| **`ErrorCodes`** | DNS 処理の結果を表す列挙型で、`NOERROR`, `NXDOMAIN`, `FORMERR` が含まれる。 |
| **`ResponseHandler`** | エラーコードに応じた DNS 応答のヘッダ構築などを担当するユーティリティクラス。`handleResponse()` でパケットをラップする。 |
| **`ServerSavedData`** | サーバに登録されたドメインとサーバ座標のマッピングを保持するクラス。`getServers()` でドメイン→IP マップを取得する。 |
| **`Blocks`** | デバッグ用に `OBSIDIAN` などのブロックを設置するために使用。 |
| **`findCablePathBetweenRouters()`** (static import) | ルータ間を繋ぐケーブル経路を探索するメソッド。 |
| **`StandardCharsets`** | 文字列をバイト配列に変換するときの文字セット (UTF‑8)。 |
| **Minecraft 基本クラス** (`BlockPos`, `BlockState`, `BlockEntity`, `ServerLevel`) | 座標情報、ブロック状態、ワールドアクセス等の基本機能。 |

---

## 📄 ファイル別概要

| ファイル名／クラス名 | 主な責務 | 主要メソッド |
|---|---|---|
| **DNSServerBlockEntity.java** | DNS サーバブロックエンティティの実装。DNS クエリ受信、ドメイン解決、応答パケット生成、応答送信制御を担う。 | コンストラクタ、`receiveDataPacket()`, `buildResponsePacket()`, `tickServer()`, `validateDataPacket()`, `populateDnsMap()` 等 |
| **WifiRouterBlockEntity.java** | ルータエンティティの実装。DNS サーバへのリクエスト送信および応答の受信、パケットの中継を行う。 | `receiveTransmittedPacket()`, `transmitPacket()`, `isDNSServerRouter()` 等 |
| **ServerSavedData.java** | サーバ側で保持するドメインとサーバ座標のマッピングデータの管理。 | `getServers()` |
| **DataPacket.java** | ネットワークパケットの構造体。データやルータ経路・ケーブル経路、送受信座標等を保持し、各種更新メソッドを提供。 | `updateData()`, `invertRouterPath()`, `swapSenderAndReceiver()`, `updateCablePath()`, `getRouterPath()` |
| **ResponseHandler.java** | DNS 応答のフォーマット処理を行い、エラーコードに応じてパケットを修正するユーティリティ。 | `handleResponse()` |

---

## 💬 総評
`DNSServerBlockEntity` はシンプルながらも明確に職責分担された実装で、
DNS リクエストの受信から応答生成・送信までを完結に行っています。サーバ保存データからドメインマッピングを毎回読み込むことで、
サーバ登録の更新にも対応できる柔軟性があります。ルータ経路の反転や送受信者の入れ替えなど、Minecraft 上でのネットワーク伝送を意識した処理が組み込まれている点が特徴的です。改善点としては、ドメインマッピングが空の場合や null データに対して早期にエラー応答を返すための例外処理の強化、
現在の位置と経路探索でのエッジケースへの更なる対応が挙げられます。全体として、Minecraft Mod での DNS 処理を実験的に再現するには十分な構造を持つコードと言えます。