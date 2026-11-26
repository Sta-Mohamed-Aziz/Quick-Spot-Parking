import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { switchMap } from 'rxjs';

export type StaffRole = 'ADMIN' | 'TICKET_COUNTER';
export type StaffGrantRole = StaffRole | 'PERSONNEL';

export const staffRoleName: Record<StaffGrantRole, string> = {
  ADMIN: 'Administrator',
  TICKET_COUNTER: 'TICKET_COUNTER',
  PERSONNEL: 'Personnel',
};

export interface AreaChoice {
  id: number;
  name: string;
}

export interface StaffAccess {
  id: number;
  areaId: number;
  areaName: string;
  name: string;
  email: string;
  requestedRole: StaffRole | null;
  grantedRole: StaffRole | null;
  personnel: boolean;
}

@Injectable({ providedIn: 'root' })
export class StaffAccessService {
  private http = inject(HttpClient);
  private api = 'http://localhost:8080/api';

  getAreas() {
    return this.http.get<AreaChoice[]>(`${this.api}/staff-access/areas`);
  }

  getRequests() {
    return this.http.get<StaffAccess[]>(`${this.api}/staff-access/requests`);
  }

  request(areaId: number, role: StaffRole) {
    return this.post('/staff-access/requests', { areaId, role });
  }

  getStaff(areaId: number) {
    return this.http.get<StaffAccess[]>(`${this.api}/admin/areas/${areaId}/staff`);
  }

  grant(areaId: number, email: string, role: StaffGrantRole) {
    return this.post(`/admin/areas/${areaId}/staff`, { email, role });
  }

  approve(areaId: number, id: number) {
    return this.post(`/admin/areas/${areaId}/staff/${id}/approve`, {});
  }

  private post(path: string, body: object) {
    return this.http.get(`${this.api}/auth/csrf`).pipe(
      switchMap(() => this.http.post<StaffAccess>(`${this.api}${path}`, body))
    );
  }
}
