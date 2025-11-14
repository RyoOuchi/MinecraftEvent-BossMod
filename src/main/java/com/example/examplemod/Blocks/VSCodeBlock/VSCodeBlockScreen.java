package com.example.examplemod.Blocks.VSCodeBlock;

import com.example.examplemod.Items.CodeFileItem.CodeFileItem;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.Screens.Screen.CustomScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class VSCodeBlockScreen extends AbstractContainerScreen<VSCodeBlockContainer> {
    private final BlockPos pos;
    private final BlockEntity blockEntity;
    private final ResourceLocation GUI = new ResourceLocation(ExampleMod.MODID, "textures/gui/my_block_gui.png");
    private Button editButton;

    public VSCodeBlockScreen(VSCodeBlockContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        pos = container.getPos();
        blockEntity = container.getBlockEntity();
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        editButton = this.addRenderableWidget(new Button(
                x + 10,
                y + 10,
                60,
                20,
                new TextComponent("Edit"),
                button -> Minecraft.getInstance().setScreen(new CustomScreen(pos, getCode(), blockEntity, getFileName()))
        ));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (editButton != null) {
            editButton.active = hasCodeFileItem();
        }
    }

    private boolean hasCodeFileItem() {
        if (!(blockEntity instanceof VSCodeBlockEntity vsCodeBlockEntity)) return false;
        final ItemStack itemInSlot = vsCodeBlockEntity.getItemInSlot();
        return itemInSlot.getItem() instanceof CodeFileItem;
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShaderTexture(0, GUI);

        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;

        this.blit(pPoseStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    private String getCode() {
        if (!hasCodeFileItem()) return "";
        final VSCodeBlockEntity vsCodeBlockEntity = (VSCodeBlockEntity) blockEntity;
        final ItemStack stack = vsCodeBlockEntity.getItemInSlot();
        return getCode(stack);
    }


    public static void setCode(ItemStack stack, String code, String fileName) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("code", code);
        tag.putString("fileName", fileName);
    }

    public static String getCode(ItemStack stack) {
        if (!stack.hasTag()) return "";
        return stack.getTag().getString("code");
    }

    public static String getFileName(ItemStack stack) {
        if (!stack.hasTag()) return "";
        return stack.getTag().getString("fileName");
    }

    private String getFileName() {
        if (!hasCodeFileItem()) return "";
        final VSCodeBlockEntity vsCodeBlockEntity = (VSCodeBlockEntity) blockEntity;
        final ItemStack stack = vsCodeBlockEntity.getItemInSlot();
        return getFileName(stack);
    }
}
