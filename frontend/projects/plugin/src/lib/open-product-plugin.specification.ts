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

import {
    OpenProductConfigurationComponent
} from './components/open-product-configuration/open-product-configuration.component';
import {OPEN_PRODUCT_PLUGIN_LOGO_BASE64} from './assets';
import {PluginSpecification} from "@valtimo/plugin";
import {GetProductConfigurationComponent} from "./components/get-product/get-product-configuration.component";
import {CreateProductConfigurationComponent} from "./components/create-product/create-product-configuration.component";
import {UpdateProductConfigurationComponent} from "./components/update-product/update-product-configuration.component";
import {DeleteProductConfigurationComponent} from "./components/delete-product/delete-product-configuration.component";
import {
    GetAllProductsConfigurationComponent
} from "./components/get-all-products/get-all-products-configuration.component";
import {
    GetZaakDocumentenConfigurationComponent
} from "./components/get-zaak-documenten/get-zaak-documenten-configuration.component";
import {
    CreateOrUpdateProductConfigurationComponent
} from "./components/create-or-update-product/create-or-update-product-configuration.component";

const openProductPluginSpecification: PluginSpecification = {
    pluginId: 'openproduct',
    pluginConfigurationComponent: OpenProductConfigurationComponent,
    pluginLogoBase64: OPEN_PRODUCT_PLUGIN_LOGO_BASE64,
    functionConfigurationComponents: {
        /*
         For each plugin action id received from the backend, a component is provided of the interface FunctionConfigurationComponent.
         These are used to configure each plugin action.
         */
        'get-product': GetProductConfigurationComponent,
        'get-all-products': GetAllProductsConfigurationComponent,
        'get-zaak-documenten': GetZaakDocumentenConfigurationComponent,
        'create-product': CreateProductConfigurationComponent,
        'update-product': UpdateProductConfigurationComponent,
        'create-or-update-product': CreateOrUpdateProductConfigurationComponent,
        'delete-product': DeleteProductConfigurationComponent
    },

    pluginTranslations: {
        nl: {
            title: 'Open Product Plugin',
            description: 'Deze plugin maakt het mogelijk om producten van de open producten API te beheren binnen Valtimo.',
            configurationTitle: 'Configuratienaam',
            configurationTitleTooltip: 'Configuratie naam is de naam die gebruikt wordt om de configuratie te identificeren.',
            baseUrl: 'URL',
            'get-product': "Product ophalen via UUID",
            "create-product": "Product aanmaken",
            "delete-product": "Product verwijderen via UUID",
            "update-product": "Product bijwerken via UUID",
            "get-all-products": "Alle producten ophalen via BSN",
            "get-zaak-documenten": "Zaakdocumenten ophalen als productdocumenten",
            aanvraagZaakUrl: 'De URL van de zaak',
            productUuid: 'De UUID van het product',
            productNaam: 'De naam van het product',
            productTypeUuid: 'De UUID van het producttype',
            eigenaarBsn: 'De bsn van de eigenaar',
            gepubliceerd: 'Kan het object getoond worden?',
            productPrijs: 'De prijs van het product',
            productFrequentie: 'De frequentie van de betalingen',
            productStatus: 'De status van het product',
            resultaatPV: 'De naam van de process variable waar het resultaat in komt',
            authenticationPluginConfiguration: 'Selecteer de authenticatie plugin configuratie',
            dataobjectVariabelNaam: 'Naam van de procesvariabele voor het dataobject (optioneel)',
            producttypeUuid: 'UUID van het producttype om op te filteren (optioneel)',
            status: 'Status van het product om op te filteren (optioneel, bijv. gereed, actief)',
            resultaatVariabelNaam: 'Naam van de procesvariabele voor de resultatenlijst (standaard: alleProducten)',
            zaakDocumentenVariabelNaam: 'Naam van de procesvariabele voor de zaakdocumentenlijst (standaard: zaakDocumenten)',
            'create-or-update-product': 'Product aanmaken of bijwerken',
            producttypeKeuze: 'Producttype',
            producttypeKeuzeTooltip: 'De producttypen van de gekozen Open Product-configuratie. De velden van het dataobject komen uit het schema van dit producttype.',
            producttypeKiezen: 'Kies een producttype',
            openProductNietBereikbaar: 'De producttypen of het schema konden niet uit Open Producten worden opgehaald. Controleer de pluginconfiguratie.',
            eigenaarKop: 'Eigenaar',
            eigenaarType: 'De eigenaar wordt herkend aan',
            eigenaarType_bsn: 'BSN',
            eigenaarType_kvk_nummer: 'KVK-nummer',
            eigenaarType_klantnummer: 'Klantnummer',
            eigenaarIdentificatie: 'BSN, KVK-nummer of klantnummer van de eigenaar',
            eigenaarVestigingsnummer: 'Vestigingsnummer (optioneel)',
            productgegevensKop: 'Productgegevens',
            productgegevensUitleg: 'Laat een veld leeg om bij bijwerken de huidige waarde te houden. De aanvraagzaak (URN of URL) is verplicht en wordt alleen bij aanmaken meegestuurd.',
            aanvraagZaakUrn: 'De URN van de aanvraagzaak',
            startDatum: 'Startdatum (JJJJ-MM-DD)',
            eindDatum: 'Einddatum (JJJJ-MM-DD)',
            documenten: 'Procesvariabele met de documenten (optioneel)',
            documentenTooltip: 'Bijvoorbeeld de procesvariabele die de actie "Zaakdocumenten ophalen als productdocumenten" vult. Bij bijwerken worden de documenten toegevoegd aan de bestaande.',
            dataobjectKop: 'Dataobject',
            geenDataobjectVelden: 'Dit producttype heeft geen dataobject-schema.',
            resultaatVariabelNaamUpsert: 'Naam van de procesvariabele voor de UUID van het product (optioneel)',
            resultaatVariabelNaamTooltip: 'Standaard: productUuid',
            vasteWaarde: 'Vaste waarde',
            procesvariabele: 'Procesvariabele',
            procesvariabeleNaam: 'Naam van de procesvariabele',
        },
        en: {
            title: 'Open Product Plugin',
            description: 'This plugin enables management of products from the Open Products API within Valtimo.',
            configurationTitle: 'Configuration Name',
            configurationTitleTooltip: 'The name used to identify this configuration.',
            baseUrl: 'URL',
            "get-product": "Retrieve product via UUID",
            "create-product": "New product",
            "delete-product": "Delete product via UUID",
            "update-product": "Update product via UUID",
            "get-all-products": "Retrieve all products",
            "get-zaak-documenten": "Retrieve zaak documents as product documents",
            aanvraagZaakUrl: 'The URL of the zaak',
            productUuid: 'The UUID of the product',
            productNaam: 'The name of the product',
            productTypeUuid: 'The UUID of the product type',
            eigenaarBsn: 'The citizen service number (BSN) of the owner',
            gepubliceerd: 'Should this product be publicly visible?',
            productPrijs: 'The price of the product',
            productFrequentie: 'The payment frequency',
            productStatus: 'The product’s status',
            resultaatPV: 'The name of the process variable that will contain the result',
            authenticationPluginConfiguration: 'Select the authentication plugin configuration',
            dataobjectVariabelNaam: 'Process variable name for the data object (optional)',
            producttypeUuid: 'Product type UUID to filter on (optional)',
            status: 'Product status to filter on (optional, e.g. gereed, actief)',
            resultaatVariabelNaam: 'Process variable name for the results list (default: alleProducten)',
            zaakDocumentenVariabelNaam: 'Process variable name for the zaak documents list (default: zaakDocumenten)',
            'create-or-update-product': 'Create or update product',
            producttypeKeuze: 'Product type',
            producttypeKeuzeTooltip: 'The product types of the selected Open Product configuration. The dataobject fields come from the schema of this product type.',
            producttypeKiezen: 'Choose a product type',
            openProductNietBereikbaar: 'The product types or the schema could not be retrieved from Open Producten. Check the plugin configuration.',
            eigenaarKop: 'Owner',
            eigenaarType: 'The owner is identified by',
            eigenaarType_bsn: 'BSN',
            eigenaarType_kvk_nummer: 'KVK number',
            eigenaarType_klantnummer: 'Customer number',
            eigenaarIdentificatie: 'BSN, KVK number or customer number of the owner',
            eigenaarVestigingsnummer: 'Branch number (optional)',
            productgegevensKop: 'Product details',
            productgegevensUitleg: 'Leave a field empty to keep its current value on update. The application zaak (URN or URL) is required and is only sent on create.',
            aanvraagZaakUrn: 'The URN of the application zaak',
            startDatum: 'Start date (YYYY-MM-DD)',
            eindDatum: 'End date (YYYY-MM-DD)',
            documenten: 'Process variable holding the documents (optional)',
            documentenTooltip: 'For example the process variable filled by the action "Retrieve zaak documents as product documents". On update the documents are added to the existing ones.',
            dataobjectKop: 'Dataobject',
            geenDataobjectVelden: 'This product type has no dataobject schema.',
            resultaatVariabelNaamUpsert: 'Process variable name for the product UUID (optional)',
            resultaatVariabelNaamTooltip: 'Default: productUuid',
            vasteWaarde: 'Fixed value',
            procesvariabele: 'Process variable',
            procesvariabeleNaam: 'Name of the process variable',
        },
        de: {
            title: 'Open Product Plugin',
            description: 'Dieses Plugin ermöglicht die Verwaltung von Produkten der Open Products API innerhalb von Valtimo.',
            configurationTitle: 'Konfigurationsname',
            configurationTitleTooltip: 'Der Name, mit dem diese Konfiguration identifiziert wird.',
            baseUrl: 'URL',
            aanvraagZaakUrl: 'Die URL der Zaak',
            productUuid: 'Die UUID des Produkts',
            productNaam: 'Der Name des Produkts',
            productTypeUuid: 'Die UUID des Produkttyps',
            eigenaarBsn: 'Die BSN (Bürgernummer) des Eigentümers',
            gepubliceerd: 'Soll dieses Produkt öffentlich angezeigt werden?',
            productPrijs: 'Der Preis des Produkts',
            productFrequentie: 'Die Häufigkeit der Zahlungen',
            productStatus: 'Der Status des Produkts',
            resultaatPV: 'Der Name der Prozessvariablen, in der das Ergebnis gespeichert wird',
            authenticationPluginConfiguration: 'Wählen Sie die Authentifizierungs-Plugin-Konfiguration aus',
            dataobjectVariabelNaam: 'Prozessvariablenname für das Datenobjekt (optional)',
            producttypeUuid: 'Produkttyp-UUID zum Filtern (optional)',
            status: 'Produktstatus zum Filtern (optional, z. B. gereed, actief)',
            resultaatVariabelNaam: 'Prozessvariablenname für die Ergebnisliste (Standard: alleProducten)',
            zaakDocumentenVariabelNaam: 'Prozessvariablenname für die Zaakdokumentenliste (Standard: zaakDocumenten)',
            'create-or-update-product': 'Produkt anlegen oder aktualisieren',
            producttypeKeuze: 'Produkttyp',
            producttypeKeuzeTooltip: 'Die Produkttypen der gewählten Open-Product-Konfiguration. Die Felder des Datenobjekts stammen aus dem Schema dieses Produkttyps.',
            producttypeKiezen: 'Produkttyp wählen',
            openProductNietBereikbaar: 'Die Produkttypen oder das Schema konnten nicht aus Open Producten abgerufen werden. Prüfen Sie die Plugin-Konfiguration.',
            eigenaarKop: 'Eigentümer',
            eigenaarType: 'Der Eigentümer wird erkannt an',
            eigenaarType_bsn: 'BSN',
            eigenaarType_kvk_nummer: 'KVK-Nummer',
            eigenaarType_klantnummer: 'Kundennummer',
            eigenaarIdentificatie: 'BSN, KVK-Nummer oder Kundennummer des Eigentümers',
            eigenaarVestigingsnummer: 'Niederlassungsnummer (optional)',
            productgegevensKop: 'Produktdaten',
            productgegevensUitleg: 'Lassen Sie ein Feld leer, um beim Aktualisieren den aktuellen Wert zu behalten. Die Antrags-Zaak (URN oder URL) ist erforderlich und wird nur beim Anlegen gesendet.',
            aanvraagZaakUrn: 'Die URN der Antrags-Zaak',
            startDatum: 'Startdatum (JJJJ-MM-TT)',
            eindDatum: 'Enddatum (JJJJ-MM-TT)',
            documenten: 'Prozessvariable mit den Dokumenten (optional)',
            documentenTooltip: 'Zum Beispiel die Prozessvariable, die die Aktion zum Abrufen der Zaak-Dokumente füllt. Beim Aktualisieren werden die Dokumente zu den vorhandenen hinzugefügt.',
            dataobjectKop: 'Datenobjekt',
            geenDataobjectVelden: 'Dieser Produkttyp hat kein Datenobjekt-Schema.',
            resultaatVariabelNaamUpsert: 'Prozessvariablenname für die Produkt-UUID (optional)',
            resultaatVariabelNaamTooltip: 'Standard: productUuid',
            vasteWaarde: 'Fester Wert',
            procesvariabele: 'Prozessvariable',
            procesvariabeleNaam: 'Name der Prozessvariablen',
        }
    }
};

export {openProductPluginSpecification};
