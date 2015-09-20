package svenmeier.coxswain.rower;

/**
 */
public interface Rower {

    /**
     * Open the rower.
     *
     * @see #onStart()
     */
    void open();

    boolean isOpen();

    /**
     * Beats starts.
     */
    void onStart();

    void reset();

    /**
     * Row.
     *
     * @return whether still rowing
     */
    boolean row();

    /**
     * Beats ends.
     */
    void onEnd();

    /**
     * Close the rower.
     */
    void close();
}
