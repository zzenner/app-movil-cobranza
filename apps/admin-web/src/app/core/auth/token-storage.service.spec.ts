import { TestBed } from '@angular/core/testing';
import { TokenStorageService } from './token-storage.service';

describe('TokenStorageService', () => {
  let service: TokenStorageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TokenStorageService);
  });

  it('should start with null token', () => {
    expect(service.getAccessToken()).toBeNull();
  });

  it('should store and retrieve access token', () => {
    service.setAccessToken('mi-token');
    expect(service.getAccessToken()).toBe('mi-token');
  });

  it('should clear access token', () => {
    service.setAccessToken('token');
    service.clear();
    expect(service.getAccessToken()).toBeNull();
  });

  it('should not persist to localStorage', () => {
    service.setAccessToken('secreto');
    expect(localStorage.getItem('secreto')).toBeNull();
    expect(Object.keys(localStorage).some((k) => k.includes('token'))).toBe(false);
  });
});
