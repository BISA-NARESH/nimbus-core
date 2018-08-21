import { ValidationUtils } from './../../validators/ValidationUtils';
/**
 * @license
 * Copyright 2016-2018 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';
import { ControlValueAccessor } from '@angular/forms/src/directives';
import { Param, Values } from '../../../../shared/param-state';
import { Component, forwardRef, Input, OnInit, ViewChild, Output, EventEmitter } from '@angular/core';
import { FormGroup, NG_VALUE_ACCESSOR, ValidatorFn } from '@angular/forms';

import { WebContentSvc } from '../../../../services/content-management.service';
import { PageService } from '../../../../services/page.service';
import { PickList } from 'primeng/primeng';
import { BaseElement } from '../../base-element.component';
import { MAX_LENGTH_VALIDATOR } from '@angular/forms/src/directives/validators';
import { BaseControl } from './base-control.component';

export const CUSTOM_INPUT_CONTROL_VALUE_ACCESSOR: any = {
  provide: NG_VALUE_ACCESSOR,
  useExisting: forwardRef(() => OrderablePickList),
  multi: true
};

/**
 * \@author Dinakar.Meda
 * \@author Sandeep.Mantha
 * \@whatItDoes 
 * 
 * \@howToUse 
 * 
 */
@Component({
    selector: 'nm-pickList',
    providers: [
        CUSTOM_INPUT_CONTROL_VALUE_ACCESSOR,WebContentSvc
    ],
    template: `
    <!--<div [hidden]="!parent?.visible" *ngIf="parent.config?.uiStyles?.attributes?.hidden==false">-->
        <label [attr.for]="parent.config?.code"  [ngClass]="{'required': requiredCss, '': !requiredCss}">{{label}}
            <nm-tooltip *ngIf="helpText" [helpText]='helpText'></nm-tooltip>
        </label>
        <ng-template ngFor let-param [ngForOf]="parent?.type?.model?.params">
            <ng-template [ngIf]= "param?.config?.uiStyles?.attributes?.alias === 'PickListAvailable'">
            
            <div> 
            <fieldset [disabled]="false">
                <p-pickList #picklist [source]="parent.values" 
                    filterBy="label"
                    [sourceHeader] = "'Available '" 
                    [targetHeader]="'Selected'" 
                    [disabled]="!parent.enabled"
                    [target]="targetList" 
                    pDroppable="dd" 
                    [responsive]="true" 
                    [showSourceControls]="false"
                    [showTargetControls]="false"
                    (onMoveToTarget)="updateListValues($event)"
                    (onMoveToSource)="updateListValues($event)">
                    <ng-template let-itm pTemplate="item">
                        <div class="ui-helper-clearfix">
                            <div style="font-size:14px;float:right;margin:15px 5px 0 0" pDraggable="dd"  
                                (onDragStart)="dragStart($event, itm)" (onDragEnd)="dragEnd($event)">
                                {{itm.label ? itm.label : getFromSelectedList(itm)}}
                            </div>
                        </div>
                    </ng-template>
                </p-pickList>   
             </fieldset>   
            </div>
        </ng-template>
    </ng-template>

           
        
       
   `
})

export class OrderablePickList extends BaseElement implements OnInit, ControlValueAccessor {
    @Input() element: Param;
    @Input() parent : Param;
    @Input() selectedvalues : Values[];
    sourcevalues: Values[];
    @Input() form: FormGroup;
    @Input('value') _value ;
    @ViewChild('picklist') pickListControl: PickList;
    targetList: any[];
    private draggedItm: any;
    private selectedOptions: string[] = [];
    private selectedPickListParam: Param;
    private _disabled: boolean;
    public onChange: any = (_) => { /*Empty*/ }
    public onTouched: any = () => { /*Empty*/ }
    @Output() controlValueChanged =new EventEmitter();

    @Input()
    get disabled(): boolean { return this._disabled; }

    set disabled(value) { this._disabled = value; }

    constructor(wcs: WebContentSvc, private pageService: PageService) {
        super(wcs);
    }

    ngOnInit() {
        this.loadLabelConfigByCode(this.parent.config.code, this.parent.config.labelConfigs);
        this.requiredCss = ValidationUtils.applyelementStyle(this.parent);
        //set the default target list when the page loads to the config state
        console.log('target list' + this.element.leafState);

        // First check if the picklist has any values that are selected onload
        if(this.element.leafState != null) {
            this.targetList = this.element.leafState;
        } else {
            this.targetList = [];
        }

        // populate the source list based on the target, i.e if the value is already present in target - do not show in source
    //    this.sourcevalues = this.showSourceList();

        this.controlValueChanged.subscribe(($event) => {
             if ($event.config.code === 'selected' && this.parent.config.uiStyles.attributes.postEventOnChange) {
                this.pageService.postOnChange($event.path , 'state', JSON.stringify($event.leafState));
             }
         });

        if( this.form!= null) {
            
        //    const frmCtrl = this.form.controls['category'].controls;
            const frmCtrl = this.form.controls[this.element.config.code];
            if(frmCtrl != null) {
                //rebind the validations as there are dynamic validations along with the static validations
                if(this.element.activeValidationGroups != null && this.element.activeValidationGroups.length > 0) {
                    this.requiredCss = ValidationUtils.rebindValidations(frmCtrl,this.element.activeValidationGroups,this.element);
                } 
                frmCtrl.valueChanges.subscribe(
                    ($event) => { this.setState($event,this); });

                this.pageService.eventUpdate$.subscribe(event => {
                    let frmCtrl = this.form.controls[event.config.code];
                    if(frmCtrl!=null && event.path.startsWith(this.element.path)) {
                        if(event.leafState!=null) {
                            frmCtrl.setValue(event.leafState);
                        } else {
                            frmCtrl.reset();
                        }
                    }
                });
                this.pageService.validationUpdate$.subscribe(event => {
                    let frmCtrl = this.form.controls[event.config.code];
                    if(frmCtrl!=null) {
                        if(event.path === this.element.path) {
                           //bind dynamic validations on a param as a result of a state change of another param
                            if(event.activeValidationGroups != null && event.activeValidationGroups.length > 0) {
                                this.requiredCss = ValidationUtils.rebindValidations(frmCtrl,event.activeValidationGroups,this.element);
                            } else {
                                this.requiredCss = ValidationUtils.applyelementStyle(this.element);
                                var staticChecks: ValidatorFn[] = [];
                                staticChecks = ValidationUtils.buildStaticValidations(this.element);
                                frmCtrl.setValidators(staticChecks);
                            }
                            ValidationUtils.assessControlValidation(event,frmCtrl);
                        }
                    }
                });
               
                this.controlValueChanged.subscribe(($event) => {
                    // if ($event.config.uiStyles.attributes.postEventOnChange) {
                        this.pageService.postOnChange($event.path , 'state', JSON.stringify($event.leafState));
                    // }
                 });
            }   
        }

        // FROM TONY
        // TODO Move this logic to ControlSubscribers: valuesUpdateSubscriber()
        this.pageService.eventUpdate$.subscribe(event => {
            if(event.path == this.parent.path) {
                // TODO write if condition to check if values have changed

                    console.log('source values updated');

                    // When the source values change, validate that any values present in the target
                    // are removed from the source values (if they are present)
                    if (this.targetList) {
                        for(var targetItem of this.targetList) {
                            this.parent.values = this.parent.values.filter(value => value.code != targetItem);
                        }
                    }
            }
        });
    }

    emitValueChangedEvent() {
        if(this.form == null || (this.form.controls[this.element.config.code]!= null && this.form.controls[this.element.config.code].valid)) {
            this.controlValueChanged.emit(this.element);
        }
        
    }

    setState(event:any, frmInp:any) {
        frmInp.element.leafState = event;
        this.element.leafState = event;
    }

    updateListValues(event: any) {
        if(this.targetList.length === 0) {
            this.value = null;
        } else {
            this.selectedOptions = [];
            this.targetList.forEach(element => {
                if (element.code) {
                    this.selectedOptions.push(element.code);
                } else {
                    this.selectedOptions.push(element);
                }
            });
            this.value = this.selectedOptions;
        }
    //    this.controlValueChanged.emit(this.parent);
        this.emitValueChangedEvent();
    }

    updateSourceListValues(event: any) {
        if(this.targetList.length === 0) {
            this.value = null;
        } else {
            this.selectedOptions = [];
            this.targetList.forEach(element => {
                if (element.code) {
                    this.selectedOptions.push(element.code);
                } else {
                    this.selectedOptions.push(element);
                }
            });
            this.value = this.selectedOptions;
        }
        //    this.controlValueChanged.emit(this.parent);
        // update the source values based on target list on load + onMoveToSource
        // this.parent.values = this.removeduplicates(event.items[0]);
        this.emitValueChangedEvent();
    }

    removeduplicates(item:any) : Values[]{
        //the new item will already be added to parent.values ; since primeng property source is bound to parent.values
        let codelist = [];
        this.parent.values.forEach(codeval => {
            if(codeval.code) {
                codelist.push(codeval.code);
            } else {
                codelist.push(codeval);
            }
        });

        var uniquecodes = codelist.filter(function(code,index) {
            return codelist.indexOf(code) >= index;
        });

        let valuelist: Values[];
        uniquecodes.forEach(code => {
            if(code !== undefined) {
                valuelist.push(this.convertCodeToValue(code));
            }
        });

        return valuelist;

    }

    get value() {
        return this._value;
    }

    set value(val) {
        this._value = val;
        this.onChange(val);
        this.onTouched();
    }

    dragStart(event, itm: any) {
        if(this.element.enabled) {
            this.draggedItm = itm;
        }
    }


    findIndexInList(item: Values, list: Values[]): number {
        let index: number = -1;
        if(list) {
            for(let i = 0; i < list.length; i++) {
                if(list[i].code === item.code) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    dragEnd(event) {
        if (this.draggedItm) {
            let index = this.findIndexInList(this.draggedItm,this.pickListControl.source);
            if(index >=0) {
                this.pickListControl.source.splice(index, 1);
                this.pickListControl.target.push(this.draggedItm);
            } else {
                index = this.findIndexInList(this.draggedItm,this.pickListControl.target);
                if(index >=0) {
                    this.pickListControl.target.splice(index, 1);
                    this.pickListControl.source.push(this.draggedItm);
                }
            }
            this.draggedItm = null;
            //updating the internal data model
            if(this.targetList.length === 0) {
                this.value = null;
            } else {
                this.selectedOptions = [];
                this.targetList.forEach(element => {
                    this.selectedOptions.push(element);
                });
                this.value = this.selectedOptions;
            }
        }
    }

    public writeValue(obj: any): void {
        if (obj !== undefined) {
        }
    }

    public registerOnChange(fn: any): void {
       this.onChange = fn;
    }

    public registerOnTouched(fn: any): void {
        this.onTouched = fn;
    }

    public setDisabledState(isDisabled: boolean) {
        this.disabled = isDisabled;
  }

   public getFromSelectedList(itm : any) : string {
        let displayVal: string;
        const values = this.selectedvalues;
        values.forEach(value => {
            if (value.code === itm) {
                displayVal = value.label;
            }
        });
        if (displayVal === undefined) {
            displayVal = itm;
        }
        return displayVal;
   }

    showSourceList() : Values[] {
        let configSrcList: Values[];
        const availableParam : Param = this.parent.type.model.params.find( param => 
            param.config.uiStyles.attributes.alias === 'PickListAvailable');

        configSrcList = availableParam.values;
        this.sourcevalues = configSrcList;
        if (this.targetList && this.targetList.length > 0) {
            configSrcList.forEach(val => {
                this.targetList.forEach( target => {
                    if(val.code === target || val.code === target.code) {
                        this.sourcevalues.splice(this.sourcevalues.indexOf(val),1);
                        }
                });
            });
        }
       
        return this.sourcevalues;
    }

    updateSourceList(event: any): Values[] {
        const sourceList = [];
        const items = event.items;
        const codelist = [];
        this.parent.values.forEach(val => {
            codelist.push(val.code);
        });
        if (items && items.length > 0) {
            items.forEach(item => {
                if (codelist.includes(item)) {
                    console.log('dont add');
                } else {
                    sourceList.push(item);
                }
            });
        }
        let valuelist: Values[];
        sourceList.forEach(code => {
            if(code !== undefined) {
                valuelist.push(this.convertCodeToValue(code));
            }
        });

        return valuelist;

       // check if the value in the $event.items is already present in source list. If it is, then dont add to source values
    /*    this.parent.values.forEach(parentVal => { //A,B,C
            if (items && items.length > 0) {
                items.forEach(item => { //B,C,D
                    if (parentVal.code !== undefined && parentVal.code === item) {
                        console.log('dont do anything '+parentVal.code+ ' event val '+item);
                    } else {
                        sourceList.push(parentVal);
                    }
                });
            }    
        });
        console.log('################## '+sourceList.length);
        return sourceList; // A,B,C,D */
    }

    convertCodeToValue(code: any) : Values {
        const codeValue : Values[] = this.selectedvalues.filter(function(val) {
            return (val.code === code);
        }).map(function(val) {
            return val;
        });

        if (codeValue && codeValue.length > 0) {
            return codeValue[0];
        }
    }
 }
