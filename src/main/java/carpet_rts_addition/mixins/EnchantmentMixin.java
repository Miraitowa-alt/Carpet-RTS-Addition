/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.mixins;

import carpet_rts_addition.settings.CarpetRtsAdditionSettings;
import carpet_rts_addition.utils.InfinityEnchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the Infinity enchantment be applied to water and lava buckets while the rule is on.
 *
 * <p>This is the same check the anvil and {@code /enchant} use, so both paths follow the rule.
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin
{
    @Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
    private void rts$allowInfinityOnBuckets(ItemStack stack, CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetRtsAdditionSettings.infiniteBuckets
                && InfinityEnchantment.isBucket(stack)
                && InfinityEnchantment.isInfinity((Enchantment) (Object) this))
        {
            cir.setReturnValue(true);
        }
    }
}
