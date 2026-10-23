/*
 * Copyright 2015-2025 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FormArray, FormBuilder, FormGroup} from '@angular/forms';
import {FunctionConfigurationComponent, PluginConfigurationData} from '@valtimo/plugin';
import {
    BehaviorSubject,
    catchError,
    combineLatest,
    distinctUntilChanged,
    filter,
    map,
    Observable,
    of,
    Subscription,
    switchMap,
    tap,
} from 'rxjs';
import {JsonSchemaProperty, OpenProductApiService, Producttype} from '../../services/open-product-api.service';
import {
    CreateOrUpdateProductConfig,
    DataobjectMappingEntry,
    EigenaarType,
    ValueMode,
} from './models/create-or-update-product-config';

interface StandardField {
    key: keyof CreateOrUpdateProductConfig;
    options?: string[];
}

interface DataobjectField {
    key: string;
    type: string;
    required: boolean;
}

const PV_PREFIX = 'pv:';

const STANDARD_FIELDS: StandardField[] = [
    {key: 'productNaam'},
    {key: 'productStatus', options: ['initieel', 'in_aanvraag', 'gereed', 'actief', 'ingetrokken', 'geweigerd', 'verlopen']},
    {key: 'productPrijs'},
    {key: 'productFrequentie', options: ['eenmalig', 'maandelijks', 'jaarlijks']},
    {key: 'gepubliceerd', options: ['true', 'false']},
    {key: 'startDatum'},
    {key: 'eindDatum'},
    {key: 'aanvraagZaakUrn'},
    {key: 'aanvraagZaakUrl'},
];

@Component({
    standalone: false,
    selector: 'valtimo-create-or-update-product-configuration',
    templateUrl: './create-or-update-product-configuration.component.html',
    styleUrls: ['./create-or-update-product-configuration.component.scss'],
})
export class CreateOrUpdateProductConfigurationComponent
    implements FunctionConfigurationComponent, OnInit, OnDestroy {
    @Input() save$: Observable<void>;
    @Input() disabled$: Observable<boolean>;
    @Input() pluginId: string;
    @Input() prefillConfiguration$: Observable<CreateOrUpdateProductConfig>;
    @Input() selectedPluginConfigurationData$: Observable<PluginConfigurationData>;
    @Output() valid: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() configuration: EventEmitter<CreateOrUpdateProductConfig> =
        new EventEmitter<CreateOrUpdateProductConfig>();

    readonly standardFields = STANDARD_FIELDS;
    readonly eigenaarTypes: EigenaarType[] = ['bsn', 'kvk_nummer', 'klantnummer'];
    readonly producttypen$ = new BehaviorSubject<Producttype[] | null>(null);
    readonly dataobjectFields$ = new BehaviorSubject<DataobjectField[]>([]);
    readonly loadError$ = new BehaviorSubject<boolean>(false);

    readonly form: FormGroup = this.formBuilder.group({
        producttypeUuid: [''],
        eigenaarType: ['bsn'],
        eigenaarIdentificatie: this.valueGroup(),
        eigenaarVestigingsnummer: this.valueGroup(),
        standard: this.formBuilder.group(
            Object.fromEntries(STANDARD_FIELDS.map(field => [field.key, this.valueGroup()]))
        ),
        documenten: [''],
        dataobject: this.formBuilder.array([]),
        resultaatVariabelNaam: [''],
    });

    private readonly configurationId$ = new BehaviorSubject<string | null>(null);
    private readonly subscriptions = new Subscription();
    private prefillMapping: DataobjectMappingEntry[] = [];
    private prefillProducttypeUuid: string | null = null;
    private disabled = false;

    constructor(
        private readonly formBuilder: FormBuilder,
        private readonly openProductApiService: OpenProductApiService
    ) {}

    get dataobjectArray(): FormArray {
        return this.form.get('dataobject') as FormArray;
    }

    ngOnInit(): void {
        this.subscriptions.add(
            (this.selectedPluginConfigurationData$ ?? of(null))
                .pipe(map(configuration => configuration?.configurationId ?? null))
                .subscribe(configurationId => this.configurationId$.next(configurationId))
        );
        this.openProducttypenSubscription();
        this.openSchemaSubscription();
        this.openPrefillSubscription();
        this.openDisabledSubscription();
        this.subscriptions.add(this.form.valueChanges.subscribe(() => this.emitValid()));
        this.subscriptions.add(this.save$?.subscribe(() => this.save()));
        this.emitValid();
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    private openProducttypenSubscription(): void {
        this.subscriptions.add(
            this.configurationId$
                .pipe(
                    filter((configurationId): configurationId is string => !!configurationId),
                    distinctUntilChanged(),
                    switchMap(configurationId =>
                        this.openProductApiService.getProducttypen(configurationId).pipe(
                            catchError(() => {
                                this.loadError$.next(true);
                                return of([] as Producttype[]);
                            })
                        )
                    )
                )
                .subscribe(producttypen => this.producttypen$.next(producttypen))
        );
    }

    private openSchemaSubscription(): void {
        this.subscriptions.add(
            combineLatest([
                this.configurationId$.pipe(filter((configurationId): configurationId is string => !!configurationId)),
                this.form.get('producttypeUuid').valueChanges.pipe(
                    filter((producttypeUuid: string) => !!producttypeUuid),
                    distinctUntilChanged()
                ),
            ])
                .pipe(
                    tap(() => this.loadError$.next(false)),
                    switchMap(([configurationId, producttypeUuid]) =>
                        this.openProductApiService.getDataobjectSchema(configurationId, producttypeUuid).pipe(
                            catchError(() => {
                                this.loadError$.next(true);
                                return of({schema: null});
                            })
                        )
                    )
                )
                .subscribe(dataobjectSchema => {
                    const keepUnknownKeys = this.form.get('producttypeUuid').value === this.prefillProducttypeUuid;
                    const properties: {[key: string]: JsonSchemaProperty} = dataobjectSchema.schema?.properties ?? {};
                    const required = dataobjectSchema.schema?.required ?? [];
                    this.setDataobjectFields(
                        Object.entries(properties).map(([key, property]) => ({
                            key,
                            type: [property.type ?? ''].flat().join(', '),
                            required: required.includes(key),
                        })),
                        keepUnknownKeys
                    );
                })
        );
    }

    private openPrefillSubscription(): void {
        if (!this.prefillConfiguration$) {
            return;
        }
        this.subscriptions.add(
            this.prefillConfiguration$.pipe(filter(prefill => !!prefill)).subscribe(prefill => {
                this.prefillMapping = prefill.dataobjectMapping ?? [];
                this.prefillProducttypeUuid = prefill.producttypeUuid ?? null;
                this.setDataobjectFields(this.dataobjectFields$.value, true);
                this.form.patchValue({
                    producttypeUuid: prefill.producttypeUuid ?? '',
                    eigenaarType: prefill.eigenaarType ?? 'bsn',
                    eigenaarIdentificatie: this.toValueGroup(prefill.eigenaarIdentificatie),
                    eigenaarVestigingsnummer: this.toValueGroup(prefill.eigenaarVestigingsnummer),
                    standard: Object.fromEntries(
                        STANDARD_FIELDS.map(field => [field.key, this.toValueGroup(prefill[field.key])])
                    ),
                    documenten: this.withoutPvPrefix(prefill.documenten),
                    resultaatVariabelNaam: prefill.resultaatVariabelNaam ?? '',
                });
            })
        );
    }

    private openDisabledSubscription(): void {
        if (!this.disabled$) {
            return;
        }
        this.subscriptions.add(
            this.disabled$.subscribe(disabled => {
                this.disabled = disabled;
                if (disabled) {
                    this.form.disable();
                } else {
                    this.form.enable();
                }
            })
        );
    }

    // Keeps mapped keys the schema no longer lists while the saved product type is shown, so they are not lost silently.
    private setDataobjectFields(schemaFields: DataobjectField[], keepUnknownKeys: boolean): void {
        const current: DataobjectMappingEntry[] = this.dataobjectArray.length
            ? this.toDataobjectMapping()
            : this.prefillMapping;
        const unknownKeys = keepUnknownKeys
            ? current.map(entry => entry.key).filter(key => !schemaFields.some(field => field.key === key))
            : [];
        const fields = [
            ...schemaFields,
            ...unknownKeys.map(key => ({key, type: '', required: false})),
        ];
        this.dataobjectFields$.next(fields);
        this.dataobjectArray.clear();
        fields.forEach(field => {
            const value = current.find(entry => entry.key === field.key)?.value;
            this.dataobjectArray.push(
                this.formBuilder.group({key: [field.key], ...this.toValueGroup(value)})
            );
        });
        if (this.disabled) {
            this.dataobjectArray.disable();
        }
        this.emitValid();
    }

    private save(): void {
        if (this.isValid()) {
            this.configuration.emit(this.toConfiguration());
        }
    }

    private toConfiguration(): CreateOrUpdateProductConfig {
        const value = this.form.getRawValue();
        const standard = Object.fromEntries(
            STANDARD_FIELDS.map(field => [field.key, this.fromValueGroup(value.standard[field.key])])
                .filter(([, fieldValue]) => fieldValue !== undefined)
        );
        const configuration: CreateOrUpdateProductConfig = {
            producttypeUuid: value.producttypeUuid,
            eigenaarType: value.eigenaarType,
            eigenaarIdentificatie: this.fromValueGroup(value.eigenaarIdentificatie),
            ...standard,
            dataobjectMapping: this.toDataobjectMapping(),
        };
        const vestigingsnummer = this.fromValueGroup(value.eigenaarVestigingsnummer);
        if (value.eigenaarType === 'kvk_nummer' && vestigingsnummer !== undefined) {
            configuration.eigenaarVestigingsnummer = vestigingsnummer;
        }
        if (value.documenten?.trim()) {
            configuration.documenten = `${PV_PREFIX}${this.withoutPvPrefix(value.documenten.trim())}`;
        }
        if (value.resultaatVariabelNaam?.trim()) {
            configuration.resultaatVariabelNaam = value.resultaatVariabelNaam.trim();
        }
        return configuration;
    }

    private toDataobjectMapping(): DataobjectMappingEntry[] {
        return this.dataobjectArray
            .getRawValue()
            .map(entry => ({key: entry.key, value: this.fromValueGroup(entry)}))
            .filter((entry): entry is DataobjectMappingEntry => entry.value !== undefined);
    }

    private isValid(): boolean {
        const value = this.form.getRawValue();
        const mapping = this.toDataobjectMapping();
        const requiredFieldsSet = this.dataobjectFields$.value
            .filter(field => field.required)
            .every(field => mapping.some(entry => entry.key === field.key));
        const aanvraagZaakSet =
            this.fromValueGroup(value.standard.aanvraagZaakUrn) !== undefined ||
            this.fromValueGroup(value.standard.aanvraagZaakUrl) !== undefined;
        return (
            !!value.producttypeUuid &&
            !!value.eigenaarType &&
            this.fromValueGroup(value.eigenaarIdentificatie) !== undefined &&
            aanvraagZaakSet &&
            requiredFieldsSet &&
            !this.loadError$.value
        );
    }

    private emitValid(): void {
        this.valid.emit(this.isValid());
    }

    private valueGroup(): FormGroup {
        return this.formBuilder.group({mode: ['fixed'], value: ['']});
    }

    private toValueGroup(stored: unknown): {mode: ValueMode; value: string} {
        if (stored === undefined || stored === null) {
            return {mode: 'fixed', value: ''};
        }
        const text = `${stored}`;
        return text.startsWith(PV_PREFIX)
            ? {mode: 'pv', value: text.substring(PV_PREFIX.length)}
            : {mode: 'fixed', value: text};
    }

    private fromValueGroup(group: {mode: ValueMode; value: string}): string | undefined {
        const value = group?.value?.trim();
        if (!value) {
            return undefined;
        }
        return group.mode === 'pv' ? `${PV_PREFIX}${this.withoutPvPrefix(value)}` : value;
    }

    private withoutPvPrefix(stored: string | undefined): string {
        return stored?.startsWith(PV_PREFIX) ? stored.substring(PV_PREFIX.length) : stored ?? '';
    }
}
