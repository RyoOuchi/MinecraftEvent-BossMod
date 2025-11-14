package com.example.examplemod.Events;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.ServerData.ServerSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class OnMinecraftInstanceLaunched extends Event {

    /**
     * ワールドが読み込まれたときに呼び出されるイベントハンドラ。
     * ワールド（特にサーバーワールド）が起動したタイミングで、
     * セーブデータ中に登録されているサーバーブロック情報をチェックし、
     * すでに存在しない（壊された、または別ブロックに置き換えられた）サーバーを削除する。
     */
    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        System.out.println("World instance launched!"); // ワールド読み込みイベントの発火確認用ログ

        final var levelAccessor = event.getWorld();

        // LevelAccessorがLevel型であり、かつクライアント側でない（サーバー側）場合のみ処理を実行
        if (levelAccessor instanceof Level level && !level.isClientSide()) {
            System.out.println("Server world loaded: " + level.dimension().location());

            // サーバーごとのセーブデータを取得（ServerSavedDataはmod独自のセーブ管理クラス）
            final ServerSavedData data = ServerSavedData.get((ServerLevel) level);

            // データ内のサーバー情報を確認し、不正なものを取得
            List<BlockPos> toRemove = getBlockPos(level, data);

            // 不正サーバーを削除（ConcurrentModificationException回避のため別ループで削除）
            toRemove.forEach(data::removeServer);

            // 削除が発生した場合はログを出力
            if (!toRemove.isEmpty()) {
                System.out.println("🧹 Cleaned up " + toRemove.size() + " invalid servers from saved data.");
            }
        }
    }

    /**
     * セーブデータに登録されているサーバー位置情報を確認し、
     * 実際にその位置にあるブロックが「SERVER_BLOCK」でなければ削除対象として返す。
     *
     * @param level 現在のワールド（サーバー側）
     * @param data  サーバーセーブデータ（登録されているサーバー一覧）
     * @return 削除すべき不正なサーバーのBlockPosリスト
     */
    private static List<BlockPos> getBlockPos(Level level, ServerSavedData data) {
        final var servers = data.getServers(); // Map<String, BlockPos> のような構造（サーバー名→位置）

        // 削除対象を一時的に格納するリスト
        List<BlockPos> toRemove = new ArrayList<>();

        // 登録済みの全サーバーを走査
        servers.forEach((serverName, serverBlockPos) -> {
            final var block = level.getBlockState(serverBlockPos).getBlock();

            // 該当位置のブロックが正しいServerBlockであるか確認
            if (block.equals(ExampleMod.SERVER_BLOCK)) {
                System.out.println("✅ Server [" + serverName + "] is valid at " + serverBlockPos);
            } else {
                // 不正なブロックの場合は削除リストに追加
                System.out.println("❌ Server [" + serverName + "] is invalid at " + serverBlockPos + ". Marking for removal.");
                toRemove.add(serverBlockPos);
            }
        });

        return toRemove;
    }

}