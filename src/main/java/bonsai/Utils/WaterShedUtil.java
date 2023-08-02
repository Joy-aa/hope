package bonsai.Utils;

import org.opencv.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.Core.NORM_MINMAX;
import static org.opencv.core.Core.normalize;
import static org.opencv.core.CvType.CV_32SC1;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.*;

/**
 * 分水岭算法
 *
 * @author 徐文祥
 */
public class WaterShedUtil implements BaseSegment {

    private static final Logger LOG = LoggerFactory.getLogger(WaterShedUtil.class);

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

    public static String getMaskPathInternal(String imgUrl, int x, int y, int width, int height) throws Exception {
        try {
            Mat img = imread(imgUrl);// 读取原始图片
            if (img.empty()) {
                throw new Exception("image is empty");
            }

            // 新建一个矩形框
            Rect rectangle = new Rect(x, y, width, height);
            img = new Mat(img, rectangle);// 图片裁剪

            Mat gray = new Mat();// 灰度图
            cvtColor(img, gray, COLOR_BGR2GRAY);// 灰度化

            // 阈值处理，高于 128 的会变成 白色
            threshold(gray, gray, 128, 254, THRESH_BINARY + THRESH_OTSU);

            // 降噪
            Mat kernel = getStructuringElement(MORPH_RECT, new Size(3, 3));// 创建一个kernel
            morphologyEx(gray, gray, MORPH_OPEN, kernel);

            Mat fg = new Mat(img.size(), CvType.CV_8U);
            erode(gray, fg, new Mat(), new Point(-1, -1), 3);// 腐蚀

            Mat bg = new Mat(img.size(), CvType.CV_8U);
            dilate(gray, bg, new Mat(), new Point(-1, -1), 3);// 膨胀
            threshold(bg, bg, 128, 1, THRESH_BINARY_INV);

            Mat mask = new Mat(img.size(), CvType.CV_8U, new Scalar(0));// mask
            Core.add(fg, bg, mask);// 叠加

            // 分水岭算法
            mask.convertTo(mask, CV_32SC1);
            watershed(img, mask);
            mask.convertTo(mask, CvType.CV_8U);

            // imwrite("E:\\Desktop\\res\\tmp.jpg", mask);

            // findContours 找边界
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            /*
             * RETR_CCOMP: 检索所有轮廓并将它们组织成两级层次结构。在顶层，有组件的外部边界。在第二层，有洞的边界。如果连接组件的孔内有另一个轮廓，它仍然放在顶层。
             *
             * CHAIN_APPROX_NONE: 绝对存储所有轮廓
             * CHAIN_APPROX_SIMPLE: 压缩水平、垂直和对角线段，只留下它们的端点。
             */
            findContours(mask, contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);

            List<String> res = CommonUtils.fromContoursGetPath(contours, x, y);
            if (res.size() > 0) {
                res.remove(0);// 移除组件的外部边界
            }
            LOG.info("路径总条数: " + res.size());
            return res.toString();
        } catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e + " " + CommonUtils.getStackTraceString(e));
            throw e;
        }
    }

    public static String getMaskPathInternal2(String imgUrl, int x, int y, int width, int height) throws Exception {
        try {
            Mat img = imread(imgUrl);// 读取原始图片
            if (img.empty()) {
                throw new Exception("image is empty");
            }

            // 新建一个矩形框
            Rect rectangle = new Rect(x, y, width, height);
            img = new Mat(img, rectangle);// 图片裁剪

            Mat grayImage = new Mat();// 灰度图
            cvtColor(img, grayImage, COLOR_BGR2GRAY);// 灰度化

            threshold(grayImage, grayImage, 0, 255, THRESH_BINARY + THRESH_OTSU);// 阈值处理

            // 降噪
            Mat kernel = getStructuringElement(MORPH_RECT, new Size(3, 3), new Point(-1, -1));// 创建一个kernel
            morphologyEx(grayImage, grayImage, MORPH_CLOSE, kernel);
            distanceTransform(grayImage, grayImage, DIST_L2, DIST_MASK_3, 5);
            normalize(grayImage, grayImage, 0, 1, NORM_MINMAX);
            grayImage.convertTo(grayImage, CV_8UC1);
            threshold(grayImage, grayImage, 0, 255, THRESH_BINARY | THRESH_OTSU);
            morphologyEx(grayImage, grayImage, MORPH_CLOSE, kernel);

            // findContours 找边界
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Mat mask = new Mat(grayImage.size(), CV_32SC1);
            /*
             * RETR_CCOMP: 检索所有轮廓并将它们组织成两级层次结构。在顶层，有组件的外部边界。在第二层，有洞的边界。如果连接组件的孔内有另一个轮廓，它仍然放在顶层。
             *
             * CHAIN_APPROX_NONE: 绝对存储所有轮廓
             * CHAIN_APPROX_SIMPLE: 压缩水平、垂直和对角线段，只留下它们的端点。
             */
            findContours(grayImage, contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE, new Point(-1, -1));
            for (int i = 0; i < contours.size(); i++) {
                // 标记分水岭
                drawContours(mask, contours, i, new Scalar(i + 1), -1, 8, hierarchy);
            }

            watershed(img, mask);

            contours = new ArrayList<>();
            hierarchy = new Mat();

            findContours(mask, contours, hierarchy, RETR_FLOODFILL, CHAIN_APPROX_SIMPLE);

            // 后处理
            List<String> res = new ArrayList<>();
            for (MatOfPoint contour : contours) {
                // 需要加上偏移量
                Point[] points = contour.toArray();
                for (Point point : points) {
                    point.x += x;
                    point.y += y;
                }
                contour.fromArray(points);
                res.add(contour.toList().toString().replace("{", "[").replace("}", "]"));
            }
//            if (res.size() > 0) {
//                res.remove(0);// 移除组件的外部边界
//            }
            LOG.info("路径总条数: " + res.size());
            return res.toString();
        } catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e + " " + CommonUtils.getStackTraceString(e));
            throw e;
        }
    }
}
