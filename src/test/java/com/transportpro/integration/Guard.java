package com.transportpro.integration;

/** Les vérifications qui écrivent dans la base refusent de tourner ailleurs que sur une copie « *_test ». */
final class Guard {
    private Guard() {}

    static void requireTestDatabase() {
        if (!System.getProperty("emplogo.db.url", "").contains("_test")) {
            System.out.println("Refusé : lancez cette vérification sur une copie de la base, par exemple\n"
                    + "  -Demplogo.db.url=jdbc:mysql://localhost:3306/employgo_test");
            System.exit(2);
        }
    }
}
