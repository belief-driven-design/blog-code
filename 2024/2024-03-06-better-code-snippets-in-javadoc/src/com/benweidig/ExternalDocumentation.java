package com.benweidig;

public class ExternalDocumentation {

    /**
     * This includes the whole file.
     * {@snippet file="ExternalSnippets.java"}
     */
    public void wholeFile() {
        // ... 
    }

    /**
     * Include a specific class.
     * {@snippet class="ExternalSnippets"}
     */
    public void specificClass() {
        // ... 
    }

    /**
     * And this only the requested region.
     * {@snippet file="ExternalSnippets.java" region="snippet01"}
     */
    public void regionOnly() {
        // ... 
    }

    /**
     * This will render an unbalanced snippet by class name.
     * {@snippet class="ExternalSnippets" region="snippet02"}
     */
    public void unbalanced() {
        // ...
    }

    /**
     * {@snippet class="ExternalSnippets" region="snippet03" :
     * public void snippet03() {
     *     System.out.println("...");
     * }}
     */
    public void hybrid() {
        // ...
    }
}