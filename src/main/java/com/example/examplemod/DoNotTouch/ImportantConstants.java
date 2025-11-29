package com.example.examplemod.DoNotTouch;

import com.example.examplemod.DoNotTouch.apolloboss.ApolloBoss;
import com.example.examplemod.ExampleMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;

// For ease of access, important constants used throughout the mod can be defined here.
public class ImportantConstants {
    public static final String BACKEND_API_URL = "://minecraftevent-bossmod-backend.fly.dev";
    public static final EntityType<ApolloBoss> BOSS_ENTITY_TYPE = ExampleMod.APOLLO_BOSS;
    public static final String DISCORD_WEBHOOK_URL = System.getenv("DISCORD_WEBHOOK_URL");
}
