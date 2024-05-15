package me.alluseri.lunahook.modules.exploits;

import me.alluseri.lunahook.LunaCategory;
import me.alluseri.lunahook.Lunahook;
import me.alluseri.lunahook.mc;
import me.alluseri.lunahook.commands.LunaCommand;
import me.alluseri.lunahook.igui.GuiFakeDisconnected;
import me.alluseri.lunahook.impl.TextComponentLuna;
import me.alluseri.lunahook.managers.CommandManager;
import me.alluseri.lunahook.modules.LunaModule;
import me.alluseri.lunahook.utils.ingame.AccuratePos;
import me.alluseri.lunahook.utils.ingame.IUtils;
import me.alluseri.lunahook.utils.ingame.PlayerUtils;
import me.alluseri.lunahook.utils.ingame.WorldUtils;

import net.minecraft.entity.passive.AbstractChestHorse;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.network.play.server.SPacketWindowItems;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class AutoDamageDupe extends LunaModule {
	private static AutoDamageDupe insn;

	BlockPos lever1 = null;
	BlockPos lever2 = null;
	AccuratePos gravel = null;

	AccuratePos start = null;

	int stanag = 0;
	int stageCd = 0;

	int trueId = 0;
	EntityDonkey faker = null;

	public AutoDamageDupe() {
		super("AutoDamageDupe", "auto damage dupe", LunaCategory.EXPLOIT);
		insn = this;
		CommandManager.commands.add(new AutoRollbackDupeCommand());
	}

	public void toggle(boolean ingame) {
		super.toggle(ingame);
		if (enabled && ingame) {
			config.displayInArrayList = true;
			if (lever1 == null || gravel == null) {
				Lunahook.chatFormat("&7[&dAutoDamageDupe&7] &cEither of the positions are unset!");
				enabled = false;
				return;
			}
			if (!(mc.player().ridingEntity instanceof AbstractChestHorse)) {
				Lunahook.chatFormat("&7[&dAutoDamageDupe&7] &cYou have to be riding a donkey!");
				enabled = false;
				return;
			}
			start = new AccuratePos(mc.player().ridingEntity);
			stanag = 0;
			faker = null;
			gslots = null;
		}
	}

	public Packet<?> onCPacket(Packet<?> packet) {
		if (packet instanceof CPacketClickWindow) { // Fakies, haha
			return null;
		}
		if (faker != null && packet instanceof CPacketUseEntity) {
			CPacketUseEntity reap = (CPacketUseEntity) packet;
			if (reap.entityId == faker.getEntityId()) {
				reap.entityId = trueId;
			}
		}
		return packet;
	}

	ArrayList<ItemStack> gslots = null;

	public Packet<?> onSPacket(Packet<?> packet) {
		if (packet instanceof SPacketOpenWindow) {
			if (gslots == null)
				return packet;
			gslots.clear();
			for (int i = 9; i <= 44; i++) {
				gslots.add(IUtils.getInventory(i).copy());
			}
		}
		if (packet instanceof SPacketWindowItems) {
			if (gslots == null) {
				gslots = new ArrayList<>();
				return packet;
			}
			SPacketWindowItems swi = (SPacketWindowItems) packet;
			List<ItemStack> istks = swi.getItemStacks();
			final int donkeyStart = istks.size() == 90 ? 54 : 17;
			for (int i = 0; i < istks.size(); i++) {
				if (i >= donkeyStart) {
					istks.set(i, gslots.get(i - donkeyStart).copy());
				}
			}
		}
		return packet;
	}

	public void onPostUpdate() {
		if (stanag == 6) {
			faker.prevRotationYaw = faker.rotationYawHead = faker.rotationYaw = faker.prevRotationYawHead = 58;
			return;
		}
		AbstractChestHorse ach = (AbstractChestHorse) mc.player().ridingEntity;
		ach.setHealth(0.01F);
		if (stanag == 5) {
			// Create a new donkey in the start of track
			faker = new EntityDonkey(mc.player().world);
			faker.setPositionAndRotation(start.getX(), start.getY(), start.getZ(), 0, 0);
			faker.prevRotationYaw = faker.rotationYawHead = faker.rotationYaw = faker.prevRotationYawHead = 58;
			faker.setEntityId(ach.getEntityId() + 6666);
			faker.setChested(true);
			faker.rotationYaw = ach.rotationYaw;
			mc.player().world.spawnEntityInWorld(faker);
			stanag = 6;
			config.displayInArrayList = false;
			trueId = ach.getEntityId();
			return;
		}
		if (stanag == 0) {
			double dst = gravel.distanceTo2D(mc.player());
			if (dst <= 0.2) {
				stanag = 2;
				mc.player().connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(lever1, EnumFacing.DOWN, EnumHand.MAIN_HAND, 0.5F, 0.5F, 0.5F));
				Lunahook.chatFormat("&7[&dAutoDamageDupe&7] &bInit falling block.");
				return;
			}
			double[] axis = PlayerUtils.getRotation(PlayerUtils.getFacingYaw(gravel), Math.min(0.6D, dst));
			mc.player().ridingEntity.motionX = axis[0];
			mc.player().ridingEntity.motionZ = axis[1];
		} else if (stanag == 1) {
			/*if (WorldUtils.getBlock(new AccuratePos(mc.player().ridingEntity)) == Blocks.GRAVEL) {
				Lunahook.chatFormat("&7[&dAutoDamageDupe&7] &bTurn lever 2!!!");
				mc.player().connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(lever2, EnumFacing.DOWN, EnumHand.MAIN_HAND, 0.5F, 0.5F, 0.5F));
				stanag = 2;
			}*/
		} else if (stanag == 2) {
			if (WorldUtils.getBlock(new AccuratePos(mc.player().ridingEntity).add(0, 1, 0).bp()) == Blocks.GRAVEL) { // This won't actually kill us, because our donkey's true HP is full
				ach.attackEntityFrom(DamageSource.inWall, 50.0F);
				stanag = 3;
			}
		} else if (stanag == 3) {
			mc.gmc().displayGuiScreen(new GuiFakeDisconnected(null, new TextComponentLuna("AutoDamageDupe disconnect.")));
			stanag = 4;
			stageCd = 3;
		} else if (stanag == 4) {
			if (stageCd-- > 0)
				return;
			// Do our magic while we can
			double dst = start.distanceTo2D(mc.player());
			if (dst <= 0.3) {
				stanag = 5;
				mc.player().connection.getNetworkManager().closeChannel(new TextComponentLuna("AutoDamageDupe disconnect."));
				return;
			}
			double[] axis = PlayerUtils.getRotation(PlayerUtils.getFacingYaw(start), Math.min(0.7D, dst)); // Fucking pray we don't get dismounted
			mc.player().ridingEntity.motionX = axis[0];
			mc.player().ridingEntity.motionZ = axis[1];
		}
	}

	public static boolean isActive() {
		return insn.enabled;
	}

	public class AutoRollbackDupeCommand extends LunaCommand {
		public AutoRollbackDupeCommand() {
			super("autoddupe", "Manages the AutoDamageDupe module.", "addupe");
		}

		public void run(ArrayList<String> arguments) {
			switch (arguments.get(0).toLowerCase()) {
			case "lever":
				lever1 = mc.gmc().objectMouseOver.getBlockPos();
				Lunahook.chatFormat("&7[&dAutoDamageDupe&7] &aSet lever position!");
				break;
			/*case "lever2":
				lever2 = mc.gmc().objectMouseOver.getBlockPos();
				Lunahook.chatFormat("&7[&dAutoDamageDupe&7] &aSet position of the second lever!");
				break;*/
			case "gravel":
				gravel = PlayerUtils.getPosition();
				Lunahook.chatFormat("&7[&dAutoDamageDupe&7] &aSet gravel position!");
				break;
			}
		}
	}
}
