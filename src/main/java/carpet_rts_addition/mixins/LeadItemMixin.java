/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.mixins;

import carpet_rts_addition.utils.LassoLead;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Releases a captured mob when the lasso is used on a block.
 *
 * <p>Only items carrying {@code entity_data} are handled, so a normal lead keeps vanilla's
 * "tie nearby mobs" behaviour.
 */
@Mixin(LeadItem.class)
public abstract class LeadItemMixin
{
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void rts$releaseFromLasso(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir)
    {
        InteractionResult result = LassoLead.release(context);
        if (result != null)
        {
            cir.setReturnValue(result);
        }
    }
}
