package dataturks;

public class DConstants {

    public final static long FREE_PLAN_ID = 1;


    public final static String HIT_STATUS_DONE = "done";
    public final static String HIT_STATUS_SELF_LEARNING = "sl";
    public final static String HIT_STATUS_ACTIVE_LEARNING = "al";
    public final static String HIT_STATUS_NOT_DONE = "notDone";
    public final static String HIT_STATUS_SKIPPED = "skipped";
    public final static String HIT_STATUS_DELETED = "deleted";
    public final static String HIT_STATUS_PRE_TAGGED = "preTagged";
    public final static String HIT_STATUS_REQUEUED = "reQueued";
    public final static String HIT_STATUS_ALL = null; //no filter

    public final static int MAX_HITS_TO_RETURN = 100;

    public final static String NON_LOGGED_IN_USER_ID = "123";

    //used to upload/download hits-->result pairs.
    public final static String TEXT_INPUT_RESULT_SEPARATOR = "\t";
    public final static String LABEL_SEPARATOR = "____";

    public final static String TRENDING_ORG_ID = "";

    public final static String PROJECT_STATUS_AUTO_UPDATED = "autoUpdated";

    ////////////// Request Param names.
    public final static String UPLOAD_FORMAT_PARAM_NAME = "uploadFormat";
    public final static String UPLOAD_DATA_STATUS_PARAM_NAME = "dataItemStatus";

    // 大图生成小图的阈值，2M
    public static final long MAX_IMAGE_FILE_SIZE = 2 * 1024 * 1024L;
    // 默认模型
    public static final String DEFAULT_MODEL = "human-annotation";

    // 分割算法
    public static final String GRAB_CUT = "grabcut";
    public static final String CANNY = "canny";
    public static final String THRESHOLD = "threshold";
    public static final String WATER_SHED = "watershed";
    public static final String RegionGrow = "regiongrow";
    public static final String RegionSplitMerge = "regionsplitmerge";

    public static final String USER_NOT_CONFIRM = "notConfirm";
    public static final String USER_CONFIRMED = "confirmed";
    public static final String USER_DELETED = "deleted";

}
