package svenmeier.coxswain.util;

public interface Destroyable {
    Destroyable NULL = new Destroyable() {
        @Override
        public void destroy() { }
    };

    void destroy();
}
