package carpet_rts_addition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

/**
 * Carpet extension entrypoint for Carpet-RTS-Addition.
 */
@SuppressWarnings("removal")
public class CarpetRtsAddition implements CarpetExtension, ModInitializer
{
    private static SettingsManager settingsManager;

    @Override
    public void onInitialize()
    {
        // Carpet still requires the legacy manager type to parse legacy @Rule annotations.
        settingsManager = new carpet.settings.SettingsManager("26.3", "carpet-rts-addition", "Carpet-RTS-Addition");
        CarpetServer.manageExtension(this);
    }

    @Override
    public void onGameStarted()
    {
        // Rules that should appear in /carpet.
        CarpetServer.settingsManager.parseSettingsClass(CarpetRules.class);
        // Rules managed by this extension's own SettingsManager.
        settingsManager.parseSettingsClass(RtsSettings.class);

        CarpetServer.settingsManager.registerRuleObserver((source, changedRule, userInput) ->
        {
            if (changedRule.categories().contains("carpet-rts-addition"))
            {
                Messenger.m(source, "gi Psssst... make sure not to touch original carpet rules");
            }
            else if (source != null && source.getPlayer() != null)
            {
                Messenger.print_server_message(
                        source.getServer(),
                        "Ehlo everybody, " + source.getPlayer().getName().getString() + " is cheating..."
                );
            }
        });
    }

    @Override
    public void onServerLoaded(MinecraftServer server)
    {
        // Carpet reloads /carpet settings itself; the custom manager is handled through
        // extensionSettingsManager().
    }

    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext)
    {
        RtsCommand.register(dispatcher);
    }

    @Override
    public SettingsManager extensionSettingsManager()
    {
        return settingsManager;
    }
}
