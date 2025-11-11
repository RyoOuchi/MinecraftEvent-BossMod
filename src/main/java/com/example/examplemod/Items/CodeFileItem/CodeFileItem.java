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

}
