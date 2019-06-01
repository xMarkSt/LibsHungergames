package me.libraryaddict.Hungergames.Enchants;

import me.libraryaddict.Hungergames.Types.HungergamesApi;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;

public class Undroppable extends Enchantment {

    public Undroppable() {
        super(new NamespacedKey(HungergamesApi.getHungergames(), "Undroppable"));
    }

    @Override
    public boolean canEnchantItem(ItemStack item) {
        return true;
    }

    @Override
    public boolean conflictsWith(Enchantment other) {
        return false;
    }

    @Override
    public EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.WEAPON;
    }

    @Override
    public boolean isTreasure() {
        return false;
    }

    @Override
    public boolean isCursed() {
        return false;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public String getName() {
        return "Undroppable";
    }

    @Override
    public int getStartLevel() {
        return 1;
    }
}
