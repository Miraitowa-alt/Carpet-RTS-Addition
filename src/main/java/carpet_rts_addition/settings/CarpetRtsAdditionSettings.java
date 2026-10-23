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
}
