/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.settings;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Validator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

/**
 * Validators for comma separated list rules.
 *
 * <p>Carpet instantiates these with a no-argument constructor and rejects the change when
 * {@code validate} returns {@code null}.
 */
public final class ListValidators
{
    private ListValidators()
    {
    }

    /** Accepts {@code *} or a comma separated list of registered item ids. */
    public static class ItemIds extends Validator<String>
    {
        @Override
        public String validate(CommandSourceStack source, CarpetRule<String> rule, String newValue,
                               String typedString)
        {
            if (newValue == null || newValue.isBlank())
            {
                return null;
            }

            boolean found = false;
            for (String token : newValue.split(","))
            {
                String trimmed = token.trim();
                if (trimmed.isEmpty())
                {
                    continue;
                }
                found = true;
                if (!"*".equals(trimmed) && !isKnownItem(trimmed))
                {
                    return null;
                }
            }
            return found ? newValue : null;
        }

        @Override
        public String description()
        {
            return "Use * for everything, or a comma separated list of item ids";
        }

        private static boolean isKnownItem(String id)
        {
            Identifier identifier = Identifier.tryParse(id);
            return identifier != null && BuiltInRegistries.ITEM.getValue(identifier) != Items.AIR;
        }
    }
}
