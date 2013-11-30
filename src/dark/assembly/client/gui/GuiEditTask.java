package dark.assembly.client.gui;

import java.util.HashMap;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import universalelectricity.core.vector.Vector2;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import dark.api.al.coding.ITask;
import dark.api.al.coding.args.ArgumentData;
import dark.assembly.AssemblyLine;
import dark.core.prefab.invgui.GuiBase;
import dark.core.prefab.invgui.GuiMessageBox;
import dark.core.prefab.invgui.IMessageBoxDialog;

public class GuiEditTask extends GuiBase implements IMessageBoxDialog
{
    public static final ResourceLocation TEXTURE = new ResourceLocation(AssemblyLine.instance.DOMAIN, AssemblyLine.GUI_DIRECTORY + "gui_task_edit.png");

    protected GuiEncoderCoder gui;
    protected ITask task, editTask;
    int ySpacing = 20;
    int xStart = 13, yStart = 50;
    protected GuiTextField[] argTextBoxes;
    int getFocus = -1;

    public GuiEditTask(GuiEncoderCoder gui, ITask task)
    {
        this.guiSize.y = 380 / 2;
        this.gui = gui;
        this.task = task;
        this.editTask = task.clone();
        this.editTask.load(task.save(new NBTTagCompound()));
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.drawButtons();
        Keyboard.enableRepeatEvents(true);
    }

    @SuppressWarnings("unchecked")
    public void drawButtons()
    {
        this.buttonList.clear();

        this.buttonList.add(new GuiButton(0, (this.width - this.guiSize.intX()) / 2 + 13, (this.height - this.guiSize.intY()) / 2 + 135, 50, 20, "Save"));

        this.buttonList.add(new GuiButton(1, (this.width - this.guiSize.intX()) / 2 + 68, (this.height - this.guiSize.intY()) / 2 + 135, 50, 20, "Cancel"));

        this.buttonList.add(new GuiButton(2, (this.width - this.guiSize.intX()) / 2 + 125, (this.height - this.guiSize.intY()) / 2 + 135, 40, 20, "Del"));

        HashMap<String, Object> args = task.getArgs();
        List<ArgumentData> eArgs = task.getEncoderParms();

        if (eArgs != null && !eArgs.isEmpty())
        {
            this.argTextBoxes = new GuiTextField[eArgs.size()];
            for (int i = 0; i < this.argTextBoxes.length; i++)
            {
                ArgumentData arg = eArgs.get(i);
                this.argTextBoxes[i] = new GuiTextField(this.fontRenderer, (this.width - this.guiSize.intX()) / 2 + 60, (this.height - this.guiSize.intY()) / 2 + 64 + (i * this.ySpacing), 30, 10);
                this.argTextBoxes[i].setMaxStringLength(30);
                this.argTextBoxes[i].setVisible(true);
                if (args.containsKey(arg.getName()))
                {
                    this.argTextBoxes[i].setText("" + args.get(arg.getName()));
                }
                else
                {
                    this.argTextBoxes[i].setText("" + arg.getData());
                }
            }
        }

    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void keyTyped(char character, int keycode)
    {
        if (keycode == Keyboard.KEY_ESCAPE)
        {
            this.mc.thePlayer.closeScreen();
        }
        else if (keycode == Keyboard.KEY_TAB)
        {
            if (this.argTextBoxes != null)
            {
                this.getFocus += 1;
                if (this.getFocus >= this.argTextBoxes.length)
                {
                    this.getFocus = 0;
                }
            }
            else
            {
                this.getFocus = -1;
            }
        }
        else
        {
            if (this.argTextBoxes != null && this.getFocus > -1 && this.getFocus < this.argTextBoxes.length)
            {
                if (this.argTextBoxes[this.getFocus] != null)
                    this.argTextBoxes[this.getFocus].textboxKeyTyped(character, keycode);
            }
        }
    }

    @Override
    protected void mouseClicked(int par1, int par2, int par3)
    {
        super.mouseClicked(par1, par2, par3);
        this.getFocus = -1;
        if (this.argTextBoxes != null)
        {
            for (int i = 0; i < this.argTextBoxes.length; i++)
            {
                GuiTextField box = this.argTextBoxes[i];
                if (box != null && box.getVisible())
                {
                    box.mouseClicked(par1, par2, par3);
                    if (box.isFocused())
                    {
                        this.getFocus = i;
                    }
                }
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        super.actionPerformed(button);
        switch (button.id)
        {
            case 0:
            case 1:

                if (button.id == 0)
                {
                    this.gui.getTile().updateTask(this.editTask);
                }
                FMLCommonHandler.instance().showGuiScreen(this.gui);
                break;
            case 2:
                new GuiMessageBox(this, 0, "Remove Task", "Are you sure?").show();
                break;
        }
    }

    /** Draw the background layer for the GuiContainer (everything behind the items) */
    @Override
    protected void drawBackgroundLayer(int x, int y, float var1)
    {
        FMLClientHandler.instance().getClient().renderEngine.bindTexture(TEXTURE);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        int containerWidth = (this.width - this.guiSize.intX()) / 2;
        int containerHeight = (this.height - this.guiSize.intY()) / 2;
        this.drawTexturedModalRect(containerWidth, containerHeight, 0, 0, this.guiSize.intX(), this.guiSize.intY());
        if (this.argTextBoxes != null)
        {
            for (int i = 0; i < this.argTextBoxes.length; i++)
            {
                GuiTextField box = this.argTextBoxes[i];
                if (box != null)
                {
                    box.drawTextBox();
                }
            }
        }
    }

    @Override
    protected void drawForegroundLayer(int var2, int var3, float var1)
    {
        this.fontRenderer.drawString("Edit Task", (int) (this.guiSize.intX() / 2 - 7 * 2.5), 5, 4210752);
        this.fontRenderer.drawString("Task: " + "\u00a77" + this.task.getMethodName(), (int) ((this.guiSize.intX() / 2) - 70), 20, 4210752);
        this.fontRenderer.drawString("----Task Arguments---- ", (int) ((this.guiSize.intX() / 2) - 70), 50, 4210752);

        HashMap<String, Object> args = task.getArgs();
        List<ArgumentData> eArgs = task.getEncoderParms();

        if (eArgs != null && !eArgs.isEmpty())
        {
            int i = 0;
            for (ArgumentData arg : eArgs)
            {
                i++;
                this.fontRenderer.drawString(arg.getName() + ":", (int) ((this.guiSize.intX() / 2) - 70), 45 + (i * this.ySpacing), 4210752);
                this.fontRenderer.drawString(arg.warning(), (int) ((this.guiSize.intX() / 2) + 11), 45 + (i * this.ySpacing), 4210752);
            }
        }
        else
        {
            this.fontRenderer.drawString("\u00a77" + "     No editable args ", (int) ((this.guiSize.intX() / 2) - 70), 70, 4210752);

        }

    }

    @Override
    public void onMessageBoxClosed(int id, boolean yes)
    {
        if (id == 0 && yes)
        {
            this.gui.getTile().removeTask(new Vector2(this.editTask.getCol(), this.editTask.getRow()));
            FMLCommonHandler.instance().showGuiScreen(this.gui);
        }
    }
}
