package bonsai.Utils;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.*;

/**
 * Canny坎尼算子
 * @author 徐文祥
 */
public class CannyUtil implements BaseSegment{
    private static final Logger LOG = LoggerFactory.getLogger(CannyUtil.class);

    static {
        // 接口的静态方法调用： 接口名.方法名
        BaseSegment.loadLib();
    }

    @Override
    public String getMaskPath(String imgUrl, Object... args) throws Exception {
        int x, y, width, height, threshold1, threshold2;// 算法需要的参数
        // 解析算法参数
        x = (int) args[0];
        y = (int) args[1];
        width = (int) args[2];
        height = (int) args[3];
        threshold1 = (int) args[4];
        threshold2 = (int) args[5];
        // 获取原始图片路径
        imgUrl = CommonUtils.getOriginalImagePath(imgUrl);
        return getMaskPathInternal(imgUrl, x, y, width, height, threshold1, threshold2);
    }

    private static String getMaskPathInternal(String imgUrl, int x, int y, int width, int height, int threshold1, int threshold2) throws Exception {

        try {
            Mat srcImg = imread(imgUrl, 0);// 读取原始图片
            if (srcImg.empty()) {
                throw new Exception("image is empty");
            }

            // 新建一个矩形框
            Rect rectangle = new Rect(x, y, width, height);
            Mat tmp = new Mat(srcImg, rectangle);

            GaussianBlur(tmp, tmp, new Size(3, 3), 2);// 高斯滤波降噪
            Mat mask = new Mat();
            Canny(tmp, mask, threshold1, threshold2);// canny算子

            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            // 找轮廓，仅检索极端的外部轮廓
            findContours(mask, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

            List<String> res = CommonUtils.fromContoursGetPath(contours, x, y);
//            LOG.info("路径总条数: " + res.size());
            return res.toString();
        } catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e + " " + CommonUtils.getStackTraceString(e));
            throw e;
        }
    }
}
