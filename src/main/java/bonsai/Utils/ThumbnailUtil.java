package bonsai.Utils;

import dataturks.DConstants;
import net.coobird.thumbnailator.Thumbnails;
import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.ws.rs.WebApplicationException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取缩略图工具类
 *
 * @author 徐文祥
 */
public class ThumbnailUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ThumbnailUtil.class);

    // 获取缩略图对应的原图路径
    public static String getOriginalImgUrl(String thumbnailUrl) {
        // 缩略图后缀是 .thumbnail.jpg
        int lastIndexOf = thumbnailUrl.lastIndexOf(".thumbnail.jpg");
        return thumbnailUrl.substring(0, lastIndexOf);
    }

    // 生成缩略图路径
    public static Path getThumbnailFilePath(Path folderPath, String fileName) {
        return folderPath.resolve(fileName + ".thumbnail.jpg");// 缩略图文件名
    }

    // 生成缩略图
    public static void copyThumbnail(Path originPath, Path thumbnailFilePath) {
        File originImg = new File(originPath.toString());
        long imgSize = originImg.length();// 图片大小
        try {
            // 如果小于阈值，直接拷贝一份，这样做只是为了简化操作，不破坏现有的逻辑（浮点数不能用等号比较）
            if (imgSize < DConstants.MAX_IMAGE_FILE_SIZE) {
                Files.copy(originPath, thumbnailFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
            // 大于阈值，则压缩
            else {
                // 图片尺寸不变，压缩图片文件大小outputQuality实现,参数1为最高质量
                Thumbnails.of(originPath.toString()).scale(1f).outputQuality(0.2f).toFile(thumbnailFilePath.toString());
            }
        } catch (IOException e) {
            throw new WebApplicationException("Thumbnail  generation failed, either the disk is full or some other error occured.");
        }
    }

    // 生成图片对应的 base64 字符串
    public static String getImgBase64Str(String imgUrl, int x, int y, int width, int height) throws IOException {

        InputStream source = null;// 输入流
        ImageInputStream iis = null;// 图片流
        ByteArrayOutputStream outputStream = null;// 输出流

        try {
            //获取最后一个.的位置
            int lastIndexOf = imgUrl.lastIndexOf(".");
            //获取文件的后缀名
            String format = imgUrl.substring(lastIndexOf + 1);

            // 取得图片读入器
            Iterator readers = ImageIO.getImageReadersByFormatName(format);
            ImageReader reader = (ImageReader) readers.next();
            // 获取原始图片路径
            // imgUrl = CommonUtils.getOriginalImagePath(imgUrl);
            // 获取图片流
            source = new FileInputStream(imgUrl);
            iis = ImageIO.createImageInputStream(source);
            reader.setInput(iis, true);
            // 图片参数
            ImageReadParam param = reader.getDefaultReadParam();
            // x, y是图片左上角坐标，后两个参数是裁剪后图片的宽高
            Rectangle rect = new Rectangle(x, y, width, height);
            param.setSourceRegion(rect);
            BufferedImage bi = reader.read(0, param);// 获取裁剪后的小图
            //输出流
            outputStream = new ByteArrayOutputStream();
            String outFormatName = getFormatName(format);// 设置 outputStream 图片格式
            ImageIO.write(bi, outFormatName, outputStream);

            // 不再使用 sun 公司 BASE64Encoder 这个API，使用 util 包的 API
            Base64.Encoder encoder = Base64.getEncoder();

            return encoder.encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e.toString() + " " + CommonUtils.getStackTraceString(e));
            throw e;
        } finally {
            //关流
            source.close();
            iis.close();
            outputStream.close();
        }
    }

    //把base64解码为图像
    public static byte[] decode(String base64Str,String fileName,String filePath){
        File file = null;
        //创建文件目录
        File  dir=new File(filePath);
        if (!dir.exists() && !dir.isDirectory()) {
            dir.mkdirs();
        }
        BufferedOutputStream bos = null;
        java.io.FileOutputStream fos = null;

        byte[] b = null;
//        BASE64Decoder decoder = new BASE64Decoder();
        try {
            b = Base64.getMimeDecoder().decode(base64Str);
            //window
            //file=new File(filePath+"\\"+fileName);
            //linux
            file=new File(filePath+"/"+fileName);
            fos = new java.io.FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(b);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return b;
    }

    public static String replaceEnter(String str){
        String reg ="[\n-\r]";
        Pattern p = Pattern.compile(reg);
        Matcher m = p.matcher(str);
        return m.replaceAll("");
    }

    // 根据原始文件类型返回图片格式
    public static String getFormatName(String format) {
        if (format.equalsIgnoreCase("png")) {
            return "png";
        }
        if (format.equalsIgnoreCase("jpeg")) {
            return "jpeg";
        }

        // 默认返回 jpg 格式
        return "jpg";
    }

    public static String getMrxsBase64Str(String imgPath, String mrxsPath, int x, int y, int width, int height) throws IOException {

        ByteArrayOutputStream outputStream = null;

        try {
            int hThumb, wThumb, xOut, yOut, wOut, hOut, hMrxs, wMrxs;
            BufferedImage image = ImageIO.read(new File(imgPath));
            File file = new File(mrxsPath);
            OpenSlide os = new OpenSlide(file);
            hMrxs = (int) (os.getLevel0Height());  //强制类型转换 long->int
            wMrxs = (int) (os.getLevel0Width());
            hThumb = image.getHeight();
            wThumb = image.getWidth();
            xOut = (int) (1.0 * x / hThumb * hMrxs);
            yOut = (int) (1.0 * y / wThumb * wMrxs);
            wOut = (int) (1.0 * width / wThumb * wMrxs);
            hOut = (int) (1.0 * height / hThumb * hMrxs);
            BufferedImage img = os.createThumbnailImage(xOut, yOut, wOut, hOut, 3000, BufferedImage.TYPE_INT_ARGB);
            BufferedImage newBufferedImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            newBufferedImage.createGraphics().drawImage(img, 0, 0, Color.WHITE, null);
            outputStream = new ByteArrayOutputStream();
            ImageIO.write(newBufferedImage, "jpg", outputStream);

            Base64.Encoder encoder = Base64.getEncoder();
            os.close();
            return encoder.encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            LOG.error("Error " + e.toString() + " " + CommonUtils.getStackTraceString(e));
            throw e;
        } finally {
            outputStream.close();
        }
    }
}
