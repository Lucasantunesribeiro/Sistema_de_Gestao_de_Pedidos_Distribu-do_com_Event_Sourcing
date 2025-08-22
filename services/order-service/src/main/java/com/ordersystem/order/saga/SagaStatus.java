package com.ordersystem.order.saga;

/**
 * Enumeration representing the possible states of a distributed saga.
 * 
 * Status Transitions:
 * INITIATED → IN_PROGRESS → COMPLETED (success path)
 * INITIATED → IN_PROGRESS → COMPENSATING → COMPENSATED (compensation path)
 * INITIATED → IN_PROGRESS → FAILED (failure path)
 * 
 * Terminal states: COMPLETED, COMPENSATED, FAILED
 * Active states: INITIATED, IN_PROGRESS, COMPENSATING
 */
public enum SagaStatus {
    
    /**
     * Saga has been created and is ready to start execution
     */
    INITIATED(false, false),
    
    /**
     * Saga is currently executing steps
     */
    IN_PROGRESS(false, false),
    
    /**
     * Saga completed successfully - all steps executed
     */
    COMPLETED(true, false),
    
    /**
     * Saga failed and cannot be recovered
     */
    FAILED(true, true),
    
    /**
     * Saga is executing compensation actions due to failure
     */
    COMPENSATING(false, true),
    
    /**
     * Saga completed compensation actions successfully
     */
    COMPENSATED(true, true);
    
    private final boolean terminal;
    private final boolean error;
    
    SagaStatus(boolean terminal, boolean error) {
        this.terminal = terminal;
        this.error = error;
    }
    
    /**
     * @return true if this is a terminal state (saga execution finished)
     */
    public boolean isTerminal() {
        return terminal;
    }
    
    /**
     * @return true if this represents an error state
     */
    public boolean isError() {
        return error;
    }
    
    /**
     * @return true if this saga can be recovered/retried
     */
    public boolean canBeRecovered() {
        return this == IN_PROGRESS || this == COMPENSATING;
    }
    
    /**
     * @return true if this saga is in an active state (not terminal)
     */
    public boolean isActive() {
        return !terminal;
    }
}