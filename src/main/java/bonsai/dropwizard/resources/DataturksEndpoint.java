package bonsai.dropwizard.resources;


import bonsai.Utils.*;
import bonsai.config.AppConfig;
import bonsai.config.DBBasedConfigs;
import bonsai.dropwizard.dao.d.*;
import bonsai.email.SendEmail;
import bonsai.sa.EventsLogger;
import bonsai.security.LoginAuth;
import bonsai.security.MD5Util;
import dataturks.*;
import dataturks.cache.CacheWrapper;
import dataturks.license.LicenseHandler;
import dataturks.response.*;
import dataturks.security.InternalLoginAuth;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路由处理类
 */
@Path("/dataturks")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class DataturksEndpoint {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DataturksEndpoint.class);

    @POST
    @Path("/createUser")
    public DummyResponse createUser(@NotNull @HeaderParam("token") String token,
                                    @NotNull @HeaderParam("uid") String id,
                                    @NotNull Map<String, String> req) {
        EventsLogger.logEvent("d_newUserSignup");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);
        String regStr = "SaveUser: save user=  " + req.toString();
        LOG.info(regStr);

        try {
            DReqObj reqObj = new DReqObj(id, req);
            return createUserInternal(reqObj);
        } catch (Exception e) {
            LOG.error("Error " + regStr + e.toString());
            EventsLogger.logErrorEvent("d_newUserSignupError");
            throw e;
        }
    }

    /**
     * 用户注册路由
     *
     * @param password 密码
     * @param req      前端传递的注册信息
     * @return LoginResponse（注册成功则进入首页，不成功弹出提示信息）
     */
    @POST
    @Path("/createUserWithPassword")
    public VerificationResponse createUser(@NotNull @HeaderParam("password") String password,
                                           @NotNull Map<String, String> req) {
        EventsLogger.logEvent("d_createUserWithPassword");

        String regStr = "SaveUser: save user=  " + req.toString();
        LOG.info(regStr);

        try {
            String id = InternalLoginAuth.generateUserId();
            String encryptedPassword = InternalLoginAuth.encryptedPassword(password);
            req.put("password", encryptedPassword);
            DReqObj reqObj = new DReqObj(id, req);
            createUserInternal(reqObj);
            DUsers user = AppConfig.getInstance().getdUsersDAO().findByOAuthIdInternal(id);
            if (user.getStatus().equalsIgnoreCase(DConstants.USER_CONFIRMED)) {
                // 发送验证邮件
                // SendEmail.sendConfirmRegistrationEmail(user.getEmail(), user.getOAuthId());
                // return new VerificationResponse(true, "验证邮件已发送，请到注册邮箱点击验证链接！");
                return new VerificationResponse(true, "用户创建成功，等待激活，请联系管理员激活用户");
            } else {
                return new VerificationResponse(false, "注册发生错误，请检查输入信息是否正确");
            }
        } catch (Exception e) {
            LOG.error("Error " + regStr + e);
            EventsLogger.logErrorEvent("d_newUserSignupError");
            throw e;
        }
    }

    /**
     * 注册验证
     *
     * @param authId 被加密过的authId
     * @param email  用户邮箱
     * @return 验证信息
     */
    @GET
    @Path("/confirmRegistration")
    public VerificationResponse confirmRegistration(@NotNull @QueryParam("code") String authId,
                                                    @NotNull @QueryParam("email") String email) {
        EventsLogger.logEvent("d_confirmRegistration");
        DUsers user = AppConfig.getInstance().getdUsersDAO().findByEmailInternal(email);
        if (MD5Util.verify(user.getOAuthId(), authId)) {
            user.setStatus(DConstants.USER_CONFIRMED);// 更新用户状态
            AppConfig.getInstance().getdUsersDAO().saveOrUpdateInternal(user);// 写数据库
            LOG.info("confirmRegistration函数，验证通过！");
            // 跳转到首页（这一步可以让前端做，后端跳路由这个框架不方便）
            String url = DBBasedConfigs.getConfig("clientHost", String.class, "http://10.214.211.151:3030");
            URI uri = UriBuilder.fromUri(url).build();
            Response response = Response.seeOther(uri).build();
            throw new WebApplicationException("验证通过！", response);
        } else {
            LOG.info("confirmRegistration函数，验证失败，请检查！");
            return new VerificationResponse(false, "验证失败，请检查！");
        }
    }

    /**
     * 登录路由
     *
     * @param email    用户邮箱
     * @param password 用户密码
     * @return LoginResponse（成功则进入首页，不成功弹出提示信息）
     */
    @POST
    @Path("/login")
    public LoginResponse login(@NotNull @HeaderParam("email") String email,
                               @NotNull @HeaderParam("password") String password,
                               @NotNull @HeaderParam("encrypted") String encrypted) {

        EventsLogger.logEvent("d_login");

        String regStr = "login: login user email=  " + email + ", encrypted= " + encrypted;
        LOG.info(regStr);

        if (encrypted.equalsIgnoreCase("true")) {
            return loginByEncrypted(email, password);
        }

        try {
            DUsers user = AppConfig.getInstance().getdUsersDAO().findByEmailInternal(email);
            if (user == null) {
                throw new NotAuthorizedException("No such user found");
            }
            if (user.getStatus() == null || user.getStatus().equalsIgnoreCase(DConstants.USER_NOT_CONFIRM)) {
                throw new NotAuthorizedException("用户未验证");
            }
            if (user.getStatus() == null || user.getStatus().equalsIgnoreCase(DConstants.USER_DELETED)) {
                throw new NotAuthorizedException("用户已被删除");
            }
            return loginInternal(user, password);
        } catch (Exception e) {
            LOG.error("Error " + regStr + e.toString());
            EventsLogger.logErrorEvent("d_login");
            throw e;
        }

    }


    /**
     * 使用加密过的密码直接登录
     *
     * @param email             用户邮箱
     * @param encryptedPassword 加密过的密码
     */
    public LoginResponse loginByEncrypted(@NotNull String email, @NotNull String encryptedPassword) {

        EventsLogger.logEvent("d_loginByEncrypted");

        String regStr = "login: login user email=  " + email;
        LOG.info(regStr);
        try {
            DUsers user = AppConfig.getInstance().getdUsersDAO().findByEmailInternal(email);
            if (user == null) {
                throw new NotAuthorizedException("No such user found");
            }
            return loginInternalByEncrypted(user, encryptedPassword);
        } catch (Exception e) {
            LOG.error("Error " + regStr + e.toString());
            EventsLogger.logErrorEvent("d_login");
            throw e;
        }

    }

    /**
     * 重置密码
     *
     * @param email 用户邮箱
     */
    @POST
    @Path("/resetPassword")
    public VerificationResponse resetPassword(@NotNull @HeaderParam("email") String email) {

        EventsLogger.logEvent("d_resetPassword");
        String regStr = "resetPassword: user email=  " + email;
        LOG.info(regStr);
        try {
            DUsers user = AppConfig.getInstance().getdUsersDAO().findByEmailInternal(email);
            if (user == null) {
                return new VerificationResponse(false, "用户不存在");
            }
            SendEmail.sendResetPasswordEmail(user.getEmail(), user.getPassword());// 发邮件
            return new VerificationResponse(true, "重置密码邮件已发送，请查收");
        } catch (Exception e) {
            LOG.error("Error " + regStr + e);
            EventsLogger.logErrorEvent("d_resetPassword");
            throw e;
        }
    }

    /**
     * createProject路由
     *
     * @param req 创建项目需要的各种信息
     * @return DummyResponse（成功或者失败）
     */
    @POST
    @Path("/createProject")
    public DummyResponse createProject(@NotNull @HeaderParam("token") String token,
                                       @NotNull @HeaderParam("uid") String id,
                                       @NotNull Map<String, String> req) {
        EventsLogger.logEvent("d_newProject");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        String regStr = "createProject: " + req.toString();
        LOG.info(regStr);

        // 将项目类型都改为图像分割类型
        if (!req.containsKey("taskType")) {
            req.put("taskType", DTypes.Project_Task_Type.IMAGE_POLYGON_BOUNDING_BOX_V2.toString());
        } else {
            req.replace("taskType", DTypes.Project_Task_Type.IMAGE_POLYGON_BOUNDING_BOX_V2.toString());
        }

        try {
            DReqObj reqObj = new DReqObj(id, req);
            DummyResponse response = createProjectInternal(reqObj);

            //invalidate the cache for the user.
            CacheWrapper.updateProjectCreateDelete(reqObj, reqObj.getOrgId());

            return response;
        } catch (Exception e) {
            LOG.error("Error " + regStr + e.toString());
            EventsLogger.logErrorEvent("d_newProjectError");
            throw e;
        }
    }

    /**
     * deleteProject路由
     *
     * @param projectId 项目id
     * @return DummyResponse（成功或者失败）
     */
    @POST
    @Path("/{projectId}/deleteProject")
    public DummyResponse deleteProject(@NotNull @HeaderParam("token") String token,
                                       @NotNull @HeaderParam("uid") String id,
                                       @NotNull @PathParam("projectId") String projectId) {
        EventsLogger.logEvent("d_deleteProject");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        String regStr = "deleteProject: " + projectId.toString();
        LOG.info(regStr);

        try {
            DReqObj reqObj = new DReqObj(id);
            DummyResponse response = deleteProjectInternal(projectId, reqObj);

            //invalidate the cache for the user.
            CacheWrapper.updateProjectCreateDelete(reqObj, reqObj.getOrgId());
            return response;
        } catch (Exception e) {
            LOG.error("Error " + regStr + e.toString());
            EventsLogger.logErrorEvent("d_deleteProjectError");
            throw e;
        }
    }

    /**
     * updateProject路由
     *
     * @param projectId 项目id
     * @param req       前端传递的项目信息
     * @return DummyResponse（成功或者失败）
     */
    @POST
    @Path("/{projectId}/updateProject")
    public DummyResponse updateProject(@NotNull @HeaderParam("token") String token,
                                       @NotNull @HeaderParam("uid") String id,
                                       @NotNull @PathParam("projectId") String projectId,
                                       @NotNull Map<String, String> req) {
        EventsLogger.logEvent("d_updateProject");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        String regStr = "updateProject: " + projectId.toString();
        LOG.info(regStr);

        try {
            DReqObj reqObj = new DReqObj(id, req);
            DummyResponse response = updateProjectInternal(projectId, reqObj);
            //invalidate the cache for the user.
            CacheWrapper.updateProjectCreateDelete(reqObj, reqObj.getOrgId());
            return response;
        } catch (Exception e) {
            LOG.error("Error " + regStr + e.toString());
            EventsLogger.logErrorEvent("d_updateProjectError");
            throw e;
        }
    }

    @POST
    @Path("/getUserHome")
    public UserHome getUserHome(@NotNull @HeaderParam("token") String token,
                                @NotNull @HeaderParam("uid") String id,
                                @QueryParam("cache") String cache) {


        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);


        //allows us to debug user issues.
        String impersonateId = DUtils.getImpersonatedIdIfAny(id);
        if (impersonateId != null) {
            LOG.info("Impersonating to " + impersonateId + " for the user with id= " + id);
            id = impersonateId;
        }

        boolean cacheEnabled = cache == null;

        String regStr = "getUserHome: " + id;
        LOG.info(regStr);

        UserHome response = null;
        DReqObj reqObj = null;
        try {
            reqObj = new DReqObj(id, null);
            //get from cache.
            if (cacheEnabled) {
                response = CacheWrapper.getUserHome(reqObj);
                if (response != null)
                    return response;
            }

            response = getUserHomeInternal(reqObj);
            //add to cache.
            CacheWrapper.addUserHome(reqObj, response);
            return response;

        } catch (Exception e) {
            if (!DUtils.isNonLoggedInUser(reqObj)) { //don't pollute logs for the non-logedin user case.
                LOG.error("Error " + regStr + e.toString());
                EventsLogger.logErrorEvent("d_getUserHomeError");
                throw e;
            }
        }
        return null;
    }

    /**
     * 获取缩略图
     *
     * @param req 前端传递的参数，主要是图片URL，矩形框的左上角坐标和宽高
     * @return 缩略图的base64字符串
     */
    @POST
    @Path("/getReflectImg")
    public GetReflectImgResponse getReflectImg(@NotNull @HeaderParam("token") String token,
                                               @NotNull @HeaderParam("uid") String id,
                                               @NotNull Map<String, String> req) throws IOException {

        // body 里面的数据在 req 参数里面
        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);// 验证用户身份

        try {
            // 解析前端传过来的参数
            String imgUrl = ThumbnailUtil.getOriginalImgUrl(req.get("imgUrl"));// 根据缩略图找原图的URL
            int x = (int) Double.parseDouble(req.get("xPosition"));
            int y = (int) Double.parseDouble(req.get("yPosition"));
            int width = (int) Double.parseDouble(req.get("width"));
            int height = (int) Double.parseDouble(req.get("height"));


            if (imgUrl.split("/")[imgUrl.split("/").length - 1].split("\\.")[1].equals("mrxs")) {
                String storagePath = CommonUtils.getStoragePath();
                String imgPath = storagePath + imgUrl;
                String mrxsPath = storagePath + imgUrl.split("\\.")[0] + ".mrxs";
                String base64Str = ThumbnailUtil.getMrxsBase64Str(imgPath, mrxsPath, x, y, width, height);
                return new GetReflectImgResponse(base64Str);
            }

            imgUrl = CommonUtils.getOriginalImagePath(imgUrl);
            String base64Str = ThumbnailUtil.getImgBase64Str(imgUrl, x, y, width, height);

            return new GetReflectImgResponse(base64Str);// 返回值必须是一个对象
        } catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e.toString() + " " + CommonUtils.getStackTraceString(e));
            throw e;
        }
    }

    /**
     * 获取标注图
     *
     * @param req 前端传递的参数，主要是图片URL，矩形框的左上角坐标和宽高
     * @return 缩略图的base64字符串
     */
    @POST
    @Path("/getLabelImg")
    public GetReflectImgResponse getLabelImg(@NotNull @HeaderParam("token") String token,
                                               @NotNull @HeaderParam("uid") String id,
                                               @NotNull Map<String, String> req) throws IOException {

        // body 里面的数据在 req 参数里面
        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);// 验证用户身份

        try {
            // 解析前端传过来的参数
            String imgUrl = req.get("imgUrl");// 根据缩略图找原图的URL
            int x = (int) Double.parseDouble(req.get("xPosition"));
            int y = (int) Double.parseDouble(req.get("yPosition"));
            int width = (int) Double.parseDouble(req.get("width"));
            int height = (int) Double.parseDouble(req.get("height"));


            String base64Str = ThumbnailUtil.getImgBase64Str(imgUrl, x, y, width, height);

            return new GetReflectImgResponse(base64Str);// 返回值必须是一个对象
        } catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e.toString() + " " + CommonUtils.getStackTraceString(e));
            throw e;
        }
    }

    /**
     * 根据用户选用的算法以及选中的区域，生成选中部分分割图的mask路径
     *
     * @param req 前端传递的参数，主要是图片URL，矩形框的左上角坐标和宽高以及使用的算法名
     * @return 选中部分分割图像的mask路径
     */
    @POST
    @Path("/getSmartPath")
    public ImageSegmentation getGrabCutImg(@NotNull @HeaderParam("token") String token,
                                           @NotNull @HeaderParam("uid") String id,
                                           @NotNull Map<String, String> req) throws Exception {

        // body 里面的数据在 req 参数里面
        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);// 验证用户身份

        try {
            // 解析前端传过来的参数
            String imgUrl = ThumbnailUtil.getOriginalImgUrl(req.get("imgUrl"));// 根据缩略图找原图的URL
            int x = (int) Double.parseDouble(req.get("xPosition"));
            int y = (int) Double.parseDouble(req.get("yPosition"));
            int width = (int) Double.parseDouble(req.get("width"));
            int height = (int) Double.parseDouble(req.get("height"));
            int thresh = 1;
            // int thresh = (int) Double.parseDouble(req.get("thresh"));
            String algorithm = req.get("algorithm").toLowerCase();
            String imageSegmentationMaskPath = "";
            switch (algorithm) {
                case DConstants.GRAB_CUT:// grabcut
                    int iterCount = (int) Double.parseDouble(req.get("iterCount"));
                    imageSegmentationMaskPath = new GrabCutUtil().getMaskPath(imgUrl, x, y, width, height, iterCount);
                    break;
                case DConstants.CANNY:// 坎尼
                    int threshold1 = (int) Double.parseDouble(req.get("threshold1"));
                    int threshold2 = (int) Double.parseDouble(req.get("threshold2"));
                    imageSegmentationMaskPath = new CannyUtil().getMaskPath(imgUrl, x, y, width, height, threshold1, threshold2);
                    break;
                case DConstants.THRESHOLD:// 阈值处理
                    thresh = (int) Double.parseDouble(req.get("thresh"));
                    int maxVal = (int) Double.parseDouble(req.get("maxVal"));
                    imageSegmentationMaskPath = new ThresholdUtil().getMaskPath(imgUrl, x, y, width, height, thresh, maxVal);
                    break;
                case DConstants.WATER_SHED:// 分水岭
                    imageSegmentationMaskPath = new WaterShedUtil().getMaskPath(imgUrl, x, y, width, height);
                    break;
                case DConstants.RegionGrow:// 区域生长
                    thresh = (int) Double.parseDouble(req.get("thresh"));
                    imageSegmentationMaskPath = new RegionGrowUtil().getMaskPath(imgUrl, x, y, width, height, thresh);
                    break;
                case DConstants.RegionSplitMerge:// 区域分裂合并
                    imageSegmentationMaskPath = new RegionSplitAndMergeUtil().getMaskPath(imgUrl, x, y, width, height);
                    break;
                default:
                    break;
            }
            return new ImageSegmentation(imageSegmentationMaskPath);// 返回值必须是一个对象
        } catch (Exception e) {
            // 记录日志，抛异常
            LOG.error("Error " + e.toString() + " " + CommonUtils.getStackTraceString(e));
            throw e;
        }
    }

    /**
     * 文件上传路由
     *
     * @param projectId    项目的id
     * @param stream       输入的文件流
     * @param uploadFormat 上传文件的格式
     * @return UploadResponse
     */
    @POST
    @Path("/{projectId}/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public UploadResponse fileUpload(@NotNull @HeaderParam("token") String token,
                                     @NotNull @HeaderParam("uid") String id,
                                     @NotNull @PathParam("projectId") String projectId,
                                     @NotNull @FormDataParam("file") InputStream stream,
                                     @HeaderParam("format") String uploadFormat,
                                     @HeaderParam("itemStatus") String itemStatus,
                                     @FormDataParam("file") FormDataContentDisposition fileDetail) {

        EventsLogger.logEvent("d_projectFileUpload");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        String regStr = "projectFileUpload: projectId = " + projectId + " filename = " + fileDetail.getFileName() + " uid = " + id;
        LOG.info(regStr);


        try {
            DReqObj reqObj = new DReqObj(id, null);

            if (uploadFormat != null) {
                Map<String, String> reqMap = new HashMap<>();
                reqMap.put(DConstants.UPLOAD_FORMAT_PARAM_NAME, uploadFormat);
                if (itemStatus != null) {
                    reqMap.put(DConstants.UPLOAD_DATA_STATUS_PARAM_NAME, itemStatus);
                }
                reqObj.setReqMap(reqMap);
            }

            UploadResponse response = handleFileUpload(reqObj, projectId, stream, fileDetail);

            EventsLogger.logEventLine("New file upload:" + regStr +
                    "\n\n" + " Number of records created = " + response.getNumHitsCreated() +
                    "\n\n" + " Number of records ignored = " + response.getNumHitsIgnored() +
                    "\n\n" + " File size (KB) = " + response.getTotalUploadSizeInBytes() / 1024);


            return response;
        } catch (Exception e) {
            LOG.error("Error " + regStr + e.toString() + " " + CommonUtils.getStackTraceString(e));
            EventsLogger.logErrorEvent("d_projectFileUploadError");
            throw e;
        }

    }

    @POST
    @Path("/{projectId}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM + ";charset=utf-8")
    public Response downloadData(@NotNull @HeaderParam("token") String token,
                                 @NotNull @HeaderParam("uid") String id,
                                 @NotNull @PathParam("projectId") String projectId,
                                 @QueryParam("items") String items,
                                 @QueryParam("format") String format) {

        EventsLogger.logEvent("d_downloadData");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        String regStr = "downloadData: " + projectId + " uid = " + id;
        LOG.info(regStr);

        try {
            DReqObj reqObj = new DReqObj(id, null);
            return handleDataDownload(reqObj, projectId, items, format);
        } catch (Exception e) {
            LOG.error("Error " + regStr + e.toString());
            EventsLogger.logErrorEvent("d_downloadDatadError");
            throw e;
        }

    }

    /**
     * getHits路由
     *
     * @param projectId 项目的id
     * @param status    hit状态
     * @param model     模型
     * @return hits信息
     */
    @POST
    @Path("/{projectId}/getHits")
    public GetHits getHits(@NotNull @HeaderParam("token") String token,
                           @NotNull @HeaderParam("uid") String id,
                           @NotNull @PathParam("projectId") String projectId,
                           @QueryParam("status") String status,
                           @QueryParam("userId") String userId,
                           @QueryParam("label") String label,
                           @QueryParam("count") Long count,
                           @QueryParam("start") Long start,
                           @QueryParam("evaluation") String evaluation,
                           @QueryParam("model") String model,
                           @QueryParam("order") String order) {

        EventsLogger.logEvent("d_getHits");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        String regStr = "getHits: project= " + projectId + " status = " + status
                + " label = " + label + " evaluation=" + evaluation
                + " userId= " + userId + " uid = " + id + " model = " + model;
        LOG.info(regStr);

        try {
            DReqObj reqObj = new DReqObj(id, null);// 根据请求的信息构建请求对象
            return getHitsInternal(reqObj, projectId, status, userId, label, evaluation, order, count, start, model);
        } catch (Exception e) {
            LOG.error("Error " + regStr + e.toString());
            EventsLogger.logErrorEvent("d_getHitsError");
            throw e;
        }
    }

    /**
     * addLabel路由
     *
     * @param hitId 图像id
     * @param id   用户id
     * @return hits信息
     */
    @POST
    @Path("/addLabel")
    public GetHits addLabel(@NotNull @HeaderParam("token") String token,
                           @NotNull @HeaderParam("uid") String id,
                           @NotNull @QueryParam("hitId") String hitId) {

        EventsLogger.logEvent("d_addLabel");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);
        
        String regStr = "addLabel: hit= " + hitId;
        LOG.info(regStr);

        try {
            DReqObj reqObj = new DReqObj(id, null);// 根据请求的信息构建请求对象
            return Controlcenter.addLabelById(reqObj, hitId);
            
        } catch (Exception e) {
            LOG.error("Error " + regStr + e.toString());
            EventsLogger.logErrorEvent("d_addLabelError");
            throw e;
        }
    }

    /**
     * addLabels路由
     *
     * @param projectId 项目的id
     * @param id    用户id
     * @return hits信息
     */
    @POST
    @Path("/{projectId}/addLabels")
    public GetHits addLabels(@NotNull @HeaderParam("token") String token,
                           @NotNull @HeaderParam("uid") String id,
                           @NotNull @PathParam("projectId") String projectId,
                           @QueryParam("count") Long count,
                           @QueryParam("start") Long start) {

        EventsLogger.logEvent("d_addLabels");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);
        
        String regStr = "addLabels: project= " + projectId;
        LOG.info(regStr);

        try {
            DReqObj reqObj = new DReqObj(id, null);// 根据请求的信息构建请求对象
            LOG.info("wangjiawangjia"+reqObj);
            return Controlcenter.addLabelsById(reqObj, projectId, count, start);
            
        } catch (Exception e) {
            LOG.error("Error " + regStr + e.toString());
            EventsLogger.logErrorEvent("d_addLabelError");
            throw e;
        }
    }


    /**
     * addHitResult路由
     *
     * @param projectId 项目id
     * @param hitId     图片id
     * @param req       前端传递的其他参数
     * @return DummyResponse（包含必要的信息）
     */
    @POST
    @Path("/{projectId}/addHitResult")
    public DummyResponse addHitResult(@NotNull @HeaderParam("token") String token,
                                      @NotNull @HeaderParam("uid") String id,
                                      @NotNull @PathParam("projectId") String projectId,
                                      @NotNull @QueryParam("hitId") long hitId,
                                      @NotNull Map<String, String> req) {

        EventsLogger.logEvent("d_addHitResult");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        //allows us to debug user issues.
        String impersonateId = DUtils.getImpersonatedIdIfAny(id);
        if (impersonateId != null) {
            LOG.info("Impersonating to " + impersonateId + " for the user with id= " + id);
            id = impersonateId;
        }


        String reqLogStr = "addHitResult: project= " + projectId + " uid = " + id + " hitId = " + hitId;
        LOG.info(reqLogStr);


        try {
            DReqObj reqObj = new DReqObj(id, req);
            return addHitResultInternal(reqObj, projectId, hitId);
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("d_addHitResultError");
            throw e;
        }
    }

    @POST
    @Path("/{projectId}/evaluationResult")
    public DummyResponse addEvaluationResult(@NotNull @HeaderParam("token") String token,
                                             @NotNull @HeaderParam("uid") String id,
                                             @NotNull @PathParam("projectId") String projectId,
                                             @NotNull @QueryParam("hitId") long hitId,
                                             @NotNull Map<String, String> req) {

        EventsLogger.logEvent("d_addEvaluationResult");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        String reqLogStr = "addEvaluationResult: project= " + projectId + " uid = " + id + " hitId = " + hitId;
        LOG.info(reqLogStr);


        try {
            DReqObj reqObj = new DReqObj(id, req);
            return addEvaluationResultInternal(reqObj, projectId, hitId);
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("d_addEvaluationResultError");
            throw e;
        }
    }

    @POST
    @Path("/{projectId}/addContributor")
    public DummyResponse addContributor(@NotNull @HeaderParam("token") String token,
                                        @NotNull @HeaderParam("uid") String id,
                                        @NotNull @PathParam("projectId") String projectId,
                                        @NotNull @QueryParam("userEmail") String userEmail,
                                        @QueryParam("role") String role) {

        EventsLogger.logEvent("d_addContributor");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        //allows us to debug user issues.
        String impersonateId = DUtils.getImpersonatedIdIfAny(id);
        if (impersonateId != null) {
            LOG.info("Impersonating to " + impersonateId + " for the user with id= " + id);
            id = impersonateId;
        }


        String reqLogStr = "addContributor: project= " + projectId + " uid = " + id + " email= " + userEmail + " role=" + role;
        LOG.info(reqLogStr);


        try {
            DReqObj reqObj = new DReqObj(id, null);
            return addContributorInternal(reqObj, projectId, userEmail, role);
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("d_addContributorError");
            throw e;
        }
    }

    @POST
    @Path("/{projectId}/removeContributor")
    public DummyResponse removeContributor(@NotNull @HeaderParam("token") String token,
                                           @NotNull @HeaderParam("uid") String id,
                                           @NotNull @PathParam("projectId") String projectId,
                                           @NotNull @QueryParam("userEmail") String userEmail) {

        EventsLogger.logEvent("d_removeContributor");

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        String reqLogStr = "removeContributor: project= " + projectId + " uid = " + id;
        LOG.info(reqLogStr);


        try {
            DReqObj reqObj = new DReqObj(id, null);
            return removeContributorInternal(reqObj, projectId, userEmail);
        } catch (Exception e) {
            LOG.error("Error  " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("d_removeContributorError");
            throw e;
        }
    }


    /////////////////////////////// Only view operations (may be allowed to non logged-in users) ////////////////////////////////

    /**
     * getProjectDetails路由
     *
     * @param projectId 项目id
     * @return 项目具体细节
     */
    @POST
    @Path("/{projectId}/getProjectDetails")
    public ProjectDetails getProjectDetails(@NotNull @HeaderParam("token") String token,
                                            @NotNull @HeaderParam("uid") String id,
                                            @NotNull @PathParam("projectId") String projectId) {

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        //allows us to debug user issues.
        String impersonateId = DUtils.getImpersonatedIdIfAny(id);
        if (impersonateId != null) {
            LOG.info("Impersonating to " + impersonateId + " for the user with id= " + id);
            id = impersonateId;
        }

        String reqLogStr = "getProjectDetails: project= " + projectId + " uid = " + id;
        LOG.info(reqLogStr);

        try {
            DReqObj reqObj = new DReqObj(id, null);
            return getProjectDetailsInternal(reqObj, projectId);
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("d_getProjectDetailsError");
            throw e;
        }
    }

    @POST
    @Path("/{projectId}/getProjectStats")
    public ProjectStats getProjectStats(@NotNull @HeaderParam("token") String token,
                                        @NotNull @HeaderParam("uid") String id,
                                        @NotNull @PathParam("projectId") String projectId,
                                        @QueryParam("cache") String cache) {

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);
        boolean cacheEnabled = cache == null;
        ProjectStats response = null;
        String reqLogStr = "getProjectStats: project= " + projectId + " uid = " + id;
        LOG.info(reqLogStr);

        try {
            DReqObj reqObj = new DReqObj(id, null);

            //get from cache.
            if (cacheEnabled) {
                response = CacheWrapper.getProjectStats(reqObj, projectId);
                if (response != null)
                    return response;
            }

            response = getProjectStatsInternal(reqObj, projectId);

            CacheWrapper.addProjectStats(reqObj, projectId, response);
            return response;
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("d_getProjectStatsError");
            throw e;
        }
    }

    @POST
    @Path("/getProjectId")
    public DummyResponse getProjectId(@NotNull @HeaderParam("token") String token,
                                      @NotNull @HeaderParam("uid") String id,
                                      @NotNull Map<String, String> req) {

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        String reqLogStr = "getProjectId: uid = " + id + " reg= " + req.toString();
        LOG.info(reqLogStr);

        try {
            DReqObj reqObj = new DReqObj(id, null);
            return DummyResponse.getString(getProjectIdInternal(reqObj, req));
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("d_getProjectId");
            throw e;
        }
    }

    @POST
    @Path("/getOrgProjects")
    public OrgProjects getOrgProjects(@NotNull @HeaderParam("token") String token,
                                      @NotNull @HeaderParam("uid") String id,
                                      @QueryParam("orgId") String orgId,
                                      @QueryParam("orgName") String orgName,
                                      @QueryParam("cache") String cache) {

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        boolean cacheEnabled = cache == null;
        String reqLogStr = "getOrgProjects: uid= " + id.toString() + " orgId = " + orgId + " orgName=" + orgName;
        LOG.info(reqLogStr);

        DReqObj reqObj = null;
        OrgProjects response = null;

        try {
            if (orgId == null && orgName != null) {
                DOrgs orgs = AppConfig.getInstance().getdOrgsDAO().findByNameInternal(orgName);
                if (orgs != null) {
                    orgId = orgs.getId();
                }
            }
            reqObj = new DReqObj(id, null);

            //get from cache.
            if (cacheEnabled) {
                response = CacheWrapper.getOrgProjects(reqObj, orgId);
                if (response != null) {
                    return response;
                }
            }


            response = getOrgProjectsInternal(reqObj, orgId);
            //add to cache.
            CacheWrapper.addOrgProjects(reqObj, orgId, response);
            return response;
        } catch (Exception e) {
            //if an exception occured means something changed and we should unset the cache.
            //may be a public org became paid?
            CacheWrapper.removeOrgProjects(reqObj, orgId);

            LOG.error("Error " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("d_getOrgProjects");
            throw e;
        }
    }

    @POST
    @Path("/getAPIKey")
    public APIKey getOrCreateAPIKey(@NotNull @HeaderParam("token") String token,
                                    @NotNull @HeaderParam("uid") String id) {

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);

        String reqLogStr = "createAPIKey: uid = " + id;
        LOG.info(reqLogStr);

        try {
            DReqObj reqObj = new DReqObj(id, null);
            return getOrCreateAPIKeyInternal(reqObj);
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("d_createAPIKey");
            throw e;
        }
    }

    @POST
    @Path("/getLicense")
    public License getLicense(@NotNull @HeaderParam("key") String key) {


        String reqLogStr = "getLicense: ";
        LOG.info(reqLogStr);

        try {
            return getLicenseInternal(key);
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("getLicense");
            throw e;
        }
    }

    @POST
    @Path("/addLicense")
    public DummyResponse addLicense(@NotNull @HeaderParam("license") String license) {


        String reqLogStr = "addLicense: ";
        LOG.info(reqLogStr);

        try {
            return addLicenseInternal(license);
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("addLicense");
            throw e;
        }
    }

    @POST
    @Path("/getLicenseInfo")
    public LicenseInfo getLicenseInfo() {


        String reqLogStr = "getLicenseInfo: ";
        LOG.info(reqLogStr);

        try {
            return getLicenseInfoInternal();
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("getLicenseInfo");
            throw e;
        }
    }

    @POST
    @Path("/updateAdminPassword")
    public DummyResponse updateAdminPassword(@NotNull @HeaderParam("email") String email,
                                             @NotNull @HeaderParam("password") String newPassword) {

        String reqLogStr = "updateAdminPassword: for email " + email;
        LOG.info(reqLogStr);

        try {
            return updateAdminPasswordInternal(email, newPassword);
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e.toString());
            EventsLogger.logErrorEvent("updateAdminPassword");
            throw e;
        }
    }

    /**
     * 清除自动分割的结果
     *
     * @param projectId 项目id
     * @return 各种数据设置为初始值的对象
     */
    @GET
    @Path("/clear_auto_test")
    public ClearAutoResponse clearAuto(@NotNull @QueryParam("projectId") String projectId) {
        String reqLogStr = "clearAuto: projectId is " + projectId;
        LOG.info(reqLogStr);
//        System.out.println(reqLogStr);
        // 根据 projectId 查询 hitsResults
        List<DHitsResult> hitsResults = AppConfig.getInstance().getdHitsResultDAO().findAllByProjectIdInternal(projectId);

        for (DHitsResult hitsResult : hitsResults) {
            long hitId = hitsResult.getHitId();// 获取 hid
            DHits dHits = AppConfig.getInstance().getdHitsDAO().findById(hitId);// 查询 dHits
            dHits.setStatus("notDone");// 设置状态
            AppConfig.getInstance().getdHitsDAO().saveOrUpdateInternal(dHits);// 写回数据库

            // 清空 d_hit_results
            AppConfig.getInstance().getdHitsResultDAO().deleteByProjectId(hitsResult.getProjectId());
        }

        return new ClearAutoResponse();
    }

    /**
     * 获取器官类型、医学图像格式
     *
     * @param token 用户token
     * @param id    用户id
     * @return 器官类型、医学图像格式
     */
    @GET
    @Path("/getMedicalConfig")
    public DummyResponse getMedicalConfig(@NotNull @HeaderParam("token") String token,
                                          @NotNull @HeaderParam("uid") String id) {

        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);
        String reqLogStr = "getMedicalConfig ==> uid is " + id;
        LOG.info(reqLogStr);
        try {
            // 直接从数据库中查询
            String medicalImageFormat = DBBasedConfigs.getConfig("medicalImageFormat", String.class, null);
            String imageOrganType = DBBasedConfigs.getConfig("imageOrganType", String.class, null);
            return new DummyResponse("{\"medicalImageFormat\": " + medicalImageFormat + ", "
                    + "\"imageOrganType\": " + imageOrganType + "}");
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e);
            EventsLogger.logErrorEvent("getMedicalConfig Error");
            throw e;
        }
    }

    /**
     * 获取所有的公开数据集
     *
     * @param token 用户token
     * @param id    用户id
     * @return 所有的公开数据集
     */
    @GET
    @Path("/getPublicProject")
    public PublicProjectDetail getPublicProject(@NotNull @HeaderParam("token") String token,
                                                @NotNull @HeaderParam("uid") String id) {
        LoginAuth.validateAndGetDataturksUserIdElseThrowException(id, token);
        String reqLogStr = "getPublicProject ==> uid is " + id;
        LOG.info(reqLogStr);
        try {
            List<ProjectDetails> allPublicProjectDetails = new ArrayList<>();// 结果集
            // 查询所有的公开数据集
            List<DProjects> allPublicInternal = AppConfig.getInstance().getdProjectsDAO().findAllPublicInternal();
            for (DProjects project : allPublicInternal) {
                if (project != null) {
                    // 封装到 projectDetails
                    ProjectDetails projectDetails = Controlcenter.getProjectSummary(project);
                    // 添加到结果集
                    allPublicProjectDetails.add(projectDetails);
                }
            }
            return new PublicProjectDetail(allPublicProjectDetails);
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e);
            EventsLogger.logErrorEvent("getPublicProject Error");
            throw e;
        }
    }

    /////////////////////////////// ALL INTERNAL FUNCTIONS /////////////////////////////////////////////////////////


    public static Response handleDataDownload(DReqObj reqObj, String projectId, String items, String formatStr) {
        DTypes.File_Download_Type downloadType = DTypes.File_Download_Type.DONE;
        if (items != null && !items.isEmpty()) {
            downloadType = DTypes.File_Download_Type.valueOf(items);
        }

        DTypes.File_Download_Format format = DTypes.File_Download_Format.ANY;
        if (formatStr != null && !formatStr.isEmpty()) {
            try {
                format = DTypes.File_Download_Format.valueOf(formatStr);
            } catch (Exception e) {
                throw new WebApplicationException("Unkown format type " + formatStr, Response.Status.BAD_REQUEST);
            }
        }

        String filepath = Controlcenter.handleDataDownload(reqObj, projectId, downloadType, format);
        if (filepath != null) {
            java.nio.file.Path path = Paths.get(filepath);
            //show a good filename to user
            String[] parts = filepath.split("____");
            String fileName = parts[parts.length - 1];


            return Response.ok().entity(new StreamingOutput() {
                @Override
                public void write(final OutputStream output) throws IOException, WebApplicationException {
                    try {
                        Files.copy(path, output);
                    } finally {
                        Files.delete(path);
                    }
                }
            }).header("content-disposition", "attachment; filename = " + fileName).build();

        }
        throw new WebApplicationException("Unable to download data", Response.Status.BAD_GATEWAY);
    }

    public static ProjectDetails getProjectDetailsInternal(DReqObj reqObj, String projectId) {
        ProjectDetails projectDetails = Controlcenter.getProjectDetailsInternal(reqObj, projectId);
        return projectDetails;
    }

    public static ProjectStats getProjectStatsInternal(DReqObj reqObj, String projectId) {
        ProjectStats projectStats = Controlcenter.getProjectStatsInternal(reqObj, projectId);
        return projectStats;
    }

    public static String getProjectIdInternal(DReqObj reqObj, Map<String, String> req) {
        if (req.containsKey("orgName") && req.containsKey("projectName")) {
            return Controlcenter.getProjectIdInternal(reqObj, req.get("orgName"), req.get("projectName"));
        }
        throw new WebApplicationException("orgName or projectName not passed", Response.Status.BAD_REQUEST);
    }

    public static DummyResponse addContributorInternal(DReqObj reqObj, String projectId, String userEmail, String roleStr) {
        DTypes.Project_User_Role role = DTypes.Project_User_Role.CONTRIBUTOR;
        try {
            role = (roleStr == null) ? role : DTypes.Project_User_Role.valueOf(roleStr);
        } catch (Exception e) {
            LOG.error("addContributorInternal " + e.toString());
            throw new WebApplicationException("Invalid role", Response.Status.BAD_REQUEST);
        }

        Controlcenter.addContributorInternal(reqObj, projectId, userEmail.trim(), role);
        return DummyResponse.getOk();
    }

    public static DummyResponse removeContributorInternal(DReqObj reqObj, String projectId, String userEmail) {
        Controlcenter.removeContributorInternal(reqObj, projectId, userEmail.trim());
        return DummyResponse.getOk();
    }


    public static DummyResponse addHitResultInternal(DReqObj reqObj, String projectId, long hitId) {
        Controlcenter.addHitResultInternal(reqObj, projectId, hitId);
        return DummyResponse.getOk();
    }

    public static DummyResponse addEvaluationResultInternal(DReqObj reqObj, String projectId, long hitId) {
        Controlcenter.addEvaluationResultInternal(reqObj, projectId, hitId);
        return DummyResponse.getOk();
    }

    public static OrgProjects getOrgProjectsInternal(DReqObj reqObj, String orgId) {
        return Controlcenter.getOrgProjectsInternal(reqObj, orgId);
    }


    // return hits.
    // if the project is private, make sure to check if the user has permission
    // for public and restricted projects we can return the hits.
    // for pagination use start/count (used when accessing hits serially, mostly for viewing all done)
    public static GetHits getHitsInternal(DReqObj reqObj, String projectId, String status,
                                          String userId, String label, String evaluation, String orderByStr,
                                          Long count, Long start, String model) {

        //don't let ppl crawl our APIs.
        int maxHitsToReturn = DBBasedConfigs.getConfig("maxHitsToReturnPerCall", Integer.class, DConstants.MAX_HITS_TO_RETURN);
        if (count == null)
            count = (long) maxHitsToReturn;
        count = Math.min(count, maxHitsToReturn);

        if (start == null)
            start = 0l;

        if (status == null || status.equals(""))
            status = DConstants.HIT_STATUS_ALL;

        // 判断传递的字段是否为空
        userId = (userId == null || userId.isEmpty()) ? null : userId;
        label = (label == null || label.isEmpty()) ? null : label;
        evaluation = (evaluation == null || evaluation.isEmpty()) ? null : evaluation.toUpperCase();
        model = (model == null || model.isEmpty()) ? null : model;

        DTypes.HIT_ORDER_Type orderBy = DTypes.HIT_ORDER_Type.NEW_FIRST;

        return Controlcenter.getHits(reqObj, projectId, status, userId, label, evaluation, count, start, orderBy, model);
    }


    //> make sure the user has permission to upload file
    // Make sure the file is of proper type.
    // process file for creating hits.
    // delete the temp file from disk.
    public static UploadResponse handleFileUpload(DReqObj reqObj, String projectId, InputStream stream, FormDataContentDisposition fileDetail) {

        String uploadedFile = null;
        UploadResponse response = null;
        try {
            // get the config for the org.
            DUtils.setConfigForOrg(reqObj);
            // 放在了临时文件夹里面
            uploadedFile = UploadFileUtil.uploadStreamToFile(reqObj, stream, fileDetail);

            response = Controlcenter.handleFileUpload(reqObj, projectId, uploadedFile);
            return response;
        } finally {
            //delete the temp file.
            if (uploadedFile != null) {
                try {
                    //if the response is null, something wrong happened, lets not delete the file yet,
                    //which will help to analyse this issue
                    if (response != null) {
                        File file = new File(uploadedFile);
                        file.delete();
                    }
                } catch (Exception e) {
                    //do nothing
                }
            }
        }
    }

    private static UserHome getUserHomeInternal(DReqObj reqObj) {
        if (reqObj != null) {
            return Controlcenter.getUserHomeInternal(reqObj);
        }

        return null;
    }

    public static DummyResponse createProjectInternal(DReqObj reqObj) {
        if (reqObj != null && reqObj.getUid() != null &&
                reqObj.getReqMap() != null &&
                reqObj.getReqMap().containsKey("name") &&
                reqObj.getReqMap().containsKey("taskType")) {
            DProjects project = new DProjects(reqObj.getReqMap().get("name"), reqObj.getOrgId());
            // 设置project的字段信息
            updateProjectFromRequest(project, reqObj);
            // 新建project
            String projectId = Controlcenter.createNewProject(project, reqObj);

            EventsLogger.logEventLine("New project created by uid= " + reqObj.getUid() + " " + project.getName() + " Rules= " + project.getTaskRules());
            return DummyResponse.getString(projectId);
        }
        return null;
    }

    public static DummyResponse updateProjectInternal(String projectId, DReqObj reqObj) {
        if (reqObj != null && reqObj.getReqMap() != null) {
            DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
            if (project == null) {
                throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
            }
            updateProjectFromRequest(project, reqObj);
            Controlcenter.updateProject(project, reqObj);

        }
        return DummyResponse.getOk();
    }

    public static DummyResponse deleteProjectInternal(String projectId, DReqObj reqObj) {
        DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
        if (project == null) {
            throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
        }
        Controlcenter.deleteProject(project, reqObj);
        return DummyResponse.getOk();
    }

    private static void updateProjectFromRequest(DProjects project, DReqObj reqObj) {

        // 医学图像格式
        if (reqObj.getReqMap().containsKey("medicalImageFormat")) {
            // 如果为 null 值则设置为空字符串
            String medicalImageFormat = reqObj.getReqMap().getOrDefault("medicalImageFormat", "");
            if (medicalImageFormat == null) {
                medicalImageFormat = "";
            }
            project.setMedicalImageFormat(medicalImageFormat.trim());
        }

        // 图像器官类型
        if (reqObj.getReqMap().containsKey("imageOrganType")) {
            // 如果为 null 值则设置为空字符串
            String imageOrganType = reqObj.getReqMap().getOrDefault("imageOrganType", "");
            if (imageOrganType == null) {
                imageOrganType = "";
            }
            project.setImageOrganType(imageOrganType.trim());
        }

        if (reqObj.getReqMap().containsKey("name")) {
            project.setName(reqObj.getReqMap().get("name").trim());
        }

        if (reqObj.getReqMap().containsKey("taskType")) {
            project.setTaskType(Validations.getProjectTaskType(reqObj.getReqMap().get("taskType")));
        }

        if (reqObj.getReqMap().containsKey("accessType")) {
            project.setAccessType(Validations.getProjectAccessType(reqObj.getReqMap().get("accessType")));
        }

        if (reqObj.getReqMap().containsKey("rules")) {
            project.setTaskRules(reqObj.getReqMap().get("rules"));
        }

        if (reqObj.getReqMap().containsKey("description")) {
            project.setDescription(reqObj.getReqMap().get("description"));
        }

        if (reqObj.getReqMap().containsKey("shortDescription")) {
            project.setShortDescription(reqObj.getReqMap().get("shortDescription"));
        }

        if (reqObj.getReqMap().containsKey("minGoldenHITs")) {
            project.setMinGoldenHITs(CommonUtils.parseLong(reqObj.getReqMap().get("minGoldenHITs"), 0l));
        }

        if (reqObj.getReqMap().containsKey("HITRepeatCount")) {
            project.setHITRepeatCount(CommonUtils.parseLong(reqObj.getReqMap().get("HITRepeatCount"), 0l));
        }

        if (reqObj.getReqMap().containsKey("validateWithGoldenHITs")) {
            project.setValidateWithGoldenHITs(reqObj.getReqMap().containsKey("validateWithGoldenHITs"));
        }
    }


    private static DummyResponse createUserInternal(DReqObj reqObj) {
        if (reqObj != null && reqObj.getUid() != null &&
                reqObj.getReqMap() != null && reqObj.getReqMap().containsKey("email")) {
            Map<String, String> req = reqObj.getReqMap();
            DUsers user = new DUsers(reqObj.getUid(), req.get("email"));
            user.setFirstName(req.get("firstName"));
            user.setSecondName(req.get("secondName"));
            user.setOAuthType(req.get("authType"));
            user.setStatus(DConstants.USER_CONFIRMED);
            if (req.containsKey("password")) {
                user.setPassword(req.get("password"));
            }

            // 创建用户
            Controlcenter.createNewUser(user, reqObj);
//            AppConfig.getInstance().getdUsersDAO().createInternal(user);

        } else {
            throw new WebApplicationException("No email address for the user present.");
        }
        return DummyResponse.getOk();
    }

    private static APIKey getOrCreateAPIKeyInternal(DReqObj reqObj) {
        return Controlcenter.getOrCreateAPIKeyInternal(reqObj);
    }

    ////////////////// On prem function /////////////////////////////

    private LoginResponse loginInternal(DUsers user, String password) {
        if (user == null) {
            throw new NotAuthorizedException("No such user found");
        }
        if (user.getPassword().contentEquals(InternalLoginAuth.encryptedPassword(password))) {
            String token = InternalLoginAuth.generateRandomUserToken();
            InternalLoginAuth.addToken(user.getId(), token);
            return new LoginResponse(user.getId(), token);
        }
        throw new NotAuthorizedException("User email/password doesn't match", Response.Status.BAD_REQUEST);
    }

    /**
     * AIX登录
     *
     * @param:
     * @return:
     */
    private LoginResponse loginInternalByEncrypted(DUsers user, String encryptedPassword) {
        if (user == null) {
            throw new NotAuthorizedException("No such user found");
        }
        if (user.getPassword().contentEquals(encryptedPassword)) {
            String token = InternalLoginAuth.generateRandomUserToken();
            InternalLoginAuth.addToken(user.getId(), token);
            return new LoginResponse(user.getId(), token);
        }
        throw new NotAuthorizedException("User email/password doesn't match", Response.Status.BAD_REQUEST);
    }

    private static DummyResponse addLicenseInternal(String licenseOrKey) {
        if (!DUtils.isOnPremMode()) {
            throw new NotAuthorizedException("You don't have permission for this");
        }

        LicenseHandler.handleAddNewLicense(licenseOrKey);

        return DummyResponse.getOk();
    }

    private static LicenseInfo getLicenseInfoInternal() {
        if (!DUtils.isOnPremMode()) {
            throw new NotAuthorizedException("You don't have permission for this");
        }

        return LicenseHandler.getLicenseInfo();
    }


    private static License getLicenseInternal(String key) {
        if (DUtils.isOnPremMode()) {
            throw new NotAuthorizedException("You don't have permission for this");
        }
        return Controlcenter.getLicenseInternal(key);
    }

    private static DummyResponse updateAdminPasswordInternal(String email, String newPassword) {

        DUsers user = AppConfig.getInstance().getdUsersDAO().findByEmailInternal(email);
        if (user != null) {
            String password = InternalLoginAuth.encryptedPassword(newPassword);
            user.setPassword(password);
            AppConfig.getInstance().getdUsersDAO().saveOrUpdateInternal(user);
        } else {
            throw new WebApplicationException("No user with email " + email + " found.", Response.Status.BAD_REQUEST);
        }

        return DummyResponse.getOk();
    }

    public static class DummyResponse {

        private String response;

        public DummyResponse(String response) {
            this.response = response;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public static DummyResponse getOk() {
            return new DummyResponse("Ok");
        }

        public static DummyResponse getString(String projectId) {
            return new DummyResponse(projectId);
        }
    }
}
