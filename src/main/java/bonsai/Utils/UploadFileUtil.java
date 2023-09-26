package bonsai.Utils;

import bonsai.Constants;
import bonsai.config.DBBasedConfigs;
import dataturks.DReqObj;
import dataturks.DTypes;
import dataturks.fileFormats.ZipHandler;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UploadFileUtil {

    private static final Logger LOG = LoggerFactory.getLogger(UploadFileUtil.class);
    public static final int BUFFER_SIZE = 1024 * 1024;

    private static UploadFileUtil instance = new UploadFileUtil();
    private Tika tika;

    private UploadFileUtil() {
        tika = new Tika();
        int maxLength = DBBasedConfigs.getConfig("maxStringLengthForTextTasks", Integer.class, Constants.MAX_STRING_LENGTH_FOR_TEXT_TASK);
        tika.setMaxStringLength(maxLength * 10);
    }

    public static UploadFileUtil getInstance() {
        return instance;
    }

    public Tika getTika() {
        return tika;
    }

    public static long getFileSize(String path) {
        File f = new File(path);
        return f.length();
    }

    public static String getRandomUploadPath(String filename) {
        //  DEFAULT_FILE_UPLOAD_DIR
        String uploadDir = DBBasedConfigs.getConfig("fileUploadDir", String.class, Constants.DEFAULT_FILE_UPLOAD_DIR);
        // We don't override existing files, create a new UUID File name:
        String targetFileName = UUID.randomUUID().toString();
        if (filename != null) {
            targetFileName = targetFileName + "___" + filename;
        }

        return uploadDir + "/" + targetFileName;
    }

    public static String uploadStreamToFile(DReqObj reqObj, InputStream stream, FormDataContentDisposition fileDetail) {

        long maxFileSize = reqObj.getConfigs().maxUploadSizeInBytes;

        String path = getRandomUploadPath(fileDetail.getFileName());
        boolean success = copy(stream, Paths.get(path), maxFileSize);
        try {
            stream.close();
        } catch (Exception e) { //can be safely ignored?
            LOG.error("Error close stream :Error = " + e.toString());
        }
        if (success) {
            return path;
        }
        return null;
    }

    public static boolean copy(InputStream input, Path target, long sizeLImitInBytes) {

        OutputStream out = null;
        boolean isExceed = false;
        try {
            out = Files.newOutputStream(target,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            long nread = 0L;
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = input.read(buf)) > 0) {
                out.write(buf, 0, n);
                nread += n;
                if (nread > sizeLImitInBytes) {// Exceeds size
                    isExceed = true;
                    break;
                }
            }
        } catch (IOException ex) {
            LOG.error("Exception while copying file " + target + " Error = " + ex.toString());
            throw new WebApplicationException("Some internal error occurred, please contact support.", Response.Status.BAD_GATEWAY);


        } finally {

            try {
                out.close();
                input.close();
            } catch (Exception e) { //can be safely ignored?
                LOG.error("Exception while copying file " + target + " Error = " + e.toString());
            }

            if (isExceed) {// Abort the copy
                try {
                    Files.deleteIfExists(target);
                } catch (Exception e) {
                    LOG.error("Exception while copying file " + target + " Error = " + e.toString());
                }

                throw new WebApplicationException("The file size is too big. Maximum allowed (MBs) = " + sizeLImitInBytes / (1024 * 1024), Response.Status.BAD_REQUEST);
            }
        }

        return !isExceed;
    }


    public static DTypes.File_Type getFileType(String filepath) {
        InputStream in = null;
        try {
            Tika tika = getInstance().getTika();
            in = new FileInputStream(new File(filepath));
            //detecting the file type using detect method
            String filetype = tika.detect(in);// 获取上传文件的类型
            if (filetype != null) {
                if (filetype.contains("image") && !filepath.contains("mrxs")) return DTypes.File_Type.IMAGE;
                if (filetype.contains("image") && filepath.contains("mrxs")) return DTypes.File_Type.MRXS;
                if (filetype.equalsIgnoreCase("text/plain")) return DTypes.File_Type.TEXT;
                if (filetype.equalsIgnoreCase("application/pdf")) return DTypes.File_Type.PDF;
                if (filetype.equalsIgnoreCase("application/x-tar")) return DTypes.File_Type.TAR;
                if (filetype.equalsIgnoreCase("application/x-gzip")) return DTypes.File_Type.GZIP;
                if (filetype.equalsIgnoreCase("application/gzip")) return DTypes.File_Type.GZ;
                if (filetype.equalsIgnoreCase("application/dicom")) return DTypes.File_Type.DCM;
                if (filetype.equalsIgnoreCase("application/octet-stream")) return DTypes.File_Type.NII;
                // even docx files are returned as zip,
                // look deeper to verify the actual type.
                if (filetype.equalsIgnoreCase("application/zip")) {
                    if (isDocX(filepath)) {
                        return DTypes.File_Type.DOCX;
                    }
                    return DTypes.File_Type.ZIP;
                } else return DTypes.File_Type.OTHERS;
            }
            //System.out.println(filepath + " : " + filetype);
        } catch (Exception e) {
            LOG.error("Error detecting type for file : " + filepath + " " + e.toString());
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception e) {

            }
        }
        return null;
    }


    public static String getFileContents(String filepath) throws IOException, TikaException {
        Tika tika = getInstance().getTika();
        String content = tika.parseToString(new File(filepath));
        if (content != null) {
            content = content.trim();
            //remove all unicode control characters, non-printable characters.
            content = CommonUtils.cleanUnicode(content);
            content = !content.isEmpty() ? content : null;
        }
        return content;
    }

    public static boolean isDocX(String filepath) {
        return ZipHandler.isDocX(filepath);
    }

    public static List<String> getAllFilesFromArchive(String archiveFilepath, String destinationDirPath) {
        if (getFileType(archiveFilepath) == DTypes.File_Type.ZIP) {
            return ZipHandler.getAllFilesFromArchive(archiveFilepath, destinationDirPath);
        }
        return null;
    }

    /*
     * Read text file which has pretagged data. Only difference is we don'tdo too much processing on the text here.
     * */
    public static String readPreTaggedFromText(String filePath) throws Exception {
        return CommonUtils.readFile(filePath, false);
    }

    /*
     * Read a file and if its a valid text readble file, read it else throw exception.
     * */
    public static String readText(String filePath) throws Exception {
        String content = null;
        //make sure its a text file.
        DTypes.File_Type file_type = UploadFileUtil.getFileType(filePath);

        //any of the non-text types.
        if (file_type == DTypes.File_Type.IMAGE ||
                file_type == DTypes.File_Type.ZIP ||
                file_type == DTypes.File_Type.TAR ||
                file_type == DTypes.File_Type.GZIP) {
            //do nothing.
        }
        //plain old text type.
        else if (file_type == DTypes.File_Type.TEXT) {
            content = CommonUtils.readFile(filePath, true);
        } //pdf, doc, etc.
        else if (file_type == DTypes.File_Type.PDF ||
                file_type == DTypes.File_Type.DOC ||
                file_type == DTypes.File_Type.DOCX ||
                file_type == DTypes.File_Type.OTHERS) {

            content = UploadFileUtil.getFileContents(filePath);
        }

        if (content == null) {
            throw new WebApplicationException("Please upload a valid text/pdf/doc file.", Response.Status.BAD_REQUEST);
        }

        return content;
    }

    //given a set of file paths, just return the ones which are images.
    public static List<String> filterOnlyImageFiles(List<String> filepaths) {
        if (filepaths == null || filepaths.isEmpty()) return filepaths;

        List<String> imageOnlyFiles = new ArrayList<>();
        for (String file : filepaths) {
            DTypes.File_Type file_type = UploadFileUtil.getFileType(file);
            if (file_type == DTypes.File_Type.IMAGE) {
                imageOnlyFiles.add(file);
            }
        }
        return imageOnlyFiles;
    }


    public static void main(String[] args) {
    }

}
