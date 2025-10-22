package yourname.aoe;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import baritone.api.BaritoneAPI;
import com.mojang.brigadier.CommandDispatcher;

import java.util.ArrayList;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class AoEMiner implements ClientModInitializer {

    private static final int PROBE_DEPTH = 2;
    private static boolean AOE_ENABLED = true;
    private static boolean AUTO_SNEAK = true;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(this::registerCommands);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.player == null || client.world == null) return;

            var baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            boolean active = baritone.getMineProcess().isActive() || baritone.getPathingBehavior().isPathing();
            if (!active) { setSneak(client, false); return; }

            if (!AOE_ENABLED) { setSneak(client, false); return; }

            ItemStack hand = client.player.getMainHandStack();
            if (!TunnelLoreDetector.looksLikeTunnelPick(hand)) { setSneak(client, false); return; }

            if (AUTO_SNEAK && isBreaking(client)) setSneak(client, true);
            else if (!AUTO_SNEAK) setSneak(client, false);

            HitResult hr = client.crosshairTarget;
            if (!(hr instanceof BlockHitResult bhr)) return;

            Direction face = bhr.getSide();
            BlockPos origin = bhr.getBlockPos().offset(face);

            List<BlockPos> front = plane3x3(origin, face);
            List<BlockPos> probe = new ArrayList<>(front);
            for (int d = 1; d <= PROBE_DEPTH; d++) for (BlockPos p : front) probe.add(p.offset(face, d));

            BlockPos leak = null;
            for (BlockPos p : probe) {
                FluidState fs = client.world.getFluidState(p);
                if (fs.isIn(FluidTags.LAVA)) { leak = p.toImmutable(); break; }
            }
            if (leak != null) {
                baritone.getCommandManager().execute("pause");
                int slot = findSolidBlockSlot(client);
                if (slot != -1) client.player.getInventory().selectedSlot = slot;
                BlockPos plug = leak.offset(face.getOpposite());
                tryPlaceBlock(client, plug, face);
                scheduleLater(client, 5, () -> baritone.getCommandManager().execute("resume"));
            }
        });
    }

    private void registerCommands(CommandDispatcher<FabricClientCommandSource> d, CommandRegistryAccess a) {
        d.register(literal("aoe")
            .executes(ctx -> { ctx.getSource().sendFeedback(Text.literal("AOE: "+(AOE_ENABLED?"on":"off")+", autosneak: "+(AUTO_SNEAK?"on":"off"))); return 1; })
            .then(literal("on").executes(ctx -> { AOE_ENABLED=true; ctx.getSource().sendFeedback(Text.literal("AOE enabled")); return 1; }))
            .then(literal("off").executes(ctx -> { AOE_ENABLED=false; ctx.getSource().sendFeedback(Text.literal("AOE disabled")); return 1; }))
            .then(literal("autosneak")
                .then(literal("on").executes(ctx -> { AUTO_SNEAK=true; ctx.getSource().sendFeedback(Text.literal("AutoSneak enabled")); return 1; }))
                .then(literal("off").executes(ctx -> { AUTO_SNEAK=false; ctx.getSource().sendFeedback(Text.literal("AutoSneak disabled")); return 1; }))
            )
        );
    }

    // --- helpers ---
    private static boolean isBreaking(MinecraftClient c){ return c.interactionManager!=null && c.interactionManager.isBreakingBlock(); }
    private static void setSneak(MinecraftClient c, boolean down){ if (c.player!=null) c.player.setSneaking(down); }
    private static int findSolidBlockSlot(MinecraftClient c){
        var inv = c.player.getInventory();
        for(int i=0;i<9;i++){
            var st = inv.getStack(i);
            if (!st.isEmpty() && st.getItem().getTranslationKey().contains("block")) return i;
        }
        return -1;
    }
    private static List<BlockPos> plane3x3(BlockPos center, Direction face){
        List<BlockPos> out = new ArrayList<>(9);
        Direction u = switch (face.getAxis()) {
            case X -> Direction.NORTH;
            case Y -> Direction.EAST;
            case Z -> Direction.EAST;
        };
        Direction v = (face.getAxis()==Direction.Axis.Y)? Direction.SOUTH : Direction.UP;
        for(int du=-1;du<=1;du++) for(int dv=-1;dv<=1;dv++) out.add(center.offset(u,du).offset(v,dv));
        return out;
    }
    private static void tryPlaceBlock(MinecraftClient c, BlockPos pos, Direction against){
        if (c.interactionManager==null) return;
        c.interactionManager.interactBlock(c.player, c.world, Hand.MAIN_HAND, new BlockHitResult(c.player.getPos(), against, pos, false));
    }
    private static void scheduleLater(MinecraftClient c, int ticks, Runnable r){
        final int[] left={ticks};
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
            @Override public void onEndTick(MinecraftClient client){
                if(--left[0]<=0){ r.run(); ClientTickEvents.END_CLIENT_TICK.unregister(this); }
            }
        });
    }
}
