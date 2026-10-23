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

import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {BaseApiService, ConfigService} from '@valtimo/shared';
import {Observable} from 'rxjs';

interface Producttype {
    uuid: string;
    naam: string | null;
    code: string | null;
}

interface JsonSchemaProperty {
    type?: string | string[];
    title?: string;
    description?: string;
}

interface DataobjectSchema {
    schema: {
        properties?: {[key: string]: JsonSchemaProperty};
        required?: string[];
    } | null;
}

@Injectable({providedIn: 'root'})
class OpenProductApiService extends BaseApiService {
    constructor(
        protected readonly httpClient: HttpClient,
        protected readonly configService: ConfigService
    ) {
        super(httpClient, configService);
    }

    getProducttypen(pluginConfigurationId: string): Observable<Producttype[]> {
        return this.httpClient.get<Producttype[]>(
            this.getApiUrl(`/management/v1/openproduct/${pluginConfigurationId}/producttypen`)
        );
    }

    getDataobjectSchema(pluginConfigurationId: string, producttypeUuid: string): Observable<DataobjectSchema> {
        return this.httpClient.get<DataobjectSchema>(
            this.getApiUrl(
                `/management/v1/openproduct/${pluginConfigurationId}/producttypen/${producttypeUuid}/dataobject-schema`
            )
        );
    }
}

export {DataobjectSchema, JsonSchemaProperty, OpenProductApiService, Producttype};
