# 📘 コードドキュメント

## 概要
このファイルに定義された `ServerBlockEntity` クラスは、Minecraft Mod 内におけるサーバブロックの実装です。ブラウザ側のクライアントと TCP ハンドシェイクを行い、URL（ファイルパス）を受け取ってそのファイル内容を返すという一連のやり取りをシミュレートします。サーバは各クライアントごとに状態を保持し、スライディングウィンドウ方式でデータを送受信します。また、ファイルシステムや端末コマンドの実行といった仮想 OS 機能も備えています。主な処理の流れは以下のとおりです。

- **ハンドシェイク処理**：クライアントから `TCP_HANDSHAKE` クエリを受け取ると `handleHandshake()` で SYN/ACK のやり取りを行い、各クライアントに対して `ConnectionState` を設定する。
- **受信フェーズ**：クライアントから URL データを受信 (`TCP_ESTABLISHED`) する際は `handleReceivePhase()` が呼び出され、順序制御（SEQ 番号の検証）、重複検出、再送通知を行いながらデータ片を蓄積する。すべてのデータが届き終わると `handleCompleteDataTransfer()` でファイルを読み込み、送信フェーズへ移行する。
- **送信フェーズ**：サーバからクライアントへファイル内容を返す際は `handleSendPhase()` が呼び出され、`SlidingWindow` を用いたウィンドウ制御でデータパケットを送信する。ACK を受け取るたびにウィンドウを更新し、重複 ACK が検出された場合は `fastRetransmit()` によって迅速な再送を行う。
- **切断処理**：`TCP_DISCONNECT` クエリを受け取ると `handleDisconnect()` で各種状態をクリーンアップし、クライアントへ切断応答を返す。
- **ファイルシステム操作**：受信した URL からファイルを読み込む際は `FileSystem` を介してファイル内容を取得し、`CommandInterpreter` を通じてサーバ端末コマンドの実行結果を返すこともできる。

---

## 🌐 グローバル変数一覧
| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|-------|----|---------|-----------|------|
| `clientSyncMap` | `Map<BlockPos, ConnectionState>` | 空の `HashMap` | `handleHandshake()`, `fastRetransmit()`, `sendFileData()`, `initializeClientStateIfNeeded()` など | クライアントごとに TCP の SEQ/ACK 状態を保持する。ハンドシェイク完了後の送受信基点となる。 |
| `clientSeqMap` | `Map<BlockPos, Map<Integer, Body>>` | 空の `HashMap` | `handleReceivePhase()`, `advanceExpectedSeq()` | 各クライアントから受信したデータ片を SEQ 番号に応じて保持する。`TreeMap` を使うことで順序を維持する。 |
| `expectedSeqMap` | `Map<BlockPos, Integer>` | 空の `HashMap` | `handleReceivePhase()`, `advanceExpectedSeq()`, `sendAckResponse()`, `fastRetransmit()` | 各クライアントごとに次に受信すべき SEQ 番号を記録。順序検査および ACK 生成に用いる。 |
| `duplicateAckCount` | `Map<BlockPos, Integer>` | 空の `HashMap` | `handleReceivePhase()`, `handleOutOfOrderPacket()`, `handleDuplicateAcks()` | 重複 ACK を受け取った回数を記録し、閾値に達すると高速再送 (`fastRetransmit()`) をトリガする。 |
| `endReceivedMap` | `Map<BlockPos, Boolean>` | 空の `HashMap` | `sendAckResponse()`, `handleCompleteDataTransfer()` | クライアントから END フラグが届いたかどうかを保持。全データ受信完了の判定に用いる。 |
| `serverResponseMap` | `Map<BlockPos, String>` | 空の `HashMap` | `handleCompleteDataTransfer()`, `fastRetransmit()`, `sendFileData()` | クライアントごとに返送すべきファイル内容を保持。ファイル読込後に格納し、送信フェーズ中に参照される。 |
| `lastAckNumberMap` | `Map<BlockPos, Integer>` | 空の `HashMap` | `handleDuplicateAcks()` | 前回受け取った ACK 番号を記録し、同一 ACK の連続を検出する。 |
| `receivePhaseMap` | `Map<BlockPos, Boolean>` | 空の `HashMap` | `handleDataTransfer()` | クライアントごとの処理フェーズを表す。`true` の場合は受信フェーズ、`false` の場合は送信フェーズ。 |
| `fileSystem` | `FileSystem` | 新規インスタンス | `handleCompleteDataTransfer()`, `saveAdditional()`, `load()`, `executeCommand()` | 仮想ファイルシステム。ファイルの読み書きやシリアライズ／デシリアライズを行う。 |
| `interpreter` | `CommandInterpreter` | `new CommandInterpreter(fileSystem, this)` | `executeCommand()`, `sendTerminalOutputToClient()` | 仮想端末のコマンドインタプリタ。サーバブロック経由でコマンドを実行し、その結果をクライアントに送ることができる。 |
| `readyToTransmit` | `boolean` | `false` | `tickServer()` | 現在のパケット送信中かどうかを示すフラグ。キューから新しい送信タスクを取り出す前に参照される。 |
| `currentPacket` | `DataPacket` | `null` | `tickServer()` | 送信中のデータパケットを一時的に保持する。送信完了後に `null` に戻される。 |
| `transmissionQueue` | `Queue<TransmissionTask>` | 空の `LinkedList` | `tickServer()`, `queueTransmission()` | 送信待機中のタスクを FIFO で保持するキュー。各タスクはパケットと送信先ルータを含む。 |
| `slidingWindowMap` | `Map<BlockPos, SlidingWindow>` | 空の `HashMap` | `handleHandshake()`, `handleSendPhase()`, `sendFileData()`, `sendPacketWithSlidingWindow()`, `tickServer()` | クライアントごとの送信ウィンドウを管理する。サーバ→クライアントへのデータ送信時にウィンドウサイズや未確認パケットを追跡する。 |
| `worldPosition` | `BlockPos` | 親クラスより | 多数 | ブロックエンティティの座標。ログ出力やルータ検索に使用する。 |
| `level` | `Level` | 親クラスより | `tickServer()`, `receiveDataPacket()`, `load()`, `saveAdditional()` | ワールドコンテキスト。クライアント／サーバ判定やエンティティ取得に利用する。 |

---

## 🧩 関数一覧

### `tickServer() -> void`
- **概要**: サーバ側のティックごとに呼ばれ、送信キューの管理やタイムアウトパケットの再送を行うメインループ。
- **主な処理**:
    1. `level` が `null` またはクライアントサイドの場合は何もしない。
    2. `readyToTransmit` が `false` かつ `transmissionQueue` が空でない場合、キューから次の `TransmissionTask` を取り出し、そのパケットを対応するルータ (`WifiRouterBlockEntity`) に送信 (`transmitPacket()`) する。送信開始後は `readyToTransmit = true`、`currentPacket` に保存。
    3. `readyToTransmit` が `true` の場合、送信が完了したとみなしログを出して `resetTransmissionState()` で状態をリセット。
    4. `slidingWindowMap` に登録された各クライアントについて、`SlidingWindow.getTimedOutPackets()` でタイムアウトしたパケットがないか検査し、存在すれば `retransmitTimedOutPackets()` を呼んで再送処理を行う。
- **呼び出し元**: サーバブロックの毎ティック処理。
- **呼び出し先**: `transmitPacket()`, `resetTransmissionState()`, `retransmitTimedOutPackets()`。

### `resetTransmissionState() -> void`
- **概要**: 送信中フラグと現在のパケットを初期状態に戻す。
- **処理内容**: `readyToTransmit` を `false` にし、`currentPacket` を `null` に設定。
- **呼び出し元**: `tickServer()`。

### `queueTransmission(DataPacket packet, WifiRouterBlockEntity router) -> void`
- **概要**: 指定したパケットと送信先ルータを送信キューに登録する。
- **引数**: `packet` – 送信すべきデータパケット。`router` – パケット送信先の `WifiRouterBlockEntity`。いずれかが `null` の場合は処理しない。
- **処理内容**: 新しい `TransmissionTask` (内部クラス) を生成し、`transmissionQueue.offer()` でキューに追加。ログに送信予定のルータ位置を出力。
- **呼び出し元**: `handleDisconnect()`, `sendAckResponse()`, `fastRetransmit()`, `sendPacketWithSlidingWindow()`, `retransmitTimedOutPackets()` など。

### `receiveDataPacket(DataPacket packet) -> void`
- **概要**: ルータから転送されたデータパケットを受け取り、パケットに含まれるクエリタイプに応じて適切な処理関数を呼び出す。
- **主な処理**:
    1. 受信データを `String` 化してログ出力する。
    2. `Header` を生成してシーケンス番号 (`SEQ`)、確認番号 (`ACK`) を取得。
    3. パケットのクエリタイプ (`Queries`) を取得し、以下のいずれかに分岐：
        - **`TCP_HANDSHAKE`**: `handleHandshake()` を呼び出してハンドシェイク処理を行う。
        - **`TCP_ESTABLISHED`**: `handleDataTransfer()` を呼び出してデータ転送処理を行う（受信フェーズ／送信フェーズで更に分岐）。
        - **`TCP_DISCONNECT`**: `handleDisconnect()` を呼び出してクライアントとの接続をクリーンアップする。
        - **その他**: 未知のクエリタイプとしてログ出力。
- **呼び出し元**: ルータ (`WifiRouterBlockEntity`) から DNS サーバブロックへパケットが届いた際に呼び出される。
- **呼び出し先**: `handleHandshake()`, `handleDataTransfer()`, `handleDisconnect()`。

### `handleDisconnect(DataPacket packet) -> void`
- **概要**: クライアントからの切断要求 (`TCP_DISCONNECT`) を処理し、サーバ側に保持しているクライアントごとの状態を全て削除する。
- **処理内容**:
    1. `clientPos = packet.getClientBlockPos()` で切断元クライアントの位置を取得。
    2. `clientSyncMap`, `expectedSeqMap`, `clientSeqMap`, `duplicateAckCount`, `endReceivedMap`, `serverResponseMap`, `receivePhaseMap`, `lastAckNumberMap` から該当クライアントのエントリを削除。
    3. 応答用パケットを作成するために、`packet` を `invertRouterPath()` し `swapSenderAndReceiver()` してエラーコードを `NOERROR` に更新。
    4. この応答パケットを `queueTransmission()` で下のルータへ送信キューに登録。
- **呼び出し元**: `receiveDataPacket()`。

### `handleDataTransfer(DataPacket packet) -> void`
- **概要**: データ転送クエリ (`TCP_ESTABLISHED`) を処理し、受信フェーズ（クライアント→サーバ）または送信フェーズ（サーバ→クライアント）に応じて処理を振り分ける。
- **処理内容**:
    1. パケットのクライアント位置を `clientPos` として取得し、`clientSyncMap` にエントリがない場合は不正なクライアントとしてログ出力して終了。
    2. `receivePhaseMap.getOrDefault(clientPos, true)` により現在のフェーズを取得。`true` なら受信フェーズとして `handleReceivePhase()` を、`false` なら送信フェーズとして `handleSendPhase()` を呼び出す。
- **呼び出し元**: `receiveDataPacket()`。

### `handleReceivePhase(DataPacket packet, BlockPos clientPos) -> void`
- **概要**: URL データの受信フェーズで呼び出され、クライアントからのデータパケットを処理する。順序検証や重複検出、ACK 送信などを行う。
- **主な処理**:
    1. パケットから `Message` と `Header` を取得し、SEQ、ACK、END の各フィールドを読み込む。
    2. `shouldDropPacket(0.2)` により 20% の確率でパケットロスをシミュレートする。ロスした場合はログを出して処理を終了。
    3. ボディ (`Body`) を取り出し、ペイロードの長さを計算してログ出力。
    4. 必要であれば `initializeClientStateIfNeeded()` を呼び、`clientSeqMap`, `expectedSeqMap`, `duplicateAckCount` を初期化。
    5. `handlePacketProcessing()` を呼び、受信した SEQ 番号に応じて順序通りかどうかを判定し、適宜データを `clientSeqMap` に格納したり `expectedSeqMap` を更新したりする。
    6. 処理後、`sendAckResponse()` を呼び出して ACK パケットを生成・送信する。
- **呼び出し元**: `handleDataTransfer()`。

### `handleSendPhase(DataPacket packet, BlockPos clientPos) -> void`
- **概要**: サーバ→クライアントの送信フェーズで呼び出され、受信した ACK パケットを処理し、スライディングウィンドウの更新や高速再送を行う。
- **主な処理**:
    1. 受信データのヘッダから ACK 番号を取得し、`handleDuplicateAcks()` を呼んで重複 ACK に応じた処理（重複 ACK カウンタの更新および `fastRetransmit()` の呼び出し）を行う。
    2. `slidingWindowMap.get(clientPos)` から該当クライアントのウィンドウを取得し、`SlidingWindow.acknowledge(ack)` により ACK を反映。
    3. `logSlidingWindowStatus()` を呼び、ウィンドウの現在状態（未確認パケット数や最後に ACK された SEQ）をログ出力。
    4. ウィンドウが空いていれば `sendPacketWithSlidingWindow()` を呼び出し次のデータパケットを送信し、全てのデータが ACK された場合はログを出す。
- **呼び出し元**: `handleDataTransfer()`。

### `initializeClientStateIfNeeded(BlockPos clientPos) -> void`
- **概要**: 指定クライアントに対する受信フェーズ用のデータ構造を初期化する。
- **処理内容**: `clientSeqMap` に `TreeMap` を割り当て、`expectedSeqMap` に `ConnectionState` から得た `serverSeq` を初期値として設定し、`duplicateAckCount` を 0 に初期化する。
- **呼び出し元**: `handleReceivePhase()`。

### `handlePacketProcessing(int seq, Body body, Map<Integer, Body> delivered, int expectedSeq, BlockPos clientPos, int payloadLength) -> void`
- **概要**: 受信したパケットの SEQ 番号をもとに、順序通りか否かを判定し、適切な処理関数へ振り分ける。
- **処理内容**:
    1. `delivered.containsKey(seq)` で重複受信かチェック。重複の場合はログのみ出力。
    2. `seq > expectedSeq` なら `handleOutOfOrderPacket()` を呼び、データを一時バッファに格納して重複 ACK を送る。
    3. `seq == expectedSeq` なら `handleInOrderPacket()` を呼び、順序通りのデータを `delivered` に格納して `expectedSeq` を更新する。
- **呼び出し元**: `handleReceivePhase()`。

### `handleOutOfOrderPacket(int seq, Body body, Map<Integer, Body> delivered, int expectedSeq, BlockPos clientPos) -> void`
- **概要**: 期待する SEQ より大きい（未来の）パケットを受信した場合に呼ばれ、一時保存と重複 ACK カウントの更新を行う。
- **処理内容**:
    1. 受信した SEQ を `delivered.put(seq, body)` で保存。
    2. `duplicateAckCount` のカウンタを 1 増やし、ログに重複 ACK 送信を記録。
- **呼び出し元**: `handlePacketProcessing()`。

### `handleInOrderPacket(int seq, Body body, Map<Integer, Body> delivered, int expectedSeq, BlockPos clientPos, int payloadLength) -> void`
- **概要**: 期待していた SEQ 番号のパケットを受信した場合に呼ばれ、データを即座に配達し次に期待する SEQ を更新する。
- **処理内容**:
    1. `delivered.put(seq, body)` でデータを格納し、`expectedSeq += payloadLength` により次に期待する SEQ を進める。
    2. `advanceExpectedSeq()` を呼び、連続してバッファにあるデータがあればさらに `expectedSeq` を更新する。
- **呼び出し元**: `handlePacketProcessing()`。

### `advanceExpectedSeq(Map<Integer, Body> delivered, int expectedSeq, BlockPos clientPos) -> void`
- **概要**: バッファに連続して格納されている後続データを検出し、`expectedSeq` をまとめて進める。
- **処理内容**: `delivered` のキーを走査し、`expectedSeq` に一致する SEQ があればそのボディ長分 `expectedSeq` を加算。何度も進む場合はループで継続。更新後は `expectedSeqMap.put(clientPos, expectedSeq)` とし、`duplicateAckCount` を 0 にリセット。
- **呼び出し元**: `handleInOrderPacket()`。

### `handleDuplicateAcks(int ack, BlockPos clientPos, DataPacket packet) -> void`
- **概要**: 送信フェーズで受信した ACK が重複しているかを検出し、必要に応じて高速再送をトリガする。
- **処理内容**:
    1. `lastAckNumberMap.getOrDefault(clientPos, -1)` と比較し、同一 ACK なら `duplicateAckCount` を増やす。
    2. 重複回数が 1 以上になった場合、`fastRetransmit()` を呼び出して指定の SEQ 番号からデータを再送する。
    3. 異なる ACK の場合はカウンタを 0 に戻し、`lastAckNumberMap` を更新。
- **呼び出し元**: `handleSendPhase()`。

### `logSlidingWindowStatus(SlidingWindow slidingWindow, int ack) -> void`
- **概要**: 現在のスライディングウィンドウの状態を詳細にログ出力するための補助メソッド。
- **出力内容**: 最後に ACK された SEQ、未確認パケットの数、その詳細など。
- **呼び出し元**: `handleSendPhase()`。

### `sendAckResponse(DataPacket packet, int expectedSeq, Map<Integer, Body> delivered, int end, BlockPos clientPos) -> void`
- **概要**: 受信フェーズの処理後に ACK パケットを構築してクライアントへ送信する。必要に応じて全データ受信完了後の処理を行う。
- **主な処理**:
    1. `NetworkUtils.createAckPacket(expectedSeq)` で ACK 番号を持つ TCP ヘッダを作成。
    2. 元パケットを用いて送信者／受信者を入れ替え、ルータ経路を反転し、クエリタイプを `TCP_ESTABLISHED` に更新して ACK パケットを生成。
    3. END フラグ (`end`) が立っている場合は `endReceivedMap.put(clientPos, true)` として最終パケット受信を記録。
    4. END 受信済みかつ `isDeliveredDataContiguous(delivered)` が `true` の場合は `handleCompleteDataTransfer()` を呼び出し、ファイル読み込み・送信準備に移る。そうでない場合は現在再構築済みのデータをログに出力。
    5. クライアントへの ACK パケットを `queueTransmission()` で送信キューに登録し、配達済みデータの内容や再構築文字列をデバッグ出力。
- **呼び出し元**: `handleReceivePhase()`。

### `handleCompleteDataTransfer(DataPacket ackPacket, Map<Integer, Body> delivered, BlockPos clientPos, DataPacket originalPacket, BlockEntity routerEntity) -> void`
- **概要**: すべてのデータが連続して受信され、END フラグも受信済みの場合に呼び出される。受信した URL をファイルパスとして解釈し、そのファイル内容を取得して送信フェーズへ移行する。
- **主な処理**:
    1. `NetworkUtils.reconstructData(delivered)` でクライアントから受け取った全データを組み合わせ、ファイルパス文字列を得る。
    2. パスに `/` が含まれている場合はファイル名以降を取り出し、`fileSystem.cat()` で内容を取得する。ファイルが存在しない場合は `handleFileNotFound()` を呼ぶ。
    3. ファイル内容が取得できたら `serverResponseMap.put(clientPos, fileContents)` に保存し、`receivePhaseMap.put(clientPos, false)` として送信フェーズに切り替える。
    4. `slidingWindowMap.get(clientPos)` でクライアント用のスライディングウィンドウを取得し、`sendFileData()` を呼び出してファイル内容を分割送信する。
- **呼び出し元**: `sendAckResponse()`。

### `handleFileNotFound(DataPacket ackPacket, int expectedSeq, BlockPos clientPos, BlockEntity routerEntity) -> void`
- **概要**: 指定されたファイルが見つからない場合に呼び出され、クライアントに切断パケットを送信する。
- **処理内容**: ACK パケットのクエリタイプを `TCP_DISCONNECT` に変更し、下のルータへ送信する。ログでファイルが存在しない旨を出力。
- **呼び出し元**: `handleCompleteDataTransfer()`。

### `fastRetransmit(WifiRouterBlockEntity responseRouter, int ackNumber, BlockPos clientPos, DataPacket originalPacket) -> void`
- **概要**: 送信フェーズ中に重複 ACK が検出された際に呼び出され、失われた可能性のあるデータチャンクを再送する。
- **処理内容**:
    1. `serverResponseMap.get(clientPos)` から送信すべきデータ全体を取得し、`clientSyncMap.get(clientPos)` からサーバ SEQ を取得。
    2. `ackNumber` を基準に再送すべきデータのオフセットを計算し、`SlidingWindow.MAX_DATA_SIZE` 分のチャンクを取り出す。
    3. `NetworkUtils.createTcpPacket()` で TCP ヘッダを再構築し、元パケットを `swapSenderAndReceiver()`／`invertRouterPath()` して新しい `DataPacket` に入れ替える。
    4. `queueTransmission()` で該当ルータに再送パケットを登録する。
- **呼び出し元**: `handleDuplicateAcks()`。

### `getRouterBelowServer() -> @Nullable WifiRouterBlockEntity`
- **概要**: サーバブロック直下に配置されているルータブロックを取得する。存在しない場合は `null` を返し、ログにエラーを出力。
- **呼び出し元**: `tickServer()`, `handleHandshake()`, `fastRetransmit()`, `sendFileData()` など。

### `sendFileData(WifiRouterBlockEntity responseRouter, DataPacket clientRequestPacket, byte[] fileContents, SlidingWindow window) -> void`
- **概要**: ファイル内容をクライアントへ送るためにチャンクへ分割し、各チャンクを `SlidingWindow` に登録して送信する。
- **主な処理**:
    1. 入力データが `null` または空の場合はログ出力して終了。
    2. `window` が `null` の場合はハンドシェイク未完了として警告を出して終了。
    3. `clientSyncMap` からクライアントの `ConnectionState` を取得し、サーバ SEQ (`serverSeq`) とクライアント SEQ (`clientSeq`) を基点としてチャンクごとに SEQ/ACK を設定。
    4. `SlidingWindow.MAX_DATA_SIZE` ごとにファイルを分割し、各チャンクに対して `NetworkUtils.createTcpPacket()` でヘッダを付けたバイト列を生成。
    5. 元パケットを `updateQueryType(Queries.TCP_ESTABLISHED)`→`updateData()`→`updateErrorCode(ErrorCodes.NOERROR)`→`invertRouterPath()`→`swapSenderAndReceiver()` と更新し、新しいケーブル経路 (`NetworkUtils.getCablePathToNextRouter()`) を設定して最終パケットとし、`window.queueData()` に登録。
    6. 準備完了後に `sendPacketWithSlidingWindow()` を呼び出して送信を開始。
- **呼び出し元**: `handleCompleteDataTransfer()`。

### `sendPacketWithSlidingWindow(WifiRouterBlockEntity responseRouter, SlidingWindow slidingWindow) -> void`
- **概要**: スライディングウィンドウに登録されている未送信パケットを取得し、送信キューに登録する。
- **処理内容**:
    1. `slidingWindow.getPacketsToSend()` で送信候補パケット一覧を取得。
    2. 各パケットに対して `queueTransmission()` を呼び、ルータへ送信登録する。
    3. 送信するパケットの SEQ 番号をログ出力。
- **呼び出し元**: `handleSendPhase()`, `sendFileData()`。

### `isDeliveredDataContiguous(Map<Integer, Body> deliveredMap) -> boolean`
- **概要**: `NetworkUtils.isDataContiguous()` を呼び、受信済みデータが連続しているかどうかを判定するヘルパ。
- **呼び出し元**: `sendAckResponse()`。

### `retransmitTimedOutPackets(WifiRouterBlockEntity responseRouter, List<DataPacket> timedOutPackets) -> void`
- **概要**: スライディングウィンドウにおいて送信したまま ACK が得られずタイムアウトしたパケットを再送する。
- **処理内容**: 各 `timedOutPacket` に対して `queueTransmission()` を呼び出し、送信キューに登録。SEQ 番号をログ出力。
- **呼び出し元**: `tickServer()`。

### `boolean shouldDropPacket(double lossProbability)`
- **概要**: 指定した確率で `true` を返し、パケットロスをシミュレートするユーティリティ。
- **使用箇所**: `handleReceivePhase()` にて受信パケットの破棄をランダムに模擬する。
- **呼び出し元**: `handleReceivePhase()`。

### `handleHandshake(DataPacket packet, int clientSynNumber, int clientAckNumber) -> void`
- **概要**: TCP ハンドシェイク処理を担当し、クライアント→サーバの初期接続確立を行う。クライアントから送られてくる SYN/ACK の値に応じて 2 段階で処理する。
- **処理内容**:
    1. **初回送信 (clientAckNumber == -1)**: クライアントから初めて SYN を受信した場合。サーバは `ackNumber = clientSynNumber + 1`、`serverSynNumber = random()` を生成し、`ConnectionState` を新規作成して `clientSyncMap` に保存。ルータ経路を反転 (`invertRouterPath()`)、送受信者を入れ替え (`swapSenderAndReceiver()`)、データ部にヘッダを設定して応答パケットを作成し、下のルータへ送信キューに登録する。
    2. **二回目送信 (clientSynNumber == -1)**: クライアントから 2 回目のハンドシェイクパケットを受信した場合。`clientSyncMap` に記録された `ConnectionState.validateServerAckNumber()` で ACK 番号を検証し、正しければ接続確立。`serverSeq` をインクリメントし、`expectedSeqMap`, `duplicateAckCount`, `clientSeqMap`, `receivePhaseMap` を初期化。さらに `SlidingWindow` を生成して `slidingWindowMap` に保存する。
    3. その他のケースではログにエラーを出力し握手失敗とする。
- **呼び出し元**: `receiveDataPacket()`。

### `executeCommand(String command) -> String`
- **概要**: 仮想端末のコマンドを実行し、その標準出力を文字列として返す。
- **処理内容**: `System.setOut()` で出力を `ByteArrayOutputStream` に一時的に切り替え、`interpreter.execute()` を実行したのち元の `System.out` に戻す。結果文字列を整形して返す。
- **呼び出し元**: 外部 (GUI やスクリプト) がサーバブロックに OS コマンドを送る際に使用。
- **戻り値**: コマンド実行結果の文字列。例外発生時は空文字列を返す。

### `sendTerminalOutputToClient(ServerPlayer player, String output) -> void`
- **概要**: クライアントプレイヤーに端末出力と現在のディレクトリを通知する。
- **処理内容**: `interpreter.getContext().getCurrentDirectory().getFullPath()` でカレントディレクトリを取得し、`ExampleMod.CHANNEL.sendTo()` を使用して `TerminalOutputPacket` を送信する。
- **呼び出し元**: コマンド実行後の結果をクライアントに返す際に使用。
- **外部依存**: Forge ネットワーク API。クライアントにパケットを送るために `NetworkDirection.PLAY_TO_CLIENT` を指定する。

### `saveAdditional(CompoundTag tag)`, `load(CompoundTag tag)`
- **概要**: ブロックエンティティの状態を NBT に保存／ロードする。特に `FileSystem` の内容を `VirtualFileSystem` タグとして保存・復元する。
- **処理内容**: `saveAdditional()` では `fileSystem.saveToNBT()` の結果をタグに挿入し、`load()` ではそれを読み出して `fileSystem.loadFromNBT()` に渡す。
- **呼び出し元**: Minecraft の保存処理により自動的に呼び出される。

---

## 🔁 呼び出し関係図（関数依存）
サーバブロック内での関数呼び出しの流れを以下に示します。主要な分岐と依存関係がわかるよう簡略化したものです。
```
tickServer()
├─ queueTransmission()        …送信タスクを登録
├─ resetTransmissionState()  …送信フラグをリセット
└─ retransmitTimedOutPackets()  …未ACKのパケットを再送

receiveDataPacket()
├─ handleHandshake()         …TCP ハンドシェイク処理
├─ handleDataTransfer()      …データ転送処理
│    ├─ handleReceivePhase()
│    │    ├─ initializeClientStateIfNeeded()
│    │    ├─ handlePacketProcessing()
│    │    │    ├─ handleOutOfOrderPacket()
│    │    │    └─ handleInOrderPacket()
│    │    │         └─ advanceExpectedSeq()
│    │    └─ sendAckResponse()
│    │         ├─ handleCompleteDataTransfer()
│    │         │    ├─ handleFileNotFound()
│    │         │    └─ sendFileData()
│    │         │         ├─ sendPacketWithSlidingWindow()
│    │         └─ queueTransmission()
│    └─ handleSendPhase()
│         ├─ handleDuplicateAcks()
│         │    └─ fastRetransmit()
│         └─ sendPacketWithSlidingWindow()
└─ handleDisconnect()
└─ queueTransmission()
```
---

## ⚙️ 外部依存関係
| クラス／ライブラリ | 用途と理由 |
|------------------|-----------|
| **`ExampleMod.SERVER_BLOCK_ENTITY`** | Forge のブロックエンティティ登録 ID。`ServerBlockEntity` をゲームに登録するためにコンストラクタで指定する。 |
| **`WifiRouterBlockEntity`** | ルータブロックの実装。パケット送信 (`transmitPacket()`)、DNSServer へのリクエスト送信や応答の中継に使われる。サーバは自身の真下に設置されたルータへパケットを送る。 |
| **`ConnectionState`** | TCP 接続の SEQ/ACK 番号や状態を保持するデータ構造。クライアントのハンドシェイク情報を追跡するために `clientSyncMap` に保存される。 |
| **`DataPacket`** | ネットワーク上を流れるパケットを表すクラス。データ部やヘッダ (`Header`)、ルータ経路やケーブル経路の情報を持ち、`updateData()`, `invertRouterPath()`, `swapSenderAndReceiver()`, `updateErrorCode()` などで内容を変換する。 |
| **`Header`, `Message`, `Body`** | `DataPacket` に含まれるヘッダ・メッセージ・ボディのラッパークラス。ヘッダから `SEQ`、`ACK`、`END` などを取り出したり、ボディからペイロードを抽出したりする。 |
| **`Queries`** | ネットワーク通信のフェーズを表す列挙型。`TCP_HANDSHAKE`, `TCP_ESTABLISHED`, `TCP_DISCONNECT` が定義されており、`receiveDataPacket()` の分岐に使用される。 |
| **`ErrorCodes`** | ネットワーク通信におけるエラーコードを表す列挙型。`NOERROR`, `NXDOMAIN`, `FORMERR` など。サーバは基本的に `NOERROR` を使用するが、DNS やファイル不存在の場合に利用する。 |
| **`NetworkUtils`** | TCP パケットの生成 (`createTcpPacket()`)、ACK パケットの作成 (`createAckPacket()`)、データ再構築 (`reconstructData()`)、データ連続性チェック (`isDataContiguous()`)、ケーブル経路探索 (`getCablePathToNextRouter()`) など、多数のネットワークユーティリティを提供する。 |
| **`SlidingWindow`** | スライディングウィンドウ方式による送信制御を実装したクラス。ウィンドウサイズ、未確認パケットの管理、タイムアウト検出などを行う。サーバはクライアントごとにこのクラスをインスタンス化して `slidingWindowMap` に保持する。 |
| **`FileSystem`** | 仮想ファイルシステムの実装。パスの解決やファイル内容の読み書き (`cat()`) を行い、NBT への保存 (`saveToNBT()`)、ロード (`loadFromNBT()`) に対応する。 |
| **`CommandInterpreter`** | 仮想端末のコマンドインタプリタ。`execute()` によりコマンドを実行し、結果を標準出力に書き込む。 |
| **`TerminalOutputPacket`**, **`NetworkDirection`** | Forge ネットワーク API。端末出力とカレントディレクトリをクライアントに送る際に使用される。 |
| **`ServerPlayer`** | Minecraft のサーバプレイヤーオブジェクト。端末出力を送るときに宛先として使用される。 |
| **`StandardCharsets`** | 文字列を UTF-8 バイト配列に変換する際に使用。 |
| **Minecraft 基本クラス** (`BlockPos`, `BlockState`, `BlockEntity`, `CompoundTag`, `Level`) | ブロック座標の取り扱いや NBT シリアライズ、ワールド情報アクセスに使用される基本クラス群。 |

---

## 📄 ファイル別概要
| ファイル／クラス名 | 主な責務 | 主要メソッド |
|-----------------|-----------|--------------|
| **`ServerBlockEntity.java`** | サーバブロックエンティティの実装。本クラスは TCP ハンドシェイク、クライアントからの URL データ受信、ファイル読込、スライディングウィンドウによるデータ送信、重複 ACK 対応、タイムアウト再送を包含した複雑なネットワークシミュレーションを行う。 | `tickServer()`, `receiveDataPacket()`, `handleHandshake()`, `handleDataTransfer()`, `handleReceivePhase()`, `handleSendPhase()`, `handleCompleteDataTransfer()`, `fastRetransmit()`, `sendFileData()`, `sendPacketWithSlidingWindow()` など |
| **`WifiRouterBlockEntity.java`** | ネットワークルータを模したブロックエンティティ。ケーブルからパケットを受け取りサーバまたはブラウザへ転送する。`transmitPacket()` や `receiveTransmittedPacket()` を通じてデータ転送の中継点となる。 |
| **`FileSystem.java` / `CommandInterpreter.java`** | 仮想 OS 機能を提供するクラス。ファイルシステムはファイルの読み書きと NBT への保存/復元を担当し、コマンドインタプリタはシェルコマンドの実行と出力取得を担当する。 | `cat()`, `saveToNBT()`, `loadFromNBT()`, `execute()` |
| **`SlidingWindow.java`** | スライディングウィンドウ方式によるフロー制御クラス。サーバ・クライアント双方でデータ送信管理に使用され、未 ACK パケットの再送やウィンドウ空きの確認を行う。 | `queueData()`, `getPacketsToSend()`, `acknowledge()`, `getTimedOutPackets()` |
| **`NetworkUtils.java`** | ネットワーク関連ユーティリティ集。ACK パケット生成、TCP パケット生成、データ再構築、ケーブル経路探索など通信に必要な低レベル機能をまとめる。 |
| **`TerminalOutputPacket.java`** | サーバ端末の出力をクライアントに送るためのネットワークパケットクラス。 |

---

## 💬 総評
`ServerBlockEntity` クラスは、Minecraft の世界内で仮想的な TCP サーバを実装する複雑なコードであり、ハンドシェイクからデータ受信・ファイル読込・データ送信まで一貫したプロトコル処理が行われています。クライアントごとに複数の状態を `Map` に保持し、非同期・断続的なパケット到着に対応するための仕組みが豊富に盛り込まれています。特にスライディングウィンドウによるフロー制御や重複 ACK による高速再送など、実際の TCP の挙動を模倣している点が特徴です。

改善点としては、コード全体の規模が大きくネストが深いため、処理をサービスクラスなどに分割することで可読性やテスト容易性を向上させることが考えられます。また、エラーハンドリングがコンソール出力に依存しているため、より堅牢な異常処理やユーザー通知の仕組みを追加すると実用性が高まるでしょう。それでも、本実装は教育的な目的やゲーム内シミュレーションにおいて十分な機能を提供しており、ネットワークプロトコルの理解を深める教材としても価値があります。