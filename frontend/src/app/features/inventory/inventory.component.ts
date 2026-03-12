import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { InventoryService } from '../../core/services/inventory.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { InventoryStatus, InventoryItem } from '../../core/models/inventory.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

type PageState = 'loading' | 'error' | 'empty' | 'success';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, LoadingSpinnerComponent],
  templateUrl: './inventory.component.html',
  styleUrls: ['./inventory.component.scss'],
})
export class InventoryComponent implements OnInit, OnDestroy {
  private readonly inventoryService = inject(InventoryService);
  private readonly wsService = inject(WebSocketService);
  private wsSub!: Subscription;

  state: PageState = 'loading';
  inventory: InventoryStatus | null = null;
  errorMessage = '';
  lastUpdated: Date | null = null;
  filterLowStock = false;

  ngOnInit(): void {
    this.loadInventory();
    this.wsSub = this.wsService.inventoryEvents$.subscribe(() => {
      this.loadInventory();
    });
  }

  loadInventory(): void {
    this.state = 'loading';
    this.inventoryService.getInventoryStatus().subscribe({
      next: (data) => {
        this.inventory = data;
        this.lastUpdated = new Date();
        this.state = data.items.length === 0 ? 'empty' : 'success';
      },
      error: (err: Error) => {
        this.errorMessage = err.message;
        this.state = 'error';
      },
    });
  }

  get filteredItems(): InventoryItem[] {
    if (!this.inventory) return [];
    if (this.filterLowStock) {
      return this.inventory.items.filter(
        (item) => item.availableQuantity < 10
      );
    }
    return this.inventory.items;
  }

  getStockStatus(item: InventoryItem): 'critical' | 'low' | 'ok' {
    if (item.availableQuantity === 0) return 'critical';
    if (item.availableQuantity < 10) return 'low';
    return 'ok';
  }

  getStockPercent(item: InventoryItem): number {
    if (item.totalQuantity === 0) return 0;
    return Math.round((item.availableQuantity / item.totalQuantity) * 100);
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }
}
