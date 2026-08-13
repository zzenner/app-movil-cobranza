import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { LayoutComponent } from './layout.component';
import { AuthService } from '../auth/auth.service';

const profileCompleto = {
  usuarioId: 'u1', sesionId: 's1', dispositivoId: null, tipoCliente: 'WEB' as const,
  nombreUsuario: 'tecnologia', roles: ['JEFE_SUPERVISORES'],
  permisos: ['USUARIOS_VER', 'DATOS_IMPORTAR'],
};

const profileSoloUsuarios = {
  usuarioId: 'u2', sesionId: 's2', dispositivoId: null, tipoCliente: 'WEB' as const,
  nombreUsuario: 'sup', roles: ['SUPERVISOR'],
  permisos: ['USUARIOS_VER'],
};

const profileSinImportacion = {
  usuarioId: 'u3', sesionId: 's3', dispositivoId: null, tipoCliente: 'WEB' as const,
  nombreUsuario: 'ejecutivo', roles: ['EJECUTIVO'],
  permisos: [],
};

async function renderLayout(permisos: string[]) {
  const profile = { ...profileCompleto, permisos };
  const fixture = TestBed.createComponent(LayoutComponent);
  const authService = TestBed.inject(AuthService);
  authService.markAsAuthenticated(profile, 'tok');
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement };
}

describe('LayoutComponent — menú de navegación', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LayoutComponent],
      providers: [
        provideRouter([
          { path: 'home', component: LayoutComponent },
          { path: 'usuarios', component: LayoutComponent },
          { path: 'importacion', component: LayoutComponent },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
      ],
    }).compileComponents();
  });

  // ── Inicio ─────────────────────────────────────────────────────────────────

  it('siempre muestra la opción Inicio', async () => {
    const { el } = await renderLayout(['USUARIOS_VER', 'DATOS_IMPORTAR']);
    const inicio = el.querySelector('a[routerlink="/home"]');
    expect(inicio).toBeTruthy();
    expect(inicio?.textContent).toContain('Inicio');
  });

  it('Inicio no requiere permiso especial — visible sin permisos', async () => {
    const { el } = await renderLayout([]);
    const inicio = el.querySelector('a[routerlink="/home"]');
    expect(inicio).toBeTruthy();
  });

  // ── Usuarios ───────────────────────────────────────────────────────────────

  it('muestra Usuarios cuando el perfil tiene USUARIOS_VER', async () => {
    const { el } = await renderLayout(['USUARIOS_VER']);
    const usuarios = el.querySelector('[data-testid="menu-usuarios"]');
    expect(usuarios).toBeTruthy();
    expect(usuarios?.textContent).toContain('Usuarios');
  });

  it('NO muestra Usuarios cuando el perfil no tiene USUARIOS_VER', async () => {
    const { el } = await renderLayout(['DATOS_IMPORTAR']);
    const usuarios = el.querySelector('[data-testid="menu-usuarios"]');
    expect(usuarios).toBeNull();
  });

  it('enlace de Usuarios apunta a /usuarios', async () => {
    const { el } = await renderLayout(['USUARIOS_VER']);
    const link = el.querySelector('[data-testid="menu-usuarios"]') as HTMLAnchorElement;
    expect(link?.getAttribute('routerlink') ?? link?.getAttribute('href')).toContain('usuarios');
  });

  // ── Importaciones ──────────────────────────────────────────────────────────

  it('muestra Importaciones cuando el perfil tiene DATOS_IMPORTAR', async () => {
    const { el } = await renderLayout(['DATOS_IMPORTAR']);
    const importacion = el.querySelector('[data-testid="menu-importacion"]');
    expect(importacion).toBeTruthy();
    expect(importacion?.textContent).toContain('Importaciones');
  });

  it('NO muestra Importaciones cuando el perfil no tiene DATOS_IMPORTAR', async () => {
    const { el } = await renderLayout(['USUARIOS_VER']);
    const importacion = el.querySelector('[data-testid="menu-importacion"]');
    expect(importacion).toBeNull();
  });

  it('NO muestra Importaciones con perfil sin permisos', async () => {
    const { el } = await renderLayout([]);
    const importacion = el.querySelector('[data-testid="menu-importacion"]');
    expect(importacion).toBeNull();
  });

  it('enlace de Importaciones apunta a /importacion', async () => {
    const { el } = await renderLayout(['DATOS_IMPORTAR']);
    const link = el.querySelector('[data-testid="menu-importacion"]') as HTMLAnchorElement;
    expect(link?.getAttribute('routerlink') ?? link?.getAttribute('href')).toContain('importacion');
  });

  // ── Perfil con ambos permisos ──────────────────────────────────────────────

  it('muestra Usuarios e Importaciones con ambos permisos', async () => {
    const { el } = await renderLayout(['USUARIOS_VER', 'DATOS_IMPORTAR']);
    expect(el.querySelector('[data-testid="menu-usuarios"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="menu-importacion"]')).toBeTruthy();
  });

  // ── computed signals ───────────────────────────────────────────────────────

  it('tienePermisoImportacion es true con DATOS_IMPORTAR', async () => {
    const { fixture } = await renderLayout(['DATOS_IMPORTAR']);
    expect(fixture.componentInstance.tienePermisoImportacion()).toBe(true);
  });

  it('tienePermisoImportacion es false sin DATOS_IMPORTAR', async () => {
    const { fixture } = await renderLayout(['USUARIOS_VER']);
    expect(fixture.componentInstance.tienePermisoImportacion()).toBe(false);
  });
});
