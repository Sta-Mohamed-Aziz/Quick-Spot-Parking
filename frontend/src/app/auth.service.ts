import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, Observable, of, switchMap, tap } from 'rxjs';

export interface User {
  id: number;
  name: string;
  email: string;
  role: string;
  isHandicapped: boolean;
  isEvOwner: boolean;
  isBlocked: boolean;
  platformAdmin: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

const STORAGE_KEY = 'parking_admin_user';
const API_BASE = 'http://localhost:8080/api';

const WEB_ROLES = ['ADMIN', 'TICKET_COUNTER', 'EMPLOYEE'];

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  private _currentUser: User | null = null;

  constructor() {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        this._currentUser = JSON.parse(stored) as User;
      } catch {
        this._currentUser = null;
      }
    }
  }

  get currentUser(): User | null {
    return this._currentUser;
  }

  get isLoggedIn(): boolean {
    return this._currentUser !== null;
  }

  get isTicketCounter(): boolean {
    return this._currentUser?.role === 'TICKET_COUNTER';
  }

  get homeUrl(): string {
    if (this._currentUser?.role === 'EMPLOYEE') return '/request-access';
    return this.isTicketCounter ? '/counter' : '/';
  }

  get isAdmin(): boolean {
    return this._currentUser?.role === 'ADMIN';
  }

  get isPlatformAdmin(): boolean {
    return this._currentUser?.platformAdmin === true;
  }

  login(email: string, password: string): Observable<User> {
    return this.http.get(`${API_BASE}/auth/csrf`).pipe(
      switchMap(() => this.http.post<User>(`${API_BASE}/auth/login`, { email, password })),
      tap(user => this.rememberWebUser(user))
    );
  }

  register(name: string, email: string, password: string): Observable<User> {
    return this.http.post<User>(`${API_BASE}/auth/staff-register`, { name, email, password }).pipe(
      tap(user => this.rememberWebUser(user))
    );
  }

  validateSession(): Observable<boolean> {
    const current = this._currentUser;
    if (!current || !WEB_ROLES.includes(current.role)) return of(false);
    return this.http.get<User>(`${API_BASE}/auth/me`).pipe(
      tap(user => {
        if (user.id !== current.id) throw new Error('The signed-in account changed');
        this.rememberWebUser(user);
      }),
      map(() => true),
      catchError(() => {
        this.clearUser();
        return of(false);
      })
    );
  }

  logout(): Observable<void> {
    this.clearUser();
    return this.http.get(`${API_BASE}/auth/csrf`).pipe(
      switchMap(() => this.http.post<void>(`${API_BASE}/auth/logout`, {})),
      catchError(() => of(undefined))
    );
  }

  private rememberWebUser(user: User) {
    if (!WEB_ROLES.includes(user.role) || user.isBlocked) {
      this.clearUser();
      throw new Error('Access denied');
    }
    this._currentUser = user;
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user));
  }

  private clearUser() {
    this._currentUser = null;
    sessionStorage.removeItem(STORAGE_KEY);
  }
}
