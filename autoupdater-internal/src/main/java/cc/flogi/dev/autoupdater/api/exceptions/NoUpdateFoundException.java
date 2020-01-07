package cc.flogi.dev.autoupdater.api.exceptions;

/**
 * @author Caden Kriese
 *
 * Created on 01/05/2020.
 */
public class NoUpdateFoundException extends Exception {
    public NoUpdateFoundException(String message) {
        super(message);
    }
}
