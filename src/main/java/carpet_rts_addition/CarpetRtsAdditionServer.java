/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet_rts_addition.commands.RegisterCommands;
import carpet_rts_addition.settings.CarpetRtsAdditionSettings;
import carpet_rts_addition.settings.CarpetRuleRegistrar;
import carpet_rts_addition.translations.RtsTranslations;
import carpet_rts_addition.utils.AfkProtection;
import carpet_rts_addition.utils.AutoEatAfk;
import carpet_rts_addition.utils.InfinityEnchantment;
import carpet_rts_addition.utils.PickupFilter;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Carpet extension singleton. Holds no state of its own; rules live in the settings class
 * and commands in the commands package.
 */
public class CarpetRtsAdditionServer implements CarpetExtension
{
    private static final CarpetRtsAdditionServer INSTANCE = new CarpetRtsAdditionServer();

    private CarpetRtsAdditionServer()
    {
    }

    public static CarpetRtsAdditionServer getInstance()
    {
        return INSTANCE;
    }

    public static void init()
    {
        CarpetServer.manageExtension(INSTANCE);
    }

    @Override
    public void onGameStarted()
    {
        CarpetRuleRegistrar.register(CarpetServer.settingsManager, CarpetRtsAdditionSettings.class);
    }

    @Override
    public String version()
    {
        return CarpetRtsAdditionMod.getModId();
    }

    @Override
    public void onServerLoaded(MinecraftServer server)
    {
        InfinityEnchantment.attach(server);
    }

    @Override
    public void onTick(MinecraftServer server)
    {
        AfkProtection.tick(server);
        AutoEatAfk.tick(server);
    }

    @Override
    public void onPlayerLoggedOut(ServerPlayer player)
    {
        AfkProtection.forget(player);
        AutoEatAfk.forget(player);
    }

    @Override
    public void onServerClosed(MinecraftServer server)
    {
        InfinityEnchantment.detach();
        AfkProtection.reset();
        AutoEatAfk.reset();
        PickupFilter.reset();
    }

    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext)
    {
        RegisterCommands.registerCommands(dispatcher);
    }

    @Override
    public Map<String, String> canHasTranslations(String lang)
    {
        return RtsTranslations.getTranslations(lang);
    }
}
