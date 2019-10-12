package com.qunar.qchat.constants;


import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.BindAuthenticationHandler;
import org.ldaptive.auth.SearchDnResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class LdapConfig {

    @Bean
    public Authenticator authenticator() {
        ConnectionConfig connConfig = new ConnectionConfig(Config.getProperty("ldap.url"));
        connConfig.setConnectionInitializer(
                new BindConnectionInitializer(Config.getProperty("ldap.admin.dn"), new Credential(Config.getProperty("ldap.admin.pass", "xxx"))));

        SearchDnResolver dnResolver = new SearchDnResolver(
                new DefaultConnectionFactory(connConfig));
        dnResolver.setBaseDn(Config.getProperty("ldap.user.base"));
        dnResolver.setUserFilter(Config.getProperty("ldap.user.filter"));
        dnResolver.setSubtreeSearch(true);

        BindAuthenticationHandler authHandler = new BindAuthenticationHandler(
                new DefaultConnectionFactory(connConfig));

        Authenticator authenticator = new Authenticator(dnResolver, authHandler);
        return authenticator;
    }
}
