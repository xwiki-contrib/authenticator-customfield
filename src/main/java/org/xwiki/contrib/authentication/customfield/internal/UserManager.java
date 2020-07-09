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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

/**
 * Various tools to manipulate users.
 * 
 * @version $Id$
 */
@Component(roles = UserManager.class)
@Singleton
public class UserManager
{
    /**
     * The local reference of a user object.
     */
    public static final LocalDocumentReference USERCLASS_REFERENCE = new LocalDocumentReference("XWiki", "XWikiUsers");

    @Inject
    private QueryManager queryManager;

    @Inject
    private Logger logger;

    /**
     * @param fieldName the name of the field containing the user identifier
     * @param fieldValue the value to match with the existing fields
     * @param caseSensitive true of the value of the uid should be compared in a case sensitive way
     * @param warn true if several user with the same identifier should produce a warning
     * @return the reference of the user profile document containing the passed identifier
     * @throws QueryException when failing to get the user with the passed identifier
     * @since 1.1
     */
    public String getUser(String fieldName, String fieldValue, boolean caseSensitive, boolean warn)
        throws QueryException
    {
        StringBuilder builder = new StringBuilder("from doc.object(XWiki.XWikiUsers) as user where ");
        if (!caseSensitive) {
            builder.append("lower(");
        }
        builder.append("user.");
        builder.append(fieldName);
        if (!caseSensitive) {
            builder.append(')');
        }
        builder.append(" = :fieldValue");

        Query query = this.queryManager.createQuery(builder.toString(), Query.XWQL);
        query.bindValue("fieldValue", caseSensitive ? fieldValue : fieldName.toLowerCase());

        this.logger.debug("Executing query [{}] with fieldValue={}", query.getStatement(), fieldValue);

        List<String> documents = query.execute();

        if (documents.isEmpty()) {
            return null;
        }

        if (warn && documents.size() > 1) {
            this.logger.warn("Several users could be found with field [{}] having value [{}]", fieldName, fieldValue);
        }

        return documents.get(0);
    }
}
