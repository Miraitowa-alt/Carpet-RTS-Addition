/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.utils;

import carpet.patches.EntityPlayerMPFake;
import carpet_rts_addition.settings.CarpetRtsAdditionSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds the fake player pickup configuration and decides whether a pickup may happen.
 *
 * <p>The on/off switch is the {@code fakePlayerPickup} Carpet rule; everything else is managed by
 * the {@code /fakePlayerPickup} command. Whitelist and blacklist share one item set: blacklist
 * blocks listed items, whitelist allows only listed items. An empty player set means no fake player
 * is filtered, so the rule does nothing until names are added.
 */
public final class PickupFilter
{
    public static final String MODE_OFF = "off";
    public static final String MODE_BLACKLIST = "blacklist";
    public static final String MODE_WHITELIST = "whitelist";
    public static final List<String> MODES = List.of(MODE_OFF, MODE_BLACKLIST, MODE_WHITELIST);

    private static final Set<Item> ITEMS = new LinkedHashSet<>();
    private static final Set<String> PLAYERS = new LinkedHashSet<>();

    private static String mode = MODE_BLACKLIST;

    private PickupFilter()
    {
    }

    public static boolean isEnabled()
    {
        return CarpetRtsAdditionSettings.fakePlayerPickup;
    }

    public static String mode()
    {
        return mode;
    }

    public static boolean setMode(String newMode)
    {
        if (!MODES.contains(newMode))
        {
            return false;
        }
        mode = newMode;
        return true;
    }

    /**
     * @return the configured item ids in insertion order, for display
     */
    public static List<String> itemIds()
    {
        List<String> ids = new ArrayList<>(ITEMS.size());
        for (Item item : ITEMS)
        {
            ids.add(BuiltInRegistries.ITEM.getKey(item).toString());
        }
        return ids;
    }

    /**
     * @return the registered item, or {@code null} when the id is unknown
     */
    public static Item parseItem(Identifier identifier)
    {
        Item item = BuiltInRegistries.ITEM.getValue(identifier);
        return item == Items.AIR ? null : item;
    }

    public static boolean addItem(Item item)
    {
        return ITEMS.add(item);
    }

    public static boolean removeItem(Item item)
    {
        return ITEMS.remove(item);
    }

    public static void clearItems()
    {
        ITEMS.clear();
    }

    public static List<String> playerNames()
    {
        return new ArrayList<>(PLAYERS);
    }

    /**
     * @return the names that were not configured before
     */
    public static List<String> addPlayers(List<String> names)
    {
        List<String> added = new ArrayList<>();
        for (String name : names)
        {
            if (!containsPlayer(name))
            {
                PLAYERS.add(name);
                added.add(name);
            }
        }
        return added;
    }

    /**
     * @return the names that were actually removed
     */
    public static List<String> removePlayers(List<String> names)
    {
        List<String> removed = new ArrayList<>();
        for (String name : names)
        {
            if (containsPlayer(name))
            {
                PLAYERS.removeIf(existing -> existing.equalsIgnoreCase(name));
                removed.add(name);
            }
        }
        return removed;
    }

    private static boolean containsPlayer(String name)
    {
        for (String existing : PLAYERS)
        {
            if (existing.equalsIgnoreCase(name))
            {
                return true;
            }
        }
        return false;
    }

    public static void reset()
    {
        mode = MODE_BLACKLIST;
        ITEMS.clear();
        PLAYERS.clear();
    }

    /**
     * @return {@code true} when the pickup should be cancelled
     */
    public static boolean shouldBlock(Player player, ItemStack stack)
    {
        if (!isEnabled() || MODE_OFF.equals(mode) || stack.isEmpty())
        {
            return false;
        }
        if (!(player instanceof EntityPlayerMPFake) || !isTargeted(player))
        {
            return false;
        }

        boolean listed = ITEMS.contains(stack.getItem());
        return MODE_BLACKLIST.equals(mode) ? listed : !listed;
    }

    private static boolean isTargeted(Player player)
    {
        // An empty list means "no fake player is filtered"; the filter stays inert until names are added
        String name = player.getScoreboardName();
        for (String target : PLAYERS)
        {
            if (target.equalsIgnoreCase(name))
            {
                return true;
            }
        }
        return false;
    }
}
