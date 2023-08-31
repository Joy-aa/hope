package bonsai.Utils;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
public class AlphaUtil implements BaseSegment{

    private static final Logger LOG = LoggerFactory.getLogger(AlphaUtil.class);

    static {
        // 接口的静态方法调用： 接口名.方法名
        BaseSegment.loadLib();
    }

    @Override
    public String getMaskPath(String imgUrl, Object... args) throws Exception {
        String dstUrl = (String)args[0];

        // 获取原始图片路径
//        imgUrl = CommonUtils.getOriginalImagePath(imgUrl);
//        LOG.info("OriginalImagePath: " + imgUrl);
//        LOG.info("DstImagePath: " + dstUrl);


        return getMaskPathInternal(imgUrl, dstUrl);
    }

    private static String getMaskPathInternal(String imgUrl, String dstUrl) throws Exception {
        try{
//            LOG.info("OriginalImagePath: " + imgUrl);
//            LOG.info("DstImagePath: " + dstUrl);
            Mat img = imread(imgUrl, -1);// 读取原始图片
            if (img.empty()) {
                throw new Exception("image is empty");
            }
            if (img.channels() != 4)
            {
                throw new Exception("image is not 4-channels");
            }
            byte[] data = new byte[(img.cols() * img.rows() * img.channels())];
            img.get(0, 0, data);
            for (int i = 0; i < img.rows(); i++)
            {
                for (int j = 0; j < img.cols(); j++)
                {
                    if(data[i*img.cols() + j*4] == 255 && data[i*img.cols() + j*4 + 1] == 255 &&
                            data[i*img.cols() + j*4 + 2] == 255 && data[i*img.cols() + j*4 + 3] == 0){
                        data[i*img.cols() + j] = 0;
                        data[i*img.cols() + j+1] = 0;
                        data[i*img.cols() + j+2] = 0;
                        data[i*img.cols() + j+3] = 1;
                    }

                }
            }
            img.put(0, 0, data);
            imwrite(dstUrl, img);
            LOG.info("saveUrl:",dstUrl);
            return "success";
        }
        catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e.toString() + " " + CommonUtils.getStackTraceString(e));
            throw e;
        }
    }


}
