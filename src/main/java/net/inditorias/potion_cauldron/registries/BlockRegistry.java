package net.inditorias.potion_cauldron.registries;


import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.inditorias.potion_cauldron.PotionCauldron;
import net.inditorias.potion_cauldron.block.PotionCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class BlockRegistry {

    public static final Block POTION_CAULDRON = registerBlockWithoutGroup("potion_cauldron", new PotionCauldronBlock(FabricBlockSettings.copy(Blocks.WATER_CAULDRON)));


    private static Block registerBlock(String name, Block block, ItemGroup group) {
        registerBlockItem(name, block, group);
        return Registry.register(Registries.BLOCK, new Identifier(PotionCauldron.MOD_ID, name), block);
    }

    private static Block registerBlockWithoutGroup(String name, Block block) {
        return Registry.register(Registries.BLOCK, new Identifier(PotionCauldron.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block, ItemGroup group) {
        Item item = Registry.register(Registries.ITEM, new Identifier(PotionCauldron.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
        ItemGroupEvents.modifyEntriesEvent(group).register(entries -> entries.add(item));
        return item;
    }

    public static void registerModBlocks() {
    }
}
