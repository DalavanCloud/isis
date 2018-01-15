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

package org.apache.isis.core.metamodel.facets.object.navparent.annotation;

import java.beans.IntrospectionException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.isis.applib.annotation.Parent;
import org.apache.isis.core.commons.config.IsisConfiguration;
import org.apache.isis.core.commons.lang.NullSafe;
import org.apache.isis.core.commons.reflection.Reflect;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facetapi.FacetUtil;
import org.apache.isis.core.metamodel.facetapi.FeatureType;
import org.apache.isis.core.metamodel.facetapi.MetaModelValidatorRefiner;
import org.apache.isis.core.metamodel.facets.Annotations;
import org.apache.isis.core.metamodel.facets.FacetFactoryAbstract;
import org.apache.isis.core.metamodel.facets.object.navparent.method.NavigableParentFacetMethod;
import org.apache.isis.core.metamodel.services.ServicesInjector;
import org.apache.isis.core.metamodel.services.persistsession.PersistenceSessionServiceInternal;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.specloader.validator.MetaModelValidatorComposite;
import org.apache.isis.core.metamodel.specloader.validator.MetaModelValidatorVisiting;
import org.apache.isis.core.metamodel.specloader.validator.ValidationFailures;

/**
 * For detailed behavioral specification see 
 * <a href="https://issues.apache.org/jira/browse/ISIS-1816">ISIS-1816</a>.
 * 
 * @author ahuber@apache.org
 * @since 2.0.0
 *
 */
public class NavigableParentAnnotationFacetFactory extends FacetFactoryAbstract implements MetaModelValidatorRefiner {

    public NavigableParentAnnotationFacetFactory() {
        super(FeatureType.OBJECTS_ONLY);
    }

    @Override
    public void process(final ProcessClassContext processClassContext) {
        final Class<?> cls = processClassContext.getCls();
        final FacetHolder facetHolder = processClassContext.getFacetHolder();

        // Starting from the current domain-object class, we search down the object 
        // inheritance hierarchy (super class, super super class, ...), until we find 
        // the first class that has a @Parent annotation. That's the one we use to 
        // resolve the current domain-object's navigable parent. 
        
        final List<Annotations.Evaluator<Parent>> evaluators = 
        		Annotations.findFirstInHierarchyHaving(cls, Parent.class);
        
        if (NullSafe.isEmpty(evaluators)) {
            return; // no parent resolvable
        } else if (evaluators.size()>1) {
        	// code should not be reached, since case should be handled by meta-data validation
        	throw new RuntimeException("unable to determine navigable parent due to ambiguity");
        }
        
        final Annotations.Evaluator<Parent> parentEvaluator = evaluators.get(0);
        
        final Method method;

        // find method that provides the parent ...
        if(parentEvaluator instanceof Annotations.MethodEvaluator) {
        	// we have a @Parent annotated method
        	method = ((Annotations.MethodEvaluator<Parent>) parentEvaluator).getMethod();
        } else if(parentEvaluator instanceof Annotations.FieldEvaluator) {
        	// we have a @Parent annotated field (occurs if one uses lombok's @Getter on a field)
        	final Field field = ((Annotations.FieldEvaluator<Parent>) parentEvaluator).getField();
        	try {
				method = Reflect.getGetter(cls, field.getName());
			} catch (IntrospectionException e) {
				return; // no parent resolvable
			}
        } else {
        	return; // no parent resolvable
        }
        
        try {
			FacetUtil.addFacet(new NavigableParentFacetMethod(method, facetHolder));
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
    }


    /**
     * For detailed behavioral specification see 
     * <a href="https://issues.apache.org/jira/browse/ISIS-1816">ISIS-1816</a>.
     */
    @Override
    public void refineMetaModelValidator(MetaModelValidatorComposite metaModelValidator, IsisConfiguration configuration) {
        metaModelValidator.add(new MetaModelValidatorVisiting(new MetaModelValidatorVisiting.Visitor() {
        	
            @Override
            public boolean visit(ObjectSpecification objectSpec, ValidationFailures validationFailures) {
                final Class<?> cls = objectSpec.getCorrespondingClass();
                
                final List<Annotations.Evaluator<Parent>> evaluators = 
                		Annotations.findFirstInHierarchyHaving(cls, Parent.class);
                
                if (NullSafe.isEmpty(evaluators)) {
                	return true; // no conflict
                } else if (evaluators.size()>1) {
                	
                	validationFailures.add(
                            "%s: conflict for determining a strategy for retrieval of (navigable) parent for class, "
                            + "contains multiple annotations '@%s', while at most one is allowed.",
                            objectSpec.getIdentifier().getClassName(),
                            Parent.class.getName());
                }
                
                return true; // no conflict
                
            }

        }));
    }

    // -- ADAPTER INJECTION
    
    @Override
    public void setServicesInjector(final ServicesInjector servicesInjector) {
        super.setServicesInjector(servicesInjector);
        adapterManager = servicesInjector.getPersistenceSessionServiceInternal();
    }

    PersistenceSessionServiceInternal adapterManager;
    
}
