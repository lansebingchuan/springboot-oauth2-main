package com.zht.jwt.config.oauth2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * OAuth2认证服务配置
 */
@EnableAuthorizationServer
@Configuration
public class OAuth2AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private DataSource dataSource;

    // 注入认证管理器
    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.authenticationManager(authenticationManager);
        endpoints.tokenStore(tokenStore());
        endpoints.tokenEnhancer(accessTokenConverter());

        DefaultTokenServices tokenServices = (DefaultTokenServices) endpoints.getDefaultAuthorizationServerTokenServices();
        // 支持刷新token
        tokenServices.setSupportRefreshToken(true);
        // 1天
        tokenServices.setAccessTokenValiditySeconds((int) TimeUnit.DAYS.toSeconds(1));
        // 设置生成token时的客户端从数据库查找，，设置的时候代理为自己，及设置为下面配置的：clients.jdbc(dataSource);
        tokenServices.setClientDetailsService(endpoints.getClientDetailsService());
        // 令牌默认有效期2小时
        tokenServices.setAccessTokenValiditySeconds(7200);
        // 刷新令牌默认有效期3天
        tokenServices.setRefreshTokenValiditySeconds(259200);
        endpoints.tokenServices(tokenServices);
        // 只允许post提交方式
        endpoints.allowedTokenEndpointRequestMethods(HttpMethod.POST);
        super.configure(endpoints);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security.tokenKeyAccess("permitAll()");
        security.checkTokenAccess("permitAll()");
        // 允许表单登录
        security.allowFormAuthenticationForClients();
    }

    /**
     * OAuth2 客户端服务配置
     *
     * @param clients
     * @throws Exception
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        // 注入客户端信息从数据库当中获取
        clients.jdbc(dataSource);
    }

    /**
     * 指定token存储的位置
     *
     * @return
     */
    @Bean
    public TokenStore tokenStore() {
        return new JdbcTokenStore(dataSource);
    }

    /**
     * 注入自定义token生成方式
     *
     * @return
     */
    @Bean
    public TokenEnhancer customerEnhancer() {
        return new CustomTokenEnhancer();
    }

    //    @Bean
//    public TokenStore tokenStore() {
//    return new JwtTokenStore(accessTokenConverter());
//    }

    @Bean
    public TokenEnhancer accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        // 定义key
        converter.setSigningKey("123");
        converter.setSigner(new MacSigner(new byte[1]));
        converter.setAccessTokenConverter(new CustomerAccessTokenConverter());
        return converter;
    }

    @EventListener
    public void authSuccessEventListener(AuthenticationSuccessEvent authorizedEvent) {
        // write custom code here for login success audit
        System.err.println("ser Oauth2 登录成功");
        System.err.println("This is success event : " + authorizedEvent.getAuthentication().getPrincipal());
    }

    @EventListener
    public void authFailedEventListener(AbstractAuthenticationFailureEvent oAuth2AuthenticationFailureEvent) {
        // write custom code here login failed audit.
        System.err.println("User Oauth2 登录失败");
        System.err.println(oAuth2AuthenticationFailureEvent.getAuthentication().getPrincipal());
    }

}
