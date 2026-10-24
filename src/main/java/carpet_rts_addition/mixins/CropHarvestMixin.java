/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.mixins;

import carpet_rts_addition.utils.RightClickHarvest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the {@code rightClickHarvest} rule to every block interaction.
 *
 * <p>{@code CropBlock} does not override either entry point, so injecting into the base class covers
 * crops while leaving every other block to vanilla. Holding an item goes through
 * {@code useItemOn}, an empty hand through {@code useWithoutItem}.
 */
@Mixin(BlockBehaviour.class)
public abstract class CropHarvestMixin
{
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void rts$harvestWithItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                     InteractionHand hand, BlockHitResult hit,
                                     CallbackInfoReturnable<InteractionResult> cir)
    {
        InteractionResult result = RightClickHarvest.harvest(state, level, pos, player, stack);
        if (result != null)
        {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void rts$harvestWithEmptyHand(BlockState state, Level level, BlockPos pos, Player player,
                                          BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir)
    {
        InteractionResult result = RightClickHarvest.harvest(state, level, pos, player, ItemStack.EMPTY);
        if (result != null)
        {
            cir.setReturnValue(result);
        }
    }
}
