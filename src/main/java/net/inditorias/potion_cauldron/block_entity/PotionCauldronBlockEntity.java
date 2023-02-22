package net.inditorias.potion_cauldron.block_entity;

import net.inditorias.potion_cauldron.registries.BlockEntityRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.stat.Stat;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PotionCauldronBlockEntity extends BlockEntity {

    private List<StatusEffectInstance> effects = new ArrayList<>();

    public PotionCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.POTION_CAULDRON_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
//        int raw_type;
//        int duration;
//        int amplifier;
//        boolean ambient;
//        boolean showParticles;
//        boolean showIcon;
        ArrayList<Integer> raw_types = new ArrayList<>();
        ArrayList<Integer> durations = new ArrayList<>();
        ArrayList<Integer> amplifiers = new ArrayList<>();
        ArrayList<Integer> ambiance = new ArrayList<>();
        ArrayList<Integer> particles = new ArrayList<>();
        ArrayList<Integer> icons = new ArrayList<>();

        for(StatusEffectInstance effect : effects){
            raw_types.add(StatusEffect.getRawId(effect.getEffectType()));
            durations.add(effect.getDuration());
            amplifiers.add(effect.getAmplifier());
            ambiance.add(effect.isAmbient() ? 1 : 0);
            particles.add(effect.shouldShowParticles()?1:0);
            icons.add((effect.shouldShowIcon()?1:0));
        }

        nbt.putIntArray("raw_types", raw_types);
        nbt.putIntArray("durations", durations);
        nbt.putIntArray("amplifiers", amplifiers);
        nbt.putIntArray("ambiance", ambiance);
        nbt.putIntArray("particles", particles);
        nbt.putIntArray("icons", icons);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        int[] raw_types = nbt.getIntArray("raw_types");
        int[] durations = nbt.getIntArray("durations");
        int[] amplifiers = nbt.getIntArray("amplifiers");
        int[] ambiance = nbt.getIntArray("ambiance");
        int[] particles = nbt.getIntArray("particles");
        int[] icons = nbt.getIntArray("icons");

        effects = new ArrayList<>();

        for(int i = 0; i < raw_types.length; i++){
            effects.add(new StatusEffectInstance(
                    StatusEffect.byRawId(raw_types[i]),
                    durations[i],
                    amplifiers[i],
                    ambiance[i] == 1,
                    particles[i] == 1,
                    icons[i] == 1
            ));
        }
        super.readNbt(nbt);
    }

    public ItemStack getMixedPotion(){
        return PotionUtil.setCustomPotionEffects(PotionUtil.setPotion(Items.POTION.getDefaultStack(), Potions.THICK),effects).setCustomName(Text.literal("Brewed Potion"));
    }

    public void addPotionEffects(List<StatusEffectInstance> p_effects){
        effects = combineEffects(effects, p_effects);
        this.markDirty();
    }

    private List<StatusEffectInstance> combineEffects(List<StatusEffectInstance> effects1, List<StatusEffectInstance> effects2){
        List<StatusEffectInstance> combined = effects1;
        combined.addAll(effects2);
        combined.sort((StatusEffectInstance::compareTo));
        for(int i = 0; i < combined.size() - 1; i++){
            if(combined.get(i).getEffectType().equals(combined.get(i + 1).getEffectType())){
                combined.set(i, combineEffects(combined.get(i), combined.get(i + 1)).get(0));
                combined.remove(i + 1);
                i--;
            }
        }
        return combined;
    }

    private List<StatusEffectInstance> combineEffects(StatusEffectInstance effect1, StatusEffectInstance effect2){
        List<StatusEffectInstance> combined = new ArrayList<>();
        if(effect1.getEffectType().equals(effect2.getEffectType())){
            combined.add(new StatusEffectInstance(
                    effect1.getEffectType(),
                    effect1.getDuration() + effect2.getDuration(),
                    Math.max(effect1.getAmplifier(), effect2.getAmplifier()),
                    effect1.isAmbient() || effect2.isAmbient(),
                    effect1.shouldShowParticles() || effect2.shouldShowParticles(),
                    effect1.shouldShowIcon() || effect2.shouldShowIcon()
            ));
        }else{
            combined.add(effect1);
            combined.add(effect2);
        }
        return combined;
    }

    public void dilutePotionEffects(){
        List<StatusEffectInstance> n_effects = new ArrayList<>();
        for(StatusEffectInstance effect : effects){
            n_effects.add(new StatusEffectInstance(effect.getEffectType(), (int)(effect.getDuration() * 0.5),
                    effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon()));
        }
        effects = n_effects;
        this.markDirty();
    }

    public int getColor(){
        return PotionUtil.getColor(effects);
    }
}
