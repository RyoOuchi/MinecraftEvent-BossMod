package com.example.examplemod;

import com.example.examplemod.Blocks.BrowserBlock.BrowserBlock;
import com.example.examplemod.Blocks.BrowserBlock.BrowserBlockEntity;
import com.example.examplemod.Blocks.CableBlock.CableBlockEntity;
import com.example.examplemod.Blocks.DNSServerBlock.DNSServerBlock;
import com.example.examplemod.Blocks.DNSServerBlock.DNSServerBlockEntity;
import com.example.examplemod.Blocks.ServerBlock.ServerBlock;
import com.example.examplemod.Blocks.ServerBlock.ServerBlockEntity;
import com.example.examplemod.Packet.*;
import com.example.examplemod.Blocks.CableBlock.CableBlock;
import com.example.examplemod.Items.CodeFileItem.CodeFileItem;
import com.example.examplemod.Blocks.VSCodeBlock.VSCodeBlock;
import com.example.examplemod.Blocks.VSCodeBlock.VSCodeBlockContainer;
import com.example.examplemod.Blocks.VSCodeBlock.VSCodeBlockEntity;
import com.example.examplemod.Blocks.WifiRouterBlock.WifiRouterBlock;
import com.example.examplemod.Blocks.WifiRouterBlock.WifiRouterBlockEntity;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.example.examplemod.Blocks.VSCodeBlock.VSCodeBlockScreen;


@Mod(ExampleMod.MODID)
public class ExampleMod {

    //MODID
    public static final String MODID = "examplemod";

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("examplemod", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );


    private static int packetId = 0;

    public static final Block VSCODE_BLOCK = new VSCodeBlock().setRegistryName(MODID, "vscode_block");

    public static final BlockEntityType<VSCodeBlockEntity> VSCODE_BLOCK_ENTITY = BlockEntityType.Builder
            .of(VSCodeBlockEntity::new, VSCODE_BLOCK)
            .build(null);

    public static final MenuType<VSCodeBlockContainer> VS_CODE_BLOCK_CONTAINER = IForgeMenuType.create((windowId, inv, data) ->
            new VSCodeBlockContainer(windowId, data.readBlockPos(), inv, inv.player));

    public static final Item CODE_FILE_ITEM = new CodeFileItem().setRegistryName(MODID, "code_file_item");

    public static final Block WIFI_ROUTER = new WifiRouterBlock().setRegistryName(MODID, "wifi_router");

    public static final BlockEntityType<WifiRouterBlockEntity> WIFI_ROUTER_ENTITY = BlockEntityType.Builder
            .of(WifiRouterBlockEntity::new, WIFI_ROUTER)
            .build(null);

    public static final Block BROWSER_BLOCK = new BrowserBlock().setRegistryName(MODID, "browser_block");

    public static final BlockEntityType<BrowserBlockEntity> BROWSER_BLOCK_ENTITY = BlockEntityType.Builder
            .of(BrowserBlockEntity::new, BROWSER_BLOCK)
            .build(null);

    public static final Block CABLE_BLOCK = new CableBlock().setRegistryName(MODID, "cable_block");

    public static final BlockEntityType<CableBlockEntity> CABLE_BLOCK_ENTITY = BlockEntityType.Builder
            .of(CableBlockEntity::new, CABLE_BLOCK)
            .build(null);

    public static final Block DNSSERVER_BLOCK = new DNSServerBlock().setRegistryName(MODID, "dns_server_block");

    public static final BlockEntityType<DNSServerBlockEntity> DNSSERVER_BLOCK_ENTITY = BlockEntityType.Builder
            .of(DNSServerBlockEntity::new, DNSSERVER_BLOCK)
            .build(null);

    public static final Block SERVER_BLOCK = new ServerBlock().setRegistryName(MODID, "server_block");

    public static final BlockEntityType<ServerBlockEntity> SERVER_BLOCK_ENTITY = BlockEntityType.Builder
            .of(ServerBlockEntity::new, SERVER_BLOCK)
            .build(null);

    public ExampleMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(packetId++, SaveCodePacket.class, SaveCodePacket::encode, SaveCodePacket::decode, SaveCodePacket::handle);
            CHANNEL.registerMessage(packetId++, RequestUrlPacket.class, RequestUrlPacket::encode, RequestUrlPacket::decode, RequestUrlPacket::handle);
            CHANNEL.registerMessage(packetId++ , AddServerPacket.class, AddServerPacket::encode, AddServerPacket::decode, AddServerPacket::handle);
            CHANNEL.registerMessage(packetId++, BrowserResponsePacket.class, BrowserResponsePacket::encode, BrowserResponsePacket::decode, BrowserResponsePacket::handle);
            CHANNEL.registerMessage(packetId++, TerminalCommandPacket.class, TerminalCommandPacket::encode, TerminalCommandPacket::decode, TerminalCommandPacket::handle);
            CHANNEL.registerMessage(packetId++, TerminalOutputPacket.class, TerminalOutputPacket::encode, TerminalOutputPacket::decode, TerminalOutputPacket::handle);
        });
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(VS_CODE_BLOCK_CONTAINER, VSCodeBlockScreen::new);
        });
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        private static final RegisterBlockData[] registerBlocks = {
                new RegisterBlockData(VSCODE_BLOCK),
                new RegisterBlockData(WIFI_ROUTER),
                new RegisterBlockData(BROWSER_BLOCK),
                new RegisterBlockData(CABLE_BLOCK),
                new RegisterBlockData(DNSSERVER_BLOCK),
                new RegisterBlockData(SERVER_BLOCK)

        };

        private static final Item[] registerItems = {
                // ここにItemを書いてね！
                CODE_FILE_ITEM
        };

        @SubscribeEvent
        public static void onBlockEntityRegistry(final RegistryEvent.Register<BlockEntityType<?>> event) {
            event.getRegistry().register(VSCODE_BLOCK_ENTITY.setRegistryName(MODID, "vscode_block_entity"));
            event.getRegistry().register(BROWSER_BLOCK_ENTITY.setRegistryName(MODID, "browser_block_entity"));
            event.getRegistry().register(WIFI_ROUTER_ENTITY.setRegistryName(MODID, "wifi_router_block_entity"));
            event.getRegistry().register(DNSSERVER_BLOCK_ENTITY.setRegistryName(MODID, "dns_server_block_entity"));
            event.getRegistry().register(CABLE_BLOCK_ENTITY.setRegistryName(MODID, "cable_block_entity"));
            event.getRegistry().register(SERVER_BLOCK_ENTITY.setRegistryName(MODID, "server_block_entity"));
        }

        @SubscribeEvent
        public static void onBiomeRegistry(final RegistryEvent.Register<Biome> event) {

        }

        @SubscribeEvent
        public static void onAttributeCreation(final EntityAttributeCreationEvent event) {

        }

        @SubscribeEvent
        public static void onEntitiesRegistry(final RegistryEvent.Register<EntityType<?>> event) {

        }

        @SubscribeEvent
        public static void onRegisterMenuType(final RegistryEvent.Register<MenuType<?>> event) {
            event.getRegistry().register(VS_CODE_BLOCK_CONTAINER.setRegistryName(MODID, "vs_code_block_container"));
        }

        // ======================================================================================================
        // ここから下はいじらないよ！

        private static void setupBiome(Biome biome, int weight, BiomeManager.BiomeType biomeType, BiomeDictionary.Type... types) {
            ResourceKey<Biome> key = ResourceKey.create(ForgeRegistries.Keys.BIOMES, ForgeRegistries.BIOMES.getKey(biome));

            BiomeDictionary.addTypes(key, types);
            BiomeManager.addBiome(biomeType, new BiomeManager.BiomeEntry(key, weight));
        }

        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> event) {
            LOGGER.info("HELLO from Register Block");
            for (RegisterBlockData data : registerBlocks) {
                event.getRegistry().register(data.block);
            }
        }

        @SubscribeEvent
        public static void onItemsRegistry(final RegistryEvent.Register<Item> event) {
            for (RegisterBlockData data : registerBlocks) {
                event.getRegistry().register(new BlockItem(data.block, new Item.Properties().tab(data.creativeModeTab)).setRegistryName(data.block.getRegistryName()));
            }

            for (Item item : registerItems) {
                event.getRegistry().register(item);
            }
        }

        static class RegisterBlockData {
            Block block;
            CreativeModeTab creativeModeTab;

            public RegisterBlockData(Block block) {
                this.block = block;
                creativeModeTab = CreativeModeTab.TAB_BUILDING_BLOCKS;
            }

            public RegisterBlockData(Block block, CreativeModeTab creativeModeTab) {
                this.block = block;
                this.creativeModeTab = creativeModeTab;
            }
        }
    }
}
