package bonsai.dropwizard.dao.d;

import dataturks.DConstants;
import dataturks.DTypes;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.*;
import org.hibernate.context.internal.ManagedSessionContext;

import java.util.List;

public class DHitsDAO extends AbstractDAO<DHits> implements IDDao<DHits>{

    /**
     * Constructor.
     *
     * @param sessionFactory Hibernate session factory.
     */
    SessionFactory sessionFactory ;
    public DHitsDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
        this.sessionFactory = sessionFactory;
    }

    public List<DHits> findAll() {
        return list(namedQuery("bonsai.dropwizard.dao.d.DHits.findAll"));
    }

    //Called from within the app and not via a hibernate resources hence does not have session binding.
    public List<DHits> findAllInternal() {
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            Transaction transaction = session.beginTransaction();
            try {
                List<DHits> list= findAll();
                transaction.commit();
                return list;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    public String create(DHits entry) {
        return persist(entry).getId() + "";
    }

    public String createInternal(DHits entry) {
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            Transaction transaction = session.beginTransaction();
            try {
                String  id = create(entry);
                transaction.commit();
                return id;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }


    public DHits findById(long id) {

        List<DHits> list =  list(
                namedQuery("bonsai.dropwizard.dao.d.DHits.findById")
                        .setParameter("id", id)
        );

        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Deprecated
    public DHits findByIdInternal(String id) {
        return findByIdInternal(Long.parseLong(id));
    }

    public DHits findByIdInternal(long id) {
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            Transaction transaction = session.beginTransaction();
            try {
                DHits DHits = findById(id);
                transaction.commit();
                return DHits;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }

    }

    public boolean deleteInternal(DHits DHits) {
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            Transaction transaction = session.beginTransaction();
            try {
                session.delete(DHits);
                transaction.commit();
                return true;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    public boolean saveOrUpdateInternal(DHits DHits) {
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            Transaction transaction = session.beginTransaction();
            try {
                session.saveOrUpdate(DHits);
                transaction.commit();
                return true;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    private List<DHits> findAllByProjectId(String projectId) {

        List<DHits> list =  list(
                namedQuery("bonsai.dropwizard.dao.d.DHits.findByProjectId")
                        .setParameter("projectId", projectId)
        );

        return list;
    }

    public List<DHits> findAllByProjectIdInternal(String projectId) {
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            Transaction transaction = session.beginTransaction();
            try {
                List<DHits> results = findAllByProjectId(projectId);
                transaction.commit();
                return results;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }

    }

    private long getCountForProjectForStatus(String projectId, String status) {
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            org.hibernate.Transaction transaction = session.beginTransaction();
            try {
                Query query = null;
                if (status == null) {
                     query = session.createQuery("select count(*) from DHits e where e.projectId=:projectId");
                }
                else {
                     query = session.createQuery("select count(*) from DHits e where e.projectId=:projectId AND e.status=:status");
                     query.setParameter("status", status);
                }

                query.setParameter("projectId", projectId);
                Long count = (Long)query.uniqueResult();
                transaction.commit();
                return count != null? count : 0;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    private long getCountForProjectForEvaluation(String projectId, DTypes.HIT_Evaluation_Type evaluation) {
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            org.hibernate.Transaction transaction = session.beginTransaction();
            try {
                Query query = null;
                query = session.createQuery("select count(*) from DHits e where e.projectId=:projectId AND e.evaluation=:evaluation" +
                        " AND (e.status=:status1 OR e.status=:status2)");
                query.setParameter("evaluation", evaluation);
                query.setParameter("projectId", projectId);
                query.setParameter("status1", DConstants.HIT_STATUS_DONE);
                query.setParameter("status2", DConstants.HIT_STATUS_PRE_TAGGED);
                Long count = (Long)query.uniqueResult();
                transaction.commit();
                return count != null? count : 0;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    public long getCountForProject(String projectId) {
        return getCountForProjectForStatus(projectId, null);
    }

    public long getCountForProjectDone(String projectId) {
        return getCountForProjectForStatus(projectId, DConstants.HIT_STATUS_DONE);
    }

    public long getCountForProjectSkipped(String projectId) {
        return getCountForProjectForStatus(projectId, DConstants.HIT_STATUS_SKIPPED);
    }

    public long getCountForProjectDeleted(String projectId) {
        return getCountForProjectForStatus(projectId, DConstants.HIT_STATUS_DELETED);
    }


    public long getCountForProjectEvaluationCorrect(String projectId) {
        return getCountForProjectForEvaluation(projectId, DTypes.HIT_Evaluation_Type.CORRECT);
    }

    public long getCountForProjectEvaluationInCorrect(String projectId) {
        return getCountForProjectForEvaluation(projectId, DTypes.HIT_Evaluation_Type.INCORRECT);
    }

    // get count rows for tagging from the project selected randomly.
    // using randomly so that two users are not returned the same hit to be tagged.
    public List<DHits> getUndoneRandomlyInternal(String projectId, String status, long count) {
        String query = "FROM DHits where projectId = :projectId AND status = :status";
        return getInternal(query, projectId, status, null,0, count, DTypes.HIT_ORDER_Type.RANDOM);
    }

    public List<DHits> getUndoneInternal(String projectId, String status, long count, DTypes.HIT_ORDER_Type orderBy) {
        return getInternal(projectId, 0, count, status, orderBy);
    }

    //with evaluation filtering.
    public List<DHits> getInternal(String projectId, long start, long count, String status, String evaluation, DTypes.HIT_ORDER_Type orderBy) {
        String query = "FROM DHits where projectId = :projectId AND evaluation = :evaluation";
        if (status != null) {
            query = "FROM DHits where projectId = :projectId AND status = :status AND evaluation = :evaluation";
        }
        return getInternal(query, projectId, status, evaluation,  start, count, orderBy);
    }

    public List<DHits> getInternalByPid(String projectId, long start, long count) {
        String query = "FROM DHits where projectId = :projectId";
        return getInternalByPid(query, projectId, start, count);
    }

    public List<DHits> getInternal(String projectId, long start, long count, String status) {
        return getInternal(projectId, start, count, status,  DTypes.HIT_ORDER_Type.NONE);
    }

    public List<DHits> getInternal(String projectId, long start, long count, String status, DTypes.HIT_ORDER_Type orderBy) {
        String query = "FROM DHits where projectId = :projectId";
        if (status != null) {
            query = "FROM DHits where projectId = :projectId AND status = :status";
        }
        return getInternal(query, projectId, status, null, start, count, orderBy);
    }

    public List<DHits> getHitsByStatus(String projectId, long start, long count, String status, DTypes.HIT_ORDER_Type orderBy) {
        String sql = "select * " +
                "from d_hits as dh " +
                "where dh.projectId=? and dh.id in (select dhs.hitId from d_hits_result as dhs where dhs.status=?)";
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            org.hibernate.Transaction transaction = session.beginTransaction();
            try {
                String orderByString = " ORDER BY updated_timestamp DESC";
                sql += orderByString;

                SQLQuery query = session.createSQLQuery(sql);
                query.setParameter(0, projectId);
                query.setParameter(1, status);
                query.addEntity(DHits.class);// 绑定实体类
                query.setFirstResult((int)start);
                query.setMaxResults((int)count);
                List<DHits> list = query.list();
                transaction.commit();
                return list;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    private List<DHits> getInternal(String HQLQuery, String projectId, String status, String evaluation,
                                    long start, long count, DTypes.HIT_ORDER_Type orderBy) {
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            org.hibernate.Transaction transaction = session.beginTransaction();
            try {
                String orderByString = " ORDER BY ";
                if (orderBy == DTypes.HIT_ORDER_Type.RANDOM) {
                    orderByString += "RAND()";
                }
                if (orderBy == DTypes.HIT_ORDER_Type.NEW_FIRST) {
                    orderByString += "updated_timestamp DESC";
                }
                if (orderBy == DTypes.HIT_ORDER_Type.OLD_FIRST) {
                    orderByString += "updated_timestamp ASC";
                }
                if (orderBy == DTypes.HIT_ORDER_Type.NONE) {
                    orderByString = "";
                }

                HQLQuery += orderByString;

                Query query = session.createQuery(HQLQuery);
                query.setParameter("projectId", projectId);
                if (status != null) {
                    query.setParameter("status", status);
                }
                if (evaluation != null) {
                    query.setParameter("evaluation", DTypes.HIT_Evaluation_Type.valueOf(evaluation));
                }
                query.setFirstResult((int)start);
                query.setMaxResults((int)count);
                List<DHits> list = query.list();
                transaction.commit();
                return list;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    public void deleteByProjectId(String projectId) {
        Session session = sessionFactory.openSession();
        try {
            String HQLQuery = "DELETE DHits where projectId = :projectId";
            ManagedSessionContext.bind(session);
            org.hibernate.Transaction transaction = session.beginTransaction();
            try {
                Query query = session.createQuery(HQLQuery);
                query.setParameter("projectId", projectId);
                query.executeUpdate();
                transaction.commit();
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    private List<DHits> getInternalByPid(String HQLQuery, String projectId, long start, long count) {
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            org.hibernate.Transaction transaction = session.beginTransaction();
            try {
                String orderByString = " ORDER BY updated_timestamp ASC";
                HQLQuery += orderByString;

                Query query = session.createQuery(HQLQuery);
                query.setParameter("projectId", projectId);
                query.setFirstResult((int)start);
                query.setMaxResults((int)count);
                List<DHits> list = query.list();
                transaction.commit();
                return list;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    public List<DHits> getHitsNotDoneInternal(String projectId, long start, long count, DTypes.HIT_ORDER_Type orderBy, String model) {
//        // 图片在 d_hits_result 没有数据的情况，判定为 notDone
//        List<DHits> hitsList1 = getHitsNotDone(projectId, start, count, orderBy, model);
//        // 图片在 d_hits_result 有数据的情况，但是status 是 notDone
//        List<DHits> hitsList2 = getHitsNotDone2(projectId, start, count, orderBy, model);
//        // 把结果拼起来
//        hitsList1.addAll(hitsList2);
//        return hitsList1;

        return getHitsNotDone3(projectId, start, count, orderBy, model);
    }



    // 图片在 d_hits_result 没有数据的情况，判定为 notDone
    private List<DHits> getHitsNotDone(String projectId, long start, long count, DTypes.HIT_ORDER_Type orderBy, String model){
        String sql = "select * " +
                "from d_hits as dh " +
                "where dh.projectId=? and dh.id not in (select dhs.hitId from d_hits_result as dhs where dhs.model=?)";
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            org.hibernate.Transaction transaction = session.beginTransaction();
            try {
                String orderByString = " ORDER BY updated_timestamp DESC";
                sql += orderByString;

                SQLQuery query = session.createSQLQuery(sql);
                query.setParameter(0, projectId);
                query.setParameter(1, model);
                query.addEntity(DHits.class);// 绑定实体类
                query.setFirstResult((int)start);
                query.setMaxResults((int)count);
                List<DHits> list = query.list();
                transaction.commit();
                return list;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    // 图片在 d_hits_result 有数据的情况，但是status 是 notDone
    private List<DHits> getHitsNotDone2(String projectId, long start, long count, DTypes.HIT_ORDER_Type orderBy, String model){
        String sql = "select * " +
                "from d_hits as dh " +
                "where dh.id in " +
                "(select dhs.hitId " +
                "from d_hits_result as dhs " +
                "where dhs.projectId=? and dhs.model=? and dhs.status='notDone')";
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            org.hibernate.Transaction transaction = session.beginTransaction();
            try {
                String orderByString = " ORDER BY updated_timestamp DESC";
                sql += orderByString;

                SQLQuery query = session.createSQLQuery(sql);
                query.setParameter(0, projectId);
                query.setParameter(1, model);
                query.addEntity(DHits.class);// 绑定实体类
                query.setFirstResult((int)start);
                query.setMaxResults((int)count);
                List<DHits> list = query.list();
                transaction.commit();
                return list;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    // 图片为 notDone
    private List<DHits> getHitsNotDone3(String projectId, long start, long count, DTypes.HIT_ORDER_Type orderBy, String model){
        String sql = "select * " +
                "from d_hits as dh " +
                "where dh.projectId=? " +
                "and (" +
                "dh.id in (select dhs.hitId from d_hits_result as dhs where dhs.model=? and dhs.status='notDone') " +
                "or " +
                "dh.id not in (select dhs.hitId from d_hits_result as dhs where dhs.model=?)" +
                ")";
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            org.hibernate.Transaction transaction = session.beginTransaction();
            try {
                String orderByString = " ORDER BY updated_timestamp DESC";
                sql += orderByString;

                SQLQuery query = session.createSQLQuery(sql);
                query.setParameter(0, projectId);
                query.setParameter(1, model);
                query.setParameter(2, model);
                query.addEntity(DHits.class);// 绑定实体类
                query.setFirstResult((int)start);
                query.setMaxResults((int)count);
                List<DHits> list = query.list();
                transaction.commit();
                return list;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    public List<DHits> getCorrectResultIsNotNullInternalByPid(String projectId, long start, long count) {
        String sql = "select * from d_hits as dh where dh.projectId=? and dh.correctResult is not null";
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            org.hibernate.Transaction transaction = session.beginTransaction();
            try {
                String orderByString = " ORDER BY updated_timestamp DESC";
                sql += orderByString;

                SQLQuery query = session.createSQLQuery(sql);
                query.setParameter(0, projectId);
                query.addEntity(DHits.class);// 绑定实体类
                query.setFirstResult((int)start);
                query.setMaxResults((int)count);
                List<DHits> list = query.list();
                transaction.commit();
                return list;
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    public void updateHitById(long hitId, String labelPath) {
        String sql = "update d_hits as dh set dh.extras=? where dh.id=?";
        Session session = sessionFactory.openSession();
        try {
            ManagedSessionContext.bind(session);
            org.hibernate.Transaction transaction = session.beginTransaction();
            try {

                SQLQuery query = session.createSQLQuery(sql);
                query.setParameter(0, labelPath);
                query.setParameter(1, hitId);
                query.executeUpdate();
                transaction.commit();
            }
            catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
        finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }
}


