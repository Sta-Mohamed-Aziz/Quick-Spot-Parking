import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from "../auth.service";

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  email = '';
  name = '';
  password = '';
  error = '';
  loading = false;
  registering = false;

  toggleRegistration() {
    this.registering = !this.registering;
    this.error = '';
    this.password = '';
  }

  submit() {
    this.error = '';

    if (!this.email.trim() || !this.password || (this.registering && !this.name.trim())) {
      this.error = this.registering ? 'Please enter your name, email and password.' : 'Please enter your email and password.';
      return;
    }

    this.loading = true;
    const request = this.registering
      ? this.auth.register(this.name.trim(), this.email.trim(), this.password)
      : this.auth.login(this.email.trim(), this.password);
    request.subscribe({
      next: () => {
        this.loading = false;
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(!this.registering && returnUrl?.startsWith('/') ? returnUrl : this.auth.homeUrl);
      },
      error: (err: unknown) => {
        this.loading = false;
        if (err instanceof Error && err.message.startsWith('Access denied')) {
          this.error = 'Use a staff account to sign in here.';
        } else if (err instanceof HttpErrorResponse) {
          if (err.status === 0) {
            this.error = 'Cannot reach the server. Make sure the backend is running.';
          } else if (err.status === 401) {
            this.error = 'Invalid email or password.';
          } else if (err.status === 403) {
            this.error = err.error?.message ?? 'Account is blocked or access is forbidden.';
          } else {
            this.error = err.error?.message ?? 'Login failed. Please try again.';
          }
        } else {
          this.error = 'Login failed. Please try again.';
        }
      },
    });
  }
}
