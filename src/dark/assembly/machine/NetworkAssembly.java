package dark.assembly.machine;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.core.block.IElectrical;
import universalelectricity.core.block.IElectricalStorage;
import universalelectricity.core.vector.Vector3;
import dark.api.tilenetwork.INetworkEnergyPart;
import dark.api.tilenetwork.INetworkPart;
import dark.core.prefab.tilenetwork.NetworkSharedPower;
import dark.core.prefab.tilenetwork.NetworkUpdateHandler;

public class NetworkAssembly extends NetworkSharedPower
{
    public static final int damandUpdateDelay = 100;
    /** Set of tiles that count as power sources */
    private Set<TileEntity> powerSources = new HashSet<TileEntity>();
    /** Set of tiles that count as power loads */
    private Set<TileEntity> powerLoads = new HashSet<TileEntity>();
    /** Network last calculation of power required by the network */
    private float lastNetDemand = 0;
    /** System time when the last power calculation was made */
    private long lastDemandCalcTime = 0;
    /** Network last calculation of power required by the network parts */
    private float lastNetPartsDemand = 0;
    /** System time when the last power calculation was made for network parts */
    private long lastDemandPCalcTime = 0;
    /** Date instance used to record the tile when last calculation were made */
    private Date date = new Date();

    /** Average power demand of the entire network */
    public float averageDemand = 0;
    /** highest demand the network has seen */
    public float maxDemand = 0;
    /** lowest demand the network has seen */
    public float minDemand = 0;
    static
    {
        NetworkUpdateHandler.registerNetworkClass("AssemblyNet", NetworkAssembly.class);
    }

    public NetworkAssembly()
    {
        super();
    }

    public NetworkAssembly(INetworkPart... parts)
    {
        super(parts);
    }

    /** Gets the demand of all parts of the network including network parts */
    public float getNetworkDemand()
    {
        float lastDemand = lastNetDemand;
        float currentDemand = 0;
        long lastTime = lastDemandCalcTime;
        long time = date.getTime();
        if (lastTime == 0)
        {
            lastTime = time;
        }
        currentDemand += getNetworkPartsDemand();
        Iterator<TileEntity> it = this.powerLoads.iterator();
        while (it.hasNext())
        {
            currentDemand += this.getDemandTile(it.next());
        }

        /* Average calculations */
        this.lastDemandCalcTime = time;
        this.lastNetDemand = currentDemand;
        //TODO calculate the averages over time to produce a better number
        if (this.minDemand == 0)
        {
            this.minDemand = currentDemand;
        }
        if (this.averageDemand == 0)
        {
            this.averageDemand = currentDemand;
        }
        if (currentDemand > this.maxDemand)
        {
            this.maxDemand = currentDemand;
        }
        if (currentDemand < this.minDemand)
        {
            this.minDemand = currentDemand;
        }
        this.averageDemand = (lastDemand + currentDemand + averageDemand) / 3;
        return currentDemand;
    }

    /** Gets the demand of all parts contained in the network so to power them first */
    public float getNetworkPartsDemand()
    {
        float lastDemand = lastNetPartsDemand;
        float currentDemand = 0;
        long lastTime = lastDemandPCalcTime;
        long time = date.getTime();
        if (lastTime == 0)
        {
            lastTime = time;
        }

        for (INetworkPart part : this.getMembers())
        {
            if (part instanceof TileEntityAssembly)
            {
                currentDemand += ((TileEntityAssembly) part).getWattLoad();
                currentDemand += ((TileEntityAssembly) part).getExtraLoad();
            }
        }

        lastDemandPCalcTime = time;
        lastNetPartsDemand = currentDemand;
        //TODO calculate average
        return currentDemand * 2;
    }

    public float getDemandTile(TileEntity te)
    {
        float demand = 0;
        if (te instanceof IElectrical)
        {
            Vector3 vec = new Vector3(te);
            for (int side = 0; side < 6; side++)
            {
                ForgeDirection dir = ForgeDirection.getOrientation(side);
                TileEntity ent = vec.clone().modifyPositionFromSide(dir).getTileEntity(te.worldObj);
                if (ent instanceof INetworkEnergyPart && ((INetworkEnergyPart) ent).getTileNetwork().equals(this))
                {
                    if (((IElectrical) te).canConnect(dir.getOpposite()))
                    {
                        demand += (((IElectrical) te).getRequest(dir.getOpposite()));
                    }
                }
            }
            if (te instanceof IElectricalStorage)
            {
                demand = Math.min(((IElectricalStorage) te).getMaxEnergyStored(), Math.min(((IElectricalStorage) te).getMaxEnergyStored() - ((IElectricalStorage) te).getEnergyStored(), demand));
            }
        }
        return demand;
    }

    /** Called when the network gets more power then its parts need. Also called after all parts in
     * the network get there power and is time to start suppling connections */
    public void supplyPower(float power)
    {

    }

    @Override
    public boolean addTile(TileEntity tileEntity, boolean member)
    {
        boolean higher = super.addTile(tileEntity, member);
        if (!higher && !member && tileEntity instanceof IElectrical)
        {
            Vector3 vec = new Vector3(tileEntity);
            for (int side = 0; side < 6; side++)
            {
                ForgeDirection dir = ForgeDirection.getOrientation(side);
                TileEntity ent = vec.clone().modifyPositionFromSide(dir).getTileEntity(tileEntity.worldObj);
                if (ent instanceof INetworkEnergyPart && ((INetworkEnergyPart) ent).getTileNetwork().equals(this))
                {
                    if (((IElectrical) tileEntity).canConnect(dir.getOpposite()))
                    {
                        if (!this.powerSources.contains(tileEntity) && ((IElectrical) tileEntity).getProvide(dir.getOpposite()) <= 0)
                        {
                            this.powerSources.add(tileEntity);
                            higher = true;
                        }
                        if (!this.powerLoads.contains(tileEntity) && ((IElectrical) tileEntity).getRequest(dir.getOpposite()) <= 0)
                        {
                            this.powerLoads.add(tileEntity);
                            higher = true;
                        }
                    }
                }
            }
        }
        return higher;
    }

    @Override
    public boolean removeTile(TileEntity ent)
    {
        return super.removeTile(ent) || this.powerLoads.remove(ent) || this.powerSources.remove(ent);
    }

    @Override
    public void cleanUpMembers()
    {
        super.cleanUpMembers();
        Iterator<TileEntity> it = powerSources.iterator();
        for (int set = 0; set < 2; set++)
        {
            while (it.hasNext())
            {
                TileEntity te = it.next();
                if (te == null)
                {
                    it.remove();
                }
                if (te.isInvalid())
                {
                    it.remove();
                }
                if (!(te instanceof IElectrical))
                {
                    it.remove();
                }
                else
                {
                    Vector3 vec = new Vector3(te);
                    int failedConnections = 0;

                    for (int side = 0; side < 6; side++)
                    {
                        ForgeDirection dir = ForgeDirection.getOrientation(side);
                        TileEntity ent = vec.clone().modifyPositionFromSide(dir).getTileEntity(te.worldObj);
                        if (ent instanceof INetworkEnergyPart && ((INetworkEnergyPart) ent).getTileNetwork() != null)
                        {
                            if (((INetworkEnergyPart) ent).getTileNetwork() != this)
                            {
                                it.remove();
                            }
                            if (!((IElectrical) te).canConnect(dir.getOpposite()))
                            {
                                failedConnections++;
                            }
                            else if (set == 0 && ((IElectrical) te).getProvide(dir.getOpposite()) <= 0)
                            {
                                failedConnections++;
                            }
                            else if (set == 1 && ((IElectrical) te).getRequest(dir.getOpposite()) <= 0)
                            {
                                failedConnections++;
                            }
                        }
                    }
                    if (failedConnections >= 6)
                    {
                        it.remove();
                    }
                }

            }
            it = powerLoads.iterator();
        }

    }

    @Override
    public boolean isValidMember(INetworkPart part)
    {
        return super.isValidMember(part) && part instanceof TileEntityAssembly;
    }

}
