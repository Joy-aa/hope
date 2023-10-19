package bonsai.Utils;


import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.*;


/**
 * 阈值分割算法
 *
 * @author 徐文祥
 */
public class OTSUUtil implements BaseSegment {
    private static final Logger LOG = LoggerFactory.getLogger(ThresholdUtil.class);

    static {
        // 接口的静态方法调用： 接口名.方法名
        BaseSegment.loadLib();
    }

    @Override
    public String getMaskPath(String imgUrl, Object... args) throws Exception {
        int x, y, width, height, thresh, maxVal;// 算法需要的参数
        // 解析算法参数
        x = (int) args[0];
        y = (int) args[1];
        width = (int) args[2];
        height = (int) args[3];
//        thresh = (int) args[4];
//        maxVal = (int) args[5];
        // 获取原始图片路径
        imgUrl = CommonUtils.getOriginalImagePath(imgUrl);
        return getMaskPathInternal(imgUrl, x, y, width, height);
    }

    private static String getMaskPathInternal(String imgUrl, int x, int y, int width, int height) throws Exception {

        try {
            Mat srcImg = imread(imgUrl, 0);// 读取原始图片
            if (srcImg.empty()) {
                throw new Exception("image is empty");
            }

            // 新建一个矩形框
            Rect rectangle = new Rect(x, y, width, height);
            Mat tmp = new Mat(srcImg, rectangle);

            Mat mask = new Mat();
            threshold(tmp, mask, 0, 255, THRESH_BINARY_INV+THRESH_OTSU);// 基于阈值对图像的局部进行分割
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            // 找轮廓
            Imgproc.findContours(mask, contours, hierarchy, RETR_CCOMP, CHAIN_APPROX_SIMPLE);

            List<String> res = CommonUtils.fromContoursGetPath(contours, x, y);
            LOG.info("路径总条数: " + res.size());
            return res.toString();
        } catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e + " " + CommonUtils.getStackTraceString(e));
            throw e;
        }
    }
}

