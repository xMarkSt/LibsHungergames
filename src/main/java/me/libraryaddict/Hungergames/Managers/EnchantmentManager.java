package me.libraryaddict.Hungergames.Managers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import me.libraryaddict.Hungergames.Enchants.*;
import me.libraryaddict.Hungergames.Types.HungergamesApi;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EnchantmentManager {

    private static final int[] BVAL = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
    private static List<NamespacedKey> customEnchants = new ArrayList<>();

    // Parallel arrays used in the conversion process.
    private static final String[] RCODE = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };

    public static Enchantment UNDROPPABLE;

    static {
        UNDROPPABLE = new Undroppable();
        try {
            Field field = Enchantment.class.getDeclaredField("acceptingNew");
            field.setAccessible(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        if (Enchantment.getByKey(UNDROPPABLE.getKey()) == null) {
            Enchantment.registerEnchantment(UNDROPPABLE);
            customEnchants.add(UNDROPPABLE.getKey());
        }
    }

    public static boolean isNatural(Enchantment ench) {
        if (customEnchants.contains(ench.getKey()))
            return false;
        return true;
    }

    // =========================================================== binaryToRoman
    private static String toRoman(int binary) {
        if (binary <= 0) {
            return "";
        }
        String roman = "";
        for (int i = 0; i < RCODE.length; i++) {
            while (binary >= BVAL[i]) {
                binary -= BVAL[i];
                roman += RCODE[i];
            }
        }
        return roman;
    }

    public static ItemStack updateEnchants(ItemStack item) {
        ArrayList<String> enchants = new ArrayList<String>();
        NameManager nm = HungergamesApi.getNameManager();
        for (Enchantment ench : item.getEnchantments().keySet()) {
            if (!isNatural(ench)) {
                String enchantName = nm.getEnchantName(ench);
                enchants.add(ChatColor.GRAY + enchantName + " " + toRoman(item.getEnchantments().get(ench)));
            }
        }
        ItemMeta meta = item.getItemMeta();
        if (meta.hasLore())
            for (String lore : meta.getLore())
                enchants.add(lore);
        meta.setLore(enchants);
        item.setItemMeta(meta);
        return item;
    }
}
