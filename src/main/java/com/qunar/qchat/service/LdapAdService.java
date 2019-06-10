package com.qunar.qchat.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.qunar.qchat.constants.Config;
import com.qunar.qchat.dao.IHostUserDao;
import com.qunar.qchat.dao.IUserInfo;
import com.qunar.qchat.dao.model.UserInfoQtalk;
import com.qunar.qchat.model.JsonResult;
import com.qunar.qchat.utils.JacksonUtils;
import com.qunar.qchat.utils.JsonResultUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LdapAdService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapAdService.class);

    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private IHostUserDao hostUserDao;
    @Resource
    private IUserInfo iUserInfo;

    public JsonResult<?> initAdUsers() {
        try {
            String ldapSearchBase = Config.getProperty("ldapSearchBase");
            String ldapResultMapping = Config.getProperty("ldapResultMapping");
            if (StringUtils.isAnyEmpty(ldapSearchBase, ldapResultMapping)) {
                LOGGER.warn("getAdUsers properties base:{} mapping:{} is empty", ldapSearchBase, ldapResultMapping);
                return JsonResultUtils.fail(411, "配置错误");
            }

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", "user"));

            ContextSource contextSource =  ldapTemplate.getContextSource();
            DirContext ctx = contextSource.getReadWriteContext();
            LdapContext lCtx = (LdapContext) ctx;

            SearchControls schCtrls = new SearchControls();
            schCtrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            Map<String, String> stringMap = JacksonUtils.string2Obj(ldapResultMapping, new TypeReference<Map<String, String>>() {
            });
            String returnedAtts[] = null;
            if (MapUtils.isNotEmpty(stringMap)) {
                returnedAtts = stringMap.values().toArray(new String[3]);
            } else {
                stringMap = new HashMap<>();
            }

            schCtrls.setReturningAttributes(returnedAtts);
            int pageSize = 2000;
            lCtx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, Control.CRITICAL)});
            byte[] cookie = null;
            List<UserInfoQtalk> result = new ArrayList<>();
            LinkedHashMap<String, UserInfoQtalk> adUser = new LinkedHashMap<>();
            do {
                NamingEnumeration<SearchResult> results = lCtx.search(ldapSearchBase, filter.encode(), schCtrls);
                while (results != null && results.hasMoreElements()) {
                    SearchResult sr = results.next();
                    parseUser(sr, stringMap, adUser);
                }
                cookie = parseControls(lCtx.getResponseControls());
                lCtx.setRequestControls(new Control[] {new PagedResultsControl(pageSize, cookie, Control.CRITICAL)});
            } while ((cookie != null) && (cookie.length != 0));

            List<UserInfoQtalk> userInfoQtalks = hostUserDao.selectOnJobUserFromHostUser(1);
            HashMap<String, UserInfoQtalk> dbUser = new HashMap<>();
            dbUser = (HashMap) userInfoQtalks.stream().collect(Collectors.toMap(UserInfoQtalk::getUser_id, A -> A, (k1, k2) -> k1));
            compareAndPrcess(adUser, dbUser);

            return JsonResultUtils.success();
        } catch (NamingException | IOException e) {
            LOGGER.error("LdapAdService/getAdUsers error");
            return JsonResultUtils.fail(500, "server端错误");
        }
    }

    private void compareAndPrcess(LinkedHashMap<String, UserInfoQtalk> adUser, HashMap<String, UserInfoQtalk> dbUser) {

        int maxVersion = hostUserDao.selectMaxVersion("host_users");
        int nextVersion = maxVersion + 1;

        Map<String, MapDifference.ValueDifference<UserInfoQtalk>> both = null;
        long start = System.currentTimeMillis();
        MapDifference<String, UserInfoQtalk> diff = Maps.difference(adUser, dbUser);
        LOGGER.warn("update struct step 3  cost{}",System.currentTimeMillis()-start);
        Map<String, UserInfoQtalk> onlyEmployee = diff.entriesOnlyOnLeft();
        Map<String, UserInfoQtalk> onlyPgUser = diff.entriesOnlyOnRight();
        both = diff.entriesDiffering();
        List<UserInfoQtalk> insertPgTemp = new LinkedList<>();
        List<UserInfoQtalk> setDimission = new LinkedList<>();
        List<UserInfoQtalk> updatePg = new LinkedList<>();
        start = System.currentTimeMillis();

        onlyEmployee.values().stream().forEach(value -> {
            value.setVersion(nextVersion);
            insertPgTemp.add(value);
        });
        onlyPgUser.values().stream().forEach(user -> {
            user.setVersion(nextVersion);
            user.setHire_flag(0);
            setDimission.add(user);
        });

        both.keySet().stream().forEach(key -> {
            UserInfoQtalk userInfoQtalk = dbUser.get(key);
            userInfoQtalk.setVersion(nextVersion);
            updatePg.add(userInfoQtalk);
        });

        LOGGER.warn("update struct step 4 cost{}",System.currentTimeMillis()-start);
        start = System.currentTimeMillis();
        updatePg(insertPgTemp, setDimission, updatePg);
        LOGGER.warn("update struct step 5 cost{}",System.currentTimeMillis()-start);

    }

    public void updatePg(List<UserInfoQtalk> insert, List<UserInfoQtalk> delete, List<UserInfoQtalk> update) {
        if (insert != null) {
            LOGGER.info(">>>>>>>>>>>>>此次更新入职{}人", insert.size());
            insert.stream().forEach(x -> {
                LOGGER.info("update structure new insert >> {}", JacksonUtils.obj2String(x));
                hostUserDao.insertUser(x);
                 insertVcard(x);
            });
        }
        if (delete != null) {
            LOGGER.info(">>>>>>>>>>>>>此次更新离职{}人", delete.size());
            delete.stream().forEach(x -> {
                LOGGER.info("update structure update user info >> {}", JacksonUtils.obj2String(x));
               // hostUserDao.updateHostUserHireType(x);
            });
        }
        if (update != null) {
            LOGGER.info(">>>>>>>>>>>>>此次更新更新{}人", update.size());
            update.stream().forEach(x -> {
                LOGGER.info("update structure leave user >> {}", JacksonUtils.obj2String(x));
                hostUserDao.updateHostUser(x);
            });
        }
    }


    private void parseUser(SearchResult sr, Map<String, String> stringMap, LinkedHashMap<String, UserInfoQtalk> adUser) {

        try {
            UserInfoQtalk userInfoQtalk = new UserInfoQtalk();
            Attributes attrs = sr.getAttributes();
            String userIdMapping = stringMap.getOrDefault("userId", "sAMAccountName");
            String userNameMapping = stringMap.getOrDefault("userName", "cn");
            String departmentMapping = stringMap.getOrDefault("department", "department");
            String mailMapping = stringMap.getOrDefault("email", "mail");
            String sexMapping = stringMap.getOrDefault("sex", "sex");
            String userId = attrs.get(userIdMapping) == null ? null : attrs.get(userIdMapping).get().toString();
            userInfoQtalk.setUser_id(userId);
            userInfoQtalk.setUser_name(attrs.get(userNameMapping) == null ? null : attrs.get(userNameMapping).get().toString());
            userInfoQtalk.setEmail(attrs.get(mailMapping) == null ? null : attrs.get(mailMapping).get().toString());
            userInfoQtalk.setSex(attrs.get(sexMapping) == null ? null : attrs.get(sexMapping).get().toString());
            userInfoQtalk.setUser_type("u");
            userInfoQtalk.setHire_flag(1);
            String department = "";
            if (attrs.get(departmentMapping) != null) {
                department = attrs.get(departmentMapping).get().toString();
                parseDepartment(department, userInfoQtalk);
            }
            userInfoQtalk.setDepartment(department);

            adUser.put(userId, userInfoQtalk);
        } catch (NamingException e) {
            LOGGER.error("parseUser error", e);
        }
    }

    private void parseDepartment(String department, UserInfoQtalk userInfoQtalk) {
        String splitRex = Config.getProperty("ldapDepartmentSplit", "\\\\");
        if (StringUtils.isNotEmpty(department)) {
            String[] split = department.split(splitRex);

            switch (split.length) {
                case 5:
                    userInfoQtalk.setDep5(split[4]);
                case 4:
                    userInfoQtalk.setDep4(split[3]);
                case 3:
                    userInfoQtalk.setDep3(split[2]);
                case 2:
                    userInfoQtalk.setDep2(split[1]);
                case 1:
                    userInfoQtalk.setDep1(split[0]);
                    break;
            }
        }
    }


    private static byte[] parseControls(Control[] controls) throws NamingException {
        byte[] cookie = null;
        if (controls != null) {
            for (Control control : controls) {
                if (control instanceof PagedResultsResponseControl) {
                    PagedResultsResponseControl prrc = (PagedResultsResponseControl) control;
                    cookie = prrc.getCookie();
                }
            }
        }
        return (cookie == null) ? new byte[0] : cookie;
    }

    public void insertVcard(UserInfoQtalk userInfoQtalk) {
        LOGGER.info("insert user into vcard_version {}", userInfoQtalk.getUser_id());
        String domain = iUserInfo.getDomain(1);
        String malePhoto = Config.getProperty("malePhoto");
        String famalePhoto = Config.getProperty("famalePhoto");

        if (userInfoQtalk.getGender().equals(1)) {
            iUserInfo.insertVcardVersion(userInfoQtalk.getUser_id(), domain, malePhoto, userInfoQtalk.getGender());
        }
        if (userInfoQtalk.getGender().equals(2)) {
            iUserInfo.insertVcardVersion(userInfoQtalk.getUser_id(), domain, famalePhoto, userInfoQtalk.getGender());
        }
    }
}
