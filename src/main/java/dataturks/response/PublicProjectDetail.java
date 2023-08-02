package dataturks.response;

import java.util.List;

/**
 * PublicProjectDetail
 *
 * @Author: tcmyxc(徐文祥)
 * @Date: 2021/11/24 19:51
 */
public class PublicProjectDetail {

    private List<ProjectDetails> allPublicProjectDetail;

    public PublicProjectDetail() {
    }

    public PublicProjectDetail(List<ProjectDetails> allPublicProjectDetail) {
        this.allPublicProjectDetail = allPublicProjectDetail;
    }

    public List<ProjectDetails> getAllPublicProjectDetail() {
        return allPublicProjectDetail;
    }

    public void setAllPublicProjectDetail(List<ProjectDetails> allPublicProjectDetail) {
        this.allPublicProjectDetail = allPublicProjectDetail;
    }
}
