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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Make sure we don't get duplicate values for user identifier field value.
 * 
 * @version $Id$
 */
@Component
@Named(UniqueFieldValueListener.NAME)
@Singleton
public class UniqueFieldValueListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.contrib.authentication.customfield.internal.UniqueFieldValueListener";

    @Inject
    private CustomFieldConfiguration configuration;

    @Inject
    private UserManager userManager;

    @Inject
    private Logger logger;

    /**
     * Configure the listener.
     */
    public UniqueFieldValueListener()
    {
        super(NAME, new DocumentCreatingEvent(), new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        try {
            checkExists((CancelableEvent) event, document);
        } catch (Exception e) {
            this.logger.warn("Failed to check uniqueness of use uid, not blocking the save: {}",
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void checkExists(CancelableEvent event, XWikiDocument document) throws XWikiException, QueryException
    {
        String field = this.configuration.getField();

        String newValue = getFieldValue(field, document);

        // If the value is null no need to check since it's not possible to login with it
        if (newValue != null) {
            // If the value did not changed no need to check since it would not make sense to cancel the save
            // If the value can't be found in any existing user profile then we can save it
            if (!newValue.equals(getFieldValue(field, document.getOriginalDocument()))
                && this.userManager.getUser(field, newValue, this.configuration.isCaseSensitive(), false) != null) {
                // Cancel the document save
                ((CancelableEvent) event)
                    .cancel("A user with field [" + field + "] at [" + newValue + "] already exist");
            }
        }
    }

    private String getFieldValue(String field, XWikiDocument document) throws XWikiException
    {
        BaseObject userObject = document.getXObject(UserManager.USERCLASS_REFERENCE);

        if (userObject != null) {
            BaseProperty property = (BaseProperty) userObject.get(field);

            if (property != null) {
                Object value = property.getValue();

                // If the value is not a String no need to check since it's not possible to login with it
                if (value instanceof String) {
                    return (String) value;
                }
            }
        }

        return null;
    }
}
