package bonsai.dropwizard.core;


import bonsai.config.AppConfig;
import bonsai.config.DBBasedConfigs;
import bonsai.dropwizard.DbConfig;
import bonsai.dropwizard.MetricsRequestFilter;
import bonsai.dropwizard.dao.DBConfigEntry;
import bonsai.dropwizard.dao.DBConfigEntryDAO;
import bonsai.dropwizard.dao.KeyValueItem;
import bonsai.dropwizard.dao.KeyValueItemDAO;
import bonsai.dropwizard.dao.d.*;
import bonsai.dropwizard.resources.Admin;
import bonsai.dropwizard.resources.DataturksEndpoint;
import com.codahale.metrics.servlets.MetricsServlet;
import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

/**
 * Created by mohan.gupta on 04/04/17.
 */
public class MainApp extends Application<DbConfig> {

    /**
     * Hibernate bundle.新建一个 Hibernate 实例
     */
    private final HibernateBundle<DbConfig> hibernateBundle = new HibernateBundle<DbConfig>(
            DBConfigEntry.class,
            KeyValueItem.class,
            DHits.class,
            DHitsResult.class,
            DOrgs.class,
            DOrgUsers.class,
            DProjects.class,
            DProjectUsers.class,
            DSubscriptions.class,
            DSubscriptionPlans.class,
            DUsers.class,
            DProjectInvites.class,
            DAPIKeys.class,
            DLicense.class,
            // 20210924 新加
            DASMStatus.class
    ) {
        @Override
        public DataSourceFactory getDataSourceFactory(DbConfig configuration) {
            return configuration.getDataSourceFactory();
        }
    };

    // 必须有 main 方法标记这是个主类
    public static void main(String[] args) throws Exception {
        new MainApp().run(args);
    }

    @Override
    public String getName() {
        return "vipa-dataturks";
    }

    @Override
    public void initialize(Bootstrap<DbConfig> bootstrap) {
        // 在 Bootstrap 注册 hibernate
        bootstrap.addBundle(hibernateBundle);
    }

    @Override
    public void run(DbConfig configuration, Environment environment) {

        new EnableCors(environment).insecure();// 配置跨域

        // 下面是各种 dao 实例
        final DBConfigEntryDAO dbConfigEntryDAO = new DBConfigEntryDAO(hibernateBundle.getSessionFactory());
        final KeyValueItemDAO keyValueItemDAO = new KeyValueItemDAO(hibernateBundle.getSessionFactory());

        //Dataturks annotation initi.
        final DHitsDAO dHitsDAO = new DHitsDAO(hibernateBundle.getSessionFactory());
        final DHitsResultDAO dHitsResultDAO = new DHitsResultDAO(hibernateBundle.getSessionFactory());
        final DOrgsDAO dOrgsDAO = new DOrgsDAO(hibernateBundle.getSessionFactory());
        final DOrgUsersDAO dOrgUsersDAO = new DOrgUsersDAO(hibernateBundle.getSessionFactory());
        final DProjectsDAO dProjectsDAO = new DProjectsDAO(hibernateBundle.getSessionFactory());
        final DProjectUsersDAO dProjectUsersDAO = new DProjectUsersDAO(hibernateBundle.getSessionFactory());
        final DSubscriptionPlansDAO dSubscriptionPlansDAO = new DSubscriptionPlansDAO(hibernateBundle.getSessionFactory());
        final DSubscriptionsDAO dSubscriptionsDAO = new DSubscriptionsDAO(hibernateBundle.getSessionFactory());
        final DUsersDAO dUsersDAO = new DUsersDAO(hibernateBundle.getSessionFactory());
        final DProjectInvitesDAO dProjectInvitesDAO = new DProjectInvitesDAO(hibernateBundle.getSessionFactory());
        final DAPIKeysDAO dapiKeysDAO = new DAPIKeysDAO(hibernateBundle.getSessionFactory());
        final DLicenseDAO dLicenseDAO = new DLicenseDAO(hibernateBundle.getSessionFactory());
        // 20210924 新加
        final DASMStatusDAO dasmStatusDAO = new DASMStatusDAO(hibernateBundle.getSessionFactory());


        // bazaar initialization
        AppConfig.getInstance().setDbConfigEntryDAO(dbConfigEntryDAO);
        AppConfig.getInstance().setKeyValueItemDAO(keyValueItemDAO);

        // Dataturks init.
        AppConfig.getInstance().setdHitsDAO(dHitsDAO);
        AppConfig.getInstance().setdHitsResultDAO(dHitsResultDAO);
        AppConfig.getInstance().setdOrgsDAO(dOrgsDAO);
        AppConfig.getInstance().setdOrgUsersDAO(dOrgUsersDAO);
        AppConfig.getInstance().setdProjectsDAO(dProjectsDAO);
        AppConfig.getInstance().setdProjectUsersDAO(dProjectUsersDAO);
        AppConfig.getInstance().setdSubscriptionPlansDAO(dSubscriptionPlansDAO);
        AppConfig.getInstance().setdSubscriptionsDAO(dSubscriptionsDAO);
        AppConfig.getInstance().setdUsersDAO(dUsersDAO);
        AppConfig.getInstance().setdProjectInvitesDAO(dProjectInvitesDAO);
        AppConfig.getInstance().setDapiKeysDAO(dapiKeysDAO);
        AppConfig.getInstance().setdLicenseDAO(dLicenseDAO);
        // 20210924 新加
        AppConfig.getInstance().setDasmStatusDAO(dasmStatusDAO);

        initWholeApp(configuration);

        //Dropwizar specific.
        final Admin admin = new Admin();

        //Dataturks endpoint
        final DataturksEndpoint dataturksEndpoint = new DataturksEndpoint();
        environment.jersey().register(dataturksEndpoint);

        environment.jersey().register(admin);
        environment.jersey().register(MultiPartFeature.class);
        environment.servlets().addFilter("MetricsServletFilter", new MetricsRequestFilter())
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

        environment.servlets().addServlet("metrics-servlet", new MetricsServlet(AppConfig.getInstance().getMetrics())).addMapping("/metrics");

    }

    private void initWholeApp(DbConfig configuration) {
        // All App init calls.
        AppConfig.getInstance().init(configuration);
        DBBasedConfigs.getInstance().init(AppConfig.getInstance().getDbConfigEntryDAO());
    }

    // 配置跨域
    protected class EnableCors {
        private Environment environment;

        public EnableCors(Environment environment) {
            this.environment = environment;
        }

        public void insecure() {
            // Enable CORS headers
            final FilterRegistration.Dynamic cors =
                    environment.servlets().addFilter("CORS", CrossOriginFilter.class);

            // Configure CORS parameters
            cors.setInitParameter("allowedOrigins", "*");// 允许前端跨域
            // 配置头里面允许跨域的字段：If the value is a single "*", this means that any headers will be accepted.
            cors.setInitParameter("allowedHeaders", "*");
            cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");
            cors.setInitParameter("Access-Control-Allow-Origin", "*");// 允许跨域的主机地址
            cors.setInitParameter("Access-Control-Allow-Methods", "*");
            cors.setInitParameter("Access-Control-Allow-Headers", "*");

            // Add URL mapping
            cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
            cors.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, Boolean.FALSE.toString());
        }
    }
}
