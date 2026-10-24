/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.settings;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Validator;
import net.minecraft.commands.CommandSourceStack;

/**
 * Validators for the {@code betterLead} rules.
 *
 * <p>Carpet instantiates these with a no-argument constructor and rejects the change when
 * {@code validate} returns {@code null}.
 */
public final class LeadValidators
{
    private LeadValidators()
    {
    }

    /** The lasso name is matched against the item's custom name, so it must not be blank. */
    public static class LassoName extends Validator<String>
    {
        private static final int MAX_LENGTH = 32;

        @Override
        public String validate(CommandSourceStack source, CarpetRule<String> rule, String newValue,
                               String typedString)
        {
            if (newValue == null || newValue.isBlank() || newValue.length() > MAX_LENGTH)
            {
                return null;
            }
            return newValue;
        }

        @Override
        public String description()
        {
            return "Must be a non-blank name of at most " + MAX_LENGTH + " characters";
        }
    }
}
