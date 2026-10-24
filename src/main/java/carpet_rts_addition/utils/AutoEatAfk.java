/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.utils;

import carpet.patches.EntityPlayerMPFake;
import carpet_rts_addition.settings.CarpetRtsAdditionSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Feeds idle players and Carpet fake players when their food level drops below the configured
 * threshold.
 *
 * <p>Eating goes through {@link ItemStack#finishUsingItem} so nutrition, saturation, consume
 * effects, sounds, particles and statistics all behave exactly like a normal meal.
 */
public final class AutoEatAfk
{
    /** Guards against eating an entire stack in a single second. */
    private static final long EAT_COOLDOWN_MILLIS = 1_000L;

    private static final Map<UUID, Long> LAST_EAT = new HashMap<>();
    private static String cachedFoodSpec;
    private static Set<Item> cachedFoods;

    private AutoEatAfk()
    {
    }

    public static void tick(MinecraftServer server)
    {
        if (!CarpetRtsAdditionSettings.autoEatAfk)
        {
            return;
        }

        long now = Util.getMillis();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (shouldFeed(player, now))
            {
                feed(player, now);
            }
        }
    }

    public static void forget(ServerPlayer player)
    {
        LAST_EAT.remove(player.getUUID());
    }

    public static void reset()
    {
        LAST_EAT.clear();
        cachedFoodSpec = null;
        cachedFoods = null;
    }

    private static boolean shouldFeed(ServerPlayer player, long now)
    {
        if (!matchesTarget(player) || player.isUsingItem())
        {
            return false;
        }
        if (player.getFoodData().getFoodLevel() >= CarpetRtsAdditionSettings.autoEatAfkThreshold)
        {
            return false;
        }
        Long lastEat = LAST_EAT.get(player.getUUID());
        return lastEat == null || now - lastEat >= EAT_COOLDOWN_MILLIS;
    }

    private static boolean matchesTarget(ServerPlayer player)
    {
        boolean fake = player instanceof EntityPlayerMPFake;
        String target = CarpetRtsAdditionSettings.autoEatAfkTarget;
        if ("fakePlayers".equals(target))
        {
            return fake;
        }
        if ("idlePlayers".equals(target))
        {
            return !fake && AfkProtection.isIdle(player);
        }
        return fake || AfkProtection.isIdle(player);
    }

    private static void feed(ServerPlayer player, long now)
    {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);
            if (!isAllowedFood(stack))
            {
                continue;
            }

            ItemStack result = stack.finishUsingItem(player.level(), player);
            if (result != stack)
            {
                inventory.setItem(slot, result);
            }
            LAST_EAT.put(player.getUUID(), now);
            return;
        }
    }

    private static boolean isAllowedFood(ItemStack stack)
    {
        if (stack.isEmpty() || stack.get(DataComponents.FOOD) == null)
        {
            return false;
        }
        Set<Item> allowed = allowedFoods();
        return allowed == null || allowed.contains(stack.getItem());
    }

    /**
     * @return the configured item whitelist, or {@code null} when every food is allowed
     */
    private static Set<Item> allowedFoods()
    {
        String spec = CarpetRtsAdditionSettings.autoEatAfkFoods;
        if (spec == null || spec.isBlank() || "*".equals(spec.trim()))
        {
            return null;
        }
        if (spec.equals(cachedFoodSpec))
        {
            return cachedFoods;
        }

        Set<Item> items = new HashSet<>();
        for (String token : spec.split(","))
        {
            String trimmed = token.trim();
            if (trimmed.isEmpty() || "*".equals(trimmed))
            {
                continue;
            }
            Identifier id = Identifier.tryParse(trimmed);
            if (id == null)
            {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item != Items.AIR)
            {
                items.add(item);
            }
        }

        cachedFoodSpec = spec;
        cachedFoods = items;
        return items;
    }
}
