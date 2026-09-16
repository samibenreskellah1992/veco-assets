package dz.vecopharm.vecoassets.exception;

/** Raised when a requested resource (asset, site, campaign, ...) does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
