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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PasswordClass;

/**
 * Authentication using custom field instead of the user identifier.
 * 
 * @version $Id$
 */
@Component(roles = InternalAuthenticator.class)
@Singleton
public class InternalAuthenticator
{
    @Inject
    private CustomFieldConfiguration configuration;

    @Inject
    private UserManager userManager;

    /**
     * @param fieldValue the value of the login
     * @param password the value of password
     * @param xcontext the XWiki context
     * @return the document of the authenticated use or null if none matches the passed credentials
     * @throws Exception when an unexpected exception was thrown during authentication process
     */
    public XWikiDocument authenticate(String fieldValue, String password, XWikiContext xcontext) throws Exception
    {
        String fieldName = this.configuration.getField();

        String documentReference = this.userManager.getUser(fieldName, fieldValue, true);

        if (documentReference != null) {
            XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
            BaseObject userObject = document.getXObject(UserManager.USERCLASS_REFERENCE);
            String stored = userObject.getStringValue("password");

            if (new PasswordClass().getEquivalentPassword(stored, password).equals(stored)) {
                return document;
            }
        }

        return null;
    }
}
