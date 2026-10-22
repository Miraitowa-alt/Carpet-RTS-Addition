package carpet_rts_addition;

import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.literal;

/**
 * Registers /testcommand and prints the current setting values.
 */
public class RtsCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(literal("testcommand")
                .then(literal("first").executes(c ->
                {
                    Messenger.m(c.getSource(), "gi Shhhh.....");
                    return 1;
                }))
                .then(literal("second").executes(c -> listSettings(c.getSource()))));
    }

    private static int listSettings(CommandSourceStack source)
    {
        Messenger.m(source, "w Here is all the settings we manage:");
        Messenger.m(source, "w Own stuff:");
        Messenger.m(source, "w  - boolean: " + RtsSettings.boolSetting);
        Messenger.m(source, "w  - string: " + RtsSettings.stringSetting);
        Messenger.m(source, "w  - int: " + RtsSettings.intSetting);
        Messenger.m(source, "w  - enum: " + RtsSettings.optionSetting);
        Messenger.m(source, "w Carpet Managed:");
        Messenger.m(source, "w  - makarena: " + CarpetRules.makarena);
        Messenger.m(source, "w  - numerical setting: " + CarpetRules.numericalSetting);
        return 1;
    }
}
