package dataturks.response;

public class ImageSegmentation {

    private String maskPath;

    public ImageSegmentation() {
    }

    public ImageSegmentation(String maskPath) {
        this.maskPath = maskPath;
    }

    public String getMaskPath() {
        return maskPath;
    }

    public void setMaskPath(String maskPath) {
        this.maskPath = maskPath;
    }

    @Override
    public String toString() {
        return "ImageSegmentation{" +
                "maskPath='" + maskPath + '\'' +
                '}';
    }
}
