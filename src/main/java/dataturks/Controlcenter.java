package dataturks;


import bonsai.Constants;
import bonsai.Utils.*;
import bonsai.config.AppConfig;
import bonsai.config.DBBasedConfigs;
import bonsai.dropwizard.dao.d.*;
import bonsai.email.EmailSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dataturks.cache.CachedItems;
import dataturks.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


//All business logic code.
public class Controlcenter {
    private static final Logger LOG = LoggerFactory.getLogger(Controlcenter.class);

    /**
     * setup everything for onboarding a new user.
     *
     * @return true if everything fine else False
     */
    public static boolean createNewUser(DUsers user, DReqObj reqObj) {
        boolean result = false;
        if (user != null && reqObj != null) {
            //make sure the user doesn't exits already.
            DUsers userFromDb = AppConfig.getInstance().getdUsersDAO().findByEmailInternal(user.getEmail());
            if (userFromDb != null) {
                throw new WebApplicationException("User with the given email already exists", Response.Status.BAD_REQUEST);
            }
            result = createNewFreePlanUser(user, reqObj);
        }
        return result;
    }

    /**
     * setup everything for creating a new project.
     *
     * @return true if everything fine else False
     */
    public static String createNewProject(DProjects project, DReqObj reqObj) {

        if (!Validations.isValidProjectName(project.getName())) {
            throw new WebApplicationException("HTML special characters (like colon, slash, question mark, comma, dot, etc) are not allowed in project name.", Response.Status.BAD_REQUEST);
        }

        String result = "";
        if (project != null && reqObj != null) {

            boolean isFreePlanUser = Validations.isFreePlanUser(reqObj);

            if (project.getAccessType() == null) {
                //set default.
                project.setAccessType(DTypes.Project_Access_Type.RESTRICTED);
            }
            //validate the project access type and the user subscription.
            else if (project.getAccessType() == DTypes.Project_Access_Type.PRIVATE && isFreePlanUser) {
                //allowed only if its a paid account
                throw new NotAuthorizedException("Failed: Private projects not allowed for free plan users.", Response.Status.UNAUTHORIZED);
            }

            //make sure that same name project doesn't exist for the org.
            DProjects projectFromDb = DUtils.getProject(reqObj.getOrgId(), project.getName());
            if (projectFromDb != null) {
                throw new WebApplicationException("Project with name " + project.getName() + " already exists.", Response.Status.BAD_REQUEST);
            }

            result = createNewProjectInternal(project, reqObj);
        }
        return result;
    }

    public static String updateProject(DProjects project, DReqObj reqObj) {

        if (!Validations.isValidProjectName(project.getName())) {
            throw new WebApplicationException("HTML special characters not allowed in project name.", Response.Status.BAD_REQUEST);
        }

        String result = "OK";
        if (project != null && reqObj != null) {
            if (DTypes.Project_User_Role.OWNER != getProjectUserRole(project.getId(), reqObj.getUid())) {
                throw new NotAuthorizedException("Failed: You don't have update access to the project.", Response.Status.UNAUTHORIZED);
            }

            //make sure the project is not duplicate.
            DProjects projectFromDb = DUtils.getProject(reqObj.getOrgId(), project.getName());
            if (projectFromDb != null && !projectFromDb.getId().equalsIgnoreCase(project.getId())) {
                throw new WebApplicationException("Project with name " + project.getName() + " already exists.", Response.Status.BAD_REQUEST);
            }

            AppConfig.getInstance().getdProjectsDAO().saveOrUpdateInternal(project);
        }
        return result;
    }

    public static String deleteProject(DProjects project, DReqObj reqObj) {
        String result = "OK";
        if (project != null && reqObj != null) {
            if (DTypes.Project_User_Role.OWNER != getProjectUserRole(project.getId(), reqObj.getUid())) {
                throw new NotAuthorizedException("Failed: You don't have delete access to the project.", Response.Status.UNAUTHORIZED);
            }

            ProjectDetails projectDetails = getProjectSummary(project);
            if (!Validations.isProjectDeletable(projectDetails)) {
                throw new NotAuthorizedException("Failed: For your plan, project delete is not allowed.", Response.Status.UNAUTHORIZED);
            }


            // delete hits/hit result
            AppConfig.getInstance().getdHitsResultDAO().deleteByProjectId(project.getId());
            AppConfig.getInstance().getdHitsDAO().deleteByProjectId(project.getId());

            AppConfig.getInstance().getdProjectsDAO().setAsDeletedInternal(project);

        }
        return result;
    }


    //> make sure the user has permission to upload file
    // Call the right handler based on the task type
    public static UploadResponse handleFileUpload(DReqObj reqObj, String projectId, String filePath) {
        if (reqObj != null && projectId != null && filePath != null) {

            //make sure the user has permission to upload data to this project.
            DTypes.Project_User_Role role = getProjectUserRole(projectId, reqObj.getUid());
            if (role == DTypes.Project_User_Role.OWNER) {
                DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);

                if (project != null && project.getTaskType() != null) {
                    UploadResponse response = null;
                    //based on task type call the right handler.
                    switch (project.getTaskType()) {
                        case POS_TAGGING:
                            response = DataUploadHandler.handlePOSTagging(reqObj, project, filePath);
                            break;
                        case POS_TAGGING_GENERIC:
                            response = DataUploadHandler.handlePOSTaggingGeneric(reqObj, project, filePath);
                            break;
                        case TEXT_CLASSIFICATION:
                            response = DataUploadHandler.handleTextClassification(reqObj, project, filePath);
                            break;
                        case TEXT_SUMMARIZATION:
                            response = DataUploadHandler.handleTextSummarization(reqObj, project, filePath);
                            break;
                        case TEXT_MODERATION:
                            response = DataUploadHandler.handleTextModeration(reqObj, project, filePath);
                            break;
                        case DOCUMENT_ANNOTATION:
                            response = DataUploadHandler.handleDocumentAnnotation(reqObj, project, filePath);
                            break;
                        case IMAGE_CLASSIFICATION:
                            response = DataUploadHandler.handleImageClassification(reqObj, project, filePath);
                            break;
                        case IMAGE_BOUNDING_BOX:
                            response = DataUploadHandler.handleImageBoundingBox(reqObj, project, filePath);
                            break;
                        case IMAGE_POLYGON_BOUNDING_BOX:
                        case IMAGE_POLYGON_BOUNDING_BOX_V2:
                            response = DataUploadHandler.handleImagePolygonBoundingBox(reqObj, project, filePath);
                            break;
                        case VIDEO_BOUNDING_BOX:
                        case VIDEO_CLASSIFICATION:
                            response = DataUploadHandler.handleVideoTasks(reqObj, project, filePath);
                            break;
                        default:
                            throw new WebApplicationException("Unkown task type", Response.Status.BAD_REQUEST);
                    }
                    //if not set by the individual project type.
                    if (response.getTotalUploadSizeInBytes() == 0) {
                        response.setTotalUploadSizeInBytes(UploadFileUtil.getFileSize(filePath));
                    }
                    return response;

                } else {
                    throw new WebApplicationException("No such project found", Response.Status.BAD_REQUEST);
                }
            } else {
                throw new NotAuthorizedException("Failed: You must be a project owner to perform this action.", Response.Status.UNAUTHORIZED);
            }

        }
        return null;
    }

    //copy the data in a local file and return file path
    public static String handleDataDownload(DReqObj reqObj, String projectId, DTypes.File_Download_Type downloadType, DTypes.File_Download_Format format) {
        if (projectId != null) {
            DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
            if (project == null) {
                throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
            }
            canUserReadProjectElseThrowException(reqObj, project);
            String filePath = null;
            //handle download based on the project type.
            if (project.getTaskType() != null) {
                //based on task type call the right handler.
                switch (project.getTaskType()) {
                    case POS_TAGGING:
                        filePath = DataDownloadHandler.handlePOSTagging(reqObj, project, downloadType, format);
                        break;
                    case POS_TAGGING_GENERIC:
                        filePath = DataDownloadHandler.handlePOSTaggingGeneric(reqObj, project, downloadType, format);
                        break;
                    case TEXT_CLASSIFICATION:
                        filePath = DataDownloadHandler.handleTextClassification(reqObj, project, downloadType);
                        break;
                    case TEXT_SUMMARIZATION:
                        filePath = DataDownloadHandler.handleTextSummarization(reqObj, project, downloadType);
                        break;
                    case TEXT_MODERATION:
                        filePath = DataDownloadHandler.handleTextModeration(reqObj, project, downloadType);
                        break;
                    case DOCUMENT_ANNOTATION:
                        filePath = DataDownloadHandler.handleDocumentAnnotation(reqObj, project, downloadType, format);
                        break;
                    case IMAGE_CLASSIFICATION:
                        filePath = DataDownloadHandler.handleImageClassification(reqObj, project, downloadType);
                        break;
                    case IMAGE_BOUNDING_BOX:
                        filePath = DataDownloadHandler.handleImageBoundingBox(reqObj, project, downloadType);
                        break;
                    case IMAGE_POLYGON_BOUNDING_BOX:
                    case IMAGE_POLYGON_BOUNDING_BOX_V2:
                        filePath = DataDownloadHandler.handleImagePolygonBoundingBox(reqObj, project, downloadType);
                        break;
                    case VIDEO_CLASSIFICATION:
                        filePath = DataDownloadHandler.handleVideoClassification(reqObj, project, downloadType);
                        break;
                    case VIDEO_BOUNDING_BOX:
                        filePath = DataDownloadHandler.handleVideoBoundingBox(reqObj, project, downloadType);
                        break;
                    default:
                        throw new WebApplicationException("Unkown task type", Response.Status.BAD_REQUEST);
                }
                return filePath;

            } else {
                throw new WebApplicationException("No such project found", Response.Status.BAD_REQUEST);
            }
        }
        return null;
    }

    // if the project is private, make sure to check if the user has permission
    // for public and restricted projects we can return the hits.
    public static GetHits getHits(DReqObj reqObj, String projectId, String status,
                                  String userId, String label, String evaluation,
                                  long count, long start, DTypes.HIT_ORDER_Type orderBy, String model) {

        if (projectId != null) {
            DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
            if (project != null) {

                if(reqObj != null) {
                    canUserReadProjectElseThrowException(reqObj, project);
                }

                if (DUtils.isSerialOrderingProject(projectId)) orderBy = DTypes.HIT_ORDER_Type.OLD_FIRST;

                GetHits getHits = new GetHits();
                List<DHits> dHits = new ArrayList<>();

                // 如果 model 字段为空，说明是查询每个图片对应的正确标记结果
                if (model == null) {
                    // 查询 correctResult 不为空的结果
                    dHits.addAll(AppConfig.getInstance().getdHitsDAO().getCorrectResultIsNotNullInternalByPid(projectId, start, count));
                } else {
                    getHits.addRelevantProjectDetails(project, model);
                    // if status = not_done, randmonly select from db so that we do not assign same hit to multiple ppl.
                    // for all practical purposes: notDone/preTagged/reQueued are treated equally
                    if (status != null && status.equalsIgnoreCase(DConstants.HIT_STATUS_NOT_DONE)) {
                        // 查询数据集在具体model下 notDone 的图片
                        dHits.addAll(AppConfig.getInstance().getdHitsDAO()
                                .getHitsNotDoneInternal(projectId, start, count, orderBy, model));
                    } else if (status != null && status.equalsIgnoreCase(DConstants.HIT_STATUS_PRE_TAGGED)) {
                        // 查询状态是 preTagged 的图片
                        dHits.addAll(AppConfig.getInstance().getdHitsDAO()
                                .getHitsByStatus(projectId, start, count, DConstants.HIT_STATUS_PRE_TAGGED, orderBy));
                    } else if (label != null || userId != null) { //we need to do some filtering based on label/userId
                        return getHitsFiltered(reqObj, getHits, projectId, status, userId, label, count, start);
                    } else { // if status = done/ skipped / all select items serially.
                        if (evaluation != null) {
                            dHits = AppConfig.getInstance().getdHitsDAO()
                                    .getInternal(projectId, start, count, status, evaluation, orderBy);
                        } else {
                            // 根据 projectId 查询数据集中的所有图片
                            dHits = AppConfig.getInstance().getdHitsDAO().getInternalByPid(projectId, start, count);
                        }
                    }
                }

                if (dHits != null && !dHits.isEmpty()) {
                    for (DHits hit : dHits) {
                        List<DHitsResult> hitResults = null;
                        // 获取数据集中所有的图片
                        if (model != null && model.equalsIgnoreCase(DConstants.DEFAULT_MODEL) && status == null) {
                            hitResults = AppConfig.getInstance().getdHitsResultDAO()
                                    .findByHitIdAndCorrectResultInternal(hit.getId(), hit.getCorrectResult());
                            getHits.addSigleHit(hit, hitResults, 0);
                            continue;
                        }
                        if (model == null) {
                            // model 参数传递的是 correctResult 的值
                            hitResults = AppConfig.getInstance().getdHitsResultDAO()
                                    .findByHitIdAndCorrectResultInternal(hit.getId(), hit.getCorrectResult());
                            // 封装数据
                            // 如果hitResults 的结果为空，返回结果不封装此记录
                            if (hitResults.size() != 0) {
                                getHits.addSigleHit(hit, hitResults, 0);
                            }
                            continue;
                        }
                        // 从 hit_result 中查询：参数是 hitid, mode, status
                        hitResults = AppConfig.getInstance().getdHitsResultDAO()
                                .findByHitIdAndModelAndStatusInternal(hit.getId(), model, status);
                        // 如果 status 是 notDone，直接封装，因为这个状态，肯定没有 hitResults，但是前端需要这些数据
                        if (status != null && status.equalsIgnoreCase(DConstants.HIT_STATUS_NOT_DONE)) {
                            getHits.addSigleHit(hit, hitResults, 0);
                        } else {
                            // 其他的status，如果hitResults 的结果为空，返回结果不封装此记录
                            if (hitResults.size() != 0) {
                                getHits.addSigleHit(hit, hitResults, 0);
                            }
                        }
                        hitResults = null;
                    }
                }
                dHits = null;
//                getHits.delNotes();
//                getHits.delExtras();
//                System.gc();
                return getHits;
            } else {
                throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
            }
        }
        return null;
    }

    public static GetHits getHit(String hitId, String status, String model) {

        if(hitId != null) {
            DHits hit = AppConfig.getInstance().getdHitsDAO().findByIdInternal(Long.parseLong(hitId));
            GetHits getHits = new GetHits();
            if(hit != null) {
                    List<DHitsResult> hitResults = null;
                    // 获取数据集中所有的图片
                    if (model != null && model.equalsIgnoreCase(DConstants.DEFAULT_MODEL) && status == null) {
                        hitResults = AppConfig.getInstance().getdHitsResultDAO()
                                .findByHitIdAndCorrectResultInternal(hit.getId(), hit.getCorrectResult());
                        getHits.addSigleHit(hit, hitResults, 0);
                    }
                    else if (model == null) {
                        // model 参数传递的是 correctResult 的值
                        hitResults = AppConfig.getInstance().getdHitsResultDAO()
                                .findByHitIdAndCorrectResultInternal(hit.getId(), hit.getCorrectResult());
                        // 封装数据
                        // 如果hitResults 的结果为空，返回结果不封装此记录
                        if (hitResults.size() != 0) {
                            getHits.addSigleHit(hit, hitResults, 0);
                        }
                    }
                    else {
                        // 从 hit_result 中查询：参数是 hitid, mode, status
                        hitResults = AppConfig.getInstance().getdHitsResultDAO()
                                .findByHitIdAndModelAndStatusInternal(hit.getId(), model, status);
                        // 如果 status 是 notDone，直接封装，因为这个状态，肯定没有 hitResults，但是前端需要这些数据
                        if (status != null && status.equalsIgnoreCase(DConstants.HIT_STATUS_NOT_DONE)) {
                            getHits.addSigleHit(hit, hitResults, 0);
                        } else {
                            // 其他的status，如果hitResults 的结果为空，返回结果不封装此记录
                            if (hitResults.size() != 0) {
                                getHits.addSigleHit(hit, hitResults, 0);
                            }
                        }
                    }
                    hitResults = null;
                    hit = null;
//                    System.gc();
                    return getHits;
            } else {
                throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
            }
        }
        return null;

    }

    public static GetHits addLabelById(DReqObj reqObj, String hitId) {
        if(hitId != null) {
            DHits hit = AppConfig.getInstance().getdHitsDAO().findByIdInternal(Long.parseLong(hitId));
            GetHits getHits = new GetHits();
            if(hit != null) {
                Path url = Paths.get(hit.getData());
                String hitName = url.getFileName().toString();
                int idx = hitName.indexOf("___");
                String imgName = hitName.substring(idx+3);
                String imgStem = imgName.substring(0, imgName.indexOf('.'));
                String newImgName = imgStem + ".json";
                // storagePath 的值从数据库查询
                String storagePath = DBBasedConfigs.getConfig("dPreLabelStoragePath", String.class, Constants.DEFAULT_PRELABEL_STORAGE_DIR);
                // 拼接文件夹的全路径
                Path folderPath = Paths.get(storagePath);
                String newUrl = "/" + folderPath.getFileName() + "/" + newImgName;
                AppConfig.getInstance().getdHitsDAO().updateHitById(hit.getId(), newUrl);
                hit.setExtras(newUrl);
                getHits.addSigleHit(hit, null, 0);
                return getHits;
            }
        }
        return null;
    }

    public static GetHits addLabelsById(DReqObj reqObj, String projectId, long count, long start) {
        if(projectId != null) {
//            LOG.info("wangjiawangjia1:"+projectId);
            DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
            if (project != null) {
//                LOG.info("wangjiawangjia2"+project);
                if(reqObj != null) {
                    canUserReadProjectElseThrowException(reqObj, project);
                }

                GetHits getHits = new GetHits();
                List<DHits> dHits = new ArrayList<>();

                // 根据 projectId 查询数据集中的所有图片
                dHits = AppConfig.getInstance().getdHitsDAO().getInternalByPid(projectId, start, count);

                if (dHits != null && !dHits.isEmpty()) {
                    for (DHits hit : dHits) {
//                        LOG.info("wangjiawangjia3"+hit);
                        // 获取数据集中所有的图片
                        Path url = Paths.get(hit.getData());
                        String hitName = url.getFileName().toString();
                        int idx = hitName.indexOf("___");
                        String imgName = hitName.substring(idx+3);
                        String imgStem = imgName.substring(0, imgName.indexOf('.'));
                        String newImgName = imgStem + ".json";
                        // storagePath 的值从数据库查询
                        String storagePath = DBBasedConfigs.getConfig("dPreLabelStoragePath", String.class, Constants.DEFAULT_PRELABEL_STORAGE_DIR);
                        // 拼接文件夹的全路径
                        Path folderPath = Paths.get(storagePath);
                        String newUrl = "/" + folderPath.getFileName() + "/" + newImgName;
                        AppConfig.getInstance().getdHitsDAO().updateHitById(hit.getId(), newUrl);
                        hit.setExtras(newUrl);
                        getHits.addSigleHit(hit, null, 0);
                    }
                }

                return getHits;
            }else {
                throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
            }
        }
        return null;
    }

    //we need to do some filtering based on label/userId
    // we need to look into hit results and then do filtering.
    public static GetHits getHitsFiltered(DReqObj reqObj, GetHits getHits, String projectId, String status, String userId, String label, long count, long start) {

        List<DHitsResult> dHitsResults = new ArrayList<>();
        DHitsResultDAO resultDAO = AppConfig.getInstance().getdHitsResultDAO();
        DHitsDAO hitsDAO = AppConfig.getInstance().getdHitsDAO();
        //fetch all the hit_results first
        if (userId != null && label == null) {
            //simple filtering on userId, pagination can be directly handled by the db.
            dHitsResults = resultDAO.getInternal(projectId, start, count, userId, null);
        }

        if (label != null) {
            //fetch results matching label and user_id (if present as criteria).
            List<DHitsResult> dHitsResultsUnfiltered = resultDAO.getInternal(projectId, 0, 1000000, userId, label);
            // walk through all items, expand the json and select only items with the given label and start/count criteria.
            if (dHitsResultsUnfiltered != null && !dHitsResultsUnfiltered.isEmpty()) {
                int itemsProcessed = 0;
                for (DHitsResult result : dHitsResultsUnfiltered) {
                    List<String> labels = getAllLabels(result);
                    if (labels.contains(label)) {
                        itemsProcessed++;
                        if (itemsProcessed > start) {
                            dHitsResults.add(result);
                        }
                        if (dHitsResults.size() >= count) break; //pagination
                    }
                }
            }
        }

        //fit hit objects for each of the results and form response.
        for (DHitsResult result : dHitsResults) {
            DHits hit = hitsDAO.findByIdInternal(result.getHitId());
            if (hit != null) {
                List<DHitsResult> results = new ArrayList<>();
                results.add(result);
                getHits.addSigleHit(hit, results, 0);
            }
        }

        return getHits;
    }

    //return all label strings from a dhit result object.
    private static List<String> getAllLabels(DHitsResult result) {
        ObjectMapper mapper = AppConfig.getInstance().getObjectMapper();
        List<String> labels = null;
        try {
            JsonNode node = null;
            //assuming the result is a JSON
            try {
                node = mapper.readTree(result.getResult());
                if (node.has("annotationResult")) {
                    node = node.get("annotationResult");
                }
            } catch (Exception e) {
                //may be not a json? do nothing.
            }
            //result was infact a json.
            if (node != null) {
                if (node.isArray()) {
                    labels = new ArrayList<>();
                    for (JsonNode item : (ArrayNode) node) {
                        labels.addAll(getLabels(item));
                    }
                } else {
                    labels = getLabels(node);
                }
            } else { //old way of storing result.
                labels = Arrays.asList(result.getResult().split(DConstants.LABEL_SEPARATOR));
            }
            return labels;
        } catch (Exception e) {
            LOG.error("Error: getHitsFiltered for hitid = " + result.getHitId() + " " + e.toString());
        }
        return Collections.emptyList();
    }

    public static List<String> getLabels(JsonNode node) {
        JsonNode labelNode = node.has("labels") ? node.get("labels") : node.get("label");
        if (labelNode != null) {
            List<String> labels = new ArrayList<>();

            if (labelNode.isArray()) {
                for (JsonNode item : (ArrayNode) labelNode) {
                    labels.add(item.textValue());
                }
            } else { //polygon bb label is not array.
                labels.add(labelNode.textValue());
            }
            return labels;
        }
        return Collections.emptyList();
    }


    // validate the user has permission to write.
    // update the data for the hit.
    public static boolean addHitResultInternal(DReqObj reqObj, String projectId, long hitId) throws Exception {
        try{
            if (projectId != null) {
                // 验证 projectId 是否合法
                DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
                if (project == null) {
                    throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
                }

                canUserWriteProjectElseThrowException(reqObj, project);

                // 根据 hitid 从 hits 表中查询结果
                DHits hit = AppConfig.getInstance().getdHitsDAO().findByIdInternal(hitId);
                if (hit == null) {
                    throw new WebApplicationException("No such hit found", Response.Status.NOT_FOUND);
                }

                if(reqObj.getReqMap().containsKey("label")) {
                    String base64Str = reqObj.getReqMap().get("label");
                    Path url = Paths.get(hit.getData());
//                    System.out.println(url);
//                Path fileName = url.getFileName();
                    String fileName = ThumbnailUtil.getOriginalImgUrl(url.getFileName().toString());
                    String newName = fileName.substring(0, fileName.lastIndexOf(".")) + ".json";
//                    System.out.println("newName:"+newName);
                    // 文件夹名
                    String folderName = project.getId();
                    // storagePath 的值从数据库查询
                    String storagePath = DBBasedConfigs.getConfig("dResultStoragePath", String.class, Constants.DEFAULT_LABEL_STORAGE_DIR);
                    // 拼接文件夹的全路径
                    Path tmpPath = Paths.get(DBBasedConfigs.getConfig("dTmpStoragePath", String.class,Constants.DEFAULT_FILE_UPLOAD_DIR), folderName);
//                    System.out.println("tmpPath:"+tmpPath);
                    Path folderPath = Paths.get(storagePath, folderName);
                    String newUrl = "/" + folderPath.getParent().getFileName() + "/" + folderPath.getFileName() + "/" + newName;
//                    byte[] b = ThumbnailUtil.decode(base64Str, newName, tmpPath.toString());
                    if(base64Str == null) {
                        throw new WebApplicationException("Img-json is null'", Response.Status.NOT_FOUND);
                    }
                    else {
                        File  dir=new File(folderPath.toString());
                        if (!dir.exists() && !dir.isDirectory()) {
                            dir.mkdirs();
                        }
                        String dstFile = Paths.get(folderPath.toString(),newName).toString();
                        File file=new File(dstFile);
                        FileOutputStream fileOutputStream=new FileOutputStream(file);//实例化FileOutputStream
                        OutputStreamWriter outputStreamWriter=new OutputStreamWriter(fileOutputStream,"utf-8");//将字符流转换为字节流
                        BufferedWriter bufferedWriter= new BufferedWriter(outputStreamWriter);//创建字符缓冲输出流对象
                        bufferedWriter.write(base64Str);//将格式化的jsonarray字符串写入文件
                        bufferedWriter.flush();//清空缓冲区，强制输出数据
                        bufferedWriter.close();//关闭输出流
                        fileOutputStream.close();
                        hit.setNotes(newUrl);
                    }
                }

                if(reqObj.getReqMap().containsKey("base64Str")) {
                    String base64Str = reqObj.getReqMap().get("base64Str");
                    if(base64Str != null && !base64Str.isEmpty()) {
                        Path url = Paths.get(hit.getData());
                        String fileName = ThumbnailUtil.getOriginalImgUrl(url.getFileName().toString());
                        String newName = fileName.substring(0, fileName.lastIndexOf(".")) + ".png";
                        // 文件夹名
                        String folderName = project.getId();
                        // storagePath 的值从数据库查询
                        String storagePath = DBBasedConfigs.getConfig("dResultStoragePath", String.class, Constants.DEFAULT_LABEL_STORAGE_DIR);
                        // 拼接文件夹的全路径
                        Path tmpPath = Paths.get(DBBasedConfigs.getConfig("dTmpStoragePath", String.class, Constants.DEFAULT_FILE_UPLOAD_DIR), folderName);
                        Path folderPath = Paths.get(storagePath, folderName);
                        byte[] b = ThumbnailUtil.decode(base64Str, newName, tmpPath.toString());
                        if (b == null) {
                            throw new WebApplicationException("Img-base64 decode fails'", Response.Status.NOT_FOUND);
                        } else {
                            File dir = new File(folderPath.toString());
                            if (!dir.exists() && !dir.isDirectory()) {
                                dir.mkdirs();
                            }
                            String tmpFile = Paths.get(tmpPath.toString(), newName).toString();
                            String dstFile = Paths.get(folderPath.toString(), newName).toString();
                            new AlphaUtil().getMaskPath(tmpFile, dstFile);
//                            File file1 = new File(tmpFile);
//                            file1.delete();
//                            File file2 = new File(tmpPath.toString());
//                            file2.delete();
                        }
                    }
                }

                String hitStatus = hit.getStatus();
                // 把项目的状态改为前端传来的状态
                // 1、判断前端是否给了 status 这个字段，
                // 2、有则设置，没有的话判断是否有 skipped 这个字段
                // 3、有的话设置状态为 skipped，没有就设置成 done
                if (reqObj.getReqMap().containsKey("status")) {
                    String status = reqObj.getReqMap().get("status");
                    if (DUtils.isValidHitStatus(status)) {
                        hitStatus = status;
                    } else {
                        throw new WebApplicationException("Unknown state for hit " + status, Response.Status.BAD_REQUEST);
                    }
                } //old way when we did not pass status in the post call.
                else {
                    boolean skipped = reqObj.getReqMap().containsKey("skipped");
                    if (skipped) {
                        hitStatus = DConstants.HIT_STATUS_SKIPPED;
                    } else {
                        hitStatus = DConstants.HIT_STATUS_DONE;
                    }
                }

                // 获取当前图片使用的模型
                String model = "";
                if (reqObj.getReqMap().containsKey("model")) {
                    model = reqObj.getReqMap().get("model");
                }

                if (hitStatus.equalsIgnoreCase("done")) {
                    hit.setCorrectResult(model);// 设置当前图片的 correctResult
                    //update the hit status.
                    hit.setStatus(hitStatus);
                }
                if(hitStatus.equalsIgnoreCase(DConstants.HIT_STATUS_REQUEUED)){
                    hit.setCorrectResult("");// 设置当前图片的 correctResult
                    //update the hit status.
                    hit.setStatus(DConstants.HIT_STATUS_NOT_DONE);
                    // 修改状态为 notDone
                    hitStatus = DConstants.HIT_STATUS_NOT_DONE;
                }

                if (DUtils.isHittedStatus(hitStatus))// 状态必须是有效的
                {
                    // 更新或创建
                    DHitsResultDAO dao = AppConfig.getInstance().getdHitsResultDAO();
                    DHitsResult result = null;

                    //find any hit result which might be present for the HIT already.
                    // Once can tag and then edit and do skip and then retag..so can't rely on if the hit is skipped etc..we might have the
                    // hit result in the db any way.
//                List<DHitsResult> results = dao.findByHitIdInternal(hit.getId());
                    // 根据 hitid 和 model 查询结果
                    List<DHitsResult> results = dao.findByHitIdAndModelInternal(hit.getId(), model);
                    if (results != null && !results.isEmpty()) result = results.get(0);

                    // 没有则创建
                    if (result == null)
                        result = new DHitsResult(hitId, projectId, reqObj.getUid());

                    // 设置各种字段的值
                    result.setUserId(reqObj.getUid());
                    result.setPredLabel(reqObj.getReqMap().get("predLabel"));
                    result.setResult(reqObj.getReqMap().get("result"));
                    result.setNotes(reqObj.getReqMap().get("notes"));
                    // 2021.08.28 添加
                    result.setModel(model);
                    result.setStatus(hitStatus);
                    try {
                        int time = 0;
                        if (reqObj.getReqMap().containsKey("timeTakenToLabelInSec")) {
                            time = (int) Double.parseDouble(reqObj.getReqMap().get("timeTakenToLabelInSec"));
                        }
                        time = time > 10000 ? 0 : time; //no point keeping wrong values.
                        result.setTimeTakenToLabelInSec(time);
                    } catch (Exception e) {
                        LOG.error(e.toString() + " time taken value = " + reqObj.getReqMap().get("timeTakenToLabelInSec"));
                    }
                    // 更新 d_hits_result 表
                    AppConfig.getInstance().getdHitsResultDAO().saveOrUpdateInternal(result);
//                hitStatus = DConstants.HIT_STATUS_DONE;
                }

                // 更新 d_hits 表
                AppConfig.getInstance().getdHitsDAO().saveOrUpdateInternal(hit);
            }

            return true;
        }
        catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e.toString() + " " + CommonUtils.getStackTraceString(e));
            throw e;
        }
    }

    public static boolean addEvaluationResultInternal(DReqObj reqObj, String projectId, long hitId) {
        if (projectId != null) {

            DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
            if (project == null) {
                throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
            }

            canUserWriteProjectElseThrowException(reqObj, project);

            //make sure that such a hit exists.
            DHits hit = AppConfig.getInstance().getdHitsDAO().findByIdInternal(hitId);
            if (hit == null) {
                throw new WebApplicationException("No such hit found", Response.Status.NOT_FOUND);
            }

            if (reqObj.getReqMap().containsKey("evaluation")) {
                DTypes.HIT_Evaluation_Type evaluation_type = DTypes.HIT_Evaluation_Type.valueOf(reqObj.getReqMap().get("evaluation").toUpperCase());
                hit.setEvaluationType(evaluation_type);
                AppConfig.getInstance().getdHitsDAO().saveOrUpdateInternal(hit);
            }

            return true;
        }
        return false;
    }


    //provide the project details etc.
    public static ProjectDetails getProjectDetailsInternal(DReqObj reqObj, String projectId) {
        DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
        if (project == null) {
            throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
        }

        canUserReadProjectElseThrowException(reqObj, project);
        // 获取项目信息
        ProjectDetails projectDetails = getProjectSummary(project);


        //set permissions for the user.
        DTypes.Project_User_Role role = getProjectUserRole(projectId, reqObj.getUid());
        projectDetails.setPermissions(DUtils.getProjectPermissionsForRole(role));

        //if not logged in, don't allow downloads.
        setPermissionForDownload(reqObj, projectDetails);

        if (DTypes.Project_User_Role.OWNER == role) {
            //can the project be deleted?
            boolean isDeleteTable = Validations.isProjectDeletable(projectDetails);
            if (isDeleteTable) {
                projectDetails.getPermissions().setCanDeleteProject(true);
            }
        }
        // NOTE: Always call this after permissions for the given user has been set.
        handleSubscriptionExpiry(reqObj, projectDetails);

        addProjectContributorDetails(project, projectDetails);
        maskDetails(reqObj, project, projectDetails);

        return projectDetails;
    }

    public static ProjectStats getProjectStatsInternal(DReqObj reqObj, String projectId) {
        DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
        if (project == null) {
            throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
        }
        canUserReadProjectElseThrowException(reqObj, project);

        //handle based on the project type.
        if (project.getTaskType() != null) {

            ProjectStats stats = getProjectStatsInternal(reqObj, project);
            if (stats != null) {
                ProjectDetails details = getProjectSummary(project);
                stats.setDetails(details);
            }
            return stats;

        } else {
            throw new WebApplicationException("No such task type found", Response.Status.NOT_FOUND);
        }
    }

    public static ProjectStats getProjectStatsInternal(DReqObj reqObj, DProjects project) {
        return getProjectStatsForHITStatus(reqObj, project, Arrays.asList(new String[]{DConstants.HIT_STATUS_DONE, DConstants.HIT_STATUS_PRE_TAGGED}));
    }

    public static ProjectStats getProjectStatsForHITStatus(DReqObj reqObj, DProjects project, List<String> hitStatuses) {
        //handle based on the project type.
        //based on task type call the right handler.
        ProjectStats stats = null;
        reqObj.setValidStatesForStatsCalculation(hitStatuses);

        switch (project.getTaskType()) {
            case POS_TAGGING:
                stats = StatsHandler.handlePOSTagging(reqObj, project);
                break;
            case POS_TAGGING_GENERIC:
                stats = StatsHandler.handlePOSTaggingGeneric(reqObj, project);
                break;
            case TEXT_CLASSIFICATION:
                stats = StatsHandler.handleTextClassification(reqObj, project);
                break;
            case TEXT_SUMMARIZATION:
                stats = StatsHandler.handleTextSummarization(reqObj, project);
                break;
            case TEXT_MODERATION:
                stats = StatsHandler.handleTextModeration(reqObj, project);
                break;
            case DOCUMENT_ANNOTATION:
                stats = StatsHandler.handleDocumentAnnotation(reqObj, project);
                break;
            case IMAGE_CLASSIFICATION:
                stats = StatsHandler.handleImageClassification(reqObj, project);
                break;
            case IMAGE_BOUNDING_BOX:
                stats = StatsHandler.handleImageBoundingBox(reqObj, project);
                break;
            case IMAGE_POLYGON_BOUNDING_BOX:
                stats = StatsHandler.handleImagePolygonBoundingBox(reqObj, project);
                break;
            case IMAGE_POLYGON_BOUNDING_BOX_V2:
                stats = StatsHandler.handleImagePolygonBoundingBox(reqObj, project);
                break;
            case VIDEO_CLASSIFICATION:
                stats = StatsHandler.handleVideoClassification(reqObj, project);
                break;
            case VIDEO_BOUNDING_BOX:
                stats = StatsHandler.handleVideoBoundingBox(reqObj, project);
                break;
            default:
                // result empty stats.
                stats = new ProjectStats(project.getId());
                //throw new WebApplicationException("Unkown task type" , Response.Status.BAD_REQUEST);
        }

        return stats;

    }


    //name to id mapping.
    public static String getProjectIdInternal(DReqObj reqObj, String orgName, String projectName) {
        DOrgs org = AppConfig.getInstance().getdOrgsDAO().findByNameInternal(orgName);
        if (org != null) {
            String orgId = org.getId();
            DProjects project = DUtils.getProject(orgId, projectName);
            if (project != null) {
                canUserReadProjectElseThrowException(reqObj, project);
                return project.getId();
            }
        }

        throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
    }

    // a basic summary of the project, like type, rules, hits, done etc.
    //TODO: ignore who all are deleted.
    public static ProjectDetails getProjectSummary(DProjects project) {
        ProjectDetails projectDetails = new ProjectDetails(project.getId(), project.getName());
        projectDetails.setAccess_type(project.getAccessType());
        projectDetails.setTask_type(project.getTaskType());
        projectDetails.setCreated_timestamp(project.getCreated_timestamp());
        projectDetails.setTaskRules(project.getTaskRules());

        // project信息里添加医学图像格式、器官类型信息
        projectDetails.setMedicalImageFormat(project.getMedicalImageFormat());
        projectDetails.setImageOrganType(project.getImageOrganType());
        // 添加状态和数据集是否公开
        projectDetails.setPublic(project.isPublic());
        projectDetails.setStatus(project.getStatus());

        //write org specific
        DOrgs orgs = AppConfig.getInstance().getdOrgsDAO().findByIdInternal(project.getOrgId());
        if (orgs != null) {
            projectDetails.setOrgId(project.getOrgId());
            projectDetails.setOrgName(orgs.getName());

            if (Validations.isPaidPlanOrg(project.getOrgId())) {
                projectDetails.setVisibility_type(DTypes.Project_Visibility_Type.PRIVATE);
            } else {
                projectDetails.setVisibility_type(DTypes.Project_Visibility_Type.PUBLIC);
            }
        }

        //numhits, num done, % completed etc.
        long totalHits = AppConfig.getInstance().getdHitsDAO().getCountForProject(project.getId());
        long totalDone = AppConfig.getInstance().getdHitsDAO().getCountForProjectDone(project.getId());
        long totalSkipped = AppConfig.getInstance().getdHitsDAO().getCountForProjectSkipped(project.getId());
        long totalDeleted = AppConfig.getInstance().getdHitsDAO().getCountForProjectDeleted(project.getId());


        long totalEvaluationCorrect = AppConfig.getInstance().getdHitsDAO().getCountForProjectEvaluationCorrect(project.getId());
        long totalEvaluationInCorrect = AppConfig.getInstance().getdHitsDAO().getCountForProjectEvaluationInCorrect(project.getId());

        projectDetails.setTotalHits(totalHits);
        projectDetails.setTotalHitsDone(totalDone);
        projectDetails.setTotalHitsSkipped(totalSkipped);
        projectDetails.setTotalHitsDeleted(totalDeleted);

        projectDetails.setTotalEvaluationCorrect(totalEvaluationCorrect);
        projectDetails.setTotalEvaluationInCorrect(totalEvaluationInCorrect);

        projectDetails.setDescription(project.getDescription());
        projectDetails.setShortDescription(project.getShortDescription());

        return projectDetails;
    }

    // make sure the requesting user is the project owner.
    public static boolean addContributorInternal(DReqObj reqObj, String projectId, String userEmail, DTypes.Project_User_Role role) {
        if (projectId != null) {

            DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
            if (project == null) {
                throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
            }

            //only an admin can add others.
            if (!isProjectAdmin(projectId, reqObj.getUid())) {
                throw new NotAuthorizedException("Failed: You don't have permission to perform this action. Only a admin can add a contributor.", Response.Status.UNAUTHORIZED);
            }


            DUsers requestingUser = reqObj.getUser();
            //do we have a user with the email id.
            DUsers userToBeAdded = AppConfig.getInstance().getdUsersDAO().findByEmailInternal(userEmail);
            if (userToBeAdded != null) {
                addUserToProject(userToBeAdded, projectId, role); //either add a new entry or update the role.
                EmailSender.sendDataturksUserAddedToProject(requestingUser, userToBeAdded, project);
            } else {
                //add email in a invite table.
                DProjectInvites invites = new DProjectInvites(reqObj.getUid(), projectId, userEmail, role.toString());
                AppConfig.getInstance().getdProjectInvitesDAO().createInternal(invites);
                EmailSender.sendDataturksEmailAddedToProject(requestingUser, userEmail, project);
            }

        }
        return true;
    }

    // make sure the requesting user is the project owner. Also if remove user from the project
    // and also from the invited list if present.
    public static boolean removeContributorInternal(DReqObj reqObj, String projectId, String userEmail) {
        if (projectId != null) {

            DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
            if (project == null) {
                throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
            }

            DTypes.Project_User_Role role = getProjectUserRole(projectId, reqObj.getUid());
            if (role != DTypes.Project_User_Role.OWNER) {
                throw new NotAuthorizedException("Failed: You don't have admin access to the project.", Response.Status.UNAUTHORIZED);
            }

            //do we have a user with the email id.
            DUsers user = AppConfig.getInstance().getdUsersDAO().findByEmailInternal(userEmail);
            if (user != null) {
                removeUserFromProject(user, projectId);
            }

            //Also, remove from the invite list.
            List<DProjectInvites> projectInvites = AppConfig.getInstance().getdProjectInvitesDAO().findByEmailInternal(userEmail);
            if (projectInvites != null) {
                for (DProjectInvites projectInvite : projectInvites) {
                    // we can have duplicate invites, hence don't break after deleting one.
                    if (projectInvite.getProjectId().equalsIgnoreCase(projectId)) {
                        AppConfig.getInstance().getdProjectInvitesDAO().deleteInternal(projectInvite);
                    }
                }
            }


        }
        return true;
    }

    //return true if added else flase.
    private static boolean addUserToProject(DUsers user, String projectId, DTypes.Project_User_Role role) {
        DProjectUsers projectUser = AppConfig.getInstance().getdProjectUsersDAO().findByUserAndProjectIdInternal(user.getId(), projectId);
        if (projectUser == null) {
            projectUser = new DProjectUsers(projectId, user.getId(), role);
            AppConfig.getInstance().getdProjectUsersDAO().createInternal(projectUser);
            return true;
        } else { //else update the role.
            projectUser.setRole(role);
            AppConfig.getInstance().getdProjectUsersDAO().saveOrUpdateInternal(projectUser);
            return true;

        }
    }

    //return true if removed else false.
    private static boolean removeUserFromProject(DUsers user, String projectId) {
        DProjectUsers projectUsers = AppConfig.getInstance().getdProjectUsersDAO().findByUserAndProjectIdInternal(user.getId(), projectId);
        if (projectUsers != null && projectUsers.getRole() != DTypes.Project_User_Role.OWNER) {
            AppConfig.getInstance().getdProjectUsersDAO().deleteInternal(projectUsers);
            return true;
        }
        return false;
    }

    //do this user context has read permission for the project.
    // for an org project only org users can read.
    // for a private project only the contributors can read.
    private static boolean canUserReadProjectElseThrowException(DReqObj reqObj, DProjects project) {

        //for free projects all can read.
        if (Validations.isFreePlanProject(project)) return true;


        //for org projects which are not private, all org users can read
        if (Validations.isPaidPlanProject(project) &&
                project.getAccessType() != DTypes.Project_Access_Type.PRIVATE &&
                Validations.doesUserBelongToOrg(reqObj, project.getOrgId())) {
            return true;

        }

        // all contributors can read a project.
        DTypes.Project_User_Role role = getProjectUserRole(project.getId(), reqObj.getUid());
        if (role != null) return true;

        //if the user is any one the configured devs at Dataturks.
        if (Validations.isDataturksAdminUser(reqObj)) return true;

        throw new NotAuthorizedException("Failed: You don't have access to this project.", Response.Status.UNAUTHORIZED);
    }

    //do this user context has write permission for the project.
    // for a public project all can write else only contributors can write.
    // for a org public project make sure the user is either a contributor or inside org.
    private static boolean canUserWriteProjectElseThrowException(DReqObj reqObj, DProjects project) {

        // for Public projects all can write.
        if (project.getAccessType() == DTypes.Project_Access_Type.PUBLIC) {
            if (Validations.isFreePlanProject(project)) return true;
            else if (Validations.isPaidPlanProject(project) && Validations.doesUserBelongToOrg(reqObj, project.getOrgId()))
                return true;
        }

        // all contributors can write.
        DTypes.Project_User_Role role = getProjectUserRole(project.getId(), reqObj.getUid());
        if (role != null) return true;

        throw new NotAuthorizedException("Failed: You don't have access to this project. Please contact the project admin.", Response.Status.UNAUTHORIZED);

    }

    /**
     * Get the user's role in the given project.
     *
     * @param projectId
     * @param userId
     * @return
     */
    private static DTypes.Project_User_Role getProjectUserRole(String projectId, String userId) {
        if (projectId != null && userId != null) {
            DProjectUsers projectUser = AppConfig.getInstance().getdProjectUsersDAO().findByUserAndProjectIdInternal(userId, projectId);
            if (projectUser != null) {
                return projectUser.getRole();
            }
        }
        return null;
    }

    public static boolean isProjectAdmin(String projectId, String userId) {
        DTypes.Project_User_Role role = getProjectUserRole(projectId, userId);
        return role == DTypes.Project_User_Role.OWNER;
    }

    /**
     * @return true if everything fine else False
     */
    private static String createNewProjectInternal(DProjects project, DReqObj reqObj) {
        //create the project.
        String projectId = AppConfig.getInstance().getdProjectsDAO().createInternal(project);

        //create a project user entry.
        DProjectUsers projectUser = new DProjectUsers(projectId, reqObj.getUid());
        projectUser.setRole(DTypes.Project_User_Role.OWNER);

        AppConfig.getInstance().getdProjectUsersDAO().createInternal(projectUser);
        return projectUser.getProjectId();
    }

    public static APIKey getOrCreateAPIKeyInternal(DReqObj reqObj) {
        DAPIKeys apiKey = AppConfig.getInstance().getDapiKeysDAO().findByUIDInternal(reqObj.getUid());
        if (apiKey == null) {
            apiKey = createAPIKey(reqObj);
        }
        APIKey key = new APIKey(apiKey.getKey(), apiKey.getSecret());
        return key;
    }

    private static DAPIKeys createAPIKey(DReqObj reqObj) {
        //only paid plans allow create API key.
        if (Validations.isFreePlanUser(reqObj)) {
            throw new NotAuthorizedException("Failed: API Keys are only allowed on a paid account.", Response.Status.UNAUTHORIZED);

        }

        String key = DUtils.generateAPIKey();
        String secret = DUtils.generateAPISecret();
        DAPIKeys apiKey = new DAPIKeys(reqObj.getUid());
        apiKey.setKey(key);
        apiKey.setSecret(secret);
        AppConfig.getInstance().getDapiKeysDAO().createInternal(apiKey);
        return apiKey;
    }

    public static License getLicenseInternal(String key) {

        License licenseRes = new License();
        DLicenseDAO licenseDAO = AppConfig.getInstance().getdLicenseDAO();
        DLicense license = licenseDAO.findByKeyInternal(key);

        if (license != null) {
            licenseRes.setDoesKeyExist(true);

            if (license.getActivated_on_timestamp() != null) {
                licenseRes.setKeyAvailable(false);
                return licenseRes;
            }
            //activate the license.
            license.setActivated_on_timestamp(new Date());
            licenseDAO.saveOrUpdateInternal(license);

            licenseRes.setLicenseText(license.getLicenseText());
        }

        return licenseRes;
    }

    /**
     * @return true if everything fine else False
     */
    private static boolean createNewFreePlanUser(DUsers user, DReqObj reqObj) {
        boolean result = false;
        //create user.
        AppConfig.getInstance().getdUsersDAO().createInternal(user);

        //create org for a new user.
        String orgName = DUtils.createUniqOrgName(user);
        DOrgs orgs = new DOrgs(orgName);
        orgs.setCity(user.getCity());
        orgs.setContactEmail(user.getEmail());
        orgs.setContactName(user.getFirstName());
        orgs.setContactPhone(user.getPhone());
        orgs.setLogoPic(user.getProfilePic());
        orgs.setWebsite(reqObj.getReqMap().get("website"));
        String orgId = AppConfig.getInstance().getdOrgsDAO().createInternal(orgs);
        //now fetch from the DB.
        orgs = AppConfig.getInstance().getdOrgsDAO().findByIdInternal(orgId);

        //create a subscription plan for the user.
        DSubscriptions subs = new DSubscriptions(orgs.getId());
        subs.setPlanId((long) DBBasedConfigs.getConfig("dFreePlanId", Integer.class, (int) DConstants.FREE_PLAN_ID));
        long subsId = AppConfig.getInstance().getdSubscriptionsDAO().createInternal(subs);

        //update subs id.
        orgs.setSubscriptionId(subsId);
        AppConfig.getInstance().getdOrgsDAO().saveOrUpdateInternal(orgs);

        //add user<->Org mapping.
        DOrgUsers orgUsers = new DOrgUsers(orgs.getId(), user.getId());
        orgUsers.setRole(DTypes.User_Roles.ADMIN);
        AppConfig.getInstance().getdOrgUsersDAO().createInternal(orgUsers);

        try {
            //if the user has any pending project invitations add them as well.
            addUserProjectInvitations(user);

            //add default projects.
            // addDefaultProjects(user);
        } catch (Exception e) {
            LOG.error("For user " + user.getId() + " Error= " + e.toString());
        }

        result = true;

        return result;
    }


    //when a new user has signed up, add him to projects which he has been invited to.
    private static void addUserProjectInvitations(DUsers user) {
        try {
            if (user != null) {
                List<DProjectInvites> projectInvites = AppConfig.getInstance().getdProjectInvitesDAO().findByEmailInternal(user.getEmail());
                for (DProjectInvites projectInvite : projectInvites) {
                    addUserToProject(user, projectInvite.getProjectId(), DTypes.Project_User_Role.valueOf(projectInvite.getRole()));
                }
            }
        } catch (Exception e) {
            LOG.error("While addUserProjectInvitations: for userid " + user.getId() + " Error: " + e.toString());
        }
    }

    //when a new user has signed up, add him to some demo projects by default
    private static void addDefaultProjects(DUsers user) {
        try {
            if (user != null) {
                List<String> projectIds = DBBasedConfigs.getConfig("dtNewUserDefaultProjects", List.class, Collections.EMPTY_LIST);
                for (String projectId : projectIds) {
                    addUserToProject(user, projectId, DTypes.Project_User_Role.CONTRIBUTOR);
                }
            }
        } catch (Exception e) {
            LOG.error("While addDefaultProjects: for userid " + user.getId() + " Error: " + e.toString());
        }
    }

    public static UserHome getUserHomeInternal(DReqObj reqObj) {
        UserHome userHome = new UserHome(reqObj.getUid());
        DUsers user = reqObj.getUser();
        if (user == null) {
            throw new WebApplicationException("No such user found", Response.Status.NOT_FOUND);
        }

        // get the config for the org.
        DUtils.setConfigForOrg(reqObj);

        //user details.
        userHome.setUserDetails(new UserDetails(user));

        // Set<String> defaultProjects = DUtils.getDefaultProjects();

        //set project details.
        List<DProjectUsers> projectUsers = AppConfig.getInstance().getdProjectUsersDAO().findAllByUserIdInternal(reqObj.getUid());
        if (projectUsers != null && !projectUsers.isEmpty()) {
            for (DProjectUsers projectUser : projectUsers) {

                // 如果用户没有访问权，不放到结果集里面
                if (projectUser.getRole() != DTypes.Project_User_Role.OWNER) {
                    continue;
                }

                DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectUser.getProjectId());
                if (project != null) {
                    ProjectDetails projectDetails = Controlcenter.getProjectSummary(project);
                    UserProjectPermissions permissions = DUtils.getProjectPermissionsForRole(projectUser.getRole());
                    if (Validations.hasSubscriptionExpired(projectDetails.getOrgId())) {
                        permissions.setCanCompleteHITs(false);
                    }
                    userHome.addUserProjects(projectDetails, projectUser.getRole(), permissions);
                }
            }
        }

        //user Org details
        DOrgs orgs = reqObj.getOrg();
        setOrgDetails(reqObj, orgs, userHome);
        setSubscriptionDetails(reqObj, orgs, userHome);

        // sort projects by created time. Latest first.
        userHome.reorderProjects();

        //update the last login time.
        updateLastLoginTime(user);

        return userHome;
    }

    private static void setOrgDetails(DReqObj reqObj, DOrgs orgs, UserHome userHome) {
        if (orgs != null) {
            userHome.setOrgId(orgs.getId());
            userHome.setOrgName(orgs.getName());
        }
    }

    private static void setSubscriptionDetails(DReqObj reqObj, DOrgs orgs, UserHome userHome) {
        DOrgConfigs config = reqObj.getConfigs();

        DSubscriptions subscriptions = AppConfig.getInstance().getdSubscriptionsDAO().findByOrgIdInternal(orgs.getId());
        if (subscriptions != null) {
            userHome.setLabelsDone(subscriptions.getLabelsDone());
            userHome.setSubscriptionExpiryTimestamp(subscriptions.getValidTill());

            DSubscriptionPlans plan = AppConfig.getInstance().getdSubscriptionPlansDAO().findByIdInternal(subscriptions.getPlanId());
            if (plan != null)
                userHome.setPlanName(plan.getDisplayName());
        }
        if (config != null) {
            userHome.setLabelsAllowed(config.getNumLabelsAllowed());
        }

        if (Validations.hasSubscriptionExpired(config, subscriptions)) {
            userHome.setHasSubscriptionExpired(true);
        }
    }

    public static OrgProjects getOrgProjectsInternal(DReqObj reqObj, String orgId) {
        DOrgs orgs = AppConfig.getInstance().getdOrgsDAO().findByIdInternal(orgId);
        if (orgs == null) {
            throw new WebApplicationException("No such org found", Response.Status.NOT_FOUND);
        }

        List<DProjects> projects = null;
        if (DUtils.isTrendingOrg(orgId)) {
            return CachedItems.getTopProjects();
            //projects = getTrendingProjects();
        } else {
            projects = AppConfig.getInstance().getdProjectsDAO().findByOrgIdInternal(orgId);
        }


        OrgProjects orgProjects = getOrgProjectsInternal(reqObj, projects);

        //either we have some projects to show inside the org or the user belongs to the org
        // then we add org details.
        if ((orgProjects.getProjects() != null && !orgProjects.getProjects().isEmpty()) ||
                Validations.doesUserBelongToOrg(reqObj, orgId)) {
            orgProjects.setOrgId(orgs.getId());
            orgProjects.setOrgName(orgs.getName());

        } else {
            throw new WebApplicationException("No such org found", Response.Status.NOT_FOUND);
        }

        DUtils.sortProjectsByDate(orgProjects.getProjects(), false);

        return orgProjects;
    }

    private static OrgProjects getOrgProjectsInternal(DReqObj reqObj, List<DProjects> projects) {
        OrgProjects orgProjects = new OrgProjects();
        if (projects != null) {
            for (DProjects project : projects) {
                try {
                    canUserReadProjectElseThrowException(reqObj, project);
                    ProjectDetails details = getProjectSummary(project);
                    orgProjects.addProject(details);
                } catch (Exception e) {
                    //do nothing.
                }
            }
        }
        return orgProjects;
    }

    private static List<DProjects> getTrendingProjects() {
        List<String> projectIds = DBBasedConfigs.getConfig("dTrendingProjects", List.class, Collections.emptyList());
        if (projectIds != null && !projectIds.isEmpty()) {
            List<DProjects> projects = new ArrayList<>();
            for (String projectId : projectIds) {
                DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
                if (project != null) {
                    projects.add(project);
                }
            }
            return projects;
        }
        return null;
    }

    // update the details object with contributor level details like HITS done, average time taken etc.
    public static void addProjectContributorDetails(DProjects project, ProjectDetails details) {
        List<DHitsResult> results = AppConfig.getInstance().getdHitsResultDAO().findAllByProjectIdInternal(project.getId());
        List<DProjectUsers> projectUsers = AppConfig.getInstance().getdProjectUsersDAO().findAllByProjectIdInternal(project.getId());

        Map<String, List<DHitsResult>> contributorMap = new HashMap<>();
        if (results != null) {
            for (DHitsResult result : results) {
                if (!contributorMap.containsKey(result.getUserId())) {
                    contributorMap.put(result.getUserId(), new ArrayList<>());
                }
                contributorMap.get(result.getUserId()).add(result);
            }

            contributorMap = sortByHitsDone(contributorMap);
            for (String userId : contributorMap.keySet()) {
                DUsers user = AppConfig.getInstance().getdUsersDAO().findByIdInternal(userId);
                List<DHitsResult> userHits = contributorMap.get(userId);

                ContributorDetails contributorDetails = new ContributorDetails(new UserDetails(user));
                contributorDetails.setHitsDone(userHits.size());
                contributorDetails.setAvrTimeTakenInSec(getAvrgTimePerHit(userHits));

                //get user role.
                if (projectUsers != null) {
                    for (DProjectUsers projectUser : projectUsers) {
                        if (projectUser.getUserId().equalsIgnoreCase(userId)) {
                            contributorDetails.setRole(projectUser.getRole());
                        }
                    }
                }

                details.addContributorDetails(contributorDetails);
            }
        }

        //also add contributors who might have 0 hits.
        if (projectUsers != null) {
            for (DProjectUsers projectUser : projectUsers) {
                if (!contributorMap.containsKey(projectUser.getUserId())) {
                    DUsers user = AppConfig.getInstance().getdUsersDAO().findByIdInternal(projectUser.getUserId());
                    if (user == null) {
                        continue;
                    }
                    ContributorDetails contributorDetails = new ContributorDetails(new UserDetails(user));
                    contributorDetails.setRole(projectUser.getRole());
                    details.addContributorDetails(contributorDetails);
                }
            }
        }

    }

    // don't show user email etc to non-contributors.
    public static void maskDetails(DReqObj reqObj, DProjects project, ProjectDetails details) {
        if (getProjectUserRole(project.getId(), reqObj.getUid()) == null) {
            for (ContributorDetails contributorDetails : details.getContributorDetails()) {
                contributorDetails.getUserDetails().setEmail(null);
            }
        }
    }

    public static void setPermissionForDownload(DReqObj reqObj, ProjectDetails projectDetails) {
        if (!DUtils.isLoggedInUser(reqObj)) {
            projectDetails.getPermissions().setCanDownloadData(false);
        }
    }

    // if the project org's subscription expired, disable tagging and set expiry flag (flag only in case where labeling was allowed.)
    // so for a public project a non-contributor will never see that the subscription has expired.
    public static void handleSubscriptionExpiry(DReqObj reqObj, ProjectDetails projectDetails) {

        if (projectDetails.getPermissions().isCanCompleteHITs()) {
            if (Validations.hasSubscriptionExpired(projectDetails.getOrgId())) {
                projectDetails.getPermissions().setCanCompleteHITs(false);
                projectDetails.setHasSubscriptionExpired(true);
            }

        }
    }


    private static int getAvrgTimePerHit(List<DHitsResult> userHits) {
        int totalTimeInSec = 0;
        if (userHits != null) {
            for (DHitsResult result : userHits) {
                totalTimeInSec += result.getTimeTakenToLabelInSec();
            }
            return totalTimeInSec / userHits.size();
        }
        return 0;
    }

    private static Map<String, List<DHitsResult>> sortByHitsDone(Map<String, List<DHitsResult>> unsortMap) {
        List<Map.Entry<String, List<DHitsResult>>> list = new LinkedList<Map.Entry<String, List<DHitsResult>>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, List<DHitsResult>>>() {
            public int compare(Map.Entry<String, List<DHitsResult>> o1,
                               Map.Entry<String, List<DHitsResult>> o2) {
                return o2.getValue().size() - o1.getValue().size(); //reverse sorted
            }
        });
        // Maintaining insertion order with the help of LinkedList
        Map<String, List<DHitsResult>> sortedMap = new LinkedHashMap<String, List<DHitsResult>>();
        for (Map.Entry<String, List<DHitsResult>> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }


    private static void updateLastLoginTime(DUsers user) {
        user.setUpdated_timestamp(new Date());
        AppConfig.getInstance().getdUsersDAO().saveOrUpdateInternal(user);
    }

}
