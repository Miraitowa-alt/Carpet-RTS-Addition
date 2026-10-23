/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.settings;

import carpet.api.settings.SettingsManager;

/**
 * Single place where rule classes are handed to Carpet. Kept as a wrapper so the call site
 * stays stable if registration needs to grow (observers, extra passes).
 */
public final class CarpetRuleRegistrar
{
    private CarpetRuleRegistrar()
    {
    }

    public static void register(SettingsManager settingsManager, Class<?> settingsClass)
    {
        settingsManager.parseSettingsClass(settingsClass);
    }
}
