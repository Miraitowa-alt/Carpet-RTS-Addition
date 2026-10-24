/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.mixins;

import carpet_rts_addition.utils.AfkProtection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels accidental damage for the {@code afkPlayerProtection} rule.
 *
 * <p>{@code ServerPlayer#hurtServer} is the single entry point for suffocation, lightning, fall
 * damage and mob attacks, and only players reach it, so one injection covers all four cases.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDamageMixin
{
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void rts$protectAfkPlayer(ServerLevel level, DamageSource source, float amount,
                                      CallbackInfoReturnable<Boolean> cir)
    {
        if (AfkProtection.shouldBlock((ServerPlayer) (Object) this, source))
        {
            cir.setReturnValue(false);
        }
    }
}
