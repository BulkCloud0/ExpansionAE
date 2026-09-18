package dev.bulkcloud.expansionae.client;
import com.mojang.blaze3d.matrix.MatrixStack;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import dev.bulkcloud.expansionae.IngredientBufferContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
public final class IngredientBufferScreen extends AEBaseScreen<IngredientBufferContainer>{
    public IngredientBufferScreen(IngredientBufferContainer c,PlayerInventory p,ITextComponent t,ScreenStyle s){super(c,p,t,s);}
    @Override public void drawBG(MatrixStack pose,int x,int y,int mx,int my,float pt){
        fill(pose,x,y,x+xSize,y+ySize,0xff373737);fill(pose,x+1,y+1,x+xSize-1,y+ySize-1,0xffeeeeee);
        fill(pose,x+3,y+3,x+xSize-3,y+ySize-3,0xffc6c6c6);
        for(net.minecraft.inventory.container.Slot slot:container.inventorySlots){int sx=x+slot.xPos,sy=y+slot.yPos;fill(pose,sx-1,sy-1,sx+17,sy+17,0xff373737);fill(pose,sx,sy,sx+16,sy+16,0xff8b8b8b);}
    }
    @Override public void drawFG(MatrixStack pose,int ox,int oy,int mx,int my){
        super.drawFG(pose,ox,oy,mx,my);
        font.drawString(pose,new TranslationTextComponent("gui.expansionae.ingredient_buffer.fluid").getString(),8,94,0x404040);
        font.drawString(pose,container.getBuffer().getFluid().getAmount()+" mB",8,105,0x404040);
    }
}
