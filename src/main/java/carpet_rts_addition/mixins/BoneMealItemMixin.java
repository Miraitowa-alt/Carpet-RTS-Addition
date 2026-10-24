/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.mixins;

import carpet_rts_addition.utils.BoneMealCatalyst;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the {@code boneMealCatalyst} rule to bone meal use.
 *
 * <p>Vanilla decides in {@code growCrop} that only {@code BonemealableBlock} may grow, and otherwise
 * falls through to the water plant and grass paths, so the head of {@code useOn} is the only place
 * that can grow sugar cane, cactus, nether wart and vines without re-implementing vanilla's checks.
 */
@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin
{
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void rts$catalysePlants(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir)
    {
        InteractionResult result = BoneMealCatalyst.tryCatalyse(context);
        if (result != null)
        {
            cir.setReturnValue(result);
        }
    }
}
