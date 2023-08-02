package dataturks;

import bonsai.Constants;
import bonsai.Utils.CommonUtils;
import bonsai.Utils.ThumbnailUtil;
import bonsai.config.DBBasedConfigs;
import bonsai.dropwizard.dao.d.DProjects;

import javax.ws.rs.WebApplicationException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DataStorageHandler {

	private static final Logger LOG = LoggerFactory.getLogger(DOrgConfigs.class);

    public static List<String> uploadAndGetURLs(List<String> files, DProjects project) {
        return uploadAndGetURLsLocal(files, project);
    }

    public static String uploadAndGetURL(String filepath, DProjects project) {
        // 直接存储在本地
        return uploadAndGetURLLocal(filepath, project);
    }

    private static List<String> uploadAndGetURLsLocal(List<String> files, DProjects project) {
        List<String> urls = new ArrayList<>();
        for (String file : files) {
            String url = uploadAndGetURLLocal(file, project);
            if (url != null) {
                urls.add(url);
            }
        }
        return urls;
    }

    public static String uploadAndGetURLLocal(String filePath, DProjects project) {
        try {
            // 文件夹名
            String folderName = project.getId();
            // storagePath 的值从数据库查询
            String storagePath = DBBasedConfigs.getConfig("dUploadStoragePath", String.class, Constants.DEFAULT_FILE_STORAGE_DIR);
            // 拼接文件夹的全路径
            Path folderPath = Paths.get(storagePath, folderName);
            // 生成大图文件名
            String fileName = DUtils.createUniqueFileName(filePath);
			if(fileName.split("\\.")[1].equals("mrxs")) {
                String mrxsName = filePath.split("/")[filePath.split("/").length - 1].split("\\.")[0];
                File mrxsPathFile = new File(storagePath + "/" + folderName + "/" + mrxsName);
                File mrxsPathFileTo = new File(storagePath + "/" + folderName + "/" + fileName.split("\\.")[0]);
                mrxsPathFile.renameTo(mrxsPathFileTo);
                File mrxsFile = new File(storagePath + "/" + folderName + "/" + mrxsName + ".mrxs");
                File mrxsFileTo = new File(storagePath  + "/" + folderName + "/" + fileName.split("\\.")[0] + ".mrxs");
                mrxsFile.renameTo(mrxsFileTo);
            }
            Path newFilePath = folderPath.resolve(fileName);// 生成大图对应的路径
            Path thumbnailFilePath = ThumbnailUtil.getThumbnailFilePath(folderPath, fileName);// 生成缩略图路径
            File directory = new File(folderPath.toString());
	    LOG.info("folferPath:"+folderPath);
            // 下面这个创建文件夹语句不一定能执行成功，这就导致了文件不能复制
            if (!directory.exists()) {
		    LOG.info("directory:"+directory);
                directory.mkdirs();
            }

            Path oldFilePath = Paths.get(filePath);
            Files.copy(oldFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);

            // 为图片生成缩略图，也就是标注页面左上角的小地图
            ThumbnailUtil.copyThumbnail(newFilePath, thumbnailFilePath);

//            return "/" + folderPath.getParent().getFileName() + "/" + folderPath.getFileName() + "/" + newFilePath.getFileName();
            // 存储的格式是：/uploads/ff8081817d6af127017d6afa5d9f0001/5091afff-ac40-4717-a3c4-8f69781bf6b2___radiopaedia_27_86410_0-3.png.thumbnail.jpg
            return "/" + folderPath.getParent().getFileName() + "/" + folderPath.getFileName() + "/" + thumbnailFilePath.getFileName();
        }
        catch (Exception e) {
            throw new WebApplicationException("Error storing file locally, either the disk is full or some other error occured.");
        }
    }

}
