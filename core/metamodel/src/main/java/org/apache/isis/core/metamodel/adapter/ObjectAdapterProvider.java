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
package org.apache.isis.core.metamodel.adapter;

import javax.annotation.Nullable;

import org.apache.isis.core.metamodel.adapter.oid.RootOid;
import org.apache.isis.core.metamodel.spec.ManagedObject;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.OneToManyAssociation;

/**
 * 
 * @since 2.0.0-M2
 *
 */
public interface ObjectAdapterProvider {
    
    // -- INTERFACE

    /**
     * @return standalone (value) or root adapter
     */
    @Nullable ObjectAdapter adapterFor(@Nullable Object domainObject);
    
    /**
     * @return collection adapter.
     */
    ObjectAdapter adapterForCollection(
            Object domainObject,
            RootOid parentOid,
            OneToManyAssociation collection);

    ObjectAdapter adapterForViewModel(Object viewModelPojo, String mementoStr);
    

    // -- DOMAIN OBJECT CREATION SUPPORT
    
    /**
     * <p>
     * Creates a new instance of the specified type and returns it.
     *
     * <p>
     * The returned object will be initialized (had the relevant callback
     * lifecycle methods invoked).
     *
     * <p>
     * While creating the object it will be initialized with default values and
     * its created lifecycle method (its logical constructor) will be invoked.
     *
     */
    ObjectAdapter newTransientInstance(ObjectSpecification objectSpec);
    
    @Nullable ObjectAdapter adapterForViewModelMementoString(ObjectSpecification objectSpec, @Nullable final String memento);
    
    
    // -- FOR THOSE THAT IMPLEMENT THROUGH DELEGATION
    
    public static interface Delegating extends ObjectAdapterProvider {
        
        ObjectAdapterProvider getObjectAdapterProvider();
        
        default ObjectAdapter adapterFor(Object domainObject) {
            return getObjectAdapterProvider().adapterFor(domainObject);
        }
        
        default ObjectAdapter adapterForServicePojo(Object servicePojo) {
        	return getObjectAdapterProvider().adapterFor(servicePojo);
        }

        default ObjectAdapter adapterForCollection(
                final Object pojo,
                final RootOid parentOid,
                OneToManyAssociation collection) {
            return getObjectAdapterProvider().adapterForCollection(pojo, parentOid, collection);
        }

        default ObjectAdapter adapterForViewModel(final Object viewModelPojo, final String mementoString) {
            return getObjectAdapterProvider().adapterForViewModel(viewModelPojo, mementoString);
        }
        
        default ObjectAdapter newTransientInstance(ObjectSpecification objectSpec) {
            return getObjectAdapterProvider().newTransientInstance(objectSpec);
        }
        
        default ObjectAdapter adapterForViewModelMementoString(ObjectSpecification objectSpec, final String memento) {
            return getObjectAdapterProvider().adapterForViewModelMementoString(objectSpec, memento);
        }
        
    }
    

}
