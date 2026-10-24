/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.utils;

import carpet_rts_addition.settings.CarpetRtsAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/**
 * Harvests a mature crop on right-click and replants it in one step.
 *
 * <p>Only {@link CropBlock} is handled, which covers wheat, carrots, potatoes, beetroot and any
 * modded crop that extends it. The replanting seed is recognised as the block item of the crop
 * itself, so no per-crop table is needed.
 */
public final class RightClickHarvest
{
    private RightClickHarvest()
    {
    }

    /**
     * @return the result to report to the caller, or {@code null} to leave vanilla behaviour alone
     */
    public static InteractionResult harvest(BlockState state, Level level, BlockPos pos, Player player, ItemStack tool)
    {
        if (!CarpetRtsAdditionSettings.rightClickHarvest || player.isSpectator() || player.isShiftKeyDown())
        {
            return null;
        }
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state))
        {
            return null;
        }

        if (level instanceof ServerLevel serverLevel)
        {
            harvestAndReplant(serverLevel, pos, state, crop, player, tool);
        }
        // Success on both sides so the client does not fall through to placing the held item;
        // only the server side actually changes the world.
        return InteractionResult.SUCCESS;
    }

    private static void harvestAndReplant(ServerLevel level, BlockPos pos, BlockState state, CropBlock crop,
                                          Player player, ItemStack tool)
    {
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, tool);
        boolean replanted = player.hasInfiniteMaterials() || consumeSeed(drops, player, state.getBlock());

        if (replanted)
        {
            level.setBlockAndUpdate(pos, crop.getStateForAge(0));
        }
        else
        {
            level.removeBlock(pos, false);
        }

        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F,
                0.8F + level.getRandom().nextFloat() * 0.4F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);

        for (ItemStack drop : drops)
        {
            if (!drop.isEmpty())
            {
                Block.popResource(level, pos, drop);
            }
        }
    }

    /**
     * Takes one seed for the replant, preferring the harvest itself over the player's inventory so a
     * normal harvest never eats seeds the player did not just get back.
     */
    private static boolean consumeSeed(List<ItemStack> drops, Player player, Block crop)
    {
        for (ItemStack drop : drops)
        {
            if (isSeedOf(drop, crop))
            {
                drop.shrink(1);
                return true;
            }
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            if (isSeedOf(inventory.getItem(slot), crop))
            {
                inventory.removeItem(slot, 1);
                return true;
            }
        }
        return false;
    }

    private static boolean isSeedOf(ItemStack stack, Block crop)
    {
        return !stack.isEmpty() && isSeedOf(stack.getItem(), crop);
    }

    private static boolean isSeedOf(Item item, Block crop)
    {
        return item instanceof BlockItem blockItem && blockItem.getBlock() == crop;
    }
}
