package com.benweidig;

public class InlineDocumentation {

    /**
     * A demo of the new JavaDoc block tag for snippets.
     *
     * {@snippet id='demo' lang='java' :
     * public void snippetsSpecialCharacters() {
     *     System.out.println("No more escaping! @ & 🎉");
     * }
     * }
     */
    public void newSnippetTag() {
        // ...
    }

    /**
     * {@snippet :
     * public static void main(String... args) { // @highlight substring="main"
     *     for (var arg : args) { // @highlight region regex="\barg\b" type="highlighted"
     *         if (!arg.isBlank()) {
     *             System.out.println(arg);
     *         }
     *     }                      // @end
     * }
     * }
     */
    public void highlight() {
        // ...
    }

    /**
     * {@snippet :
     * class HelloWorld {
     *     public static void main(String... args) {
     *         System.out.println("Hello World!");  // @replace regex='".*"' replacement="..."
     *     }
     * }
     * }
     */
    public void replace() {
        // ...
    }

    /**
     * {@snippet :
     * class HelloWorld {
     *     public static void main(String... args) {
     *         System.out.println("Hello World!");  // @replace regex='"(.*)"' replacement='"I said, $1"'
     *     }
     * }
     * }
     */
    public void replaceWithGroup() {
        // ...
    }

    /**
     * {@snippet :
     * System.out.println("Link to println"); // @link substring="println" target="java.io.PrintStream#println(String)"
     * }
     */
    public void link() { 
        //...
    }
}