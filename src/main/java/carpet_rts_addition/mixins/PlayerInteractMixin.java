/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.mixins;

import carpet_rts_addition.utils.LassoLead;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures mobs with a lasso on right-click.
 *
 * <p>{@code Player#interactOn} is the single entry point for every entity interaction (the server
 * packet handler calls it before the entity's own {@code interact}), so injecting here happens
 * before villagers open their trade screen or animals react to the held item.
 */
@Mixin(Player.class)
public abstract class PlayerInteractMixin
{
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void rts$captureWithLasso(Entity target, InteractionHand hand, Vec3 location,
                                      CallbackInfoReturnable<InteractionResult> cir)
    {
        if (!(target instanceof Mob mob))
        {
            return;
        }
        InteractionResult result = LassoLead.capture((Player) (Object) this, mob, hand);
        if (result != null)
        {
            cir.setReturnValue(result);
        }
    }
}
