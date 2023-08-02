package bonsai.Utils;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.grabCut;

/**
 * GrabCut算法
 *
 * @author 徐文祥
 */
public class GrabCutUtil implements BaseSegment {

    private static final Logger LOG = LoggerFactory.getLogger(GrabCutUtil.class);

    static {
        // 接口的静态方法调用： 接口名.方法名
        BaseSegment.loadLib();
    }

    @Override
    public String getMaskPath(String imgUrl, Object... args) throws Exception {
        int x, y, width, height, iterCount;// 算法需要的参数
        // 解析算法参数
        x = (int) args[0];
        y = (int) args[1];
        width = (int) args[2];
        height = (int) args[3];
        iterCount = (int) args[4];

        // 获取原始图片路径
        // LOG.info("imgUrl: " + imgUrl);
        imgUrl = CommonUtils.getOriginalImagePath(imgUrl);
        // LOG.info("OriginalImagePath: " + imgUrl);


        return getMaskPathInternal(imgUrl, x, y, width, height, iterCount);
    }

    // 使用 grabcut 分割图像，并获取分割图 mask 的路径
    public static String getMaskPathInternal(String imgUrl, int x, int y, int width, int height, int iterCount) throws Exception {
        try {
            Mat img = imread(imgUrl);// 读取原始图片
            if (img.empty()) {
                throw new Exception("image is empty");
            }

            // 新建一个矩形框
            Rect rectangle = new Rect(x, y, width, height);
            // 前景、背景、mask
            Mat mask = new Mat();
            Mat bgdModel = new Mat();
            Mat fgdModel = new Mat();
            Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(3));
            // 图像分割
            long start = System.currentTimeMillis();
            // grabCut(img, mask, rectangle, bgdModel, fgdModel, 3, Imgproc.GC_INIT_WITH_RECT);
            grabCut(img, mask, rectangle, bgdModel, fgdModel, iterCount, Imgproc.GC_INIT_WITH_RECT);
            long end = System.currentTimeMillis();
            LOG.info("分割图像花费时间是: " + (end - start) / 1000 + "s");
            // 通过比较把可能为前景的像素保存到 mask 中
            Core.compare(mask, source, mask, Core.CMP_EQ);
            // 设置前景色为黑色
            Mat foreground = new Mat(img.size(), CvType.CV_8UC3, new Scalar(0, 0, 0));
            // 将 mask 区域拷贝到 foreground 中
            img.copyTo(foreground, mask);

            // findContours 找边界
            // 灰度化
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            // 找轮廓
            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

            List<String> res = new ArrayList<>();
            for (MatOfPoint contour : contours) {
                res.add(contour.toList().toString().replace("{", "[").replace("}", "]"));
            }
            LOG.info("路径总条数: " + res.size());
            return res.toString();
        } catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e.toString() + " " + CommonUtils.getStackTraceString(e));
            throw e;
        }
    }
}
