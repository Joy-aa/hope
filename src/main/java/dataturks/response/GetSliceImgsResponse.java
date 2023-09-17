package dataturks.response;

import lombok.Data;

import java.util.List;

@Data
public class GetSliceImgsResponse {
    public String url;
    public List<Integer> xList;
    public List<Integer> yList;
    public List<Integer> widthList;
    public List<Integer> heightList;

}
