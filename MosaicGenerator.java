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
    List<BufferedImage> assets;
    boolean hasAlpha;
    boolean keepAspect;
    int columns;
    int rows;
    double scale;

    public MosaicGenerator(){
        this(new ArrayList<>(),false,false,50,50,2);
    }

    public MosaicGenerator(List<BufferedImage> assets,boolean hasAlpha,boolean keepAspect,int columns,int rows,double scale){
        this.assets=new ArrayList<>(assets);
        this.hasAlpha=hasAlpha;
        this.keepAspect=keepAspect;
        this.columns=columns;
        this.rows=rows;
        this.scale=scale;
    }

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

    public int getColumns(){
        return columns;
    }

    public void setRows(int rows){
        this.rows=rows;
    }

    public int getRows(){
        return rows;
    }

    public void setScale(double scale){
        this.scale=scale;
    }

    public double getScale(){
        return scale;
    }

    public void setHasAlpha(boolean hasAlpha){
        this.hasAlpha=hasAlpha;
    }

    public void setKeepAspect(boolean keepAspect){
        this.keepAspect=keepAspect;
    }

    public double progress;
    public void generate(File originalFile,File resultFile)throws Exception{
        Thread thread=new Thread(){
            public void run(){
                progress=0;
                try{
                    BufferedImage original=ImageIO.read(originalFile);
            
                    final double ORIGINAL_WIDTH=original.getWidth();
                    final double ORIGINAL_HEIGHT=original.getHeight();
            
                    final double ORIGINAL_ATOM_WIDTH=ORIGINAL_WIDTH/columns;
                    final double ORIGINAL_ATOM_HEIGHT=ORIGINAL_HEIGHT/rows;
            
                    final double RESULT_WIDTH=ORIGINAL_WIDTH*scale;
                    final double RESULT_HEIGHT=ORIGINAL_HEIGHT*scale;
            
                    final double RESULT_ATOM_WIDTH=RESULT_WIDTH/columns;
                    final double RESULT_ATOM_HEIGHT=RESULT_HEIGHT/rows;
            
                    BufferedImage result=new BufferedImage((int)Math.round(RESULT_WIDTH),(int)Math.round(RESULT_HEIGHT),hasAlpha?BufferedImage.TYPE_4BYTE_ABGR:BufferedImage.TYPE_3BYTE_BGR);
                    Graphics2D g=result.createGraphics();
            
                    for(int c=0;c<columns;c++){
                        final int ORIGINAL_X_START=(int)Math.round(ORIGINAL_ATOM_WIDTH*c);
                        final int ORIGINAL_X_END=(int)Math.round(ORIGINAL_ATOM_WIDTH*(c+1));
                        final int RESULT_X=(int)Math.round(RESULT_ATOM_WIDTH*c);
                        for(int r=0;r<rows;r++){
                            final int ORIGINAL_Y_START=(int)Math.round(ORIGINAL_ATOM_HEIGHT*r);
                            final int ORIGINAL_Y_END=(int)Math.round(ORIGINAL_ATOM_HEIGHT*(r+1));
                            final int RESULT_Y=(int)Math.round(RESULT_ATOM_HEIGHT*r);
            
                            List<Integer> colors=new ArrayList<>();
                            for(int x=ORIGINAL_X_START;x<ORIGINAL_X_END;x++){
                                for(int y=ORIGINAL_Y_START;y<ORIGINAL_Y_END;y++){
                                    colors.add(original.getRGB(x,y));
                                }
                            }
            
                            BufferedImage atom=ImageUtil.resize(getImage(getColor(colors).getRGB()),(int)Math.round(RESULT_ATOM_WIDTH),(int)Math.round(RESULT_ATOM_HEIGHT),keepAspect);
                            if(!hasAlpha){
                                ImageUtil.removeAlpha(atom);
                            }
                            g.drawImage(atom,RESULT_X,RESULT_Y,null);
                        }
                        progress=99.9*((c+1d)/columns);
                        System.out.println(String.format("%02.2f",99*((c+1d)/columns))+"% generated");
                    }
                    String resultFileName=resultFile.getName();
                    String formatName=resultFileName.substring(resultFileName.lastIndexOf(".")+1);
            
                    ImageIO.write(result,formatName,resultFile);
                    progress=100;
                }catch(Exception e){
                    e.printStackTrace();
                    progress=-1;
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
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

    private Color getColor(List<Integer> argbs){
        int a,r,g,b;
        a=r=g=b=0;
        final int LENGTH=argbs.size();
        for(int i=0;i<LENGTH;i++){
            a+=argbs.get(i)>>>24&0xff;
            r+=argbs.get(i)>>>16&0xff;
            g+=argbs.get(i)>>>8&0xff;
            b+=argbs.get(i)&0xff;
        }
        a/=LENGTH;
        r/=LENGTH;
        g/=LENGTH;
        b/=LENGTH;

        int argb=(int)((a<<24)+(r<<16)+(g<<8)+b);

        return new Color(argb);
    }
}
