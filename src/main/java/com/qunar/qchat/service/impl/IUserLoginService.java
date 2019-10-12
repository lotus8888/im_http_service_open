package com.qunar.qchat.service.impl;

import com.google.common.base.Strings;
import com.qunar.qchat.constants.Config;
import com.qunar.qchat.dao.IUserInfo;
import com.qunar.qchat.dao.model.UserPasswordModel;
import com.qunar.qchat.dao.model.UserPasswordRO;
import com.qunar.qchat.service.IUserLogin;
import com.qunar.qchat.utils.JacksonUtils;
import com.qunar.qchat.utils.Md5Utils;
import com.qunar.qchat.utils.RSAEncrypt;
import com.qunar.qchat.utils.RedisUtil;
import org.ldaptive.Credential;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.control.ResponseControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Decoder;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IUserLoginService implements IUserLogin {
    private static final Logger LOGGER = LoggerFactory.getLogger(IUserLoginService.class);
    private static final String PASSWORD_SIGN = "p";
    private static final String USERID_SIGN = "u";
    private static final String HOST_SIGN = "h";
    private static final char SPLITTER_USERID = '\0';
    @Resource
    private IUserInfo iUserInfo;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private Authenticator authenticator;

    private static String RSA_PRIVATE = Config.getProperty("rsa_private_key");

    @Override
    public UserPasswordModel checkUserLogin(UserPasswordRO userInput) {

        if ("ldap".equalsIgnoreCase(Config.getProperty("user.check.type", "local"))) {
            return checkUserLoginByLdap(userInput);
        }

        UserPasswordModel userPasswordModel = new UserPasswordModel();
        String decodeUserLogin = decodePassword(userInput.getP()); //解密密码
        if (Strings.isNullOrEmpty(decodeUserLogin)) {
            userPasswordModel.setErrCode(1);
            LOGGER.info("user [{}],h:[{}] auth fail due to decode password fail", userInput.getU(), userInput.getH());
            return userPasswordModel;
        }
        String userID = userInput.getU();
        UserPasswordModel passwordDB = iUserInfo.getUserPassword(userID, userInput.getH());
        if (passwordDB == null) {
            userPasswordModel.setErrCode(2);
            LOGGER.warn("can not find user info from the db user [{}],h:[{}]", userInput.getU(), userInput.getH());
            return userPasswordModel;
        }
        if (checkPassword(decodeUserLogin, passwordDB.getPasswd(), passwordDB.getPasswdSalt())) {
            passwordDB.setToken(buildLoginToken(passwordDB.getUserID(), userInput.getH()));
            userPasswordModel = passwordDB;
            userPasswordModel.setErrCode(0);
            LOGGER.info("login success user [{}],h:[{}]", userInput.getU(), userInput.getH());
            return userPasswordModel;
        }
        LOGGER.info("user [{}],h:[{}] auth fail due to password error", userInput.getU(), userInput.getH());
        userPasswordModel.setErrCode(3);
        return userPasswordModel;
    }

    private UserPasswordModel checkUserLoginByLdap(UserPasswordRO userInput) {
        LOGGER.info("checkUserLoginByLdap param:{}", JacksonUtils.obj2String(userInput));
        UserPasswordModel userPasswordModel = new UserPasswordModel();
        String username = userInput.getU();
        String password = userInput.getP();
        try {
            AuthenticationResponse response = authenticator.authenticate(new AuthenticationRequest(username, new Credential(password), "uid", "mail"));
            if (response.getResult()) { // authentication succeeded
                LdapEntry entry = response.getLdapEntry(); // read uid and mail attributes
                // 获取uid
                String uid = entry.getAttribute("uid").getStringValue();
                System.out.println("uid: " + uid);
                // 获取邮箱
                LdapAttribute mailAttr = entry.getAttribute("mail");
                if (mailAttr != null) {
                    Collection<String> mails = mailAttr.getStringValues();
                    System.out.println("mail: " + mails.toArray(new String[mails.size()]));
                }
                userPasswordModel.setErrCode(0);
                userPasswordModel.setToken(buildLoginToken(username, userInput.getH()));
            } else {
                String msg = response.getMessage(); // read the failure message
                ResponseControl[] ctls = response.getControls(); // read any response controls
                userPasswordModel.setErrCode(3);
                LOGGER.warn("checkUserLoginByLdap auth user:{} fail msg:{}", username, msg);
            }
        } catch (LdapException e) {
            LOGGER.error("checkUserLoginByLdap user:{} error", username, e);
            userPasswordModel.setErrCode(5);
        }
        return userPasswordModel;
    }

    @Override
    public String buildLoginToken(String userID, String host) {
        StringBuilder stringBuilder = new StringBuilder(userID);
        stringBuilder.append("@").append(host);
        StringBuilder tokenSB = new StringBuilder();
        tokenSB.append(UUID.randomUUID()).append("-").append(System.currentTimeMillis());
        redisUtil.hPut(2, stringBuilder.toString(), tokenSB.toString(), tokenSB.toString(), 7, TimeUnit.DAYS);
        return tokenSB.toString();
    }

    @Override
    public boolean checkUserToken(String userId, String host, String token) {
        StringBuilder stringBuilder = new StringBuilder(userId);
        stringBuilder.append("@").append(host);
        Integer hostId = iUserInfo.getHostInfo(host);
        Integer hireFlag = iUserInfo.getUserHireFlag(userId, hostId);
        if(hireFlag==null){
            LOGGER.info("user {} host {} no user in db ", userId, host);
            return false;

        }
        if (!hireFlag.equals(1)) {
            LOGGER.info("user {} host {} have left ", userId, host);
            return false;
        }
        Set<String> tokens = redisUtil.hkeys(2, stringBuilder.toString());
        if (tokens.size() < 1) {
            return false;
        }
        for (String tokenRedis : tokens) {
            if (token.equals(tokenRedis)) {
                redisUtil.hDel(2, stringBuilder.toString(), tokenRedis);
                redisUtil.hPut(2, stringBuilder.toString(), tokenRedis, tokenRedis, 7, TimeUnit.DAYS);
                return true;
            }
        }
        return false;
    }

    @Override
    public String generatePassword(String originPassword, String salt) {
        String md5Step1 = Md5Utils.md5Encode(originPassword);
        String md5Step2 = Md5Utils.md5Encode(md5Step1 + salt);
        String md5Step3 = Md5Utils.md5Encode(md5Step2);
        return "CRY:" + md5Step3;
    }

    @Override
    public String decodePassword(String encodePassword) {
        if (Strings.isNullOrEmpty(encodePassword)) {
            LOGGER.warn("encodePassword is empty");
            return null;
        }
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] baseDecode = decoder.decodeBuffer(encodePassword);
            String res = new String(baseDecode);
            return RSAEncrypt.decrypt(res, RSA_PRIVATE);
        } catch (Exception e) {
            LOGGER.error("decode password error encode password {}", encodePassword, e);
            return null;
        }
    }

    @Override
    public boolean checkPassword(String cleartext_pwd, String passWd_db, String salt) {
        if (Strings.isNullOrEmpty(cleartext_pwd) || Strings.isNullOrEmpty(passWd_db)) {
            return false;
        }
        if (passWd_db.startsWith("CRY:")) {
            return generatePassword(cleartext_pwd, salt).equals(passWd_db);
        }
        return passWd_db.equals(cleartext_pwd);
    }


}