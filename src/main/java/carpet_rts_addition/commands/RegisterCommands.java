/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.commands;

import carpet.patches.EntityPlayerMPFake;
import carpet_rts_addition.utils.PickupFilter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the extension's commands.
 *
 * <p>{@code /fakePlayerPickup} configures the fake player pickup filter, while the feature itself
 * is switched by {@code /carpet fakePlayerPickup}. Configuration is accepted while the feature is
 * off, so a setup can be prepared before enabling it.
 */
public final class RegisterCommands
{
    private static final String PREFIX = "carpet_rts_addition.command.";

    private static final SuggestionProvider<CommandSourceStack> ITEM_IDS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> FAKE_PLAYERS = (context, builder) -> {
        for (String name : onlineFakePlayerNames(context.getSource()))
        {
            builder.suggest(name);
        }
        return builder.suggest("*").buildFuture();
    };

    private RegisterCommands()
    {
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("fakePlayerPickup")
                .executes(context -> status(context.getSource()))
                .then(Commands.literal("mode")
                        .executes(context -> showMode(context.getSource()))
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(PickupFilter.MODES, builder))
                                .executes(context -> setMode(context.getSource(),
                                        StringArgumentType.getString(context, "mode")))))
                .then(Commands.literal("items")
                        .executes(context -> listItems(context.getSource()))
                        .then(Commands.literal("list")
                                .executes(context -> listItems(context.getSource())))
                        .then(Commands.literal("clear")
                                .executes(context -> clearItems(context.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.argument("item", IdentifierArgument.id())
                                        .suggests(ITEM_IDS)
                                        .executes(context -> addItem(context.getSource(),
                                                IdentifierArgument.getId(context, "item")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("item", IdentifierArgument.id())
                                        .suggests(ITEM_IDS)
                                        .executes(context -> removeItem(context.getSource(),
                                                IdentifierArgument.getId(context, "item"))))))
                .then(Commands.literal("player")
                        .executes(context -> showTargets(context.getSource()))
                        .then(Commands.literal("list")
                                .executes(context -> showTargets(context.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                        .suggests(FAKE_PLAYERS)
                                        .executes(context -> addTargets(context.getSource(),
                                                StringArgumentType.getString(context, "players")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("players", StringArgumentType.greedyString())
                                        .suggests(FAKE_PLAYERS)
                                        .executes(context -> removeTargets(context.getSource(),
                                                StringArgumentType.getString(context, "players")))))));
    }

    private static int status(CommandSourceStack source)
    {
        source.sendSuccess(() -> Component.translatableWithFallback(PREFIX + "status",
                "fakePlayerPickup: %s | mode: %s | items: %s | fake players: %s",
                enabledLabel(), PickupFilter.mode(), describeItems(), describeTargets()), false);
        return 1;
    }

    private static int showMode(CommandSourceStack source)
    {
        source.sendSuccess(() -> Component.translatableWithFallback(PREFIX + "mode",
                "Pickup mode: %s", PickupFilter.mode()), false);
        return 1;
    }

    private static int setMode(CommandSourceStack source, String mode)
    {
        if (!PickupFilter.setMode(mode))
        {
            source.sendFailure(Component.translatableWithFallback(PREFIX + "unknown_mode",
                    "Unknown mode: %s (use off, blacklist or whitelist)", mode));
            return 0;
        }
        source.sendSuccess(() -> Component.translatableWithFallback(PREFIX + "mode_set",
                "Pickup mode set to %s", mode), true);
        return 1;
    }

    private static int listItems(CommandSourceStack source)
    {
        source.sendSuccess(() -> Component.translatableWithFallback(PREFIX + "items",
                "Pickup items: %s", describeItems()), false);
        return PickupFilter.itemIds().size();
    }

    private static int addItem(CommandSourceStack source, Identifier id)
    {
        Item item = PickupFilter.parseItem(id);
        if (item == null)
        {
            return unknownItem(source, id);
        }
        String shown = itemId(item);
        if (!PickupFilter.addItem(item))
        {
            source.sendFailure(Component.translatableWithFallback(PREFIX + "item_already",
                    "Already listed: %s", shown));
            return 0;
        }
        source.sendSuccess(() -> Component.translatableWithFallback(PREFIX + "item_added",
                "Added %s", shown), true);
        return 1;
    }

    private static int removeItem(CommandSourceStack source, Identifier id)
    {
        Item item = PickupFilter.parseItem(id);
        if (item == null)
        {
            return unknownItem(source, id);
        }
        String shown = itemId(item);
        if (!PickupFilter.removeItem(item))
        {
            source.sendFailure(Component.translatableWithFallback(PREFIX + "item_not_listed",
                    "Not listed: %s", shown));
            return 0;
        }
        source.sendSuccess(() -> Component.translatableWithFallback(PREFIX + "item_removed",
                "Removed %s", shown), true);
        return 1;
    }

    private static int clearItems(CommandSourceStack source)
    {
        PickupFilter.clearItems();
        source.sendSuccess(() -> Component.translatableWithFallback(PREFIX + "items_cleared",
                "Pickup item list cleared"), true);
        return 1;
    }

    private static int showTargets(CommandSourceStack source)
    {
        source.sendSuccess(() -> Component.translatableWithFallback(PREFIX + "targets",
                "Pickup filter applies to: %s", describeTargets()), false);
        return 1;
    }

    private static int addTargets(CommandSourceStack source, String names)
    {
        List<String> requested = "*".equals(names.trim())
                ? onlineFakePlayerNames(source)
                : splitNames(names);

        if (requested.isEmpty())
        {
            source.sendFailure(Component.translatableWithFallback(PREFIX + "no_online_players",
                    "No fake player online; use %s", "/fakePlayerPickup player set <name>"));
            return 0;
        }

        List<String> added = PickupFilter.addPlayers(requested);
        if (added.isEmpty())
        {
            source.sendFailure(Component.translatableWithFallback(PREFIX + "target_already",
                    "Already applies to %s", String.join(", ", requested)));
            return 0;
        }
        source.sendSuccess(() -> Component.translatableWithFallback(PREFIX + "target_added",
                "Added %s", String.join(", ", added)), true);
        return added.size();
    }

    private static int removeTargets(CommandSourceStack source, String names)
    {
        if (PickupFilter.playerNames().isEmpty())
        {
            source.sendFailure(Component.translatableWithFallback(PREFIX + "no_targets",
                    "No fake player configured; add one with %s", "/fakePlayerPickup player set <name>"));
            return 0;
        }

        List<String> requested = "*".equals(names.trim())
                ? PickupFilter.playerNames()
                : splitNames(names);

        List<String> removed = PickupFilter.removePlayers(requested);
        if (removed.isEmpty())
        {
            source.sendFailure(Component.translatableWithFallback(PREFIX + "target_not_listed",
                    "Not listed: %s", names.trim()));
            return 0;
        }
        source.sendSuccess(() -> Component.translatableWithFallback(PREFIX + "target_removed",
                "Removed %s", String.join(", ", removed)), true);
        return removed.size();
    }

    private static int unknownItem(CommandSourceStack source, Object id)
    {
        source.sendFailure(Component.translatableWithFallback(PREFIX + "unknown_item",
                "Unknown item: %s", id));
        return 0;
    }

    private static String itemId(Item item)
    {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static Component enabledLabel()
    {
        boolean enabled = PickupFilter.isEnabled();
        return Component.translatableWithFallback(PREFIX + (enabled ? "on" : "off"),
                enabled ? "on" : "off (use /carpet fakePlayerPickup true)");
    }

    private static Component describeItems()
    {
        return describeList(PickupFilter.itemIds());
    }

    private static Component describeTargets()
    {
        return describeList(PickupFilter.playerNames());
    }

    private static Component describeList(List<String> values)
    {
        return values.isEmpty()
                ? Component.translatableWithFallback(PREFIX + "none", "(none)")
                : Component.literal(String.join(", ", values));
    }

    /**
     * Splits a comma separated name list.
     */
    private static List<String> splitNames(String names)
    {
        List<String> parsed = new ArrayList<>();
        for (String token : names.split(","))
        {
            String name = token.trim();
            if (!name.isEmpty())
            {
                parsed.add(name);
            }
        }
        return parsed;
    }

    private static List<String> onlineFakePlayerNames(CommandSourceStack source)
    {
        List<String> names = new ArrayList<>();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers())
        {
            if (player instanceof EntityPlayerMPFake)
            {
                names.add(player.getScoreboardName());
            }
        }
        return names;
    }
}
