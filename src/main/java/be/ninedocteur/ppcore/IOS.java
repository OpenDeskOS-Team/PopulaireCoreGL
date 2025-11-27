package be.ninedocteur.ppcore;

public interface IOS {
    void startOS(String[] args);
    /**
     * Called every frame to allow the OS to draw in the existing OpenGL window.
     */
    void displayLoop();
}
