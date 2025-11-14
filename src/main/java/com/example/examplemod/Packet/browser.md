# コードドキュメント

## 概要
このクラス `BrowserResponsePacket` は、サーバーからクライアントへ「ブラウザ表示用データ」を送信するためのネットワークパケットを定義している。  
主に、サーバーがリクエストに対して生成した HTML 形式やテキストデータなどをクライアントに返し、  
クライアント側で `BrowserDisplayScreen`（ブラウザ画面）として表示する役割を担う。

Minecraft Forge のネットワーク API を利用しており、サーバー→クライアント通信を安全に非同期実行する設計となっている。

---

## グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `pos` | `BlockPos` | コンストラクタ引数で指定 | `encode()`, `decode()`, `handleClient()` | 表示対象ブロックの座標を示す。主にブラウザブロックの位置。 |
| `message` | `String` | コンストラクタ引数で指定 | `encode()`, `decode()`, `handleClient()` | ブラウザに表示する本文データ（例：HTMLやレスポンス文字列）。 |
| `fileName` | `String` | コンストラクタ引数で指定 | `encode()`, `decode()` | サーバー上のファイル名またはレスポンス元識別子。UI では未使用だが、後続機能拡張に備えた設計。 |

---

## 関数一覧

### `public BrowserResponsePacket(BlockPos pos, String message, String fileName)`
- **概要**: 受信パケットの初期化コンストラクタ。
- **引数**:
    - `pos`: 対象ブロック（例：ブラウザ）のワールド座標。
    - `message`: サーバーからのレスポンスメッセージ。
    - `fileName`: 関連ファイル名（HTMLやテキストデータの識別用）。
- **処理**: クラス内の3つのフィールドを初期化。
- **呼び出し元**: サーバー側でクライアントにレスポンスパケットを生成する際に呼び出される。

---

### `public static void encode(BrowserResponsePacket msg, FriendlyByteBuf buf)`
- **概要**: サーバー側で送信するパケットをバイト列に変換する（シリアライズ処理）。
- **引数**:
    - `msg`: 送信対象の `BrowserResponsePacket` インスタンス。
    - `buf`: Forge の `FriendlyByteBuf` バッファオブジェクト。
- **主な処理**:
    1. ブロック座標 (`pos`) を書き込む。
    2. メッセージ (`message`) を UTF-8 文字列として書き込む。
    3. ファイル名 (`fileName`) を UTF-8 文字列として書き込む。
- **呼び出し元**: Forge のネットワーク送信処理。
- **備考**: パケットはバイト単位でネットワーク経由し、クライアント側 `decode()` で復元される。

---

### `public static BrowserResponsePacket decode(FriendlyByteBuf buf)`
- **概要**: クライアント側で受信したパケットをオブジェクトに復元する（デシリアライズ）。
- **引数**:
    - `buf`: サーバーから送信されたシリアライズ済みのデータ。
- **戻り値**: 復元された `BrowserResponsePacket` インスタンス。
- **処理内容**:
    1. `readBlockPos()` により座標を読み取る。
    2. `readUtf()` でメッセージ本文を取得。
    3. もう一度 `readUtf()` でファイル名を取得。
    4. 取得データを基に新しい `BrowserResponsePacket` を生成して返す。

---

### `public static void handle(BrowserResponsePacket msg, Supplier<NetworkEvent.Context> ctx)`
- **概要**: Forge ネットワークイベントに基づき、受信パケットの処理を行うエントリーポイント。
- **引数**:
    - `msg`: デコード済みのパケットデータ。
    - `ctx`: Forge のネットワークイベントコンテキスト。
- **主な処理**:
    1. `enqueueWork()` により、メインスレッド上で安全に処理をスケジュール。
    2. クライアント側専用メソッド `handleClient()` を呼び出す。
    3. `setPacketHandled(true)` によりパケット処理済みを通知。
- **呼び出し元**: Forge のネットワークディスパッチャ（クライアント側受信時）。
- **エラーハンドリング**:
    - 非同期処理中に例外が発生した場合、Forge によって安全に処理停止される。

---

### `@OnlyIn(Dist.CLIENT) private static void handleClient(BrowserResponsePacket msg)`
- **概要**: クライアント環境でブラウザ画面を表示する処理。
- **注釈**: `@OnlyIn(Dist.CLIENT)` によりクライアント専用コードであることを明示。
- **処理内容**:
    1. `Minecraft.getInstance()` により現在のゲームクライアントインスタンスを取得。
    2. `setScreen()` を呼び出し、`BrowserDisplayScreen` を新たに開く。
    3. コンストラクタ引数 `msg.message` を渡して、サーバーから受け取ったデータを画面に表示。
- **呼び出し元**: `handle()` の内部から `enqueueWork()` 経由で呼び出される。

---

## 呼び出し関係図（関数依存）
```
サーバー側
↓
encode() ──→ FriendlyByteBuf
↓
ネットワーク経由（送信）
↓
クライアント側
↓
decode()
↓
handle()
└─→ handleClient()
└─→ Minecraft.setScreen(new BrowserDisplayScreen(message))
```
---

## 外部依存関係

| クラス / パッケージ | 目的 | 使用箇所 |
|----------------------|------|-----------|
| `net.minecraftforge.network.NetworkEvent` | Forge のネットワークイベント管理 | `handle()` |
| `net.minecraft.network.FriendlyByteBuf` | パケットデータのシリアライズ／デシリアライズ | `encode()`, `decode()` |
| `net.minecraft.client.Minecraft` | クライアントメインインスタンス管理 | `handleClient()` |
| `BrowserDisplayScreen` | ブラウザUIの描画処理 | `handleClient()` |
| `@OnlyIn(Dist.CLIENT)` | クライアント限定コードの指定 | `handleClient()` |

---

## ファイル別概要

| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| `BrowserResponsePacket.java` | サーバー→クライアント間のブラウザデータ転送を管理 | `encode()`, `decode()`, `handle()`, `handleClient()` |
| `BrowserDisplayScreen.java` | 受信メッセージを画面に描画するクライアントUI | `render()`, `init()`, `setText()` |

---

## 技術的補足
- Forge のネットワーク API はスレッドセーフ性を保証しないため、`enqueueWork()` によりメインスレッド上で UI 更新処理を行っている。
- `@OnlyIn(Dist.CLIENT)` によって、クライアント以外（サーバー側）でこのメソッドが誤って呼ばれないように制御されている。
- `BrowserDisplayScreen` は、HTML風のテキスト表示や簡易的なレンダリング処理を提供するカスタムGUIクラスである。

---

## 総括
`BrowserResponsePacket` はサーバーからクライアントへの **UI更新用パケット** を扱うクラスであり、  
特に Minecraft 内で「ウェブブラウザ画面を表示する」ための通信インターフェースとして機能している。

パケットのエンコード／デコードから UI 表示までの流れはシンプルだが、  
非同期処理・クライアント専用処理・画面遷移の三要素を適切に分離しており、  
安全かつ明確なネットワーク通信設計が実現されている。