package net.inditorias.potion_cauldron.registries;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.inditorias.potion_cauldron.block_entity.PotionCauldronBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;

public class ColorRegistry {
    public static void registerColors(){
        ColorProviderRegistry.BLOCK.register(ColorRegistry::getPotionCauldronColor, BlockRegistry.POTION_CAULDRON);
    }

    private static int getPotionCauldronColor(BlockState state, BlockRenderView view, BlockPos pos, int tintIndex){
        if(view != null && view.getBlockEntity(pos) instanceof PotionCauldronBlockEntity entity){
            return entity.getColor();
        }
        return 0;
    }
}
