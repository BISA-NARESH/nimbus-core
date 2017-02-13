/**
 * 
 */
package com.anthem.oss.nimbus.core.api.domain.state;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.anthem.nimbus.platform.spec.model.dsl.binder.FlowState;
import com.anthem.nimbus.platform.spec.model.util.StateAndConfigSupportProvider;
import com.anthem.oss.nimbus.core.domain.Command;
import com.anthem.oss.nimbus.core.domain.InvalidConfigException;
import com.anthem.oss.nimbus.core.domain.model.ModelConfig;
import com.anthem.oss.nimbus.core.domain.model.ParamConfig;
import com.anthem.oss.nimbus.core.domain.model.ParamType;
import com.anthem.oss.nimbus.core.domain.model.state.ExecutionState;
import com.anthem.oss.nimbus.core.domain.model.state.DefaultListElemParamState;
import com.anthem.oss.nimbus.core.domain.model.state.DefaultListModelState;
import com.anthem.oss.nimbus.core.domain.model.state.DefaultModelState;
import com.anthem.oss.nimbus.core.domain.model.state.DefaultParamState;
import com.anthem.oss.nimbus.core.domain.model.state.StateAndConfigMeta;
import com.anthem.oss.nimbus.core.domain.model.state.StateType;
import com.anthem.oss.nimbus.core.domain.model.state.DomainState.Model;

/**
 * @author Soham Chakravarti
 *
 */
@Component
public class DomainStateBuilder extends AbstractDomainStateBuilder {

	public <V, C> ExecutionState<V, C>.ExModel buildExec(Command cmd, StateAndConfigSupportProvider provider, ExecutionState<V, C> eState, StateAndConfigMeta.View<V, C> viewMeta) {
		return buildExec(cmd, provider, eState, viewMeta.getExecutionConfig());
	}
	
	public <V, C> ExecutionState<V, C>.ExModel buildExec(Command cmd, StateAndConfigSupportProvider provider, ExecutionState<V, C> eState, ExecutionState.Config<V, C> exConfig) {
		ExecutionState<V, C>.ExModel execModelSAC = eState.new ExParam(cmd, provider, exConfig).getRootModel().unwrap(ExecutionState.ExModel.class);
		
		// core param sac
		DefaultParamState<C> coreParamSAC = buildParam(provider, execModelSAC, execModelSAC.getConfig().getCoreParam(), null);
		execModelSAC.templateParams().add(coreParamSAC);
		
		// view param sac
		if(exConfig.getView()!=null) {
			DefaultParamState<V> viewParamSAC = buildParam(provider, execModelSAC, execModelSAC.getConfig().getViewParam(), execModelSAC);
			execModelSAC.templateParams().add(viewParamSAC);
		}
		
		// flow param sac
		if(exConfig.getFlow()!=null) {
			DefaultParamState<FlowState> flowParamSAC = buildParam(provider, execModelSAC, execModelSAC.getConfig().getFlowParam(), null);
			execModelSAC.templateParams().add(flowParamSAC);
		}
		
		return execModelSAC;
	}

	@Override
	public <T, P> DefaultParamState<P> buildParam(StateAndConfigSupportProvider provider, DefaultModelState<T> mState, ParamConfig<P> mpConfig, Model<?> mapsToSAC) {
		final DefaultParamState<P> mpState = createParam(provider, mState, mapsToSAC, mpConfig);
		logit.debug(()->"[buildInternal] mpStatePath: "+ mpState.getPath());
		
		//handle param type
		StateType type = buildParamType(provider, mpConfig, mState, mpState, mapsToSAC);
		mpState.setType(type);
		
		return mpState;
	}
	
	public <T, P> DefaultModelState<T> buildModel(StateAndConfigSupportProvider provider, DefaultParamState<T> parentState, ModelConfig<T> mConfig, DefaultModelState<?> mapsToSAC) {
		return buildModelInternal(provider, parentState, mConfig, mapsToSAC);
	}
	

	protected <T, P> DefaultModelState<T> buildModelInternal(StateAndConfigSupportProvider provider, DefaultParamState<T> associatedParam, ModelConfig<T> mConfig, Model<?> mapsToSAC) {
		
		if(mConfig==null) return null;

		/* if model & param are mapped, then  mapsToSAC must not be null */
		if(mConfig.isMapped() && mapsToSAC==null) 
			throw new InvalidConfigException("Model class: "+mConfig.getReferredClass()+" is mapped: "+mConfig.findIfMapped().getMapsTo().getReferredClass()
						+" but mapsToSAC is not supplied for param: "+associatedParam.getPath()+". Was this model's config loaded first as part of core?");
		
		DefaultModelState<T> mState = createModel(associatedParam, mConfig, provider, mapsToSAC); //(provider, parentState, mConfig, mGet, mSet, mapsToSAC);
		
		if(mConfig.getParams()==null) return mState;
		
		/* iterate through config params and create state instances in the same order */
		for(ParamConfig<?> mpConfigRawType : mConfig.getParams()) {
			@SuppressWarnings("unchecked")
			final ParamConfig<P> mpConfig = (ParamConfig<P>)mpConfigRawType;
			
			final DefaultParamState<P> mpState = buildParam(provider, mState, mpConfig, mapsToSAC);
			 
			/* add param state to model state in same order */
			mState.templateParams().add(mpState);
		}
		
		return mState;
	}
	
	public <E> DefaultListElemParamState<E> buildElemParam(StateAndConfigSupportProvider provider, DefaultListModelState<E> mState, ParamConfig<E> mpConfig, String elemId) {
		final DefaultListElemParamState<E> mpState = createElemParam(provider, mState, mpConfig, elemId);
		logit.debug(()->"[buildInternal] mpStatePath: "+ mpState.getPath());
		

		//handle param type
		StateType type = buildParamType(provider, mpConfig, mState, mpState, Optional.ofNullable(mState.findIfMapped()).map(m->m.getMapsTo()).orElse(null));
		mpState.setType(type);
		
		return mpState;
	}
	
	protected <P> StateType buildParamType(StateAndConfigSupportProvider provider, ParamConfig<P> mpConfig, DefaultModelState<?> mState, DefaultParamState<P> associatedParam, Model<?> mapsToSAC) {
		
		if(mpConfig.getType().isCollection()) {
			ParamType.NestedCollection<P> nmcType = mpConfig.getType().findIfCollection();
			ModelConfig<List<P>> nmConfig = nmcType.getModel();
			
			DefaultListElemParamState.Creator<P> elemCreator = (colModelState, elemId) -> buildElemParam(provider, colModelState, colModelState.getElemConfig(), elemId);
			DefaultListModelState<P> nmcState = createCollectionModel(associatedParam.findIfCollection(), nmConfig, provider, elemCreator); 
			
			StateType.NestedCollection<P> nctSAC = new StateType.NestedCollection<>(nmcType, nmcState);
			return nctSAC;
			
		} else if(mpConfig.getType().isNested()) {
			//handle nested model
			@SuppressWarnings("unchecked")
			ParamType.Nested<P> mpNmType = ((ParamType.Nested<P>)mpConfig.getType());
			
			ModelConfig<P> mpNmConfig = mpNmType.getModel();
			
			/* determine mapsTo model SAC for param's nested model */
			final Model<?> mpNmMapsToSAC;
			
			if(!associatedParam.isMapped()) {
				mpNmMapsToSAC = mapsToSAC;
			} else if(associatedParam.getConfig().getType().isNested()) {
				mpNmMapsToSAC = associatedParam.findIfMapped().getMapsTo().getType().findIfNested().getModel();
			} else { // leaf
				mpNmMapsToSAC = associatedParam.findIfMapped().getMapsTo().getParentModel();
			}
			
			
			/* create nested model SAC */
			DefaultModelState<P> nmState = buildModelInternal(provider, associatedParam, mpNmConfig, mpNmMapsToSAC);
			StateType.Nested<P> ntState = new StateType.Nested<>(mpNmType, nmState);
			return ntState;
			
		} else {
			
			return new StateType(mpConfig.getType());
		}
	}
	
}