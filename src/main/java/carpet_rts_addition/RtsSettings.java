package carpet_rts_addition;

import carpet.settings.Rule;

/**
 * Settings managed by this extension's own SettingsManager.
 */
@SuppressWarnings("removal")
public class RtsSettings
{
    public enum Option
    {
        OPTION_A, OPTION_B, OPTION_C
    }

    @Rule(desc = "Independent integer setting", category = "misc")
    public static int intSetting = 10;

    @Rule(
            desc = "Independent string setting",
            options = {"foo", "bar", "baz"},
            extra = {
                    "Accepts a free-form value",
                    "with tab-completion suggestions"
            },
            category = "misc",
            strict = false
    )
    public static String stringSetting = "foo";

    @Rule(
            desc = "Independent enum setting",
            extra = {"Parsed and validated automatically"},
            category = "misc")
    public static Option optionSetting = Option.OPTION_A;

    @Rule(desc = "Independent boolean setting", category = "misc")
    public static boolean boolSetting;
}
