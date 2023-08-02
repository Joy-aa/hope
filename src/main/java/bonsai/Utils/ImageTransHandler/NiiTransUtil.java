package bonsai.Utils.ImageTransHandler;

import bonsai.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NiiTransUtil {
    private static final Logger LOG = LoggerFactory.getLogger(NiiTransUtil.class);

    public static List<String> trans(String sourcePath, String destPath){
        try{
            Process process = Runtime.getRuntime().exec("python3 " + Constants.TRANS_PY_LOCATE + " " + sourcePath + " " + destPath); //修改调用python文件方式
            int result = process.waitFor();
            if(result != 0) {          // result非0代表trans函数异常;
                LOG.error("Unable to Transform the medical image");
            }
            List<String> niiLists = new ArrayList<>();
            String[] parts = sourcePath.split("/");
            niiLists.add(destPath + "/" + parts[parts.length - 1].split("\\.")[0] + "-1.png");
            niiLists.add(destPath + "/" + parts[parts.length - 1].split("\\.")[0] + "-2.png");
            niiLists.add(destPath + "/" + parts[parts.length - 1].split("\\.")[0] + "-3.png");   //生成不同方向三个切片并保存
            return niiLists;
        }catch (Exception e){
            return null;
        }
    }
}
