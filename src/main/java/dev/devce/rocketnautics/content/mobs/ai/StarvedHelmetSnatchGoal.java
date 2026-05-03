package dev.devce.rocketnautics.content.mobs.ai;

import dev.devce.rocketnautics.registry.RocketItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class StarvedHelmetSnatchGoal extends Goal {

    private final Zombie mob;
    private final double speed;
    private Player target;

    private int grabCooldown = 0;

    public StarvedHelmetSnatchGoal(Zombie mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(mob.getTarget() instanceof Player player)) return false;

        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(RocketItems.SPACE_HELMET.get());
    }

    @Override
    public boolean canContinueToUse() {
        if (!(mob.getTarget() instanceof Player player)) return false;

        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return player.isAlive() && head.is(RocketItems.SPACE_HELMET.get());
    }

    @Override
    public void start() {
        this.target = (Player) mob.getTarget();
    }

    @Override
    public void stop() {
        this.target = null;
    }

    @Override
    public void tick() {
        if (target == null) return;

        // countdown happens regardless
        if (grabCooldown > 0) {
            grabCooldown--;
        }

        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        mob.getNavigation().moveTo(target, speed);

        double distSq = mob.distanceToSqr(target);

        if (distSq <= 2.5D * 2.5D && grabCooldown <= 0) {
            tryStripHelmet(target);
        }
    }

    private void tryStripHelmet(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);

        if (!helmet.is(RocketItems.SPACE_HELMET.get())) return;

        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

        player.spawnAtLocation(helmet.copy());

        mob.swing(mob.getUsedItemHand());

        grabCooldown = 40;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}