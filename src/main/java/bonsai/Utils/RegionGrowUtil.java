package bonsai.Utils;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.*;

/**
 * 广度优先实现区域生长算法
 *
 * @author 徐文祥
 */
public class RegionGrowUtil implements BaseSegment {

    private static final Logger LOG = LoggerFactory.getLogger(RegionGrowUtil.class);

    static {
        // 接口的静态方法调用： 接口名.方法名
        BaseSegment.loadLib();
    }

    @Override
    public String getMaskPath(String imgUrl, Object... args) throws Exception {
        int x, y, width, height, thresh;// 算法需要的参数
        // 解析算法参数
        x = (int) args[0];
        y = (int) args[1];
        width = (int) args[2];
        height = (int) args[3];
        thresh = (int) args[4];

        // 获取原始图片路径
        imgUrl = CommonUtils.getOriginalImagePath(imgUrl);
        return getMaskPathInternal(imgUrl, x, y, width, height, thresh);
    }

    private static String getMaskPathInternal(String imgUrl, int x, int y, int width, int height, int thresh) throws Exception {

        // int thresh = 1;// 可调整
        try {
            Mat src = imread(imgUrl);// 读取原始图片
            if (src.empty()) {
                throw new Exception("image is empty");
            }

            // 新建一个矩形框
            Rect rectangle = new Rect(x, y, width, height);
            Mat tmp = new Mat(src, rectangle);
            cvtColor(tmp, tmp, COLOR_BGR2GRAY);// 转灰度图
            GaussianBlur(tmp, tmp, new Size(9, 9), 1);// 高斯平滑
            threshold(tmp, tmp, 0, 255, THRESH_BINARY + THRESH_OTSU);// 转0-1灰度图，自适应阈值调节

            Mat mask = new Mat(tmp.size(), CvType.CV_8UC1, new Scalar(0));// mask，全黑

            Queue<Point> seedQueue = new LinkedList<>();
            seedQueue.add(new Point(width / 2, height / 2));// 初始点，从中心扩张
            // 广度优先遍历
            Point[] connects = selectConnects();
            while (seedQueue.size() > 0) {
                Point curPoint = seedQueue.poll();// 取第一个元素
                // 标记当前元素被访问
                mask.row(curPoint.x).col(curPoint.y).setTo(new Scalar(255));

                for (int i = 0; i < 8; i++) {
                    // 邻接点
                    int tmpX = curPoint.x + connects[i].x;
                    int tmpY = curPoint.y + connects[i].y;
                    Point tmpPt = new Point(tmpX, tmpY);

                    // 超出边界的不处理
                    if (tmpX < 0 || tmpY < 0 || tmpX >= height || tmpY >= width)
                        continue;

                    int grayDiff = getGrayDiff(tmp, curPoint, tmpPt);// 计算灰度差
                    // 如果灰度差小于阈值并且该点没访问过
                    if (grayDiff < thresh && mask.get(tmpX, tmpY)[0] == 0) {
                        // 标记当前元素被访问
                        mask.row(tmpX).col(tmpY).setTo(new Scalar(255));
                        seedQueue.offer(tmpPt);// 入队
                    }
                }
            }

            // findContours 找边界
            // 灰度化
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            // 找轮廓
            Imgproc.findContours(mask, contours, hierarchy, RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

            List<String> res = CommonUtils.fromContoursGetPath(contours, x, y);
//            LOG.info("路径总条数: " + res.size());
            return res.toString();
        } catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e.toString() + " " + CommonUtils.getStackTraceString(e));
            throw e;
        }
    }

    private static int getGrayDiff(Mat img, Point p1, Point p2) {
        return (int) Math.abs(img.get(p1.x, p1.y)[0] - img.get(p2.x, p2.y)[0]);
    }

    private static Point[] selectConnects() {
        // 8连接
        Point[] connects = {
                new Point(-1, -1),
                new Point(-1, 0),
                new Point(-1, 1),
                new Point(1, -1),
                new Point(1, 0),
                new Point(1, 1),
                new Point(0, -1),
                new Point(0, 1),
        };
        return connects;
    }

    private static class Point {
        private int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
