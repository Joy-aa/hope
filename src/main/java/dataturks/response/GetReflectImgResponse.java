package dataturks.response;

public class GetReflectImgResponse {

    private String imgStr;

    public GetReflectImgResponse(String imgStr) {
        this.imgStr = imgStr;
    }

    public GetReflectImgResponse() {
    }

    public String getImgStr() {
        return imgStr;
    }

    public void setImgStr(String imgStr) {
        this.imgStr = imgStr;
    }
}
