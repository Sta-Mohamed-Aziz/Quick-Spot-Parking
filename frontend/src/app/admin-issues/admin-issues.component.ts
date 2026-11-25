import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { AdminIssueReport, AdminReportsService, IssueType } from '../admin-reports/admin-reports.service';

@Component({
  selector: 'app-admin-issues',
  standalone: true,
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './admin-issues.component.html',
  styleUrl: '../admin-reports/admin-reports.component.css',
  styles: ['.note { white-space: pre-wrap; overflow-wrap: anywhere; }',
    '.actions { flex-wrap: wrap; }', '.refund-confirmation { margin-top: 8px; }'],
})
export class AdminIssuesComponent implements OnInit {
  private service = inject(AdminReportsService);

  readonly filters = ['Open', 'Resolved', 'All'] as const;
  readonly labels: Record<IssueType, string> = {
    SPOT_TAKEN: 'Another vehicle in the spot',
    BLOCKED: 'Blocked spot',
    DAMAGED: 'Damaged or unsafe spot',
    OTHER: 'Other issue',
  };

  reports: AdminIssueReport[] = [];
  filter: typeof this.filters[number] = 'Open';
  loading = true;
  busy = false;
  confirmRefundId: number | null = null;
  error = '';
  message = '';

  ngOnInit() {
    this.load();
  }

  get visibleReports() {
    if (this.filter === 'All') return this.reports;
    return this.reports.filter(report => (report.resolvedAt === null) === (this.filter === 'Open'));
  }

  load() {
    this.loading = true;
    this.error = '';
    this.confirmRefundId = null;
    this.service.getIssueReports().subscribe({
      next: reports => {
        this.reports = reports;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.error = err.error?.message ?? 'Could not load driver reports';
        this.loading = false;
      },
    });
  }

  resolve(report: AdminIssueReport, refund: boolean) {
    this.busy = true;
    this.error = '';
    this.message = '';
    this.service.resolveIssue(report.id, refund).subscribe({
      next: updated => {
        this.busy = false;
        this.confirmRefundId = null;
        this.reports = this.reports.map(current => {
          if (current.id === updated.id) return updated;
          if (updated.refundAmount !== null && current.reservationId === updated.reservationId) {
            return { ...current, refundableAmount: null };
          }
          return current;
        });
        this.message = updated.refundAmount !== null
          ? 'Report resolved and the reservation credited.'
          : 'Report marked as resolved.';
      },
      error: (err: HttpErrorResponse) => {
        this.error = err.error?.message ?? 'Could not resolve the report';
        this.busy = false;
      },
    });
  }
}
