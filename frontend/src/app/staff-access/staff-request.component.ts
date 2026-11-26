import { Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../auth.service';
import { AreaChoice, StaffAccess, StaffAccessService, StaffRole, staffRoleName } from './staff-access.service';

@Component({
  selector: 'app-staff-request',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './staff-request.component.html',
  styleUrl: './staff-access.css',
})
export class StaffRequestComponent implements OnInit {
  private access = inject(StaffAccessService);
  private auth = inject(AuthService);
  private router = inject(Router);

  readonly roleName = staffRoleName;
  areas: AreaChoice[] = [];
  requests: StaffAccess[] = [];
  areaId: number | null = null;
  role: StaffRole = 'TICKET_COUNTER';
  busy = false;
  error = '';
  message = '';

  ngOnInit() {
    this.load();
  }

  submit() {
    if (this.areaId === null || this.busy) return;
    this.busy = true;
    this.error = '';
    this.message = '';
    this.access.request(this.areaId, this.role).subscribe({
      next: request => {
        this.busy = false;
        this.requests = [request, ...this.requests.filter(existing => existing.id !== request.id)];
        this.message = request.requestedRole ? 'Your request was sent.' : 'You already have access to this area.';
      },
      error: error => this.showError(error, 'Could not send your request'),
    });
  }

  refresh() {
    this.busy = true;
    this.auth.validateSession().subscribe(valid => {
      if (!valid) {
        this.router.navigate(['/login']);
      } else if (this.auth.homeUrl !== '/request-access') {
        this.router.navigateByUrl(this.auth.homeUrl);
      } else {
        this.load();
      }
    });
  }

  logout() {
    this.busy = true;
    this.auth.logout().subscribe(() => this.router.navigate(['/login']));
  }

  private load() {
    this.busy = true;
    this.error = '';
    forkJoin({ areas: this.access.getAreas(), requests: this.access.getRequests() }).subscribe({
      next: ({ areas, requests }) => {
        this.areas = areas;
        this.requests = requests;
        this.busy = false;
      },
      error: error => this.showError(error, 'Could not load access requests'),
    });
  }

  private showError(error: HttpErrorResponse, fallback: string) {
    this.error = error.error?.message ?? fallback;
    this.busy = false;
  }
}
