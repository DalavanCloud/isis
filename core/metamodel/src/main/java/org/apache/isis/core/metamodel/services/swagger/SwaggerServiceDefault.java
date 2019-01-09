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
package org.apache.isis.core.metamodel.services.swagger;

import static org.apache.isis.commons.internal.base._Strings.prefix;
import static org.apache.isis.commons.internal.base._With.ifPresentElse;
import static org.apache.isis.commons.internal.resources._Resources.getRestfulPathIfAny;
import static org.apache.isis.commons.internal.resources._Resources.prependContextPathIfPresent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.services.swagger.SwaggerService;
import org.apache.isis.core.metamodel.services.swagger.internal.SwaggerSpecGenerator;
import org.apache.isis.core.metamodel.specloader.SpecificationLoader;

@Singleton
public class SwaggerServiceDefault implements SwaggerService {

    // private final static Logger LOG = LoggerFactory.getLogger(SwaggerServiceDefault.class);

    private String basePath;

    @PostConstruct
    public void init() {

        final String restfulPath = ifPresentElse(getRestfulPathIfAny(), "undefined");
        
        this.basePath = prefix(prependContextPathIfPresent(restfulPath), "/");
    }

    @Programmatic
    @Override
    public String generateSwaggerSpec(
            final Visibility visibility,
            final Format format) {

        final SwaggerSpecGenerator swaggerSpecGenerator = new SwaggerSpecGenerator(specificationLoader);
        final String swaggerSpec = swaggerSpecGenerator.generate(basePath, visibility, format);
        return swaggerSpec;
    }

    @Inject SpecificationLoader specificationLoader;

}
