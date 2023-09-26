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

import java.io.*;
import java.util.ArrayList;
import java.util.List;


import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opencv.core.*;

import javax.validation.constraints.Null;

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
            SingleHit singleHit = new SingleHit(hit.getId(), hit.getData());
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
            try{
                String labelUrl = hit.getNotes();
                if(labelUrl == null || labelUrl.isEmpty())
                    singleHit.setNotes(labelUrl);
                else {
                    String jsonLabelPath = CommonUtils.getOriginalLabelPath(hit.getNotes());
                    File file = new File(jsonLabelPath);
                    FileReader fileReader = new FileReader(file);
                    Reader reader = new InputStreamReader(new FileInputStream(file),"Utf-8");
                    int ch = 0;
                    StringBuffer sb = new StringBuffer();
                    while ((ch = reader.read()) != -1) {
                        sb.append((char) ch);
                    }
                    fileReader.close();
                    reader.close();
                    String jsonStr = sb.toString();
                    JSONObject object = JSONObject.fromObject(jsonStr); //创建Json对象
                    singleHit.setNotes(object.toString());
                }

                String preLabelUrl = hit.getExtras();
                if(preLabelUrl == null || preLabelUrl.isEmpty()) {
                    singleHit.setExtras(preLabelUrl);
                }
                else {
                    String jsonPreLabelPath = CommonUtils.getOriginalPreLabelPath(hit.getExtras());
                    LOG.info("prelabel:"+hit.getExtras());
                    File file1 = new File(jsonPreLabelPath);
                    FileReader fileReader1 = new FileReader(file1);
                    Reader reader1 = new InputStreamReader(new FileInputStream(file1),"utf-8");
                    int ch = 0;
                    StringBuffer stringBuffer = new StringBuffer();
                    while((ch = reader1.read()) != -1) {
                        stringBuffer.append((char) ch);
                    }
                    fileReader1.close();
                    reader1.close();
                    String jsonStr1 = stringBuffer.toString();
                    JSONObject object1 = JSONObject.fromObject(jsonStr1);
                    singleHit.setExtras(object1.toString());
                }

            }
            catch (Exception e) {
                LOG.error("Error Gethits" + " " + CommonUtils.getStackTraceString(e));
            }
//            singleHit.setNotes(hit.getNotes());
            singleHit.addHitResults(results);
            singleHit.setCorrectResult(hit.getCorrectResult());
            this.hits.add(singleHit);
        }
    }

    public void delNotes() {
        for(SingleHit hit: this.hits) {
            hit.setNotes(null);
        }
    }

    public void delExtras() {
        for(SingleHit hit: this.hits) {
            hit.setExtras(null);
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
