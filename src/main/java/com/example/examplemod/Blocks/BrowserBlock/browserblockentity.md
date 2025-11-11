# 📘 コードドキュメント

## 概要
本クラスはブラウザ機能を模倣したブロックエンティティであり、仮想 DNS 問い合わせ、TCP ハンドシェイク、スライディングウィンドウによるデータ転送、ACK 再送制御、コネクションの切断などを実装している。プレイヤーがブラウザブロックから URL を入力すると、このエンティティは対応するサーバ・ブロックに接続し、TCP の状態遷移に従ってデータ受信と再構築を行い、最終的にクライアントへ結果を返す。主な処理の流れは以下の通りである。

- DNS 応答を受け取り、指定したドメイン名に対応するサーバ位置（`BlockPos`）を取得する。
- クライアント初期シーケンス番号をランダムに生成し、TCP ハンドシェイク（SYN／SYN+ACK／ACK）を完了する。
- `SlidingWindow` を使用してデータを分割・送信し、ACK の確認に応じて次のデータを送る。
- タイムアウト・重複 ACK による再送制御を行う。
- すべてのデータを再構成し、受信完了後にサーバへ切断要求を送る。
- クライアント側 GUI にデータ（ファイル内容やメッセージ）を返す。

---

## 🌐 グローバル変数一覧
| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `serverIPAddress` | `BlockPos` | `null` | `receiveDNSResponse()`, `receiveHandshakeServerResponse()`, `handleEstablishedDataSegment()` | サーバブロックの座標（DNS応答後に設定） |
| `urlData` | `byte[]` | `null` | `sendUrlData()`, `fastRetransmit()`, `setUrlData()` | クライアントが送信するURLデータ |
| `duplicateAckCount` | `int` | `0` | `handleEstablishedAckSegment()`, `handleEstablishedDataSegment()` | 重複ACKをカウントし、閾値超過で再送をトリガ |
| `lastAckNumber` | `int` | `-1` | `handleEstablishedAckSegment()` | 直近のACK番号。重複ACK検出用 |
| `slidingWindow` | `SlidingWindow` | `null` | 各データ送信関数・再送関数 | TCP送信制御（スライディングウィンドウ方式） |
| `connectionState` | `ConnectionState` | `null` | `receiveHandshakeServerResponse()`, `fastRetransmit()` | TCPのSEQ/ACK状態管理 |
| `receivedDataChunks` | `Map<Integer, Body>` | `new TreeMap<>()` | `handleEstablishedDataSegment()` | 受信したデータ片をSEQ順に保持 |
| `expectedSeqNumber` | `int` | `-1` | `handleEstablishedDataSegment()` | 次に期待するシーケンス番号 |
| `endFlagReceived` | `boolean` | `false` | `handleEstablishedDataSegment()` | 最終パケット受信フラグ |

---

## 🧩 関数一覧

### `convertUrlToByte(String url) -> byte[]`
- **概要**: URL文字列をUTF-8エンコードしてバイト列に変換。
- **呼び出し元**: `sendUrlData()` など。
- **補足**: Java標準の `StandardCharsets.UTF_8` を使用。

---

### `receiveDNSResponse(DataPacket dnsResponsePacket, WifiRouterBlockEntity responseRouter)`
- **概要**: DNS応答を解析し、成功ならサーバ位置を取得してTCPハンドシェイクを開始。
- **主な処理**:
    1. `ErrorCodes` に応じてハンドラを切り替える。
    2. `NXDOMAIN` や `FORMERR` はエラーメッセージをクライアントへ返す。
    3. `NOERROR` なら `convertBytesToBlockPos()` でサーバ位置を特定し、初期SEQ番号を生成して `performServerRequest()` を呼び出す。
- **呼び出し元**: DNSルータ (`WifiRouterBlockEntity`)。
- **呼び出し先**: `convertBytesToBlockPos()`, `performServerRequest()`, `sendBrowserResponseToClient()`。

---

### `tickServer()`
- **概要**: サーバ側で定期的に呼ばれ、スライディングウィンドウのタイムアウトパケットを検出して再送。
- **呼び出し先**: `slidingWindow.getTimedOutPackets()`, `retransmitTimedOutPackets()`。
- **呼び出し元**: Minecraftサーバのtick処理。
- **ログ出力**: 再送対象パケットのSEQ番号と再送結果。

---

### `shouldDropPacket(double lossProbability) -> boolean`
- **概要**: 疑似的にパケットロスを発生させる関数。0〜1の確率で `true` を返す。
- **使用箇所**: `handleEstablishedDataSegment()` 内でランダムドロップを模擬。

---

### `receiveHandshakeServerResponse(DataPacket serverResponsePacket, WifiRouterBlockEntity responseRouter)`
- **概要**: サーバからの SYN+ACK を受信してACKを返し、TCP確立状態に移行。
- **主な処理**:
    1. `validateClientAckNumber()` でACK番号を検証。
    2. 問題なければ `performServerRequest()` でACKを送信。
    3. `SlidingWindow` を初期化して `sendUrlData()` を呼ぶ。
- **呼び出し先**: `performServerRequest()`, `sendUrlData()`。

---

### `receiveDisconnectServerResponse(DataPacket packet, WifiRouterBlockEntity blockEntity)`
- **概要**: サーバから切断応答を受け取り、全変数をリセット。
- **役割**: 接続終了後の後始末。
- **副作用**: 全グローバル変数を初期化 (`serverIPAddress`, `urlData`, `slidingWindow` など)。

---

### `receiveEstablishedServerResponse(DataPacket packet, WifiRouterBlockEntity blockEntity)`
- **概要**: 確立状態で受信したデータ／ACKを処理。
- **主な処理**:
    1. `Header.getResponseNumber()` によって分岐。
    2. データセグメントなら `handleEstablishedDataSegment()`、ACKなら `handleEstablishedAckSegment()` を呼ぶ。

---

### `handleEstablishedDataSegment(EstablishedContext ctx)`
- **概要**: 受信したデータセグメントを順序確認し、再構成。
- **ロジック**:
    - 順序通り受信 → `expectedSeqNumber` 更新。
    - 未来パケット → 一時バッファに保存。
    - 重複 → 無視。
- **再送制御**: 欠落データがある場合は `duplicateAckCount` により再送を誘発。
- **最終処理**:
    - ENDフラグ検出で `DISCONNECT` パケットを送信。
    - `sendBrowserResponseToClient()` で結果を返す。

---

### `handleEstablishedAckSegment(EstablishedContext ctx)`
- **概要**: ACK応答を処理し、ウィンドウ更新と高速再送を制御。
- **条件分岐**:
    - 同一ACK番号が続く → `fastRetransmit()` を呼ぶ。
    - ACK更新 → 未確認パケットを更新。
- **次の送信**:
    - ウィンドウに空きがあれば `sendPacketWithSlidingWindow()` で次の送信を行う。

---

### `fastRetransmit(WifiRouterBlockEntity responseRouter, int ackNumber)`
- **概要**: 重複ACKを受けた際の高速再送を行う。
- **動作**:
    - 該当SEQに対応するURLデータ部分を再構築。
    - `NetworkUtils.createTcpPacket()` でパケット生成。
    - 再送を `performServerRequest()` で実施。

---

### `sendPacketWithSlidingWindow(WifiRouterBlockEntity responseRouter)`
- **概要**: スライディングウィンドウ内で送信可能なデータパケットを送信。
- **処理内容**:
    - `getPacketsToSend()` で送信候補を取得。
    - 各パケットをルータ経由で送信。
    - 送信SEQをログ出力。

---

### `sendUrlData(WifiRouterBlockEntity responseRouter, DataPacket serverResponsePacket)`
- **概要**: URLデータをチャンク分割して送信。
- **主要アルゴリズム**:
    1. `NetworkUtils.createDataChunks()` でチャンク化。
    2. 各チャンクにSEQ番号とENDフラグを付与。
    3. `SlidingWindow.queueData()` で送信キューへ登録。
    4. 実際の送信は `sendPacketWithSlidingWindow()` に委譲。

---

### `sendBrowserResponseToClient(String message, String fileName)`
- **概要**: サーバサイドの処理結果をクライアントに返す。
- **実装**:
    - Forge のネットワークチャンネル (`ExampleMod.CHANNEL`) を利用。
    - `PacketDistributor.TRACKING_CHUNK` により同チャンク内プレイヤーへ送信。

---

### `convertBytesToBlockPos(byte[] bytes) -> BlockPos`
- **概要**: DNS応答データ（文字列化された座標）を `BlockPos` に変換。
- **例外処理**: 不正フォーマット時は `IllegalArgumentException` をスロー。

---

## 🔁 呼び出し関係図（関数依存）
```
receiveDNSResponse()
├─ convertBytesToBlockPos()
├─ sendBrowserResponseToClient()        (NXDOMAIN, FORMERR)
└─ performServerRequest() [SYN送信]

receiveHandshakeServerResponse()
├─ connectionState.validateClientAckNumber()
├─ performServerRequest() [ACK送信]
├─ SlidingWindow(...)  (初期化)
└─ sendUrlData()
├─ NetworkUtils.createDataChunks()
├─ NetworkUtils.createTcpPacket()
├─ NetworkUtils.getCablePathToNextRouter()
├─ slidingWindow.queueData()
└─ sendPacketWithSlidingWindow()
└─ performServerRequest() [データ送信]

receiveEstablishedServerResponse()
├─ handleEstablishedDataSegment()  (レスポンス番号==1)
│   ├─ shouldDropPacket()
│   ├─ NetworkUtils.createAckPacket()
│   ├─ performServerRequest() [ACK送信]
│   ├─ NetworkUtils.reconstructData()
│   ├─ NetworkUtils.isDataContiguous()
│   ├─ performServerRequest() [DISCONNECT送信]
│   └─ sendBrowserResponseToClient()
└─ handleEstablishedAckSegment()  (その他)
├─ fastRetransmit()  (重複ACK検出)
├─ slidingWindow.acknowledge()
├─ sendPacketWithSlidingWindow()
└─ performServerRequest() [データ送信]

fastRetransmit()
├─ NetworkUtils.createTcpPacket()
└─ performServerRequest() [再送]

tickServer()
└─ retransmitTimedOutPackets()
└─ performServerRequest() [再送]
```

---

## ⚙️ 外部依存関係
- **`WifiRouterBlockEntity`**: ルータ機能。パケット送信と経路シミュレーション。
- **`NetworkUtils`**: パケット生成・再構成ユーティリティ。
- **`SlidingWindow`**: TCPウィンドウ制御の実装。
- **`ConnectionState`**: SEQ/ACK 状態管理。
- **`ExampleMod.CHANNEL`**: Forge通信API。

---

## 💬 総評
`BrowserBlockEntity` は Minecraft 世界内での仮想TCP通信を再現しており、学習・デバッグに適した設計となっている。通信状態・再送制御・順序保証などの要素が整理されており、ログ出力も豊富で可視化性が高い。