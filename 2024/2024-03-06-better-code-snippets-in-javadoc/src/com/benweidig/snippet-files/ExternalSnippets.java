package com.benweidig;

public class ExternalSnippet {

    // @start region="snippet01"
    /**
     * The first snippet.
     */
    public void snippet01() {
        System.out.println("Hello World!");  // @replace regex='".*"' replacement='"..."'
    }
    // @end

    // @start region="snippet02"
    /**
     * The second snippet.
     */
    public void snippet02() {
        System.out.println("Unbalanced Hello World!"); // @end region="snippet02"
    }

    
    public void snippet03() { // @start region="snippet03"
        System.out.println("Hello World!"); // @replace regex='".*"' replacement='"..."'
    } // @end
    
}