/**
 * 
 */
package com.anthem.nimbus.platform.spec.model.dsl.binder;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Transient;

import com.anthem.nimbus.platform.spec.model.command.ValidationResult;
import com.anthem.nimbus.platform.spec.model.dsl.binder.DomainState.Model;
import com.anthem.nimbus.platform.spec.model.dsl.binder.Notification.ActionType;
import com.anthem.nimbus.platform.spec.model.dsl.config.ModelConfig;
import com.anthem.nimbus.platform.spec.model.util.CollectionsTemplate;
import com.anthem.nimbus.platform.spec.model.util.StateAndConfigSupportProvider;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Soham Chakravarti
 *
 */
@Getter @Setter
public class DomainModelState<T> extends AbstractDomainState<T> implements Model<T>, Serializable {
	
    private static final long serialVersionUID = 1L;

    @JsonIgnore final private Param<T> associatedParam;
    
    private List<Param<? extends Object>> params;

	@JsonIgnore private transient ValidationResult validationResult;
	
	@JsonIgnore private ExecutionStateTree executionStateTree;
	
	public DomainModelState(Param<T> associatedParam, ModelConfig<T> config, StateAndConfigSupportProvider provider/*, Model<?> backingCoreModel*/) {
		super(config, provider);
		
		Objects.requireNonNull(associatedParam, "Associated Param for Model must not be null.");
		this.associatedParam = associatedParam;
		super.setPath(getAssociatedParam().getPath());
	}

	@Override
	public ModelConfig<T> getConfig() {
		return (ModelConfig<T>)super.getConfig();
	}
	
	
	@Transient @JsonIgnore @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
	private final transient CollectionsTemplate<List<DomainState.Param<? extends Object>>, DomainState.Param<? extends Object>> templateParams = new CollectionsTemplate<>(
			() -> getParams(), (p) -> setParams(p), () -> new LinkedList<>());

	@JsonIgnore @Override
	public CollectionsTemplate<List<DomainState.Param<?>>, DomainState.Param<?>> templateParams() {
		return templateParams;
	}
	
	
	@JsonIgnore @Override
	public RootModel<?> getRootModel() {
		if(getAssociatedParam() == null) return this.findIfRoot();
		
		return getAssociatedParam().getRootModel();
	}
	
	@Override
	public T instantiateOrGet() {
		T instance = getProvider().getParamStateGateway()._getRaw(this.getAssociatedParam());
		return instance==null ? instantiateAndSet() : instance; 
	}
	
	@Override
	public T instantiateAndSet() {
		T newInstance =  getProvider().getParamStateGateway()._instantiateAndSet(this.getAssociatedParam());
		 
		getAssociatedParam().notifySubscribers(new Notification<>(this.getAssociatedParam(), ActionType._newModel, this.getAssociatedParam()));
		return newInstance;
	}
	
	@Override
	public <P> Param<P> findParamByPath(String[] pathArr) {
//		if(ArrayUtils.isEmpty(pathArr)) {//return null; 
//			return (getAssociatedParam() != null) ? getAssociatedParam().findParamByPath(getPath()) : null;
//		}
		return getAssociatedParam()==null ? null : getAssociatedParam().findParamByPath(pathArr);
	}

	
//	@JsonIgnore @Override
//	public void setState(T selectedValue) {
//		boolean hasAccess = false;
//		ClientUser cu = this.getProvider().getClientUser();
//		Action a = setStateInternal(selectedValue);
//		if(a != null){
//			ClientUserAccessBehavior clientUserAccessBehavior = cu.newBehaviorInstance(ClientUserAccessBehavior.class);
//			hasAccess = clientUserAccessBehavior.canUserPerformAction(getPath(),a.name());
//		}
//		
//		if(a != null) {
//			rippleToMapsTo();
//		}
//		
//		//safe to cast as the constructor restricts to only Param type for parent
//		if(hasAccess){
//			setStateAndNotifyObservers(selectedValue);
//			if(this.getParent() == null) {
//				emitEvent(this);
//			}
//			else{
//				emitEvent(getParent());
//			}
//		}else{
//			if(a != null){
//				logit.debug(()->"User does not have access to set state on ["+getPath()+"] for action : ["+a+"]");
//				throw new UserNotAuthorizedException("User not authorized to set state on ["+getPath()+"] for action : ["+a+"]" +this);
//			}
//		}
//			
//	}
	

	/**
	 * 
	 */
//	@Override
//	public void validateAndSetState(T state) {
//		validate(state);
//		
//		if(CollectionUtils.isNotEmpty(this.validationResult.getErrors())) {
//			throw new ValidationException(this.validationResult);
//	    } 
//		else {
//	    	setState(state);
//	    }
//	}
	
	/**
	 * 
	 * @param state
	 */
//	private void validate(T state) {
//		this.validationResult = new ValidationResult();
//	    this.validationResult.addValidationErrors(getProvider().getValidatorProvider().getValidator().validate(state));
//	}
	
	
}