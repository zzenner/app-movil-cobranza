import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError, Subject } from 'rxjs';
import { vi } from 'vitest';
import { CarterasListComponent } from './carteras-list.component';
import { CarterasService } from '../../services/carteras.service';
import { ItemCatalogoCartera } from '../../models/cartera.models';

describe('CarterasListComponent', () => {
  let fixture: ComponentFixture<CarterasListComponent>;
  const serviceMock = {
    listarCatalogo: vi.fn().mockReturnValue(of([])),
  };

  beforeEach(async () => {
    serviceMock.listarCatalogo = vi.fn().mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [CarterasListComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CarterasService, useValue: serviceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CarterasListComponent);
  });

  it('muestra spinner mientras no hay datos cargados', () => {
    const pending$ = new Subject<ItemCatalogoCartera[]>();
    serviceMock.listarCatalogo.mockReturnValue(pending$.asObservable());
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(fixture.componentInstance.cargando()).toBe(true);
    expect(el.querySelector('mat-spinner')).toBeTruthy();
    pending$.complete();
  });

  it('muestra carteras al cargar exitosamente', () => {
    serviceMock.listarCatalogo.mockReturnValue(
      of([{ id: '1', codigoOrigen: 'C1', nombre: 'Cartera 1', descripcion: null, activa: true }]),
    );
    fixture.detectChanges();
    expect(fixture.componentInstance.carteras()).toHaveLength(1);
    expect(fixture.componentInstance.cargando()).toBe(false);
  });

  it('muestra error cuando falla la carga', () => {
    serviceMock.listarCatalogo.mockReturnValue(throwError(() => new Error('HTTP error')));
    fixture.detectChanges();
    expect(fixture.componentInstance.error()).not.toBeNull();
    expect(fixture.componentInstance.cargando()).toBe(false);
  });
});
