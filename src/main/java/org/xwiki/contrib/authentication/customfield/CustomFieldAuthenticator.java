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
package org.xwiki.contrib.authentication.customfield;

import java.security.Principal;

import org.apache.commons.lang3.StringUtils;
import org.securityfilter.realm.SimplePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.contrib.authentication.customfield.internal.CustomFieldConfiguration;
import org.xwiki.contrib.authentication.customfield.internal.InternalAuthenticator;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;
import com.xpn.xwiki.web.Utils;

/**
 * Authentication using custom field instead of the user identifier.
 * 
 * @version $Id: a55364b2ef0b4dd7d3ec758bd7b0c743bdf2109e $
 */
public class CustomFieldAuthenticator extends XWikiAuthServiceImpl
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomFieldAuthenticator.class);

    private static final String MESSAGE = "message";

    private CustomFieldConfiguration configuration = Utils.getComponent(CustomFieldConfiguration.class);

    private InternalAuthenticator authenticator = Utils.getComponent(InternalAuthenticator.class);

    private EntityReferenceSerializer<String> compactSerializer =
        Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");

    @Override
    public Principal authenticate(String username, String password, XWikiContext xcontext) throws XWikiException
    {
        if (username == null) {
            // If we can't find the username field then we are probably on the login screen
            return null;
        }

        if (StringUtils.isBlank(username)) {
            setMessage("nousername", xcontext);
            return null;
        }

        // Check for empty passwords
        if (password == null || password.isEmpty()) {
            setMessage("nopassword", xcontext);
            return null;
        }

        // Try current wiki
        XWikiDocument userDocument = authenticateWiki(username, password, xcontext);

        // Fallback on main wiki (if not already on it)
        if (userDocument == null && !xcontext.isMainWiki()) {
            // Reset message
            removeMessage(xcontext);

            String currentWiki = xcontext.getWikiId();

            try {
                // Switch to main wiki
                xcontext.setWikiId(xcontext.getMainXWiki());

                userDocument = authenticateWiki(username, password, xcontext);
            } finally {
                // Restore current wiki
                xcontext.setWikiId(currentWiki);
            }
        }

        Principal principal;

        if (userDocument != null) {
            LOGGER.debug("Found valid user with reference [{}]", userDocument.getDocumentReference());

            principal = new SimplePrincipal(this.compactSerializer.serialize(userDocument.getDocumentReference()));

            LOGGER.debug("User principal is [{}]", principal.toString());
        } else if (this.configuration.fallback()) {
            // Reset message
            removeMessage(xcontext);

            // Fallback on standard XWiki authentication (if enabled)
            LOGGER.debug("Fallback on standard XWiki authentication (user id)");

            principal = super.authenticate(username, password, xcontext);
        } else {
            principal = null;
        }

        return principal;
    }

    private void setMessage(String message, XWikiContext xcontext)
    {
        xcontext.put(MESSAGE, message);
    }

    private void removeMessage(XWikiContext xcontext)
    {
        xcontext.remove(MESSAGE);
    }

    private XWikiDocument authenticateWiki(String fieldValue, String password, XWikiContext xcontext)
    {
        LOGGER.debug("Authenticate on wiki [{}]", xcontext.getWikiId());

        try {
            XWikiDocument userDocument = this.authenticator.authenticate(fieldValue, password, xcontext);

            if (userDocument == null) {
                LOGGER.debug("Did not found any valid user");

                setMessage("invalidcredentials", xcontext);
            }

            return userDocument;
        } catch (Exception e) {
            LOGGER.debug("Failed to authenticate", e);

            setMessage("loginfailed", xcontext);
        }

        return null;
    }
}
