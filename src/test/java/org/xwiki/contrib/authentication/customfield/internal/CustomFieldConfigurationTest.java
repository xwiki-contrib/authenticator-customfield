/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.authentication.customfield.internal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Validate {@link CustomFieldConfiguration}.
 * 
 * @version $Id$
 */
public class CustomFieldConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<CustomFieldConfiguration> mocker =
        new MockitoComponentMockingRule<>(CustomFieldConfiguration.class);

    private ConfigurationSource source;

    @Before
    public void before() throws Exception
    {
        this.source = this.mocker.getInstance(ConfigurationSource.class);

        when(this.source.getProperty(any(), (Object) any())).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArguments()[1];
            }
        });
    }

    @Test
    public void getField() throws ComponentLookupException
    {
        Assert.assertEquals("email", this.mocker.getComponentUnderTest().getField());

        when(this.source.getProperty(eq("authentication.customfield.field"), (Object) any())).thenReturn("field");

        Assert.assertEquals("field", this.mocker.getComponentUnderTest().getField());
    }

    @Test
    public void isCaseSensitive() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isCaseSensitive());

        when(this.source.getProperty(eq("authentication.customfield.field"), (Object) any())).thenReturn("field");

        Assert.assertTrue(this.mocker.getComponentUnderTest().isCaseSensitive());

        when(this.source.getProperty(eq("authentication.customfield.caseSensitive"), (Object) any())).thenReturn(false);

        Assert.assertFalse(this.mocker.getComponentUnderTest().isCaseSensitive());
    }
}
