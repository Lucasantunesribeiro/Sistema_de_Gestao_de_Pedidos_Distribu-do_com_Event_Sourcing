import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  FormArray,
  Validators,
  AbstractControl,
} from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { NotificationService } from '../../../core/services/notification.service';
import { CreateOrderRequest, PaymentMethod } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './order-create.component.html',
  styleUrls: ['./order-create.component.scss'],
})
export class OrderCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly orderService = inject(OrderService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  submitting = false;
  submitError = '';

  readonly paymentMethods: Array<{ value: PaymentMethod; label: string }> = [
    { value: 'PIX', label: 'PIX' },
    { value: 'CREDIT_CARD', label: 'Credit Card' },
    { value: 'DEBIT_CARD', label: 'Debit Card' },
    { value: 'BOLETO', label: 'Boleto' },
  ];

  readonly form: FormGroup = this.fb.group({
    customerId: ['', [Validators.required, Validators.minLength(3)]],
    customerName: ['', [Validators.required, Validators.minLength(2)]],
    paymentMethod: ['', Validators.required],
    items: this.fb.array(
      [this.createItemGroup()],
      [Validators.required, Validators.minLength(1)]
    ),
  });

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  get canAddItem(): boolean {
    return this.items.length < 3;
  }

  createItemGroup(): FormGroup {
    return this.fb.group({
      productId: ['', Validators.required],
      productName: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1), Validators.max(100)]],
      unitPrice: [null, [Validators.required, Validators.min(0.01)]],
    });
  }

  addItem(): void {
    if (this.canAddItem) {
      this.items.push(this.createItemGroup());
    }
  }

  removeItem(index: number): void {
    if (this.items.length > 1) {
      this.items.removeAt(index);
    }
  }

  itemTotal(index: number): number {
    const item = this.items.at(index);
    const qty = item.get('quantity')?.value ?? 0;
    const price = item.get('unitPrice')?.value ?? 0;
    return qty * price;
  }

  get orderTotal(): number {
    return this.items.controls.reduce((sum, _, i) => sum + this.itemTotal(i), 0);
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  }

  isFieldInvalid(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  getItemControl(index: number, field: string): AbstractControl | null {
    return this.items.at(index)?.get(field) ?? null;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.value;
    const request: CreateOrderRequest = {
      customerId: raw.customerId.trim(),
      customerName: raw.customerName.trim(),
      paymentMethod: raw.paymentMethod,
      items: raw.items.map((item: {
        productId: string;
        productName: string;
        quantity: number;
        unitPrice: number;
      }) => ({
        productId: item.productId.trim(),
        productName: item.productName.trim(),
        quantity: Number(item.quantity),
        unitPrice: Number(item.unitPrice),
      })),
    };

    this.submitting = true;
    this.submitError = '';

    this.orderService.createOrder(request).subscribe({
      next: (order) => {
        this.submitting = false;
        this.notificationService.success(
          `Order created successfully! ID: ${order.orderId.slice(0, 8)}...`
        );
        this.router.navigate(['/orders', order.orderId]);
      },
      error: (err: Error) => {
        this.submitting = false;
        this.submitError = err.message;
        this.notificationService.error('Failed to create order. Please review the form.');
      },
    });
  }
}
