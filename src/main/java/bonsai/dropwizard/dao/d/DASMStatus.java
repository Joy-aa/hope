package bonsai.dropwizard.dao.d;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "d_asm_status")
@NamedQueries(
        @NamedQuery(name = "bonsai.dropwizard.dao.d.DASMStatus.findAll",
                query = "select e from DASMStatus e")
)
public class DASMStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String info;
    private String type;
    private Date startTime;
    private Date endTime;
    private String status;

    @Column(updatable = false)// 不可更新
    @Temporal(TemporalType.TIMESTAMP)
    @CreationTimestamp
    private Date created_timestamp;

    @Temporal(TemporalType.TIMESTAMP)
    @UpdateTimestamp
    private Date updated_timestamp;

    public DASMStatus() {
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreated_timestamp() {
        return created_timestamp;
    }

    public void setCreated_timestamp(Date created_timestamp) {
        this.created_timestamp = created_timestamp;
    }

    public Date getUpdated_timestamp() {
        return updated_timestamp;
    }

    public void setUpdated_timestamp(Date updated_timestamp) {
        this.updated_timestamp = updated_timestamp;
    }
}
