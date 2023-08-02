package dataturks.response;

public class VerificationResponse {

    private Boolean success;
    private String msg;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public VerificationResponse(Boolean success, String msg) {
        this.success = success;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "VerificationResponse{" +
                "success=" + success +
                ", msg='" + msg + '\'' +
                '}';
    }
}
