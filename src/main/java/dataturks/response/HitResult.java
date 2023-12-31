package dataturks.response;


import java.util.Date;

public class HitResult {
    private long id;
    private String userId;
    private String result;
    private int timeTakenToLabelInSec;
    private java.util.Date createdTimestamp;
    private java.util.Date updatedTimestamp;
    private String notes;
    private String predLabel;
    private String model;
    private String status;

    public HitResult(){}

    public HitResult(long id) {
        this();
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getTimeTakenToLabelInSec() {
        return timeTakenToLabelInSec;
    }

    public void setTimeTakenToLabelInSec(int timeTakenToLabelInSec) {
        this.timeTakenToLabelInSec = timeTakenToLabelInSec;
    }

    public Date getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public Date getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    public void setUpdatedTimestamp(Date updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public String getPredLabel() {
        return predLabel;
    }

    public void setPredLabel(String predLabel) {
        this.predLabel = predLabel;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
