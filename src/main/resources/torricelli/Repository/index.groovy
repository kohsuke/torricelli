L l = taglib(L)

l.layout {// "Torricelli &#x00BB; ${it.name}"
    DIV(ID:"center",STYLE:"text-align:center; margin:1em;",
        "Check out this repository by: <tt style='margin-left:1em'>hg clone ${request.requestURL}</tt>")

    l.left {
        P("main content!")
    }

    l.right {
        l.nav([
            [HREF:"configure",  TITLE:"Configure"],
            [HREF:"tags",       TITLE:"Tags"]
        ])
    }
}