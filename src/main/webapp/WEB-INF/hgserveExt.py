# Mercurial extension module to be loaded into the "hg serve" process.
# This defines a set of additional commands that the front end uses
from mercurial.hgweb.hgweb_mod import hgweb

# test
def do_kohsuke(self, req):
    req.header([('Content-Type','text/plain')])
    req.write("Hello")

hgweb.do_kohsuke = do_kohsuke