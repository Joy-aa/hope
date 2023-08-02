package bonsai;

/**
 * Created by mohan.gupta on 11/04/17.
 */
public class Constants {

    //distance based.
    public static final int INFINITE_DISTANCE_MTRS = 999999;

    //Data turks annotations
    //all files uploaded on the system.

    // zip文件暂存目录。但是 tmp 文件夹非root用户无法访问
    public static final String DEFAULT_FILE_UPLOAD_DIR = "/nfs/DamDetection/apps/hope/tmp";
//    public static final String DEFAULT_FILE_UPLOAD_DIR = "/new_disk_1/xwx/apps/vipa-dataturks/tmp";// zip文件暂存目录
//    public static final String DEFAULT_FILE_UPLOAD_DIR = "E:\\Github\\dataturks-vipa\\client\\tmp";


    public static final long MAX_FILE_UPLOAD_SIZE = 1024 * 1024 * 1024 * 20L; //20 GB.
    public static final int MAX_STRING_LENGTH_FOR_TEXT_TASK = 1000;
    public static final int MAX_NUM_HITS_PER_PROJECT = 10000;
    public static final int NUM_LABELS_ALLOWED = 20000;

    public static final String DEFAULT_FILE_DOWNLOAD_DIR = DEFAULT_FILE_UPLOAD_DIR;// 下载数据时数据的暂存目录
    public static final String DEFAULT_FILE_STORAGE_DIR = "/nfs/DamDetection/apps/client/build/uploads";// 图片存储目录
    public static final String DEFAULT_LABEL_STORAGE_DIR = "/nfs/DamDetection/apps/labels";// 参考标注存储目录

    //图像转换py代码文件位置（这个地址耦合性太高了）
    public static final String TRANS_PY_LOCATE = "/nfs/DamDetection/apps/hope/src/main/java/bonsai/Utils/ImageTransHandler/pyimage.py";
    //    public static final String TRANS_PY_LOCATE = "/new_disk_1/xwx/dataturks-vipa/hope/src/main/java/bonsai/Utils/ImageTransHandler/pyimage.py";// 实验室
//    public static final String TRANS_PY_LOCATE = "E:\\Github\\dataturks-vipa\\hope\\src\\main\\java\\bonsai\\Utils\\ImageTransHandler\\pyimage.py";// 阿里云

    // opencv 库文件位置
    public static final String OPENCV_LIB = "/nfs/DamDetection/apps/hope/src/main/resources/lib/opencv/libopencv_java451.so";
//    public static final String OPENCV_LIB = "E:\\Github\\dataturks-vipa\\hope\\src\\main\\resources\\lib\\opencv\\opencv_java451.dll";

    /**
     * The constant INSTAMOJO_TEST_AUTH_ENDPOINT.
     */
    public static final String INSTAMOJO_TEST_AUTH_ENDPOINT = "https://test.instamojo.com/oauth2/token/";
    /**
     * The constant TEST_CLIENT_ID.
     */
    public static final String TEST_CLIENT_ID = "Vq0zV7oiSq2s5duSqKAUaq4n27Wyi7fhEVAtyiX5";
    /**
     * The constant TEST_CLIENT_SECRET.
     */
    public static final String TEST_CLIENT_SECRET = "0M9XOHXovYAwb9Ml4u8ph8Jz3vSwKinXVKKohaQ2bvgEK2b73oUVqkmKEhi5AJO30N4YyD25Q0yHOQL7XKDVFEBlgkZhoT2LgEej3q5w3tXMC2In4LyLiQKvKAYiG1S6";

    public final static String PROD_CLIENT_ID = "rP757gWRUy0K5dLRp80t6VjMz1DzXOOlCAZ9w0mZ";
    public final static String PROD_CLIENT_SECRET = "f2LkKjH6SDy319lv1cO5cIUGmraKu5uK97HPxDOjDvlzMAgBAoqJx20jPcmrUAhiO5W2AXo0mfpCVw8RY0fWFXQJr2xlm5hSqY1KDFvct5yl9QV3ac3hj0AB4qgtOpgW";
    /**
     * The constant INSTAMOJO_TEST_AUTH_ENDPOINT.
     */
    public static final String INSTAMOJO_PROD_AUTH_ENDPOINT = "https://api.instamojo.com/oauth2/token/";
}
