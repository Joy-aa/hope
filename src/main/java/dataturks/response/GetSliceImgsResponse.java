package dataturks.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GetSliceImgsResponse {
    public List<String> imgUrls;
    public List<String> labelUrls;
    public List<String> prelabelUrls;
    public List<Integer> x1List;
    public List<Integer> y1List;
    public List<Integer> x2List;
    public List<Integer> y2List;

    public GetSliceImgsResponse() {
        this.imgUrls = new ArrayList<>();
        this.labelUrls = new ArrayList<>();
        this.prelabelUrls = new ArrayList<>();
        this.x1List = new ArrayList<>();
        this.x2List = new ArrayList<>();
        this.y1List = new ArrayList<>();
        this.y2List = new ArrayList<>();
    }
}
