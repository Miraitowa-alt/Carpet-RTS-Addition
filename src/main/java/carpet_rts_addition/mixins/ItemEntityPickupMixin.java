/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.mixins;

import carpet_rts_addition.utils.PickupFilter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks item pickup for fake players according to the {@code fakePlayerPickupMode} rules.
 *
 * <p>{@code ItemEntity#playerTouch} is the only server-side pickup path (it returns immediately on
 * the client and on a pickup delay), so cancelling at its head leaves the item on the ground
 * untouched instead of removing it.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupMixin
{
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void rts$filterFakePlayerPickup(Player player, CallbackInfo ci)
    {
        if (PickupFilter.shouldBlock(player, ((ItemEntity) (Object) this).getItem()))
        {
            ci.cancel();
        }
    }
}
