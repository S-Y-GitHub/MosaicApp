import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;

public class MosaicGenerator{
    List<BufferedImage> assets=new ArrayList<>();
    boolean hasAlpha=false;
    boolean keepAspect=false;
    int columns=50;
    int rows=50;

    public void addImage(File imageFile)throws IOException{
        assets.add(ImageIO.read(imageFile));
    }

    public void addImages(File... imageFiles)throws IOException{
        for(File imageFile:imageFiles){
            addImage(imageFile);
        }
    }

    public void removeImage(File imageFile)throws IOException{
        BufferedImage image=ImageIO.read(imageFile);
        for(BufferedImage asset:assets){
            if(isSameImage(asset,image)){
                assets.remove(asset);
            }
        }
    }

    public void removeImages(File... imageFiles)throws IOException{
        for(File imageFile:imageFiles){
            removeImage(imageFile);
        }
    }

    public void setColumns(int columns){
        this.columns=columns;
    }

    public void setRows(int rows){
        this.rows=rows;
    }

    public void setMatrixSize(int columns,int rows){
        this.columns=columns;
        this.rows=rows;
    }

    public void setHasAlpha(boolean hasAlpha){
        this.hasAlpha=hasAlpha;
    }

    public void setKeepAspect(boolean keepAspect){
        this.keepAspect=keepAspect;
    }

    public void generate(File originalFile,File resultFile)throws Exception{
        BufferedImage original=ImageIO.read(originalFile);

        final int ORIGINAL_WIDTH=original.getWidth();
        final int ORIGINAL_HEIGHT=original.getHeight();
        double atomWidth=(double)ORIGINAL_WIDTH/columns;
        double atomHeight=(double)ORIGINAL_HEIGHT/rows;

        BufferedImage result=new BufferedImage(ORIGINAL_WIDTH,ORIGINAL_HEIGHT,hasAlpha?BufferedImage.TYPE_4BYTE_ABGR:BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g=result.createGraphics();

        for(int c=0;c<columns;c++)for(int r=0;r<rows;r++){
            final int X_START=(int)(atomWidth*c);
            final int X_END=(int)(atomWidth*(c+1));
            final int Y_START=(int)(atomHeight*r);
            final int Y_END=(int)(atomHeight*(r+1));

            List<Integer> colors=new ArrayList<>();
            for(int x=X_START;x<X_END;x++)for(int y=Y_START;y<Y_END;y++){
                colors.add(original.getRGB(x,y));
            }

            int[] colorsInt=new int[colors.size()];
            for(int i=0;i<colorsInt.length;i++){
                colorsInt[i]=colors.get(i);
            }
            
            BufferedImage image=ImageUtil.resize(getImage(getColor(colorsInt).getRGB()),(int)atomWidth,(int)atomHeight,keepAspect);
            if(!hasAlpha){
                ImageUtil.removeAlpha(image);
            }
            g.drawImage(image,X_START,Y_START,null);
        }
        String resultFileName=resultFile.getName();
        String formatName=resultFileName.substring(resultFileName.lastIndexOf(".")+1);

        ImageIO.write(result,formatName,resultFile);
    }

    private boolean isSameImage(BufferedImage image1,BufferedImage image2){
        int width,height;
        if((width=image1.getWidth())!=image2.getWidth()||(height=image1.getHeight())!=image2.getHeight()){
            return false;
        }
        for(int x=0;x<width;x++)for(int y=0;y<height;y++){
            if(image1.getRGB(x,y)!=image2.getRGB(x,y)){
                return false;
            }
        }
        return true;
    }

    private BufferedImage getImage(int rgb){
        int r=rgb>>>16&0xff;
        int g=rgb>>>8&0xff;
        int b=rgb&0xff;
        int a=rgb>>>24&0xff;

        Map<Double,BufferedImage> pointedImages=new HashMap<>();
        Random random=new Random();

        for(BufferedImage asset:assets){
            Color c=ImageUtil.getColor(asset);

            double point=0;

            point=(0xff-Math.abs(r-c.getRed()));
            point+=(0xff-Math.abs(g-c.getGreen()));
            point+=(0xff-Math.abs(b-c.getBlue()));

            if(hasAlpha){
                point+=(0xff-Math.abs(a-c.getAlpha()));
            }

            if(pointedImages.containsKey(point)){
                if(random.nextBoolean()){
                    pointedImages.put(point,asset);
                }
            }else{
                pointedImages.put(point,asset);
            }
        }

        Object[] keys=pointedImages.keySet().toArray();
        Arrays.sort(keys);

        return pointedImages.get(keys[keys.length-1]);
    }

    private Color getColor(int... argbs){
        int a,r,g,b;
        a=r=g=b=0;
        for(int i=0;i<argbs.length;i++){
            a+=argbs[i]>>>24&0xff;
            r+=argbs[i]>>>16&0xff;
            g+=argbs[i]>>>8&0xff;
            b+=argbs[i]&0xff;
        }
        a/=argbs.length;
        r/=argbs.length;
        g/=argbs.length;
        b/=argbs.length;

        int argb=(int)((a<<24)+(r<<16)+(g<<8)+b);

        return new Color(argb);
    }


}
