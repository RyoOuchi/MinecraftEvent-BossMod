package com.example.examplemod.Items.CodeFileItem;

import com.example.examplemod.Screens.BrowserScreen.BrowserDisplayScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CodeFileItem extends Item {
    public CodeFileItem() {
        super(new Properties().tab(CreativeModeTab.TAB_COMBAT));
    }

    // for debugging purpose
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        final ItemStack itemStack = pPlayer.getMainHandItem();
        final String string = itemStack.getOrCreateTag().getString("code");
        System.err.println("CodeFileItem used! code:\n" + string);

        if (pLevel.isClientSide) {
            String html = """
<h1>Welcome to MineNet!</h1>
This is a <b>test page</b> rendered inside Minecraft.<br>
Try <i>scrolling</i> or <color=#00FF00>colored text</color>.<br>
Visit <a href="https://example.com">Example</a> for more!
""";
            Minecraft.getInstance().setScreen(new BrowserDisplayScreen(html));
        }

        return super.use(pLevel, pPlayer, pUsedHand);
    }
}
