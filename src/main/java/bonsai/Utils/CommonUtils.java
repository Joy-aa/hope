package bonsai.Utils;

import bonsai.Constants;
import bonsai.config.DBBasedConfigs;
import bonsai.dropwizard.dao.d.DUsers;
import com.amazonaws.util.IOUtils;
import com.google.api.client.repackaged.com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mohan.gupta on 04/04/17.
 */
public class CommonUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CommonUtils.class);

    public static int parseInt(String str, int defaultVal) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {

        }
        return defaultVal;
    }

    public static double parseDouble(String str, double defaultVal) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {

        }
        return defaultVal;
    }

    public static Long parseLong(String str, Long defaultVal) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {

        }
        return defaultVal;
    }


    // Given an exception extract the stacktrace of the origin of that exception.
    // replacement for e.printStackTrace.
    public static String getStackTraceString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return sw.toString(); // stack trace as a string
    }

    public static Date convertToDate(String dateStr) {
        Date date = null;
        //convert string to date obj
        if (dateStr != null) {
            try {
                Date now = new Date();
                //dateString could be timestamp. assuming in sec.
                date = new Date(1000 * Long.parseLong(dateStr));
                long diffDays = TimeUnit.DAYS.convert(now.getTime() - date.getTime(), TimeUnit.MILLISECONDS);
                // the date string cannot be from future or older than 2 years.
                if (diffDays < 0) {
                    //may be the input is in milisec already.
                    date = new Date(Long.parseLong(dateStr));
                    diffDays = TimeUnit.DAYS.convert(now.getTime() - date.getTime(), TimeUnit.MILLISECONDS);
                }
                if (diffDays > 365) {
                    //not able to parse
                    date = null;
                }

            } catch (Exception e) {
                return convertToDateFromMysql(dateStr);
            }
        }

        return date;
    }

    public static Date convertToDateFromMysql(String dateStr) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = df.parse(dateStr);
            return date;
        } catch (Exception e) {

        }
        return null;
    }


    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public static String capitalizeFirst(String str) {
        String cleanedName = str;
        if (cleanedName != null) {
            if (cleanedName.matches(".*\\d+.*") || cleanedName.length() < 3) return null;
            //Just make first letter capital.
            cleanedName = cleanedName.substring(0, 1).toUpperCase() + cleanedName.substring(1);
        }

        return cleanedName;
    }

    public static String getUserFirstName(DUsers user) {
        String firstNameDefault = "There";
        String firstName = firstNameDefault;

        if (user != null
                && user.getOAuthType().equalsIgnoreCase("google.com")) {

            String str = user.getFirstName().trim();
            String[] parts = str.split(" ");

            if (parts[0].length() > 2 && parts[0].length() < 12) {
                firstName = parts[0];
            } else {
                firstName = str;
            }

            if (!StringUtils.isAlpha(firstName) || firstName.length() > 15) {
                firstName = firstNameDefault;
            }

//            if (firstName.contains(".") || firstName.contains("_") || firstName.matches(".*\\d+.*")) {
//                firstName = firstNameDefault;
//            }
        }
        return capitalizeFirst(firstName.toLowerCase());
    }

    public static Double parseMoneyString(String moneyStr) {
        if (moneyStr != null && !moneyStr.isEmpty()) {
            moneyStr = moneyStr.toLowerCase();
            moneyStr = moneyStr.replace(",", "");
            moneyStr = moneyStr.replace("rs", "");
            moneyStr = moneyStr.replace("inr", "");
            moneyStr = moneyStr.replaceAll("\\s+", "");
            moneyStr = moneyStr.replaceAll("^[^0-9]+", "");
            moneyStr = moneyStr.replaceAll("[^0-9]+$", "");
            moneyStr = moneyStr.trim();
            return Double.parseDouble(moneyStr);
        }
        return null;
    }

    public static int calculateDistance(double lat1, double longi1, double lat2, double longi2) {
        if (lat1 == 0 | longi1 == 0 || lat2 == 0 || longi2 == 0) {
            return Constants.INFINITE_DISTANCE_MTRS;
        }
        int mtrs = (int) distance(lat1, longi1, lat2, longi2);
        //add 20% buffer.
        double extraPercent = DBBasedConfigs.getConfig("distancePercentExtra", Double.class, 0.2);
        int finalDistance = (int) (mtrs + extraPercent * mtrs);

        //anything greater than this is like infinity.
        if (finalDistance > 99 * 1000) {
            finalDistance = Constants.INFINITE_DISTANCE_MTRS;
        }

        return finalDistance;
    }


    public static String getMonthDay(Calendar cal) {
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "June", "July", "Aug", "Sept", "Oct", "Nov", "Dec"};
        int monthNumber = cal.get(Calendar.MONTH);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        return monthNames[monthNumber] + "'" + dayOfMonth;
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        dist = dist * 1609.344;
        return (dist);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts decimal degrees to radians             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts radians to decimal degrees             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }


    static final Map<String, String> HTMLSpecialCharMap = ImmutableMap.<String, String>builder()
            .put("&lt;", "<")
            .put("&gt;", ">")
            .put("&amp;", "&")
            .put("&quot;", "\"")
            .put("&apos;", "'")
            .put("&cent;", "\u00a2")
            .put("&pound;", "\u00a3")
            .put("&yen;", "\u00a5")
            .put("&euro;", "\u20ac")
            .put("&copy;", "\u00a9")
            .put("&reg;", "\u00ae")
            .build();

    //replace the common html special characters like '&lt;' etc.
    public static String handleHTMLSpecialChars(String text) {
        for (String key : HTMLSpecialCharMap.keySet()) {
            String value = HTMLSpecialCharMap.get(key);
            text = text.replace(key, value);
        }
        return text;
    }

    public static String handleHTMLEntities(String text) {
        //if the input is not HTML then below JSOUP clean deletes everything after '<X' but not after '< x', guess the first one is treated as an HTML tag
        if (DBBasedConfigs.getConfig("dFixHTMLLessThanIssue", Boolean.class, true)) {
            text = text.replace("<", "< ");
        }
        // decode any encoded html, preventing &lt;script&gt; to be rendered as <script>(convert &lt;---> '<')
        String html = StringEscapeUtils.unescapeHtml(text);
        // remove all html tags, but maintain line breaks
        String clean = Jsoup.clean(html, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
        //String clean = Jsoup.clean(html, "", Whitelist.simpleText(), new Document.OutputSettings().escapeMode(Entities.EscapeMode.xhtml).prettyPrint(false));
        // decode html again to convert character entities back into text
        clean = StringEscapeUtils.unescapeHtml(clean);
        return clean;
    }

    public static String cleanUnicode(String unicodeStr) {
        if (unicodeStr != null) {
            // replace windows new lines by unix new lines.
            unicodeStr = unicodeStr.replaceAll("\\r\\n|\\r|\\n", "\n");

            //remove every other control character except new line and tab.
            CharMatcher charsToPreserve = CharMatcher.anyOf("\n\t");
            CharMatcher allButPreserved = charsToPreserve.negate();
            CharMatcher controlCharactersToRemove = CharMatcher.JAVA_ISO_CONTROL.and(allButPreserved);

            unicodeStr = controlCharactersToRemove.removeFrom(unicodeStr);

            unicodeStr = handleHTMLEntities(unicodeStr);

        }
        return unicodeStr;
    }


    public static List<String> readAllLines(String filepath) {
        List<String> lines = new ArrayList<>();
        BufferedReader br = null;
        FileReader fr = null;
        try {
            fr = new FileReader(filepath);
            br = new BufferedReader(fr);

            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                lines.add(cleanUnicode(sCurrentLine));
            }
        } catch (IOException e) {
            e.printStackTrace();

        } finally {

            try {
                if (br != null)
                    br.close();

                if (fr != null)
                    fr.close();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return lines;
    }

    public static void writeAllLines(List<String> lines, String filepath) {
        Writer writer = null;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(filepath), "utf-8"));
            for (String line : lines) {
                writer.write(line + "\n");
            }
        } catch (IOException ex) {
            // report
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {/*ignore*/}
        }
    }

    public static String readFile(String path, boolean doCleaning) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            String content = new String(encoded);
            if (doCleaning) {
                return cleanUnicode(content);
            }
            return content;

        } catch (Exception e) {
            LOG.error("Error reading file: " + path + " , error= " + e.toString());
        }
        return null;
    }

    // remove unprintable unicode, html tags etc.
    public static String readStream(InputStream stream) {
        try {
            String theString = IOUtils.toString(stream);
            return cleanUnicode(theString);
        } catch (Exception e) {
            LOG.error("Error reading file:  error= " + e.toString());
        }
        return null;
    }

    //do not do any processing on the read stream.
    public static String readStreamRaw(InputStream stream) {
        try {
            String theString = IOUtils.toString(stream);
            return theString;
        } catch (Exception e) {
            LOG.error("Error reading file:  error= " + e.toString());
        }
        return null;
    }


    public static boolean isTestUser(String userid) {
        List<String> testUsers = DBBasedConfigs.getConfig("testUsers", List.class, Collections.EMPTY_LIST);
        return testUsers.contains(userid);
    }


    public static boolean isValidPhoneNumber(String txt) {
        if (txt == null || txt.trim().isEmpty()) return false;
        String EMAIL_PATTERN = "^(?:(?:\\+|0{0,2})91(\\s*[\\-]\\s*)?|[0]?)?[789]\\d{9}$";

        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(txt);
        return matcher.matches();
    }

    public static String capitalizieFirstLetter(String contactName) {
        return (contactName != null ? contactName.substring(0, 1).toUpperCase() + contactName.substring(1) : contactName);

    }

    public static boolean isValidInfluencerId(String id) {
        //for now very basic check
        return (id != null && id.length() == 28);
    }

    /**
     * 从URL获取原始图像文件名
     */
    public static String getFileNameFromURL(String url) {
        String[] parts = url.split("___");
        return ThumbnailUtil.getOriginalImgUrl(parts[parts.length - 1]);
    }

    /**
     * 从轮廓中提取路径
     *
     * @param contours 使用opencv-java找到的轮廓
     * @param x        矩形框左上角x坐标
     * @param y        矩形框左上角y坐标
     * @return 轮廓中提取出的路径path
     */
    public static List<String> fromContoursGetPath(List<MatOfPoint> contours, int x, int y) {
        List<String> res = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            Point[] points = contour.toArray();
            for (Point point : points) {
                // 需要加上矩形框左上角的坐标
                point.x += x;
                point.y += y;
            }
            contour.fromArray(points);
            res.add(contour.toList().toString().replace("{", "[").replace("}", "]"));
        }

        return res;
    }

    // 获取图片的存储路径
    public static String getStoragePath() {
        String storagePath = DBBasedConfigs.getConfig("dUploadStoragePath", String.class, Constants.DEFAULT_FILE_STORAGE_DIR);
        Path folderPath = Paths.get(storagePath);
        return folderPath.getParent().toString();// 返回不带 uploads 的路径
    }

    // 获取原始图片路径
    public static String getOriginalImagePath(String imgUrl) {
        String storagePath = getStoragePath();
        return storagePath + imgUrl;
    }

    public static String getLabelStoragePath() {
        String storagePath = DBBasedConfigs.getConfig("dLabelStoragePath", String.class, Constants.DEFAULT_LABEL_STORAGE_DIR);
        Path folderPath = Paths.get(storagePath);
        return folderPath.getParent().toString();// 返回不带 labels 的路径
    }

    // 获取原始图片路径
    public static String getOriginalLabelPath(String imgUrl) {
        String storagePath = getLabelStoragePath();
        return storagePath + imgUrl;
    }

    public static String getPreStoragePath() {
        String storagePath = DBBasedConfigs.getConfig("dPreLabelStoragePath", String.class, Constants.DEFAULT_PRELABEL_STORAGE_DIR);
        Path folderPath = Paths.get(storagePath);
        return folderPath.getParent().toString();// 返回不带 uploads 的路径
    }

    // 获取原始图片路径
    public static String getOriginalPreLabelPath(String imgUrl) {
        String storagePath = getPreStoragePath();
        return storagePath + imgUrl;
    }
}
