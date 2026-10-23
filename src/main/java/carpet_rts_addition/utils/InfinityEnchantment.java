/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.utils;

import carpet_rts_addition.settings.CarpetRtsAdditionSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Resolves the Infinity enchantment of the running server's datapack registry.
 *
 * <p>The registry is rebuilt on every reload, so the enchantment instance is looked up on demand
 * instead of being cached by identity.
 */
public final class InfinityEnchantment
{
    private static volatile MinecraftServer server;

    private InfinityEnchantment()
    {
    }

    public static void attach(MinecraftServer minecraftServer)
    {
        server = minecraftServer;
    }

    public static void detach()
    {
        server = null;
    }

    public static boolean isBucket(ItemStack stack)
    {
        return stack.getItem() == Items.WATER_BUCKET || stack.getItem() == Items.LAVA_BUCKET;
    }

    public static boolean isInfinity(Enchantment enchantment)
    {
        HolderGetter<Enchantment> enchantments = enchantments();
        return enchantments != null && enchantments.getOrThrow(Enchantments.INFINITY).value() == enchantment;
    }

    public static boolean hasInfinity(ItemStack stack)
    {
        HolderGetter<Enchantment> enchantments = enchantments();
        if (enchantments == null)
        {
            return false;
        }
        Holder<Enchantment> infinity = enchantments.getOrThrow(Enchantments.INFINITY);
        return EnchantmentHelper.getItemEnchantmentLevel(infinity, stack) > 0;
    }

    /**
     * True when an already enchanted water or lava bucket should be kept after use.
     */
    public static boolean keepsAfterUse(ItemStack stack)
    {
        return CarpetRtsAdditionSettings.infiniteBuckets && isBucket(stack) && hasInfinity(stack);
    }

    private static HolderGetter<Enchantment> enchantments()
    {
        MinecraftServer current = server;
        return current == null ? null : current.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    }
}
