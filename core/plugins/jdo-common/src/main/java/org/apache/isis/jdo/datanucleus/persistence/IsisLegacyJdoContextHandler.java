package org.apache.isis.jdo.datanucleus.persistence;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.isis.commons.internal.cdi._CDI;
import org.apache.isis.commons.internal.debug._Probe;
import org.apache.isis.commons.internal.exceptions._Exceptions;
import org.apache.isis.commons.internal.uri._URI.ContainerType;
import org.apache.isis.commons.internal.uri._URI.ContextType;
import org.apache.isis.core.metamodel.adapter.oid.Oid;
import org.apache.isis.core.metamodel.spec.ManagedObject;
import org.apache.isis.core.metamodel.spec.ManagedObject.SimpleManagedObject;
import org.apache.isis.core.metamodel.spec.ManagedObjectState;
import org.apache.isis.core.metamodel.spec.ObjectSpecId;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.specloader.SpecificationLoader;
import org.apache.isis.core.runtime.system.context.managers.AuthorityDescriptor;
import org.apache.isis.core.runtime.system.context.managers.ContextHandler;
import org.apache.isis.core.runtime.system.session.IsisSession;
import org.apache.isis.jdo.IsisJdoRuntimePlugin;

import lombok.val;

/**
 * 
 * @since 2.0.0-M3
 *
 */
@Singleton
public class IsisLegacyJdoContextHandler implements ContextHandler {

	private final static _Probe probe = _Probe.unlimited().label("IsisLegacyJdoContextHandler");
	
	@Inject SpecificationLoader specLoader;
	
	private IsisJdoRuntimePlugin jdoRuntimePlugin;
	
	@PostConstruct
	public void init() {
		jdoRuntimePlugin = IsisJdoRuntimePlugin.get();
	}
	
	@Override
	public ManagedObjectState stateOf(ManagedObject managedObject) {
		val persistenceSession = IsisSession.currentIfAny().getPersistenceSession();
		val state = persistenceSession.stateOf(managedObject.getPojo());
		return state;
	}

	@Override
	public URI uriOf(ManagedObject managedObject) {
		
		val pojo = managedObject.getPojo();
		val spec = managedObject.getSpecification();
		
    	try {
			
    		val objectUri = identifierForPersistable(pojo, spec);
			
    		return objectUri;
			
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw _Exceptions.unrecoverable(e);
		}
	}

	@Override
	public Instance<ManagedObject> resolve(ObjectSpecId specId, URI objectUri) {
		
		val spec = specLoader.lookupBySpecId(specId);
		val id = objectUri.getQuery();
		
		val rootOid = Oid.Factory.persistentOf(specId, id);
		
		probe.println("resolve '%s' rootOid.id='%s'", objectUri, rootOid.getIdentifier());

		val persistenceSession = IsisSession.currentIfAny().getPersistenceSession();
		val pojo = persistenceSession.fetchPersistentPojo(rootOid);

//TODO [2033] remove, this was the old way of doing it ...		
//		val objectAdapter = persistenceSession.adapterFor(rootOid);
//		val pojo = objectAdapter.getPojo();
		
		val managedObject = SimpleManagedObject.of(spec, pojo);
		
		return _CDI.InstanceFactory.singleton(managedObject);
		
	}

	@Override
	public boolean recognizes(ObjectSpecification spec) {
		return jdoRuntimePlugin.isPersistenceEnhanced(spec.getCorrespondingClass());
	}

	@Override
	public boolean recognizes(URI uri) {
		return DEFAULT_AUTHORITY.matches(uri);
	}

	@Override
	public Optional<AuthorityDescriptor> authorityFor(ObjectSpecification spec) {
		if(!recognizes(spec)) {
			return Optional.empty();
		}
		return Optional.of(DEFAULT_AUTHORITY);
	}
	
	// -- HELPER
	
	private final static AuthorityDescriptor DEFAULT_AUTHORITY =
			AuthorityDescriptor.of(ContainerType.jdo, ContextType.entities, null);
			
	
	private URI identifierForPersistable(Object persistable, ObjectSpecification spec) 
    		throws NoSuchMethodException, SecurityException, IllegalAccessException, 
    		IllegalArgumentException, InvocationTargetException {
		
		val persistenceSession = IsisSession.currentIfAny().getPersistenceSession();
		
        val isRecognized = persistenceSession.isRecognized(persistable);
        
        val identifier = isRecognized
        		? persistenceSession.identifierFor(persistable)
        				: "1";
        		
        probe.println("identifierForPersistable spec='%s' pojo='%s' recogniced='%b'", 
        		spec.getSpecId().asString(), persistable, isRecognized);
    		
		return DEFAULT_AUTHORITY.toUoidDtoBuilder(spec.getSpecId())
				.query(identifier)
				.build()
				.toURI();
    }
	
	
}

