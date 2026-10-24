/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.mixins;

import carpet_rts_addition.utils.LassoLead;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures mobs with a lasso when the mob's own interaction is what runs first.
 *
 * <p>{@code Player#interactOn} covers normal clients, but automation (Carpet's {@code /player use})
 * calls {@code Entity#interact} directly and stops as soon as it consumes the action. Hooking
 * {@code Mob#interact} covers both paths; capturing twice is impossible because the mob is already
 * gone when the second hook runs.
 */
@Mixin(Mob.class)
public abstract class MobInteractMixin
{
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void rts$captureWithLasso(Player player, InteractionHand hand, Vec3 location,
                                      CallbackInfoReturnable<InteractionResult> cir)
    {
        InteractionResult result = LassoLead.capture(player, (Mob) (Object) this, hand);
        if (result != null)
        {
            cir.setReturnValue(result);
        }
    }
}
