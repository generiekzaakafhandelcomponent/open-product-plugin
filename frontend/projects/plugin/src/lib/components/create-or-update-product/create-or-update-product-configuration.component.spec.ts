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

import {CommonModule} from '@angular/common';
import {Pipe, PipeTransform} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {InputModule, SelectModule} from 'carbon-components-angular';
import {Observable, of, Subject} from 'rxjs';
import {DataobjectSchema, OpenProductApiService, Producttype} from '../../services/open-product-api.service';
import {CreateOrUpdateProductConfigurationComponent} from './create-or-update-product-configuration.component';
import {CreateOrUpdateProductConfig} from './models/create-or-update-product-config';

@Pipe({name: 'pluginTranslate', standalone: false})
class PluginTranslateStubPipe implements PipeTransform {
    transform(key: string): Observable<string> {
        return of(key);
    }
}

const PRODUCTTYPEN: Producttype[] = [
    {uuid: 'type-parkeren', naam: 'Parkeervergunning', code: 'PV'},
    {uuid: 'type-hond', naam: 'Hondenbelasting', code: null},
];

const SCHEMA: DataobjectSchema = {
    schema: {
        properties: {kenteken: {type: 'string'}, aantalUren: {type: 'number'}},
        required: ['kenteken'],
    },
};

const STANDARD_KEYS = [
    'productNaam',
    'productStatus',
    'productPrijs',
    'productFrequentie',
    'gepubliceerd',
    'startDatum',
    'eindDatum',
    'aanvraagZaakUrn',
    'aanvraagZaakUrl',
];

describe('CreateOrUpdateProductConfigurationComponent', () => {
    let fixture: ComponentFixture<CreateOrUpdateProductConfigurationComponent>;
    let component: CreateOrUpdateProductConfigurationComponent;
    let save$: Subject<void>;
    let validity: boolean[];
    let saved: CreateOrUpdateProductConfig[];
    let schemaRequests: string[][];
    let lateSchema$: Subject<DataobjectSchema> | null;

    function create(prefill?: CreateOrUpdateProductConfig, disabled = false): void {
        fixture = TestBed.createComponent(CreateOrUpdateProductConfigurationComponent);
        component = fixture.componentInstance;
        save$ = new Subject<void>();
        validity = [];
        saved = [];
        component.save$ = save$;
        component.disabled$ = of(disabled);
        component.pluginId = 'openproduct';
        component.selectedPluginConfigurationData$ = of({configurationId: 'configuration-1', configurationTitle: 'Open Product'});
        if (prefill) {
            component.prefillConfiguration$ = of(prefill);
        }
        component.valid.subscribe(valid => validity.push(valid));
        component.configuration.subscribe(configuration => saved.push(configuration));
        fixture.detectChanges();
    }

    function element<T extends HTMLElement>(selector: string): T | null {
        return fixture.nativeElement.querySelector(selector);
    }

    function nativeSelect(testId: string): HTMLSelectElement {
        return element<HTMLSelectElement>(`[data-testid="${testId}"] select`);
    }

    function choose(testId: string, value: string): void {
        const select = nativeSelect(testId);
        select.value = value;
        select.dispatchEvent(new Event('change'));
        fixture.detectChanges();
    }

    function type(testId: string, value: string): void {
        const input = element<HTMLInputElement>(`input[data-testid="${testId}"]`);
        input.value = value;
        input.dispatchEvent(new Event('input'));
        fixture.detectChanges();
    }

    beforeEach(async () => {
        schemaRequests = [];
        lateSchema$ = null;
        await TestBed.configureTestingModule({
            declarations: [CreateOrUpdateProductConfigurationComponent, PluginTranslateStubPipe],
            imports: [CommonModule, ReactiveFormsModule, InputModule, SelectModule],
            providers: [
                {
                    provide: OpenProductApiService,
                    useValue: {
                        getProducttypen: () => of(PRODUCTTYPEN),
                        getDataobjectSchema: (configurationId: string, producttypeUuid: string) => {
                            schemaRequests.push([configurationId, producttypeUuid]);
                            return lateSchema$ ?? of(SCHEMA);
                        },
                    },
                },
            ],
        }).compileComponents();
    });

    it('should list the product types of the selected plugin configuration', () => {
        create();

        const options = Array.from(nativeSelect('producttype').options).map(option => option.textContent.trim());

        expect(options).toEqual(['producttypeKiezen', 'Parkeervergunning', 'Hondenbelasting']);
    });

    it('should offer a fixed value or a process variable for the owner, every standard field and every dataobject field', () => {
        create();
        choose('producttype', 'type-parkeren');

        expect(schemaRequests).toEqual([['configuration-1', 'type-parkeren']]);
        const fieldKeys = ['eigenaarIdentificatie', ...STANDARD_KEYS, 'dataobject.kenteken', 'dataobject.aantalUren'];
        fieldKeys.forEach(fieldKey => {
            const modes = Array.from(nativeSelect(`mode-${fieldKey}`).options).map(option => option.value);
            expect(modes).withContext(fieldKey).toEqual(['fixed', 'pv']);
            expect(element(`[data-testid="value-${fieldKey}"]`)).withContext(fieldKey).not.toBeNull();
        });
        expect(element('[data-testid="row-dataobject.kenteken"]').textContent).toContain('kenteken (string) *');
        expect(element('[data-testid="row-dataobject.aantalUren"]').textContent).not.toContain('*');
    });

    it('should ask for a vestigingsnummer only when the owner is identified by KVK number', () => {
        create();
        expect(element('[data-testid="row-eigenaarVestigingsnummer"]')).toBeNull();

        choose('eigenaarType', 'kvk_nummer');

        expect(element('[data-testid="row-eigenaarVestigingsnummer"]')).not.toBeNull();
    });

    it('should only be valid with a product type, an owner, the application zaak and every required dataobject field', () => {
        create();
        expect(validity[validity.length - 1]).toBeFalse();

        choose('producttype', 'type-parkeren');
        choose('mode-eigenaarIdentificatie', 'pv');
        type('value-eigenaarIdentificatie', 'bsn');
        type('value-dataobject.kenteken', 'AB-12-CD');
        expect(validity[validity.length - 1]).toBeFalse();

        choose('mode-aanvraagZaakUrl', 'pv');
        type('value-aanvraagZaakUrl', 'zaakUrl');
        expect(validity[validity.length - 1]).toBeTrue();

        type('value-dataobject.kenteken', '');
        expect(validity[validity.length - 1]).toBeFalse();
    });

    it('should save a fixed value as the literal and a process variable as a single pv reference', () => {
        create();
        choose('producttype', 'type-parkeren');
        choose('mode-eigenaarIdentificatie', 'pv');
        type('value-eigenaarIdentificatie', 'bsn');
        choose('mode-productNaam', 'pv');
        type('value-productNaam', 'pv:naam');
        type('value-productPrijs', '12.50');
        choose('value-productFrequentie', 'eenmalig');
        choose('mode-aanvraagZaakUrl', 'pv');
        type('value-aanvraagZaakUrl', 'zaakUrl');
        choose('mode-dataobject.kenteken', 'pv');
        type('value-dataobject.kenteken', 'kenteken');
        type('value-dataobject.aantalUren', '12');
        type('documenten', 'pv:zaakDocumenten');

        save$.next();

        expect(saved).toEqual([
            {
                producttypeUuid: 'type-parkeren',
                eigenaarType: 'bsn',
                eigenaarIdentificatie: 'pv:bsn',
                productNaam: 'pv:naam',
                productPrijs: '12.50',
                productFrequentie: 'eenmalig',
                aanvraagZaakUrl: 'pv:zaakUrl',
                dataobjectMapping: [
                    {key: 'kenteken', value: 'pv:kenteken'},
                    {key: 'aantalUren', value: '12'},
                ],
                documenten: 'pv:zaakDocumenten',
            },
        ]);
    });

    it('should keep dataobject fields read-only when the configuration is shown disabled', () => {
        lateSchema$ = new Subject<DataobjectSchema>();
        create(
            {
                producttypeUuid: 'type-parkeren',
                eigenaarType: 'bsn',
                eigenaarIdentificatie: 'pv:bsn',
                aanvraagZaakUrl: 'pv:zaakUrl',
                dataobjectMapping: [{key: 'kenteken', value: 'pv:kenteken'}],
            },
            true
        );
        lateSchema$.next(SCHEMA);
        fixture.detectChanges();

        expect(element<HTMLInputElement>('input[data-testid="value-dataobject.kenteken"]').disabled).toBeTrue();
        expect(nativeSelect('mode-dataobject.aantalUren').disabled).toBeTrue();
    });

    it('should show a saved configuration with the choice and value it was saved with', () => {
        create({
            producttypeUuid: 'type-parkeren',
            eigenaarType: 'kvk_nummer',
            eigenaarIdentificatie: '12345678',
            eigenaarVestigingsnummer: 'pv:vestiging',
            productNaam: 'pv:naam',
            productStatus: 'actief',
            aanvraagZaakUrl: 'pv:zaakUrl',
            dataobjectMapping: [
                {key: 'kenteken', value: 'pv:kenteken'},
                {key: 'aantalUren', value: '12'},
            ],
            documenten: 'pv:zaakDocumenten',
            resultaatVariabelNaam: 'opgeslagenProduct',
        });

        expect(nativeSelect('producttype').value).toBe('type-parkeren');
        expect(nativeSelect('eigenaarType').value).toBe('kvk_nummer');
        expect(nativeSelect('mode-eigenaarIdentificatie').value).toBe('fixed');
        expect(element<HTMLInputElement>('input[data-testid="value-eigenaarIdentificatie"]').value).toBe('12345678');
        expect(nativeSelect('mode-eigenaarVestigingsnummer').value).toBe('pv');
        expect(element<HTMLInputElement>('input[data-testid="value-eigenaarVestigingsnummer"]').value).toBe('vestiging');
        expect(nativeSelect('mode-productNaam').value).toBe('pv');
        expect(element<HTMLInputElement>('input[data-testid="value-productNaam"]').value).toBe('naam');
        expect(nativeSelect('value-productStatus').value).toBe('actief');
        expect(nativeSelect('mode-dataobject.kenteken').value).toBe('pv');
        expect(element<HTMLInputElement>('input[data-testid="value-dataobject.kenteken"]').value).toBe('kenteken');
        expect(nativeSelect('mode-dataobject.aantalUren').value).toBe('fixed');
        expect(element<HTMLInputElement>('input[data-testid="value-dataobject.aantalUren"]').value).toBe('12');
        expect(element<HTMLInputElement>('input[data-testid="documenten"]').value).toBe('zaakDocumenten');
        expect(element<HTMLInputElement>('input[data-testid="resultaatVariabelNaam"]').value).toBe('opgeslagenProduct');
        expect(validity[validity.length - 1]).toBeTrue();
    });
});
