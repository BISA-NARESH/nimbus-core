/**
 * 
 */
package com.anthem.oss.nimbus.core.domain.model.state;

import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.anthem.nimbus.platform.spec.model.util.StateAndConfigSupportProvider;
import com.anthem.oss.nimbus.core.domain.Action;
import com.anthem.oss.nimbus.core.domain.model.state.DomainState.Param;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.Setter;

/**
 * @author Soham Chakravarti
 *
 */
public class SimpleState<T> extends AbstractState<T> implements Observer {

	@JsonIgnore final private DomainState<?> parent;
	@JsonIgnore @Setter(AccessLevel.PROTECTED) private transient Supplier<T> getter;
	@JsonIgnore @Setter(AccessLevel.PROTECTED) private transient Consumer<T> setter;
	
	public SimpleState(Param<?> parent, Supplier<T> getter, Consumer<T> setter, StateAndConfigSupportProvider provider) {
		super(provider);
		this.parent = parent;
		setGetter(getter);
		setSetter(setter);
	}
	
	@Override
	public T getState() {
		return getter.get();
	}
	@Override
	public Action setState(T state) {
		setter.accept(state);
		return Action._replace;
	}
	
	/**
	 * Set the state to mapped state (changed)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void update(Observable o, Object changedState) {
		setState((T)changedState);
	}
	
//	@Override
//	public void setState(T newState) {
//		Action a = setStateInternal(newState);
//		
//		if(a != null) {
//			setStateAndNotifyObservers(newState);
//			
//			emitEvent((ParamStateAndConfig<?>)getParent());
//		}
//	}
	
	@Override
	public void validateAndSetState(T state)  {
		//throw new OperationNotSupportedException("Cannot validate and set state for SimplateState type: "+state);
	}
	
}