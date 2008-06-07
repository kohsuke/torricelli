package torricelli.Group;

class Def {
    /**
     * Generates the navigation bar.
     */
    static navList(l) {
        l.nav([
            [HREF:"configure",  TITLE:"Configure"],
            [HREF:"delete",  TITLE:"Delete this group"]
        ])
    }
}
