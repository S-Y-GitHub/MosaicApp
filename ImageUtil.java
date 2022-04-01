import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class ImageUtil{
    public static Color getColor(BufferedImage image){
        final long WIDTH=image.getWidth();
        final long HEIGHT=image.getHeight();
        final long PIXELS=WIDTH*HEIGHT;

        long a,r,g,b;
        a=r=g=b=0;
        
        for(int x=0;x<WIDTH;x++)for(int y=0;y<HEIGHT;y++){
            int rgb=image.getRGB(x,y);
            a+=rgb>>>24&0xff;
            r+=rgb>>>16&0xff;
            g+=rgb>>>8&0xff;
            b+=rgb&0xff;
        }

        a/=PIXELS;
        r/=PIXELS;
        g/=PIXELS;
        b/=PIXELS;

        int argb=(int)((a<<24)+(r<<16)+(g<<8)+b);

        return new Color(argb);
    }

    public static BufferedImage resize(BufferedImage image,int width,int height,boolean keepAspect){
        BufferedImage result=new BufferedImage(width,height,image.getType());
        Graphics2D g=result.createGraphics();
        if(keepAspect){
            g.setColor(getColor(image));
            g.fillRect(0,0,width,height);

            double imageWidth=image.getWidth();
            double imageHeight=image.getHeight();

            double widthRatio=width/imageWidth;
            double heightRatio=height/imageHeight;

            if(widthRatio<heightRatio){
                double scaledImageHeight=imageHeight*widthRatio;
                int y=(int)((height-scaledImageHeight)/2);
                g.drawImage(image.getScaledInstance(width,(int)scaledImageHeight,Image.SCALE_SMOOTH),0,y,null);
            }else if(widthRatio>heightRatio){
                double scaledImageWidth=imageWidth*heightRatio;
                int x=(int)((width-scaledImageWidth)/2);
                g.drawImage(image.getScaledInstance((int)scaledImageWidth,height,Image.SCALE_SMOOTH),x,0,null);
            }else{
                g.drawImage(image.getScaledInstance(width,height,Image.SCALE_SMOOTH),0,0,null);
            }
        }else{
            g.drawImage(image.getScaledInstance(width,height,Image.SCALE_SMOOTH),0,0,width,height,null);
        }
        return result;
    }

    public static double getRatio(BufferedImage image){
        return (double)image.getWidth()/image.getHeight();
    }

    public static void removeAlpha(BufferedImage image){
        for(int x=0;x<image.getWidth();x++)for(int y=0;y<image.getHeight();y++){
            image.setRGB(x,y,image.getRGB(x,y)|0xff000000);
        }
    }
}