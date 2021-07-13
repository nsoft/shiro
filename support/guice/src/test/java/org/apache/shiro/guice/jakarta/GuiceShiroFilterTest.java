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

import com.google.inject.spi.InjectionPoint;
import org.apache.shiro.jakarta.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class GuiceShiroFilterTest {

    @Test
    public void ensureInjectable() {
        try {
            InjectionPoint ip = InjectionPoint.forConstructorOf(GuiceShiroFilter.class);
        } catch (Exception e) {
            fail("Could not create constructor injection point.");
        }
    }

    @Test
    public void testConstructor() {
        WebSecurityManager securityManager = createMock(WebSecurityManager.class);
        FilterChainResolver filterChainResolver = createMock(FilterChainResolver.class);

        GuiceShiroFilter underTest = new GuiceShiroFilter(securityManager, filterChainResolver);

        assertSame(securityManager, underTest.getSecurityManager());
        assertSame(filterChainResolver, underTest.getFilterChainResolver());
    }
}
