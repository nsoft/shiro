/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shiro.guice.jakarta;

import com.google.inject.*;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import org.apache.shiro.env.Environment;
import org.apache.shiro.guice.ShiroModuleTest;
import org.apache.shiro.guice.jakarta.ShiroWebModule;
import org.apache.shiro.guice.jakarta.SimpleFilterChain;
import org.apache.shiro.guice.jakarta.SimpleFilterChainResolver;
import org.apache.shiro.guice.jakarta.WebGuiceEnvironment;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.jakarta.env.EnvironmentLoader;
import org.apache.shiro.jakarta.env.WebEnvironment;
import org.apache.shiro.jakarta.filter.InvalidRequestFilter;
import org.apache.shiro.jakarta.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.jakarta.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.jakarta.filter.authz.PermissionsAuthorizationFilter;
import org.apache.shiro.jakarta.filter.authz.RolesAuthorizationFilter;
import org.apache.shiro.jakarta.filter.mgt.FilterChainResolver;
import org.apache.shiro.jakarta.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.jakarta.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.jakarta.session.mgt.ServletContainerSessionManager;
import org.easymock.EasyMock;
import org.junit.Assume;
import org.junit.Test;

import javax.inject.Named;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;


public class ShiroWebModuleTest {


    @Test
    public void basicInstantiation() {
        final ShiroModuleTest.MockRealm mockRealm = createMock(ShiroModuleTest.MockRealm.class);
        ServletContext servletContext = createMock(ServletContext.class);

        Injector injector = Guice.createInjector(new ShiroWebModule(servletContext) {
            @Override
            protected void configureShiroWeb() {
                bindRealm().to(ShiroModuleTest.MockRealm.class);
                expose(SessionManager.class);
            }

            @Provides
            public ShiroModuleTest.MockRealm createRealm() {
                return mockRealm;
            }

        });
        // we're not getting a WebSecurityManager here b/c it's not exposed.  There didn't seem to be a good reason to
        // expose it outside of the Shiro module.
        SecurityManager securityManager = injector.getInstance(SecurityManager.class);
        assertNotNull(securityManager);
        assertTrue(securityManager instanceof WebSecurityManager);
        SessionManager sessionManager = injector.getInstance(SessionManager.class);
        assertNotNull(sessionManager);
        assertTrue(sessionManager instanceof ServletContainerSessionManager);
        assertTrue(((DefaultWebSecurityManager)securityManager).getSessionManager() instanceof ServletContainerSessionManager);
    }

    @Test
    public void testBindGuiceFilter() throws Exception {

    }

    @Test
    public void testBindWebSecurityManager() throws Exception {
        final ShiroModuleTest.MockRealm mockRealm = createMock(ShiroModuleTest.MockRealm.class);
        ServletContext servletContext = createMock(ServletContext.class);

        Injector injector = Guice.createInjector(new ShiroWebModule(servletContext) {
            @Override
            protected void configureShiroWeb() {
                bindRealm().to(ShiroModuleTest.MockRealm.class);
                expose(WebSecurityManager.class);
            }

            @Provides
            public ShiroModuleTest.MockRealm createRealm() {
                return mockRealm;
            }

            @Override
            protected void bindWebSecurityManager(AnnotatedBindingBuilder<? super WebSecurityManager> bind) {
                bind.to(MyDefaultWebSecurityManager.class).asEagerSingleton();
            }
        });
        SecurityManager securityManager = injector.getInstance(SecurityManager.class);
        assertNotNull(securityManager);
        assertTrue(securityManager instanceof MyDefaultWebSecurityManager);
        WebSecurityManager webSecurityManager = injector.getInstance(WebSecurityManager.class);
        assertNotNull(webSecurityManager);
        assertTrue(webSecurityManager instanceof MyDefaultWebSecurityManager);
        // SHIRO-435: Check both keys SecurityManager and WebSecurityManager are bound to the same instance
        assertTrue( securityManager == webSecurityManager );
    }

    @Test
    public void testBindWebEnvironment() throws Exception {
        final ShiroModuleTest.MockRealm mockRealm = createMock(ShiroModuleTest.MockRealm.class);
        ServletContext servletContext = createMock(ServletContext.class);

        Injector injector = Guice.createInjector(new ShiroWebModule(servletContext) {
            @Override
            protected void configureShiroWeb() {
                bindRealm().to(ShiroModuleTest.MockRealm.class);
                expose(WebEnvironment.class);
                expose(Environment.class);
            }

            @Provides
            public ShiroModuleTest.MockRealm createRealm() {
                return mockRealm;
            }

            @Override
            protected void bindWebEnvironment(AnnotatedBindingBuilder<? super WebEnvironment> bind) {
                bind.to(MyWebEnvironment.class).asEagerSingleton();
            }
        });
        Environment environment = injector.getInstance(Environment.class);
        assertNotNull(environment);
        assertTrue(environment instanceof MyWebEnvironment);
        WebEnvironment webEnvironment = injector.getInstance(WebEnvironment.class);
        assertNotNull(webEnvironment);
        assertTrue(webEnvironment instanceof MyWebEnvironment);
        // SHIRO-435: Check both keys Environment and WebEnvironment are bound to the same instance
        assertTrue( environment == webEnvironment );
    }

    /**
     * @since 1.4
     */
    @Test
    public void testAddFilterChainGuice3and4() {

        final ShiroModuleTest.MockRealm mockRealm = createMock(ShiroModuleTest.MockRealm.class);
        ServletContext servletContext = createMock(ServletContext.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        servletContext.setAttribute(eq(EnvironmentLoader.ENVIRONMENT_ATTRIBUTE_KEY), EasyMock.anyObject());
        expect(request.getAttribute("jakarta.servlet.include.context_path")).andReturn("").anyTimes();
        expect(request.getCharacterEncoding()).andReturn("UTF-8").anyTimes();
        expect(request.getAttribute("jakarta.servlet.include.path_info")).andReturn(null).anyTimes();
        expect(request.getPathInfo()).andReturn(null).anyTimes();
        expect(request.getAttribute("jakarta.servlet.include.servlet_path")).andReturn("/test_authc");
        expect(request.getAttribute("jakarta.servlet.include.servlet_path")).andReturn("/test_custom_filter");
        expect(request.getAttribute("jakarta.servlet.include.servlet_path")).andReturn("/test_authc_basic");
        expect(request.getAttribute("jakarta.servlet.include.servlet_path")).andReturn("/test_perms");
        expect(request.getAttribute("jakarta.servlet.include.servlet_path")).andReturn("/multiple_configs");
        replay(servletContext, request);

        Injector injector = Guice.createInjector(new ShiroWebModule(servletContext) {
            @Override
            protected void configureShiroWeb() {
                bindRealm().to(ShiroModuleTest.MockRealm.class);
                expose(FilterChainResolver.class);
                this.addFilterChain("/test_authc/**", filterConfig(AUTHC));
                this.addFilterChain("/test_custom_filter/**", Key.get(CustomFilter.class));
                this.addFilterChain("/test_authc_basic/**", AUTHC_BASIC);
                this.addFilterChain("/test_perms/**", filterConfig(PERMS, "remote:invoke:lan,wan"));
                this.addFilterChain("/multiple_configs/**", filterConfig(AUTHC), filterConfig(ROLES, "b2bClient"), filterConfig(PERMS, "remote:invoke:lan,wan"));
            }

            @Provides
            public ShiroModuleTest.MockRealm createRealm() {
                return mockRealm;
            }
        });

        FilterChainResolver resolver = injector.getInstance(FilterChainResolver.class);
        assertThat(resolver, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChainResolver.class));
        org.apache.shiro.guice.jakarta.SimpleFilterChainResolver simpleFilterChainResolver = (org.apache.shiro.guice.jakarta.SimpleFilterChainResolver) resolver;

        // test the /test_authc resource
        FilterChain filterChain = simpleFilterChainResolver.getChain(request, null, null);
        assertThat(filterChain, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChain.class));
        Filter nextFilter = getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain);
        assertThat(nextFilter, instanceOf(InvalidRequestFilter.class));
        nextFilter = getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain);
        assertThat(nextFilter, instanceOf(FormAuthenticationFilter.class));

        // test the /test_custom_filter resource
        filterChain = simpleFilterChainResolver.getChain(request, null, null);
        assertThat(filterChain, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChain.class));
        nextFilter = getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain);
        assertThat(nextFilter, instanceOf(InvalidRequestFilter.class));
        nextFilter = getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain);
        assertThat(nextFilter, instanceOf(CustomFilter.class));

        // test the /test_authc_basic resource
        filterChain = simpleFilterChainResolver.getChain(request, null, null);
        assertThat(filterChain, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChain.class));
        nextFilter = getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain);
        assertThat(nextFilter, instanceOf(InvalidRequestFilter.class));
        nextFilter = getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain);
        assertThat(nextFilter, instanceOf(BasicHttpAuthenticationFilter.class));

        // test the /test_perms resource
        filterChain = simpleFilterChainResolver.getChain(request, null, null);
        assertThat(filterChain, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChain.class));
        nextFilter = getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain);
        assertThat(nextFilter, instanceOf(InvalidRequestFilter.class));
        nextFilter = getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain);
        assertThat(nextFilter, instanceOf(PermissionsAuthorizationFilter.class));

        // test the /multiple_configs resource
        filterChain = simpleFilterChainResolver.getChain(request, null, null);
        assertThat(filterChain, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChain.class));
        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), instanceOf(InvalidRequestFilter.class));
        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), instanceOf(FormAuthenticationFilter.class));
        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), instanceOf(RolesAuthorizationFilter.class));
        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), instanceOf(PermissionsAuthorizationFilter.class));

        verify(servletContext, request);
    }

    /**
     * @since 1.4
     */
    @Test
    public void testAddFilterChainGuice3Only() {

        Assume.assumeTrue("This test only runs agains Guice 3.x", ShiroWebModule.isGuiceVersion3());

        final ShiroModuleTest.MockRealm mockRealm = createMock(ShiroModuleTest.MockRealm.class);
        ServletContext servletContext = createMock(ServletContext.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        servletContext.setAttribute(eq(EnvironmentLoader.ENVIRONMENT_ATTRIBUTE_KEY), EasyMock.anyObject());
        expect(request.getAttribute("jakarta.servlet.include.context_path")).andReturn("").anyTimes();
        expect(request.getCharacterEncoding()).andReturn("UTF-8").anyTimes();
        expect(request.getAttribute("jakarta.servlet.include.request_uri")).andReturn("/test_authc");
        expect(request.getAttribute("jakarta.servlet.include.request_uri")).andReturn("/test_custom_filter");
        expect(request.getAttribute("jakarta.servlet.include.request_uri")).andReturn("/test_perms");
        expect(request.getAttribute("jakarta.servlet.include.request_uri")).andReturn("/multiple_configs");
        replay(servletContext, request);

        Injector injector = Guice.createInjector(new ShiroWebModule(servletContext) {
            @Override
            protected void configureShiroWeb() {
                bindRealm().to(ShiroModuleTest.MockRealm.class);
                expose(FilterChainResolver.class);
                this.addFilterChain("/test_authc/**", AUTHC);
                this.addFilterChain("/test_custom_filter/**", Key.get(CustomFilter.class));
                this.addFilterChain("/test_perms/**", config(PERMS, "remote:invoke:lan,wan"));
                this.addFilterChain("/multiple_configs/**", AUTHC, config(ROLES, "b2bClient"), config(PERMS, "remote:invoke:lan,wan"));
            }

            @Provides
            public ShiroModuleTest.MockRealm createRealm() {
                return mockRealm;
            }
        });

        FilterChainResolver resolver = injector.getInstance(FilterChainResolver.class);
        assertThat(resolver, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChainResolver.class));
        org.apache.shiro.guice.jakarta.SimpleFilterChainResolver simpleFilterChainResolver = (org.apache.shiro.guice.jakarta.SimpleFilterChainResolver) resolver;

        // test the /test_authc resource
        FilterChain filterChain = simpleFilterChainResolver.getChain(request, null, null);
        assertThat(filterChain, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChain.class));
        Filter nextFilter = getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain);
        assertThat(nextFilter, instanceOf(FormAuthenticationFilter.class));

        // test the /test_custom_filter resource
        filterChain = simpleFilterChainResolver.getChain(request, null, null);
        assertThat(filterChain, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChain.class));
        nextFilter = getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain);
        assertThat(nextFilter, instanceOf(CustomFilter.class));

        // test the /test_perms resource
        filterChain = simpleFilterChainResolver.getChain(request, null, null);
        assertThat(filterChain, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChain.class));
        nextFilter = getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain);
        assertThat(nextFilter, instanceOf(PermissionsAuthorizationFilter.class));

        // test the /multiple_configs resource
        filterChain = simpleFilterChainResolver.getChain(request, null, null);
        assertThat(filterChain, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChain.class));
        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), instanceOf(FormAuthenticationFilter.class));
        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), instanceOf(RolesAuthorizationFilter.class));
        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), instanceOf(PermissionsAuthorizationFilter.class));

        verify(servletContext, request);
    }

    @Test
    public void testDefaultPath() {

        final ShiroModuleTest.MockRealm mockRealm = createMock(ShiroModuleTest.MockRealm.class);
        ServletContext servletContext = createMock(ServletContext.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        servletContext.setAttribute(eq(EnvironmentLoader.ENVIRONMENT_ATTRIBUTE_KEY), EasyMock.anyObject());
        expect(request.getAttribute("jakarta.servlet.include.context_path")).andReturn("").anyTimes();
        expect(request.getCharacterEncoding()).andReturn("UTF-8").anyTimes();
        expect(request.getAttribute("jakarta.servlet.include.path_info")).andReturn(null).anyTimes();
        expect(request.getPathInfo()).andReturn(null).anyTimes();
        expect(request.getAttribute("jakarta.servlet.include.servlet_path")).andReturn("/test/foobar");
        replay(servletContext, request);

        Injector injector = Guice.createInjector(new ShiroWebModule(servletContext) {
            @Override
            protected void configureShiroWeb() {
                bindRealm().to(ShiroModuleTest.MockRealm.class);
                expose(FilterChainResolver.class);
                // no paths configured
            }

            @Provides
            public ShiroModuleTest.MockRealm createRealm() {
                return mockRealm;
            }
        });

        FilterChainResolver resolver = injector.getInstance(FilterChainResolver.class);
        assertThat(resolver, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChainResolver.class));
        org.apache.shiro.guice.jakarta.SimpleFilterChainResolver simpleFilterChainResolver = (org.apache.shiro.guice.jakarta.SimpleFilterChainResolver) resolver;

        // test the /test_authc resource
        FilterChain filterChain = simpleFilterChainResolver.getChain(request, null, null);
        assertThat(filterChain, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChain.class));

        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), instanceOf(InvalidRequestFilter.class));
        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), nullValue());

        verify(servletContext, request);
    }

    @Test
    public void testDisableGlobalFilters() {

        final ShiroModuleTest.MockRealm mockRealm = createMock(ShiroModuleTest.MockRealm.class);
        ServletContext servletContext = createMock(ServletContext.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        servletContext.setAttribute(eq(EnvironmentLoader.ENVIRONMENT_ATTRIBUTE_KEY), EasyMock.anyObject());
        expect(request.getAttribute("jakarta.servlet.include.context_path")).andReturn("").anyTimes();
        expect(request.getCharacterEncoding()).andReturn("UTF-8").anyTimes();
        expect(request.getAttribute("jakarta.servlet.include.path_info")).andReturn(null).anyTimes();
        expect(request.getPathInfo()).andReturn(null).anyTimes();
        expect(request.getAttribute("jakarta.servlet.include.servlet_path")).andReturn("/test/foobar");
        replay(servletContext, request);

        Injector injector = Guice.createInjector(new ShiroWebModule(servletContext) {
            @Override
            protected void configureShiroWeb() {
                bindRealm().to(ShiroModuleTest.MockRealm.class);
                expose(FilterChainResolver.class);
                this.addFilterChain("/**", filterConfig(AUTHC));
            }

            @Override
            public List<FilterConfig<? extends Filter>> globalFilters() {
                return Collections.emptyList();
            }

            @Provides
            public ShiroModuleTest.MockRealm createRealm() {
                return mockRealm;
            }
        });

        FilterChainResolver resolver = injector.getInstance(FilterChainResolver.class);
        assertThat(resolver, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChainResolver.class));
        org.apache.shiro.guice.jakarta.SimpleFilterChainResolver simpleFilterChainResolver = (org.apache.shiro.guice.jakarta.SimpleFilterChainResolver) resolver;

        // test the /test_authc resource
        FilterChain filterChain = simpleFilterChainResolver.getChain(request, null, null);
        assertThat(filterChain, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChain.class));

        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), instanceOf(FormAuthenticationFilter.class));
        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), nullValue());

        verify(servletContext, request);
    }

    @Test
    public void testChangeInvalidFilterConfig() {

        final ShiroModuleTest.MockRealm mockRealm = createMock(ShiroModuleTest.MockRealm.class);
        ServletContext servletContext = createMock(ServletContext.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        servletContext.setAttribute(eq(EnvironmentLoader.ENVIRONMENT_ATTRIBUTE_KEY), EasyMock.anyObject());
        expect(request.getAttribute("jakarta.servlet.include.context_path")).andReturn("").anyTimes();
        expect(request.getCharacterEncoding()).andReturn("UTF-8").anyTimes();
        expect(request.getAttribute("jakarta.servlet.include.path_info")).andReturn(null).anyTimes();
        expect(request.getPathInfo()).andReturn(null).anyTimes();
        expect(request.getAttribute("jakarta.servlet.include.servlet_path")).andReturn("/test/foobar");
        replay(servletContext, request);

        Injector injector = Guice.createInjector(new ShiroWebModule(servletContext) {
            @Override
            protected void configureShiroWeb() {

                bindConstant().annotatedWith(Names.named("shiro.blockBackslash")).to(false);

                bindRealm().to(ShiroModuleTest.MockRealm.class);
                expose(FilterChainResolver.class);
                this.addFilterChain("/**", filterConfig(AUTHC));
            }

            @Provides
            public ShiroModuleTest.MockRealm createRealm() {
                return mockRealm;
            }
        });

        FilterChainResolver resolver = injector.getInstance(FilterChainResolver.class);
        assertThat(resolver, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChainResolver.class));
        org.apache.shiro.guice.jakarta.SimpleFilterChainResolver simpleFilterChainResolver = (org.apache.shiro.guice.jakarta.SimpleFilterChainResolver) resolver;

        // test the /test_authc resource
        FilterChain filterChain = simpleFilterChainResolver.getChain(request, null, null);
        assertThat(filterChain, instanceOf(org.apache.shiro.guice.jakarta.SimpleFilterChain.class));

        Filter invalidRequestFilter = getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain);
        assertThat(invalidRequestFilter, instanceOf(InvalidRequestFilter.class));
        assertFalse("Expected 'blockBackslash' to be false", ((InvalidRequestFilter) invalidRequestFilter).isBlockBackslash());
        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), instanceOf(FormAuthenticationFilter.class));
        assertThat(getNextFilter((org.apache.shiro.guice.jakarta.SimpleFilterChain) filterChain), nullValue());

        verify(servletContext, request);
    }

    private Filter getNextFilter(org.apache.shiro.guice.jakarta.SimpleFilterChain filterChain) {

        Iterator<? extends Filter> filters = filterChain.getFilters();
        if (filters.hasNext()) {
            return filters.next();
        }

        return null;
    }

    public static class MyDefaultWebSecurityManager extends DefaultWebSecurityManager {
        @Inject
        public MyDefaultWebSecurityManager(Collection<Realm> realms) {
            super(realms);
        }
    }

    public static class MyDefaultWebSessionManager extends DefaultWebSessionManager {
    }

    public static class MyWebEnvironment extends org.apache.shiro.guice.jakarta.WebGuiceEnvironment {
        @Inject
        MyWebEnvironment(FilterChainResolver filterChainResolver, @Named(ShiroWebModule.NAME) ServletContext servletContext, WebSecurityManager securityManager) {
            super(filterChainResolver, servletContext, securityManager);
        }
    }

    public static class CustomFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {}

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {}

        @Override
        public void destroy() {}
    }
}