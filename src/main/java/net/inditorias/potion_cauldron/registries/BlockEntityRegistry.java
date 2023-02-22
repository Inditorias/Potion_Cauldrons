package net.inditorias.potion_cauldron.registries;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.inditorias.potion_cauldron.PotionCauldron;
import net.inditorias.potion_cauldron.block_entity.PotionCauldronBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class BlockEntityRegistry {
    public static final BlockEntityType<PotionCauldronBlockEntity> POTION_CAULDRON_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            new Identifier(PotionCauldron.MOD_ID, "potion_cauldron_block_entity"),
            FabricBlockEntityTypeBuilder.create(PotionCauldronBlockEntity::new, BlockRegistry.POTION_CAULDRON).build());
    public static void registerModBlockEntities(){

    }
}
