package net.inditorias.potion_cauldron.mixin;


import net.inditorias.potion_cauldron.PotionCauldron;
import net.inditorias.potion_cauldron.block.PotionCauldronBlock;
import net.inditorias.potion_cauldron.block_entity.PotionCauldronBlockEntity;
import net.inditorias.potion_cauldron.registries.BlockRegistry;
import net.minecraft.block.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PotionItem.class)
public class VanillaCauldronToPotionCauldron {

    @Inject(at = @At("TAIL"), method = "useOnBlock", cancellable = true)
    private void EmptyCauldronToPotionCauldron(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir){
        PlayerEntity playerEntity = context.getPlayer();
        if (playerEntity == null) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);
        ItemStack itemStack = context.getStack();
        List<StatusEffectInstance> effects = PotionUtil.getCustomPotionEffects(itemStack);
        if(effects.isEmpty()){
            effects = PotionUtil.getPotionEffects(itemStack);
        }
        if(effects.isEmpty() || world.isClient()){
            return;
        }
        if (blockState.isOf(Blocks.WATER_CAULDRON)){
            int levels = blockState.get(LeveledCauldronBlock.LEVEL);
            if(levels < 3){
                world.setBlockState(blockPos, BlockRegistry.POTION_CAULDRON.getDefaultState().with(PotionCauldronBlock.LEVEL, blockState.get(LeveledCauldronBlock.LEVEL)));
                addPotion(effects, world.getBlockState(blockPos), world, blockPos);
                dilutePotions(world.getBlockState(blockPos), world, blockPos);
                world.setBlockState(blockPos, world.getBlockState(blockPos).cycle(PotionCauldronBlock.LEVEL));
                world.playSound(null, blockPos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.emitGameEvent(null, GameEvent.FLUID_PLACE, blockPos);
            }
        }else if(blockState.isOf(Blocks.CAULDRON)){
            world.setBlockState(blockPos, BlockRegistry.POTION_CAULDRON.getDefaultState());
            addPotion(effects, world.getBlockState(blockPos), world, blockPos);
            world.playSound(null, blockPos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.emitGameEvent(null, GameEvent.FLUID_PLACE, blockPos);
        }
    }

    private void addPotion(List<StatusEffectInstance> effects, BlockState state, World world, BlockPos pos){
        if(world.getBlockEntity(pos) instanceof PotionCauldronBlockEntity potionCauldronBlockEntity){
            potionCauldronBlockEntity.addPotionEffects(effects);
        }
    }

    private ItemStack getMixedPotion(BlockState state, World world, BlockPos pos){
        if(world.getBlockEntity(pos) instanceof PotionCauldronBlockEntity potionCauldronBlockEntity){
            return potionCauldronBlockEntity.getMixedPotion();
        }
        return Items.POTION.getDefaultStack();
    }

    private void dilutePotions(BlockState state, World world, BlockPos pos){
        if(world.getBlockEntity(pos) instanceof PotionCauldronBlockEntity potionCauldronBlockEntity){
            potionCauldronBlockEntity.dilutePotionEffects();
        }
    }
}
