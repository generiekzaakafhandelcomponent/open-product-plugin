import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FunctionConfigurationComponent, PluginTranslationService} from "@valtimo/plugin";
import {BehaviorSubject, combineLatest, Observable, Subscription, take} from 'rxjs';
import {fromJaNeeSelectionId, jaNeeSelectItems$, toJaNeeSelectionId} from '../../models';
import {CreateProductConfig} from './models/create-product-config';

@Component({
    standalone: false,
    selector: 'valtimo-create-product-configuration',
    templateUrl: './create-product-configuration.component.html',
    styleUrls: ['./create-product-configuration.component.scss'],
})
export class CreateProductConfigurationComponent
    // The component explicitly implements the FunctionConfigurationComponent interface
    implements FunctionConfigurationComponent, OnInit, OnDestroy {
    @Input() save$: Observable<void>;
    @Input() disabled$: Observable<boolean>;
    @Input() pluginId: string;
    @Input() prefillConfiguration$: Observable<CreateProductConfig>;
    @Output() valid: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() configuration: EventEmitter<CreateProductConfig> =
        new EventEmitter<CreateProductConfig>();

    private saveSubscription!: Subscription;

    private readonly formValue$ = new BehaviorSubject<CreateProductConfig | null>(null);
    private readonly valid$ = new BehaviorSubject<boolean>(false);

    jaNeeSelectItems$!: Observable<Array<{id: string; text: string}>>;

    constructor(private readonly pluginTranslationService: PluginTranslationService) {}

    ngOnInit(): void {
        this.jaNeeSelectItems$ = jaNeeSelectItems$(this.pluginTranslationService, this.pluginId);
        this.openSaveSubscription();
    }

    gepubliceerdSelectionId(prefill: CreateProductConfig | null): string {
        return toJaNeeSelectionId(prefill?.gepubliceerd);
    }

    koppelZaakDocumentenSelectionId(prefill: CreateProductConfig | null): string {
        return toJaNeeSelectionId(prefill?.koppelZaakDocumenten);
    }

    ngOnDestroy() {
        this.saveSubscription?.unsubscribe();
    }

    formValueChange(formValue: CreateProductConfig): void {
        this.formValue$.next(formValue);
        this.handleValid(formValue);

    }

    private handleValid(formValue: CreateProductConfig): void {
        const valid = !!(formValue.productTypeUuid && formValue.eigenaarBsn && formValue.productStatus && (formValue.aanvraagZaakUrn || formValue.aanvraagZaakUrl));

        this.valid$.next(valid);
        this.valid.emit(valid);
    }

    private openSaveSubscription(): void {
        this.saveSubscription = this.save$?.subscribe(save => {
            combineLatest([this.formValue$, this.valid$])
                .pipe(take(1))
                .subscribe(([formValue, valid]) => {
                    if (valid) {
                        this.configuration.emit({
                            ...formValue,
                            gepubliceerd: fromJaNeeSelectionId(formValue.gepubliceerd),
                            koppelZaakDocumenten: fromJaNeeSelectionId(formValue.koppelZaakDocumenten),
                        });
                    }
                });
        });
    }
}
