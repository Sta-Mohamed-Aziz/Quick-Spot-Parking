import { Routes } from '@angular/router';
import { authGuard, counterGuard, staffRequestGuard } from './auth.guard';
import { LoginComponent } from './login/login.component';
import { ParkingListComponent } from './parking-list/parking-list.component';
import { LayoutEditorComponent } from './layout-editor/layout-editor.component';
import { CounterComponent } from './counter/counter.component';
import { StaffRequestComponent } from './staff-access/staff-request.component';
import { StaffManagementComponent } from './staff-access/staff-management.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'request-access', component: StaffRequestComponent, canActivate: [staffRequestGuard] },
  { path: '', component: ParkingListComponent, canActivate: [authGuard] },
  { path: 'parking-areas/:id', component: LayoutEditorComponent, canActivate: [authGuard] },
  { path: 'parking-areas/:id/staff', component: StaffManagementComponent, canActivate: [authGuard] },
  { path: 'counter', component: CounterComponent, canActivate: [counterGuard] },
  { path: '**', redirectTo: '' },
];
