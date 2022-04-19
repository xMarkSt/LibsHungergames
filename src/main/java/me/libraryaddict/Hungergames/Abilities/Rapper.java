package me.libraryaddict.Hungergames.Abilities;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.libraryaddict.Hungergames.Interfaces.Disableable;
import me.libraryaddict.Hungergames.Types.AbilityListener;
import me.libraryaddict.Hungergames.Types.HungergamesApi;

/**
 * Kit that gives the user a note block and lets them right click on it to give everything in audible range nausea.
 * The nausea doesn't stack and the user is affected by it too.
 */
public class Rapper extends AbilityListener implements Disableable {
    public int hearingRange = 16;
    public int nauseaLength = 7;

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (hasAbility(event.getPlayer())) {
            if (event.getAction() != Action.PHYSICAL && event.getClickedBlock() != null) {
                if (event.getClickedBlock().getType() == Material.NOTE_BLOCK) {
                    for (Entity entity : event.getPlayer().getNearbyEntities(hearingRange, hearingRange, hearingRange)) {
                        if (entity instanceof LivingEntity
                                && (!(entity instanceof Player) || HungergamesApi.getPlayerManager().getGamer(entity).isAlive())) {
                            ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION,
                                    nauseaLength * 20, 0), true);
                        }
                    }
                    ((LivingEntity) event.getPlayer()).addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION,
                            nauseaLength * 20, 0), true);
                }
            }
        }
    }

}
