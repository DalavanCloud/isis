/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.core.runtime.system.transaction;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.services.audit.AuditerService;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.applib.services.iactn.InteractionContext;
import org.apache.isis.applib.services.user.UserService;
import org.apache.isis.applib.services.xactn.Transaction;
import org.apache.isis.applib.services.xactn.TransactionService;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facets.actions.action.invocation.CommandUtil;
import org.apache.isis.core.metamodel.facets.object.audit.AuditableFacet;

/**
 * Wrapper around {@link org.apache.isis.applib.services.audit.AuditerService}.
 */
@Singleton
public class AuditingServiceInternal {

    Boolean whetherCanAudit;

    private boolean canAudit() {
        if(whetherCanAudit == null) {
            whetherCanAudit = determineWhetherCanAudit();
        }
        return whetherCanAudit;
    }

    private boolean determineWhetherCanAudit() {
        for (final AuditerService auditerService : auditerServices) {
            if (auditerService.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    @Programmatic
    public void audit() {
        if(!canAudit()) {
            return;
        }
        final Set<Map.Entry<AdapterAndProperty, PreAndPostValues>> changedObjectProperties =
                changedObjectsServiceInternal.getChangedObjectProperties();

        final String currentUser = userService.getUser().getName();
        final java.sql.Timestamp currentTime = clockService.nowAsJavaSqlTimestamp();

        for (Map.Entry<AdapterAndProperty, PreAndPostValues> auditEntry : changedObjectProperties) {
            auditChangedProperty(currentTime, currentUser, auditEntry);
        }
    }

    private void auditChangedProperty(
            final java.sql.Timestamp timestamp,
            final String user,
            final Map.Entry<AdapterAndProperty, PreAndPostValues> auditEntry) {

        final AdapterAndProperty aap = auditEntry.getKey();
        final ObjectAdapter adapter = aap.getAdapter();

        final AuditableFacet auditableFacet = adapter.getSpecification().getFacet(AuditableFacet.class);
        if(auditableFacet == null || auditableFacet.isDisabled()) {
            return;
        }

        final Bookmark target = aap.getBookmark();
        final String propertyId = aap.getPropertyId();
        final String memberId = aap.getMemberId();

        final PreAndPostValues papv = auditEntry.getValue();
        final String preValue = papv.getPreString();
        final String postValue = papv.getPostString();

        final String targetClass = CommandUtil.targetClassNameFor(adapter);

        Transaction transaction = transactionService.currentTransaction();
        final UUID transactionId = transaction.getUniqueId();
        final int sequence = transaction.getSequence();

        for (AuditerService auditerService : auditerServices) {
            if (auditerService.isEnabled()) {
                auditerService
                .audit(transactionId, sequence, targetClass, target, memberId, propertyId, preValue, postValue, user, timestamp);
            }
        }
    }

    @Inject @Any Instance<AuditerService> auditerServices;
    @Inject ChangedObjectsServiceInternal changedObjectsServiceInternal;
    @Inject UserService userService;
    @Inject ClockService clockService;
    @Inject InteractionContext interactionContext;
    @Inject TransactionService transactionService;

}
