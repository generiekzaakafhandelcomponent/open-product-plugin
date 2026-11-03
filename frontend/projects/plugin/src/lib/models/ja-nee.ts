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

import {PluginTranslationService} from '@valtimo/plugin';
import {combineLatest, map, Observable} from 'rxjs';

const JA = 'true';
const NEE = 'false';

const jaNeeSelectItems$ = (
    pluginTranslationService: PluginTranslationService,
    pluginId: string
): Observable<Array<{id: string; text: string}>> =>
    combineLatest([
        pluginTranslationService.translate('ja', pluginId),
        pluginTranslationService.translate('nee', pluginId),
    ]).pipe(map(([ja, nee]) => [{id: JA, text: ja}, {id: NEE, text: nee}]));

// Process links written by hand can hold the value as a string ("true"), the UI stores a boolean.
const toJaNeeSelectionId = (value: boolean | string | null | undefined): string =>
    value === true || value === JA ? JA : NEE;

const fromJaNeeSelectionId = (value: boolean | string | null | undefined): boolean =>
    value === true || value === JA;

export {jaNeeSelectItems$, toJaNeeSelectionId, fromJaNeeSelectionId};
