package org.apache.isis.core.metamodel.facets.object.choices;

import java.util.function.Predicate;

import org.apache.isis.applib.query.QueryFindAllInstances;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.ObjectAdapterPredicate;

/**
 * 
 * Specialization of QueryFindAllInstances to honor also visibility. 
 * 
 * @since 2.0.0-M3
 *
 * @param <T>
 */
class QueryFindAllChoices<T> extends QueryFindAllInstances<T> implements ObjectAdapterPredicate {

	private static final long serialVersionUID = 1L;

	private final Predicate<ObjectAdapter> visibilityFilter;
	
	public QueryFindAllChoices(
			final String string, 
			final Predicate<ObjectAdapter> visibilityFilter, 
			final long ... range) {
		
        super(string, range);
        this.visibilityFilter = visibilityFilter;
    }

	@Override
	public boolean test(ObjectAdapter objectAdapter) {
		return visibilityFilter.test(objectAdapter);
	}

}
