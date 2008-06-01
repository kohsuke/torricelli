import torricelli.Repository

L l = taglib(L)

List<Repository> repos = my.listRepositories();

/**
 * Generate a drop-down list of repositories
 */
def repoList = {
    SELECT(NAME:"src",STYLE:"width:100%") {
        repos.each { OPTION(it.name) }
    }
}

l.layout {
    l.left {
        H2("Mercurial Repositories")

        DIV(CLASS:"repositoryList") {
            repos.each { repo ->
                DIV {
                    A(HREF:repo.name) {
                        l.img(ALIGN:"middle", "package.gif")
                    }
                    A(HREF:repo.name,repo.name)
                    // just for now
                    text(" ")
                    A(HREF:repo.name+'/configure',STYLE:"font-size:0.5em; margin-left:1em","(configure)")
                }
            }
        }
    }

    l.right {
        DIV(CLASS:"box",STYLE:"width:auto;") {
            H2("Create a new repository")
            form("create") {
                TR {
                    TD("Name")
                    TD { INPUT(TYPE:"text",NAME:"name") }
                }
            }

            H2("Clone an existing repository")
            form("clone") {
                TR {
                    TD("From")
                    TD(repoList)
                }
                TR {
                    TD("To")
                    TD { INPUT(TYPE:"text",NAME:"name") }
                }
            }

            H2("Clone a remote repository")
            form("remoteClone") {
                TR {
                    TD("From")
                    TD { INPUT(TYPE:"text",NAME:"src") }
                }
                TR {
                    TD("To")
                    TD { INPUT(TYPE:"text",NAME:"name") }
                }
            }

            H2("Delete a repository")
            form("delete") {
                TR {
                    TD("Name")
                    TD(repoList)
                }
            }
        }
    }
}

/**
 * Defines a form in the box.
 */
def form(String name,Closure body) {
    FORM(ACTION:name,METHOD:"post") {
        TABLE(body)
        DIV(ALIGN:"right") {
            INPUT(TYPE:"submit",VALUE:name)
        }
    }
}
