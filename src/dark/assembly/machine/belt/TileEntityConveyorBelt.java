package dark.assembly.machine.belt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.core.vector.Vector3;
import universalelectricity.prefab.tile.IRotatable;

import com.google.common.io.ByteArrayDataInput;

import cpw.mods.fml.common.network.Player;
import dark.api.al.IBelt;
import dark.assembly.ALRecipeLoader;
import dark.assembly.machine.TileEntityAssembly;
import dark.core.network.PacketHandler;
import dark.machines.DarkMain;

/** Conveyer belt TileEntity that allows entities of all kinds to be moved
 * 
 * @author DarkGuardsman */
public class TileEntityConveyorBelt extends TileEntityAssembly implements IBelt, IRotatable
{

    public enum SlantType
    {
        NONE,
        UP,
        DOWN,
        TOP
    }

    public static final int MAX_FRAME = 13;
    public static final int MAX_SLANT_FRAME = 23;
    /** Packet name to ID the packet containing info on the angle of the belt */
    public static final String slantPacketID = "slantPacket";
    /** Acceleration of entities on the belt */
    public final float acceleration = 0.01f;
    /** max speed of entities on the belt */
    public final float maxSpeed = 0.1f;
    /** Current rotation of the model wheels */
    public float wheelRotation = 0;
    /** Frame count for texture animation from 0 - maxFrame */
    private int animFrame = 0;
    private SlantType slantType = SlantType.NONE;
    /** Entities that are ignored allowing for other tiles to interact with them */
    public List<Entity> IgnoreList = new ArrayList<Entity>();

    public TileEntityConveyorBelt()
    {
        super(.1f);
    }

    @Override
    public void updateEntity()
    {
        super.updateEntity();
        /* PROCESSES IGNORE LIST AND REMOVES UNNEED ENTRIES */
        Iterator<Entity> it = this.IgnoreList.iterator();
        while (it.hasNext())
        {
            if (!this.getAffectedEntities().contains(it.next()))
            {
                it.remove();
            }
        }
        if (this.worldObj.isRemote && this.isFunctioning())
        {
            if (this.ticks % 10 == 0 && this.worldObj.isRemote && this.worldObj.getBlockId(this.xCoord - 1, this.yCoord, this.zCoord) != ALRecipeLoader.blockConveyorBelt.blockID && this.worldObj.getBlockId(xCoord, yCoord, zCoord - 1) != ALRecipeLoader.blockConveyorBelt.blockID)
            {
                this.worldObj.playSound(this.xCoord, this.yCoord, this.zCoord, "mods.assemblyline.conveyor", 0.5f, 0.7f, true);
            }
            if (!DarkMain.zeroAnimation)
            {
                this.wheelRotation = (40 + this.wheelRotation) % 360;

                float wheelRotPct = wheelRotation / 360f;

                // Sync the animation. Slant belts are slower.
                if (this.getSlant() == SlantType.NONE || this.getSlant() == SlantType.TOP)
                {
                    this.animFrame = (int) (wheelRotPct * MAX_FRAME);
                    if (this.animFrame < 0)
                        this.animFrame = 0;
                    if (this.animFrame > MAX_FRAME)
                        this.animFrame = MAX_FRAME;
                }
                else
                {
                    this.animFrame = (int) (wheelRotPct * MAX_SLANT_FRAME);
                    if (this.animFrame < 0)
                        this.animFrame = 0;
                    if (this.animFrame > MAX_SLANT_FRAME)
                        this.animFrame = MAX_SLANT_FRAME;
                }
            }
        }

    }

    @Override
    public boolean canFunction()
    {
        return super.canFunction() && !this.worldObj.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public Packet getDescriptionPacket()
    {
        if (this.slantType != SlantType.NONE)
        {
            return PacketHandler.instance().getTilePacket(this.getChannel(), this, slantPacketID, this.isFunctioning(), this.slantType.ordinal());
        }
        return super.getDescriptionPacket();
    }

    @Override
    public boolean simplePacket(String id, ByteArrayDataInput dis, Player player)
    {
        if (!super.simplePacket(id, dis, player) && this.worldObj.isRemote)
        {
            try
            {
                if (id.equalsIgnoreCase(slantPacketID))
                {
                    this.functioning = dis.readBoolean();
                    this.slantType = SlantType.values()[dis.readInt()];
                    return true;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return false;
    }

    public SlantType getSlant()
    {
        return slantType;
    }

    public void setSlant(SlantType slantType)
    {
        if (slantType == null)
        {
            slantType = SlantType.NONE;
        }
        this.slantType = slantType;
        this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    /** Is this belt in the front of a conveyor line? Used for rendering. */
    public boolean getIsFirstBelt()
    {
        Vector3 vec = new Vector3(this);
        TileEntity fBelt = vec.clone().modifyPositionFromSide(this.getDirection()).getTileEntity(this.worldObj);
        TileEntity bBelt = vec.clone().modifyPositionFromSide(this.getDirection().getOpposite()).getTileEntity(this.worldObj);
        if (fBelt instanceof TileEntityConveyorBelt && !(bBelt instanceof TileEntityConveyorBelt))
        {
            return ((TileEntityConveyorBelt) fBelt).getDirection() == this.getDirection();
        }
        return false;
    }

    /** Is this belt in the middile of two belts? Used for rendering. */
    public boolean getIsMiddleBelt()
    {

        Vector3 vec = new Vector3(this);
        TileEntity fBelt = vec.clone().modifyPositionFromSide(this.getDirection()).getTileEntity(this.worldObj);
        TileEntity bBelt = vec.clone().modifyPositionFromSide(this.getDirection().getOpposite()).getTileEntity(this.worldObj);
        if (fBelt instanceof TileEntityConveyorBelt && bBelt instanceof TileEntityConveyorBelt)
        {
            return ((TileEntityConveyorBelt) fBelt).getDirection() == this.getDirection() && ((TileEntityConveyorBelt) bBelt).getDirection() == this.getDirection();
        }
        return false;
    }

    /** Is this belt in the back of a conveyor line? Used for rendering. */
    public boolean getIsLastBelt()
    {
        Vector3 vec = new Vector3(this);
        TileEntity fBelt = vec.clone().modifyPositionFromSide(this.getDirection()).getTileEntity(this.worldObj);
        TileEntity bBelt = vec.clone().modifyPositionFromSide(this.getDirection().getOpposite()).getTileEntity(this.worldObj);
        if (bBelt instanceof TileEntityConveyorBelt && !(fBelt instanceof TileEntityConveyorBelt))
        {
            return ((TileEntityConveyorBelt) bBelt).getDirection() == this.getDirection().getOpposite();
        }
        return false;
    }

    @Override
    public void setDirection(ForgeDirection facingDirection)
    {
        this.worldObj.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, facingDirection.ordinal(), 3);
    }

    @Override
    public ForgeDirection getDirection()
    {
        return ForgeDirection.getOrientation(this.getBlockMetadata());
    }

    @Override
    public List<Entity> getAffectedEntities()
    {
        return worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(this.xCoord, this.yCoord, this.zCoord, this.xCoord + 1, this.yCoord + 1, this.zCoord + 1));
    }

    public int getAnimationFrame()
    {
        return this.animFrame;
    }

    /** NBT Data */
    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        this.slantType = SlantType.values()[nbt.getByte("slant")];
    }

    /** Writes a tile entity to NBT. */
    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setByte("slant", (byte) this.slantType.ordinal());
    }

    @Override
    public void ignoreEntity(Entity entity)
    {
        if (!this.IgnoreList.contains(entity))
        {
            this.IgnoreList.add(entity);
        }

    }

    @Override
    public boolean canConnect(ForgeDirection direction)
    {
        return direction == ForgeDirection.DOWN;
    }

    @Override
    public void refresh()
    {
        super.refresh();
        if (this.worldObj != null && !this.worldObj.isRemote)
        {
            Vector3 face = new Vector3(this).modifyPositionFromSide(this.getDirection());
            Vector3 back = new Vector3(this).modifyPositionFromSide(this.getDirection().getOpposite());
            TileEntity front, rear;
            if (this.slantType == SlantType.DOWN)
            {
                face.translate(new Vector3(0, -1, 0));
                back.translate(new Vector3(0, 1, 0));
            }
            else if (this.slantType == SlantType.UP)
            {
                face.translate(new Vector3(0, 1, 0));
                back.translate(new Vector3(0, -1, 0));
            }
            else
            {
                return;
            }
            front = face.getTileEntity(this.worldObj);
            rear = back.getTileEntity(this.worldObj);
            if (front instanceof TileEntityAssembly)
            {
                this.getTileNetwork().mergeNetwork(((TileEntityAssembly) front).getTileNetwork(), this);
                this.connectedTiles.add(front);
            }
            if (rear instanceof TileEntityAssembly)
            {
                this.getTileNetwork().mergeNetwork(((TileEntityAssembly) rear).getTileNetwork(), this);
                this.connectedTiles.add(rear);
            }

        }
    }

    @Override
    public double getWattLoad()
    {
        return 0.05 + (0.01 * this.getAffectedEntities().size());//50w + (10w * loadSize)
    }

}
