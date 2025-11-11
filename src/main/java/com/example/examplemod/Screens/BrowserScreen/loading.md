# コードドキュメント：`BrowserLoadingScreen.java`

## 🧩 概要
このクラス `BrowserLoadingScreen` は、**Minecraft 内部で「ロード中」状態を視覚的に表示するための簡易ローディング画面（GUI）**を実装している。  
テクスチャ（`icons.png`）を回転アニメーション付きで描画し、ユーザーに処理中であることを示す。

主な目的は、ブラウザ画面（`BrowserDisplayScreen`）やネットワーク通信の結果を待機している間にプレイヤーへ視覚的なフィードバックを与えることである。

---

## 🧾 グローバル変数一覧

| 変数名 | 型 | 初期値 | 使用箇所 | 説明 |
|--------|----|--------|----------|------|
| `LOADING_TEXTURE` | `ResourceLocation` | `"textures/gui/icons.png"` | `render()` | 回転アイコン描画に使用するテクスチャリソース。Minecraft標準アイコンを利用。 |
| `rotationAngle` | `float` | `0f` | `render()` | 回転アニメーションの角度（度数法）。毎フレーム更新される。 |

---

## ⚙️ 関数一覧

### `BrowserLoadingScreen()`
- **概要**: コンストラクタ。画面タイトルを `"Loading..."` に設定する。
- **呼び出し元**: 他クラス（例: ブラウザ起動時やデータ読み込み処理中）から新しいローディング画面を表示する際に使用される。
- **呼び出し先**: `Screen` クラスのスーパークラスコンストラクタ。

---

### `init()`
- **戻り値**: なし (`void`)
- **概要**: GUI 初期化処理を行う。
- **主な処理**:
    - スーパークラスの `init()` を呼び出す。
    - 本クラスでは追加ウィジェットやボタンは存在しない。
- **呼び出し元**: Forge が画面初期化時に自動的に呼び出す。
- **呼び出し先**: `super.init()`

---

### `render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)`
- **戻り値**: なし (`void`)
- **概要**: 画面のフレームごとに描画を行う。テキスト「Loading...」と、回転するアイコンを中心に描画する。
- **主な処理**:
    1. 背景を描画 (`renderBackground(poseStack)`)。
    2. 画面中央 (`centerX`, `centerY`) を計算。
    3. テキスト `"Loading..."` を中央上部に描画。
    4. 回転アイコン描画処理：
        - `RenderSystem.setShaderTexture()` により `LOADING_TEXTURE` を設定。
        - `poseStack.pushPose()` で現在の座標変換を保存。
        - `poseStack.translate(centerX, centerY, 0)` により描画位置を画面中央へ移動。
        - `poseStack.mulPose(Vector3f.ZP.rotationDegrees(rotationAngle))` でZ軸回転を適用。
        - `blit()` により16x16ピクセルのアイコンを描画。
        - `poseStack.popPose()` で座標変換を復元。
    5. フレームごとに `rotationAngle` を更新：
       ```java
       rotationAngle = (rotationAngle + (partialTicks * 10f)) % 360f;
       ```  
       → 毎フレーム10度ずつ回転するように設定。
    6. 最後に `super.render()` を呼び出して他の要素を描画。
- **呼び出し元**: Minecraft の描画ループ。
- **呼び出し先**:
    - `renderBackground()`
    - `RenderSystem`（OpenGLラッパー）
    - `blit()`

---

### `isPauseScreen() -> boolean`
- **戻り値**: `false`
- **概要**: この画面が開いていてもゲーム全体を一時停止させない設定。
- **呼び出し元**: Minecraft の画面管理システム。
- **目的**: ローディング中もゲーム内部処理（データ通信やレンダリング）を継続させる。

---

## 🔗 呼び出し関係図
```
BrowserLoadingScreen()
└─ init()
└─ render()
├─ renderBackground()
├─ RenderSystem.setShaderTexture()
├─ poseStack.pushPose() / popPose()
├─ blit()
└─ super.render()
```
---

## 🧩 外部依存関係

| クラス名 | パッケージ | 用途 |
|-----------|-------------|------|
| `Screen` | `net.minecraft.client.gui.screens` | 画面描画のベースクラス。 |
| `PoseStack` | `com.mojang.blaze3d.vertex` | 描画座標や回転などの行列変換管理。 |
| `RenderSystem` | `com.mojang.blaze3d.systems` | OpenGL描画設定API。テクスチャやブレンドを制御。 |
| `ResourceLocation` | `net.minecraft.resources` | Minecraft のリソース（画像等）を参照するための識別子。 |
| `Vector3f` | `com.mojang.math` | 3D座標軸定義（ここではZ軸回転用）。 |
| `Minecraft` | `net.minecraft.client` | 現在のクライアントコンテキストを取得する。 |

---

## 📦 ファイル別概要

| ファイル名 | 主な責務 | 主な関数 |
|-------------|-----------|-----------|
| `BrowserLoadingScreen.java` | ローディング状態を示すアニメーション画面を表示する。 | `render()`, `init()`, `isPauseScreen()` |

---

## 🧭 処理全体の流れ（ストーリー形式）

1. サーバーやネットワークからデータを読み込む処理が開始されると、`BrowserLoadingScreen` が表示される。
2. `render()` が毎フレーム呼ばれ、背景と「Loading...」の文字が描画される。
3. 同時に、アイコンテクスチャが画面中央でゆっくり回転し続ける。
4. `rotationAngle` がフレームごとに増加し、360度を超えると0度に戻る。
5. ロード完了後、別の画面（例：`BrowserDisplayScreen`）に切り替わる。
6. `isPauseScreen()` が `false` のため、ゲーム処理は停止せずバックグラウンドで進行する。

---

## ⚙️ 設計上の特徴

- **軽量設計**  
  描画処理のみを行い、外部通信やボタン操作は一切行わない。
- **アニメーション表現**  
  回転角度を `partialTicks` に基づきフレーム補間しているため、フレームレートに依存しないスムーズな動き。
- **Minecraft標準リソース使用**  
  独自画像ではなく、`icons.png` を用いることで余分なリソース管理を不要にしている。
- **非ポーズ仕様**  
  ローディング画面中もバックエンドで処理を継続可能。

---

## 💡 まとめ
`BrowserLoadingScreen` は、Minecraft 内でのブラウザデータ読込中に表示される**シンプルで視覚的なローディングアニメーション画面**である。  
中心に回転するアイコンと「Loading...」テキストによってプレイヤーに処理中であることを明確に伝え、次の画面（データ読込完了後の `BrowserDisplayScreen`）への橋渡しを担う。
