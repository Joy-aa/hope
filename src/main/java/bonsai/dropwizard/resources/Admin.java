package bonsai.dropwizard.resources;


import bonsai.DB.Refresher;
import bonsai.config.AppConfig;
import bonsai.dropwizard.dao.d.DProjects;
import bonsai.sa.EventsLogger;
import dataturks.response.GetHits;
import dataturks.response.ProjectDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static bonsai.dropwizard.resources.DataturksEndpoint.getHitsInternal;
import static dataturks.Controlcenter.getProjectSummary;


/**
 * Created by mohan.gupta on 18/04/17.
 */
@Path("/dataturks")
@Produces(MediaType.APPLICATION_JSON)
public class Admin {

    private static final Logger LOG = LoggerFactory.getLogger(Admin.class);

    @GET
    @Path("/refresh")
    public Response refresh() {
        try {
            Refresher.refresh();
        } catch (Exception e) {
            LOG.error(e.toString());
            throw new WebApplicationException("Internal Server error occured");
        }

        return Response.ok().entity("OK").build();
    }

    /**
     * getProjectDetails路由
     *
     * @param projectId 项目id
     * @return 项目具体细节
     */
    @POST
    @Path("/getProjectDetails/{projectId}")
    public ProjectDetails getProjectDetails(@NotNull @PathParam("projectId") String projectId) {

        String reqLogStr = "admin ==> getProjectDetails: project= " + projectId;
        LOG.info(reqLogStr);

        try {
            return getProjectDetailsInternal(projectId);
        } catch (Exception e) {
            LOG.error("Error " + reqLogStr + e);
            EventsLogger.logErrorEvent("admin ==> d_getProjectDetailsError");
            throw e;
        }
    }

    /**
     * getHits路由
     *
     * @param projectId 项目的id
     * @param status    hit状态
     * @param model     模型
     * @return hits信息
     */
    @POST
    @Path("/getHits/{projectId}")
    public GetHits getHits(@NotNull @PathParam("projectId") String projectId,
                           @QueryParam("status") String status,
                           @QueryParam("userId") String userId,
                           @QueryParam("label") String label,
                           @QueryParam("count") Long count,
                           @QueryParam("start") Long start,
                           @QueryParam("evaluation") String evaluation,
                           @QueryParam("model") String model,
                           @QueryParam("order") String order) {

        EventsLogger.logEvent("admin ==> d_getHits");

        String regStr = "admin ==> getHits: project= " + projectId + " status = " + status
                + " label = " + label + " evaluation=" + evaluation
                + " userId= " + userId + " model = " + model;
        LOG.info(regStr);

        try {
            return getHitsInternal(null, projectId, status, userId, label, evaluation, order, count, start, model);
        } catch (Exception e) {
            LOG.error("Error " + regStr + e.toString());
            EventsLogger.logErrorEvent("admin ==> d_getHitsError");
            throw e;
        }
    }

    //===============================================================================================//
    private ProjectDetails getProjectDetailsInternal(String projectId) {
        DProjects project = AppConfig.getInstance().getdProjectsDAO().findByIdInternal(projectId);
        if (project == null) {
            throw new WebApplicationException("No such project found", Response.Status.NOT_FOUND);
        }
        // 获取项目信息
        return getProjectSummary(project);
    }
}
