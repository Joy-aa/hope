package bonsai.Utils.ImageTransHandler;

import bonsai.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageTransUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ImageTransUtil.class);

    public static String trans(String sourcePath, String destPath){
        try{
            Process process = Runtime.getRuntime().exec("python3 " + Constants.TRANS_PY_LOCATE + " " + sourcePath + " " + destPath); //修改python运行方式
            int result = process.waitFor();
            if(result != 0) {       // result非0代表trans函数异常;
                LOG.error("Unable to Transform the medical image");
            }
            //生成输出路径
            String[] parts = sourcePath.split("/");
            if(parts[parts.length - 1].split("\\.")[1].equals("mrxs"))
                return destPath + '/' + parts[parts.length - 1].split("\\.")[0] + ".mrxs.png";
            else
                return destPath + '/' + parts[parts.length - 1].split("\\.")[0] + ".png";
        }catch (Exception e){
            return null;
        }
    }
}