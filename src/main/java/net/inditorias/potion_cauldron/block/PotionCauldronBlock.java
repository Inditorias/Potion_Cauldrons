package net.inditorias.potion_cauldron.block;


import net.inditorias.potion_cauldron.block_entity.PotionCauldronBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

public class PotionCauldronBlock extends AbstractCauldronBlock implements BlockEntityProvider{


    /* Layered Cauldron */
    public static final IntProperty LEVEL = Properties.LEVEL_3;
    public static final Predicate<Biome.Precipitation> RAIN_PREDICATE = precipitation -> precipitation == Biome.Precipitation.RAIN;

    private final Predicate<Biome.Precipitation> precipitationPredicate;
    public PotionCauldronBlock(Settings settings) {
        super(settings, CauldronBehavior.createMap());
        this.precipitationPredicate = RAIN_PREDICATE;
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.get(LEVEL) == 3;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && this.isEntityTouchingFluid(state, pos, entity)) {
            if(entity instanceof LivingEntity){
                List<StatusEffectInstance> effects = PotionUtil.getCustomPotionEffects(getMixedPotion(state, world, pos));
                LivingEntity living = (LivingEntity) entity;
                for(StatusEffectInstance effect : effects){
                    living.addStatusEffect(new StatusEffectInstance(effect.getEffectType(), effect.getDuration() / 4, effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon()));
                }
            }
            if(entity.isOnFire()) {
                entity.extinguish();
                if (entity.canModifyAt(world, pos)) {
                    this.onFireCollision(state, world, pos);
                }
            }
        }
    }

    @Override
    protected boolean canBeFilledByDripstone(Fluid fluid) {
        return fluid == Fluids.WATER && this.precipitationPredicate == RAIN_PREDICATE;
    }

    @Override
    protected double getFluidHeight(BlockState state) {
        return (6.0 + (double)state.get(LEVEL).intValue() * 3.0) / 16.0;
    }

    protected void onFireCollision(BlockState state, World world, BlockPos pos) {
        decrementFluidLevel(state, world, pos);
    }
    public static void decrementFluidLevel(BlockState state, World world, BlockPos pos) {
        int i = state.get(LEVEL) - 1;
        BlockState blockState = i == 0 ? Blocks.CAULDRON.getDefaultState() : (BlockState)state.with(LEVEL, i);
        world.setBlockState(pos, blockState);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
    }

    protected static boolean canFillWithPrecipitation(World world, Biome.Precipitation precipitation) {
        if (precipitation == Biome.Precipitation.RAIN) {
            return world.getRandom().nextFloat() < 0.05f;
        }
        if (precipitation == Biome.Precipitation.SNOW) {
            return world.getRandom().nextFloat() < 0.1f;
        }
        return false;
    }

    @Override
    public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
        if (canFillWithPrecipitation(world, precipitation) || state.get(LEVEL) == 3 || !this.precipitationPredicate.test(precipitation)) {
            return;
        }
        dilutePotions(state, world, pos);
        BlockState blockState = (BlockState)state.cycle(LEVEL);
        world.setBlockState(pos, blockState);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
    }
    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        int level = PotionUtil.getPotion(getMixedPotion(state, world, pos)).getEffects().size();
        return Math.min(level, 15);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected void fillFromDripstone(BlockState state, World world, BlockPos pos, Fluid fluid) {
        if (this.isFull(state)) {
            return;
        }
        dilutePotions(state, world, pos);
        BlockState blockState = (BlockState)state.with(LEVEL, state.get(LEVEL) + 1);
        world.setBlockState(pos, blockState);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
        world.syncWorldEvent(WorldEvents.POINTED_DRIPSTONE_DRIPS_WATER_INTO_CAULDRON, pos, 0);
    }

    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(Items.CAULDRON);
    }

    /* Block Entity */

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PotionCauldronBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }


    /* Potion Cauldron */

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(!world.isClient()){
            ItemStack itemStack = player.getStackInHand(hand);
            if(itemStack.isOf(Items.POTION)) {
                if (state.get(LEVEL) < 3) {
                    List<StatusEffectInstance> effects = PotionUtil.getCustomPotionEffects(itemStack);
                    if (effects.isEmpty()) {
                        effects = PotionUtil.getPotionEffects(itemStack);
                    }
                    addPotion(effects, state, world, pos);
                    dilutePotions(state, world, pos);
                    world.setBlockState(pos, state.cycle(LEVEL));
                    if (!player.isCreative()) {
                        player.setStackInHand(hand, ItemUsage.exchangeStack(itemStack, player, new ItemStack(Items.GLASS_BOTTLE)));
                    }
                    world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
                    return ActionResult.PASS;
                }
            }else if(itemStack.isOf(Items.GLASS_BOTTLE)){
                player.giveItemStack(getMixedPotion(state, world, pos));
                if(!player.isCreative()){
                    itemStack.decrement(1);
                }
                decrementFluidLevel(state, world, pos);
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
                return ActionResult.PASS;
            }else if(itemStack.isOf(Items.ARROW)){
                List<StatusEffectInstance> effects = PotionUtil.getCustomPotionEffects(itemStack);
                if(effects.isEmpty()){
                    effects = PotionUtil.getPotionEffects(itemStack);
                }
                int numItems = itemStack.getCount();
                if(!player.isCreative()){
                    itemStack.decrement(itemStack.getCount());
                }

                ItemStack itemStack2 = new ItemStack(Items.TIPPED_ARROW, numItems);
                PotionUtil.setPotion(itemStack2, PotionUtil.getPotion(itemStack));
                PotionUtil.setCustomPotionEffects(itemStack2, PotionUtil.getCustomPotionEffects(itemStack));

                player.giveItemStack(itemStack2);
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
                return ActionResult.PASS;
//            }else if(itemStack.isOf(Items.SPECTRAL_ARROW)){
//                List<StatusEffectInstance> effects = PotionUtil.getCustomPotionEffects(itemStack);
//                if(effects.isEmpty()){
//                    effects = PotionUtil.getPotionEffects(itemStack);
//                }
//                if(!player.isCreative()){
//                    itemStack.decrement(itemStack.getCount());
//                }
//                effects.add(new StatusEffectInstance(StatusEffects.GLOWING, 120));
//                player.giveItemStack(PotionUtil.setCustomPotionEffects(Items.TIPPED_ARROW.getDefaultStack(), effects));
//                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
//                world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
//                return ActionResult.PASS;
            }
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    public void addPotion(List<StatusEffectInstance> effects, BlockState state, World world, BlockPos pos){
        if(world.getBlockEntity(pos) instanceof PotionCauldronBlockEntity potionCauldronBlockEntity){
            potionCauldronBlockEntity.addPotionEffects(effects);
        }
    }

    public ItemStack getMixedPotion(BlockState state, World world, BlockPos pos){
        if(world.getBlockEntity(pos) instanceof PotionCauldronBlockEntity potionCauldronBlockEntity){
            return potionCauldronBlockEntity.getMixedPotion();
        }
        return Items.POTION.getDefaultStack();
    }

    public void dilutePotions(BlockState state, World world, BlockPos pos){
        if(world.getBlockEntity(pos) instanceof PotionCauldronBlockEntity potionCauldronBlockEntity){
            potionCauldronBlockEntity.dilutePotionEffects();
        }
    }
}
