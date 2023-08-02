package bonsai.Utils;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.*;

/**
 * 区域分裂与合并算法
 *
 * @author 徐文祥
 */
public class RegionSplitAndMergeUtil implements BaseSegment {

    private static final Logger LOG = LoggerFactory.getLogger(RegionSplitAndMergeUtil.class);

    static {
        // 接口的静态方法调用： 接口名.方法名
        BaseSegment.loadLib();
    }

    @Override
    public String getMaskPath(String imgUrl, Object... args) throws Exception {
        int x, y, width, height;// 算法需要的参数
        // 解析算法参数
        x = (int) args[0];
        y = (int) args[1];
        width = (int) args[2];
        height = (int) args[3];

        // 获取原始图片路径
        imgUrl = CommonUtils.getOriginalImagePath(imgUrl);

        return getMaskPathInternal(imgUrl, x, y, width, height);
    }

    private static String getMaskPathInternal(String imgUrl, int x, int y, int width, int height) throws Exception {
        try {
            Mat src = imread(imgUrl);// 读取原始图片
            if (src.empty()) {
                throw new Exception("image is empty");
            }

            // 新建一个矩形框
            Rect rectangle = new Rect(x, y, width, height);
            Mat tmp = new Mat(src, rectangle);
            Mat gray = new Mat();
            cvtColor(tmp, gray, COLOR_BGR2GRAY);// 转灰度图
            GaussianBlur(gray, gray, new Size(7, 7), 1);
            Mat mask = gray;
            // 区域分裂与合并
            split(mask, 0, 0, width, height);

            // findContours 找边界
            // 灰度化
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            // 找轮廓
            Imgproc.findContours(mask, contours, hierarchy, RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

            List<String> res = CommonUtils.fromContoursGetPath(contours, x, y);
            LOG.info("路径总条数: " + res.size());
            return res.toString();
        } catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e.toString() + " " + CommonUtils.getStackTraceString(e));
            throw e;
        }
    }

    // 分裂
    private static void split(Mat gray, int w0, int h0, int width, int height) {
        if (judge(gray, w0, h0, width, height) && Math.min(width, height) > 3) {
            split(gray, w0, h0, width / 2, height / 2);
            // 注意下面不要少像素
            split(gray, w0 + width / 2, h0, width - width / 2, height / 2);
            split(gray, w0, h0 + height / 2, width / 2, height - height / 2);
            split(gray, w0 + width / 2, h0 + height / 2, width - width / 2, height - height / 2);
        } else {
            fillColor(gray, w0, h0, width, height);
        }
    }

    /**
     * 判断是否要分裂
     *
     * @param gray   灰度图
     * @param w0     图像左上角坐标x
     * @param h0     图像左上角坐标y
     * @param width  图像宽度
     * @param height 图像宽度
     */
    private static boolean judge(Mat gray, int w0, int h0, int width, int height) {
        // 选取部分图像
        Rect rectangle = new Rect(w0, h0, width, height);
        Mat tmp = new Mat(gray, rectangle);
        //System.out.println("judge-> tmpMat is "+tmp.dump());

        // 计算灰度的均值和方差
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();
        Core.meanStdDev(tmp, mean, std);
        double mu = mean.get(0, 0)[0];
        double sigma = std.get(0, 0)[0];

        int total = width * height;
        int cnt = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (Math.abs(tmp.get(i, j)[0] - mu) < sigma) {
                    cnt++;
                }
            }
        }
        // 如果小于2*sigma，说明该区域还需要分裂
        return cnt * 1.0 / total < 0.95;
    }

    // 填充颜色
    private static void fillColor(Mat gray, int w0, int h0, int width, int height) {
        for (int i = h0; i < h0 + height; i++) {
            for (int j = w0; j < w0 + width; j++) {
                if (gray.get(i, j)[0] > 210) {
                    gray.row(i).col(j).setTo(new Scalar(255));
                } else {
                    gray.row(i).col(j).setTo(new Scalar(0));
                }
            }
        }
    }

    private static boolean isEven(int num) {
        return num % 2 == 0;
    }
}
