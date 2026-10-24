/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.utils;

import carpet_rts_addition.settings.CarpetRtsAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Lets bone meal grow plants that vanilla refuses to bonemeal: sugar cane, cactus, nether wart and
 * vines. Vanilla makes this decision in {@code BoneMealItem#growCrop}, which only accepts
 * {@code BonemealableBlock}, so the blocks below are exactly the ones vanilla skips.
 */
public final class BoneMealCatalyst
{
    /** Vanilla bone meal particle event; data 15 is the amount vanilla itself uses. */
    private static final int BONE_MEAL_PARTICLES = 1505;
    private static final int BONE_MEAL_PARTICLE_DATA = 15;
    private static final int TALL_PLANT_MAX_HEIGHT = 3;

    private BoneMealCatalyst()
    {
    }

    /**
     * @return the result to report to the caller, or {@code null} to let vanilla handle the use
     */
    public static InteractionResult tryCatalyse(UseOnContext context)
    {
        if (!CarpetRtsAdditionSettings.boneMealCatalyst)
        {
            return null;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!supports(state))
        {
            return null;
        }

        if (!(level instanceof ServerLevel serverLevel))
        {
            // Vanilla bone meal also reports success on the client before the server answers
            return InteractionResult.SUCCESS;
        }

        if (!grow(serverLevel, pos, state))
        {
            // Nothing left to grow: behave like vanilla bone meal on an invalid target
            return null;
        }

        ItemStack stack = context.getItemInHand();
        stack.shrink(1);
        stack.causeUseVibration(context.getPlayer(), GameEvent.ITEM_INTERACT_FINISH);
        level.levelEvent(BONE_MEAL_PARTICLES, pos, BONE_MEAL_PARTICLE_DATA);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean supports(BlockState state)
    {
        Block block = state.getBlock();
        return block instanceof SugarCaneBlock
                || block instanceof CactusBlock
                || block instanceof NetherWartBlock
                || block instanceof VineBlock;
    }

    private static boolean grow(ServerLevel level, BlockPos pos, BlockState state)
    {
        Block block = state.getBlock();
        if (block instanceof SugarCaneBlock)
        {
            return growTall(level, pos, Blocks.SUGAR_CANE);
        }
        if (block instanceof CactusBlock)
        {
            return growTall(level, pos, Blocks.CACTUS);
        }
        if (block instanceof NetherWartBlock)
        {
            return growWart(level, pos, state);
        }
        if (block instanceof VineBlock)
        {
            return growVine(level, pos, state);
        }
        return false;
    }

    /**
     * Adds one block on top of a sugar cane or cactus column, keeping the vanilla height limit.
     */
    private static boolean growTall(ServerLevel level, BlockPos pos, Block plant)
    {
        BlockPos bottom = pos;
        while (is(level, bottom.below(), plant))
        {
            bottom = bottom.below();
        }

        int height = 1;
        while (is(level, bottom.above(height), plant))
        {
            height++;
        }
        if (height >= TALL_PLANT_MAX_HEIGHT)
        {
            return false;
        }

        BlockPos target = bottom.above(height);
        if (!level.getBlockState(target).isAir())
        {
            return false;
        }

        BlockState newState = plant.defaultBlockState();
        // Cactus refuses to grow next to a solid neighbour, so let the block decide
        if (!newState.canSurvive(level, target))
        {
            return false;
        }
        return level.setBlockAndUpdate(target, newState);
    }

    private static boolean growWart(ServerLevel level, BlockPos pos, BlockState state)
    {
        int age = state.getValue(NetherWartBlock.AGE);
        if (age >= NetherWartBlock.MAX_AGE)
        {
            return false;
        }
        return level.setBlockAndUpdate(pos, state.setValue(NetherWartBlock.AGE, age + 1));
    }

    /**
     * Extends a vine column one block downwards, copying the wall the column currently hangs on.
     */
    private static boolean growVine(ServerLevel level, BlockPos pos, BlockState state)
    {
        BlockPos bottom = pos;
        while (is(level, bottom.below(), Blocks.VINE))
        {
            bottom = bottom.below();
        }

        BlockPos target = bottom.below();
        if (!level.getBlockState(target).isAir())
        {
            return false;
        }

        BlockState newState = Blocks.VINE.defaultBlockState();
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            BooleanProperty face = VineBlock.getPropertyForFace(direction);
            if (state.getValue(face))
            {
                newState = newState.setValue(face, true);
            }
        }
        if (!newState.canSurvive(level, target))
        {
            return false;
        }
        return level.setBlockAndUpdate(target, newState);
    }

    private static boolean is(Level level, BlockPos pos, Block plant)
    {
        return level.getBlockState(pos).getBlock() == plant;
    }
}
