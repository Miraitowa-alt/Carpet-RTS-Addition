package carpet_rts_addition.mixins;

import carpet_rts_addition.CarpetRules;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMakarenaMixin extends LivingEntity
{
    protected MobMakarenaMixin(EntityType<? extends LivingEntity> entityType, Level level)
    {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void danceMakarena(CallbackInfo ci)
    {
        if (CarpetRules.makarena && onGround())
        {
            int stage = tickCount % 200;
            if (stage > 155 && level().getNearestPlayer(this, 32.0) != null)
            {
                yHeadRot += 2;
                yHeadRotO += 2;
                if (stage == 199)
                {
                    push(0.0, 0.4, 0.0);
                }
            }
        }
    }
}
