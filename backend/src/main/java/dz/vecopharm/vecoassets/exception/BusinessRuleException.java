package dz.vecopharm.vecoassets.exception;

/**
 * Raised by the service layer when a business rule is violated
 * (e.g. reforming an asset that is already assigned, scanning a closed
 * inventory campaign, duplicating a serial number). Maps to HTTP 422.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
