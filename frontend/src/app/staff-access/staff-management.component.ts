import { Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';
import { ParkingService } from '../parking.service';
import { StaffAccess, StaffAccessService, StaffRole, staffRoleName } from './staff-access.service';

@Component({
  selector: 'app-staff-management',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './staff-management.component.html',
  styleUrl: './staff-access.css',
})
export class StaffManagementComponent implements OnInit {
  private access = inject(StaffAccessService);
  private parking = inject(ParkingService);
  private areaId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  readonly roleName = staffRoleName;
  areaName = '';
  entries: StaffAccess[] = [];
  email = '';
  role: StaffRole = 'TICKET_COUNTER';
  busy = true;
  error = '';
  message = '';

  get requests() {
    return this.entries.filter(entry => entry.requestedRole !== null);
  }

  get staff() {
    return this.entries.filter(entry => entry.grantedRole !== null);
  }

  ngOnInit() {
    forkJoin({
      area: this.parking.getParkingArea(this.areaId),
      staff: this.access.getStaff(this.areaId),
    }).subscribe({
      next: ({ area, staff }) => {
        this.areaName = area.name;
        this.entries = staff;
        this.busy = false;
      },
      error: error => this.showError(error, 'Could not load staff for this area'),
    });
  }

  grant() {
    if (!this.email.trim() || this.busy) return;
    this.save(this.access.grant(this.areaId, this.email.trim(), this.role), true);
  }

  approve(request: StaffAccess) {
    this.save(this.access.approve(this.areaId, request.id));
  }

  private save(action: Observable<StaffAccess>, clearEmail = false) {
    this.busy = true;
    this.error = '';
    this.message = '';
    action.subscribe({
      next: entry => {
        this.entries = [entry, ...this.entries.filter(existing => existing.id !== entry.id)];
        this.message = `${entry.email} now has ${this.roleName[entry.grantedRole!].toLowerCase()} access.`;
        if (clearEmail) this.email = '';
        this.busy = false;
      },
      error: error => this.showError(error, 'Could not grant access'),
    });
  }

  private showError(error: HttpErrorResponse, fallback: string) {
    this.error = error.error?.message ?? fallback;
    this.busy = false;
  }
}
