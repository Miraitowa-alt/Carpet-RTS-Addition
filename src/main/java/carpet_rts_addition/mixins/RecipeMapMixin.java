/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition.mixins;

import carpet_rts_addition.CarpetRtsAdditionMod;
import carpet_rts_addition.settings.CarpetRtsAdditionSettings;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

/**
 * Hides this mod's recipes while the {@code rtsRecipes} rule is off.
 *
 * <p>Recipes are data pack registry entries in 26.x, so they cannot be added or removed at
 * runtime. Filtering the lookups instead makes the rule take effect immediately: while it is
 * off, matching and id-based lookup both behave as if the recipes did not exist.
 */
@Mixin(RecipeMap.class)
public abstract class RecipeMapMixin
{
    @Inject(method = "getRecipesFor", at = @At("RETURN"), cancellable = true)
    private void rts$hideDisabledRecipes(RecipeType<?> type, RecipeInput input, Level level,
                                         CallbackInfoReturnable<Stream<RecipeHolder<?>>> cir)
    {
        if (CarpetRtsAdditionSettings.rtsRecipes)
        {
            return;
        }
        cir.setReturnValue(cir.getReturnValue().filter(holder -> !rts$isOurRecipe(holder.id())));
    }

    @Inject(method = "byKey", at = @At("RETURN"), cancellable = true)
    private void rts$hideDisabledRecipe(ResourceKey<Recipe<?>> key, CallbackInfoReturnable<RecipeHolder<?>> cir)
    {
        if (!CarpetRtsAdditionSettings.rtsRecipes && rts$isOurRecipe(key))
        {
            cir.setReturnValue(null);
        }
    }

    @Unique
    private static boolean rts$isOurRecipe(ResourceKey<Recipe<?>> key)
    {
        return key != null && CarpetRtsAdditionMod.MOD_NAMESPACE.equals(key.identifier().getNamespace());
    }
}
