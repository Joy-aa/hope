//package dataturks.response;
//
//import bonsai.dropwizard.dao.d.DHitsResult;
//import com.google.gson.JsonObject;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class NewSingleHit {
//    public long id;
//    public int imgWidth;
//    public int imgHeight;
//    public String data;
//    public String extras;
//    public boolean isURL;
//    //    public String status;
//    public String  fileName;
//    public List<HitResult> hitResults;
//    public JsonObject notes;
//    public String evaluation;
//    public String correctResult;
//
//    public NewSingleHit(){
//
//    }
//
//    public SingleHit(long hitId, String data) {
//        this.id = hitId;
//        this.data = data;
//    }
//
//    public SingleHit(long hitId, String data, String extras) {
//        this(hitId, data);
//        this.extras = extras;
//    }
//
//    public long getId() {
//        return id;
//    }
//
//    public void setId(long id) {
//        this.id = id;
//    }
//
//    public long getImgHeight() {
//        return imgHeight;
//    }
//
//    public void setImgHeight(int imgHeight) {
//        this.imgHeight = imgHeight;
//    }
//
//    public long getImgWidth() {
//        return imgWidth;
//    }
//
//    public void setImgWidth(int imgWidth) {
//        this.imgWidth = imgWidth;
//    }
//
//    public String getData() {
//        return data;
//    }
//
//    public void setData(String data) {
//        this.data = data;
//    }
//
//    public String getExtras() {
//        return extras;
//    }
//
//    public void setExtras(String extras) {
//        this.extras = extras;
//    }
//
//    public boolean isURL() {
//        return isURL;
//    }
//
//    public void setURL(boolean URL) {
//        isURL = URL;
//    }
//
//    public String getNotes() {
//        return notes;
//    }
//
//    public void setNotes(String notes) {
//        this.notes = notes;
//    }
//
//    public String getFileName() {
//        return fileName;
//    }
//
//    public void setFileName(String fileName) {
//        this.fileName = fileName;
//    }
//
//    public String getEvaluation() {
//        return evaluation;
//    }
//
//    public void setEvaluation(String evaluation) {
//        this.evaluation = evaluation;
//    }
//
//    public List<HitResult> getHitResults() {
//        return hitResults;
//    }
//
//    public void setHitResults(List<HitResult> hitResults) {
//        this.hitResults = hitResults;
//    }
//
//    public String getCorrectResult() {
//        return correctResult;
//    }
//
//    public void setCorrectResult(String correctResult) {
//        this.correctResult = correctResult;
//    }
//
//    public void addHitResults(List<DHitsResult> results) {
//        if (results != null && !results.isEmpty()) {
//            for (DHitsResult result : results) {
//                addHitResult(result);
//            }
//        }
//    }
//
//    public void addHitResult(DHitsResult result) {
//        if (result != null) {
//            if (hitResults == null) hitResults = new ArrayList<>();
//
//            HitResult hitResult = new HitResult(result.getId());
//            hitResult.setResult(result.getResult());
//            hitResult.setUserId(result.getUserId());
//            hitResult.setTimeTakenToLabelInSec(result.getTimeTakenToLabelInSec());
//            hitResult.setCreatedTimestamp(result.getCreated_timestamp());
//            hitResult.setUpdatedTimestamp(result.getUpdated_timestamp());
//            hitResult.setNotes(result.getNotes());
//            hitResult.setPredLabel(result.getPredLabel());// 添加标签
//            hitResult.setModel(result.getModel());// 添加所属模型
//            hitResult.setStatus(result.getStatus());
//            hitResults.add(hitResult);
//        }
//    }
//}
