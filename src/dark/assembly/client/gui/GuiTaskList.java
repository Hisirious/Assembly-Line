package dark.assembly.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import dark.api.al.coding.IProgram;
import dark.api.al.coding.IRedirectTask;
import dark.api.al.coding.ITask;
import dark.assembly.armbot.command.TaskEnd;
import dark.assembly.armbot.command.TaskStart;
import dark.assembly.machine.encoder.TileEntityEncoder;
import dark.core.interfaces.IScroll;

/** Not a gui itself but a component used to display task as a box inside of a gui
 *
 * @author DarkGuardsman */
public class GuiTaskList extends Gui implements IScroll
{
    protected int scrollY = 0, scrollX;

    protected TileEntity entity;

    /** The string displayed on this control. */
    public String displayString;

    int xPos, yPos;
    int countX = 6, countY = 7;
    GuiEncoderCoder coder;

    public GuiTaskList(TileEntity entity, GuiEncoderCoder coder, int x, int y)
    {
        this.xPos = x;
        this.yPos = y;
        this.coder = coder;
        this.entity = entity;

        if (this.getProgram() != null)
        {
            if (this.getProgram().getSize().intX() < (this.countX / 2))
            {
                this.scrollX = -2;
            }
            else
            {
                this.scrollX = 0;
            }
        }
        else
        {
            this.scrollX = 0;
            this.scrollY = 0;
        }

    }

    public IProgram getProgram()
    {
        if (entity instanceof TileEntityEncoder)
        {
            return ((TileEntityEncoder) entity).getProgram();
        }
        return null;
    }

    @Override
    public void scroll(int amount)
    {
        this.scrollY += amount;
    }

    public void scrollSide(int i)
    {
        this.scrollX += i;
    }

    @Override
    public void setScroll(int length)
    {
        this.scrollY = length;
    }

    @Override
    public int getScroll()
    {
        return this.scrollY;
    }

    public void drawConsole(Minecraft mc)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        for (int colume = 0; colume < countX; colume++)
        {
            int actualCol = colume + this.scrollX;
            for (int row = 0; row < countY; row++)
            {
                int actualRow = row + this.scrollY - 1;
                boolean drawnButton = false;

                if (this.getProgram() != null)
                {
                    ITask task = this.getProgram().getTaskAt(actualCol, actualRow);
                    if (actualRow == -1 && colume + this.scrollX - 1 == -1)
                    {
                       task = new TaskStart();
                    }
                    else if (actualRow == this.getProgram().getSize().intY() + 1 && colume + this.scrollX - 1 == -1)
                    {
                        task = new TaskEnd();
                    }
                    if (task != null && (!(task instanceof IRedirectTask) || task instanceof IRedirectTask && ((IRedirectTask) task).render()))
                    {
                        drawnButton = true;
                        FMLClientHandler.instance().getClient().renderEngine.bindTexture(task.getTextureSheet());
                        this.drawTexturedModalRect(xPos + (20 * colume), yPos + (20 * row), task.getTextureUV().intX(), task.getTextureUV().intY(), 20, 20);
                    }
                }
                if (!drawnButton)
                {
                    FMLClientHandler.instance().getClient().renderEngine.bindTexture(ITask.TaskType.TEXTURE);
                    this.drawTexturedModalRect(xPos + (20 * colume), yPos + (20 * row), 0, 40, 20, 20);
                }
            }

        }
    }

    protected void drawGuiContainerForegroundLayer(Minecraft mc, int cx, int cy)
    {
        ITask task = this.getTaskAt(cx, cy);
        if (task != null && coder != null)
        {
            coder.drawTooltip(cx - coder.getGuiLeft(), cy - coder.getGuiTop() + 10, task.getMethodName());
        }
    }

    public void mousePressed(int cx, int cy)
    {
        ITask task = this.getTaskAt(cx, cy);
        if (task != null)
        {
            FMLCommonHandler.instance().showGuiScreen(new GuiEditTask(this.coder, task));
        }
    }

    public ITask getTaskAt(int cx, int cz)
    {
        if (cx >= this.xPos && cz >= this.yPos && cx < this.xPos + (this.countX * 20) + 20 && cz < this.yPos + (this.countX * 20) + 20)
        {
            int col = ((cx - this.xPos) / 20) + this.scrollX;
            int row = ((cz - this.yPos) / 20) + this.scrollY;
            if (this.getProgram() != null)
            {
                return this.getProgram().getTaskAt(col, row - 1);
            }
        }
        return null;
    }
}
