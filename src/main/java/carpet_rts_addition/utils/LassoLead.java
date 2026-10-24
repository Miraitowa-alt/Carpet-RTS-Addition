/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.utils;

import carpet_rts_addition.settings.CarpetRtsAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Turns a lead renamed to the configured lasso name into a mob container.
 *
 * <p>Capturing stores the mob's full NBT in the item's {@code entity_data} component and removes the
 * mob; using the same item on a block spawns it back. {@link TypedEntityData#loadInto(Entity)} keeps
 * the freshly created entity's own UUID, so repeated release never produces duplicate UUIDs.
 */
public final class LassoLead
{
    private LassoLead()
    {
    }

    /**
     * @return the result to report to the caller, or {@code null} to let the entity handle the click
     */
    public static InteractionResult capture(Player player, Mob mob, InteractionHand hand)
    {
        if (!CarpetRtsAdditionSettings.betterLead)
        {
            return null;
        }

        ItemStack lasso = player.getItemInHand(hand);
        if (!isLasso(lasso))
        {
            return null;
        }
        if (lasso.get(DataComponents.ENTITY_DATA) != null)
        {
            // A filled lasso must be emptied on a block first. Report success so vanilla leashing
            // cannot consume the item and destroy the stored mob.
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !mob.isAlive())
        {
            return null;
        }
        if (mob.getType() == EntityTypes.ENDER_DRAGON || mob.getType() == EntityTypes.WITHER)
        {
            return null;
        }

        ItemStack captured = lasso.copyWithCount(1);
        captured.set(DataComponents.ENTITY_DATA, TypedEntityData.of(mob.getType(), save(mob)));

        lasso.consume(1, player);
        if (!serverPlayer.getInventory().add(captured))
        {
            serverPlayer.drop(captured, false, Prediction.SERVER_ONLY);
        }
        mob.discard();
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * @return the result to report to the caller, or {@code null} when the item holds no mob
     */
    public static InteractionResult release(UseOnContext context)
    {
        if (!CarpetRtsAdditionSettings.betterLead)
        {
            return null;
        }

        ItemStack stack = context.getItemInHand();
        TypedEntityData<EntityType<?>> stored = stack.get(DataComponents.ENTITY_DATA);
        if (stored == null)
        {
            return null;
        }
        if (!(context.getLevel() instanceof ServerLevel level))
        {
            // Report success on the client so the held item is not used for anything else
            return InteractionResult.SUCCESS;
        }

        Entity entity = stored.type().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (entity == null)
        {
            return InteractionResult.FAIL;
        }
        stored.loadInto(entity);

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        float yaw = player == null ? 0.0F : player.getYRot() + 180.0F;
        entity.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);

        if (!level.addFreshEntity(entity))
        {
            return InteractionResult.FAIL;
        }
        // The lead goes back to being an empty lasso, so one lasso can be reused
        stack.remove(DataComponents.ENTITY_DATA);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean isLasso(ItemStack stack)    {
        if (stack.getItem() != Items.LEAD)
        {
            return false;
        }
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        String expected = CarpetRtsAdditionSettings.betterLeadName;
        return name != null && expected != null && expected.trim().equalsIgnoreCase(name.getString().trim());
    }

    private static CompoundTag save(Entity entity)
    {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }
}
