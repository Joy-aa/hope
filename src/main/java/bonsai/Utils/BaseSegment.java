package bonsai.Utils;

import bonsai.Constants;

/**
 * 分割算法的基础接口
 *
 * @author 徐文祥
 * @since 2021/10/18
 */
public interface BaseSegment {

    /**
     * 获取分割图 mask 的路径
     *
     * <p>返回的分割图 mask 的路径格式为json数组，具体如下：
     * <p>[
     * <p>[[point_1_x, point_1_y], ..., [point_n_x, point_n_y]],
     * <p>...,
     * <p>[[point_1_x, point_1_y], ..., [point_n_x, point_n_y]]
     * <p>]
     * <p>
     * <p> 其中 [[point_1_x, point_1_y], ..., [point_n_x, point_n_y]] 代表一条路径，[point_n_x, point_n_y] 代表一个点的坐标
     * <p><strong>如果算法的返回值实在不能满足上面的要求，可以和前端协商，由前端对算法返回值的格式进行单独处理</strong>
     *
     * @param imgUrl 图片url
     * @param args   算法需要的额外参数，需要和前端协商，后端需要自行接收处理
     * @return 分割图 mask 的路径
     * @throws Exception 相关异常
     * @author 徐文祥
     * @since 2021/10/18
     */
    String getMaskPath(String imgUrl, Object... args) throws Exception;

    // 加载库文件
    static void loadLib() {
        // 解决awt报错问题
        System.setProperty("java.awt.headless", "true");
        // 加载动态库
        System.load(Constants.OPENCV_LIB);
    }
}
