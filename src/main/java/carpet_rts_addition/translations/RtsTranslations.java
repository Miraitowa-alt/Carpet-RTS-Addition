/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.translations;

import carpet.utils.Translations;

import java.util.Map;

/**
 * Supplies the extension's language entries to Carpet. Carpet calls this on every language
 * refresh, right before rules are parsed, so keys are available when descriptions are resolved.
 *
 * <p>Keys follow Carpet's format: {@code <identifier>.rule.<field>.desc}, where the identifier is
 * that of the settings manager the rule was registered with.
 */
public final class RtsTranslations
{
    private static final String LANG_PATH = "assets/carpet_rts_addition/lang/%s.json";
    private static final String FALLBACK_LANGUAGE = "en_us";

    private RtsTranslations()
    {
    }

    public static Map<String, String> getTranslations(String lang)
    {
        Map<String, String> translations = load(lang);
        if (translations.isEmpty() && !FALLBACK_LANGUAGE.equals(lang))
        {
            // Carpet shows the raw translation key when a language is missing, so fall back to English
            translations = load(FALLBACK_LANGUAGE);
        }
        return translations;
    }

    private static Map<String, String> load(String lang)
    {
        return Translations.getTranslationFromResourcePath(String.format(LANG_PATH, lang));
    }
}
