/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.mixins;

import carpet_rts_addition.utils.InfinityEnchantment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops an Infinity bucket from turning into an empty bucket after a successful use.
 *
 * <p>{@code BucketItem#use} builds its result through this static helper, so returning the
 * original stack keeps the bucket (and its enchantment) in hand.
 */
@Mixin(BucketItem.class)
public abstract class BucketItemMixin
{
    @Inject(method = "getEmptySuccessItem", at = @At("HEAD"), cancellable = true)
    private static void rts$keepInfinityBucket(ItemStack stack, Player player, CallbackInfoReturnable<ItemStack> cir)
    {
        if (InfinityEnchantment.keepsAfterUse(stack))
        {
            // Must be a copy: the caller consumes the original stack before using this result
            cir.setReturnValue(stack.copy());
        }
    }
}
