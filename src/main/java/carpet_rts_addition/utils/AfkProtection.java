/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.utils;

import carpet.patches.EntityPlayerMPFake;
import carpet_rts_addition.settings.CarpetRtsAdditionSettings;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps idle players and Carpet fake players from dying to accidents.
 *
 * <p>A player counts as idle when neither moving, looking around nor clicking happened for
 * {@link #AFK_MILLIS}. Fake players are always protected. Only damage kinds that punish standing
 * still are filtered, so mobs, players, lava and the void still kill normally.
 */
public final class AfkProtection
{
    private static final long AFK_MILLIS = 60_000L;

    private static final Map<UUID, Activity> ACTIVITY = new HashMap<>();

    private AfkProtection()
    {
    }

    public static void tick(MinecraftServer server)
    {
        long now = Util.getMillis();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            Activity activity = ACTIVITY.computeIfAbsent(player.getUUID(), uuid -> new Activity(player, now));
            if (activity.moved(player))
            {
                activity.snapshot(player);
                activity.lastActive = now;
            }
        }
    }

    public static void forget(ServerPlayer player)
    {
        ACTIVITY.remove(player.getUUID());
    }

    public static void reset()
    {
        ACTIVITY.clear();
    }

    public static boolean shouldBlock(ServerPlayer player, DamageSource source)
    {
        return CarpetRtsAdditionSettings.afkPlayerProtection && isAccidental(source) && isIdle(player);
    }

    public static boolean isIdle(ServerPlayer player)
    {
        if (player instanceof EntityPlayerMPFake)
        {
            return true;
        }

        Activity activity = ACTIVITY.get(player.getUUID());
        if (activity == null)
        {
            // Not sampled yet (just logged in), so there is no baseline to compare against
            return false;
        }
        long lastActive = Math.max(activity.lastActive, player.getLastActionTime());
        return Util.getMillis() - lastActive >= AFK_MILLIS;
    }

    private static boolean isAccidental(DamageSource source)
    {
        return source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.LIGHTNING_BOLT)
                || source.is(DamageTypes.FALL)
                || source.getEntity() instanceof Phantom;
    }

    /**
     * Last observed transform of a player, used to tell "standing still" from "playing".
     */
    private static final class Activity
    {
        private ResourceKey<Level> dimension;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private long lastActive;

        private Activity(ServerPlayer player, long now)
        {
            this.lastActive = now;
            this.snapshot(player);
        }

        private boolean moved(ServerPlayer player)
        {
            return player.getX() != x || player.getY() != y || player.getZ() != z
                    || player.getYRot() != yaw || player.getXRot() != pitch
                    || !player.level().dimension().equals(dimension);
        }

        private void snapshot(ServerPlayer player)
        {
            this.dimension = player.level().dimension();
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.yaw = player.getYRot();
            this.pitch = player.getXRot();
        }
    }
}
