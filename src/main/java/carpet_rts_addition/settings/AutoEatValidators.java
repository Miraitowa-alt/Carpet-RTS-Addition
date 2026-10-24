/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.settings;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Validator;
import net.minecraft.commands.CommandSourceStack;

/**
 * Validators for the {@code autoEatAfk} rules.
 *
 * <p>Carpet instantiates these with a no-argument constructor and rejects the change when
 * {@code validate} returns {@code null}.
 */
public final class AutoEatValidators
{
    private AutoEatValidators()
    {
    }

    /**
     * Food level is a 0-20 scale in vanilla.
     */
    public static class FoodLevel extends Validator<Integer>
    {
        private static final int MIN = 0;
        private static final int MAX = 20;

        @Override
        public Integer validate(CommandSourceStack source, CarpetRule<Integer> rule, Integer newValue,
                                String typedString)
        {
            return newValue != null && newValue >= MIN && newValue <= MAX ? newValue : null;
        }

        @Override
        public String description()
        {
            return "Must be between " + MIN + " and " + MAX;
        }
    }
}
