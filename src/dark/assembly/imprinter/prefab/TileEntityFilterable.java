package dark.assembly.imprinter.prefab;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.prefab.tile.IRotatable;
import dark.api.al.IFilterable;
import dark.assembly.imprinter.ItemImprinter;
import dark.assembly.machine.TileEntityAssembly;

public abstract class TileEntityFilterable extends TileEntityAssembly implements IRotatable, IFilterable
{
    private ItemStack filterItem;
    private boolean inverted;
    public static final int FILTER_SLOT = 0;
    public static final int BATERY_DRAIN_SLOT = 1;

    public TileEntityFilterable()
    {
        super(0f);
        this.invSlots = 2;
    }

    public TileEntityFilterable(float wattsPerTick, float maxEnergy)
    {
        super(wattsPerTick, maxEnergy);
    }

    public TileEntityFilterable(float wattsPerTick)
    {
        super(wattsPerTick);
    }

    /** Looks through the things in the filter and finds out which item is being filtered.
     * 
     * @return Is this filterable block filtering this specific ItemStack? */
    public boolean isFiltering(ItemStack itemStack)
    {
        if (this.getFilter() != null && itemStack != null)
        {
            ArrayList<ItemStack> checkStacks = ItemImprinter.getFilters(getFilter());

            if (checkStacks != null)
            {
                for (ItemStack stack : checkStacks)
                {
                    if (stack.isItemEqual(itemStack))
                    {
                        return !inverted;
                    }
                }
            }
        }

        return inverted;
    }

    @Override
    public void setFilter(ItemStack filter)
    {
        this.filterItem = filter;
        this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public ItemStack getFilter()
    {
        return this.filterItem;
    }

    public void setInverted(boolean inverted)
    {
        this.inverted = inverted;
        this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public boolean isInverted()
    {
        return this.inverted;
    }

    public void toggleInversion()
    {
        setInverted(!isInverted());
    }

    @Override
    public ForgeDirection getDirection()
    {
        return ForgeDirection.getOrientation(this.getBlockMetadata());
    }

    @Override
    public void setDirection(ForgeDirection facingDirection)
    {
        this.worldObj.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, facingDirection.ordinal(), 3);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setBoolean("inverted", inverted);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        if (nbt.hasKey("filter"))
        {
            this.getInventory().setInventorySlotContents(0, ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("filter")));
        }
        inverted = nbt.getBoolean("inverted");
    }

}
