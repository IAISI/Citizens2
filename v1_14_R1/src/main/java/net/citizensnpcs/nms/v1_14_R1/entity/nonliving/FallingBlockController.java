package net.citizensnpcs.nms.v1_14_R1.entity.nonliving;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.CraftServer;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftFallingBlock;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_14_R1.util.NMSBoundingBox;
import net.citizensnpcs.nms.v1_14_R1.util.NMSImpl;
import net.citizensnpcs.npc.AbstractEntityController;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_14_R1.AxisAlignedBB;
import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.EntityFallingBlock;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.EnumMoveType;
import net.minecraft.server.v1_14_R1.EnumPistonReaction;
import net.minecraft.server.v1_14_R1.FluidType;
import net.minecraft.server.v1_14_R1.IBlockData;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.Tag;
import net.minecraft.server.v1_14_R1.Vec3D;
import net.minecraft.server.v1_14_R1.World;
import net.minecraft.server.v1_14_R1.WorldServer;

public class FallingBlockController extends AbstractEntityController {


    @Override
    protected Entity createEntity(Location at, NPC npc) {
        WorldServer ws = ((CraftWorld) at.getWorld()).getHandle();
        Block id = CraftMagicNumbers.getBlock(npc.getItemProvider().get().getType());
        final EntityFallingBlockNPC handle = new EntityFallingBlockNPC(ws, npc, at.getX(), at.getY(), at.getZ(),
                id.getBlockData());
        return handle.getBukkitEntity();
    }

    @Override
    public FallingBlock getBukkitEntity() {
        return (FallingBlock) super.getBukkitEntity();
    }

    public static class EntityFallingBlockNPC extends EntityFallingBlock implements NPCHolder {
        private final CitizensNPC npc;

        public EntityFallingBlockNPC(EntityTypes<? extends EntityFallingBlock> types, World world) {
            this(types, world, null);
        }

        public EntityFallingBlockNPC(EntityTypes<? extends EntityFallingBlock> types, World world, NPC npc) {
            super(types, world);
            this.npc = (CitizensNPC) npc;
        }

        public EntityFallingBlockNPC(World world, NPC npc, double d0, double d1, double d2, IBlockData data) {
            super(world, d0, d1, d2, data);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        public void a(AxisAlignedBB bb) {
            super.a(NMSBoundingBox.makeBB(npc, bb));
        }

        @Override
        public boolean a(EntityPlayer player) {
            return NMS.shouldBroadcastToPlayer(npc, () -> super.a(player));
        }

        @Override
        public boolean b(Tag<FluidType> tag) {
            if (npc == null)
                return super.b(tag);
            Vec3D old = getMot().add(0, 0, 0);
            boolean res = super.b(tag);
            if (!npc.isPushableByFluids()) {
                setMot(old);
            }
            return res;
        }

        @Override
        public void collide(net.minecraft.server.v1_14_R1.Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.collide(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public boolean d(NBTTagCompound save) {
            return npc == null ? super.d(save) : false;
        }

        @Override
        public void f(double x, double y, double z) {
            Vector vector = Util.callPushEvent(npc, x, y, z);
            if (vector != null) {
                super.f(vector.getX(), vector.getY(), vector.getZ());
            }
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                NMSImpl.setBukkitEntity(this, new FallingBlockNPC(this));
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public EnumPistonReaction getPushReaction() {
            return Util.callPistonPushEvent(npc) ? EnumPistonReaction.IGNORE : super.getPushReaction();
        }

        @Override
        public void tick() {
            if (npc != null) {
                npc.update();
                Vec3D mot = getMot();
                if (Math.abs(mot.getX()) > EPSILON || Math.abs(mot.getY()) > EPSILON
                        || Math.abs(mot.getZ()) > EPSILON) {
                    mot = mot.d(0.98, 0.98, 0.98);
                    setMot(mot);
                    move(EnumMoveType.SELF, mot);
                }
            } else {
                super.tick();
            }
        }

        @Override
        public void updateSize() {
            if (npc == null) {
                super.updateSize();
            } else {
                NMSImpl.setSize(this, justCreated);
            }
        }

        private static final double EPSILON = 0.001;
    }

    public static class FallingBlockNPC extends CraftFallingBlock implements NPCHolder {
        private final CitizensNPC npc;

        public FallingBlockNPC(EntityFallingBlockNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
        }

        @Override
        public NPC getNPC() {
            return npc;
        }
    }
}
