package fluent.bundle;

import static java.util.Objects.requireNonNull;

public final class FluentBundleException extends RuntimeException {

    ///  Constructor. A non-null message is required.
    FluentBundleException(String message) {
        super( requireNonNull( message ) );
    }

    ///  getMessage(), but should never be null.
    public String getMessage() {
        return super.getMessage();
    }

}
