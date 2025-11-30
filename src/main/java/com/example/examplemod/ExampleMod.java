package com.example.examplemod;

import com.example.examplemod.DoNotTouch.ChristmasTree.ChristmasTree;
import com.example.examplemod.DoNotTouch.ChristmasTree.ChristmasTreeBlock;
import com.example.examplemod.DoNotTouch.ChristmasTree.ChristmasTreeRenderer;
import com.example.examplemod.DoNotTouch.apolloboss.ApolloBossRenderer;
import com.example.examplemod.DoNotTouch.Packets.ConfirmTeamPacket;
import com.example.examplemod.DoNotTouch.Packets.OpenGuiPacket;
import com.example.examplemod.DoNotTouch.Packets.SummonEntityPacket;
import com.example.examplemod.DoNotTouch.apolloboss.ApolloBoss;
import com.example.examplemod.DoNotTouch.apolloboss.ApolloChocolate;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.MinecraftForge;
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
import software.bernie.geckolib3.GeckoLib;

@Mod(ExampleMod.MODID)
public class ExampleMod {

    //MODID
    public static final String MODID = "examplemod";

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public ExampleMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        GeckoLib.initialize();
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // ==================== 絶対触らないで！====================
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(packetId++, SummonEntityPacket.class, SummonEntityPacket::encode, SummonEntityPacket::decode, SummonEntityPacket::handle);
            CHANNEL.registerMessage(packetId++, OpenGuiPacket.class, OpenGuiPacket::encode, OpenGuiPacket::decode, OpenGuiPacket::handle);
            CHANNEL.registerMessage(packetId++, ConfirmTeamPacket.class, ConfirmTeamPacket::encode, ConfirmTeamPacket::decode, ConfirmTeamPacket::handle);
        });
        // ====================================================
        // ここならいいよ！
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // ==================== 絶対触らないで！====================
        EntityRenderers.register(CHRISTMAS_TREE_ENTITY, ChristmasTreeRenderer::new);
        EntityRenderers.register(APOLLO_BOSS, ApolloBossRenderer::new);
        // ====================================================
        // ここならいいよ！
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        private static final RegisterBlockData[] registerBlocks = {
                // ここにBlockを書いてね！
        };

        private static final Item[] registerItems = {

        };

        @SubscribeEvent
        public static void onBiomeRegistry(final RegistryEvent.Register<Biome> event) {

        }

        @SubscribeEvent
        public static void onAttributeCreation(final EntityAttributeCreationEvent event) {
            // ==================== 絶対触らないで！====================
            event.put(CHRISTMAS_TREE_ENTITY, ChristmasTree.setAttributes());
            event.put(APOLLO_BOSS, ApolloBoss.setAttributes());
            // ====================================================
            // ここならいいよ！

        }

        @SubscribeEvent
        public static void onEntitiesRegistry(final RegistryEvent.Register<EntityType<?>> event) {
            // ==================== 絶対触らないで！====================
            event.getRegistry().register(CHRISTMAS_TREE_ENTITY.setRegistryName(MODID, "christmas_tree_entity"));
            event.getRegistry().register(APOLLO_BOSS.setRegistryName(MODID,"apollo_boss"));
            // ====================================================
            // ここならいいよ！

        }

        // ======================================================================================================
        // ここから下はいじらないよ！

        private static void setupBiome(Biome biome, int weight, BiomeManager.BiomeType biomeType, BiomeDictionary.Type... types) {
            ResourceKey<Biome> key = ResourceKey.create(ForgeRegistries.Keys.BIOMES, ForgeRegistries.BIOMES.getKey(biome));

            BiomeDictionary.addTypes(key, types);
            BiomeManager.addBiome(biomeType, new BiomeManager.BiomeEntry(key, weight));
        }

        private static final RegisterBlockData blockData = new RegisterBlockData(CHRISTMAS_TREE_BLOCK);

        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> event) {
            LOGGER.info("HELLO from Register Block");
            for (RegisterBlockData data : registerBlocks) {
                event.getRegistry().register(data.block);
            }

            event.getRegistry().register(blockData.block);
        }

        @SubscribeEvent
        public static void onItemsRegistry(final RegistryEvent.Register<Item> event) {
            for (RegisterBlockData data : registerBlocks) {
                event.getRegistry().register(new BlockItem(data.block, new Item.Properties().tab(data.creativeModeTab)).setRegistryName(data.block.getRegistryName()));
            }

            event.getRegistry().register(new BlockItem(blockData.block, new Item.Properties().tab(blockData.creativeModeTab)).setRegistryName(blockData.block.getRegistryName()));

            for (Item item : registerItems) {
                event.getRegistry().register(item);
            }

            event.getRegistry().register(APOLLO_CHOCOLATE);
            event.getRegistry().register(APOLLO_SPAWN_EGG);
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

    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int packetId = 0;

    public static final EntityType<ChristmasTree> CHRISTMAS_TREE_ENTITY = EntityType.Builder.of(ChristmasTree::new, MobCategory.CREATURE)
            .sized(0.9f, 1.4f)
            .setTrackingRange(32)
            .setShouldReceiveVelocityUpdates(true)
            .build("christmas_tree_entity");

    public static final Block CHRISTMAS_TREE_BLOCK = new ChristmasTreeBlock().setRegistryName(MODID, "christmas_tree_base");

    public static final EntityType<ApolloBoss> APOLLO_BOSS=EntityType.Builder.of(ApolloBoss::new, MobCategory.CREATURE)
            .sized(2F,5F)
            .setTrackingRange(32)
            .setShouldReceiveVelocityUpdates(true)
            .build("apollo_boss");

    public static final Item APOLLO_SPAWN_EGG=
            new SpawnEggItem(APOLLO_BOSS,
                    0xFF0000,
                    0x00FF00,
                    new Item.Properties().tab(CreativeModeTab.TAB_MISC)
            ).setRegistryName(MODID,"apollo_spawn_egg");

    public static final Item APOLLO_CHOCOLATE=new ApolloChocolate().setRegistryName(MODID,"apollo_chocolate");
}
