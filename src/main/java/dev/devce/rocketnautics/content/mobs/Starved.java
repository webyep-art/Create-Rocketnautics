package dev.devce.rocketnautics.content.mobs;

import dev.devce.rocketnautics.content.mobs.ai.StarvedHelmetSnatchGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public class Starved extends Zombie {
    public Starved(EntityType<? extends Zombie> p_34271_, Level p_34272_) {
        super(p_34271_, p_34272_);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new StarvedHelmetSnatchGoal(this, 1.15D));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, (double)35.0F).add(Attributes.MOVEMENT_SPEED, (double)0.23F).add(Attributes.ATTACK_DAMAGE, (double)3.0F).add(Attributes.ARMOR, (double)0.0F).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
    }

    protected boolean convertsInWater() {
        return false;
    }
}
