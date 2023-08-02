package dataturks.response;

public class ClearAutoResponse {

    private int total_num;
    private int cur_idx;
    private int progress;
    private int sl_num;
    private int al_num;
    private String sl_img_src;
    private String al_img_src;

    public ClearAutoResponse() {
        total_num = 0;
        cur_idx = 0;
        progress = 0;
        sl_num = 0;
        al_num = 0;
        sl_img_src = "";
        al_img_src = "";
    }

    public ClearAutoResponse(int total_num, int cur_idx, int progress, int sl_num, int al_num, String sl_img_src, String al_img_src) {
        this.total_num = total_num;
        this.cur_idx = cur_idx;
        this.progress = progress;
        this.sl_num = sl_num;
        this.al_num = al_num;
        this.sl_img_src = sl_img_src;
        this.al_img_src = al_img_src;
    }

    public int getTotal_num() {
        return total_num;
    }

    public void setTotal_num(int total_num) {
        this.total_num = total_num;
    }

    public int getCur_idx() {
        return cur_idx;
    }

    public void setCur_idx(int cur_idx) {
        this.cur_idx = cur_idx;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getSl_num() {
        return sl_num;
    }

    public void setSl_num(int sl_num) {
        this.sl_num = sl_num;
    }

    public int getAl_num() {
        return al_num;
    }

    public void setAl_num(int al_num) {
        this.al_num = al_num;
    }

    public String getSl_img_src() {
        return sl_img_src;
    }

    public void setSl_img_src(String sl_img_src) {
        this.sl_img_src = sl_img_src;
    }

    public String getAl_img_src() {
        return al_img_src;
    }

    public void setAl_img_src(String al_img_src) {
        this.al_img_src = al_img_src;
    }

    @Override
    public String toString() {
        return "ClearAutoResponse{" +
                "total_num=" + total_num +
                ", cur_idx=" + cur_idx +
                ", progress=" + progress +
                ", sl_num=" + sl_num +
                ", al_num=" + al_num +
                ", sl_img_src='" + sl_img_src + '\'' +
                ", al_img_src='" + al_img_src + '\'' +
                '}';
    }
}
