package com.example.examplemod.DoNotTouch.TestItem;

import com.example.examplemod.DoNotTouch.Networking.EndPoints;
import com.example.examplemod.DoNotTouch.Networking.NetworkUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public class TestItem extends Item {
    public TestItem() {
        super(new Properties().tab(CreativeModeTab.TAB_COMBAT));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (!pLevel.isClientSide) {
            HashMap<String, String> payload = new HashMap<>();
            payload.put("name", "A");
            payload.put("bossId", "1");
            payload.put("spawned", "true");
            NetworkUtils.performApiPostRequest(EndPoints.DEFEATED_BOSS.getEndPointPath(), payload);
        }
        return super.use(pLevel, pPlayer, pUsedHand);
    }
}
