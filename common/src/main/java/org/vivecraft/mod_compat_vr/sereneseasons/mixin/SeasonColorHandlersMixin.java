package org.vivecraft.mod_compat_vr.sereneseasons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "sereneseasons.handler.season.SeasonColorHandlers")
public class SeasonColorHandlersMixin {

    @Shadow
    private static ColorResolver originalGrassColorResolver;
    @Shadow
    private static ColorResolver originalFoliageColorResolver;

    @Inject(at = @At("HEAD"), method = "lambda$registerGrassAndFoliageColorHandlers$1", cancellable = true)
    private static void grassColor(Biome biome, double x, double y, CallbackInfoReturnable<Integer> cir) {
        if (Minecraft.getInstance().level == null) {
            cir.setReturnValue(originalGrassColorResolver.getColor(biome, x, y));
        }
    }

    @Inject(at = @At("HEAD"), method = "lambda$registerGrassAndFoliageColorHandlers$3", cancellable = true)
    private static void foliageColor(Biome biome, double x, double y, CallbackInfoReturnable<Integer> cir) {
        if (Minecraft.getInstance().level == null) {
            cir.setReturnValue(originalFoliageColorResolver.getColor(biome, x, y));
        }
    }

    @Inject(at = @At("HEAD"), method = "lambda$registerBirchColorHandler$4", cancellable = true)
    private static void birchColor(BlockState blockState, BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, int tintIndex, CallbackInfoReturnable<Integer> cir) {
        if (Minecraft.getInstance().level == null) {
            cir.setReturnValue(FoliageColor.getBirchColor());
        }
    }
}