package yourname.aoe;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.text.Text;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class TunnelLoreDetector {
    private static final Pattern TUNNEL3 = Pattern.compile("(?i)\btunnel\s*(iii|3)\b");
    private static final Pattern ALT = Pattern.compile("(?i)\bтоннел(ь|я)?\s*(iii|3)\b");

    public static boolean looksLikeTunnelPick(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof MiningToolItem)) return false;
        if (textMatches(stack.getName())) return true;
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) if (textMatches(line)) return true;
        }
        return false;
    }
    private static boolean textMatches(Text t) {
        if (t == null) return false;
        String s = strip(t.getString());
        return TUNNEL3.matcher(s).find() || ALT.matcher(s).find();
    }
    private static String strip(String s) {
        if (s == null) return "";
        String norm = Normalizer.normalize(s, Normalizer.Form.NFKC);
        norm = norm.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
        norm = norm.replaceAll("\p{C}", "");
        norm = norm.replaceAll("\s+", " ").trim();
        return norm;
    }
}
