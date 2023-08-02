package bonsai.email;

import bonsai.dropwizard.dao.d.DProjects;
import bonsai.dropwizard.dao.d.DUsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mohan on 20/6/17.
 * 因为没有邮件服务，所以不会真的发邮件(2021-09-22 tcmyxc)
 */
public class EmailSender {

    private static final Logger LOG = LoggerFactory.getLogger(EmailSender.class);

    public static void main(String[] args) {

    }

    public static void sendEventMail(String subject, String content) {
        LOG.info("sendEventMail...");
    }

    public static void sendAppEventMail(String subject, String data) {
        LOG.info("sendAppEventMail...");
    }

    //////////////////////// Dataturks annotation emails ////////////////////////////

    public static void sendDataturksUserFeedbackEmail(DUsers user) {
        LOG.info("sendDataturksUserFeedbackEmail...");
    }

    //when an existing user is added to a project
    public static void sendDataturksUserAddedToProject(DUsers inviter, DUsers user, DProjects project) {
        LOG.info("sendDataturksUserAddedToProject...");
    }

    //when an email is added to a project. Ask user to signup and be part of the project.
    public static void sendDataturksEmailAddedToProject(DUsers inviter, String userEmail, DProjects project) {
        LOG.info("sendDataturksEmailAddedToProject...");
    }
}
