package dataturks.response;

import bonsai.Utils.CommonUtils;
import bonsai.Utils.ThumbnailUtil;
import bonsai.config.AppConfig;
import bonsai.dropwizard.dao.d.DHits;
import bonsai.dropwizard.dao.d.DHitsResult;
import bonsai.dropwizard.dao.d.DProjects;
import dataturks.DTypes;
import dataturks.DUtils;
import bonsai.Constants;

import java.util.ArrayList;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opencv.core.*;
import static org.opencv.imgcodecs.Imgcodecs.imread;

public class GetHits {
    private static final Logger LOG = LoggerFactory.getLogger(GetHits.class);

    public ProjectDetails projectDetails;
    public List<SingleHit> hits;

    public GetHits() {
        this.hits = new ArrayList<>();
    }


    public void addRelevantProjectDetails(DProjects project) {
        if (project != null) {
            this.projectDetails = new ProjectDetails(project.getId(), project.getName());
            projectDetails.setTask_type(project.getTaskType());
            projectDetails.setAccess_type(project.getAccessType());
            projectDetails.setTaskRules(project.getTaskRules());
        }
    }

    // 数据集+模型：一个数据集，使用了不同的模型，应该分别统计对应模型下的信息
    public void addRelevantProjectDetails(DProjects project, String model) {
        if (project != null) {
            this.projectDetails = new ProjectDetails(project.getId(), project.getName());
            projectDetails.setTask_type(project.getTaskType());
            projectDetails.setAccess_type(project.getAccessType());
            projectDetails.setTaskRules(project.getTaskRules());

            // 查询一些统计信息
            // 这个总数是整个数据集的图片，所以应该从 hits 中查询
            long totalHits = AppConfig.getInstance().getdHitsDAO().getCountForProject(project.getId());
            // 下面的是具体某个模型下的统计信息，需要从 hits_result 中查询
            long totalDone = AppConfig.getInstance().getdHitsResultDAO().getCountForProjectDone(project.getId(), model);
            long totalSkipped = AppConfig.getInstance().getdHitsResultDAO().getCountForProjectSkipped(project.getId(), model);
            long totalDeleted = AppConfig.getInstance().getdHitsResultDAO().getCountForProjectDeleted(project.getId(), model);

//            long totalEvaluationCorrect = AppConfig.getInstance().getdHitsDAO().getCountForProjectEvaluationCorrect(project.getId());
//            long totalEvaluationInCorrect = AppConfig.getInstance().getdHitsDAO().getCountForProjectEvaluationInCorrect(project.getId());

            projectDetails.setTotalHits(totalHits);
            projectDetails.setTotalHitsDone(totalDone);
            projectDetails.setTotalHitsSkipped(totalSkipped);
            projectDetails.setTotalHitsDeleted(totalDeleted);

//            projectDetails.setTotalEvaluationCorrect(totalEvaluationCorrect);
//            projectDetails.setTotalEvaluationInCorrect(totalEvaluationInCorrect);
        }
    }

    public void addSigleHit(DHits hit, List<DHitsResult> results) {
        if (hit != null) {
            SingleHit singleHit = new SingleHit(hit.getId(), hit.getData(), hit.getExtras());
            singleHit.setURL(hit.isURL());
            // 解决awt报错问题
            System.setProperty("java.awt.headless", "true");
            // 加载动态库
            System.load(Constants.OPENCV_LIB);
            // LOG.info("hit.getData:" + hit.getData());
            String originUrl = ThumbnailUtil.getOriginalImgUrl(hit.getData());
            String imgUrl = CommonUtils.getOriginalImagePath(originUrl);
            // LOG.info("imgUrl" + imgUrl);
            Mat srcImg = imread(imgUrl, 0);
            if (srcImg.empty()) {
                singleHit.setImgHeight(0);
                singleHit.setImgWidth(0);
            }
            else{
                singleHit.setImgHeight((int)srcImg.size().height);
                singleHit.setImgWidth((int)srcImg.size().width);
            }
//            singleHit.setStatus(hit.getStatus());
            if (DUtils.isProjectWithURLs(projectDetails) || hit.isURL() ) {
                singleHit.setFileName(DUtils.getURLFilename(hit));
            }
            singleHit.setEvaluation(getHitEvaluationDisplay(hit));
            singleHit.setNotes(hit.getNotes());
            singleHit.addHitResults(results);
            singleHit.setCorrectResult(hit.getCorrectResult());
            this.hits.add(singleHit);
        }
    }

    //only return a valid display value.
    private static String getHitEvaluationDisplay(DHits hit) {
        if (hit != null) {
            if (DTypes.HIT_Evaluation_Type.NONE != hit.getEvaluationType()) {
                return hit.getEvaluationType().toString();
            }
        }
        return null;
    }

}