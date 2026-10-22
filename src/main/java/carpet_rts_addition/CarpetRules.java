package carpet_rts_addition;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Validator;
import carpet.api.settings.Validators;
import carpet.settings.Rule;
import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;

import static carpet.api.settings.RuleCategory.CREATIVE;

/**
 * Rules registered into Carpet's /carpet command.
 */
@SuppressWarnings("removal")
public class CarpetRules
{
    /**
     * Validator for the numerical test rule. Returning null rejects the new value.
     */
    private static class CheckValue extends Validator<Integer>
    {
        @Override
        public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String typedString)
        {
            Messenger.m(source, "rb Congrats, you just changed a setting to " + newValue);
            return newValue < 20000000 ? newValue : null;
        }
    }

    @Rule(
            desc = "Numerical setting used to test Carpet rule parsing",
            options = {"32768", "250000", "1000000"},
            validate = {Validators.NonNegativeNumber.class, CheckValue.class},
            category = {CREATIVE, "carpet-rts-addition"}
    )
    public static int numericalSetting = 32768;

    @Rule(desc = "Makes nearby mobs dance Makarena", category = {"fun", "carpet-rts-addition"})
    public static boolean makarena = false;
}
