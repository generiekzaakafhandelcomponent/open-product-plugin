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

type EigenaarType = 'bsn' | 'kvk_nummer' | 'klantnummer';

type ValueMode = 'fixed' | 'pv';

interface DataobjectMappingEntry {
    key: string;
    value: string;
}

interface CreateOrUpdateProductConfig {
    producttypeUuid: string;
    eigenaarType: EigenaarType;
    eigenaarIdentificatie: string;
    eigenaarVestigingsnummer?: string;
    productNaam?: string;
    productStatus?: string;
    productPrijs?: string;
    productFrequentie?: string;
    gepubliceerd?: string;
    startDatum?: string;
    eindDatum?: string;
    aanvraagZaakUrn?: string;
    aanvraagZaakUrl?: string;
    documenten?: string;
    dataobjectMapping: DataobjectMappingEntry[];
    resultaatVariabelNaam?: string;
}

export {CreateOrUpdateProductConfig, DataobjectMappingEntry, EigenaarType, ValueMode};
