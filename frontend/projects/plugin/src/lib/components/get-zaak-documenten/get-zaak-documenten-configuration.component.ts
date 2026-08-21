import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FunctionConfigurationComponent} from "@valtimo/plugin";
import {BehaviorSubject, combineLatest, Observable, Subscription, take} from 'rxjs';
import {GetZaakDocumentenConfig} from './models/get-zaak-documenten-config';

@Component({
    standalone: false,
    selector: 'valtimo-get-zaak-documenten-configuration',
    templateUrl: './get-zaak-documenten-configuration.component.html'
})
export class GetZaakDocumentenConfigurationComponent
    // The component explicitly implements the FunctionConfigurationComponent interface
    implements FunctionConfigurationComponent, OnInit, OnDestroy {
    @Input() save$: Observable<void>;
    @Input() disabled$: Observable<boolean>;
    @Input() pluginId: string;
    @Input() prefillConfiguration$: Observable<GetZaakDocumentenConfig>;
    @Output() valid: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() configuration: EventEmitter<GetZaakDocumentenConfig> =
        new EventEmitter<GetZaakDocumentenConfig>();

    private saveSubscription!: Subscription;

    private readonly formValue$ = new BehaviorSubject<GetZaakDocumentenConfig | null>(null);
    private readonly valid$ = new BehaviorSubject<boolean>(false);

    ngOnInit(): void {
        this.openSaveSubscription();
    }

    ngOnDestroy() {
        this.saveSubscription?.unsubscribe();
    }

    formValueChange(formValue: GetZaakDocumentenConfig): void {
        this.formValue$.next(formValue);
        this.handleValid(formValue);
    }

    private handleValid(formValue: GetZaakDocumentenConfig): void {
        const valid = !!formValue?.aanvraagZaakUrl;

        this.valid$.next(valid);
        this.valid.emit(valid);
    }

    private openSaveSubscription(): void {
        this.saveSubscription = this.save$?.subscribe(save => {
            combineLatest([this.formValue$, this.valid$])
                .pipe(take(1))
                .subscribe(([formValue, valid]) => {
                    if (valid) {
                        this.configuration.emit(formValue);
                    }
                });
        });
    }
}
