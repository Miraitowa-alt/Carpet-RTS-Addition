/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.settings;

import carpet.api.settings.Rule;

/**
 * Rules exposed through {@code /carpet}.
 *
 * <p>Add public static fields annotated with {@link carpet.api.settings.Rule} here; the rule name
 * is the field name. Descriptions are not part of the annotation in the current Carpet API and
 * must be provided through translation keys, see {@code translations.RtsTranslations}.
 */
public final class CarpetRtsAdditionSettings
{
    private CarpetRtsAdditionSettings()
    {
    }

    @Rule(categories = "carpet-rts-addition")
    public static boolean rtsRecipes = false;

    @Rule(categories = "carpet-rts-addition")
    public static boolean infiniteBuckets = false;

    @Rule(categories = "carpet-rts-addition")
    public static boolean rightClickHarvest = false;

    @Rule(categories = "carpet-rts-addition")
    public static boolean boneMealCatalyst = false;

    @Rule(categories = "carpet-rts-addition")
    public static boolean afkPlayerProtection = false;

    @Rule(categories = "carpet-rts-addition")
    public static boolean autoEatAfk = false;

    @Rule(categories = "carpet-rts-addition", options = {"fakePlayers", "trueplayer", "all"})
    public static String autoEatAfkTarget = "all";

    @Rule(categories = "carpet-rts-addition", validators = {AutoEatValidators.FoodLevel.class})
    public static int autoEatAfkThreshold = 6;

    @Rule(categories = "carpet-rts-addition", validators = {AutoEatValidators.FoodList.class})
    public static String autoEatAfkFoods = "*";
}
