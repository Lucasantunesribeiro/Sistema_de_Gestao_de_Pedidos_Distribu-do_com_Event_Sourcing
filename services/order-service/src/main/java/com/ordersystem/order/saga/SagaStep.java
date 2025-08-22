package com.ordersystem.order.saga;

/**
 * Enumeration representing the steps in the order processing saga workflow.
 * 
 * Normal Flow:
 * INVENTORY_RESERVATION → PAYMENT_PROCESSING → ORDER_CONFIRMATION → COMPLETED
 * 
 * Compensation Flow:
 * Any step → COMPENSATING → FAILED
 * 
 * Each step has specific responsibilities and next step transitions.
 */
public enum SagaStep {
    
    /**
     * Reserve inventory for the order items
     * Next: PAYMENT_PROCESSING (success) or COMPENSATING (failure)
     */
    INVENTORY_RESERVATION(false, false),
    
    /**
     * Process payment for the order
     * Next: ORDER_CONFIRMATION (success) or COMPENSATING (failure)
     */
    PAYMENT_PROCESSING(false, false),
    
    /**
     * Confirm order and finalize transaction
     * Next: COMPLETED (success) or COMPENSATING (failure)
     */
    ORDER_CONFIRMATION(false, false),
    
    /**
     * Execute compensation actions (rollback)
     * Next: FAILED or COMPENSATED
     */
    COMPENSATING(false, true),
    
    /**
     * Saga completed successfully
     * Terminal state
     */
    COMPLETED(true, false),
    
    /**
     * Saga failed and could not be recovered
     * Terminal state
     */
    FAILED(true, true);
    
    private final boolean terminal;
    private final boolean compensation;
    
    SagaStep(boolean terminal, boolean compensation) {
        this.terminal = terminal;
        this.compensation = compensation;
    }
    
    /**
     * @return true if this is a terminal step (saga execution finished)
     */
    public boolean isTerminal() {
        return terminal;
    }
    
    /**
     * @return true if this step is part of compensation flow
     */
    public boolean isCompensation() {
        return compensation;
    }
    
    /**
     * @return the next step in the normal flow
     */
    public SagaStep getNextStep() {
        switch (this) {
            case INVENTORY_RESERVATION:
                return PAYMENT_PROCESSING;
            case PAYMENT_PROCESSING:
                return ORDER_CONFIRMATION;
            case ORDER_CONFIRMATION:
                return COMPLETED;
            case COMPENSATING:
                return FAILED;
            default:
                throw new IllegalStateException("No next step for terminal step: " + this);
        }
    }
    
    /**
     * @return the compensation step for this step
     */
    public SagaStep getCompensationStep() {
        if (isTerminal()) {
            throw new IllegalStateException("Cannot compensate terminal step: " + this);
        }
        return COMPENSATING;
    }
    
    /**
     * @return true if this step can be retried in case of failure
     */
    public boolean canRetry() {
        return !terminal && !compensation;
    }
    
    /**
     * @return execution order for this step (used for recovery ordering)
     */
    public int getExecutionOrder() {
        switch (this) {
            case INVENTORY_RESERVATION: return 1;
            case PAYMENT_PROCESSING: return 2;
            case ORDER_CONFIRMATION: return 3;
            case COMPENSATING: return 4;
            case COMPLETED: return 5;
            case FAILED: return 6;
            default: return 0;
        }
    }
}